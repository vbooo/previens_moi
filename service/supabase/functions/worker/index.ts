import {serve,json,rpc,Fault,bytes} from '../_shared/http.ts';
import {sendOne,identity,type Delivery,type ServiceAccount} from '../_shared/fcm.ts';
serve(async req=>{
 if(req.method!=='POST')throw new Fault('not_found',404);
 const secret=Deno.env.get('WORKER_SECRET');if(!secret)throw new Fault('temporary_failure',503);
 const presented=req.headers.get('Authorization')??'';
 const digest=async(value:string)=>new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value)));
 const [expected,actual]=await Promise.all([digest('Bearer '+secret),digest(presented)]);
 let diff=0;for(let i=0;i<expected.length;i++)diff|=expected[i]^actual[i];
 if(diff!==0)throw new Fault('access_denied',403);
 if(req.body)await bytes(req,1024);
 // This endpoint does not execute provider jobs: those methods are not qualified.
 if(Deno.env.get('FCM_EXECUTION_ENABLED')!=='true')return json({state:'disabled'},200);
 const account=Deno.env.get('FIREBASE_SERVICE_ACCOUNT');if(!account)throw new Fault('temporary_failure',503);
 let serviceAccount:ServiceAccount;try{serviceAccount=JSON.parse(account);}catch{throw new Fault('temporary_failure',503);}
 const state=await sendOne({
   async lease(){return await rpc('s3_delivery_lease',{}) as Delivery|null;},
   async allowed(d){return await rpc('s3_delivery_allowed',{delivery:d.id,lease:d.lease}) as boolean;},
   async finish(d,outcome){await rpc('s3_delivery_finish',{delivery:d.id,lease:d.lease,outcome});},
 },identity(serviceAccount),true);
 return json({state},200);
});
