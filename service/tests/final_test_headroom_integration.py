"""Disposable empty PostgreSQL database, synthetic counters, no provider calls."""
from pathlib import Path
import concurrent.futures
import json
import uuid
import psycopg
from psycopg import sql

ROOT = Path(__file__).resolve().parents[2]
BASE = 'postgresql://postgres:postgres@127.0.0.1:56322/'
NAME = 'final_headroom_' + uuid.uuid4().hex[:10]
created = False
report = {'passed': False, 'activeDatabaseModified': False, 'providerCalls': 0}
try:
    with psycopg.connect(BASE + 'postgres', autocommit=True, connect_timeout=5) as admin:
        admin.execute(sql.SQL('create database {}').format(sql.Identifier(NAME)))
        created = True
    with psycopg.connect(BASE + NAME, autocommit=True) as conn:
        conn.execute('create schema private')
        original = (ROOT / 'service/supabase/migrations/202610220030_general_engine.sql').read_text().splitlines()[2]
        assert original.startswith('create table private.general_budget(')
        conn.execute(original)
        conn.execute('alter table private.general_budget drop constraint general_budget_model_limit_check, drop constraint general_budget_credit_limit_check')
        conn.execute('alter table private.general_budget add check(model_limit between 0 and 300000), add check(credit_limit between 0 and 200), add column model_committed bigint not null default 0 check(model_committed>=0), add column credit_committed int not null default 0 check(credit_committed>=0), add check(model_used+model_held+model_committed<=model_limit and credit_used+credit_held+credit_committed<=credit_limit)')
        conn.execute('insert into private.general_budget(id,enabled,qualified,model_limit,model_used,model_held,model_committed,credit_limit,credit_used,credit_held,credit_committed) values(true,true,true,300000,89160,20000,172651,171,116,34,21)')
        before = conn.execute('select to_jsonb(g) from private.general_budget g').fetchone()[0]
        available = 'select model_used+model_held+model_committed+20000<=model_limit from private.general_budget where id'
        assert conn.execute(available).fetchone()[0] is False
        conn.execute((ROOT / 'service/supabase/migrations/202611200045_final_test_model_headroom.sql').read_text())
        assert conn.execute('select to_jsonb(g) from private.general_budget g').fetchone()[0] == before
        report['migrationPreservesRuntimeCounters'] = True
        conn.execute('update private.general_budget set model_limit=320000 where id')
        after = conn.execute('select to_jsonb(g) from private.general_budget g').fetchone()[0]
        assert {k:v for k,v in after.items() if k != 'model_limit'} == {k:v for k,v in before.items() if k != 'model_limit'}
        assert conn.execute(available).fetchone()[0] is True
        report['interpretationHeadroomWithoutRefundOrReset'] = True
        for statement in ['update private.general_budget set model_limit=320001', 'update private.general_budget set model_limit=280000', 'update private.general_budget set model_held=-1', 'update private.general_budget set credit_limit=201', 'update private.general_budget set credit_held=35']:
            blocked = False
            try:
                with conn.transaction():
                    conn.execute(statement)
            except psycopg.errors.CheckViolation:
                blocked = True
            assert blocked, statement
        report['finiteCapAndCommitmentGuardsRemain'] = True
    def reserve():
        with psycopg.connect(BASE + NAME) as conn:
            result = conn.execute('update private.general_budget set model_held=model_held+20000 where id and model_used+model_held+model_committed+20000<=model_limit returning id').fetchone()
            return result is not None
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        assert sum(pool.map(lambda _: reserve(), range(4))) == 1
    report['concurrentRequestsCannotOversubscribe'] = True
    report['passed'] = True
finally:
    if created:
        with psycopg.connect(BASE + 'postgres', autocommit=True) as admin:
            admin.execute(sql.SQL('drop database {} with (force)').format(sql.Identifier(NAME)))
output = ROOT / 'validation/final-watch-headroom'
output.mkdir(exist_ok=True)
(output / 'local-database.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report))
