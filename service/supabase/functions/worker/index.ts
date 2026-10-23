import {serve,json,rpc,Fault,bytes} from '../_shared/http.ts';
import {generalOne} from '../_shared/general-worker.ts';
import {providerOne} from '../_shared/provider-worker.ts';
import {workerAuthorized} from '../_shared/worker-auth.ts';
import {interpretOne} from '../_shared/admission-worker.ts';
import {stopOne} from '../_shared/stop.ts';
import {Firecrawl} from '../_shared/firecrawl.ts';
import {eraseAccounts,eraseRemote} from '../_shared/erasure.ts';
import {sendOne,identity,type Delivery,type ServiceAccount} from '../_shared/fcm.ts';
serve(async req=>{
 if(req.method!=='POST')throw new Fault('not_found',404);
 const secret=Deno.env.get('WORKER_SECRET');if(!secret)throw new Fault('temporary_failure',503);
 if(!await workerAuthorized(req.headers,secret,async value=>await rpc('s3_worker_nonce',{value}) as boolean))throw new Fault('access_denied',403);
 if(req.body)await bytes(req,1024);
 const generalEnabled=Deno.env.get('GENERAL_EXECUTION_ENABLED')==='true';
 const runGeneral=async()=>await generalOne({async lease(){return await rpc('s3_general_lease',{});},async finish(work,result,actual,credits){await rpc('s3_general_finish',{target:work.id,lease_id:work.lease,result,actual,credits});}},Deno.env.get('OPENAI_API_KEY')??'',new Firecrawl(Deno.env.get('FIRECRAWL_API_KEY')??'',generalEnabled),generalEnabled);
 const admission=await interpretOne({async lease(){return await rpc('s3_admission_lease',{});},async finish(job,result,interpretation,actual){await rpc('s3_admission_finish',{target:job.id,lease_id:job.lease,result,interpretation,actual});}},Deno.env.get('OPENAI_API_KEY')??'',Deno.env.get('ADMISSION_EXECUTION_ENABLED')==='true');
 const providerEnabled=Deno.env.get('PROVIDER_EXECUTION_ENABLED')==='true';
 const provider=await providerOne({async enqueueDue(){await rpc('s3_enqueue_due',{});},async lease(){return await rpc('s3_provider_lease',{});},async checkpoint(job,event,value={}){return await rpc('s3_provider_checkpoint',{job_id:job.id,lease:job.lease_token,event,value}) as boolean;},async allowed(job){return await rpc('s3_provider_allowed',{job_id:job.id,lease:job.lease_token}) as boolean;},async settle(job,actual){await rpc('s3_settle',{reservation_id:job.id,actual});},async event(provider,check_key){return await rpc('s3_provider_event',{provider,check_key});},async complete(job,observed,outcome,credits,verified){await rpc('s3_provider_complete',{job_id:job.id,lease:job.lease_token,observed,outcome,credits,fact_key:verified?.factKey??null,finding:verified?.finding??null});}},new Firecrawl(Deno.env.get('FIRECRAWL_API_KEY')??'',providerEnabled),providerEnabled,Deno.env.get('PUBLIC_WEBHOOK_URL')??'');
 const stop=await stopOne({async lease(){return await rpc('s3_stop_lease',{});},async confirm(job){await rpc('s3_confirm_stop',{job_id:job.id,lease:job.lease_token});},async rememberRemote(job,provider){await rpc('s3_stop_remember',{job_id:job.id,lease:job.lease_token,provider});},async retry(job){await rpc('s3_stop_retry',{job_id:job.id,lease:job.lease_token});}},new Firecrawl(Deno.env.get('FIRECRAWL_API_KEY')??'',providerEnabled),providerEnabled);
 const remoteErasure=await eraseRemote({async pending(){return await rpc('s3_remote_erasure_pending',{}) as string[];},async done(provider){await rpc('s3_remote_erased',{provider});}},new Firecrawl(Deno.env.get('FIRECRAWL_API_KEY')??'',providerEnabled),providerEnabled);
 const erasure=await eraseAccounts({
  async candidates(){return await rpc('s3_erasure_candidates',{}) as string[];},
  async purge(target){await rpc('s3_erase_data',{target});},
  async deleteIdentity(owner){
   const key=Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
   const response=await fetch(Deno.env.get('SUPABASE_URL')+'/auth/v1/admin/users/'+owner,{method:'DELETE',headers:{apikey:key,Authorization:'Bearer '+key},redirect:'error',signal:AbortSignal.timeout(10000)});
   await response.body?.cancel();
   if(!response.ok && response.status!==404)throw new Error('Identity deletion pending');
  },
  async complete(target){await rpc('s3_erasure_identity_done',{target});},
 });
 // Provider execution above is independently gated; FCM delivery has its own switch.
 if(Deno.env.get('FCM_EXECUTION_ENABLED')!=='true'){const general=provider==='empty'&&stop==='empty'?await runGeneral():'busy';return json({state:'disabled',general,admission,provider,stop,remoteErasure,erasure},200);}
 const account=Deno.env.get('FIREBASE_SERVICE_ACCOUNT');if(!account)throw new Fault('temporary_failure',503);
 let serviceAccount:ServiceAccount;try{serviceAccount=JSON.parse(account);}catch{throw new Fault('temporary_failure',503);}
 const state=await sendOne({
   async lease(){return await rpc('s3_delivery_lease',{}) as Delivery|null;},
   async allowed(d){return await rpc('s3_delivery_allowed',{delivery:d.id,lease:d.lease}) as boolean;},
   async finish(d,outcome){await rpc('s3_delivery_finish',{delivery:d.id,lease:d.lease,outcome});},
 },identity(serviceAccount),true);
 const preparation=state==='empty'?await sendOne({async lease(){return await rpc('s4_preparation_delivery_lease',{}) as Delivery|null;},async allowed(d){return await rpc('s4_preparation_delivery_allowed',{delivery:d.id,lease_id:d.lease}) as boolean;},async finish(d,outcome){await rpc('s4_preparation_delivery_finish',{delivery:d.id,lease_id:d.lease,outcome});}},identity(serviceAccount),true):'busy';
 const general=provider==='empty'&&stop==='empty'&&state==='empty'&&preparation==='empty'?await runGeneral():'busy';
 return json({state,preparation,general,admission,provider,stop,remoteErasure,erasure},200);
});
