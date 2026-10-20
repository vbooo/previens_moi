import {Ajv2020} from 'npm:ajv@8.17.1/dist/2020.js';
import spec from './admission-spec.json' with {type:'json'};
export const admissionSpec=spec;
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
 const body={model:spec.model,instructions:spec.prompt,input:JSON.stringify(input),store:false,temperature:0,max_output_tokens:spec.max_output_tokens,service_tier:'default',text:{format:{type:'json_schema',name:'watch_interpretation',strict:true,schema:spec.schema}}};
 const raw=JSON.stringify(body);if(new TextEncoder().encode(raw).length>20000)throw new Error('Admission input too large');return raw;
}
export function parseAdmissionResponse(response:Record<string,unknown>):Interpretation {
 if(response.status!=='completed')throw new Error('Incomplete admission');
 const output=response.output as {content?:{type:string;text?:string}[]}[];
 const texts=output?.flatMap(x=>x.content??[]).filter(x=>x.type==='output_text');
 if(texts?.length!==1||!texts[0].text)throw new Error('Missing admission text');
 return interpretation(JSON.parse(texts[0].text));
}
