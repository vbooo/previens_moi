import {coverageRequest,parseCoverage} from '../service/supabase/functions/_shared/coverage.ts';
import {modelRequest,responseValue,parseIntent,parseJudgment,factKey} from '../service/supabase/functions/_shared/semantic.ts';
const chunks=[];for await(const chunk of Deno.stdin.readable)chunks.push(chunk);const size=chunks.reduce((n,c)=>n+c.length,0);if(size>200000)throw new Error('Input too large');const raw=new Uint8Array(size);let offset=0;for(const c of chunks){raw.set(c,offset);offset+=c.length;}
const input=JSON.parse(new TextDecoder().decode(raw));
if(input.operation==='request')console.log(input.kind==='coverage'?coverageRequest(input.data.draft,input.data.intent,input.data.documents):modelRequest(input.kind,input.data));
else {
 const value=responseValue(input.response);
 const parsed=input.kind==='coverage'?parseCoverage(value,input.data.intent,input.data.documents):input.kind==='intent'?parseIntent(value,input.data):parseJudgment(value,input.data.draft,input.data.intent,input.data.documents,input.data.previous);
 console.log(JSON.stringify({value:parsed,key:'identity' in parsed&&parsed.identity?await factKey(parsed.identity):null}));
}
