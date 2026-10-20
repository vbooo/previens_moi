import {eraseAccounts,type ErasurePort} from '../supabase/functions/_shared/erasure.ts';
const assert=(v:unknown)=>{if(!v)throw new Error('assertion failed');};
Deno.test('erasure retries after Auth failure without claiming completion or exposing identity',async()=>{
 const calls:string[]=[];let fail=true;
 const port:ErasurePort={async candidates(){return ['private-owner'];},async purge(){calls.push('purge');},async deleteIdentity(){calls.push('auth');if(fail)throw new Error('private diagnostics');},async complete(){calls.push('done');}};
 const first=await eraseAccounts(port);assert(first.pending===1&&first.completed===0);assert(calls.join(',')==='purge,auth');
 fail=false;const second=await eraseAccounts(port);assert(second.completed===1&&second.pending===0);assert(calls.join(',')==='purge,auth,purge,auth,done');assert(!JSON.stringify(first).includes('private'));
});
Deno.test('erasure processes other eligible accounts despite one interrupted deletion',async()=>{
 const done:string[]=[];const result=await eraseAccounts({async candidates(){return ['a','b'];},async purge(owner){if(owner==='a')throw new Error();},async deleteIdentity(){},async complete(owner){done.push(owner);}});
 assert(result.pending===1&&result.completed===1&&done.join(',')==='b');
});
