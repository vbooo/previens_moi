"""Isolated database copy. Model/provider responses injected; zero paid calls.
Never migrates or truncates the active local/cloud databases.
"""
from pathlib import Path
import subprocess,uuid,json,time,datetime as dt,sys
import psycopg
from psycopg.types.json import Jsonb
R=Path(__file__).resolve().parents[2];D='/Applications/Docker.app/Contents/Resources/bin/docker';C='supabase_db_previens-moi-s3';DB='postgresql://postgres:postgres@127.0.0.1:56322/';name='capacity_'+uuid.uuid4().hex[:10]
def run(args,**kwargs):return subprocess.run(args,check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kwargs)
with psycopg.connect(DB+'postgres') as source:
 assert source.execute("select count(*) from auth.users where email not like '%%.test' and email is not null").fetchone()[0]==0,'Refuse export of non-test local identities'
raw=run([D,'exec',C,'pg_dump','-U','postgres','-d','postgres','--format=custom','--schema=auth','--schema=private','--schema=public','--schema=extensions','--no-owner','--no-privileges']).stdout
created=False;report={'passed':False,'scope':'Disposable local database; injected model/source outcomes, not semantic quality or cloud transport','paidCalls':0,'activeDatabaseModified':False}
def v7():
 h=f'{int(time.time()*1000):012x}'+'7'+uuid.uuid4().hex[13:16]+'8'+uuid.uuid4().hex[17:];return str(uuid.UUID(h))
try:
 run([D,'exec',C,'createdb','-U','postgres',name]);created=True;run([D,'exec',C,'psql','-U','postgres','-d',name,'-c','drop schema public cascade']);run([D,'exec','-i',C,'pg_restore','-U','postgres','-d',name,'--no-owner','--no-privileges','--exit-on-error'],input=raw)
 with psycopg.connect(DB+name,autocommit=True) as c:
  def q(query,args=()):
   cursor=c.execute(query,args);return cursor.fetchone()[0] if cursor.description else None
  q('create extension if not exists pgcrypto with schema extensions')
  cap=q("select pg_get_constraintdef(oid) from pg_constraint where conrelid='private.general_budget'::regclass and conname='general_budget_credit_limit_check'")
  if '64' not in cap:q((R/'service/supabase/migrations/202610220032_general_trial_credits.sql').read_text())
  q((R/'service/supabase/migrations/202610230033_understand_then_prepare.sql').read_text())
  q((R/'service/supabase/migrations/202610230034_known_page_trial_guard.sql').read_text())
  before=q("select to_jsonb(general_budget) from private.general_budget");q((R/'service/supabase/migrations/202610230035_v1_source_policy.sql').read_text());assert q("select to_jsonb(general_budget) from private.general_budget")==before;report['migrationKeepsBudgetCountersAndLimits']=True
  # Migration itself preserves every runtime counter/limit and existing watch.
  before=q('select to_jsonb(general_budget) from private.general_budget')
  cap_before=q('select to_jsonb(capacity) from private.capacity')
  watches_before=q('select coalesce(jsonb_agg(to_jsonb(w) order by id),\'[]\'::jsonb) from private.watches w')
  q((R/'service/supabase/migrations/202610240036_test_capacity.sql').read_text())
  assert q('select to_jsonb(general_budget) from private.general_budget')==before
  cap_after=q('select to_jsonb(capacity) from private.capacity');assert cap_after.pop('active_per_account')==1 and cap_after==cap_before
  assert q('select coalesce(jsonb_agg(to_jsonb(w) order by id),\'[]\'::jsonb) from private.watches w')==watches_before
  report['migrationPreservesRuntimeBudgetAndWatches']=True
  q('truncate private.accounts,private.previews cascade');q('truncate private.general_attempts')
  q('update private.capacity set enabled=true,slots=4,active_per_account=3,reserved=0,budget_units=100,used_units=0,reserved_units=0')
  q('update private.general_budget set enabled=true,qualified=true,model_limit=100000,model_used=21052,model_held=0,credit_limit=100,credit_used=45,credit_held=0,qualification_preview_hash=null')
  q('update private.admission_capacity set enabled=false');q('update private.page_change_pilot set enabled=false');q("update private.method_qualifications set qualified=(method in ('web_search','known_page'))")
  owner=str(uuid.uuid4());other=str(uuid.uuid4());token='synthetic-test-token-with-more-than-32-characters'
  for u in (owner,other):
   q('insert into auth.users(id,email) values(%s,%s)',(u,'capacity-'+u+'@example.test'));q('insert into private.accounts(id) values(%s)',(u,))
  def claim(u):q("select set_config('request.jwt.claims',%s,false)",(json.dumps({'sub':u,'role':'authenticated','iat':int(time.time()),'amr':[{'method':'otp','timestamp':int(time.time())}]}),))
  claim(owner)
  token=q("select public.s3_preview('session')")['token']
  draft={'text':'Promotions PS5 en France','language':'fr','mode':'continuous','revision':1,'answers':[]}
  intent={'status':'understood','title':'Promotions PS5','criteria':[{'id':'a','text':'Promotions PS5','quote':'Promotions PS5'}],'queries':['PS5 France promotions'],'questions':[],'explanation':'Public sources.'}
  def proposal(store=True):
   p={'id':str(uuid.uuid4()),'version':1,'draft':draft,'title':'PS5','criteria':['Promotions PS5'],'exclusions':[],'coverage':'Public','partial':True,'sources':[],'method':'web_search','backgroundPreparation':True,'intervalHours':1,'sourcePolicy':'stable_v1','limitations':[],'limits':q('select private.limits()'),'expiresAt':(dt.datetime.now(dt.timezone.utc)+dt.timedelta(minutes=30)).isoformat()}
   if store:
    q("insert into private.proposals(id,token_hash,version,payload,expires_at) values(%s,encode(extensions.digest(%s,'sha256'),'hex'),1,%s,now()+interval '30 minutes')",(p['id'],token,Jsonb(p)));q('insert into private.general_plans values(%s,%s)',(p['id'],Jsonb(intent)))
   return p
  def activate(p):return {'id':v7(),'operation':'activate','proposalId':p['id'],'proposalVersion':1,'proposalToken':token,'acceptPartial':True}
  def execute(b):return q('select public.s3_command(%s)',(Jsonb(b),))
  def denied(b,code='quota_exceeded'):
   try:execute(b)
   except psycopg.Error as e:assert code in str(e);return
   raise AssertionError('Request unexpectedly allowed')
  assert q('select private.limits()')['activePerAccount']==3
  eid=v7();q("select public.s3_preview('understand',%s,%s)",(token,Jsonb({'id':eid,'draft':draft,'previousEvaluationId':None})))
  work=q('select public.s4_understanding_lease(%s,%s)',(eid,token));understood=proposal(False);understood['limits']['activePerAccount']=1
  q('select public.s3_general_finish(%s,%s,%s,0,0)',(work['id'],work['lease'],Jsonb({'intent':intent,'proposal':understood})))
  ready=q("select public.s3_preview('evaluation',%s,null,%s)",(token,eid))
  assert ready['proposal']['limits']['activePerAccount']==3
  report['newProposalPublishesEffectivePolicy']=True
  commands=[activate(proposal()) for _ in range(4)]
  receipts=[execute(b) for b in commands[:3]]
  assert all(r['state']=='pending' for r in receipts) and q('select reserved from private.capacity')==3
  denied(commands[3]);assert q('select reserved from private.capacity')==3
  assert execute(commands[0])==receipts[0] and q('select reserved from private.capacity')==3
  report['threePerAccountAndIdempotentReplay']=True
  claim(other);execute(activate(proposal()));denied(activate(proposal()));assert q('select reserved from private.capacity')==4
  report['globalCapacityEnforcedAcrossAccounts']=True
  claim(owner);wid=receipts[0]['watchId'];pause={'id':v7(),'operation':'pause','watchId':wid,'expectedVersion':1}
  r=execute(pause);assert r['state']=='pending'
  j=q("select to_jsonb(j) from private.jobs j where watch_id=%s and kind='stop'",(wid,));lease=str(uuid.uuid4())
  q("update private.jobs set state='leased',lease_token=%s,lease_until=now()+interval '2 minutes' where id=%s",(lease,j['id']))
  q('select public.s3_confirm_stop(%s,%s)',(j['id'],lease))
  assert q('select reserved from private.capacity')==3
  assert execute(pause)['state']=='succeeded' and q('select reserved from private.capacity')==3
  # Once paused, resume respects both the account and global policy.
  execute(commands[3]);assert q('select reserved from private.capacity')==4
  q('update private.capacity set slots=5')
  denied({'id':v7(),'operation':'resume','watchId':wid,'expectedVersion':2})
  assert q("select status from private.watches where id=%s",(wid,))=='paused'
  report['pauseFreesExactlyOneSlotAndResumeCannotBypassLimit']=True
  q('update private.general_budget set credit_limit=credit_used+credit_held')
  denied(activate(proposal()));assert q('select model_used from private.general_budget')==21052 and q('select credit_used from private.general_budget')==45
  q('update private.general_budget set credit_limit=100,model_limit=model_used+model_held')
  denied(activate(proposal()));report['spentCountersAndPreflightCapsPreserved']=True
  assert not q("select has_function_privilege('authenticated','public.s3_command_transaction(jsonb)','execute')")
  # The DB ceiling is bounded; no accidental unbounded budget extension.
  rejected=False
  try:q('update private.general_budget set credit_limit=101')
  except psycopg.Error:rejected=True
  assert rejected
  report['internalRpcAndStructuralCeilingPreserved']=True
  report['passed']=True
except Exception as e:
 report['failure']=type(e).__name__;raise
finally:
 if created:run([D,'exec',C,'dropdb','-U','postgres',name])
 out=R/'validation/test-capacity';out.mkdir(parents=True,exist_ok=True);(out/'local-database.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
