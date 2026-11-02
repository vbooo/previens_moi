import {handleRequest,Fault,type RequestTrace} from '../supabase/functions/_shared/http.ts';
const assert=(value:unknown)=>{if(!value)throw new Error('assertion');};
Deno.test('request identity correlates sanitized error, header and trace',async()=>{
  const traces:RequestTrace[]=[];
  const response=await handleRequest(async()=>{throw new Fault('session_required',401);},new Request('https://private.example/api/v1/watches/private-account?email=secret',{headers:{Authorization:'Bearer secret'}}),entry=>traces.push(entry));
  const body=await response.json();
  assert(response.status===401&&body.requestId===response.headers.get('X-Request-Id')&&body.requestId===traces[0].requestId);
  assert(traces[0].route==='watches'&&!JSON.stringify(traces).includes('secret')&&!JSON.stringify(traces).includes('private-account'));
});
Deno.test('unexpected exceptions never expose provider or database diagnostics',async()=>{
  const traces:RequestTrace[]=[];
  const response=await handleRequest(async()=>{throw new Error('password=private provider body');},new Request('https://example.test/secret-path'),entry=>traces.push(entry));
  const text=await response.text();
  assert(response.status===503&&!text.includes('password')&&!JSON.stringify(traces).includes('private')&&traces[0].route==='other');
});
Deno.test('successful response remains intact and logging failure cannot break it',async()=>{
  const response=await handleRequest(async()=>new Response(null,{status:204}),new Request('https://example.test/api/v1/devices/id',{method:'DELETE'}),()=>{throw new Error('sink offline');});
  assert(response.status===204&&await response.text()===''&&response.headers.has('X-Request-Id'));
});
