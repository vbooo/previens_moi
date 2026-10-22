import {MODEL,modelRequest,responseValue,parseIntent,parseJudgment,type Draft,type Intent,type Document} from './semantic.ts';
import {coverageRequest,parseCoverage} from './coverage.ts';
import {discoverSources,type RetrievedSource} from './source-discovery.ts';
import {decideNovelty,type Memory} from './novelty.ts';
import {Firecrawl} from './firecrawl.ts';
import {valid} from './http.ts';
export interface GeneralWork {id:string;lease:string;kind:'admission'|'start'|'check';phase:'interpret'|'collect'|'decide';generation:number;watchId:string|null;context:{draft:Draft;referenceNow?:string;intent?:Intent;documents?:RetrievedSource[];priorDocuments?:Document[];memory?:Memory;cycle?:number;plan?:any;retrievalHealthy?:boolean}}
export interface GeneralPort {lease():Promise<GeneralWork|null>;finish(work:GeneralWork,result:unknown,actualMicroUsd:number|null,credits:number|null):Promise<void>}
/** One bounded phase per wakeup. Reservations are committed by lease BEFORE HTTP.
 * A lost response leaves the reservation held and is never automatically retried. */
export async function generalOne(port:GeneralPort,key:string,client:Firecrawl,enabled:boolean,request:typeof fetch=fetch):Promise<string> {
 if(!enabled||!key)return 'disabled';const work=await port.lease();if(!work)return 'empty';
 let actual:number|null=work.phase==='collect'?0:null;let credits:number|null=work.phase==='collect'?null:0;
 const c=work.context;let result:unknown;
 const model=async(body:string)=>{
  // UTF-8 bytes plus schema/transport margin are an input-token upper bound.
  const upper=Math.ceil((new TextEncoder().encode(body).length+4096)*.4+JSON.parse(body).max_output_tokens*1.6);if(upper>20000)throw new Error('Model reservation exceeded');
  const r=await request('https://api.openai.com/v1/responses',{method:'POST',headers:{Authorization:'Bearer '+key,'Content-Type':'application/json'},body,redirect:'error',signal:AbortSignal.timeout(25000)});
  if(!r.ok){await r.body?.cancel();throw new Error('Model unavailable');}
  const value=await r.json();if(value.model!==MODEL||value.service_tier!=='default'||!Number.isSafeInteger(value.usage?.input_tokens)||!Number.isSafeInteger(value.usage?.output_tokens)||Math.min(value.usage.input_tokens,value.usage.output_tokens)<0)throw new Error('Unknown model charge');
  actual=Math.ceil(value.usage.input_tokens*.4+value.usage.output_tokens*1.6);if(actual>20000)throw new Error('Unexpected model charge');return responseValue(value);
 };
 try {
  if(work.phase==='interpret')result={intent:parseIntent(await model(modelRequest('intent',{...c.draft,referenceNow:c.referenceNow??new Date().toISOString()})),c.draft)};
  else if(work.phase==='collect'){
   if(!c.intent)throw new Error('Missing criteria');let held=0,unknown=false;const n=c.cycle??0;
   const intent={...c.intent,queries:[c.intent.queries[n%c.intent.queries.length]]};
   const got=await discoverSources(client,c.draft,intent,{async reserve(_,upper){if(held+upper>3)return false;held+=upper;return true;},async settle(){},async unknown(){unknown=true;}},true,{queries:1,pages:1,offset:n});
   credits=unknown?null:held;
   const documents=got.sources.map(d=>{const old=c.priorDocuments?.find(o=>o.url===d.url);if(!old)return d;const lines=new Set(old.text.normalize('NFC').split(/\r?\n/).map(s=>s.trim()).filter(Boolean));return {...d,changedText:d.text.normalize('NFC').split(/\r?\n/).map(s=>s.trim()).filter(s=>s&&!lines.has(s)).join('\n')};});
   result={documents,retrievalHealthy:got.state==='retrieved'&&got.failures.length===0};
  } else {
   const intent=c.intent!,documents=c.documents??[];
   if(!intent||!documents.length)throw new Error('No source evidence');
   if(work.kind==='admission'){
    const coverage=parseCoverage(await model(coverageRequest(c.draft,intent,documents)),intent,documents);
    result={coverage};
    if(coverage.status==='observable'){
     const fr=c.draft.language==='fr';const expiresAt=new Date(Date.now()+30*60000).toISOString();
     const proposal={id:crypto.randomUUID(),version:1,draft:c.draft,title:intent.title,criteria:intent.criteria.map(x=>x.text),exclusions:[fr?'Comptes privés, contenu inaccessible et sources non lues.':'Private accounts, inaccessible content and unread sources.'],coverage:fr?'Recherche Web publique bornée : une requête, trois candidats au maximum et une page lue par passage. Les requêtes et candidats alternent entre les passages. Les sources ci-dessous ont été lues ; elles peuvent évoluer avec la recherche. Ce suivi ne couvre pas tout le Web.':'Bounded public web search: one query, up to three candidates and one page read per pass. Queries and candidates rotate between passes. Sources below were read and may change with discovery. This does not cover the whole web.',partial:true,sources:documents.map(d=>({url:d.url,label:d.label})),method:'web_search',intervalHours:24,limitations:[fr?'L’enveloppe d’essai est limitée : le suivi est suspendu à épuisement du quota.':'The trial envelope is finite: monitoring suspends when its quota is exhausted.',fr?'Première lecture sans notification ; ensuite, seuls les faits nouveaux prouvés sont signalés.':'Initial reading is silent; subsequent alerts require proven new facts.',fr?'Une page nouvellement trouvée sans date de publication vérifiée ne suffit pas à prouver une nouveauté.':'A newly found page without a verified publication date does not establish novelty.',fr?'Un fait au plus évalué par passage. Les pages protégées, absentes de l’index ou non sélectionnées peuvent manquer.':'At most one fact assessed per pass. Protected, unindexed or unselected pages may be missed.'],limits:{activePerAccount:1,globalAvailable:0,previewRemaining:0,resetsAt:null},expiresAt};
     if(!valid('Proposal',proposal))throw new Error('Invalid general proposal');result={coverage,proposal};
    }
   } else {
    const memory=c.memory!;let judgment;
    if(memory.baselineAt!==null&&documents.every(d=>d.changedText==='')){actual=0;judgment={status:'no_match',fact:'',reason:'No added source text.',identity:null,sameAs:null,evidence:[]};}
    else judgment=parseJudgment(await model(modelRequest('judgment',{draft:c.draft,intent,documents,previous:memory.facts.slice(-20),baseline:memory.baselineAt===null,observedAt:new Date().toISOString()})),c.draft,intent,documents,memory.facts);
    const observedAt=new Date().toISOString();const decision=await decideNovelty(memory,work.generation,c.draft,intent,[{documents,response:judgment}],observedAt,c.retrievalHealthy===true);
    if(decision.memory.facts.length>80)throw new Error('Fact memory limit reached');
    const findings=decision.findings.map(f=>{
     const evidence=[...new Map(f.evidence.map(e=>[e.url+'\0'+e.quote,{url:e.url,quote:e.quote,observedAt,publication:documents.find(d=>d.url===e.url)?.publication?{precision:'instant',value:documents.find(d=>d.url===e.url)!.publication}:null}])).values()].slice(0,10);
     const finding={id:crypto.randomUUID(),watchId:work.watchId,planId:c.plan.id,planVersion:c.plan.version,title:intent.title,fact:f.fact.text,reason:judgment.reason,criteriaSnapshot:c.plan.criteria,evidence,observedAt,event:f.fact.identity.when&&/^\d{4}-\d\d-\d\d$/.test(f.fact.identity.when)&&Number.isFinite(Date.parse(f.fact.identity.when))?{precision:'date',value:f.fact.identity.when}:null,noveltyBasis:f.publication?'verified_publication':'baseline_change',correctsFindingId:null};
     if(!valid('Finding',finding))throw new Error('Invalid general finding');return {key:f.fact.key,finding};
    });
    result={decision,findings,documents:decision.outcome==='degraded'?c.priorDocuments??[]:documents,observedAt};
   }
  }
 } catch {result={failed:true};}
 await port.finish(work,result,actual,credits);return (result as any).failed?'failed':work.kind+':'+work.phase;
}
