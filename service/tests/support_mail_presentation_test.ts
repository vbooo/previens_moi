import {supportMailPresentation} from '../supabase/functions/_shared/support-mail-presentation.ts';
import {valid} from '../supabase/functions/_shared/http.ts';
import type {SupportMessage} from '../supabase/functions/_shared/support.ts';
const assert=(value:unknown)=>{if(!value)throw new Error('support mail identity mismatch');};
const sample:SupportMessage={id:'d1f8d53f-120a-49e0-8396-dba450b12001',category:'question',message:'Synthetic message',replyEmail:null,appVersion:'0.2.4',language:'fr'};
const categories={question:'Question',problem:'Problème',idea:'Idée / avis'} as const;

Deno.test('Préviens-moi keeps its exact sender, subject and content for every category and language',()=>{
 for(const language of ['fr','en'] as const)for(const category of Object.keys(categories) as (keyof typeof categories)[]){
  const message={...sample,category,language};
  const mail=supportMailPresentation(message);
  assert(mail.senderName==='Préviens-moi — assistance');
  assert(mail.subject===`[Préviens-moi] ${categories[category]}`);
  assert(mail.text===`${message.message}\n\n---\nVersion : ${message.appVersion}\nLangue : ${language}\nRéférence : ${message.id}`);
 }
});

Deno.test('Mesures sur photos has its own identity in French and English without changing the API contract',()=>{
 for(const language of ['fr','en'] as const)for(const category of Object.keys(categories) as (keyof typeof categories)[]){
  const message={...sample,category,language,appVersion:'MSP-0.0.21-Android-15'};
  assert(valid('SupportMessage',message));
  const mail=supportMailPresentation(message);
  assert(mail.senderName==='Mesures sur photos — assistance');
  assert(mail.subject===`[Mesures sur photos] ${categories[category]}`);
  assert(mail.text===`${message.message}\n\n---\nApplication : Mesures sur photos\nVersion : ${message.appVersion}\nLangue : ${language}\nRéférence : ${message.id}`);
 }
});

Deno.test('only the complete MSP version marker changes display identity, never user text or reply email',()=>{
 for(const appVersion of ['0.2.4','0.2.5-Android-15','MSP-','MSP-0.0.21','MSP-0.0.21-Android-','xMSP-0.0.21-Android-15']){
  const mail=supportMailPresentation({...sample,appVersion,message:'MSP-0.0.21-Android-15',replyEmail:'MSP@example.test'});
  assert(mail.subject==='[Préviens-moi] Question');
 }
 for(const appVersion of ['MSP-0.0.21-Android-15','MSP-1.2.0-beta-Android-16.0']){
  assert(supportMailPresentation({...sample,appVersion}).subject==='[Mesures sur photos] Question');
 }
});
