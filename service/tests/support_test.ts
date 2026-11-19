import {valid,handleRequest} from '../supabase/functions/_shared/http.ts';
import {deliverSupport,type SupportMessage} from '../supabase/functions/_shared/support.ts';
const assert=(v:unknown)=>{if(!v)throw new Error('assertion');};
const sample:SupportMessage={id:'d1f8d53f-120a-49e0-8396-dba450b12001',category:'question',message:'Synthetic test',replyEmail:null,appVersion:'0.2.4',language:'fr'};
Deno.test('support bounds content, email and categories and forbids recipient or attachments',()=>{
 assert(valid('SupportMessage',sample));
 for(const extra of [{message:' '},{message:'a'.repeat(2001)},{replyEmail:'x\r\nBcc: x@elsewhere.test'},{category:'other'},{to:'private@example.test'},{attachment:'file'},{appVersion:'bad\nversion'}])assert(!valid('SupportMessage',{...sample,...extra}));
 assert(valid('SupportMessage',{...sample,replyEmail:'reply@example.test',language:'en'}));
});
Deno.test('one queued message is acknowledged only after SMTP accepts it',async()=>{
 const finished:unknown[]=[];let sent=0;
 assert(await deliverSupport({async lease(){return sample;},async finish(id,delivered){finished.push([id,delivered]);}},async()=>{sent++;})==='sent');
 assert(sent===1&&JSON.stringify(finished)===JSON.stringify([[sample.id,true]]));
});
Deno.test('mail failure is retained for review and never logged with message content',async()=>{
 let delivered=true;const old=console.warn;const log:unknown[]=[];
 try{console.warn=(v:unknown)=>{log.push(v);};
 assert(await deliverSupport({async lease(){return sample;},async finish(_id,value){delivered=value;}},async()=>{throw new Error('private SMTP password');})==='failed');
 assert(!delivered&&!JSON.stringify(log).includes(sample.message)&&!JSON.stringify(log).includes('password'));
 }finally{console.warn=old;}
});
Deno.test('empty support inbox does not contact SMTP',async()=>{
 assert(await deliverSupport({async lease(){return null;},async finish(){throw new Error('unexpected');}},async()=>{throw new Error('unexpected');})==='empty');
});
Deno.test('support diagnostics contain no reply email or message',async()=>{
 const logs:unknown[]=[];
 const response=await handleRequest(async()=>new Response('{}',{status:202}),new Request('https://example.test/public/v1/support',{method:'POST',body:JSON.stringify(sample)}),e=>logs.push(e));
 assert(response.status===202&&JSON.stringify(logs).includes('support')&&!JSON.stringify(logs).includes('Synthetic'));
});
