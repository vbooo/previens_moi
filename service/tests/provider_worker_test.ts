import {providerOne,type ProviderJob,type ProviderPort} from '../supabase/functions/_shared/provider-worker.ts';
import {Firecrawl} from '../supabase/functions/_shared/firecrawl.ts';
const assert=(v:unknown)=>{if(!v)throw new Error('assertion');};
const webhook='https://projectref.supabase.co/functions/v1/webhook';
function fixture(kind:'start'|'check'='start') {
 const now=new Date().toISOString();const job:ProviderJob={id:'00000000-0000-4000-8000-000000000001',lease_token:'lease',kind,watch_id:'watch',generation:1,plan:{method:'known_page',sources:[{url:'https://source.example/page'}],draft:{text:'Changements de cette fiche'}},providerId:null,attempt:{phase:'poll',monitor_id:'remote',check_id:'check',credits_before:100,run_intent_at:now,created_at:now}};
 const events:string[]=[];const complete:unknown[]=[];
 const check={id:'check',monitorId:'remote',status:'completed',startedAt:now,summary:{changed:0},pages:[{url:'https://source.example/page',status:'same'}]};
 let event:Record<string,unknown>|null={id:'check',type:'monitor.check.completed',success:true,data:[{monitorId:'remote',checkId:'check',status:'completed',summary:check.summary}]};
 const port:ProviderPort={async enqueueDue(){events.push('enqueue');},async lease(){events.push('lease');return job;},async checkpoint(_,e){events.push(e);return true;},async allowed(){return true;},async settle(){events.push('settle');},async event(){return event;},async complete(_,observed,outcome,credits){complete.push({observed,outcome,credits});}};
 const requests:string[]=[];const client=new Firecrawl('test-key',true,(async(url,init)=>{const path=String(url);requests.push(init?.method??'GET');let data:unknown=path.endsWith('/credit-usage')?{remainingCredits:99}:path.endsWith('/checks/check')?check:{id:'remote',name:'test',status:'paused',currentCheckId:null};return new Response(JSON.stringify({success:true,data}));}) as typeof fetch);
 return {job,events,complete,check,port,client,requests,setEvent(value:Record<string,unknown>|null){event=value;}};
}
Deno.test('provider execution requires dedicated public receiver and does not lease otherwise',async()=>{const f=fixture();assert(await providerOne(f.port,f.client,true,'http://localhost/webhook')==='disabled');assert(f.events.length===0);assert(await providerOne(f.port,f.client,false,webhook)==='disabled');});
Deno.test('baseline waits for signed completion then remains silent',async()=>{const f=fixture();f.setEvent(null);assert(await providerOne(f.port,f.client,true,webhook)==='awaiting_signed_completion');assert(f.complete.length===0&&f.events.includes('later'));const good=fixture();assert(await providerOne(good.port,good.client,true,webhook)==='baseline');assert((good.complete[0] as any).outcome==='baseline'&&(good.complete[0] as any).credits===1);});
Deno.test('same check succeeds; unqualified semantic change becomes visible failure',async()=>{for(const changed of [false,true]){const f=fixture('check');f.check.pages[0].status=changed?'changed':'same';assert(await providerOne(f.port,f.client,true,webhook)==='checked');assert((f.complete[0] as any).outcome===(changed?'failed':'no_change'));}});
Deno.test('lost run intent never triggers another provider POST',async()=>{const f=fixture();f.job.attempt!.phase='run';assert(await providerOne(f.port,f.client,true,webhook)==='unknown');assert(f.requests.length===0&&f.events.includes('unknown'));});
Deno.test('source mismatch is visible and cannot confirm baseline or create a result',async()=>{const f=fixture();f.check.pages[0].url='https://foreign.example/page';assert(await providerOne(f.port,f.client,true,webhook)==='unknown');assert(f.complete.length===0);});

Deno.test('qualified literal page changes produce verified findings; missing evidence is a failed check',async()=>{
 for(const valid of [true,false]) {
  const f=fixture('check');Object.assign(f.job.plan,{id:'00000000-0000-4000-8000-000000000002',version:1,criteria:['Ajouts et suppressions de texte sur la page indiquée.']});Object.assign(f.job.plan.draft,{language:'fr'});
  f.job.watch_id='00000000-0000-4000-8000-000000000003';
  Object.assign(f.check.pages[0],{status:'changed',diff:{text:valid?'@@ -1 +1 @@\n-Avant\n+Après':'not a diff'}});
  let saved:any;f.port.complete=async(_,__,outcome,credits,proof)=>{saved={outcome,credits,proof};};
  assert(await providerOne(f.port,f.client,true,webhook)==='checked');assert(saved.credits===1);
  assert(valid?saved.outcome==='finding'&&saved.proof.finding.evidence[0].quote.includes('+ Après'):saved.outcome==='failed'&&!saved.proof);
 }
});
