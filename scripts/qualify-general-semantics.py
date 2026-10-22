#!/usr/bin/env python3
"""Frozen, one-shot generic intent/evidence qualification. Never retries a paid call."""
import datetime as dt,hashlib,json,sqlite3,subprocess,urllib.request,urllib.error,unicodedata,os
from pathlib import Path
R=Path(__file__).resolve().parents[1];OUT=R/'.local/s3-general-20261022';MANIFEST=R/'validation/s3/general-m1-m2-manifest.json';DENO=str(R/'service/node_modules/.bin/deno');HELPER=str(R/'scripts/semantic-request.ts')
def run(value):
 r=subprocess.run([DENO,'run',HELPER],input=json.dumps(value,ensure_ascii=False),text=True,capture_output=True,check=True);return r.stdout.strip()
def folded(s):return ''.join(c for c in unicodedata.normalize('NFD',s.lower()) if not unicodedata.combining(c))
def main():
 m=json.loads(MANIFEST.read_text());assert len(m['cases'])<=m['maxPaidCalls']==22 and m['newTotalEnvelopeUSD']==.4
 for f,h in m['codeSha256'].items():assert hashlib.sha256((R/f).read_bytes()).hexdigest()==h,'Code changed after freeze'
 baseline=json.loads((R/'validation/s3/cost-reconciliation.json').read_text());assert baseline['missingOrAmbiguousHistoricalRequests']==0 and baseline['completedResponsesMatched']==128
 assert (baseline['usageUpperUSD']+.05+m['newTotalEnvelopeUSD'])*1.5*1.25<2
 requests=[]
 for case in m['cases']:
  raw=run({'operation':'request','kind':case['kind'],'data':case['data']});body=json.loads(raw)
  upper=((len(raw.encode())+4096)*.4+body['max_output_tokens']*1.6)/1e6
  assert upper<.03 and body['model']==m['model'] and body['service_tier']=='default' and body['store'] is False
  requests.append((case,raw,upper))
 if '--execute' not in os.sys.argv:print(json.dumps({'prepared':True,'calls':len(requests),'sumFullReservationsUSD':sum(r[2] for r in requests),'newEnvelopeUSD':.4}));return
 OUT.mkdir(exist_ok=True);fd=os.open(OUT/'started',os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600);os.close(fd)
 key=next(l.split('=',1)[1].strip().strip('\"\'') for l in (R/'engine-lab/.env').read_text().splitlines() if l.startswith('OPENAI_API_KEY='))
 class NoRedirect(urllib.request.HTTPRedirectHandler):
  def redirect_request(self,*a,**k):return None
 opener=urllib.request.build_opener(NoRedirect());ledger=sqlite3.connect(OUT/'ledger.sqlite')
 ledger.execute('create table if not exists calls(label text primary key,status text,reserved real,actual real,input_tokens integer,output_tokens integer)')
 results=[];stopped=None
 for case,raw,upper in requests:
  held=ledger.execute('select coalesce(sum(coalesce(actual,reserved)),0) from calls').fetchone()[0];assert held+upper<=.4
  ledger.execute('insert into calls(label,status,reserved) values(?,\'reserved\',?)',(case['id'],upper));ledger.commit()
  value=None;actual=None
  try:
   req=urllib.request.Request('https://api.openai.com/v1/responses',data=raw.encode(),headers={'Authorization':'Bearer '+key,'Content-Type':'application/json'},method='POST')
   with opener.open(req,timeout=60) as response:value=json.load(response)
   f=OUT/(case['id']+'-response.json');f.write_text(json.dumps(value,ensure_ascii=False,indent=2));f.chmod(0o600)
   usage=value['usage'];a,b=usage['input_tokens'],usage['output_tokens'];assert type(a) is int and type(b) is int and min(a,b)>=0
   actual=(a*.4+b*1.6)/1e6;assert actual<=upper
   ledger.execute('update calls set status=\'completed\',actual=?,input_tokens=?,output_tokens=? where label=?',(actual,a,b,case['id']));ledger.commit()
   parsed=json.loads(run({'operation':'parse','kind':case['kind'],'data':case['data'],'response':value}));v=parsed['value']
   missing=[s for s in case.get('requiredTerms',[]) if folded(s) not in folded(' '.join(c['text'] for c in v.get('criteria',[])))]
   passed=v['status'] in case['expectedStatuses'] and not missing and ('expectedSameAs' not in case or v.get('sameAs')==case['expectedSameAs'])
   results.append({'id':case['id'],'passed':passed,'expectedStatuses':case['expectedStatuses'],'status':v['status'],'missingCriteriaTerms':missing,'value':v,'factKey':parsed.get('key'),'usageUSD':actual})
   print(json.dumps({'id':case['id'],'passed':passed,'status':v['status'],'missing':missing}),flush=True)
  except Exception as e:
   if actual is None:
    ledger.execute('update calls set status=\'unknown\' where label=?',(case['id'],));ledger.commit();stopped=type(e).__name__;break
   results.append({'id':case['id'],'passed':False,'failure':type(e).__name__,'usageUSD':actual});print(json.dumps({'id':case['id'],'passed':False,'failure':type(e).__name__}),flush=True)
 used,held,count=ledger.execute('select coalesce(sum(actual),0),coalesce(sum(case when actual is null then reserved else 0 end),0),count(*) from calls').fetchone()
 report={'date':'2026-10-22','manifestSha256':hashlib.sha256(MANIFEST.read_bytes()).hexdigest(),'results':results,'completed':len(results),'total':len(m['cases']),'passed':sum(x['passed'] for x in results),'stopped':stopped,'actualCalls':count,'newEstimatedUSD':used,'unknownReservedUSD':held,'newEnvelopeUSD':.4,'cumulativeConservativeUpperEUR':(baseline['usageUpperUSD']+.05+used+held)*1.5*1.25,'independentSourceDiscoveryQualified':False,'methodsEnabledInApp':False,'recurringGeneralMonitoringQualified':False,'scope':m['scope']}
 (R/'validation/s3/general-m1-m2-result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['completed','passed','total','stopped','newEstimatedUSD','unknownReservedUSD']}),flush=True)
if __name__=='__main__':main()
