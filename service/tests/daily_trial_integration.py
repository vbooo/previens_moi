"""Disposable local DB, synthetic identities and injected provider results only."""
from pathlib import Path
import concurrent.futures,datetime as dt,json,subprocess,time,uuid
import psycopg
from psycopg.types.json import Jsonb
R=Path(__file__).resolve().parents[2]
D='/Applications/Docker.app/Contents/Resources/bin/docker';C='supabase_db_previens-moi-s3';BASE='postgresql://postgres:postgres@127.0.0.1:56322/'
name='daily_trial_'+uuid.uuid4().hex[:10];created=False
report={'passed':False,'paidCalls':0,'activeDatabaseModified':False,'scope':'Disposable local database; synthetic users and injected model/source responses.'}
def run(args,**kw):return subprocess.run(args,check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kw)
def v7():return f'{int(time.time()*1000):012x}'[:8]+'-'+f'{int(time.time()*1000):012x}'[8:]+'-7'+uuid.uuid4().hex[:3]+'-8'+uuid.uuid4().hex[:3]+'-'+uuid.uuid4().hex[:12]
def proposal():
 return {'id':str(uuid.uuid4()),'version':1,'draft':{'text':'Préviens-moi des concerts de Mira à Tours.','language':'fr','mode':'continuous','revision':1,'answers':[{'questionId':'region','value':'France'}]},'title':'Mira à Tours','criteria':['Concert de Mira à Tours'],'exclusions':[],'coverage':'Sources après confirmation.','partial':True,'sources':[],'method':'web_search','backgroundPreparation':True,'sourcePolicy':'stable_v1','trialDays':7,'intervalHours':24,'limitations':['Sources retenues.'],'limits':{'activePerAccount':1,'globalAvailable':1,'previewRemaining':0,'resetsAt':None},'expiresAt':(dt.datetime.now(dt.timezone.utc)+dt.timedelta(minutes=30)).isoformat()}
intent={'status':'understood','title':'Mira à Tours','criteria':[{'id':'a','text':'Concert de Mira à Tours','quote':'concerts de Mira à Tours'}],'queries':['Mira Tours concerts'],'questions':[],'explanation':'Sources publiques.'}
docs=[{'url':'https://source.example/'+str(n),'label':'Mira','text':'Programme de concerts à Tours.','publication':None,'changedText':None,'retrieval':'search_scrape'} for n in range(3)]
try:
 with psycopg.connect(BASE+'postgres') as source:
  assert source.execute("select count(*) from auth.users where email not like '%%.test' and email is not null").fetchone()[0]==0,'Refuse copying non-test identities'
 raw=run([D,'exec',C,'pg_dump','-U','postgres','-d','postgres','--format=custom','--schema=auth','--schema=private','--schema=public','--schema=extensions','--no-owner','--no-privileges']).stdout
 run([D,'exec',C,'createdb','-U','postgres',name]);created=True
 run([D,'exec',C,'psql','-U','postgres','-d',name,'-c','drop schema public cascade'])
 run([D,'exec','-i',C,'pg_restore','-U','postgres','-d',name,'--no-owner','--no-privileges','--exit-on-error'],input=raw)
 with psycopg.connect(BASE+name,autocommit=True) as c:
  def q(sql,args=()):
   cur=c.execute(sql,args or None);return cur.fetchone()[0] if cur.description else None
  q('create extension if not exists pgcrypto with schema extensions')
  # Accept both the historical qualification template and a current local stack.
  # Only this disposable copy receives missing migrations.
  ceiling=q("select pg_get_constraintdef(oid) from pg_constraint where conrelid='private.general_budget'::regclass and conname='general_budget_credit_limit_check'")
  if q("select to_regclass('private.trial_policy') is not null"):version=202611150038
  elif '150' in ceiling:version=202611030037
  elif q("select exists(select 1 from information_schema.columns where table_schema='private' and table_name='capacity' and column_name='active_per_account')"):version=202610240036
  elif q("select to_regprocedure('public.s5_proposal_options(uuid,text,bigint,text)') is not null"):version=202610230035
  elif q("select to_regprocedure('public.s3_command_before_workflow(jsonb)') is not null"):version=202610230034
  elif q("select to_regprocedure('public.s4_understanding_lease(uuid,text)') is not null"):version=202610230033
  elif '64' in ceiling:version=202610220032
  else:version=202610220031
  applied=[]
  for file in sorted((R/'service/supabase/migrations').glob('*.sql')):
   if int(file.name[:12])>version:q(file.read_text());applied.append(file.name[:12])
  report['migrationsAppliedInDisposableDb']=applied
  report['migrationOnHistoricalLocalSchema']=True
  q('truncate private.accounts,private.previews cascade');q('truncate private.general_attempts')
  q('update private.trial_policy set enabled=true')
  q('update private.capacity set enabled=true,slots=3,reserved=0,active_per_account=1')
  q('update private.general_budget set enabled=true,qualified=true,model_limit=300000,model_used=0,model_held=0,model_committed=0,credit_limit=200,credit_used=0,credit_held=0,credit_committed=0,qualification_preview_hash=null')
  q('update private.admission_capacity set enabled=false');q('update private.page_change_pilot set enabled=true')
  q("update private.method_qualifications set qualified=(method in ('web_search','known_page'))")
  def owner():
   u=str(uuid.uuid4());q('insert into auth.users(id,email) values(%s,%s)',(u,'trial-'+u+'@example.test'));q('insert into private.accounts(id) values(%s)',(u,));return u
  def claims(u,conn=c):conn.execute("select set_config('request.jwt.claims',%s,false)",(json.dumps({'sub':u,'role':'authenticated','iat':int(time.time()),'amr':[{'method':'otp','timestamp':int(time.time())}]}),))
  def stage(u,p=None):
   p=p or proposal();token=q("select public.s3_preview('session')")['token'];q("insert into private.proposals(id,token_hash,version,payload,expires_at) values(%s,encode(extensions.digest(%s,'sha256'),'hex'),1,%s,now()+interval '30 minutes')",(p['id'],token,Jsonb(p)));q('insert into private.general_plans values(%s,%s)',(p['id'],Jsonb(intent)));return p,{'id':v7(),'operation':'activate','proposalId':p['id'],'proposalVersion':1,'proposalToken':token,'acceptPartial':True}
  def lease():
   x=q('select public.s3_general_lease()');assert x;return x
  def finish(x,result,actual=200,credits=0):q('select public.s3_general_finish(%s,%s,%s,%s,%s)',(x['id'],x['lease'],Jsonb(result),actual,credits))
  u=owner();claims(u)
  p=proposal();token=q("select public.s3_preview('session')")['token'];eid=v7()
  r=q("select public.s3_preview('understand',%s,%s)",(token,Jsonb({'id':eid,'draft':p['draft'],'previousEvaluationId':None})));assert r['status']=='pending'
  x=q('select public.s4_understanding_lease(%s,%s)',(eid,token));assert x['context']['dailyTrial'] is True
  finish(x,{'intent':intent,'proposal':p});r=q("select public.s3_preview('evaluation',%s,null,%s)",(token,eid));assert r['proposal']['intervalHours']==24 and r['proposal']['trialDays']==7
  body={'id':v7(),'operation':'activate','proposalId':p['id'],'proposalVersion':1,'proposalToken':token,'acceptPartial':True}
  rc=q('select public.s3_command(%s)',(Jsonb(body),));wid=rc['watchId'];t=q('select to_jsonb(t) from private.account_trials t where owner_id=%s',(u,));assert q('select public.s3_command(%s)',(Jsonb(body),))==rc;assert q('select to_jsonb(t) from private.account_trials t where owner_id=%s',(u,))==t
  assert q('select model_committed from private.general_budget')==180000 and q('select credit_committed from private.general_budget')==30
  report['sevenDayAccountGrantAndReplayWithoutReset']=True
  p2,cmd2=stage(u)
  try:q('select public.s3_command(%s)',(Jsonb(cmd2),));raise AssertionError('Second active watch accepted')
  except psycopg.Error as e:assert e.diag.message_primary=='active_watch_limit'
  report['oneActiveWatchPerAccount']=True
  u2=owner();claims(u2);p2,cmd2=stage(u2)
  try:q('select public.s3_command(%s)',(Jsonb(cmd2),));raise AssertionError('Unfunded trial accepted')
  except psycopg.Error as e:assert e.diag.message_primary=='quota_exceeded'
  assert q('select count(*) from private.account_trials')==1;report['fullFutureAllowanceProtectedFromOtherAccounts']=True;claims(u)
  # A global pool exhausted by commitments rejects anonymous analysis but still
  # lets the already funded watch run through its normal phases.
  q('update private.general_budget set model_limit=model_used+model_held+model_committed')
  anonymous=q("select public.s3_preview('session')")['token'];anonymous_id=v7()
  q("select public.s3_preview('understand',%s,%s)",(anonymous,Jsonb({'id':anonymous_id,'draft':p['draft'],'previousEvaluationId':None})))
  attempt_count=q('select count(*) from private.general_attempts')
  assert q('select public.s4_understanding_lease(%s,%s)',(anonymous_id,anonymous)) is None
  assert q('select count(*) from private.general_attempts')==attempt_count
  assert q("select public.s3_preview('evaluation',%s,null,%s)",(anonymous,anonymous_id))['error']['code']=='quota_exceeded'
  report['anonymousAnalysisCannotSpendFundedTrialAllowance']=True
  x=lease();assert x['phase']=='collect' and q('select credit_committed from private.general_budget')==25
  finish(x,{'documents':docs,'retrievalHealthy':True,'checkCoverage':{'verified':3,'total':3}},0,5)
  x=lease();prepared=dict(p,sources=[{'url':d['url'],'label':d['label']} for d in docs]);finish(x,{'coverage':{'status':'observable'},'preparedPlan':prepared,'documents':docs})
  x=lease();assert x['phase']=='baseline';at=dt.datetime.now(dt.timezone.utc).isoformat();memory={'generation':x['generation'],'baselineAt':at,'observedAt':at,'completed':False,'facts':[]}
  result={'decision':{'outcome':'baseline','memory':memory,'findings':[],'issues':[]},'findings':[],'documents':docs,'observedAt':at};finish(x,result);finish(x,result)
  assert q('select extract(epoch from next_check_at-last_success_at)::int from private.watches where id=%s',(wid,))==86400
  assert q('select model_used from private.account_trials where owner_id=%s',(u,))==400
  assert q('select credit_used from private.account_trials where owner_id=%s',(u,))==5
  report['dailyScheduleAndAccountCostSettlementExactlyOnce']=True
  q('update private.general_budget set model_limit=300000')
  before=q('select count(*) from private.general_attempts');end=q('select ends_at from private.account_trials where owner_id=%s',(u,));due=q('select next_check_at from private.watches where id=%s',(wid,))
  q('select public.s3_command(%s)',(Jsonb({'id':v7(),'operation':'pause','watchId':wid,'expectedVersion':1}),));stop=q('select public.s3_stop_lease()');assert stop;q('select public.s3_confirm_stop(%s,%s)',(stop['id'],stop['lease_token']))
  rc=q('select public.s3_command(%s)',(Jsonb({'id':v7(),'operation':'resume','watchId':wid,'expectedVersion':2}),));assert rc['state']=='succeeded'
  assert q('select count(*) from private.general_attempts')==before and q('select next_check_at from private.watches where id=%s',(wid,))==due
  assert q('select ends_at from private.account_trials where owner_id=%s',(u,))==end;report['pauseResumeNoPaidPreparationAndNoClockReset']=True
  # An in-flight model answer after the deadline settles once but cannot publish.
  q("update private.watches set next_check_at=now()-interval '1 second' where id=%s",(wid,));x=lease();finish(x,{'documents':docs,'retrievalHealthy':True,'checkCoverage':{'verified':3,'total':3}},0,3);x=lease()
  q("update private.account_trials set started_at=now()-interval '8 days',ends_at=now()-interval '1 day' where owner_id=%s",(u,))
  # Inject an older retained result and a new positive but late answer.
  prior=str(uuid.uuid4());late=str(uuid.uuid4());observed=dt.datetime.now(dt.timezone.utc).isoformat()
  finding={'id':prior,'watchId':wid,'planId':p['id'],'planVersion':1,'title':'Earlier result','fact':'Prior controlled result','reason':'Reference','criteriaSnapshot':p['criteria'],'evidence':[{'url':docs[0]['url'],'quote':docs[0]['text'],'observedAt':observed,'publication':None}],'observedAt':observed,'event':None,'noveltyBasis':'baseline_change','correctsFindingId':None}
  q('insert into private.findings(id,watch_id,owner_id,generation,fact_key,payload) values(%s,%s,%s,%s,%s,%s)',(prior,wid,u,x['generation'],'b'*64,Jsonb(finding)))
  late_finding=dict(finding,id=late,title='Late result')
  answer={'decision':{'outcome':'finding','memory':dict(memory,generation=x['generation'],observedAt=observed),'findings':[],'issues':[]},'findings':[{'key':'a'*64,'finding':late_finding}],'documents':docs,'observedAt':observed}
  old_model=q('select model_used from private.general_budget');finish(x,answer);finish(x,answer)
  assert q('select model_used from private.general_budget')==old_model+200
  w=q("select public.s3_read('watch',%s)",(wid,));assert w['trialEnded'] is True and w['status']=='suspended_by_capacity'
  assert q('select model_committed from private.general_budget')==0 and q('select credit_committed from private.general_budget')==0
  assert q('select count(*) from private.findings where id=%s',(late,))==0
  assert q("select public.s3_read('finding',%s)",(prior,))['id']==prior
  report['expiryFencesLateWorkAndReleasesOnlyUnusedCommitment']=True
  for body in [dict(cmd2,id=v7()),{'id':v7(),'operation':'resume','watchId':wid,'expectedVersion':w['version']}]:
   if body['operation']=='activate':
    _,body=stage(u)
   try:q('select public.s3_command(%s)',(Jsonb(body),));raise AssertionError('Expired account renewed')
   except psycopg.Error as e:assert e.diag.message_primary=='free_trial_ended'
  report['newWatchAndResumeCannotRenewExpiredTrial']=True
  try:q('select public.s6_trial_access()');raise AssertionError('Expired analysis allowed')
  except psycopg.Error as e:assert e.diag.message_primary=='free_trial_ended'
  report['expiredSignedInAccountRejectedBeforeInterpretation']=True
  # Unknown source charge stays held in both ledgers, including after erasure.
  claims(u2);p2,cmd2=stage(u2);rw=q('select public.s3_command(%s)',(Jsonb(cmd2),));x=lease();finish(x,{'failed':True},0,None)
  assert q('select credit_held from private.account_trials where owner_id=%s',(u2,))==5
  assert q('select credit_held from private.general_budget')==5
  held=q('select credit_held from private.general_budget');q('update private.accounts set deleted_at=now() where id=%s',(u2,))
  assert q('select credit_held from private.general_budget')==held and q('select credit_committed from private.general_budget')==0
  assert q('select count(*) from private.account_trials where owner_id=%s',(u2,))==0
  report['unknownChargeSurvivesAccountLedgerErasure']=True
  assert not q("select has_function_privilege('authenticated','private.reserve_trial_phase(uuid,bigint,integer)','execute')")
  assert not q("select has_table_privilege('authenticated','private.account_trials','select')");report['internalBudgetNotExposed']=True
  # Per-account credit exhaustion stops before the next collection is reserved.
  u3=owner();claims(u3);p3,cmd3=stage(u3);rw3=q('select public.s3_command(%s)',(Jsonb(cmd3),))
  q('update private.general_budget set credit_committed=credit_committed-29,credit_used=credit_used+29')
  q('update private.account_trials set credit_used=29 where owner_id=%s',(u3,))
  attempts=q('select count(*) from private.general_attempts');assert q('select public.s3_general_lease()') is None
  assert q('select count(*) from private.general_attempts')==attempts
  assert q('select end_reason from private.account_trials where owner_id=%s',(u3,))=='quota'
  assert q("select public.s3_read('watch',%s)",(rw3['watchId'],))['trialEnded'] is True
  report['thirtyCreditAccountCapRejectsBeforeProviderWork']=True
  # Two accounts compete for one globally fundable grant: exactly one wins.
  q('truncate private.accounts,private.previews cascade')
  users=[owner(),owner()];commands=[stage(user)[1] for user in users]
  def compete(index):
   with psycopg.connect(BASE+name,autocommit=True) as other:
    claims(users[index],other)
    try:other.execute('select public.s3_command(%s)',(Jsonb(commands[index]),)).fetchone();return 'accepted'
    except psycopg.Error as e:return e.diag.message_primary
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:outcomes=list(pool.map(compete,range(2)))
  assert sorted(outcomes)==['accepted','quota_exceeded'],outcomes
  assert q('select count(*) from private.account_trials')==1;report['concurrentAdmissionsCannotOverbookGlobalBudget']=True
  # A pre-existing hourly plan is neither rewritten nor expired by a new trial.
  accepted=users[outcomes.index('accepted')];legacy=proposal();legacy.pop('trialDays');legacy['intervalHours']=1
  legacy_id=str(uuid.uuid4());q("insert into private.watches(id,owner_id,plan,status,remote_state,next_check_at) values(%s,%s,%s,'active','running',now()+interval '1 hour')",(legacy_id,accepted,Jsonb(legacy)))
  q('update private.capacity set reserved=reserved+1')
  q("update private.account_trials set started_at=now()-interval '8 days',ends_at=now()-interval '1 day' where owner_id=%s",(accepted,));q('select private.expire_trials()')
  legacy_view=q('select private.watch_json(w) from private.watches w where id=%s',(legacy_id,))
  assert legacy_view['status']=='active' and legacy_view['plan']['intervalHours']==1 and 'trialEndsAt' not in legacy_view
  report['legacyCadenceAndHistoryPreserved']=True
  report['passed']=True
except Exception as e:
 report['failure']=type(e).__name__;raise
finally:
 if created:run([D,'exec',C,'dropdb','-U','postgres',name])
 out=R/'validation/daily-trial';out.mkdir(parents=True,exist_ok=True);(out/'local-database.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
