import {workerAuthorized} from '../supabase/functions/_shared/worker-auth.ts';
const assert=(v:unknown)=>{if(!v)throw new Error('assertion');};
async function signed(stamp:string,nonce:string,secret='test-secret') {
 const enc=new TextEncoder(),key=await crypto.subtle.importKey('raw',enc.encode(secret),{name:'HMAC',hash:'SHA-256'},false,['sign']);
 const signature=Array.from(new Uint8Array(await crypto.subtle.sign('HMAC',key,enc.encode(`previens-moi-worker-v1.${stamp}.${nonce}`))),x=>x.toString(16).padStart(2,'0')).join('');
 return new Headers({'X-Worker-Time':stamp,'X-Worker-Nonce':nonce,'X-Worker-Signature':signature});
}
Deno.test('worker requires secret or valid fresh one-use MAC; replay expired and tampered requests fail',async()=>{
 const now=Date.parse('2026-10-20T20:00:00Z'),stamp=String(now/1000),nonce='00000000-0000-4000-8000-000000000001';let claims=0;
 const claim=async()=>++claims===1;const headers=await signed(stamp,nonce);
 assert(await workerAuthorized(headers,'test-secret',claim,now));assert(!await workerAuthorized(headers,'test-secret',claim,now));
 assert(!await workerAuthorized(headers,'test-secret',claim,now+91000));headers.set('X-Worker-Signature','0'.repeat(64));assert(!await workerAuthorized(headers,'test-secret',claim,now));
 assert(!await workerAuthorized(new Headers({Authorization:'Bearer wrong'}),'test-secret',claim,now));assert(await workerAuthorized(new Headers({Authorization:'Bearer test-secret'}),'test-secret',claim,now));assert(!await workerAuthorized(new Headers(),'',claim,now));
});
