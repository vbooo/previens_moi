"""Disposable local DB, synthetic identities and injected provider results only."""
from pathlib import Path
import concurrent.futures,datetime as dt,json,subprocess,time,uuid
import psycopg
from psycopg.types.json import Jsonb
R=Path(__file__).resolve().parents[2]
D='/Applications/Docker.app/Contents/Resources/bin/docker';C='supabase_db_previens-moi-s3';BASE='postgresql://postgres:postgres@127.0.0.1:56322/'
name='play_billing_'+uuid.uuid4().hex[:10];created=False
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
  assert q('select not enabled and test_only and not monitoring_enabled from private.billing_policy where id')
  initial=q('select jsonb_build_array(model_limit,credit_limit) from private.general_budget where id')
  q('truncate private.accounts,private.previews cascade');q('truncate private.general_attempts')
  q('update private.billing_policy set enabled=true,monitoring_enabled=true')
  q('update private.capacity set enabled=true,slots=10,reserved=0,active_per_account=1')
  q('update private.general_budget set enabled=true,qualified=true,model_limit=300000,model_used=0,model_held=0,model_committed=0,credit_limit=200,credit_used=0,credit_held=0,credit_committed=0,qualification_preview_hash=null')
  q('update private.admission_capacity set enabled=false')
  q("update private.method_qualifications set qualified=(method in ('web_search','known_page'))")
  def owner():
   u=str(uuid.uuid4());q('insert into auth.users(id,email) values(%s,%s)',(u,'billing-'+u+'@example.test'));q('insert into private.accounts(id) values(%s)',(u,));return u
  def claims(u):q("select set_config('request.jwt.claims',%s,false)",(json.dumps({'sub':u,'role':'authenticated','iat':int(time.time()),'amr':[{'method':'otp','timestamp':int(time.time())}]}),))
  def fail(sql,args,code):
   try:q(sql,args);raise AssertionError('Expected '+code)
   except psycopg.Error as e:assert e.diag.message_primary==code,(e.diag.message_primary,code)
  def proof(u,state='active',order='test-order-1'):
   return {'state':state,'active':state in ('active','cancelled','grace'),'expiresAt':(dt.datetime.now(dt.timezone.utc)+dt.timedelta(days=30)).isoformat(),'orderId':order,'accountId':q("select encode(extensions.digest('previens-moi:play:'||%s,'sha256'),'hex')",(u,)),'acknowledged':False,'test':True,'linkedToken':None}
  def bind(u,token,p=None,at=None):q('select public.s7_bind_purchase(%s,%s,%s,%s)',(u,token,Jsonb(p or proof(u)),at or dt.datetime.now(dt.timezone.utc)))
  def stage(u):
   p=proposal();token=q("select public.s3_preview('session')")['token'];q("insert into private.proposals(id,token_hash,version,payload,expires_at) values(%s,encode(extensions.digest(%s,'sha256'),'hex'),1,%s,now()+interval '30 minutes')",(p['id'],token,Jsonb(p)));q('insert into private.general_plans values(%s,%s)',(p['id'],Jsonb(intent)));return p,{'id':v7(),'operation':'activate','proposalId':p['id'],'proposalVersion':1,'proposalToken':token,'acceptPartial':True}
  def command(w,op):
   version=q('select version from private.watches where id=%s',(w,));return q('select public.s3_command(%s)',(Jsonb({'id':v7(),'operation':op,'watchId':w,'expectedVersion':version}),))
  def pause(w):
   command(w,'pause');stop=q('select public.s3_stop_lease()');assert stop;q('select public.s3_confirm_stop(%s,%s)',(stop['id'],stop['lease_token']))
  def finish(x,result,actual=200,credits=0):q('select public.s3_general_finish(%s,%s,%s,%s,%s)',(x['id'],x['lease'],Jsonb(result),actual,credits))
  u=owner();other=owner();claims(u);token='synthetic-purchase-token-0001'
  pending=proof(u,'pending');pending.update(active=False,expiresAt=None,orderId=None)
  bind(u,token,pending);assert not q('select public.s7_subscription()')['active']
  mismatch=proof(other)
  fail('select public.s7_bind_purchase(%s,%s,%s,now())',(other,token,Jsonb(mismatch)),'purchase_account_mismatch')
  real=proof(u);real['test']=False
  fail('select public.s7_bind_purchase(%s,%s,%s,now())',(u,token,Jsonb(real)),'billing_unavailable')
  bind(u,token);access=q('select public.s7_subscription()');assert access['active'] and access['remainingCreations']==5
  report['pendingNeverGrantsAndTestOnlyRejectsRealPayments']=True;report['purchaseCannotMoveBetweenAccounts']=True
  p,cmd=stage(u);prepared=q('select public.s7_prepare_proposal(%s,%s)',(p['id'],cmd['proposalToken']));assert prepared['accessTier']=='plus' and prepared['intervalHours']==24 and prepared['limits']['activePerAccount']==2 and 'trialDays' not in prepared
  first=q('select public.s3_command(%s)',(Jsonb(cmd),));w1=first['watchId'];assert q('select public.s3_command(%s)',(Jsonb(cmd),))==first
  assert q('select public.s7_subscription()')['remainingCreations']==4
  second=q('select public.s3_command(%s)',(Jsonb(stage(u)[1]),));w2=second['watchId']
  third=stage(u)[1];fail('select public.s3_command(%s)',(Jsonb(third),),'active_watch_limit')
  assert q('select public.s7_subscription()')['remainingCreations']==3
  report['twoActiveWatchesAndIdempotentCreationCharge']=True
  # Prepare a genuine retained baseline through the worker state machine.
  for _ in range(6):
   x=q('select public.s3_general_lease()');assert x, q("select jsonb_agg(to_jsonb(w)) from private.general_work w")
   if x['phase']=='collect':finish(x,{'documents':docs,'retrievalHealthy':True,'checkCoverage':{'verified':3,'total':3}},0,5)
   elif x['phase']=='decide':
    plan=q('select plan from private.watches where id=%s',(x['watchId'],));plan['sources']=[{'url':d['url'],'label':d['label']} for d in docs];finish(x,{'coverage':{'status':'observable'},'preparedPlan':plan,'documents':docs})
   else:
    at=dt.datetime.now(dt.timezone.utc).isoformat();memory={'generation':x['generation'],'baselineAt':at,'observedAt':at,'completed':False,'facts':[]};finish(x,{'decision':{'outcome':'baseline','memory':memory,'findings':[],'issues':[]},'findings':[],'documents':docs,'observedAt':at})
  assert q("select count(*) from private.watches where status='active'")==2
  assert q('select model_used from private.billing_periods where owner_id=%s',(u,))==800
  assert q('select credit_used from private.billing_periods where owner_id=%s',(u,))==10
  assert q('select model_held+credit_held from private.billing_periods where owner_id=%s',(u,))==0
  assert q('select extract(epoch from next_check_at-last_success_at)::int from private.watches where id=%s',(w1,))==86400
  used=q('select count(*) from private.general_attempts');pause(w1);due=q('select next_check_at from private.watches where id=%s',(w1,));resumed=command(w1,'resume');assert resumed['state']=='succeeded'
  assert q('select count(*) from private.general_attempts')==used and q('select public.s7_subscription()')['remainingCreations']==3
  report['dailyCadenceSettlementAndResumeWithoutPaidDiscovery']=True
  # Repeated renewal snapshots never reset counters; a new paid order does.
  bind(u,token);assert q('select public.s7_subscription()')['remainingCreations']==3
  stale=proof(u,'expired');stale['active']=False;bind(u,token,stale,dt.datetime.now(dt.timezone.utc)-dt.timedelta(minutes=10));assert q('select public.s7_subscription()')['active']
  report['RepeatedAndOutOfOrderProofsDoNotResetOrRevokeAccess']=True
  pause(w1)
  for _ in range(3):
   cmd=stage(u)[1];rc=q('select public.s3_command(%s)',(Jsonb(cmd),));pause(rc['watchId'])
  assert q('select public.s7_subscription()')['remainingCreations']==0
  fail('select public.s3_command(%s)',(Jsonb(stage(u)[1]),),'monthly_creation_limit')
  command(w1,'resume');assert q('select public.s7_subscription()')['remainingCreations']==0
  report['FiveCreationsPerPeriodWithFreePauseAndResume']=True
  bind(u,token,proof(u,order='test-order-2'));assert q('select public.s7_subscription()')['remainingCreations']==5
  assert q('select count(*) from private.billing_periods where owner_id=%s',(u,))==2
  report['RenewalCreatesNewQuotaExactlyOnce']=True
  # A voluntary pause stays paused; a billing suspension can recover in place.
  pause(w1)
  grace=proof(u,'grace',order='test-order-2');bind(u,token,grace)
  assert q('select public.s7_subscription()')['status']=='grace' and q('select public.s7_subscription()')['active']
  hold=proof(u,'hold',order='test-order-2');hold['expiresAt']=(dt.datetime.now(dt.timezone.utc)-dt.timedelta(minutes=1)).isoformat()
  bind(u,token,hold)
  assert q('select public.s7_subscription()')['status']=='hold' and not q('select public.s7_subscription()')['active']
  historical=proof(u,'expired',order='historical-order');historical.update(active=False,expiresAt=(dt.datetime.now(dt.timezone.utc)-dt.timedelta(days=1)).isoformat())
  bind(u,'synthetic-historical-expired-token',historical)
  assert q('select public.s7_subscription()')['status']=='hold'
  q("update private.billing_purchases set refresh_attempt_at=now()-interval '2 minutes' where owner_id=%s",(u,))
  assert q('select public.s7_refresh_candidates(%s)',(u,))[0]['token']==token
  report['GraceRetainsAccessHoldPreservesRecoveryDespiteExpiredHistory']=True
  assert q("select status='suspended_by_capacity' from private.watches where id=%s",(w2,))
  assert q("select public.s3_read('watch',%s)",(w2,))['subscriptionEnded']
  fail('select public.s3_command(%s)',(Jsonb({'id':v7(),'operation':'resume','watchId':w2,'expectedVersion':q('select version from private.watches where id=%s',(w2,))}),),'subscription_required')
  bind(u,token,proof(u,order='test-order-2'))
  assert q('select status from private.watches where id=%s',(w2,))=='active'
  assert q('select status from private.watches where id=%s',(w1,))=='suspended_by_capacity'
  report['HoldStopsChecksAndRecoveryHonorsVoluntaryPause']=True
  bind(u,token,proof(u,'cancelled',order='test-order-2'));assert q('select public.s7_subscription()')['active']
  expired=proof(u,'expired',order='test-order-2');expired.update(active=False,expiresAt=(dt.datetime.now(dt.timezone.utc)-dt.timedelta(seconds=1)).isoformat());bind(u,token,expired)
  assert not q('select public.s7_subscription()')['active'];assert q('select status from private.watches where id=%s',(w2,))=='suspended_by_capacity'
  report['CancellationRetainsAccessUntilExpiry']=True
  bind(u,token,proof(u,order='test-order-2'))
  eid=v7();q('select public.s7_analysis_access(%s)',(eid,));q('select public.s7_analysis_access(%s)',(eid,));assert q("select analyses from private.billing_periods where period_key=encode(extensions.digest(encode(extensions.digest(%s,'sha256'),'hex')||':test-order-2','sha256'),'hex')",(token,))==1
  for _ in range(24):q('select public.s7_analysis_access(%s)',(v7(),))
  fail('select public.s7_analysis_access(%s)',(v7(),),'monthly_analysis_limit');report['RepeatedUnderstandingDoesNotDoubleChargeAllowance']=True
  for function in ['public.s7_bind_purchase(uuid,text,jsonb,timestamptz)','public.s7_acknowledged(text)','public.s7_refresh_candidates(uuid)','public.s7_purchase_owner(text)']:
   assert not q("select has_function_privilege('authenticated',%s,'execute')",(function,));assert not q("select has_function_privilege('anon',%s,'execute')",(function,))
  assert not q("select has_table_privilege('authenticated','private.billing_purchases','select')")
  q('update private.accounts set deleted_at=now() where id=%s',(u,));assert q('select count(*) from private.billing_purchases where owner_id=%s',(u,))==0
  assert q('select count(*) from private.billing_owners where owner_id is null')==2
  fail('select public.s7_bind_purchase(%s,%s,%s,now())',(other,token,Jsonb(proof(other))),'purchase_account_mismatch')
  report['AccountDeletionErasesTokensWithoutAllowingTheirReuse']=True;report['PurchaseSecretsAndInternalFunctionsAreNotClientAccessible']=True
  assert q('select jsonb_build_array(model_limit,credit_limit) from private.general_budget where id')==[300000,200]
  report['GlobalProviderCeilingsNotRaised']=True
  # A signed-in paid user can prepare a proposal, but another account cannot
  # borrow its paid tier by reusing the unclaimed preview after switching login.
  paid=owner();claims(paid);bind(paid,'synthetic-transition-purchase-0001')
  p,cmd=stage(paid);q('select public.s7_prepare_proposal(%s,%s)',(p['id'],cmd['proposalToken']))
  claims(other);fail('select public.s3_command(%s)',(Jsonb(cmd),),'subscription_required')
  report['PaidProposalCannotBeActivatedByANonPayingAccount']=True
  # Upgrade a real pending trial between its coverage lease and its answer.
  q('truncate private.accounts,private.previews cascade');q('truncate private.general_attempts')
  q('update private.capacity set reserved=0')
  q('update private.general_budget set model_used=0,model_held=0,model_committed=0,credit_used=0,credit_held=0,credit_committed=0')
  q('update private.trial_policy set enabled=true')
  upgrading=owner();claims(upgrading);trial,cmd=stage(upgrading)
  wid=q('select public.s3_command(%s)',(Jsonb(cmd),))['watchId']
  x=q('select public.s3_general_lease()');finish(x,{'documents':docs,'retrievalHealthy':True,'checkCoverage':{'verified':3,'total':3}},0,5)
  coverage=q('select public.s3_general_lease()');assert coverage['phase']=='decide'
  bind(upgrading,'synthetic-transition-purchase-0002')
  assert q('select plan from private.watches where id=%s',(wid,))['accessTier']=='plus'
  assert q('select context from private.general_work where id=%s',(coverage['id'],))['plan']['accessTier']=='plus'
  old_plan=dict(trial,sources=[{'url':d['url'],'label':d['label']} for d in docs])
  finish(coverage,{'coverage':{'status':'observable'},'preparedPlan':old_plan,'documents':docs})
  assert q('select plan from private.watches where id=%s',(wid,))['accessTier']=='plus'
  baseline=q('select public.s3_general_lease()');assert baseline['phase']=='baseline'
  at=dt.datetime.now(dt.timezone.utc).isoformat();memory={'generation':baseline['generation'],'baselineAt':at,'observedAt':at,'completed':False,'facts':[]}
  finish(baseline,{'decision':{'outcome':'baseline','memory':memory,'findings':[],'issues':[]},'findings':[],'documents':docs,'observedAt':at})
  assert q('select status from private.watches where id=%s',(wid,))=='active'
  assert q('select public.s7_subscription()')['remainingCreations']==5
  assert q('select credit_committed from private.general_budget')==0
  assert q('select model_committed from private.general_budget')==0
  assert q('select model_held from private.general_budget')==0
  report['UpgradeDuringPreparationPreservesWorkWithoutResetOrDoubleCharge']=True
  report['passed']=True
finally:
 if created:run([D,'exec',C,'dropdb','-U','postgres','--force',name])
 out=R/'validation/billing/local-database.json';out.parent.mkdir(exist_ok=True,parents=True);out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
