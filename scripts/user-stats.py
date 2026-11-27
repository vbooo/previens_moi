#!/usr/bin/env python3
"""Private aggregate service snapshot. Read-only; no identities, text or tokens selected.

Run with .local/contracts-venv/bin/python scripts/user-stats.py. The output stays
under .local/operator-stats and must not be committed or published.
"""
from pathlib import Path
from datetime import datetime, timezone
import json

ROOT = Path(__file__).resolve().parents[1]
QUERIES = {
    "accounts": "select count(*) filter(where deleted_at is null) present, count(*) filter(where created_at>=now()-interval '30 days' and deleted_at is null) created30d from private.accounts",
    "usersWithWatches": "select count(distinct w.owner_id) everCreatedInRetainedData, count(distinct w.owner_id) filter(where w.status='active') withActiveWatches from private.watches w join private.accounts a on a.id=w.owner_id where a.deleted_at is null",
    "watchesByStatus": "select status,count(*) watches,count(distinct owner_id) users from private.watches group by status order by status",
    "subscriptions": "select test,state,count(*) purchases,count(distinct owner_id) users,count(distinct owner_id) filter(where active and expires_at>now()) entitledUsers from private.billing_purchases group by test,state order by test,state",
    "checks14d": "select outcome,count(*) checks,count(distinct watch_id) watches from private.checks where observed_at>=now()-interval '14 days' group by outcome order by outcome",
    "findings90d": "select count(*) findings,count(distinct owner_id) users from private.findings where created_at>=now()-interval '90 days'",
    "preparations24h": "select count(*) evaluations,count(distinct token_hash) preparationSessions from private.evaluations where created_at>=now()-interval '24 hours'",
    "delivery24h": "select state,count(*) messages from private.outbox where created_at>=now()-interval '24 hours' group by state order by state",
}

def snapshot():
    import psycopg
    settings = {}
    for line in (ROOT / '.local/cloud/supabase.env').read_text().splitlines():
        name, sep, value = line.partition('=')
        if sep and not name.lstrip().startswith('#'):
            settings[name.strip()] = value.strip().strip('\"\'')
    uri = (ROOT / 'service/supabase/.temp/pooler-url').read_text().strip()
    with psycopg.connect(uri, password=settings['SUPABASE_DB_PASSWORD'], sslmode='require', connect_timeout=15) as conn:
        conn.execute('set transaction isolation level repeatable read, read only')
        conn.execute("set local statement_timeout='15s'")
        data = {}
        for label, query in QUERIES.items():
            cur = conn.execute(query)
            data[label] = [dict(zip([c.name for c in cur.description], row)) for row in cur.fetchall()]
        conn.rollback()
    return {"asOfUtc": datetime.now(timezone.utc).isoformat(), "readOnly": True, "metrics": data,
            "limitations": ["Includes test activity; only billing rows distinguish test purchases.",
            "Retained current records, not a permanent lifetime analytics archive.",
            "Active users means owners of active watches, not daily/monthly app visitors.",
            "Anonymous preparation sessions are not identifiable or deduplicated people.",
            "Deleted accounts and purged events disappear; older history cannot be reconstructed.",
            "FCM accepted does not prove a notification was displayed or read."]}

if __name__ == '__main__':
    try:
        result = snapshot()
        folder = ROOT / '.local/operator-stats'
        folder.mkdir(parents=True, exist_ok=True, mode=0o700)
        folder.chmod(0o700)
        target = folder / 'latest.json'
        target.touch(mode=0o600, exist_ok=True)
        target.chmod(0o600)
        target.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps(result, ensure_ascii=False, indent=2))
    except Exception as exc:
        raise SystemExit('Read-only statistics failed (' + type(exc).__name__ + '); private diagnostics hidden.') from None
