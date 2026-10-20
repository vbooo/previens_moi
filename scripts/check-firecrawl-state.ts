/** Read-only reconciliation of the existing laboratory account. Never creates/runs a monitor. */
import {Firecrawl} from '../service/supabase/functions/_shared/firecrawl.ts';
const env=await Deno.readTextFile('engine-lab/.env');
const key=env.split('\n').find(v=>v.startsWith('FIRECRAWL_API_KEY='))?.split('=').slice(1).join('=').trim().replace(/^["']|["']$/g,'');
if(!key)throw new Error('Existing provider key unavailable');
let calls=0;const client=new Firecrawl(key,true,(async(url,init)=>{if(init?.method!=='GET'||++calls>5)throw new Error('Read-only bound');return await fetch(url,init);}) as typeof fetch);
const report:Record<string,unknown>={createdAt:new Date().toISOString(),scope:'Read-only existing account; no monitor create/run/change',maxCalls:5,additionalCreditsConsumed:0};
try {
 report.remainingCredits=await client.credits();
 const list=await client.call('GET','/v2/monitor') as unknown;
 if(!Array.isArray(list))throw new Error('Unknown provider list');
 const own=list.filter(v=>typeof v.name==='string'&&/^Pr[eé]viens|^previens-moi:/.test(v.name));
 report.knownTestMonitors=own.map(v=>({name:v.name,status:v.status,currentCheckIdPresent:!!v.currentCheckId}));
 report.allListedOwnMonitorsPaused=own.every(v=>v.status==='paused'&&!v.currentCheckId);
 report.scopeLimitedToReturnedPage=true;
 report.passed=report.allListedOwnMonitorsPaused;
}catch {report.passed=false;report.failure='Provider state not reconciled; no execution enabled';}
report.calls=calls;
await Deno.writeTextFile('validation/s3/firecrawl-readonly-state.json',JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report));
