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
export interface ServiceAccount {project_id:string;client_email:string;private_key:string;token_uri?:string}
export function identity(account:ServiceAccount,request:typeof fetch=fetch):FcmIdentity {
 let cached:string|undefined;let until=0;
 const enc=new TextEncoder();const b64=(bytes:Uint8Array)=>btoa(String.fromCharCode(...bytes)).replaceAll('+','-').replaceAll('/','_').replace(/=+$/,'');
 return {projectId:account.project_id,async accessToken(){
  if(cached && Date.now()<until)return cached;
  // The service-account file never controls a network destination.
  const now=Math.floor(Date.now()/1000);const uri='https://oauth2.googleapis.com/token';
  const header=b64(enc.encode(JSON.stringify({alg:'RS256',typ:'JWT'})));
  const claims=b64(enc.encode(JSON.stringify({iss:account.client_email,scope:'https://www.googleapis.com/auth/firebase.messaging',aud:uri,iat:now,exp:now+3600})));
  const pem=account.private_key.replace(/-----[^-]+-----/g,'').replaceAll(/\s/g,'');
  const key=await crypto.subtle.importKey('pkcs8',Uint8Array.from(atob(pem),c=>c.charCodeAt(0)),{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['sign']);
  const signature=new Uint8Array(await crypto.subtle.sign('RSASSA-PKCS1-v1_5',key,enc.encode(header+'.'+claims)));
  const response=await request(uri,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion:header+'.'+claims+'.'+b64(signature)}),redirect:'error',signal:AbortSignal.timeout(10000)});
  if(!response.ok){await response.body?.cancel();throw new Error('FCM identity unavailable');}
  const value=await response.json();if(typeof value.access_token!=='string'||typeof value.expires_in!=='number')throw new Error('Invalid identity response');
  cached=value.access_token;until=Date.now()+Math.min(value.expires_in,3600)*1000-60000;return cached!;
 }};
}
