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
Deno.test('grounded capability and country evidence cannot turn into a web-ready private request or infer a delivery country',()=>{
 const wire={accessMode:'private_account',privateEvidence:'mon compte bancaire privé',deliveryRequiredEvidence:null,deliveryCountryEvidence:null,contradictionEvidence:null,subjectivityEvidence:null,decision:{...intent,status:'unsupported',criteria:[],queries:[]}};
 assert(parseIntent(wire,{...draft,text:'Préviens-moi quand mon compte bancaire privé reçoit 50 €.'}).status==='unsupported');
 refused(()=>parseIntent({...wire,privateEvidence:'an invented phrase'},draft));
 const country={...wire,accessMode:'public_web',privateEvidence:null,deliveryRequiredEvidence:'delivered',decision:{...intent,criteria:[{id:'p',text:'Delivered',quote:'delivered'}]}};
 const result=parseIntent(country,{...draft,text:'Phone below £180 delivered.'});assert(result.status==='clarify'&&result.questions[0].id==='delivery_zone'&&!result.queries.length);
});
Deno.test('wire schema forbids affirmative payloads on negative decisions',()=>{
 const no={decision:{status:'no_match',fact:'',reason:'Wrong city.',identity:null,sameAs:null,evidence:[]}};
 assert(parseJudgment(no,draft,intent,docs,[]).status==='no_match');
 refused(()=>parseJudgment({decision:{...judgment,status:'no_match'}},draft,intent,docs,[]));
});
Deno.test('a duplicate suggestion cannot merge a distinct event date or location',()=>{
 const previous=[{key:'a'.repeat(64),identity:{...judgment.identity!,when:'2027-03-08'},text:'Earlier event'}];
 const result=parseJudgment({...judgment,sameAs:previous[0].key},draft,intent,docs,previous);assert(result.sameAs===null);
});
Deno.test('wrapping quote marks can be removed only when the inner text is literally grounded',()=>{
 const wire={accessMode:'public_web',privateEvidence:null,deliveryRequiredEvidence:null,deliveryCountryEvidence:null,contradictionEvidence:null,subjectivityEvidence:null,decision:{...intent,criteria:intent.criteria.map(c=>({...c,quote:'"'+c.quote+'"'}))}};
 assert(parseIntent(wire,draft).status==='understood');
 refused(()=>parseIntent({...wire,decision:{...intent,criteria:[{id:'x',text:'Wrong',quote:'"invented"'}]}},draft));
});

Deno.test('model references resolve only to existing source passages; fabricated references cannot become quotes',()=>{
 const wire={decision:{...judgment,evidence:[{criterionId:'artist',passageId:'d0p0'},{criterionId:'place',passageId:'d0p0'}]}};
 const result=parseJudgment(wire,draft,intent,docs,[]);assert(result.evidence[0].quote===docs[0].text);
 refused(()=>parseJudgment({decision:{...wire.decision,evidence:[{criterionId:'artist',passageId:'invented'}]}},draft,intent,docs,[]));
 const request=JSON.parse(modelRequest('judgment',{draft,intent,documents:docs,previous:[]}));
 assert(JSON.parse(request.input).documents[0].passages[0].text===docs[0].text&&request.text.format.schema.properties.decision.anyOf[0].properties.evidence.items.properties.passageId.enum[0]==='d0p0');
});

Deno.test('short real additions remain selectable inside a longer unchanged source body',()=>{
 const long=[{...docs[0],text:'Old header. '.repeat(30)+docs[0].text+' Older footer. '.repeat(20)}];
 const request=JSON.parse(modelRequest('judgment',{draft,intent,documents:long,previous:[]}));const p=JSON.parse(request.input).documents[0].passages.find((p:any)=>p.changed);assert(p&&p.text===docs[0].text);
 const parsed=parseJudgment({decision:{...judgment,evidence:[{criterionId:'artist',passageId:p.id},{criterionId:'place',passageId:p.id}]}},draft,intent,long,[]);assert(parsed.status==='match');
});
