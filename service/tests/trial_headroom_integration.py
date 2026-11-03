"""Disposable empty local PostgreSQL DB; no active DB data export or provider calls."""
from pathlib import Path
import json, uuid
import psycopg
from psycopg import sql
root=Path(__file__).resolve().parents[2]
base='postgresql://postgres:postgres@127.0.0.1:56322/'
name='trial_headroom_'+uuid.uuid4().hex[:10]
created=False
report={'passed':False,'activeDatabaseModified':False,'providerCalls':0}
try:
 with psycopg.connect(base+'postgres',autocommit=True,connect_timeout=5) as admin:
  admin.execute(sql.SQL('create database {}').format(sql.Identifier(name)));created=True
 with psycopg.connect(base+name,autocommit=True) as conn:
  conn.execute('create schema private')
  original=(root/'service/supabase/migrations/202610220030_general_engine.sql').read_text().splitlines()[2]
  assert original.startswith('create table private.general_budget(')
  conn.execute(original)
  conn.execute('alter table private.general_budget drop constraint general_budget_credit_limit_check')
  conn.execute('alter table private.general_budget add constraint general_budget_credit_limit_check check(credit_limit between 0 and 100)')
  conn.execute('insert into private.general_budget(id,enabled,qualified,model_limit,model_used,credit_limit,credit_used,credit_held) values(true,true,true,100000,58989,100,80,20)')
  before=conn.execute('select to_jsonb(g) from private.general_budget g').fetchone()[0]
  assert conn.execute('select credit_used+credit_held+3<=credit_limit from private.general_budget').fetchone()[0] is False
  conn.execute((root/'service/supabase/migrations/202611030037_trial_credit_headroom.sql').read_text())
  assert conn.execute('select to_jsonb(g) from private.general_budget g').fetchone()[0]==before
  report['migrationPreservesAllRuntimeValues']=True
  conn.execute('update private.general_budget set credit_limit=150 where id')
  after=conn.execute('select to_jsonb(g) from private.general_budget g').fetchone()[0]
  assert {k:v for k,v in after.items() if k!='credit_limit'}=={k:v for k,v in before.items() if k!='credit_limit'}
  assert conn.execute('select model_used+model_held+20000<=model_limit and credit_used+credit_held+3<=credit_limit from private.general_budget').fetchone()[0] is True
  report['headroomRestoresAdmissionWithoutResettingUsage']=True
  for statement in ['update private.general_budget set credit_limit=151','update private.general_budget set model_limit=100001','update private.general_budget set credit_held=71','update private.general_budget set credit_used=-1']:
   blocked=False
   try:
    with conn.transaction():conn.execute(statement)
   except psycopg.errors.CheckViolation:blocked=True
   assert blocked
  report['FiniteCeilingModelCapAndAccountingGuardsRemain']=True
  report['passed']=True
finally:
 if created:
  with psycopg.connect(base+'postgres',autocommit=True) as admin:
   admin.execute(sql.SQL('drop database {} with (force)').format(sql.Identifier(name)))
out=root/'validation/trial-headroom';out.mkdir(exist_ok=True)
(out/'local-database.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report))
