import {discoverSources,type DiscoveryBudget} from '../supabase/functions/_shared/source-discovery.ts';
import {Firecrawl} from '../supabase/functions/_shared/firecrawl.ts';
import type {Draft,Intent} from '../supabase/functions/_shared/semantic.ts';
const assert=(x:unknown)=>{if(!x)throw new Error('assertion');};
const draft:Draft={text:'Préviens-moi des publications sur les toits végétalisés en France.',language:'fr',revision:1,mode:'continuous',answers:[]};
const intent:Intent={status:'understood',title:'Toits végétalisés',criteria:[{id:'subject',text:'Toits végétalisés',quote:'toits végétalisés'}],questions:[],queries:['toits végétalisés France'],explanation:'Sources à vérifier.'};
const budget=(events:string[]):DiscoveryBudget=>({async reserve(_,n){events.push('reserve:'+n);return true;},async settle(){events.push('settle');},async unknown(){events.push('unknown');}});
Deno.test('search snippets are never proof; each source body is read and modification time is not publication',async()=>{
 const events:string[]=[];const paths:string[]=[];
 const client=new Firecrawl('test',true,(async(url,init)=>{const body=JSON.parse(String(init?.body));paths.push(String(url));
  const data=String(url).endsWith('/search')?{web:[{url:'https://source.example/article',description:'Not a source quote'}]}:{markdown:'Article scientifique sur les effets des toits végétalisés dans les villes françaises.',metadata:{title:'Étude publique',statusCode:200,modifiedTime:'2026-10-22T10:00:00Z'}};
  assert(String(url).endsWith('/search')?body.scrapeOptions===undefined:body.formats[0]==='markdown');return new Response(JSON.stringify({success:true,data}));}) as typeof fetch);
 const r=await discoverSources(client,draft,intent,budget(events));assert(r.sources.length===1&&r.sources[0].publication===null&&r.limited);assert(paths.length===2&&events.join(',')==='reserve:2,settle,reserve:1,settle');
});
Deno.test('missing capacity or unresolved intent causes no provider HTTP',async()=>{
 let calls=0;const client=new Firecrawl('test',true,(async()=>{calls++;throw new Error('unexpected');}) as typeof fetch);
 for(const i of [intent,{...intent,status:'clarify' as const}]){let refused=false;try{await discoverSources(client,draft,i,{...budget([]),async reserve(){return false;}});}catch{refused=true;}assert(refused);}assert(calls===0);
});
Deno.test('supplied URL does not silently remove wider search; lost POST is never retried',async()=>{
 const paths:string[]=[];const e:string[]=[];const client=new Firecrawl('test',true,(async(url)=>{paths.push(String(url));throw new Error('lost');}) as typeof fetch);
 let failed=false;try{await discoverSources(client,{...draft,text:draft.text+' https://source.example/page'},intent,budget(e));}catch{failed=true;}
 assert(failed&&paths.length===1&&paths[0].endsWith('/search')&&e.filter(x=>x==='unknown').length===1);
});

Deno.test('one inaccessible source is reported without hiding another readable page or retrying it',async()=>{
 const paths:string[]=[];const events:string[]=[];const client=new Firecrawl('test',true,(async(url,init)=>{
  paths.push(String(url));const body=JSON.parse(String(init?.body));
  if(String(url).endsWith('/search'))return new Response(JSON.stringify({success:true,data:{web:[{url:'https://source.example/blocked'},{url:'https://source.example/accessible'}]}}));
  if(body.url.endsWith('/blocked'))return new Response('{}',{status:408});
  return new Response(JSON.stringify({success:true,data:{markdown:'Public source body with sufficient detail on toits végétalisés in France.',metadata:{statusCode:200}}}));
 }) as typeof fetch);
 const r=await discoverSources(client,draft,intent,budget(events));
 assert(r.state==='retrieved'&&r.sources.length===1&&r.failures.length===1&&r.failures[0].httpStatus===408&&paths.length===3&&events.filter(v=>v==='unknown').length===1);
});
