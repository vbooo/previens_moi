import cases from '../../validation/s3/admission-a5-replay-fixtures.json' with {type:'json'};
import {parseAdmissionResponse} from '../supabase/functions/_shared/admission.ts';
const canonical=(v:unknown):unknown=>Array.isArray(v)?v.map(canonical):v&&typeof v==='object'?Object.fromEntries(Object.entries(v).sort(([a],[b])=>a.localeCompare(b)).map(([k,x])=>[k,canonical(x)])):v;
const isDeepStrictEqual=(a:unknown,b:unknown)=>JSON.stringify(canonical(a))===JSON.stringify(canonical(b));
for(const c of cases)Deno.test(`real A5 response regression ${c.id}`,()=>{
 const got=parseAdmissionResponse(c.response,c.input);let actual:unknown;
 if(c.id==='D01')actual={family:got.family,status:got.status,subject:got.subject,location:got.location};
 else actual={status:got.status==='understood'?'proposal':got.status,criteria:got.criteria,questions:got.questions.map(q=>q.id)};
 if(!isDeepStrictEqual(actual,c.expected))throw new Error(`Criteria changed in ${c.id}: ${JSON.stringify(actual)}`);
});
