#!/usr/bin/env python3
"""Reconcile completed request reservations against saved provider token usage.
No network, no invoice claim, no release of missing or ambiguous request evidence.
"""
from pathlib import Path
from decimal import Decimal as D
import json,sqlite3
R=Path(__file__).resolve().parents[1]
def load(p):return json.loads(p.read_text())
def cost(usage):
 a,b=usage['input_tokens'],usage['output_tokens']
 assert type(a) is int and type(b) is int and a>=0 and b>=0
 return (D(a)*D('.4')+D(b)*D('1.6'))/D(1_000_000)
def main():
 seen=set();actual=D(0);released=D(0);count=0
 history=R/'engine-lab/reports/live-openai-20261009'
 with sqlite3.connect('file:'+str(history/'ledger.sqlite')+'?mode=ro',uri=True) as c:
  rows={r[0]:r[1:] for r in c.execute('select label,status,reserved,actual,input_tokens,output_tokens from calls')}
 for f in (history/'raw').glob('*.json'):
  raw=load(f);v=raw['response'];label=raw['label'];assert label in rows
  status,reserved,charged,a,b=rows.pop(label)
  assert status==v['status']=='completed' and v['model']=='gpt-4.1-mini-2025-04-14' and v['service_tier']=='default'
  assert v['id'] not in seen;seen.add(v['id'])
  assert v['usage']['input_tokens']==a and v['usage']['output_tokens']==b
  measured=cost(v['usage']);assert abs(measured-D(str(charged)))<D('.000000000001') and measured<=D(str(reserved))
  actual+=measured;released+=D(str(reserved))-measured;count+=1
 assert not rows and count==83
 campaigns=[]
 for n in range(1,6):
  report=load(R/f'validation/s3/admission-a{n}.json');folder=R/'.local'/('a1-20261020' if n==1 else f'a{n}-20261021')
  assert report['completed']==report['total']==9 and report['failure'] is None
  subtotal=D(0);reserve=D(0)
  for case in report['results']:
   v=load(folder/(case['id']+'-response.json'));reservation=load(folder/(case['id']+'-reserved.json'))
   assert v['status']=='completed' and v['model']=='gpt-4.1-mini-2025-04-14' and v['service_tier']=='default'
   assert v['id'] not in seen;seen.add(v['id']);measured=cost(v['usage'])
   assert v['usage']['input_tokens']==case['inputTokens'] and v['usage']['output_tokens']==case['outputTokens']
   upper=D(str(reservation['reservedUsd']));assert measured<=upper
   subtotal+=measured;reserve+=upper;count+=1
  assert abs(subtotal-D(str(report['newEstimatedUsageUsd'])))<D('.000000000001')
  actual+=subtotal;released+=reserve-subtotal;campaigns.append({'id':f'A{n}','completedCalls':9,'usageUpperUSD':float(subtotal),'formerReservedUSD':float(reserve)})
 assert count==128 and actual==D('.081134')
 allowance=D('.05');fx=D('1.5');tax=D('1.25');base=(actual+allowance)*fx*tax
 proof={'date':'2026-10-22','scope':'Saved completed request usage reconciliation, not invoice reconciliation','completedResponsesMatched':count,'missingOrAmbiguousHistoricalRequests':0,'model':'gpt-4.1-mini-2025-04-14','tier':'default','uncachedInputUSDPerMillion':.4,'outputUSDPerMillion':1.6,'pricingSource':'https://developers.openai.com/api/docs/models/gpt-4.1-mini','usageUpperUSD':float(actual),'completedReservationExcessReleasedUSD':float(released),'otherHistoricalAllowanceUSD':float(allowance),'fxUpperEURPerUSD':float(fx),'taxMultiplier':float(tax),'settledConservativeUpperEUR':float(base),'invoiceReconciled':False,'campaigns':campaigns,'noNetworkCalls':True,'authorizationMaxEUR':2,'newCallsMade':0}
 p=R/'validation/s3/cost-reconciliation.json';p.write_text(json.dumps(proof,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({'matched':count,'unknown':0,'usageUpperUSD':float(actual),'settledConservativeUpperEUR':float(base),'invoiceReconciled':False,'newCallsMade':0}))
if __name__=='__main__':main()
