import {Firecrawl,ProviderFault,reconcile,runBounded,type Check,type RunPort} from '../supabase/functions/_shared/firecrawl.ts';
const assert=(x:unknown)=>{if(!x)throw new Error('assertion');};
const response=(data:unknown)=>new Response(JSON.stringify({success:true,data}));
Deno.test('disabled provider and invalid path emit no HTTP and never reveal key',async()=>{let calls=0;const req=(async()=>{calls++;return response({});}) as typeof fetch;for(const client of [new Firecrawl('private-key',false,req),new Firecrawl('private-key',true,req)]){try{await client.call('POST','/v2/monitor/../../foreign');throw new Error('unexpected');}catch(e){assert(e instanceof ProviderFault&&!String(e).includes('private-key'));}}assert(calls===0);});
Deno.test('stop requires authenticated paused monitor and no current check',async()=>{for(const current of [null,'running-check']){const calls:string[]=[];const client=new Firecrawl('key',true,(async(url,init)=>{calls.push(init?.method??'GET');return response({id:'monitor',name:'test',status:'paused',currentCheckId:current,nextRunAt:null});}) as typeof fetch);let pending=false;try{await client.pause('monitor');}catch(e){pending=e instanceof ProviderFault&&e.state==='pending';}assert(pending===(current!==null));assert(calls.join(',')==='PATCH,GET');}});
Deno.test('signed completion must match authenticated API monitor check source and summary',()=>{const c:Check={id:'check',monitorId:'monitor',status:'completed',startedAt:'2026-10-20T10:00:00Z',summary:{changed:1},pages:[{url:'https://source.example/page',status:'changed',judgment:{meaningful:true}}]};const e={id:'check',type:'monitor.check.completed',success:true,data:[{monitorId:'monitor',checkId:'check',status:'completed',summary:c.summary}]};assert(reconcile(e,c,'monitor','https://source.example/page').outcome==='candidate_requires_evidence');for(const bad of [{...c,monitorId:'foreign'},{...c,summary:{changed:0}},{...c,pages:[{url:'https://other.example',status:'same'}]}]){let failed=false;try{reconcile(e,bad,'monitor','https://source.example/page');}catch{failed=true;}assert(failed);}});
Deno.test('lost run response is unknown, pause still occurs and no second run is sent',async()=>{const methods:string[]=[];let unknown=0;const client=new Firecrawl('key',true,(async(url,init)=>{const path=String(url);methods.push((init?.method??'GET')+' '+path.split('/v2/')[1]);if(path.endsWith('/credit-usage'))return response({remainingCredits:100});if(path.endsWith('/run'))throw new Error('lost');return response({id:'monitor',name:'test',status:'paused',currentCheckId:null,nextRunAt:'2027-01-01T00:00:00Z'});}) as typeof fetch);const records:string[]=[];const port:RunPort={async reserve(n){assert(n===2);records.push('reserve');},async allowed(){return true;},async recordRunIntent(){records.push('intent');},async recordCheck(){throw new Error('no response');},async settle(){throw new Error('unknown cost');},async unknown(){unknown++;}};try{await runBounded(client,'monitor',port,Date.parse('2026-10-20T00:00:00Z'));}catch{/* expected */}assert(unknown===1&&records.join(',')==='reserve,intent');assert(methods.filter(m=>m.startsWith('POST')).length===1&&methods.at(-1)==='PATCH monitor/monitor');});
Deno.test('near automatic schedule is refused before reservation or unpause',async()=>{let reserved=false;const client=new Firecrawl('key',true,(async()=>response({id:'monitor',name:'test',status:'paused',currentCheckId:null,nextRunAt:'2026-10-20T00:01:00Z'})) as typeof fetch);try{await runBounded(client,'monitor',{async reserve(){reserved=true;}} as unknown as RunPort,Date.parse('2026-10-20T00:00:00Z'));}catch{/* expected */}assert(!reserved);});
Deno.test('lost creation is reconciled by service identity and never recreated',async()=>{
 const {createPaused}=await import('../supabase/functions/_shared/firecrawl.ts');let posts=0;const job='00000000-0000-4000-8000-000000000001';let remembered='';
 const client=new Firecrawl('key',true,(async(url,init)=>{
  if(init?.method==='POST'){posts++;throw new Error('response lost');}
  if(String(url).endsWith('/v2/monitor'))return response([{id:'remote',name:'previens-moi:'+job,status:'paused'}]);
  return response({id:'remote',name:'previens-moi:'+job,status:'paused',currentCheckId:null,nextRunAt:'2027-01-01T00:00:00Z'});
 }) as typeof fetch);
 const port={async begin(){return false;},async remember(id:string){remembered=id;},async unknown(){throw new Error('unexpected');}};
 assert(await createPaused(client,job,'https://source.example/page','Public changes','https://service.example/webhook',port)==='remote');assert(posts===0&&remembered==='remote');
});
Deno.test('remote erasure confirms absence; a lost DELETE is reconciled without claiming completion',async()=>{
 let removed=false,deletes=0;
 const client=new Firecrawl('key',true,(async(_,init)=>{
  if(removed)return new Response('{}',{status:404});
  if(init?.method==='DELETE'){removed=true;deletes++;throw new Error('lost deletion response');}
  return response({id:'remote',name:'test',status:'paused',currentCheckId:null});
 }) as typeof fetch);
 let unknown=false;try{await client.erase('remote');}catch{unknown=true;}assert(unknown&&deletes===1);await client.erase('remote');assert(deletes===1);
});
Deno.test('429 retries one safe pause after provider delay but never repeats a billed POST',async()=>{
 for(const method of ['PATCH','POST']) {
  let calls=0;const waits:number[]=[];
  const client=new Firecrawl('key',true,(async()=>{calls++;return calls===1?new Response('{}',{status:429,headers:{'Retry-After':'12'}}):response({});}) as typeof fetch,async ms=>{waits.push(ms);});
  let rejected=false;try{await client.call(method,'/v2/monitor/monitor',method==='PATCH'?{status:'paused'}:{});}catch{rejected=true;}
  assert(method==='PATCH'?!rejected&&calls===2&&waits[0]===12000:rejected&&calls===1&&waits.length===0);
 }
});
Deno.test('persistent rate limit has a bounded safe retry',async()=>{
 let calls=0;const client=new Firecrawl('key',true,(async()=>{calls++;return new Response('{}',{status:429,headers:{'Retry-After':'1'}});}) as typeof fetch,async()=>{});
 let refused=false;try{await client.monitor('monitor');}catch(e){refused=e instanceof ProviderFault&&e.state==='rate_limited';}assert(refused&&calls===2);
});

Deno.test('jsonb key order cannot reject matching signed check counters',()=>{
 const c:Check={id:'check',monitorId:'monitor',status:'completed',startedAt:'2026-10-21 19:03:49.243+00',summary:{totalPages:1,same:0,changed:0,new:1,removed:0,error:0},pages:[{url:'https://source.example/page',status:'new'}]};
 const summary={new:1,same:0,error:0,changed:0,removed:0,totalPages:1};
 const e={id:'check',type:'monitor.check.completed',success:true,data:[{monitorId:'monitor',checkId:'check',status:'completed',summary}]};
 assert(reconcile(e,c,'monitor','https://source.example/page').observedAt==='2026-10-21T19:03:49.243Z');
 for(const bad of [{...summary,totalPages:2},{...summary,error:-1},{...summary,foreign:0}]) {
  let refused=false;try{reconcile({...e,data:[{...e.data[0],summary:bad}]},c,'monitor','https://source.example/page');}catch{refused=true;}assert(refused);
 }
});
