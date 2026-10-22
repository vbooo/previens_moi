import {Firecrawl,ProviderFault,createPaused,runBounded,reconcile} from './firecrawl.ts';
import {textChangePlan,textChangeFinding} from './page-change.ts';
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
 complete(job:ProviderJob,observed:string,outcome:'baseline'|'no_change'|'failed'|'finding',credits:number,verified?:{finding:Record<string,unknown>;factKey:string}):Promise<void>;
}
export async function providerOne(port:ProviderPort,client:Firecrawl,enabled:boolean,webhook:string):Promise<string> {
 // The public receiver is a prerequisite, not an optional mocked URL in production.
 if(!enabled||!/^https:\/\/[a-z0-9]+\.supabase\.co\/functions\/v1\/webhook$/.test(webhook))return 'disabled';
 await port.enqueueDue();const job=await port.lease();if(!job)return 'empty';
 let mayHaveRun=false;
 const safeReadStage=job.kind==='check'&&(!job.attempt||job.attempt.phase==='run')&&!job.attempt?.run_intent_at;
 const checkpoint=(event:string,value?:unknown)=>port.checkpoint(job,event,value);
 try {
  if(job.plan.method!=='known_page'||job.plan.sources.length!==1)throw new Error('Unqualified provider scope');
  const source=job.plan.sources[0].url;
  if(!job.attempt&&job.kind==='check')await checkpoint('begin');
  const phase=job.attempt?.phase??(job.kind==='start'?'create':'run');
  if(phase==='create') {
   await createPaused(client,job.id,source,job.plan.draft.text,webhook,{async begin(){return await checkpoint('begin');},async remember(id){await checkpoint('remember',{id});},async unknown(){await checkpoint('unknown');}},Date.now(),!textChangePlan(job.plan));
   await checkpoint('later');return 'created_paused';
  }
  const monitor=job.attempt?.monitor_id??job.providerId;if(!monitor)throw new Error('Remote identity missing');
  if(phase==='run') {
   if(job.attempt?.run_intent_at)throw new Error('Run response must be reconciled; never repeat');
   const result=await runBounded(client,monitor,{
    async reserve(upper,before){if(upper!==2)throw new Error('Invalid envelope');await checkpoint('reserve',{before});},
    async allowed(){return await port.allowed(job);},async recordRunIntent(){mayHaveRun=true;await checkpoint('intent');},
    async recordCheck(id){await checkpoint('queued',{id});},async settle(credits){if(credits!==0)throw new Error('Unexpected cost before run');await port.settle(job,credits);},
    async unknown(){await checkpoint('unknown');},
   });
   if(result==='cancelled')throw new Error('Superseded job');
   await checkpoint('later');return 'queued';
  }
  if(phase==='poll') {
   const a=job.attempt!;
   if(!a.check_id||a.credits_before===null||!a.run_intent_at||Date.now()-Date.parse(a.run_intent_at)>15*60000)throw new Error('Unresolved provider check');
   const check=await client.check(monitor,a.check_id);
   if(!['completed','failed','partial'].includes(check.status)){await checkpoint('later');return 'pending';}
   const event=await port.event(monitor,a.check_id);
   if(!event){await checkpoint('later');return 'awaiting_signed_completion';}
   const value=reconcile(event,check,monitor,source);
   if(Date.parse(value.observedAt)<Date.parse(a.run_intent_at))throw new Error('Stale provider observation');
   const stopped=await client.pause(monitor);if(stopped.currentCheckId!==null)throw new Error('Provider still in flight');
   // Per-check billing is authoritative. A shared account balance can include
   // unrelated discovery calls while this check was running.
   if(check.billingStatus!=='confirmed'||!Number.isSafeInteger(check.actualCredits)||check.actualCredits!<0){await checkpoint('later');return 'awaiting_billing';}
   const actual=check.actualCredits!;
   if(job.kind==='start') {
    if(value.outcome==='failed')throw new Error('No usable baseline');
    await port.complete(job,value.observedAt,'baseline',actual);
   } else {
    // A change is not an affirmative finding. Generic semantic judging remains
    // unqualified after A1 and must fail visibly, never become silent no_change.
    if(value.outcome==='candidate_requires_evidence'&&value.page?.status==='changed'&&textChangePlan(job.plan)) {
     let verified;
     try {verified=await textChangeFinding(job.plan,job.watch_id,value.page,value.observedAt);}
     catch {await port.complete(job,value.observedAt,'failed',actual);return 'checked';}
     await port.complete(job,value.observedAt,verified?'finding':'no_change',actual,verified??undefined);
    } else await port.complete(job,value.observedAt,value.outcome==='no_change'?'no_change':'failed',actual);
   }
   return job.kind==='start'?'baseline':'checked';
  }
  throw new Error('Unknown provider phase');
 } catch(e) {
  if(safeReadStage&&!mayHaveRun&&e instanceof ProviderFault&&['unknown','rate_limited'].includes(e.state)){await checkpoint('defer_read');return 'preflight_deferred';}
  await checkpoint('unknown');return 'unknown';
 }
}
