import {serve,bytes,json,rpc,Fault} from '../_shared/http.ts';
serve(async req=>{
 if(req.method!=='POST')throw new Fault('not_found',404);
 const secret=Deno.env.get('PROVIDER_WEBHOOK_SECRET');if(!secret)throw new Fault('temporary_failure',503);
 const raw=await bytes(req,262144);
 const signature=req.headers.get('X-Firecrawl-Signature')??'';
 if(!/^sha256=[0-9a-f]{64}$/.test(signature))throw new Fault('access_denied',403);
 const key=await crypto.subtle.importKey('raw',new TextEncoder().encode(secret),{name:'HMAC',hash:'SHA-256'},false,['verify']);
 const sig=new Uint8Array(signature.slice(7).match(/../g)!.map(s=>parseInt(s,16)));
 if(!await crypto.subtle.verify('HMAC',key,new Uint8Array(sig),new Uint8Array(raw)))throw new Fault('access_denied',403);
 let data;try{data=JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(raw));}catch{throw new Fault('invalid_input',400);}
 // Durable receipt only. A signed provider judgment is NOT sufficient to emit a finding.
 if(!data || !['monitor.page','monitor.check.completed'].includes(data.type) || !Array.isArray(data.data) || data.data.length!==1 || !data.data[0])throw new Fault('invalid_input',400);
 const record=data.data[0];
 if([data.webhookId,record.monitorId,record.checkId].some(v=>typeof v!=='string'||v.length<1||v.length>200) || data.id!==record.checkId)throw new Fault('invalid_input',400);
 await rpc('s3_inbox',{event_id:data.webhookId,provider_id:record.monitorId,check_id:record.checkId,payload:data});
 return json({accepted:true},202);
});
