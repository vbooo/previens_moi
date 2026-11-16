/** Server-only FCM HTTP v1 transport. No retries after an ambiguous HTTP outcome. */
export interface Delivery { id:string;lease:string;token:string;findingId:string;watchId:string;planVersion:string;kind?:'preparation';noticeId?:string;watchVersion?:string }
export interface Outbox {
 lease():Promise<Delivery|null>;
 allowed(delivery:Delivery):Promise<boolean>;
 finish(delivery:Delivery,state:'accepted'|'failed'|'unknown'|'cancelled'):Promise<void>;
}
export interface FcmIdentity { projectId:string;accessToken():Promise<string> }
export function message(delivery:Delivery) {
 return {message:{token:delivery.token,data:delivery.kind==='preparation'?{kind:'preparation',noticeId:delivery.noticeId!,watchId:delivery.watchId,watchVersion:delivery.watchVersion!}:{findingId:delivery.findingId,watchId:delivery.watchId,planVersion:delivery.planVersion},android:{priority:'normal',ttl:'86400s'}}};
}
export async function sendOne(outbox:Outbox,identity:FcmIdentity,enabled:boolean,request:typeof fetch=fetch):Promise<string> {
 if(!enabled)return 'disabled';
 if(!/^[a-z][a-z0-9-]{4,62}$/.test(identity.projectId))throw new Error('Invalid FCM project id');
 const delivery=await outbox.lease();if(!delivery)return 'empty';
 let outcome:'accepted'|'failed'|'unknown'|'cancelled'='unknown';
 try {
  // Obtain credentials first, then recheck authorization as close to emission as possible.
  const token=await identity.accessToken();
  if(!await outbox.allowed(delivery)){outcome='cancelled';return outcome;}
  const response=await request(`https://fcm.googleapis.com/v1/projects/${identity.projectId}/messages:send`,{method:'POST',headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:JSON.stringify(message(delivery)),signal:AbortSignal.timeout(10000),redirect:'error'});
  // HTTP acceptance is not device delivery. Unknown/network failures require reconciliation.
  outcome=response.ok?'accepted':response.status>=500?'unknown':'failed';
  await response.body?.cancel();return outcome;
 } finally {await outbox.finish(delivery,outcome);}
}
export type {ServiceAccount} from './google-auth.ts';
import {googleAccessToken,type ServiceAccount} from './google-auth.ts';
export function identity(account:ServiceAccount,request:typeof fetch=fetch):FcmIdentity {
 return {projectId:account.project_id,accessToken:googleAccessToken(account,'https://www.googleapis.com/auth/firebase.messaging',request)};
}
