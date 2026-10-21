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
  sql('update private.page_change_pilot set enabled=false');sql("update private.capacity set enabled=false,slots=reserved");sql("update private.method_qualifications set qualified=false");sql("update private.admission_capacity set enabled=false,qualified=false")
 def setUp(self):
  sql('truncate private.watches cascade');sql('truncate private.inbox');sql('update private.method_qualifications set qualified=true')
  reset_capacity()
  sql('update private.admission_capacity set enabled=false,qualified=false')
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
  secret=next(line.split('=',1)[1] for line in (ROOT/'.local/s3-function.env').read_text().splitlines() if line.startswith('PROVIDER_WEBHOOK_SECRET='))
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

 def test_18_full_erasure_waits_for_remote_stop_and_deletes_identity(self):
  owner,token=login('erase-'+uuid.uuid4().hex[:8]+'@example.test')
  self.assertEqual(200,http('/functions/v1/api/v1/account',token=token)[0])
  wid,p=fixture_watch(owner)
  provider='fixture-'+wid
  sql("insert into private.inbox values('erase-event',%s,'check',%s,now())",(provider,Jsonb({'private':'erase me'})))
  s,_=http('/functions/v1/api/v1/account/deletion','POST',{'id':v7()},token)
  self.assertEqual(202,s)
  self.assertNotIn(owner,sql('select public.s3_erasure_candidates()',one=True))
  job=sql('select public.s3_lease()',one=True)
  sql('select public.s3_confirm_stop(%s,%s)',(job['id'],job['lease_token']))
  self.assertNotIn(owner,sql('select public.s3_erasure_candidates()',one=True))
  sql('select public.s3_remote_erased(%s)',(provider,)) # fixture acknowledgement, not a real provider deletion
  self.assertIn(owner,sql('select public.s3_erasure_candidates()',one=True))
  sql('select public.s3_erase_data(%s)',(owner,))
  # A crash before Auth deletion cannot recreate a private account from the old JWT.
  self.assertEqual(401,http('/functions/v1/api/v1/account',token=token)[0])
  sql('select public.s3_erase_data(%s)',(owner,))
  for table,column in [('accounts','id'),('watches','owner_id'),('commands','owner_id'),('devices','owner_id'),('provider_bindings','owner_id')]:
   self.assertEqual(0,sql(f'select count(*) from private.{table} where {column}=%s',(owner,),True))
  self.assertEqual(0,sql('select count(*) from private.inbox where provider_id=%s',(provider,),True))
  self.assertEqual(200,http('/auth/v1/admin/users/'+owner,'DELETE',token=ADMIN)[0])
  sql('select public.s3_erasure_identity_done(%s)',(owner,))
  self.assertNotIn(owner,sql('select public.s3_erasure_candidates()',one=True))
  self.assertEqual(0,sql('select count(*) from auth.users where id=%s',(owner,),True))
  self.assertTrue(sql('select identity_deleted_at is not null from private.erasure_ledger where owner_id=%s',(owner,),True))
 def test_19_emission_claim_survives_delete_and_blocks_stop_until_settled(self):
  wid,p=fixture_watch(self.a)
  device=str(uuid.uuid4())
  self.assertEqual(204,http('/functions/v1/api/v1/devices/'+device,'PUT',{'fcmToken':'emission-'+uuid.uuid4().hex,'language':'fr'},self.ta)[0])
  payload=finding(wid,p)
  sql("select public.s3_record_check(%s,1,'emission','finding',now(),'same-fact',%s)",(wid,Jsonb(payload)))
  d=sql('select public.s3_delivery_lease()',one=True)
  self.assertTrue(sql('select public.s3_delivery_allowed(%s,%s)',(d['id'],d['lease']),True))
  self.assertFalse(sql('select public.s3_delivery_allowed(%s,%s)',(d['id'],d['lease']),True))
  self.assertEqual(202,http('/functions/v1/api/v1/commands','POST',{'id':v7(),'operation':'delete','watchId':wid,'expectedVersion':1},self.ta)[0])
  self.assertEqual(0,sql('select count(*) from private.outbox',one=True))
  job=sql('select public.s3_lease()',one=True)
  with self.assertRaises(psycopg.errors.RaiseException):sql('select public.s3_confirm_stop(%s,%s)',(job['id'],job['lease_token']))
  sql("select public.s3_delivery_finish(%s,%s,'accepted')",(d['id'],d['lease']))
  sql('select public.s3_confirm_stop(%s,%s)',(job['id'],job['lease_token']))
  self.assertEqual('stopped',sql('select remote_state from private.watches where id=%s',(wid,),True))
 def test_20_worker_auth_and_real_identity_erasure(self):
  secret=next(line.split('=',1)[1] for line in (ROOT/'.local/s3-function.env').read_text().splitlines() if line.startswith('WORKER_SECRET='))
  self.assertEqual(403,http('/functions/v1/worker','POST',{},ADMIN)[0])
  owner,token=login('worker-erase-'+uuid.uuid4().hex[:8]+'@example.test')
  self.assertEqual(200,http('/functions/v1/api/v1/account',token=token)[0])
  self.assertEqual(202,http('/functions/v1/api/v1/account/deletion','POST',{'id':v7()},token)[0])
  for _ in range(5):
   code,value=http('/functions/v1/worker','POST',{},secret)
   self.assertEqual(200,code);self.assertEqual('disabled',value['state']);self.assertEqual('disabled',value['stop'])
   if sql('select identity_deleted_at is not null from private.erasure_ledger where owner_id=%s',(owner,),True):break
  self.assertTrue(sql('select identity_deleted_at is not null from private.erasure_ledger where owner_id=%s',(owner,),True))
  self.assertEqual(0,sql('select count(*) from auth.users where id=%s',(owner,),True))
  self.assertEqual(401,http('/functions/v1/api/v1/account',token=token)[0])
 def test_21_emission_and_pause_serialize_on_real_database_lock(self):
  wid,p=fixture_watch(self.a);device=str(uuid.uuid4())
  self.assertEqual(204,http('/functions/v1/api/v1/devices/'+device,'PUT',{'fcmToken':'race-'+uuid.uuid4().hex,'language':'fr'},self.ta)[0])
  sql("select public.s3_record_check(%s,1,'race','finding',now(),'fact',%s)",(wid,Jsonb(finding(wid,p))))
  d=sql('select public.s3_delivery_lease()',one=True)
  with futures.ThreadPoolExecutor(max_workers=1) as pool:
   with psycopg.connect(DB) as transaction:
    self.assertTrue(transaction.execute('select public.s3_delivery_allowed(%s,%s)',(d['id'],d['lease'])).fetchone()[0])
    pause=pool.submit(http,'/functions/v1/api/v1/commands','POST',{'id':v7(),'operation':'pause','watchId':wid,'expectedVersion':1},self.ta)
    with self.assertRaises(futures.TimeoutError):pause.result(timeout=.15)
   self.assertEqual(202,pause.result(timeout=10)[0])
  self.assertFalse(sql('select public.s3_delivery_allowed(%s,%s)',(d['id'],d['lease']),True))
  self.assertEqual('emitting',sql('select state from private.emissions where id=%s',(d['id'],),True))
  sql("select public.s3_delivery_finish(%s,%s,'unknown')",(d['id'],d['lease']))
 def test_22_admission_queue_idempotence_budget_and_lost_call(self):
  sql('update private.admission_capacity set enabled=true,qualified=true,budget_micro_usd=10000,used_micro_usd=0,reserved_micro_usd=0')
  t=preview();request={'id':v7(),'draft':{'text':'Un produit','language':'fr','mode':'once','revision':2,'answers':[]},'previousEvaluationId':None}
  h={'X-Preview-Token':t}
  code,value=http('/functions/v1/public/v1/evaluations','POST',request,headers=h)
  self.assertEqual((202,'pending'),(code,value['status']))
  self.assertEqual(value,http('/functions/v1/public/v1/evaluations','POST',request,headers=h)[1])
  job=sql('select public.s3_admission_lease()',one=True)
  self.assertEqual(request['id'],job['id']);self.assertIsNone(sql('select public.s3_admission_lease()',one=True))
  # Lost HTTP response consumes no second call/lease; reservation remains held.
  sql("update private.admission_jobs set lease_until=now()-interval '1 second' where id=%s",(job['id'],))
  self.assertIsNone(sql('select public.s3_admission_lease()',one=True))
  value=http('/functions/v1/public/v1/evaluations/'+request['id'],headers=h)[1]
  self.assertEqual('failed',value['status']);self.assertEqual(10000,sql('select reserved_micro_usd from private.admission_capacity',one=True))
 def test_23_admission_result_cannot_bypass_source_gate_and_cost_overrun_freezes(self):
  sql('update private.admission_capacity set enabled=true,qualified=true,budget_micro_usd=10000,used_micro_usd=0,reserved_micro_usd=0')
  t=preview();request={'id':v7(),'draft':{'text':'Demande','language':'fr','mode':'once','revision':0,'answers':[]},'previousEvaluationId':None}
  self.assertEqual(202,http('/functions/v1/public/v1/evaluations','POST',request,headers={'X-Preview-Token':t})[0])
  j=sql('select public.s3_admission_lease()',one=True)
  result={'id':j['id'],'draftRevision':0,'updatedAt':ISO(),'status':'ready'}
  with self.assertRaises(psycopg.errors.RaiseException):sql('select public.s3_admission_finish(%s,%s,%s,null,1)',(j['id'],j['lease'],Jsonb(result)))
  result.update(status='clarification',questions=[{'id':'product','label':'Quel produit ?','options':[],'required':True}])
  sql('select public.s3_admission_finish(%s,%s,%s,null,10001)',(j['id'],j['lease'],Jsonb(result)))
  self.assertFalse(sql('select enabled from private.admission_capacity',one=True))
  self.assertEqual('failed',http('/functions/v1/public/v1/evaluations/'+j['id'],headers={'X-Preview-Token':t})[1]['status'])
 def test_24_short_lived_worker_mac_replay_and_real_scheduler_transport(self):
  secret=next(line.split('=',1)[1] for line in (ROOT/'.local/s3-function.env').read_text().splitlines() if line.startswith('WORKER_SECRET='))
  stamp=str(int(time.time()));nonce=str(uuid.uuid4())
  signature=hmac.new(secret.encode(),('previens-moi-worker-v1.'+stamp+'.'+nonce).encode(),hashlib.sha256).hexdigest()
  h={'X-Worker-Time':stamp,'X-Worker-Nonce':nonce,'X-Worker-Signature':signature}
  self.assertEqual(200,http('/functions/v1/worker','POST',{},headers=h)[0])
  self.assertEqual(403,http('/functions/v1/worker','POST',{},headers=h)[0])
  sql("select vault.create_secret(%s,'previens-moi-worker') where not exists(select 1 from vault.secrets where name='previens-moi-worker')",(secret,))
  self.assertFalse(sql("select has_table_privilege('authenticated','vault.decrypted_secrets','select')",one=True))
  self.assertIsNone(sql('select public.s3_tick()',one=True))
  try:
   sql("update private.worker_schedule set enabled=true,url='http://kong:8000/functions/v1/worker'")
   # Inspect the uncommitted queue before pg_net can consume it: no durable credential.
   with psycopg.connect(DB) as c:
    rid=c.execute('select public.s3_tick()').fetchone()[0]
    headers=c.execute('select headers from net.http_request_queue where id=%s',(rid,)).fetchone()[0]
    self.assertNotIn(secret,json.dumps(headers));self.assertNotIn('Authorization',headers)
    self.assertIn('X-Worker-Signature',headers)
   response=None
   for _ in range(50):
    rows=sql('select status_code from net._http_response where id=%s',(rid,))
    if rows:response=rows[0][0];break
    time.sleep(.1)
   self.assertEqual(200,response)
  finally:sql('update private.worker_schedule set enabled=false')
 def test_25_identified_clarification_is_retrievable_and_settles_known_usage(self):
  sql('update private.admission_capacity set enabled=true,qualified=true,budget_micro_usd=10000,used_micro_usd=0,reserved_micro_usd=0')
  t=preview();request={'id':v7(),'draft':{'text':'Clarifier le produit','language':'fr','mode':'once','revision':3,'answers':[]},'previousEvaluationId':None}
  self.assertEqual(202,http('/functions/v1/public/v1/evaluations','POST',request,headers={'X-Preview-Token':t})[0])
  j=sql('select public.s3_admission_lease()',one=True)
  result={'id':j['id'],'draftRevision':3,'updatedAt':ISO(),'status':'clarification','questions':[{'id':'product','label':'Quel produit ?','options':[],'required':True}]}
  sql('select public.s3_admission_finish(%s,%s,%s,null,72)',(j['id'],j['lease'],Jsonb(result)))
  code,value=http('/functions/v1/public/v1/evaluations/'+j['id'],headers={'X-Preview-Token':t})
  self.assertEqual(200,code);self.assertEqual('product',value['questions'][0]['id'])
  self.assertEqual((0,72),sql('select reserved_micro_usd,used_micro_usd from private.admission_capacity')[0])
 def test_26_durable_provider_baseline_and_each_recurrence_has_own_reservation(self):
  t=preview();p=proposal(t);p['method']='known_page'
  sql('update private.proposals set payload=%s where id=%s',(Jsonb(p),p['id']))
  code,r=http('/functions/v1/api/v1/commands','POST',activate(t,p),self.ta);self.assertEqual(202,code);wid=r['watchId']
  j=sql('select public.s3_provider_lease()',one=True);self.assertEqual('start',j['kind'])
  args=(j['id'],j['lease_token'])
  self.assertTrue(sql("select public.s3_provider_checkpoint(%s,%s,'begin')",args,True));self.assertFalse(sql("select public.s3_provider_checkpoint(%s,%s,'begin')",args,True))
  sql("select public.s3_provider_checkpoint(%s,%s,'remember',%s)",args+(Jsonb({'id':'remote-'+wid}),))
  sql("select public.s3_provider_checkpoint(%s,%s,'later')",args)
  sql("update private.jobs set available_at=now()-interval '1 second' where id=%s",(j['id'],))
  j=sql('select public.s3_provider_lease()',one=True);args=(j['id'],j['lease_token']);self.assertEqual('run',j['attempt']['phase'])
  sql("select public.s3_provider_checkpoint(%s,%s,'reserve',%s)",args+(Jsonb({'before':100}),))
  sql("select public.s3_provider_checkpoint(%s,%s,'intent')",args)
  sql("select public.s3_provider_checkpoint(%s,%s,'queued',%s)",args+(Jsonb({'id':'baseline-check'}),))
  sql("select public.s3_provider_complete(%s,%s,now(),'baseline',1)",args)
  self.assertEqual('active',sql('select status from private.watches where id=%s',(wid,),True));self.assertEqual(0,sql('select count(*) from private.findings',one=True))
  previous=j['id']
  for n in range(2):
   sql("update private.watches set next_check_at=now()-interval '1 second' where id=%s",(wid,))
   self.assertEqual(1,sql('select public.s3_enqueue_due()',one=True));self.assertEqual(0,sql('select public.s3_enqueue_due()',one=True))
   j=sql('select public.s3_provider_lease()',one=True);self.assertNotEqual(previous,j['id']);self.assertEqual('check',j['kind']);previous=j['id'];args=(j['id'],j['lease_token'])
   sql("select public.s3_provider_checkpoint(%s,%s,'begin')",args)
   sql("select public.s3_provider_checkpoint(%s,%s,'reserve',%s)",args+(Jsonb({'before':99-n}),))
   sql("select public.s3_provider_checkpoint(%s,%s,'intent')",args)
   sql("select public.s3_provider_checkpoint(%s,%s,'queued',%s)",args+(Jsonb({'id':'check-'+str(n)}),))
   sql("select public.s3_provider_complete(%s,%s,now(),'no_change',1)",args)
  self.assertEqual((3,0),sql('select used_units,reserved_units from private.capacity')[0])
 def test_27_pause_during_create_retains_late_remote_identity_for_cleanup(self):
  t=preview();p=proposal(t);p['method']='known_page';sql('update private.proposals set payload=%s where id=%s',(Jsonb(p),p['id']))
  _,receipt=http('/functions/v1/api/v1/commands','POST',activate(t,p),self.ta);wid=receipt['watchId']
  j=sql('select public.s3_provider_lease()',one=True);args=(j['id'],j['lease_token']);sql("select public.s3_provider_checkpoint(%s,%s,'begin')",args)
  self.assertEqual(202,http('/functions/v1/api/v1/commands','POST',{'id':v7(),'operation':'pause','watchId':wid,'expectedVersion':1},self.ta)[0])
  sql("select public.s3_provider_checkpoint(%s,%s,'remember',%s)",args+(Jsonb({'id':'late-'+wid}),))
  self.assertFalse(sql('select public.s3_provider_allowed(%s,%s)',args,True))
  stop=sql('select public.s3_stop_lease()',one=True);self.assertFalse(stop['uncertainStart']);self.assertEqual('late-'+wid,stop['providerId'])
  sql('select public.s3_confirm_stop(%s,%s)',(stop['id'],stop['lease_token'])) # provider ack fixture
  self.assertEqual('done',sql('select state from private.jobs where id=%s',(j['id'],),True))
  self.assertEqual('paused',sql('select status from private.watches where id=%s',(wid,),True))
 def test_28_unsettled_cost_does_not_retain_personal_plan_after_remote_erasure(self):
  owner,token=login('unsettled-'+uuid.uuid4().hex[:8]+'@example.test');http('/functions/v1/api/v1/account',token=token)
  wid,p=fixture_watch(owner);reservation=str(uuid.uuid4());provider='fixture-'+wid
  sql('select public.s3_reserve(%s,%s,2)',(reservation,wid))
  self.assertEqual(202,http('/functions/v1/api/v1/account/deletion','POST',{'id':v7()},token)[0])
  stop=sql('select public.s3_stop_lease()',one=True);sql('select public.s3_confirm_stop(%s,%s)',(stop['id'],stop['lease_token']))
  self.assertNotIn(owner,sql('select public.s3_erasure_candidates()',one=True))
  sql('select public.s3_remote_erased(%s)',(provider,)) # fixture remote deletion acknowledged
  sql('select public.s3_erase_data(%s)',(owner,))
  self.assertEqual(0,sql('select count(*) from private.watches where owner_id=%s',(owner,),True))
  self.assertEqual(2,sql('select units from private.unsettled_costs where id=%s',(reservation,),True));self.assertEqual(2,sql('select reserved_units from private.capacity',one=True))
  self.assertEqual(200,http('/auth/v1/admin/users/'+owner,'DELETE',token=ADMIN)[0]);sql('select public.s3_erasure_identity_done(%s)',(owner,))
 def test_29_provider_and_stop_crash_after_last_lease_remain_visible_and_freeze(self):
  wid,p=fixture_watch(self.a)
  job=str(uuid.uuid4());sql("insert into private.jobs(id,watch_id,generation,kind,state,attempts,lease_until) values(%s,%s,1,'check','leased',3,now()-interval '1 second')",(job,wid))
  self.assertIsNone(sql('select public.s3_provider_lease()',one=True))
  self.assertEqual('ambiguous',sql('select state from private.jobs where id=%s',(job,),True));self.assertFalse(sql('select enabled from private.capacity',one=True))
  self.assertEqual(('unknown','degraded'),sql('select remote_state,health from private.watches where id=%s',(wid,))[0])
 def test_30_text_page_pilot_proposal_is_explicit_idempotent_and_does_not_drop_criteria(self):
  sql('update private.page_change_pilot set enabled=true')
  try:
   t=preview();h={'X-Preview-Token':t};jobs_before=sql('select count(*) from private.admission_jobs',one=True)
   cases=[('fr','Préviens-moi de tout changement sur https://source.example/page',True),('en','Notify me of any change on https://source.example/page',True),('fr','Préviens-moi de tout changement sur https://source.example/page et seulement les prix',False),('fr','Préviens-moi de tout changement sur https://127.0.0.1/page',False),('fr','Préviens-moi des nouveaux concerts dans ma ville',False)]
   for language,text,ready in cases:
    request={'id':v7(),'draft':{'text':text,'language':language,'mode':'continuous','revision':1,'answers':[]},'previousEvaluationId':None}
    code,result=http('/functions/v1/public/v1/evaluations','POST',request,headers=h)
    self.assertEqual(202,code,result);self.assertEqual('ready' if ready else 'unsupported',result['status'])
    if ready:
     p=result['proposal'];self.assertTrue(p['partial']);self.assertEqual('known_page',p['method']);self.assertEqual(text,p['draft']['text']);self.assertEqual(1,len(p['sources']))
     self.assertEqual(result,http('/functions/v1/public/v1/evaluations','POST',request,headers=h)[1])
     self.assertEqual(p,http('/functions/v1/public/v1/proposals/'+p['id'],headers=h)[1])
   self.assertEqual(jobs_before,sql('select count(*) from private.admission_jobs',one=True))
  finally:sql('update private.page_change_pilot set enabled=false')
 def test_31_verified_text_delta_completion_is_atomic_and_deduplicated(self):
  wid,p=fixture_watch(self.a);p['criteria']=['Ajouts et suppressions de texte sur la page indiquée.'];p['method']='known_page';p['sources']=[{'url':'https://source.example/page','label':'Page de test'}]
  sql('update private.watches set plan=%s where id=%s',(Jsonb(p),wid))
  device_id=str(uuid.uuid4());sql("insert into private.devices(id,owner_id,token,language) values(%s,%s,%s,'fr')",(device_id,self.a,'local-'+uuid.uuid4().hex))
  for n in range(2):
   job=str(uuid.uuid4());lease=str(uuid.uuid4());observed=ISO()
   sql("insert into private.jobs(id,watch_id,generation,kind,state,lease_token,lease_until) values(%s,%s,1,'check','leased',%s,now()+interval '1 minute')",(job,wid,lease))
   sql("insert into private.provider_attempts(job_id,phase,monitor_id,check_id,credits_before,run_intent_at) values(%s,'poll',%s,%s,100,now()-interval '1 second')",(job,'fixture-'+wid,'delta-'+str(n)))
   sql('select public.s3_reserve(%s,%s,2)',(job,wid))
   f=finding(wid,p);f['observedAt']=observed;f['evidence'][0].update(url=p['sources'][0]['url'],observedAt=observed)
   bad=json.loads(json.dumps(f));bad['criteriaSnapshot']=['Autre critère']
   with self.assertRaises(psycopg.Error):sql("select public.s3_provider_complete(%s,%s,%s,'finding',1,%s,%s)",(job,lease,observed,'a'*64,Jsonb(bad)))
   self.assertEqual(n,sql('select used_units from private.capacity',one=True))
   sql("select public.s3_provider_complete(%s,%s,%s,'finding',1,%s,%s)",(job,lease,observed,'a'*64,Jsonb(f)))
   self.assertEqual('done',sql('select state from private.jobs where id=%s',(job,),True))
  self.assertEqual(1,sql('select count(*) from private.findings where watch_id=%s',(wid,),True))
  self.assertEqual(1,sql('select count(*) from private.outbox o join private.findings f on f.id=o.finding_id where f.watch_id=%s and o.device_id=%s',(wid,device_id),True))
  self.assertEqual((2,0),sql('select used_units,reserved_units from private.capacity')[0])
if __name__=='__main__':
 suite=unittest.defaultTestLoader.loadTestsFromTestCase(Integration)
 start=time.monotonic();result=unittest.TextTestRunner(verbosity=2).run(suite)
 report={'scope':'Real local Supabase Auth + PostgreSQL + Edge HTTP; no provider or FCM','tests':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'passed':result.wasSuccessful(),'durationSeconds':round(time.monotonic()-start,2),'additionalSpend':0}
 (ROOT/'validation/s3/local-integration.json').write_text(json.dumps(report,indent=2)+'\n')
 raise SystemExit(not result.wasSuccessful())
