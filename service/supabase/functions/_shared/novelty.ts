import {isJobRequest,jobIdentity,sameJob,jobWasVisible,jobPublicationDay} from './job-opportunity.ts';
import {factKey,parseJudgment,type Draft,type Intent,type Document,type Fact} from './semantic.ts';
export interface Memory {generation:number;baselineAt:string|null;observedAt:string|null;completed:boolean;facts:Fact[];jobPolicyVersion?:number}
export interface Assessment {documents:Document[];response:unknown}
export interface NoveltyResult {memory:Memory;outcome:'baseline'|'no_change'|'finding'|'degraded';findings:{fact:Fact;evidence:{criterionId:string;url:string;quote:string}[];publication:string|null}[];issues:string[]}
/** Pure cycle decision. The caller must persist memory, findings and delivery atomically,
 * after rechecking generation/permission. Discovery alone is never proof of novelty. */
export async function decideNovelty(memory:Memory,generation:number,draft:Draft,intent:Intent,assessments:Assessment[],observedAt:string,retrievalHealthy:boolean,previousDocuments:Document[]=[]):Promise<NoveltyResult> {
 const time=Date.parse(observedAt);const baseline=memory.baselineAt===null?null:Date.parse(memory.baselineAt);
 if(memory.generation!==generation||memory.completed||!Number.isFinite(time)||baseline!==null&&(!Number.isFinite(baseline)||time<baseline)||memory.observedAt!==null&&time<=Date.parse(memory.observedAt))throw new Error('Stale or inapplicable observation');
 if(intent.status!=='understood'||!intent.criteria.length)throw new Error('Unresolved criteria');
 const jobs=isJobRequest(draft,intent);const rebaseJobs=jobs&&baseline!==null&&memory.jobPolicyVersion!==1;
 const next:Memory=structuredClone(memory);const issues:string[]=[];const findings:NoveltyResult['findings']=[];
 if(!retrievalHealthy)issues.push('incomplete_retrieval');
 // Each response is revalidated against the actual documents and known facts.
 for(const a of assessments) {
  let j;try {j=parseJudgment(a.response,draft,intent,a.documents,next.facts);}catch {issues.push('invalid_evidence');continue;}
  if(j.status==='uncertain'){issues.push('unproven_criterion');continue;}
  if(j.status==='no_match')continue;
  const key=await factKey(j.identity!);const prior=next.facts.find(f=>f.key===key||f.key===j.sameAs||j.job&&f.job&&sameJob(j.job,f.job));
  if(prior)continue;
  const fact:Fact={key,identity:j.identity!,text:j.fact,...(j.job?{job:jobIdentity(j.job)}:{})};
  if(baseline===null){next.facts.push(fact);continue;}
  const used=a.documents.filter(d=>j.evidence.some(e=>e.url===d.url));
  const changed=used.some(d=>d.changedText!==null&&j.evidence.some(e=>e.url===d.url&&d.changedText!.includes(e.quote)));
  const postingDay=j.job?jobPublicationDay(j.job.publicationQuote,observedAt):null;
  // Roll out the stricter posting identity silently for existing watches. Keep
  // all old facts and avoid re-alerting on cards already visible in prior reads.
  if(j.job&&(rebaseJobs||jobWasVisible(j.job,previousDocuments,j.evidence)||postingDay!==null&&postingDay<memory.baselineAt!.slice(0,10))){next.facts.push(fact);continue;}
  const dates=j.job?(postingDay?[postingDay+'T00:00:00Z']:[]):used.map(d=>d.publication).filter((x):x is string=>x!==null);
  // An old or undated page newly found by search is not a new publication.
  // A source-backed current-page change is a different, explicitly named basis.
  const fresh=dates.some(d=>Number.isFinite(Date.parse(d))&&Date.parse(d)>baseline&&Date.parse(d)<=time);
  if(!changed&&!fresh){issues.push(dates.length?'older_than_baseline':'publication_unknown');continue;}
  next.facts.push(fact);findings.push({fact,evidence:j.evidence,publication:dates.find(d=>Date.parse(d)>baseline&&Date.parse(d)<=time)??null});
  if(draft.mode==='once'){next.completed=true;break;}
 }
 // Never activate an initial baseline from an incomplete or uncertain cycle.
 if(baseline===null&&issues.length)return {memory:structuredClone(memory),outcome:'degraded',findings:[],issues};
 if(!assessments.length){issues.push('no_verified_assessment');return {memory:structuredClone(memory),outcome:'degraded',findings:[],issues};}
 if(jobs&&!issues.length)next.jobPolicyVersion=1;
 next.observedAt=observedAt;if(next.baselineAt===null)next.baselineAt=observedAt;
 return {memory:next,outcome:findings.length?'finding':issues.length?'degraded':baseline===null?'baseline':'no_change',findings,issues};
}
