import {Firecrawl,createPaused,runBounded,reconcile} from './firecrawl.ts';
export interface ProviderJob {
 id:string;lease_token:string;kind:'start'|'check';watch_id:string;generation:number;
 plan:{method:string;sources:{url:string}[];draft:{text:string}};providerId:string|null;
 attempt:null|{phase:string;monitor_id:string|null;check_id:string|null;credits_before:number|null;run_intent_at:string|null;created_at:string};
}
export interface ProviderPort {
 enqueueDue():Promise<void>;lease():Promise<ProviderJob|null>;
 checkpoint(job:ProviderJob,event:string,value?:unknown):Promise<boolean>;
 allowed(job:ProviderJob):Promise<boolean>;
 settle(job:ProviderJob,credits:number):Promise<void>;
 event(monitor:string,check:string):Promise<Record<string,unknown>|null>;
 complete(job:ProviderJob,observed:string,outcome:'baseline'|'no_change'|'failed',credits:number):Promise<void>;
}
export async function providerOne(port:ProviderPort,client:Firecrawl,enabled:boolean,webhook:string):Promise<string> {
 // The public receiver is a prerequisite, not an optional mocked URL in production.
 if(!enabled||!/^https:\/\/[a-z0-9]+\.supabase\.co\/functions\/v1\/webhook$/.test(webhook))return 'disabled';
 await port.enqueueDue();const job=await port.lease();if(!job)return 'empty';
 const checkpoint=(event:string,value?:unknown)=>port.checkpoint(job,event,value);
 try {
  if(job.plan.method!=='known_page'||job.plan.sources.length!==1)throw new Error('Unqualified provider scope');
  const source=job.plan.sources[0].url;
  if(!job.attempt&&job.kind==='check')await checkpoint('begin');
  const phase=job.attempt?.phase??(job.kind==='start'?'create':'run');
  if(phase==='create') {
   await createPaused(client,job.id,source,job.plan.draft.text,webhook,{async begin(){return await checkpoint('begin');},async remember(id){await checkpoint('remember',{id});},async unknown(){await checkpoint('unknown');}});
   await checkpoint('later');return 'created_paused';
  }
  const monitor=job.attempt?.monitor_id??job.providerId;if(!monitor)throw new Error('Remote identity missing');
  if(phase==='run') {
   if(job.attempt?.run_intent_at)throw new Error('Run response must be reconciled; never repeat');
   const result=await runBounded(client,monitor,{
    async reserve(upper,before){if(upper!==2)throw new Error('Invalid envelope');await checkpoint('reserve',{before});},
    async allowed(){return await port.allowed(job);},async recordRunIntent(){await checkpoint('intent');},
    async recordCheck(id){await checkpoint('queued',{id});},async settle(credits){if(credits!==0)throw new Error('Unexpected cost before run');await port.settle(job,credits);},
    async unknown(){await checkpoint('unknown');},
   });
   if(result==='cancelled')throw new Error('Superseded job');
   await checkpoint('later');return 'queued';
  }
  if(phase==='poll') {
   const a=job.attempt!;
   if(!a.check_id||a.credits_before===null||!a.run_intent_at||Date.now()-Date.parse(a.created_at)>15*60000)throw new Error('Unresolved provider check');
   const check=await client.check(monitor,a.check_id);
   if(!['completed','failed','partial'].includes(check.status)){await checkpoint('later');return 'pending';}
   const event=await port.event(monitor,a.check_id);
   if(!event){await checkpoint('later');return 'awaiting_signed_completion';}
   const value=reconcile(event,check,monitor,source);
   if(Date.parse(value.observedAt)<Date.parse(a.run_intent_at))throw new Error('Stale provider observation');
   const stopped=await client.pause(monitor);if(stopped.currentCheckId!==null)throw new Error('Provider still in flight');
   const actual=a.credits_before-await client.credits();
   if(job.kind==='start') {
    if(value.outcome==='failed')throw new Error('No usable baseline');
    await port.complete(job,value.observedAt,'baseline',actual);
   } else {
    // A change is not an affirmative finding. Generic semantic judging remains
    // unqualified after A1 and must fail visibly, never become silent no_change.
    await port.complete(job,value.observedAt,value.outcome==='no_change'?'no_change':'failed',actual);
   }
   return job.kind==='start'?'baseline':'checked';
  }
  throw new Error('Unknown provider phase');
 } catch {
  await checkpoint('unknown');return 'unknown';
 }
}
