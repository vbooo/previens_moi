import {textChangePlan,textChangeFinding,TEXT_CHANGE_FR} from '../supabase/functions/_shared/page-change.ts';
const assert=(v:unknown)=>{if(!v)throw new Error('assertion');};
const plan={id:'00000000-0000-4000-8000-000000000001',version:1,method:'known_page',criteria:[TEXT_CHANGE_FR],sources:[{url:'https://source.example/page'}],draft:{language:'fr'}};
const watch='00000000-0000-4000-8000-000000000002',observed='2026-10-21T20:00:00.000Z';
const page=(text:string)=>({url:plan.sources[0].url,status:'changed',diff:{text}});
Deno.test('literal text scope produces source-backed delta without claiming a publication date',async()=>{
 const value=await textChangeFinding(plan,watch,page('--- previous.md\n+++ current.md\n@@ -1 +1 @@\n-ancien texte\n+nouveau texte'),observed);
 assert(value?.finding.noveltyBasis==='baseline_change');assert(value?.finding.fact==='- ancien texte\n+ nouveau texte');assert((value?.finding.evidence as any)[0].publication===null);
 const repeated=await textChangeFinding(plan,watch,page('--- a\n+++ b\n@@ -1 +1 @@\n-ancien texte\n+nouveau texte'),observed);
 assert(value?.factKey===repeated?.factKey);
});
Deno.test('same literal text moved or whitespace-only change stays silent',async()=>{
 assert(await textChangeFinding(plan,watch,page('@@ -1 +1 @@\n- texte\n+texte '),observed)===null);
});
Deno.test('semantic criteria, wrong source, missing proof and oversized changes cannot become findings',async()=>{
 assert(!textChangePlan({...plan,criteria:['Prix inférieur à 150 €']}));
 for(const p of [page('not a unified diff'),{...page('@@ -1 +1 @@\n+fait'),url:'https://foreign.example/page'},page('@@ -1 +1 @@\n+'+'x'.repeat(1100))]) {
  let refused=false;try{await textChangeFinding(plan,watch,p,observed);}catch{refused=true;}assert(refused);
 }
});
