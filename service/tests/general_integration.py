"""General-engine transactions on the isolated local stack; no model/provider calls."""
import datetime as dt,json,uuid,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
from integration import sql,http,login,preview,activate,v7,ISO,NOW,Jsonb,ROOT
report={'scope':'Local PostgreSQL/HTTP; explicitly injected source/model evidence, no live semantic-quality claim','passed':False,'newPaidCalls':0}
def lease():
 value=sql('select public.s3_general_lease()',one=True);assert value;return value
def finish(work,result,actual=200,credits=0):sql('select public.s3_general_finish(%s,%s,%s,%s,%s)',(work['id'],work['lease'],Jsonb(result),actual,credits))
def evaluation(text):
 t=preview();request={'id':v7(),'draft':{'text':text,'language':'fr','mode':'continuous','revision':1,'answers':[]},'previousEvaluationId':None}
 code,value=http('/functions/v1/public/v1/evaluations','POST',request,headers={'X-Preview-Token':t});assert code==202 and value['status']=='pending',(code,value);return t,request
try:
 sql('truncate private.accounts,private.previews cascade');sql('truncate private.general_attempts')
 sql('update private.capacity set enabled=true,slots=5,reserved=0,budget_units=100,used_units=0,reserved_units=0')
 sql('update private.admission_capacity set enabled=false');sql('update private.page_change_pilot set enabled=false')
 sql('update private.general_budget set enabled=true,qualified=true,model_limit=100000,model_used=0,model_held=0,credit_limit=40,credit_used=0,credit_held=0')
 sql("update private.method_qualifications set qualified=(method in ('known_page','web_search'))")
 owner,token=login('general-'+uuid.uuid4().hex[:8]+'@example.test');assert http('/functions/v1/api/v1/account',token=token)[0]==200
 t,request=evaluation('Préviens-moi des concerts de Mira à Tours.')
 work=lease();assert work['phase']=='interpret' and sql('select model_held from private.general_budget',one=True)==20000
 intent={'status':'understood','title':'Mira à Tours','criteria':[{'id':'subject','text':'Concert de Mira à Tours','quote':'concerts de Mira à Tours'}],'queries':['Mira concerts Tours'],'questions':[],'explanation':'Sources publiques.'}
 finish(work,{'intent':intent});work=lease();assert work['phase']=='collect'
 doc={'url':'https://source.example/events','label':'Concerts','text':'Mira propose sa programmation de concerts avec dates et villes en France.','publication':None,'changedText':None,'retrieval':'supplied_page'}
 finish(work,{'documents':[doc],'retrievalHealthy':True},0,3);work=lease();assert work['phase']=='decide'
 p={'id':str(uuid.uuid4()),'version':1,'draft':request['draft'],'title':intent['title'],'criteria':[c['text'] for c in intent['criteria']],'exclusions':[],'coverage':'Page publique lue, recherche bornée.','partial':True,'sources':[{'url':doc['url'],'label':doc['label']}],'method':'web_search','intervalHours':24,'limitations':['Essai borné.'],'limits':{'activePerAccount':1,'globalAvailable':1,'previewRemaining':0,'resetsAt':None},'expiresAt':(NOW()+dt.timedelta(minutes=30)).isoformat().replace('+00:00','Z')}
 finish(work,{'coverage':{'status':'observable','reason':'Programmation publique.'},'proposal':p})
 assert http('/functions/v1/public/v1/evaluations/'+request['id'],headers={'X-Preview-Token':t})[1]['status']=='ready'
 status,receipt=http('/functions/v1/api/v1/commands','POST',activate(t,p),token);assert status==202,(status,receipt);wid=receipt['watchId']
 device=str(uuid.uuid4());assert http('/functions/v1/api/v1/devices/'+device,'PUT',{'fcmToken':'local-general-'+uuid.uuid4().hex,'language':'fr'},token)[0]==204
 work=lease();assert work['kind']=='start' and work['phase']=='collect';finish(work,{'documents':[doc],'retrievalHealthy':True},0,3);work=lease()
 observed=ISO();memory={'generation':work['generation'],'baselineAt':observed,'observedAt':observed,'completed':False,'facts':[]}
 finish(work,{'decision':{'memory':memory,'outcome':'baseline','findings':[],'issues':[]},'findings':[],'documents':[doc],'observedAt':observed})
 assert sql('select status from private.watches where id=%s',(wid,),True)=='active';assert sql('select count(*) from private.outbox',one=True)==0
 report['silentBaseline']=True
 for iteration in range(2):
  sql("update private.watches set next_check_at=now()-interval '1 second' where id=%s",(wid,));work=lease();assert work['kind']=='check';finish(work,{'documents':[doc],'retrievalHealthy':True},0,3);work=lease();observed=ISO()
  fact={'key':'a'*64,'identity':{'who':'Mira','what':'concert','where':'Tours','when':'2027-03-12','variant':None},'text':'Annonce synthétique : Mira à Tours le 12 mars 2027.'};memory.update(observedAt=observed,facts=[fact])
  finding={'id':str(uuid.uuid4()),'watchId':wid,'planId':p['id'],'planVersion':1,'title':intent['title'],'fact':fact['text'],'reason':'Annonce synthétique de contrôle.','criteriaSnapshot':p['criteria'],'evidence':[{'url':doc['url'],'quote':fact['text'],'observedAt':observed,'publication':None}],'observedAt':observed,'event':None,'noveltyBasis':'baseline_change','correctsFindingId':None}
  result={'decision':{'memory':memory,'outcome':'finding','findings':[],'issues':[]},'findings':[{'key':fact['key'],'finding':finding}],'documents':[doc],'observedAt':observed};finish(work,result);finish(work,result)
 assert sql('select count(*) from private.findings where watch_id=%s',(wid,),True)==1
 assert sql('select count(*) from private.outbox',one=True)==1;report['atomicFindingAndReplayDedup']=True
 # Pause while the next collected observation awaits judgment: late finish settles
 # known cost but cannot add a result or an outbox entry to a superseded generation.
 sql("update private.watches set next_check_at=now()-interval '1 second' where id=%s",(wid,));work=lease();finish(work,{'documents':[doc],'retrievalHealthy':True},0,3);work=lease()
 status,_=http('/functions/v1/api/v1/commands','POST',{'id':v7(),'operation':'pause','watchId':wid,'expectedVersion':1},token);assert status==202
 before=sql('select model_used from private.general_budget',one=True);finish(work,result);assert sql('select model_used from private.general_budget',one=True)==before+200
 assert sql('select count(*) from private.outbox',one=True)==1
 stop=sql('select public.s3_stop_lease()',one=True);assert stop and not stop['uncertainStart'];sql('select public.s3_confirm_stop(%s,%s)',(stop['id'],stop['lease_token']))
 assert sql('select remote_state from private.watches where id=%s',(wid,),True)=='stopped';report['pauseRejectsLateGeneration']=True
 # Budget exhaustion during a queued start/active check stops work before HTTP.
 status,_=http('/functions/v1/api/v1/commands','POST',{'id':v7(),'operation':'resume','watchId':wid,'expectedVersion':2},token);assert status==202
 sql('update private.general_budget set credit_limit=credit_used+credit_held')
 assert sql('select public.s3_general_lease()',one=True) is None
 assert sql('select status from private.watches where id=%s',(wid,),True)=='paused'
 sql("update private.watches set status='active',remote_state='running',baseline_at=now()-interval '1 hour',next_check_at=now()-interval '1 second' where id=%s",(wid,));sql('update private.capacity set reserved=reserved+1')
 assert sql('select public.s3_general_lease()',one=True) is None
 assert sql('select status from private.watches where id=%s',(wid,),True)=='suspended_by_capacity'
 code,error=http('/functions/v1/api/v1/commands','POST',{'id':v7(),'operation':'resume','watchId':wid,'expectedVersion':3},token);assert code==429 and error['code']=='quota_exceeded'
 sql('update private.general_budget set credit_limit=40');report['quotaStopsBeforeCallsAndExplainsResume']=True
 # New admission: abandoned model lease never creates another paid attempt.
 _,second=evaluation('Préviens-moi des publications sur les arbres.')
 lost=lease();sql("update private.general_work set lease_until=now()-interval '1 second' where id=%s",(lost['id'],));assert sql('select public.s3_general_lease()',one=True) is None
 assert sql('select model_held from private.general_budget',one=True)==20000;assert sql('select state from private.general_work where id=%s',(lost['id'],),True)=='unknown';report['unknownCallHeldWithoutRetry']=True
 # Identity erasure removes context/body/fact memory through FK cascades, while
 # opaque accounting survives without owner, request, page or fact text.
 sql('insert into private.erasure_ledger(owner_id) values(%s)',(owner,));sql('select public.s3_erase_data(%s)',(owner,))
 assert sql('select count(*) from private.general_memory',one=True)==0;assert sql('select count(*) from private.general_plans',one=True)==0
 report['erasureRemovesGeneralContexts']=True;report['passed']=True
finally:
 sql('update private.general_budget set enabled=false');sql("update private.method_qualifications set qualified=false where method='web_search'")
 (ROOT/'validation/s3/general-local-integration.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
