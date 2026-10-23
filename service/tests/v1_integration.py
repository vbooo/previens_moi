"""Isolated database copy. Model/provider responses injected; zero paid calls.
Never migrates or truncates the active local/cloud databases.
"""
from pathlib import Path
import subprocess,uuid,json,time,datetime as dt,sys
import psycopg
from psycopg.types.json import Jsonb
R=Path(__file__).resolve().parents[2];D='/Applications/Docker.app/Contents/Resources/bin/docker';C='supabase_db_previens-moi-s3';DB='postgresql://postgres:postgres@127.0.0.1:56322/';name='workflow_'+uuid.uuid4().hex[:10]
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
  q('truncate private.accounts,private.previews cascade');q('truncate private.general_attempts')
  q('update private.capacity set enabled=true,slots=5,reserved=0,budget_units=100,used_units=0,reserved_units=0');q('update private.general_budget set enabled=true,qualified=true,model_limit=100000,model_used=0,model_held=0,credit_limit=64,credit_used=0,credit_held=0,qualification_preview_hash=null');q('update private.admission_capacity set enabled=false');q('update private.page_change_pilot set enabled=false');q("update private.method_qualifications set qualified=(method in ('web_search','known_page'))")
  owner=str(uuid.uuid4());device=str(uuid.uuid4());q('insert into auth.users(id,email) values(%s,%s)',(owner,'workflow-'+owner+'@example.test'));q('insert into private.accounts(id) values(%s)',(owner,));q("insert into private.devices(id,owner_id,token,language) values(%s,%s,%s,'fr')",(device,owner,'synthetic-token-'+device));q("select set_config('request.jwt.claims',%s,false)",(json.dumps({'sub':owner,'role':'authenticated','iat':int(time.time()),'amr':[{'method':'otp','timestamp':int(time.time())}]}),))
  token=q("select public.s3_preview('session')")['token'];draft={'text':'Préviens-moi des concerts de Mira à Tours.','language':'fr','mode':'continuous','revision':1,'answers':[]};intent={'status':'understood','title':'Mira à Tours','criteria':[{'id':'a','text':'Concert de Mira à Tours','quote':'concerts de Mira à Tours'}],'queries':['Mira Tours concerts'],'questions':[],'explanation':'Informations publiques.'}
  eid=v7();request={'id':eid,'draft':draft,'previousEvaluationId':None};r=q("select public.s3_preview('understand',%s,%s)",(token,Jsonb(request)));assert r['status']=='pending'
  assert q('select public.s4_understanding_lease(%s,%s)',(eid,'wrong')) is None
  x=q('select public.s4_understanding_lease(%s,%s)',(eid,token));assert x and x['phase']=='interpret';assert q('select public.s4_understanding_lease(%s,%s)',(eid,token)) is None
  p={'id':str(uuid.uuid4()),'version':1,'draft':draft,'title':intent['title'],'criteria':['Concert de Mira à Tours'],'exclusions':[],'coverage':'Recherche après confirmation.','partial':True,'sources':[],'method':'web_search','backgroundPreparation':True,'intervalHours':24,'limitations':['Couverture partielle.'],'limits':{'activePerAccount':1,'globalAvailable':0,'previewRemaining':0,'resetsAt':None},'expiresAt':(dt.datetime.now(dt.timezone.utc)+dt.timedelta(minutes=30)).isoformat().replace('+00:00','Z')}
  def finish(x,result,actual=200,credits=0):q('select public.s3_general_finish(%s,%s,%s,%s,%s)',(x['id'],x['lease'],Jsonb(result),actual,credits))
  finish(x,{'intent':intent,'proposal':p});finish(x,{'intent':intent,'proposal':p});r=q("select public.s3_preview('evaluation',%s,null,%s)",(token,eid));assert r['status']=='ready' and r['proposal']['sources']==[];assert q('select credit_used from private.general_budget')==0;assert q('select count(*) from private.general_attempts')==1;report['understandingBeforeWebAndReplaySafe']=True
  receipt=q('select public.s3_command(%s)',(Jsonb({'id':v7(),'operation':'activate','proposalId':p['id'],'proposalVersion':1,'proposalToken':token,'acceptPartial':True}),));assert receipt['state']=='pending';wid=receipt['watchId']
  x=q('select public.s3_general_lease()');assert x['phase']=='collect';doc={'url':'https://source.example/events','label':'Mira','text':'Mira publie sa liste de concerts : artiste, ville, dates et annonces. Aucune date pour Tours.','publication':None,'changedText':None,'retrieval':'search_scrape'};finish(x,{'documents':[doc],'retrievalHealthy':True},0,3)
  x=q('select public.s3_general_lease()');assert x['phase']=='decide';prepared=dict(p,sources=[{'url':doc['url'],'label':doc['label']}]);finish(x,{'coverage':{'status':'observable'},'preparedPlan':prepared});assert q('select status from private.watches where id=%s',(wid,))=='pending';assert q('select count(*) from private.preparation_notices')==0;report['coverageDoesNotActivatePrematurely']=True
  x=q('select public.s3_general_lease()');assert x['phase']=='baseline';at=dt.datetime.now(dt.timezone.utc).isoformat().replace('+00:00','Z');memory={'generation':x['generation'],'baselineAt':at,'observedAt':at,'completed':False,'facts':[]};finish(x,{'decision':{'outcome':'baseline','memory':memory,'findings':[],'issues':[]},'findings':[],'documents':[doc],'observedAt':at});assert q('select status from private.watches where id=%s',(wid,))=='active';assert q('select count(*) from private.findings')==0;assert q('select count(*) from private.preparation_notices')==1;report['silentBaselineAndSeparateReadinessNotice']=True
  notice=q('select public.s4_preparation_delivery_lease()');assert notice['kind']=='preparation' and q('select public.s4_preparation_delivery_allowed(%s,%s)',(notice['id'],notice['lease']))
  q('select public.s3_command(%s)',(Jsonb({'id':v7(),'operation':'pause','watchId':wid,'expectedVersion':1}),));assert not q('select public.s4_preparation_delivery_allowed(%s,%s)',(notice['id'],notice['lease']));q("select public.s4_preparation_delivery_finish(%s,%s,'cancelled')",(notice['id'],notice['lease']));report['lateNoticeBlockedAfterPause']=True
  # A new pending generation with unusable sources must fail, never become active.
  second=str(uuid.uuid4());q("insert into private.watches(id,owner_id,plan,status,remote_state) values(%s,%s,%s,'pending','starting')",(second,owner,Jsonb(p)));q("insert into private.jobs(watch_id,generation,kind) values(%s,1,'start')",(second,));q('update private.capacity set reserved=reserved+1')
  x=q('select public.s3_general_lease()');assert x['watchId']==second;finish(x,{'documents':[doc],'retrievalHealthy':True},0,3);x=q('select public.s3_general_lease()');finish(x,{'coverage':{'status':'unobservable','reason':'No suitable source.'}});assert q('select status from private.watches where id=%s',(second,))=='paused';assert q("select plan->>'preparationFailure' from private.watches where id=%s",(second,))=='source_unusable';assert q("select count(*) from private.preparation_notices where event='failed'")==1;report['failedPreparationNotifiesWithoutActivation']=True
  q('delete from private.devices where id=%s',(device,));assert q('select count(*) from private.preparation_notices')==0;report['noticeDataFollowsDeviceErasure']=True
  assert not q("select has_function_privilege('authenticated','public.s4_understanding_lease(uuid,text)','execute')");report['internalRpcNotExposedToUsers']=True
  # V1: sources are fixed, all checked hourly; old plans remain daily.
  q('truncate private.accounts,private.previews cascade');q('truncate private.general_attempts')
  q('update private.capacity set reserved=0');q('update private.general_budget set model_used=0,model_held=0,credit_used=0,credit_held=0')
  q('insert into private.accounts(id) values(%s)',(owner,));q("insert into private.devices(id,owner_id,token,language) values(%s,%s,%s,'fr')",(device,owner,'synthetic-token-'+device))
  token=q("select public.s3_preview('session')")['token'];draft=dict(draft,answers=[{'questionId':'region','value':'France'}]);eid=v7();r=q("select public.s3_preview('understand',%s,%s)",(token,Jsonb({'id':eid,'draft':draft,'previousEvaluationId':None})));x=q('select public.s4_understanding_lease(%s,%s)',(eid,token))
  p=dict(p,id=str(uuid.uuid4()),draft=draft,intervalHours=1,sourcePolicy='stable_v1');finish(x,{'intent':intent,'proposal':p})
  p=q('select public.s5_proposal_options(%s,%s,1,%s)',(p['id'],token,'once'));assert p['version']==2 and p['draft']['mode']=='once'
  assert q('select public.s5_proposal_options(%s,%s,1,%s)',(p['id'],token,'once'))==p
  denied=False
  try:q('select public.s5_proposal_options(%s,%s,2,%s)',(p['id'],'wrong','continuous'))
  except psycopg.Error as e:denied='not_found' in str(e)
  assert denied;report['confirmationOptionsIdempotentAndTokenBound']=True
  receipt=q('select public.s3_command(%s)',(Jsonb({'id':v7(),'operation':'activate','proposalId':p['id'],'proposalVersion':2,'proposalToken':token,'acceptPartial':True}),));wid=receipt['watchId']
  docs=[dict(doc,url='https://source.example/'+str(n),text='Concert de Mira à Tours le 2 mai 2027. Source '+str(n)) for n in range(3)]
  x=q('select public.s3_general_lease()');assert q('select credit_held from private.general_budget')==5
  finish(x,{'documents':docs,'retrievalHealthy':True,'checkCoverage':{'verified':3,'total':3}},0,5)
  x=q('select public.s3_general_lease()');prepared=dict(p,sources=[{'url':d['url'],'label':d['label']} for d in docs]);finish(x,{'coverage':{'status':'observable'},'preparedPlan':prepared,'documents':docs})
  x=q('select public.s3_general_lease()');at=dt.datetime.now(dt.timezone.utc).isoformat().replace('+00:00','Z');finding={'id':str(uuid.uuid4()),'watchId':wid,'planId':p['id'],'planVersion':p['version'],'title':'Concert','fact':'Concert de Mira à Tours','reason':'Annonce vérifiée','criteriaSnapshot':p['criteria'],'evidence':[{'url':docs[0]['url'],'quote':docs[0]['text'],'observedAt':at,'publication':None}],'observedAt':at,'event':None,'noveltyBasis':'initial','correctsFindingId':None};memory={'generation':x['generation'],'baselineAt':at,'observedAt':at,'completed':True,'facts':[]}
  finish(x,{'decision':{'outcome':'baseline','memory':memory,'findings':[],'issues':[]},'findings':[{'key':'a'*64,'finding':finding}],'documents':docs,'observedAt':at});assert q('select status from private.watches where id=%s',(wid,))=='completed';assert q('select next_check_at from private.watches where id=%s',(wid,)) is None;assert q('select count(*) from private.findings')==1;assert q('select count(*) from private.outbox')==0;assert q('select reserved from private.capacity')==0
  notice=q('select public.s4_preparation_delivery_lease()');assert q('select public.s4_preparation_delivery_allowed(%s,%s)',(notice['id'],notice['lease']));report['initialResultIsVisibleWithoutFalseNoveltyAndOneOffCompletes']=True
  # Continuous variant: all three retained sources charged, exactly one hour scheduled.
  cid=str(uuid.uuid4());pc=dict(prepared,id=str(uuid.uuid4()),draft=dict(p['draft'],mode='continuous'));q("insert into private.proposals(id,token_hash,version,payload,expires_at) values(%s,encode(extensions.digest(%s,'sha256'),'hex'),2,%s,now()+interval '30 minutes')",(pc['id'],token,Jsonb(pc)));q('insert into private.general_plans values(%s,%s)',(pc['id'],Jsonb(intent)))
  rc=q('select public.s3_command(%s)',(Jsonb({'id':v7(),'operation':'activate','proposalId':pc['id'],'proposalVersion':2,'proposalToken':token,'acceptPartial':True}),));cw=rc['watchId'];x=q('select public.s3_general_lease()');assert q('select credit_held from private.general_budget')==3;finish(x,{'documents':docs,'retrievalHealthy':True,'checkCoverage':{'verified':3,'total':3}},0,3);x=q('select public.s3_general_lease()');finish(x,{'coverage':{'status':'observable'},'preparedPlan':pc,'documents':docs});x=q('select public.s3_general_lease()');at=dt.datetime.now(dt.timezone.utc).isoformat().replace('+00:00','Z');memory=dict(memory,generation=x['generation'],completed=False,baselineAt=at,observedAt=at);finish(x,{'decision':{'outcome':'baseline','memory':memory,'findings':[],'issues':[]},'findings':[],'documents':docs,'observedAt':at})
  assert q("select extract(epoch from next_check_at-last_success_at)::int from private.watches where id=%s",(cw,))==3600
  q("update private.watches set next_check_at=now()-interval '1 second' where id=%s",(cw,));x=q('select public.s3_general_lease()');assert x['context']['plan']['sources']==pc['sources'] and not x['context']['rediscover'];assert q('select credit_held from private.general_budget')==3
  finish(x,{'documents':docs[:2],'retrievalHealthy':False,'checkCoverage':{'verified':2,'total':3}},0,3);x=q('select public.s3_general_lease()');at=dt.datetime.now(dt.timezone.utc).isoformat().replace('+00:00','Z');finish(x,{'decision':{'outcome':'degraded','memory':memory,'findings':[],'issues':['incomplete_retrieval']},'findings':[],'documents':docs,'observedAt':at});assert q('select health from private.watches where id=%s',(cw,))=='degraded';assert q("select plan->'checkCoverage'->>'verified' from private.watches where id=%s",(cw,))=='2';report['hourlyStableSourcesAndPartialFailurePreserved']=True
  q('update private.general_memory set cycle=24 where watch_id=%s',(cw,));q("update private.watches set next_check_at=now()-interval '1 second' where id=%s",(cw,));x=q('select public.s3_general_lease()');assert x['context']['rediscover'] and q('select credit_held from private.general_budget')==8
  expanded=docs+[dict(doc,url='https://source.example/new'+str(n)) for n in range(3)];finish(x,{'documents':expanded,'retrievalHealthy':True,'checkCoverage':{'verified':3,'total':3}},0,8);x=q('select public.s3_general_lease()');newdocs=[docs[0],docs[1],expanded[-1]];newplan=dict(pc,sources=[{'url':d['url'],'label':d['label']} for d in newdocs]);finish(x,{'coverage':{'status':'observable'},'preparedPlan':newplan,'documents':newdocs});x=q('select public.s3_general_lease()');assert x['phase']=='baseline' and x['kind']=='check';at=dt.datetime.now(dt.timezone.utc).isoformat().replace('+00:00','Z');finish(x,{'decision':{'outcome':'no_change','memory':memory,'findings':[],'issues':[]},'findings':[],'documents':newdocs,'observedAt':at});assert q("select plan->'sources' from private.watches where id=%s",(cw,))==newplan['sources'];assert q("select plan->'criteria' from private.watches where id=%s",(cw,))==pc['criteria'];report['rediscoveryKeepsConfirmedCriteriaAndAllCurrentSources']=True
  q('update private.general_budget set credit_limit=credit_used+credit_held');q("update private.watches set next_check_at=now()-interval '1 second' where id=%s",(cw,));assert q('select public.s3_general_lease()') is None;assert q('select status from private.watches where id=%s',(cw,))=='suspended_by_capacity';report['quotaSuspendsWithoutProviderCall']=True
  q('update private.capacity set budget_units=used_units+reserved_units')
  q('update private.proposals set claimed_by=null,payload=jsonb_set(payload,%s,%s) where id=%s',(['method'],Jsonb('known_page'),pc['id']))
  rejected=False
  try:q('select public.s3_command(%s)',(Jsonb({'id':v7(),'operation':'activate','proposalId':pc['id'],'proposalVersion':2,'proposalToken':token,'acceptPartial':True}),))
  except psycopg.Error as e:rejected='quota_exceeded' in str(e)
  assert rejected;report['exhaustedLiteralPilotRejectedBeforeProviderWork']=True
  q('update private.general_budget set model_limit=model_used+model_held');eid=v7();r=q("select public.s3_preview('understand',%s,%s)",(token,Jsonb({'id':eid,'draft':draft,'previousEvaluationId':None})));assert r['status']=='failed' and r['error']['code']=='quota_exceeded';report['understandingExhaustionIsQuotaNotUnsupported']=True
  q('select public.s3_purge()');report['passed']=True
except Exception as e:
 report['failure']=type(e).__name__;raise
finally:
 if created:run([D,'exec',C,'dropdb','-U','postgres',name])
 out=R/'validation/v1';out.mkdir(parents=True,exist_ok=True);(out/'local-database.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
