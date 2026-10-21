import {Ajv2020} from 'npm:ajv@8.17.1/dist/2020.js';
import spec from './admission-spec.json' with {type:'json'};
import {literalRetail} from './literal-retail.ts';
export const admissionSpec=spec;
const validateResponse=new Ajv2020({strict:false}).compile(spec.responseSchema);
const validate=new Ajv2020({strict:false}).compile(spec.schema);
export interface Question {id:string;label:string;options:string[];required:boolean}
export interface Interpretation {family:'retail'|'concert'|'private'|'other';status:'understood'|'clarify'|'unsupported';criteria:Record<string,unknown>|null;subject:string|null;location:string|null;questions:Question[];explanation:string}
export function interpretation(value:unknown):Interpretation {
 if(!validate(value))throw new Error('Invalid interpretation');
 const v=value as unknown as Interpretation;
 if(v.explanation.length>2000||v.questions.length>8||new Set(v.questions.map(x=>x.id)).size!==v.questions.length||v.questions.some(x=>!x.id||x.id.length>80||!x.label||x.label.length>500))throw new Error('Unbounded interpretation');
 if(v.status==='clarify' && (!v.questions.length||v.criteria!==null))throw new Error('Inconsistent clarification');
 if(v.status!=='clarify' && v.questions.length)throw new Error('Unexpected questions');
 if(v.family==='private' && v.status!=='unsupported')throw new Error('Private access unavailable');
 if(v.status==='understood' && v.family==='retail' && v.criteria===null)throw new Error('Missing criteria');
 if(v.status==='understood' && v.family==='concert' && (!v.subject||!v.location))throw new Error('Missing event scope');
 return v;
}
/** Supplied only by independently authenticated source/qualification ports, never by the model. */
export interface Feasibility {state:'verified'|'missing'|'inaccessible'|'temporary_failure';method:string;qualified:boolean;coversWholeRequest:boolean;allDecisiveCriteriaObservable:boolean;sourceUrls:string[]}
export function admission(v:Interpretation,f:Feasibility):'clarification'|'unsupported'|'source_required'|'insufficient_coverage'|'temporary_failure'|'ready' {
 if(v.status==='unsupported')return 'unsupported';
 if(v.status==='clarify')return 'clarification';
 if(f.state==='temporary_failure')return 'temporary_failure';
 if(f.state==='missing')return 'source_required';
 if(f.state!=='verified'||!f.coversWholeRequest||!f.allDecisiveCriteriaObservable||!f.sourceUrls.length)return 'insufficient_coverage';
 if(!f.qualified)return 'unsupported';
 if(!f.sourceUrls.every(u=>{try{const url=new URL(u);return url.protocol==='https:'&&!url.username&&!url.password;}catch{return false;}}))throw new Error('Invalid verified source');
 return 'ready';
}
export function admissionRequest(input:unknown) {
 const body={model:spec.model,instructions:spec.prompt,input:JSON.stringify(input),store:false,temperature:0,max_output_tokens:spec.max_output_tokens,service_tier:'default',text:{format:{type:'json_schema',name:'watch_interpretation',strict:true,schema:spec.responseSchema}}};
 const raw=JSON.stringify(body);if(new TextEncoder().encode(raw).length>20000)throw new Error('Admission input too large');return raw;
}
export function parseAdmissionResponse(response:Record<string,unknown>,input?:unknown):Interpretation {
 if(response.status!=='completed')throw new Error('Incomplete admission');
 const output=response.output as {content?:{type:string;text?:string}[]}[];
 const texts=output?.flatMap(x=>x.content??[]).filter(x=>x.type==='output_text');
 if(texts?.length!==1||!texts[0].text)throw new Error('Missing admission text');
 const wire=JSON.parse(texts[0].text);
 if(!validateResponse(wire))throw new Error('Invalid admission decision');
 const raw=wire.decision as unknown as Interpretation;
 if(raw.family==='retail') {const literal=literalRetail(input,typeof wire.manufacturer==='string'?wire.manufacturer:null);if(literal)return interpretation(literal);}
 if(raw.family==='retail'&&raw.status==='clarify'&&raw.questions.some(q=>q.id==='product')&&raw.questions.some(q=>q.id==='budget'))raw.questions=['product','budget'].map(id=>raw.questions.find(q=>q.id===id)!);
 if(raw.family==='retail'&&raw.status==='understood') {
  const draft=input as {text?:unknown;answers?:unknown[];language?:string}|undefined;
  const strings=(v:unknown):string[]=>typeof v==='string'?[v]:Array.isArray(v)?v.flatMap(strings):v&&typeof v==='object'?Object.values(v).flatMap(strings):[];
  const sources=typeof input==='string'?[input]:[...strings(draft?.text),...strings(draft?.answers)];
  const evidence=wire.retail_evidence as {delivery_country:string|null;conditions:{state:string;quote:string}[]}|null;
  const literal=(quote:string|null|undefined)=>!!quote?.trim()&&sources.some(s=>s.includes(quote!));
  const norm=(s:string)=>s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
  const zone=String(raw.criteria?.delivery_zone??'');
  const country=evidence?.delivery_country;
  const countryGrounded=literal(country)&&(/^[\p{L}]/u.test(country!)&&(zone==='FR-METRO'?/^(mainland france|france metropolitaine)$/i.test(norm(country!)):norm(zone)===norm(country!)));
  const words:Record<string,RegExp>={new:/^(new|neuf|neuve|neufs|neuves)$/i,refurbished:/^(refurbished|reconditionne|reconditionnee|reconditionnes|reconditionnees)$/i,used:/^(used|second.hand|d'occasion|occasion)$/i};
  const states=evidence?.conditions??[];
  const valid=states.every(c=>literal(c.quote)&&words[c.state]?.test(norm(c.quote)))&&new Set(states.map(c=>c.state)).size===states.length;
  if(!valid)throw new Error('Unsupported condition evidence');
  const questions:Question[]=[];const fr=draft?.language==='fr'||(typeof input==='string'&&!/^Notify\b/i.test(input));
  if(!countryGrounded)questions.push({id:'delivery_zone',label:fr?'Dans quel pays faut-il livrer ?':'Which delivery country?',options:[],required:true});
  if(!states.length)questions.push({id:'condition',label:fr?'Quel état acceptez-vous ?':'Which product condition do you accept?',options:[],required:true});
  if(questions.length)return interpretation({...raw,status:'clarify',criteria:null,questions,explanation:fr?'Précisez les critères manquants.':'Please specify the missing criteria.'});
  raw.criteria={...raw.criteria,condition:states.map(c=>c.state)};
 }
 // Remove only a grounded lower-case French contraction after a preposition.
 // Upper-case initial L in a venue's proper name is preserved.
 const source=typeof input==='string'?input:(input as {text?:string}|undefined)?.text??'';
 if(raw.family==='concert'&&raw.subject&&/^l[’']\p{Lu}/u.test(raw.subject)&&source.includes('à '+raw.subject))raw.subject=raw.subject.slice(2);
 const decision=interpretation(raw);
 const brand=typeof wire.manufacturer==='string'?wire.manufacturer.trim().toLocaleLowerCase():null;
 const model=decision.criteria?.model;
 if(brand&&typeof model==='string'&&model.trim().toLocaleLowerCase().startsWith(brand+' '))throw new Error('Manufacturer included in reference');
 return decision;
}
