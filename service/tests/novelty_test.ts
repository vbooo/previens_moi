import {decideNovelty,type Memory,type Assessment} from '../supabase/functions/_shared/novelty.ts';
import type {Draft,Intent} from '../supabase/functions/_shared/semantic.ts';
const assert=(v:unknown)=>{if(!v)throw new Error('assertion');};
const draft:Draft={text:'Préviens-moi des concerts de Mira à Tours.',revision:1,language:'fr',mode:'continuous',answers:[]};
const intent:Intent={status:'understood',title:'Mira à Tours',criteria:[{id:'event',text:draft.text,quote:draft.text}],queries:['Mira Tours concert'],questions:[],explanation:'Sources publiques.'};
const memory:Memory={generation:1,baselineAt:null,observedAt:null,completed:false,facts:[]};
function observation(date:string,url='https://source.example/events',publication:string|null=null,changed=true):Assessment {
 const text='Mira annonce un concert à Tours le '+date+'.';return {documents:[{url,text,publication,changedText:changed?text:null}],response:{status:'match',fact:text,reason:'Annonce officielle.',identity:{who:'Mira',what:'concert',where:'Tours',when:date,variant:null},sameAs:null,evidence:[{criterionId:'event',url,quote:text}]}};
}
const t0='2026-10-22T08:00:00Z',t1='2026-10-22T09:00:00Z',t2='2026-10-22T10:00:00Z';
Deno.test('silent baseline, new event, cross-source repeat and restart preserve novelty',async()=>{
 const base=await decideNovelty(memory,1,draft,intent,[observation('2027-02-01')],t0,true);assert(base.outcome==='baseline'&&!base.findings.length);
 const next=await decideNovelty(base.memory,1,draft,intent,[observation('2027-02-02')],t1,true);assert(next.outcome==='finding'&&next.findings.length===1);
 const restarted=JSON.parse(JSON.stringify(next.memory));const duplicate=await decideNovelty(restarted,1,draft,intent,[observation('2027-02-02','https://other.example/report')],t2,true);assert(duplicate.outcome==='no_change'&&!duplicate.findings.length);
});
Deno.test('newly discovered old/undated documents are not new publications; dated new article is',async()=>{
 const base={...memory,baselineAt:t0,observedAt:t0};
 for(const publication of [null,'2025-01-01T10:00:00Z','2026-10-23T10:00:00Z']){
  const r=await decideNovelty(base,1,draft,intent,[observation('2027-02-02',undefined,publication,false)],t1,true);assert(r.outcome==='degraded'&&!r.findings.length&&!r.memory.facts.length);
 }
 const fresh=await decideNovelty(base,1,draft,intent,[observation('2027-02-02',undefined,'2026-10-22T08:30:00Z',false)],t1,true);assert(fresh.outcome==='finding');
});
Deno.test('incomplete initial read cannot become an active baseline; failures stay distinct from no news',async()=>{
 const bad=await decideNovelty(memory,1,draft,intent,[observation('2027-02-02')],t0,false);assert(bad.outcome==='degraded'&&bad.memory.baselineAt===null&&!bad.memory.facts.length);
 const empty=await decideNovelty(memory,1,draft,intent,[],t0,true);assert(empty.outcome==='degraded');
 const uncertain=observation('2027-02-02');uncertain.response={status:'uncertain',fact:'',reason:'Place missing',identity:null,sameAs:null,evidence:[]};
 assert((await decideNovelty(memory,1,draft,intent,[uncertain],t0,true)).memory.baselineAt===null);
});
Deno.test('one-off finishes only on a proven novelty; stale generation cannot decide delivery',async()=>{
 const base={...memory,baselineAt:t0,observedAt:t0};const r=await decideNovelty(base,1,{...draft,mode:'once'},intent,[observation('2027-02-02'),observation('2027-02-03')],t1,true);assert(r.memory.completed&&r.findings.length===1);
 for(const [m,g,time] of [[base,2,t1],[base,1,t0],[r.memory,1,t2]] as const){let rejected=false;try{await decideNovelty(m,g,draft,intent,[],time,true);}catch{rejected=true;}assert(rejected);}
});
