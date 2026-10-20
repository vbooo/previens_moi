"""One-shot historical A1 qualification; no discovery, monitors or notifications."""
import datetime as dt,hashlib,json,os,sqlite3,sys,urllib.request,urllib.error
from pathlib import Path
import jsonschema
R=Path(__file__).resolve().parents[1];SPEC=R/'service/supabase/functions/_shared/admission-spec.json';OUT=R/'.local/a1-20261020'
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 spec=json.loads(SPEC.read_text());cases=json.loads((R/'engine-lab/corpus/admissions.json').read_text())
 cases.append({'id':'D01','text':'Préviens-moi des nouveaux concerts annoncés à l’Accor Arena Paris.','expected':{'family':'concert','status':'understood','subject':'Accor Arena','location':'Paris'}})
 old=sqlite3.connect(R/'engine-lab/reports/live-openai-20261009/ledger.sqlite')
 count,reserved,actual,unknown=old.execute('select count(*),sum(reserved),sum(actual),sum(actual is null) from calls').fetchone();old.close()
 assert count==83 and unknown==0 and reserved<.588 and actual<.048
 # Full old reservations, rounded Exa allowance and adverse FX/tax buffer: EUR2 unchanged.
 upper=.1;eur_upper=(reserved+.05+upper)*1.5*1.25;assert eur_upper<2
 manifest={'createdAt':dt.datetime.now(dt.timezone.utc).isoformat(),'expiresAt':'2026-10-21T00:00:00Z','cases':cases,'specSha256':digest(SPEC),'callsMax':9,'retries':0,'inputBytesMax':20000,'maxOutputTokens':2200,'model':spec['model'],'newUpperUsd':upper,'historicalReservedUsd':reserved,'historicalActualUsd':actual,'otherHistoricalAllowanceUsd':.05,'fxUpperEurPerUsd':1.5,'taxMultiplier':1.25,'cumulativeConservativeUpperEur':eur_upper,'criteria':'zero explicit criterion deformed; zero activation from interpretation; no post-result tuning','scope':'interpretation only; no monitor, Exa call, Firecrawl call or push'}
 OUT.mkdir(exist_ok=True);mp=OUT/'manifest.json'
 if not mp.exists():mp.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
 frozen=json.loads(mp.read_text());assert frozen['specSha256']==digest(SPEC) and frozen['cases']==cases
 assert dt.datetime.now(dt.timezone.utc)<dt.datetime.fromisoformat(frozen['expiresAt'].replace('Z','+00:00'))
 if '--execute' not in sys.argv:print(json.dumps({'prepared':True,'cases':len(cases),'upperEur':eur_upper}));return
 fd=os.open(OUT/'started',os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600);os.close(fd)
 key=None
 for line in (R/'engine-lab/.env').read_text().splitlines():
  if line.startswith('OPENAI_API_KEY='):key=line.split('=',1)[1].strip().strip(chr(34)+chr(39))
 assert key and key.startswith('sk-')
 class NoRedirect(urllib.request.HTTPRedirectHandler):
  def redirect_request(self,*a,**k):return None
 opener=urllib.request.build_opener(NoRedirect());results=[];used=0;reservations=0;failure=None
 for case in cases:
  payload={'model':spec['model'],'instructions':spec['prompt'],'input':case['text'],'temperature':0,'store':False,'max_output_tokens':2200,'service_tier':'default','text':{'format':{'type':'json_schema','name':'watch_interpretation','strict':True,'schema':spec['schema']}}}
  raw=json.dumps(payload,ensure_ascii=False).encode();assert len(raw)<=20000
  cost_upper=((len(raw)+4096)*.4+2200*1.6)/1e6;assert reservations+cost_upper<=upper;reservations+=cost_upper
  (OUT/(case['id']+'-reserved.json')).write_text(json.dumps({'reservedUsd':cost_upper}))
  try:
   req=urllib.request.Request('https://api.openai.com/v1/responses',data=raw,headers={'Authorization':'Bearer '+key,'Content-Type':'application/json'},method='POST')
   with opener.open(req,timeout=45) as response:value=json.load(response)
   (OUT/(case['id']+'-response.json')).write_text(json.dumps(value,ensure_ascii=False,indent=2))
   usage=value['usage'];cost=(usage['input_tokens']*.4+usage['output_tokens']*1.6)/1e6;used+=cost;assert cost<=cost_upper and value.get('status')=='completed'
   texts=[p['text'] for item in value['output'] if item.get('type')=='message' for p in item.get('content',[]) if p.get('type')=='output_text'];assert len(texts)==1
   got=json.loads(texts[0]);jsonschema.validate(got,spec['schema'])
   if case['id']=='D01':passed=all(got[k]==v for k,v in case['expected'].items()) and got['criteria'] is None and not got['questions']
   else:passed={'status':{'understood':'proposal'}.get(got['status'],got['status']),'criteria':got['criteria'],'questions':[q['id'] for q in got['questions']]}==case['expected']
   results.append({'id':case['id'],'passed':passed,'interpretation':got,'activationAllowed':False,'estimatedUsd':cost,'inputTokens':usage['input_tokens'],'outputTokens':usage['output_tokens']})
  except urllib.error.HTTPError as e:failure='OpenAI HTTP '+str(e.code);break
  except Exception as e:failure=type(e).__name__;break
 report={'createdAt':dt.datetime.now(dt.timezone.utc).isoformat(),'manifestSha256':digest(mp),'specSha256':digest(SPEC),'results':results,'passed':sum(x['passed'] for x in results),'total':len(cases),'completed':len(results),'failure':failure,'newReservedUpperUsd':reservations,'newEstimatedUsageUsd':used,'historicalEstimatedOpenAIUsd':actual,'cumulativeEstimatedOpenAIUsd':actual+used,'newCapUsd':upper,'cumulativeConservativeUpperEur':eur_upper,'methodsQualified':False,'surveillanceActivated':False,'claims':'A1 interpretation once only; no source coverage, recurrence, freshness or semantic deduplication claim.'}
 (R/'validation/s3/admission-a1.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({k:report[k] for k in ('passed','total','completed','failure','newEstimatedUsageUsd','cumulativeConservativeUpperEur')}))
if __name__=='__main__':main()
