import type {Page} from './firecrawl.ts';
import {valid} from './http.ts';
export const TEXT_CHANGE_FR='Ajouts et suppressions de texte sur la page indiquée.';
export const TEXT_CHANGE_EN='Text additions and removals on the specified page.';
export interface TextPlan {id:string;version:number;method:string;criteria:string[];sources:{url:string}[];draft:{language:string}}
export function textChangePlan(plan:unknown):plan is TextPlan {
 const p=plan as TextPlan;
 return !!p&&p.method==='known_page'&&p.sources?.length===1&&p.criteria?.length===1&&[TEXT_CHANGE_FR,TEXT_CHANGE_EN].includes(p.criteria[0]);
}
/** This scope reports literal text deltas, not semantic relevance or publication recency. */
export async function textChangeFinding(plan:TextPlan,watchId:string,page:Page,observedAt:string):Promise<{finding:Record<string,unknown>;factKey:string}|null> {
 if(!textChangePlan(plan)||page.status!=='changed'||page.url!==plan.sources[0].url)throw new Error('Unsupported text-change scope');
 const diff=page.diff?.text;
 if(!diff||diff.length>20000||diff.includes('\0'))throw new Error('Missing or oversized diff');
 const lines=diff.split(/\r?\n/);let hunk=false;const added:string[]=[],removed:string[]=[];
 for(const line of lines) {
  if(line.startsWith('@@ ')){hunk=true;continue;}
  if(!hunk)continue;
  if(line.startsWith('+')){if(line.slice(1).trim())added.push(line.slice(1).trim().normalize('NFC'));}
  else if(line.startsWith('-')){if(line.slice(1).trim())removed.push(line.slice(1).trim().normalize('NFC'));}
  else if(line&&line[0]!==' '&&line[0]!=='\\')throw new Error('Malformed diff');
 }
 if(!hunk)throw new Error('Diff without a hunk');
 const plus=[...new Set(added)].filter(x=>!removed.includes(x)).sort();
 const minus=[...new Set(removed)].filter(x=>!added.includes(x)).sort();
 if(!plus.length&&!minus.length)return null;
 const quote=[...minus.map(x=>'- '+x),...plus.map(x=>'+ '+x)].join('\n');
 if(quote.length>1000||plus.length+minus.length>20)throw new Error('Diff exceeds declared evidence limit');
 const fr=plan.draft.language==='fr';
 const finding={id:crypto.randomUUID(),watchId,planId:plan.id,planVersion:plan.version,
  title:fr?'Le texte de la page a changé':'The page text changed',fact:quote,
  reason:fr?'Différence textuelle entre deux lectures réussies. Les signes − et + indiquent les retraits et les ajouts.':'Text difference between two successful reads. Minus and plus signs indicate removals and additions.',
  criteriaSnapshot:plan.criteria,evidence:[{url:page.url,quote,observedAt,publication:null}],observedAt,event:null,noveltyBasis:'baseline_change',correctsFindingId:null};
 if(!valid('Finding',finding))throw new Error('Invalid finding');
 const raw=new TextEncoder().encode(JSON.stringify([plan.id,plan.version,page.url,minus,plus]));
 const factKey=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',raw)),v=>v.toString(16).padStart(2,'0')).join('');
 return {finding,factKey};
}
