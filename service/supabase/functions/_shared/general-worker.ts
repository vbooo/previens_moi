import {sourceText} from './source-content.ts';
import {MODEL,modelRequest,responseValue,parseIntent,parseJudgment,type Draft,type Intent,type Document} from './semantic.ts';
import {collectStableSources} from './stable-sources.ts';
import {coverageRequest,parseCoverage} from './coverage.ts';
import {discoverSources,type RetrievedSource} from './source-discovery.ts';
import {decideNovelty,type Memory} from './novelty.ts';
import {Firecrawl} from './firecrawl.ts';
import {valid} from './http.ts';
export interface GeneralWork {id:string;lease:string;kind:'admission'|'start'|'check';phase:'interpret'|'collect'|'decide'|'baseline';generation:number;watchId:string|null;context:{draft:Draft;referenceNow?:string;intent?:Intent;documents?:RetrievedSource[];priorDocuments?:Document[];memory?:Memory;cycle?:number;plan?:any;retrievalHealthy?:boolean;understandingOnly?:boolean;dailyTrial?:boolean;prepared?:boolean;rediscover?:boolean}}
export interface GeneralPort {lease():Promise<GeneralWork|null>;finish(work:GeneralWork,result:unknown,actualMicroUsd:number|null,credits:number|null):Promise<void>}
/** One bounded phase per wakeup. Reservations are committed by lease BEFORE HTTP.
 * A lost response leaves the reservation held and is never automatically retried. */
export async function generalOne(port:GeneralPort,key:string,client:Firecrawl,enabled:boolean,request:typeof fetch=fetch,trace:(entry:{event:"model_rejected";phase:GeneralWork["phase"];status:number})=>void=entry=>console.warn(JSON.stringify(entry))):Promise<string> {
 if(!enabled||!key)return 'disabled';const work=await port.lease();if(!work)return 'empty';
 let actual:number|null=work.phase==='collect'?0:null;let credits:number|null=work.phase==='collect'?null:0;
 const c=work.context;const daily=c.dailyTrial===true||c.plan?.trialDays===7||c.plan?.accessTier==='plus';const stable=daily||c.plan?.sourcePolicy==='stable_v1'||c.draft.answers.some((a:any)=>a.questionId==='region');let result:unknown;
 const model=async(body:string)=>{
  // UTF-8 bytes plus schema/transport margin are an input-token upper bound.
  const upper=Math.ceil((new TextEncoder().encode(body).length+4096)*.4+JSON.parse(body).max_output_tokens*1.6);if(upper>20000)throw new Error('Model reservation exceeded');
  const r=await request('https://api.openai.com/v1/responses',{method:'POST',headers:{Authorization:'Bearer '+key,'Content-Type':'application/json'},body,redirect:'error',signal:AbortSignal.timeout(25000)});
  if(!r.ok){
   // An explicit authentication rejection cannot have generated output. A lost
   // response or a server error remains an unknown charge, conservatively held.
   if(r.status===401)actual=0;
   try{trace({event:'model_rejected',phase:work.phase,status:r.status});}catch{/* diagnostics cannot affect settlement */}
   await r.body?.cancel();throw new Error('Model unavailable');
  }
  const value=await r.json();if(value.model!==MODEL||value.service_tier!=='default'||!Number.isSafeInteger(value.usage?.input_tokens)||!Number.isSafeInteger(value.usage?.output_tokens)||Math.min(value.usage.input_tokens,value.usage.output_tokens)<0)throw new Error('Unknown model charge');
  actual=Math.ceil(value.usage.input_tokens*.4+value.usage.output_tokens*1.6);if(actual>20000)throw new Error('Unexpected model charge');return responseValue(value);
 };
 try {
  if(work.phase==='interpret'){
   const intent=parseIntent(await model(modelRequest('intent',{...c.draft,referenceNow:c.referenceNow??new Date().toISOString()})),c.draft);
   result={intent};
   if(c.understandingOnly&&intent.status==='understood'){
    const fr=c.draft.language==='fr';
    const proposal={id:crypto.randomUUID(),version:1,draft:c.draft,title:intent.title,criteria:intent.criteria.map(x=>x.text),exclusions:[],coverage:fr?'Après confirmation, recherche de sources publiques correspondant à ces critères. La surveillance commence après une préparation réussie.':'After confirmation, public sources matching these criteria will be sought. Monitoring starts after successful preparation.',partial:true,sources:[],method:'web_search',backgroundPreparation:true,intervalHours:daily?24:stable?1:24,...(daily?{trialDays:7}:{}),...(stable?{sourcePolicy:'stable_v1'}:{}),limitations:[fr?'Sources publiques retenues, couverture partielle et quota d’essai limité.':'Selected public sources, partial coverage and a finite trial quota.'],limits:{activePerAccount:1,globalAvailable:0,previewRemaining:0,resetsAt:null},expiresAt:new Date(Date.now()+30*60000).toISOString()};
    if(!valid('Proposal',proposal))throw new Error('Invalid understanding');result={intent,proposal};
   }
  }
  else if(work.phase==='collect'){
   if(!c.intent)throw new Error('Missing criteria');let held=0,unknown=false;const n=c.cycle??0;
   const intent={...c.intent,queries:[c.intent.queries[n%c.intent.queries.length]]};
   const upper=stable?(c.rediscover?8:!c.plan?.sources?.length?5:c.plan.sources.length):3;
   const budget={async reserve(_:string,cost:number){if(held+cost>upper)return false;held+=cost;return true;},async settle(){},async unknown(){unknown=true;}};
   const got=stable?await (async()=>{const r=await collectStableSources(client,c.draft,c.intent!,c.plan?.sources??[],c.rediscover===true,budget);return {sources:r.documents,state:r.documents.length?'retrieved':'no_source',failures:r.healthy?[]:['partial'],checkCoverage:{verified:r.verified,total:r.total}};})():await discoverSources(client,c.draft,intent,budget,true,{queries:1,pages:1,offset:n});
   credits=unknown?null:held;
   const documents=got.sources.map(d=>{const old=c.priorDocuments?.find(o=>o.url===d.url);if(!old)return d;const lines=new Set(sourceText(old.text).normalize('NFC').split(/\r?\n/).map(s=>s.trim()).filter(Boolean));return {...d,changedText:d.text.normalize('NFC').split(/\r?\n/).map(s=>s.trim()).filter(s=>s&&!lines.has(s)).join('\n')};});
   result={documents,retrievalHealthy:got.state==='retrieved'&&got.failures.length===0,...('checkCoverage' in got?{checkCoverage:got.checkCoverage}:{})};
  } else {
   const intent=c.intent!,documents=(c.documents??[]).filter(d=>!stable||!c.prepared||c.plan?.sources.some((source:any)=>source.url===d.url));
   if(!intent||!documents.length)throw new Error('No source evidence');
   if(work.kind==='admission'||(work.kind==='start'&&c.plan?.backgroundPreparation||work.kind==='check'&&c.rediscover)&&!c.prepared){
    const coverage=parseCoverage(await model(coverageRequest(c.draft,intent,documents)),intent,documents);
    result={coverage};
    if(coverage.status==='observable'){
     const selected=stable?[...new Set(coverage.evidence.map(e=>e.url))].slice(0,3):documents.map(d=>d.url);
     if(stable&&intent.criteria.some(c=>!coverage.evidence.some(e=>e.criterionId===c.id&&selected.includes(e.url))))throw new Error('Selected pages do not cover every criterion');
     const retained=documents.filter(d=>selected.includes(d.url));
     const fr=c.draft.language==='fr';const expiresAt=new Date(Date.now()+30*60000).toISOString();
     const proposal={id:crypto.randomUUID(),version:1,draft:c.draft,title:intent.title,criteria:intent.criteria.map(x=>x.text),exclusions:[fr?'Comptes privés, contenu inaccessible et sources non lues.':'Private accounts, inaccessible content and unread sources.'],coverage:stable?(fr?'Jusqu’à trois sources publiques vérifiées, conservées et lues à chaque passage. La recherche de sources est renouvelée tous les 24 passages.':'Up to three qualified public sources, retained and read on every check. Discovery is renewed every 24 checks.'):fr?'Recherche Web publique bornée : une requête, trois candidats au maximum et une page lue par passage. Les requêtes et candidats alternent entre les passages. Les sources ci-dessous ont été lues ; elles peuvent évoluer avec la recherche. Ce suivi ne couvre pas tout le Web.':'Bounded public web search: one query, up to three candidates and one page read per pass. Queries and candidates rotate between passes. Sources below were read and may change with discovery. This does not cover the whole web.',partial:true,sources:retained.map(d=>({url:d.url,label:d.label})),method:'web_search',intervalHours:daily?24:stable?1:24,...(daily?{trialDays:7}:{}),...(stable?{sourcePolicy:'stable_v1'}:{}),limitations:[fr?'L’enveloppe d’essai est limitée : le suivi est suspendu à épuisement du quota.':'The trial envelope is finite: monitoring suspends when its quota is exhausted.',fr?'Les résultats présents au démarrage sont distingués des nouveautés.':'Initial results are distinguished from new information.',fr?'Une page nouvellement trouvée sans date de publication vérifiée ne suffit pas à prouver une nouveauté.':'A newly found page without a verified publication date does not establish novelty.',fr?'Un fait au plus évalué par passage. Les pages protégées, absentes de l’index ou non sélectionnées peuvent manquer.':'At most one fact assessed per pass. Protected, unindexed or unselected pages may be missed.'],limits:{activePerAccount:1,globalAvailable:0,previewRemaining:0,resetsAt:null},expiresAt};
     if(!valid('Proposal',proposal))throw new Error('Invalid general proposal');result=work.kind==='admission'?{coverage,proposal}:{coverage,preparedPlan:{...c.plan,sources:proposal.sources,coverage:proposal.coverage,exclusions:proposal.exclusions,limitations:proposal.limitations},documents:retained,retrievalHealthy:true};
    }
   } else {
    const memory=c.memory!;let judgment;
    if(memory.baselineAt!==null&&documents.every(d=>d.changedText==='')){actual=0;judgment={status:'no_match',fact:'',reason:'No added source text.',identity:null,sameAs:null,evidence:[]};}
    else judgment=parseJudgment(await model(modelRequest('judgment',{draft:c.draft,intent,documents,previous:memory.facts.slice(-20),baseline:memory.baselineAt===null,observedAt:new Date().toISOString()})),c.draft,intent,documents,memory.facts);
    const observedAt=new Date().toISOString();const decision=await decideNovelty(memory,work.generation,c.draft,intent,[{documents,response:judgment}],observedAt,c.retrievalHealthy===true,c.priorDocuments??[]);
    if(decision.memory.facts.length>80)throw new Error('Fact memory limit reached');
    const initial=stable&&memory.baselineAt===null&&decision.outcome==='baseline'&&judgment.status==='match' ? decision.memory.facts.filter(f=>!memory.facts.some(old=>old.key===f.key)).map(f=>({fact:f,evidence:judgment.evidence,publication:null})) : [];
    if(initial.length&&c.draft.mode==='once')decision.memory.completed=true;
    const findings=[...decision.findings,...initial].map(f=>{
     const evidence=[...new Map(f.evidence.map(e=>[e.url+'\0'+e.quote,{url:e.url,quote:e.quote,observedAt,publication:documents.find(d=>d.url===e.url)?.publication?{precision:'instant',value:documents.find(d=>d.url===e.url)!.publication}:null}])).values()].slice(0,10);
     const finding={id:crypto.randomUUID(),watchId:work.watchId,planId:c.plan.id,planVersion:c.plan.version,title:f.fact.job?[f.fact.job.title,f.fact.job.employer].join(' — ').slice(0,200):intent.title,fact:f.fact.text,reason:judgment.reason,criteriaSnapshot:c.plan.criteria,evidence,observedAt,event:f.fact.identity.when&&/^\d{4}-\d\d-\d\d$/.test(f.fact.identity.when)&&Number.isFinite(Date.parse(f.fact.identity.when))?{precision:'date',value:f.fact.identity.when}:null,noveltyBasis:initial.length?'initial':f.publication?'verified_publication':'baseline_change',correctsFindingId:null};
     if(!valid('Finding',finding))throw new Error('Invalid general finding');return {key:f.fact.key,finding};
    });
    result={decision,findings,documents:decision.outcome==='degraded'?c.priorDocuments??[]:documents,observedAt};
   }
  }
 } catch {result={failed:true};}
 await port.finish(work,result,actual,credits);return (result as any).failed?'failed':work.kind+':'+work.phase;
}
