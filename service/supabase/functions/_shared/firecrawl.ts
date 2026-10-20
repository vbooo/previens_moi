/** Bounded authenticated provider adapter. Disabled until external qualification. */
export class ProviderFault extends Error {constructor(public state:'disabled'|'unknown'|'failed'|'pending'|'mismatch'|'not_found'){super(state);}}
export interface Monitor {id:string;status:string;currentCheckId:string|null;nextRunAt:string|null;name:string}
export interface Check {id:string;monitorId:string;status:string;startedAt:string;summary:unknown;pages?:Page[]}
export interface Page {url:string;status:string;judgment?:{meaningful:boolean;meaningfulChanges?:{type:string;after?:string}[]};diff?:{text?:string}}
export class Firecrawl {
 constructor(private key:string,private enabled=false,private request:typeof fetch=fetch){}
 async call(method:string,path:string,body?:unknown):Promise<Record<string,unknown>> {
  if(!this.enabled||!this.key)throw new ProviderFault('disabled');
  if(!/^\/v2\/(monitor(?:\/[A-Za-z0-9_-]+(?:\/checks(?:\/[A-Za-z0-9_-]+)?|\/run)?)?|team\/credit-usage)$/.test(path))throw new ProviderFault('failed');
  let response:Response;
  try {response=await this.request('https://api.firecrawl.dev'+path,{method,headers:{Authorization:'Bearer '+this.key,'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body),redirect:'error',signal:AbortSignal.timeout(20000)});}catch{throw new ProviderFault('unknown');}
  if(!response.ok){await response.body?.cancel();throw new ProviderFault(response.status===404?'not_found':response.status>=500?'unknown':'failed');}
  const reader=response.body?.getReader();if(!reader)throw new ProviderFault('unknown');let size=0;const chunks:Uint8Array[]=[];
  for(;;){const part=await reader.read();if(part.done)break;size+=part.value.length;if(size>524288){await reader.cancel();throw new ProviderFault('unknown');}chunks.push(part.value);}
  const raw=new Uint8Array(size);let offset=0;for(const chunk of chunks){raw.set(chunk,offset);offset+=chunk.length;}
  let value;try {value=JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(raw));}catch{throw new ProviderFault('unknown');}
  if(value.success===true&&method==='DELETE')return {};
  if(value.success!==true||value.data===undefined)throw new ProviderFault('failed');return value.data;
 }
 async credits():Promise<number>{const value=await this.call('GET','/v2/team/credit-usage');const credits=value.remainingCredits;if(!Number.isSafeInteger(credits)||Number(credits)<0)throw new ProviderFault('mismatch');return Number(credits);}
 async monitor(id:string):Promise<Monitor>{const m=await this.call('GET','/v2/monitor/'+id) as unknown as Monitor;if(m.id!==id||!['active','paused'].includes(m.status)||!('currentCheckId' in m))throw new ProviderFault('mismatch');return m;}
 async pause(id:string):Promise<Monitor>{await this.call('PATCH','/v2/monitor/'+id,{status:'paused'});const m=await this.monitor(id);if(m.status!=='paused'||m.currentCheckId!==null)throw new ProviderFault('pending');return m;}
 async findByJob(jobId:string):Promise<string>{
  if(!/^[0-9a-f-]{36}$/.test(jobId))throw new ProviderFault('failed');
  const values=await this.call('GET','/v2/monitor') as unknown;
  if(!Array.isArray(values))throw new ProviderFault('unknown');
  const found=values.filter(v=>v.name==='previens-moi:'+jobId);
  if(found.length!==1||typeof found[0].id!=='string')throw new ProviderFault('pending');return found[0].id;
 }
 async erase(id:string):Promise<void>{
  try {await this.pause(id);}catch(e){if(e instanceof ProviderFault&&e.state==='not_found')return;throw e;}
  await this.call('DELETE','/v2/monitor/'+id);
  try {await this.monitor(id);}catch(e){if(e instanceof ProviderFault&&e.state==='not_found')return;throw e;}
  throw new ProviderFault('pending');
 }
 async check(monitor:string,id:string):Promise<Check>{const c=await this.call('GET',`/v2/monitor/${monitor}/checks/${id}`) as unknown as Check;if(c.id!==id||c.monitorId!==monitor)throw new ProviderFault('mismatch');return c;}
}
export function reconcile(event:Record<string,unknown>,check:Check,monitor:string,source:string) {
 const data=event.data as Record<string,unknown>[];
 if(event.type!=='monitor.check.completed'||event.id!==check.id||data?.length!==1||data[0].monitorId!==monitor||data[0].checkId!==check.id||check.monitorId!==monitor||data[0].status!==check.status||JSON.stringify(data[0].summary)!==JSON.stringify(check.summary)||event.success!==(check.status==='completed'))throw new ProviderFault('mismatch');
 const observed=Date.parse(check.startedAt.replace(/([+-]\d{2})$/,'$1:00'));if(!Number.isFinite(observed))throw new ProviderFault('mismatch');
 if(!['completed','failed','partial'].includes(check.status))throw new ProviderFault('pending');
 if(check.status!=='completed')return {outcome:'failed',observedAt:new Date(observed).toISOString()};
 if(check.pages?.length!==1||check.pages[0].url!==source||!['new','same','changed','removed','error'].includes(check.pages[0].status))throw new ProviderFault('mismatch');
 const page=check.pages[0];return {outcome:page.status==='error'?'failed':page.status==='same'?'no_change':'candidate_requires_evidence',observedAt:new Date(observed).toISOString(),page};
}
/** Each monitor is paused at rest; a manual run must be durably reserved beforehand.
 * Persist the run intent before HTTP. A lost response cannot trigger another run.
 */
export interface RunPort {
 reserve(upperCredits:number,beforeCredits:number):Promise<void>;
 allowed():Promise<boolean>;
 recordRunIntent():Promise<void>;
 recordCheck(id:string):Promise<void>;
 settle(actualCredits:number):Promise<void>;
 unknown():Promise<void>;
}
export async function runBounded(client:Firecrawl,id:string,port:RunPort,now=Date.now()):Promise<string> {
 const m=await client.monitor(id);
 if(m.status!=='paused'||m.currentCheckId!==null||!m.nextRunAt||Date.parse(m.nextRunAt)<now+3600000)throw new ProviderFault('pending');
 const before=await client.credits();if(before<2)throw new ProviderFault('failed');
 await port.reserve(2,before);if(!await port.allowed()){await port.settle(0);return 'cancelled';}
 let sent=false,checkId:string|undefined;
 try {
  // Check ID is recorded before pause; cleanup remains necessary even if the response is lost.
  await port.recordRunIntent();
  await client.call('PATCH','/v2/monitor/'+id,{status:'active'});
  if(!await port.allowed())return 'cancelled';
  sent=true;
  const result=await client.call('POST','/v2/monitor/'+id+'/run',{});
  checkId=result.id as string;if(typeof checkId!=='string'||!checkId)throw new ProviderFault('unknown');
  await port.recordCheck(checkId);return checkId;
 } catch(e) {if(sent)await port.unknown();throw e;}
 finally {
  // Pause can be confirmed while an already-running check remains in flight.
  await client.call('PATCH','/v2/monitor/'+id,{status:'paused'});
  if(!sent){const after=await client.credits();await port.settle(Math.max(0,before-after));}
 }
}
export interface CreatePort {
 /** Atomic insert-if-absent. False means the earlier attempt must be reconciled. */
 begin():Promise<boolean>;
 remember(id:string):Promise<void>;
 unknown():Promise<void>;
}
export async function createPaused(client:Firecrawl,jobId:string,source:string,goal:string,webhook:string,port:CreatePort,now=Date.now()):Promise<string> {
 if(!/^[0-9a-f-]{36}$/.test(jobId))throw new ProviderFault('failed');
 for(const value of [source,webhook]) {
  const u=new URL(value);
  if(u.protocol!=='https:'||u.username||u.password||u.port||u.hostname==='localhost'||u.hostname.includes(':')||/^\d+(\.\d+){3}$/.test(u.hostname))throw new ProviderFault('failed');
 }
 if(!goal||goal.length>2000)throw new ProviderFault('failed');
 const name='previens-moi:'+jobId;let id:string|undefined;
 const reconcileCreate=async()=>{
  const values=await client.call('GET','/v2/monitor') as unknown;
  if(!Array.isArray(values))throw new ProviderFault('unknown');
  const found=values.filter(v=>v.name===name);
  if(found.length!==1||typeof found[0].id!=='string')throw new ProviderFault('unknown');
  return found[0].id as string;
 };
 try {
  if(await port.begin()) {
   const far=new Date(now+7*86400000);
   try {
    const value=await client.call('POST','/v2/monitor',{name,schedule:{cron:`0 0 ${far.getUTCDate()} ${far.getUTCMonth()+1} *`,timezone:'UTC'},goal,judgeEnabled:true,retentionDays:14,targets:[{type:'scrape',urls:[source]}],webhook:{url:webhook,events:['monitor.page','monitor.check.completed']}});
    if(typeof value.id!=='string')throw new ProviderFault('unknown');id=value.id;
   } catch {id=await reconcileCreate();}
  } else id=await reconcileCreate(); // Never create twice after an uncertain response.
  await port.remember(id);
  await client.pause(id);
  return id;
 } catch(e) {
  if(id){try {await client.pause(id);}catch{/* unknown marker remains; no activation acknowledgement */}}
  await port.unknown();throw e;
 }
}
