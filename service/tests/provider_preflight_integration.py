"""SQL proof that a preflight-read retry cannot replay a committed run."""
from pathlib import Path
import sys,json,uuid
sys.path.insert(0,str(Path(__file__).parent))
from integration import sql,http,login,fixture_watch,v7,ROOT
sql('update private.capacity set enabled=true,slots=10,reserved=0,budget_units=100,used_units=0,reserved_units=0')
owner,token=login('preflight-'+uuid.uuid4().hex[:8]+'@example.test');assert http('/functions/v1/api/v1/account',token=token)[0]==200
wid,_=fixture_watch(owner)
def job(intent=False):
 id=str(uuid.uuid4());lease=str(uuid.uuid4())
 sql("insert into private.jobs(id,watch_id,generation,kind,state,lease_token,lease_until) values(%s,%s,1,'check','leased',%s,now()+interval '1 minute')",(id,wid,lease))
 sql("insert into private.provider_attempts(job_id,phase,monitor_id,run_intent_at) values(%s,'run','synthetic',case when %s then now() else null end)",(id,intent));return id,lease
id,lease=job()
for n in range(4):
 if n:lease=str(uuid.uuid4());sql("update private.jobs set state='leased',lease_token=%s,lease_until=now()+interval '1 minute' where id=%s",(lease,id))
 assert sql("select public.s3_provider_checkpoint(%s,%s,'defer_read','{}')",(id,lease),True)
 assert sql('select enabled from private.capacity',one=True)
assert sql('select state from private.jobs where id=%s',(id,),True)=='done'
assert sql('select health from private.watches where id=%s',(wid,),True)=='degraded'
id,lease=job(True);refused=False
try:sql("select public.s3_provider_checkpoint(%s,%s,'defer_read','{}')",(id,lease))
except Exception:refused=True
assert refused
sql("update private.jobs set state='done' where id=%s",(id,))
id,lease=job();assert http('/functions/v1/api/v1/commands','POST',{'id':v7(),'operation':'pause','watchId':wid,'expectedVersion':1},token)[0]==202
sql("select public.s3_provider_checkpoint(%s,%s,'defer_read','{}')",(id,lease));assert sql('select state from private.jobs where id=%s',(id,),True)=='done'
report={'passed':True,'boundedPreflightReadRetry':True,'runIntentPreventsRetry':True,'pauseSupersedesLateRead':True,'unrelatedCapacityNotFrozen':True,'newProviderCalls':0,'scope':'Isolated PostgreSQL with synthetic fixtures'}
(ROOT/'validation/s3/provider-preflight-local.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
