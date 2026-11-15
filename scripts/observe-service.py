#!/usr/bin/env python3
"""Read-only trial snapshots. No worker wakeup, provider request, reset or quota change.

Install service/requirements-test.txt locally. Connection settings stay in .local.
Reports contain counters only and stay ignored under .local/observation.
"""
import argparse
from datetime import datetime, timezone
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def forecast(days=7, interval_hours=24, sources=3):
    """Conservative reservations for a NEW stable_v1 continuous watch.

    Includes understanding, preparation/coverage, baseline, and all due checks.
    Rediscovery reserves eight web credits every 24 cycles in the current worker.
    This is NOT a price per credit or permission to change a confirmed cadence.
    """
    if not (1 <= days <= 30 and interval_hours in (1, 24) and 1 <= sources <= 3):
        raise ValueError('Use 1–30 days, interval 1 or 24 hours, and 1–3 sources.')
    checks = math.ceil(days * 24 / interval_hours)
    rediscoveries = checks // 24
    return {
        'days': days,
        'intervalHours': interval_hours,
        'sourcesUpper': sources,
        'scheduledChecks': checks,
        'modelCallsUpper': checks + 3,
        'modelReservationMicroUsd': (checks + 3) * 20000,
        'webCreditReservationUpper': 2 + sources + checks * sources + rediscoveries * (8 - sources),
        'extraRestartsOrEditsIncluded': False,
        'automaticRetriesIncluded': False,
        'existingConfirmedPlansChanged': False,
        'requiresCadencePolicyChange': interval_hours != 24,
    }


QUERIES = {
    'capacity': 'select enabled,slots,reserved,active_per_account,budget_units,used_units,reserved_units from private.capacity',
    'generalBudget': 'select enabled,qualified,model_limit,model_used,model_held,model_committed,credit_limit,credit_used,credit_held,credit_committed from private.general_budget',
    'watches': """select status,remote_state,health,plan->>'method' method,
        plan->>'sourcePolicy' policy,plan->>'intervalHours' interval_hours,
        jsonb_array_length(plan->'sources') sources,count(*) count,
        min(last_success_at) oldest_success,max(last_failure_at) latest_failure,
        count(*) filter(where status='active' and next_check_at<now()-interval '15 minutes') overdue
        from private.watches group by 1,2,3,4,5,6,7""",
    'checks7Days': """select outcome,count(*) count,min(observed_at) first_at,max(observed_at) last_at
        from private.checks where observed_at>=now()-interval '7 days' group by 1""",
    'checksByDay': """select date_trunc('day',observed_at at time zone 'UTC') as day_utc,outcome,count(*) count
        from private.checks where observed_at>=now()-interval '7 days' group by 1,2 order by 1,2""",
    'attempts': """select phase,state,count(*) count,sum(actual) actual_micro_usd,sum(credits) credits,
        sum(model_reserved) model_reserved,sum(credits_reserved) credits_reserved
        from private.general_attempts group by 1,2""",
    'jobs': "select kind,state,count(*) count from private.jobs group by 1,2",
    'push24Hours': """select state,count(*) count from private.outbox
        where created_at>=now()-interval '24 hours' group by 1""",
    'scheduler': "select jobname,schedule,active from cron.job where jobname like 'previens-moi-%'",
    'scheduler24Hours': """select j.jobname,d.status,count(*) count,
        max(d.start_time) last_start,max(d.end_time) last_end
        from cron.job_run_details d join cron.job j on j.jobid=d.jobid
        where d.start_time>=now()-interval '24 hours' and j.jobname like 'previens-moi-%'
        group by 1,2""",
    'workerSchedule': 'select enabled from private.worker_schedule',
    'trials': 'select (ended_at is not null) ended,count(*) count,sum(model_used) model_used,sum(model_held) model_held,sum(credit_used) credit_used,sum(credit_held) credit_held from private.account_trials group by 1',
}


def read_snapshot():
    import psycopg
    credentials = {}
    for line in (ROOT / '.local/cloud/supabase.env').read_text().splitlines():
        name, sep, value = line.partition('=')
        if sep and not name.lstrip().startswith('#'):
            credentials[name.strip()] = value.strip().strip('\"\'')
    uri = (ROOT / 'service/supabase/.temp/pooler-url').read_text().strip()
    with psycopg.connect(uri, password=credentials['SUPABASE_DB_PASSWORD'], sslmode='require', connect_timeout=15) as conn:
        conn.execute('set transaction isolation level repeatable read, read only')
        conn.execute("set local statement_timeout='15s'")
        data = {}
        for label, sql in QUERIES.items():
            try:
                cur = conn.execute(sql)
            except psycopg.Error as exc:
                raise RuntimeError(label + ":" + (exc.sqlstate or "unknown")) from None
            names = [col.name for col in cur.description]
            data[label] = [dict(zip(names, row)) for row in cur.fetchall()]
        # pg_net responses expire: never infer seven days of delivery from this short window.
        if conn.execute("select to_regclass('net._http_response') is not null").fetchone()[0]:
            cur = conn.execute("""select status_code,timed_out,count(*) count,max(created) last_at
                from net._http_response where created>=now()-interval '6 hours' group by 1,2""")
            data['httpResponses6HoursProjectWide'] = [dict(zip([c.name for c in cur.description], row)) for row in cur.fetchall()]
        conn.rollback()
    budget = data['generalBudget'][0]
    data['remaining'] = {
        'modelMicroUsd': budget['model_limit'] - budget['model_used'] - budget['model_held'] - budget['model_committed'],
        'webCredits': budget['credit_limit'] - budget['credit_used'] - budget['credit_held'] - budget['credit_committed'],
    }
    data['sevenDayNewWatchForecasts'] = [forecast(interval_hours=n) for n in (24, 1)]
    data['createdAt'] = datetime.now(timezone.utc).isoformat()
    data['readOnly'] = True
    data['observationStarted'] = False
    data['sevenDaysProven'] = False
    data['limits'] = [
        'A snapshot is not continuous monitoring or proof of seven days.',
        'Cron success only proves database scheduling; HTTP responses are project-wide and short-lived.',
        'FCM accepted is not proof of display, reading or relevance on a phone.',
        'No titles, source bodies, user IDs, email addresses, device tokens or credentials are selected.',
        'Forecasts reserve worst-case work; unchanged pages may cost less but still need the reservation.',
    ]
    return data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('plan', 'snapshot'))
    parser.add_argument('--days', type=int, default=7)
    parser.add_argument('--interval-hours', type=int, choices=(1, 24), default=24)
    parser.add_argument('--sources', type=int, default=3)
    args = parser.parse_args()
    if args.action == 'plan':
        print(json.dumps(forecast(args.days, args.interval_hours, args.sources), indent=2))
        return
    try:
        data = read_snapshot()
        directory = ROOT / '.local/observation'
        directory.mkdir(parents=True, exist_ok=True, mode=0o700)
        directory.chmod(0o700)
        path = directory / (datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ') + '.json')
        path.touch(mode=0o600, exist_ok=False)
        path.write_text(json.dumps(data, default=str, indent=2) + '\n')
        print(json.dumps({'snapshot': str(path.relative_to(ROOT)), 'remaining': data['remaining'],
                          'observationStarted': False, 'providerCalls': 0}))
    except Exception as exc:
        # Connection/provider exception messages can contain private infrastructure details.
        raise SystemExit('Snapshot failed (' + type(exc).__name__ + '); no state changed.') from None


if __name__ == '__main__':
    main()
