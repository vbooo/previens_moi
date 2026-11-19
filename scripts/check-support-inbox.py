#!/usr/bin/env python3
"""Support SQL checks in a new empty disposable database of the LOCAL S3 stack.
Requires service/requirements-test.txt and the local PostgreSQL instance on 56322.
Never copies user data and never accepts a cloud URL.
"""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import json
import uuid
import psycopg
from psycopg import sql

ROOT = Path(__file__).resolve().parents[1]
BASE = "postgresql://postgres:postgres@127.0.0.1:56322/"


def main():
    target = "previens_support_" + uuid.uuid4().hex[:12]
    with psycopg.connect(BASE + "postgres", autocommit=True, connect_timeout=5) as admin:
        admin.execute(sql.SQL("create database {}").format(sql.Identifier(target)))
        try:
            with psycopg.connect(BASE + target, autocommit=True) as db:
                db.execute("create schema private; create schema extensions; create extension pgcrypto with schema extensions")
                for migration in ["202611190042_support.sql", "202611190043_support_retention.sql"]:
                    db.execute((ROOT / "service/supabase/migrations" / migration).read_text())

                def payload():
                    return dict(id=str(uuid.uuid4()), category="question", message="Synthetic SQL test", replyEmail=None, appVersion="0.2.4", language="fr")

                def submit(value, sender="a" * 64):
                    with psycopg.connect(BASE + target, autocommit=True) as connection:
                        return connection.execute("select public.support_submit(%s,%s,%s::jsonb)", (value["id"], sender, json.dumps(value))).fetchone()[0]

                value = payload()
                with ThreadPoolExecutor(max_workers=2) as pool:
                    receipts = list(pool.map(lambda _: submit(value), range(2)))
                assert receipts[0] == receipts[1]
                assert db.execute("select count(*) from private.support_messages").fetchone()[0] == 1
                for role in ["anon", "authenticated"]:
                    assert not db.execute("select has_function_privilege(%s,'public.support_submit(uuid,text,jsonb)','execute')", (role,)).fetchone()[0]
                    assert not db.execute("select has_table_privilege(%s,'private.support_messages','select')", (role,)).fetchone()[0]
                assert db.execute("select relrowsecurity from pg_class where oid='private.support_messages'::regclass").fetchone()[0]
                submit(payload())
                submit(payload())
                try:
                    submit(payload())
                    raise AssertionError("Per-sender cap missing")
                except psycopg.errors.RaiseException as error:
                    assert str(error).startswith("quota_exceeded")
                for index in range(17):
                    submit(payload(), f"{index:064x}")
                try:
                    submit(payload(), "f" * 64)
                    raise AssertionError("Global cap missing")
                except psycopg.errors.RaiseException as error:
                    assert str(error).startswith("quota_exceeded")
                # Keep one claim transaction open: a second worker must skip its row.
                with psycopg.connect(BASE + target) as worker:
                    first = worker.execute("select public.support_lease()").fetchone()[0]
                    second = db.execute("select public.support_lease()").fetchone()[0]
                    assert first["id"] != second["id"]
                db.execute("select public.support_finish(%s,true)", (first["id"],))
                assert db.execute("select state,message,reply_email from private.support_messages where id=%s", (first["id"],)).fetchone() == ("sent", None, None)
                db.execute("select public.support_finish(%s,false)", (second["id"],))
                assert db.execute("select state,message from private.support_messages where id=%s", (second["id"],)).fetchone() == ("failed", "Synthetic SQL test")
                assert "delete from" not in db.execute("select pg_get_functiondef('public.support_submit(uuid,text,jsonb)'::regprocedure)").fetchone()[0].lower()
                print(json.dumps(dict(passed=True, isolatedEmptyDatabase=True, concurrentIdempotence=True, privateAccess=True, perSenderAndGlobalCaps=True, concurrentClaimsDistinct=True, acceptedContentCleared=True, failedContentRetained=True, requestTimeDeletionDisabled=True, providerCalls=0)))
        finally:
            admin.execute(sql.SQL("drop database {} with (force)").format(sql.Identifier(target)))


if __name__ == "__main__":
    main()
