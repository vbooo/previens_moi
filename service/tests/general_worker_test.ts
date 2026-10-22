import {generalOne,type GeneralWork,type GeneralPort} from '../supabase/functions/_shared/general-worker.ts';
import {MODEL,type Draft,type Intent} from '../supabase/functions/_shared/semantic.ts';
import {parseCoverage,coverageRequest} from '../supabase/functions/_shared/coverage.ts';
import {Firecrawl} from '../supabase/functions/_shared/firecrawl.ts';
const assert=(v:unknown)=>{if(!v)throw new Error('assertion');};
const draft:Draft={text:'Préviens-moi quand le Luma neuf passe sous 200 euros livré en France.',revision:1,language:'fr',mode:'continuous',answers:[]};
const intent:Intent={status:'understood',title:'Luma sous 200 euros',criteria:[{id:'product',text:'Luma neuf',quote:'Luma neuf'},{id:'price',text:'Sous 200 euros livré en France',quote:'sous 200 euros livré en France'}],queries:['Luma neuf prix France'],questions:[],explanation:'Offres publiques à vérifier.'};
const url='https://source.example/luma';const text='Luma neuf : 249 euros, livraison en France incluse. Stock disponible.';
const doc={url,label:'Luma',text,publication:null,changedText:null,retrieval:'search_scrape' as const};
const memory={generation:1,baselineAt:null,observedAt:null,completed:false,facts:[]};
function job(phase:GeneralWork['phase'],kind:GeneralWork['kind']='admission'):GeneralWork{return {id:'work',lease:'lease',kind,phase,generation:1,watchId:'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',context:{draft,intent,documents:[doc],memory,cycle:0,plan:{id:'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',version:1,criteria:intent.criteria.map(c=>c.text)}}};}
function port(work:GeneralWork){const events:any[]=[];const p:GeneralPort={async lease(){events.push('reserved');return work;},async finish(_,result,actual,credits){events.push({result,actual,credits});}};return {p,events};}
function reply(value:unknown){return new Response(JSON.stringify({status:'completed',model:MODEL,service_tier:'default',usage:{input_tokens:100,output_tokens:100},output:[{content:[{type:'output_text',text:JSON.stringify(value)}]}]}));}
const unused=new Firecrawl('test',true,(async()=>{throw new Error('unexpected provider call');}) as typeof fetch);
Deno.test('coverage assesses observability rather than requiring a target price to have occurred already',()=>{
 const coverage=parseCoverage({status:'observable',reason:'The product, current price and delivery country are exposed.',evidence:[{criterionId:'product',passageId:'d0p0'},{criterionId:'price',passageId:'d0p0'}]},intent,[doc]);assert(coverage.evidence[0].quote===text);
 let bad=false;try{parseCoverage({status:'observable',reason:'Invented.',evidence:[{criterionId:'product',passageId:'missing'}]},intent,[doc]);}catch{bad=true;}assert(bad);
 const body=JSON.parse(coverageRequest(draft,intent,[doc]));assert(body.store===false&&body.model===MODEL);
});
Deno.test('general interpretation leases before model HTTP and private requests never trigger source collection',async()=>{
 const {p,events}=port(job('interpret'));const request=(async()=>{assert(events[0]==='reserved');return reply({status:'unsupported',title:'Compte privé',criteria:[],queries:[],questions:[],explanation:'Compte privé inaccessible.'});}) as typeof fetch;
 assert(await generalOne(p,'test',unused,true,request)==='admission:interpret');assert(events[1].result.intent.status==='unsupported'&&events[1].actual===200);
});
Deno.test('usable source coverage yields a bounded contract-valid proposal without activating a watch',async()=>{
 const {p,events}=port(job('decide'));await generalOne(p,'test',unused,true,(async()=>reply({status:'observable',reason:'Prix et livraison observables.',evidence:[{criterionId:'product',passageId:'d0p0'},{criterionId:'price',passageId:'d0p0'}]})) as typeof fetch);
 const result=events[1].result;assert(result.proposal.method==='web_search'&&result.proposal.partial&&result.proposal.intervalHours===24&&!('watch' in result));
});
Deno.test('collection is one search and one actual body, within a pre-reserved three-credit bound',async()=>{
 const {p,events}=port(job('collect'));let calls=0;const client=new Firecrawl('test',true,(async(url)=>{assert(events[0]==='reserved');calls++;return new Response(JSON.stringify({success:true,data:String(url).endsWith('/search')?{web:[{url:'https://source.example/luma'}]}:{markdown:text,metadata:{statusCode:200}}}));}) as typeof fetch);
 await generalOne(p,'test',client,true);assert(calls===2&&events[1].credits===3&&events[1].result.documents[0].text===text);
});
Deno.test('lost paid response is retained as unknown and never retried automatically',async()=>{
 const {p,events}=port(job('interpret'));let calls=0;const state=await generalOne(p,'test',unused,true,(async()=>{calls++;throw new Error('lost');}) as typeof fetch);assert(state==='failed'&&calls===1&&events[1].actual===null);
});
Deno.test('unchanged page avoids a model call and does not create an affirmative result',async()=>{
 const work=job('decide','check');work.context.memory={...memory,baselineAt:'2026-10-20T00:00:00Z',observedAt:'2026-10-20T00:00:00Z'};work.context.documents=[{...doc,changedText:''}];work.context.retrievalHealthy=true;
 const {p,events}=port(work);await generalOne(p,'test',unused,true,(async()=>{throw new Error('No model expected');}) as typeof fetch);assert(events[1].result.decision.outcome==='no_change'&&events[1].actual===0&&!events[1].result.findings.length);
});
Deno.test('mismatching source is an explicit unsupported coverage result, never a ready plan',async()=>{
 const {p,events}=port(job('decide'));await generalOne(p,'test',unused,true,(async()=>reply({status:'unobservable',reason:'La page ne permet pas de vérifier la livraison.',evidence:[]})) as typeof fetch);assert(events[1].result.coverage.status==='unobservable'&&!events[1].result.proposal);
});
