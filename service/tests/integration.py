"""Isolated local Supabase acceptance tests. No paid/provider network calls.
Requires own fresh stack; never connects to a remote database.
"""
import concurrent.futures as futures
import datetime as dt
import hashlib
import hmac
import json
import os
from pathlib import Path
import re
import time
import unittest
import urllib.error
import urllib.request
import urllib.parse
import uuid
import psycopg
from psycopg.types.json import Jsonb

ROOT=Path(__file__).resolve().parents[2]
STATUS=json.loads((ROOT/'.local/s3-local-status.json').read_text())
BASE=STATUS['API_URL']
assert BASE=='http://127.0.0.1:56321', 'Refuse any non-isolated test target'
DB='postgresql://postgres:postgres@127.0.0.1:56322/postgres'
KEY=STATUS['ANON_KEY']
ADMIN=STATUS['SERVICE_ROLE_KEY']
NOW=lambda: dt.datetime.now(dt.timezone.utc)
ISO=lambda: NOW().isoformat().replace('+00:00','Z')
def v7():
 h=f'{int(time.time()*1000):012x}'+'7'+uuid.uuid4().hex[13:16]+'8'+uuid.uuid4().hex[17:]
 return str(uuid.UUID(h))
def sql(q,params=(),one=False):
 with psycopg.connect(DB) as c:
  row=c.execute(q,params)
  if row.description:return row.fetchone()[0] if one else row.fetchall()
def http(path,method='GET',data=None,token=None,headers=None,raw=None):
 h={'apikey':KEY,'Content-Type':'application/json'}
 if token:h['Authorization']='Bearer '+token
 h.update(headers or {})
 req=urllib.request.Request(BASE+path,data=raw if raw is not None else json.dumps(data).encode() if data is not None else None,headers=h,method=method)
 try:
  with urllib.request.urlopen(req,timeout=35) as r:body=r.read();return r.status,json.loads(body) if body else None
 except urllib.error.HTTPError as e:
  body=e.read();return e.code,json.loads(body) if body else None

def mail_code(email):
 for attempt in range(10):
  with urllib.request.urlopen('http://127.0.0.1:56324/api/v1/messages') as r: messages=json.load(r)['messages']
  for item in messages:
   if any(v.get('Address')==email for v in item.get('To',[])):
    with urllib.request.urlopen('http://127.0.0.1:56324/api/v1/message/'+item['ID']) as r: message=json.load(r)
    match=re.search(r'\b\d{6}\b',message.get('Text','')+message.get('HTML',''))
    if match:return match.group()
  time.sleep(0.1)
 raise AssertionError('No OTP delivered to isolated mailbox')

def login(email):
 code,res=http('/auth/v1/otp','POST',{'email':email,'create_user':True})
 assert code==200, (code,res)
 code_value=mail_code(email)
 code,res=http('/auth/v1/verify','POST',{'email':email,'token':code_value,'type':'email'})
 assert code==200, (code,res)
 return res['user']['id'],res['access_token']

def proposal(token,partial=False):
 examples=json.loads((ROOT/'contracts/application-v1.examples.json').read_text())
 # Find the Proposal example irrespective of fixture wrapper.
 def walk(v):
  if isinstance(v,dict):
   if 'criteria' in v and 'expiresAt' in v and 'draft' in v and 'method' in v:return v
   for x in v.values():
    y=walk(x)
    if y:return y
  if isinstance(v,list):
   for x in v:
    y=walk(x)
    if y:return y
 p=walk(examples);assert p
 p=json.loads(json.dumps(p));p.update(id=str(uuid.uuid4()),expiresAt=(NOW()+dt.timedelta(hours=1)).isoformat().replace('+00:00','Z'),partial=partial)
 sql('insert into private.proposals(id,token_hash,version,payload,expires_at) values(%s,%s,%s,%s,%s)',(p['id'],hashlib.sha256(token.encode()).hexdigest(),p['version'],Jsonb(p),p['expiresAt']))
 return p

def preview():
 status,value=http('/functions/v1/public/v1/preview-sessions','POST',{})
 assert status==201,(status,value)
 return value['token']
def activate(token,p):return {'id':v7(),'operation':'activate','proposalId':p['id'],'proposalVersion':p['version'],'proposalToken':token,'acceptPartial':True}

def reset_capacity(slots=20):
 sql('update private.capacity set enabled=true,slots=%s,reserved=0,budget_units=100,used_units=0,reserved_units=0',(slots,))
def fixture_watch(owner,mode='continuous'):
 token=preview();p=proposal(token);p['draft']['mode']=mode
 wid=str(uuid.uuid4());p['id']=str(uuid.uuid4())
 sql("insert into private.watches(id,owner_id,plan,status,remote_state,baseline_at,provider_id) values(%s,%s,%s,'active','running',now()-interval '1 hour',%s)",(wid,owner,Jsonb(p),'fixture-'+wid))
 sql('update private.capacity set reserved=reserved+1 where id')
 return wid,p

def finding(wid,p):
 return {'id':str(uuid.uuid4()),'watchId':wid,'planId':p['id'],'planVersion':p['version'],'title':'Annonce contrôlée','fact':'La billetterie ouvre le 21 octobre.','reason':'Annonce officielle contrôlée','criteriaSnapshot':p['criteria'],'evidence':[{'url':'https://example.invalid/fixture','quote':'La billetterie ouvre le 21 octobre.','observedAt':ISO(),'publication':None}],'observedAt':ISO(),'event':None,'noveltyBasis':'baseline_change','correctsFindingId':None}

class Integration(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  # Reset only business fixtures; Auth users are local disposable addresses.
  sql('truncate private.accounts,private.previews cascade')
  cls.a,cls.ta=login('a-'+uuid.uuid4().hex[:8]+'@example.test')
  cls.b,cls.tb=login('b-'+uuid.uuid4().hex[:8]+'@example.test')
  for t in [cls.ta,cls.tb]:assert http('/functions/v1/api/v1/account',token=t)[0]==200
 @classmethod
 def tearDownClass(cls):
  # Leave no active fixture/capacity on completion (even on test failures).
  sql("update private.capacity set enabled=false,slots=reserved");sql("update private.method_qualifications set qualified=false")
 def setUp(self):
  sql('truncate private.watches cascade');sql('truncate private.inbox');sql('update private.method_qualifications set qualified=true')
  reset_capacity()
 def test_01_auth_invalid_and_cross_account(self):
  self.assertEqual(401,http('/functions/v1/api/v1/account',token=self.ta[:-5]+'bogus')[0])
  w,p=fixture_watch(self.a)
  self.assertEqual(200,http('/functions/v1/api/v1/watches/'+w,token=self.ta)[0])
  self.assertEqual(404,http('/functions/v1/api/v1/watches/'+w,token=self.tb)[0])
  for op in ['pause','delete']:
   s,r=http('/functions/v1/api/v1/commands','POST',{'id':v7(),'operation':op,'watchId':w,'expectedVersion':1},self.tb)
   self.assertEqual((404,'not_found'),(s,r['code']))
  with psycopg.connect(DB) as c:
   c.execute('set local role authenticated');c.execute("select set_config('request.jwt.claim.sub',%s,true)",(self.b,))
   self.assertEqual(0,c.execute('select count(*) from private.watches').fetchone()[0])
   with self.assertRaises(psycopg.errors.InsufficientPrivilege):c.execute('select * from private.capacity')
 def test_02_preview_scope_expiry_and_idempotence(self):
  t=preview();p=proposal(t)
  self.assertEqual(401,http('/functions/v1/public/v1/proposals/'+p['id'])[0])
  self.assertEqual(404,http('/functions/v1/public/v1/proposals/'+p['id'],headers={'X-Preview-Token':preview()})[0])
  self.assertEqual(200,http('/functions/v1/public/v1/proposals/'+p['id'],headers={'X-Preview-Token':t})[0])
  cmd=activate(t,p);s,r=http('/functions/v1/api/v1/commands','POST',cmd,self.ta)
  self.assertEqual((202,'pending'),(s,r['state']))
  sql("update private.proposals set expires_at=now()-interval '1 minute' where id=%s",(p['id'],))
  self.assertEqual(r,http('/functions/v1/api/v1/commands','POST',cmd,self.ta)[1])
  changed={**cmd,'acceptPartial':False}
  self.assertEqual(409,http('/functions/v1/api/v1/commands','POST',changed,self.ta)[0])
  self.assertEqual(409,http('/functions/v1/api/v1/commands','POST',{**cmd,'id':v7()},self.ta)[0])
  self.assertEqual(1,sql('select count(*) from private.watches',one=True))
 def test_03_atomic_last_slot(self):
  reset_capacity(1);t=preview();t2=preview();c1=activate(t,proposal(t));c2=activate(t2,proposal(t2))
  with futures.ThreadPoolExecutor(2) as ex:
   results=list(ex.map(lambda x:http('/functions/v1/api/v1/commands','POST',x[0],x[1]),[(c1,self.ta),(c2,self.tb)]))
  self.assertEqual([202,429],sorted(s for s,r in results))
  self.assertEqual(1,sql('select reserved from private.capacity',one=True))
 def test_04_budget_and_lost_create(self):
  w,p=fixture_watch(self.a);rid=str(uuid.uuid4())
  sql('select public.s3_reserve(%s,%s,80)',(rid,w))
  sql('select public.s3_reserve(%s,%s,80)',(rid,w))
  with self.assertRaises(psycopg.errors.RaiseException):sql('select public.s3_reserve(%s,%s,30)',(str(uuid.uuid4()),w))
  sql('select public.s3_settle(%s,81)',(rid,))
  self.assertFalse(sql('select enabled from private.capacity',one=True))
  self.assertEqual(80,sql('select reserved_units from private.capacity',one=True))
  sql("insert into private.jobs(watch_id,generation,kind,state,lease_until,attempts) values(%s,1,'start','leased',now()-interval '1 second',1)",(w,))
  self.assertIsNone(sql('select public.s3_lease()',one=True))
  self.assertEqual('ambiguous',sql('select state from private.jobs',one=True))
 def test_05_atomic_results_dedup_and_pause(self):
  w,p=fixture_watch(self.a);device=str(uuid.uuid4())
  self.assertEqual(204,http('/functions/v1/api/v1/devices/'+device,'PUT',{'fcmToken':'fixture-token-for-local-testing','language':'fr'},self.ta)[0])
  f=finding(w,p)
  def record(k,key='event-1'):
   return sql("select public.s3_record_check(%s,1,%s,'finding',now(),%s,%s)",(w,k,key,Jsonb(f)),True)
  self.assertEqual(f['id'],str(record('check1')))
  self.assertIsNone(record('check1'))
  f['id']=str(uuid.uuid4());self.assertIsNone(record('check2'))
  self.assertEqual(200,http('/functions/v1/api/v1/findings/'+sql('select id::text from private.findings',one=True),token=self.ta)[0])
  self.assertEqual(1,sql('select count(*) from private.outbox',one=True))
  self.assertEqual(404,http('/functions/v1/api/v1/findings/'+sql('select id::text from private.findings',one=True),token=self.tb)[0])
  leased=sql('select public.s3_delivery_lease()',one=True);self.assertTrue(leased)
  cmd={'id':v7(),'operation':'pause','watchId':w,'expectedVersion':1}
  s,r=http('/functions/v1/api/v1/commands','POST',cmd,self.ta)
  self.assertEqual((202,'stopping'),(s,r['remoteState']))
  self.assertFalse(sql('select public.s3_delivery_allowed(%s,%s)',(leased['id'],leased['lease']),True))
  self.assertIsNone(record('late-check','event-2'))
  self.assertEqual(1,sql('select reserved from private.capacity',one=True))
  job=sql('select public.s3_lease()',one=True)
  sql('select public.s3_confirm_stop(%s,%s)',(job['id'],job['lease_token']))
  self.assertEqual(0,sql('select reserved from private.capacity',one=True))
  self.assertEqual('succeeded',http('/functions/v1/api/v1/commands/'+cmd['id'],token=self.ta)[1]['state'])
 def test_06_once_and_failure_no_false_success(self):
  w,p=fixture_watch(self.a,'once');f=finding(w,p)
  sql("select public.s3_record_check(%s,1,'fail','failed',now())",(w,))
  self.assertIsNone(sql('select last_success_at from private.watches where id=%s',(w,),True))
  sql("select public.s3_record_check(%s,1,'success','finding',now(),%s,%s)",(w,'once-event',Jsonb(f)))
  self.assertEqual('completed',sql('select status from private.watches',one=True))
  self.assertEqual(1,sql('select count(*) from private.findings',one=True))
 def test_07_disabled_admission_and_contract(self):
  t=preview();body={'id':v7(),'draft':{'text':'Préviens-moi pour une nouvelle annonce','language':'fr','mode':'once','revision':1,'answers':[]},'previousEvaluationId':None}
  s,r=http('/functions/v1/public/v1/evaluations','POST',body,headers={'X-Preview-Token':t})
  self.assertEqual((202,'unsupported','unqualified_method'),(s,r['status'],r['reason']))
  self.assertEqual(r,http('/functions/v1/public/v1/evaluations','POST',body,headers={'X-Preview-Token':t})[1])
  self.assertEqual(400,http('/functions/v1/public/v1/evaluations','POST',{**body,'ownerId':self.a},headers={'X-Preview-Token':t})[0])
  self.assertEqual(0,sql('select count(*) from private.jobs',one=True))
 def test_08_deleted_account_old_jwt(self):
  w,p=fixture_watch(self.a)
  sql('update private.accounts set deleted_at=now() where id=%s',(self.a,))
  self.assertEqual(401,http('/functions/v1/api/v1/account',token=self.ta)[0])
  sql('update private.accounts set deleted_at=null where id=%s',(self.a,))
 def test_09_device_transfer_and_deleted_finding(self):
  w,p=fixture_watch(self.a);d=str(uuid.uuid4());data={'fcmToken':'fixture-token-rotation-shared','language':'fr'}
  http('/functions/v1/api/v1/devices/'+d,'PUT',data,self.ta)
  http('/functions/v1/api/v1/devices/'+d,'PUT',data,self.tb)
  self.assertEqual(self.b,str(sql('select owner_id from private.devices where id=%s',(d,),True)))
  self.assertEqual(204,http('/functions/v1/api/v1/devices/'+d,'DELETE',token=self.ta)[0])
  self.assertEqual(1,sql('select count(*) from private.devices where id=%s',(d,),True))
  f=finding(w,p);sql("select public.s3_record_check(%s,1,'c','finding',now(),'f',%s)",(w,Jsonb(f)))
  http('/functions/v1/api/v1/commands','POST',{'id':v7(),'operation':'delete','watchId':w,'expectedVersion':1},self.ta)
  self.assertEqual(404,http('/functions/v1/api/v1/findings/'+f['id'],token=self.ta)[0])

 def test_10_mail_otp_invalid_expired_and_refresh(self):
  email='expiry-'+uuid.uuid4().hex[:8]+'@example.test'
  self.assertEqual(200,http('/auth/v1/otp','POST',{'email':email,'create_user':True})[0])
  code=mail_code(email)
  self.assertEqual(403,http('/auth/v1/verify','POST',{'email':email,'token':'999999' if code!='999999' else '888888','type':'email'})[0])
  sql("update auth.users set confirmation_sent_at=now()-interval '11 minutes' where email=%s",(email,))
  self.assertEqual(403,http('/auth/v1/verify','POST',{'email':email,'token':code,'type':'email'})[0])
 def test_11_webhook_raw_signature_replay_and_limits(self):
  w,p=fixture_watch(self.a)
  secret=(ROOT/'.local/s3-function.env').read_text().strip().split('=',1)[1]
  event={'type':'monitor.check.completed','id':'check-local','webhookId':'delivery-local','success':True,'data':[{'monitorId':'fixture-'+w,'checkId':'check-local','status':'completed'}]}
  raw=json.dumps(event,separators=(',',':')).encode()
  signature='sha256='+hmac.new(secret.encode(),raw,hashlib.sha256).hexdigest()
  path='/functions/v1/webhook'
  self.assertEqual(403,http(path,'POST',raw=raw,headers={'X-Firecrawl-Signature':'sha256='+'0'*64})[0])
  self.assertEqual(0,sql('select count(*) from private.inbox',one=True))
  self.assertEqual(202,http(path,'POST',raw=raw,headers={'X-Firecrawl-Signature':signature})[0])
  self.assertEqual(202,http(path,'POST',raw=raw,headers={'X-Firecrawl-Signature':signature})[0])
  self.assertEqual(1,sql('select count(*) from private.inbox',one=True))
  self.assertEqual(403,http(path,'POST',raw=raw+b' ',headers={'X-Firecrawl-Signature':signature})[0])
  self.assertEqual(413,http(path,'POST',raw=b'x'*262145,headers={'X-Firecrawl-Signature':signature})[0])
  wrong=b'{"type":"unknown"}'
  sig='sha256='+hmac.new(secret.encode(),wrong,hashlib.sha256).hexdigest()
  self.assertEqual(400,http(path,'POST',raw=wrong,headers={'X-Firecrawl-Signature':sig})[0])
  self.assertEqual(0,sql('select count(*) from private.findings',one=True))
  http('/functions/v1/api/v1/commands','POST',{'id':v7(),'operation':'pause','watchId':w,'expectedVersion':1},self.ta)
  self.assertEqual(404,http(path,'POST',raw=raw,headers={'X-Firecrawl-Signature':signature})[0])
 def test_12_rpc_privileges_and_global_preview_limit(self):
  self.assertFalse(sql("select has_function_privilege('authenticated','public.s3_inbox(text,text,text,jsonb)','execute')",one=True))
  self.assertFalse(sql("select has_function_privilege('anon','public.s3_command(jsonb)','execute')",one=True))
  self.assertFalse(sql("select has_function_privilege('authenticated','public.s3_preview(text,text,jsonb,uuid)','execute')",one=True))
  sql("insert into private.previews(token_hash) select 'limit-'||n from generate_series(1,50) n on conflict do nothing")
  self.assertEqual(429,http('/functions/v1/public/v1/preview-sessions','POST',{})[0])
  sql("delete from private.previews where token_hash like 'limit-%%'")

 def test_13_resume_replace_confirmed_only_after_new_baseline(self):
  w,p=fixture_watch(self.a)
  def command(op,version,extra=None):
   cmd={'id':v7(),'operation':op,'watchId':w,'expectedVersion':version};cmd.update(extra or {})
   status,receipt=http('/functions/v1/api/v1/commands','POST',cmd,self.ta)
   self.assertEqual(202,status,(status,receipt));self.assertEqual('pending',receipt['state']);return cmd
  def stop():
   job=sql('select public.s3_lease()',one=True);self.assertEqual('stop',job['kind'])
   sql('select public.s3_confirm_stop(%s,%s)',(job['id'],job['lease_token']))
  def start():
   job=sql('select public.s3_lease()',one=True);self.assertEqual('start',job['kind'])
   sql('select public.s3_confirm_start(%s,%s,%s,now())',(job['id'],job['lease_token'],'fixture-resume-'+uuid.uuid4().hex))
  command('pause',1);stop();self.assertEqual(0,sql('select reserved from private.capacity',one=True))
  resumed=command('resume',2)
  self.assertEqual('pending',http('/functions/v1/api/v1/watches/'+w,token=self.ta)[1]['status'])
  self.assertIsNone(http('/functions/v1/api/v1/watches/'+w,token=self.ta)[1]['baselineAt'])
  start();self.assertEqual('succeeded',http('/functions/v1/api/v1/commands/'+resumed['id'],token=self.ta)[1]['state'])
  t=preview();replacement=proposal(t)
  replaced=command('replace',3,{'proposalId':replacement['id'],'proposalVersion':replacement['version'],'proposalToken':t,'acceptPartial':True})
  self.assertEqual(p['id'],http('/functions/v1/api/v1/watches/'+w,token=self.ta)[1]['plan']['id'])
  stop();self.assertEqual(1,sql('select reserved from private.capacity',one=True))
  self.assertEqual(replacement['id'],http('/functions/v1/api/v1/watches/'+w,token=self.ta)[1]['plan']['id'])
  start();self.assertEqual('succeeded',http('/functions/v1/api/v1/commands/'+replaced['id'],token=self.ta)[1]['state'])
  self.assertEqual(0,sql('select count(*) from private.findings',one=True))
 def test_14_direct_rpc_cannot_bypass_expected_version(self):
  w,p=fixture_watch(self.a)
  status,value=http('/rest/v1/rpc/s3_command','POST',{'body':{'id':v7(),'operation':'pause','watchId':w}},self.ta)
  self.assertEqual(400,status);self.assertEqual('invalid_input',value['message'])
  self.assertEqual('active',sql('select status from private.watches where id=%s',(w,),True))
  self.assertFalse(sql("select has_function_privilege('authenticated','public.s3_command_transaction(jsonb)','execute')",one=True))
 def test_15_account_deletion_and_restore_marker_replay(self):
  w,p=fixture_watch(self.b)
  status,receipt=http('/functions/v1/api/v1/account/deletion','POST',{'id':v7()},self.tb)
  self.assertEqual(202,status,(status,receipt));self.assertEqual('accepted',receipt['state'])
  self.assertEqual(401,http('/functions/v1/api/v1/account',token=self.tb)[0])
  # Simulate a stale restored row; replaying a separately retained ledger must close it again.
  sql('update private.accounts set deleted_at=null where id=%s',(self.b,))
  sql("update private.watches set status='active' where id=%s",(w,))
  self.assertEqual(401,http('/functions/v1/api/v1/account',token=self.tb)[0])
  sql('select public.s3_replay_erasure()')
  self.assertEqual(401,http('/functions/v1/api/v1/account',token=self.tb)[0])
  self.assertEqual('deleted',sql('select status from private.watches where id=%s',(w,),True))

 def test_16_pagination_is_stable_and_scoped(self):
  c,tc=login('page-'+uuid.uuid4().hex[:8]+'@example.test')
  http('/functions/v1/api/v1/account',token=tc)
  expected=[fixture_watch(self.a)[0] for _ in range(3)]
  observed=[];cursor=None
  for _ in range(4):
   path='/functions/v1/api/v1/watches?limit=1'+(('&cursor='+urllib.parse.quote(cursor,safe='')) if cursor else '')
   status,page=http(path,token=self.ta);self.assertEqual(200,status,(status,page))
   observed.extend(w['id'] for w in page['items']);cursor=page['nextCursor']
   if cursor is None:break
  self.assertEqual(expected,observed)
  self.assertEqual([],http('/functions/v1/api/v1/watches',token=tc)[1]['items'])
 def test_17_failed_stop_freezes_new_work(self):
  w,p=fixture_watch(self.a)
  sql("insert into private.jobs(watch_id,generation,kind,state,attempts,lease_until) values(%s,1,'stop','leased',3,now()-interval '1 second')",(w,))
  self.assertIsNone(sql('select public.s3_lease()',one=True))
  self.assertFalse(sql('select enabled from private.capacity',one=True))
  self.assertEqual('unknown',sql('select remote_state from private.watches where id=%s',(w,),True))

if __name__=='__main__':
 suite=unittest.defaultTestLoader.loadTestsFromTestCase(Integration)
 start=time.monotonic();result=unittest.TextTestRunner(verbosity=2).run(suite)
 report={'scope':'Real local Supabase Auth + PostgreSQL + Edge HTTP; no provider or FCM','tests':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'passed':result.wasSuccessful(),'durationSeconds':round(time.monotonic()-start,2),'additionalSpend':0}
 (ROOT/'validation/s3/local-integration.json').write_text(json.dumps(report,indent=2)+'\n')
 raise SystemExit(not result.wasSuccessful())
