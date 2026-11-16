import { Ajv2020 } from 'npm:ajv@8.17.1/dist/2020.js';
import addFormats from 'npm:ajv-formats@3.0.1';
import contract from './contract.json' with { type: 'json' };
const ajv = new Ajv2020({strict: false, allErrors: false});
(addFormats as unknown as (value: Ajv2020) => void)(ajv);
const defs = JSON.parse(JSON.stringify(contract.components.schemas).replaceAll('#/components/schemas/','#/$defs/'));
const validators = new Map<string, ReturnType<typeof ajv.compile>>();
export function valid(name: string, value: unknown) {
  if (!validators.has(name)) validators.set(name, ajv.compile({$defs:defs,$ref:'#/$defs/'+name}));
  return validators.get(name)!(value);
}
export class Fault extends Error { constructor(public code: string, public status: number) { super(code); } }
const statuses: Record<string, number> = {billing_unavailable:503,purchase_invalid:400,purchase_account_mismatch:403,subscription_required:403,monthly_creation_limit:429,monthly_analysis_limit:429,invalid_input:400, session_required:401,preview_required:401,access_denied:403,free_trial_ended:403,not_found:404,proposal_expired:410,command_expired:410,version_conflict:409,command_conflict:409,quota_exceeded:429,active_watch_limit:429,rate_limited:429,unsupported:409,reauthentication_required:401};
function iso(value: unknown): unknown {
  if (typeof value==='string' && /^\d{4}-\d\d-\d\dT.*\+00:00$/.test(value)) return value.replace(/\+00:00$/,'Z');
  if (Array.isArray(value)) return value.map(iso);
  if (value && typeof value==='object') return Object.fromEntries(Object.entries(value).map(([k,v])=>[k,iso(v)]));
  return value;
}
export function json(value: unknown, status=200, schema?: string) {
  const body=iso(value);
  if (schema && !valid(schema,body)) throw new Fault('temporary_failure',503);
  return new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
}
export async function bytes(req: Request, max=65536): Promise<Uint8Array> {
  const reader=req.body?.getReader(); const chunks: Uint8Array[]=[]; let size=0;
  if (!reader) throw new Fault('invalid_input',400);
  for (;;) { const {done,value}=await reader.read(); if (done) break; size+=value.length;
    if (size>max) { await reader.cancel(); throw new Fault('invalid_input',413); } chunks.push(value); }
  const out=new Uint8Array(size);let offset=0; for (const chunk of chunks) {out.set(chunk,offset);offset+=chunk.length;}return out;
}
export async function body(req: Request, schema: string) {
  if (!(req.headers.get('content-type')??'').startsWith('application/json')) throw new Fault('invalid_input',400);
  let value;try {value=JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(await bytes(req)));}catch(e){if(e instanceof Fault)throw e;throw new Fault('invalid_input',400);}
  if(!valid(schema,value))throw new Fault('invalid_input',400);return value;
}
export function uuid(value: string|undefined) {if(!value || !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value))throw new Fault('invalid_input',400);return value;}
export async function rpc(name: string, args: unknown, bearer?: string) {
  const key=Deno.env.get(bearer?'SUPABASE_ANON_KEY':'SUPABASE_SERVICE_ROLE_KEY')!;
  const r=await fetch(Deno.env.get('SUPABASE_URL')+'/rest/v1/rpc/'+name,{method:'POST',headers:{apikey:key,Authorization:bearer??'Bearer '+key,'Content-Type':'application/json'},body:JSON.stringify(args),signal:AbortSignal.timeout(15000)});
  if(!r.ok){let e;try{e=await r.json();}catch{throw new Fault('temporary_failure',503);} const code=e.message;
    throw new Fault(statuses[code]?code:'temporary_failure',statuses[code]??503);}
  return r.status===204?null:await r.json();
}
export async function authenticated(req: Request) {
  const bearer=req.headers.get('authorization');if(!bearer?.startsWith('Bearer '))throw new Fault('session_required',401);
  const r=await fetch(Deno.env.get('SUPABASE_URL')+'/auth/v1/user',{headers:{apikey:Deno.env.get('SUPABASE_ANON_KEY')!,Authorization:bearer},signal:AbortSignal.timeout(10000)});
  if(!r.ok)throw new Fault(r.status>=500?'temporary_failure':'session_required',r.status>=500?503:401);
  return bearer;
}
// Fixed route categories: never log user paths, query strings, headers or bodies.
function routeCategory(req: Request): string {
  const path = new URL(req.url).pathname;
  for (const category of ['subscription','watches','findings','commands','account','devices','understandings','evaluations','proposals','preview-sessions','health']) {
    if (path.split('/').includes(category)) return category;
  }
  return 'other';
}
export type RequestTrace = {event: 'http_request'; requestId: string; method: string; route: string; status: number; durationMs: number};
export async function handleRequest(handler:(req: Request)=>Promise<Response>, req: Request,
  trace:(entry: RequestTrace)=>void = entry=>console.info(JSON.stringify(entry))): Promise<Response> {
  const started=performance.now(), requestId=crypto.randomUUID();
  let response: Response;
  try { response=await handler(req); }
  catch(e) {
    const f=e instanceof Fault?e:new Fault('temporary_failure',503);
    const code=({preview_required:'session_required',rate_limited:'quota_exceeded',unsupported:'unsupported_client',reauthentication_required:'session_required'} as Record<string,string>)[f.code]??f.code;
    response=json({code,requestId,retryAfterSeconds:f.status===429?60:null,scope:f.code==='rate_limited'?'preview':null},f.status,'Error');
  }
  const headers=new Headers(response.headers); headers.set('X-Request-Id',requestId);
  // Diagnostics must never change the response, even when the log sink fails.
  try { trace({event:'http_request',requestId,method:['GET','POST','PUT','DELETE','OPTIONS'].includes(req.method)?req.method:'OTHER',route:routeCategory(req),status:response.status,durationMs:Math.max(0,Math.round(performance.now()-started))}); } catch { /* best effort */ }
  return new Response(response.body,{status:response.status,statusText:response.statusText,headers});
}
export function serve(handler:(req: Request)=>Promise<Response>) { Deno.serve(req=>handleRequest(handler,req)); }
