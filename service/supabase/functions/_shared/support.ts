import {body,Fault,json,rpc} from './http.ts';
export type SupportMessage={id:string;category:'question'|'problem'|'idea';message:string;replyEmail:string|null;appVersion:string;language:'fr'|'en'};
export async function receiveSupport(req:Request) {
 const value=await body(req,'SupportMessage') as SupportMessage;
 const salt=Deno.env.get('SUPPORT_IP_SALT');
 if(!salt || !Deno.env.get('SUPPORT_MAIL_CONFIG_B64'))throw new Fault('temporary_failure',503);
 // The gateway supplies this header. A global limit still applies if an IP changes.
 const ip=(req.headers.get('x-forwarded-for')??'unknown').split(',')[0].trim().slice(0,128);
 const key=await crypto.subtle.importKey('raw',new TextEncoder().encode(salt),{name:'HMAC',hash:'SHA-256'},false,['sign']);
 const hash=await crypto.subtle.sign('HMAC',key,new TextEncoder().encode(ip));
 const sender=Array.from(new Uint8Array(hash),b=>b.toString(16).padStart(2,'0')).join('');
 return json(await rpc('support_submit',{target:value.id,sender,payload:value}),202,'SupportReceipt');
}
export interface SupportQueue {lease():Promise<SupportMessage|null>;finish(id:string,delivered:boolean):Promise<void>}
export async function deliverSupport(queue:SupportQueue,send:(message:SupportMessage)=>Promise<void>) {
 const message=await queue.lease();if(!message)return 'empty';
 let delivered=false;
 try{await send(message);delivered=true;}catch{ /* Content and SMTP credentials never enter logs. */ }
 await queue.finish(message.id,delivered);
 if(!delivered)console.warn(JSON.stringify({event:'support_delivery_failed',id:message.id}));
 return delivered?'sent':'failed';
}
