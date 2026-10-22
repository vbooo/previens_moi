import {parseIntent,parseJudgment,factKey,modelRequest,responseValue,type Draft,type Intent,type Judgment} from '../supabase/functions/_shared/semantic.ts';
const assert=(v:unknown)=>{if(!v)throw new Error('assertion');};
function refused(fn:()=>unknown){let failed=false;try{fn();}catch{failed=true;}assert(failed);}
const draft:Draft={text:'Préviens-moi quand Aurora annonce un concert à Nantes.',language:'fr',revision:1,mode:'continuous',answers:[]};
const intent:Intent={status:'understood',title:'Concert Aurora à Nantes',criteria:[{id:'artist',text:'Aurora',quote:'Aurora'},{id:'place',text:'Nantes',quote:'Nantes'}],questions:[],queries:['Aurora concert Nantes'],explanation:'Annonce publique à vérifier.'};
const url='https://source.example/concerts';const docs=[{url,text:'Aurora annonce un concert à Nantes le 9 mars 2027.',publication:null,changedText:'Aurora annonce un concert à Nantes le 9 mars 2027.'}];
const judgment:Judgment={status:'match',fact:docs[0].text,reason:'Les deux critères sont prouvés.',identity:{who:'Aurora',what:'concert',where:'Nantes',when:'2027-03-09',variant:null},sameAs:null,evidence:[{criterionId:'artist',url,quote:docs[0].text},{criterionId:'place',url,quote:docs[0].text}]};
Deno.test('generic intent preserves grounded criteria and requires unresolved details before source discovery',()=>{
 assert(parseIntent(intent,draft).queries.length===1);
 refused(()=>parseIntent({...intent,criteria:[{id:'x',text:'Lyon',quote:'Lyon'}]},draft));
 refused(()=>parseIntent({...intent,status:'clarify'},draft));
 refused(()=>parseIntent({...intent,criteria:[]},draft));
 const body=JSON.parse(modelRequest('intent',draft));assert(body.store===false&&body.service_tier==='default'&&body.model==='gpt-4.1-mini-2025-04-14');
 refused(()=>modelRequest('judgment',{text:'x'.repeat(28000)}));
});
Deno.test('affirmative result requires source proof for every decisive criterion and real changed text',()=>{
 assert(parseJudgment(judgment,draft,intent,docs,[]).status==='match');
 refused(()=>parseJudgment({...judgment,evidence:judgment.evidence.slice(0,1)},draft,intent,docs,[]));
 refused(()=>parseJudgment({...judgment,evidence:[...judgment.evidence,{criterionId:'place',url,quote:'Lyon'}]},draft,intent,docs,[]));
 refused(()=>parseJudgment(judgment,draft,intent,[{...docs[0],changedText:'Nouvelle couleur du pied de page.'}],[]));
 refused(()=>parseJudgment({...judgment,evidence:judgment.evidence.map(e=>({...e,url:'https://invented.example/page'}))},draft,intent,docs,[]));
});
Deno.test('uncertain and negative outcomes cannot smuggle an affirmative result or unknown prior identity',()=>{
 const no={status:'uncertain',fact:'',reason:'Lieu non prouvé.',identity:null,sameAs:null,evidence:[]};
 assert(parseJudgment(no,draft,intent,docs,[]).status==='uncertain');
 refused(()=>parseJudgment({...judgment,status:'uncertain'},draft,intent,docs,[]));
 refused(()=>parseJudgment({...judgment,sameAs:'a'.repeat(64)},draft,intent,docs,[]));
});
Deno.test('same factual identity has stable key across source prose and JSON key order',async()=>{
 const a=judgment.identity!;const b={variant:null,when:a.when,where:' NANTES ',what:'Concert',who:'Aurora'};
 assert(await factKey(a)===await factKey(b));assert(await factKey(a)!==await factKey({...a,when:'2027-03-10'}));
});
Deno.test('incomplete or unexpected model output cannot pass price or qualification checks',()=>{
 const response={status:'completed',model:'gpt-4.1-mini-2025-04-14',service_tier:'default',output:[{content:[{type:'output_text',text:JSON.stringify(intent)}]}]};
 assert((responseValue(response) as Intent).status==='understood');
 for(const bad of [{...response,status:'incomplete'},{...response,model:'unknown'},{...response,service_tier:'priority'},{...response,output:[]}])refused(()=>responseValue(bad));
});
