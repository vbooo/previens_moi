import {discoverSources} from '../service/supabase/functions/_shared/source-discovery.ts';
import {Firecrawl,ProviderFault} from '../service/supabase/functions/_shared/firecrawl.ts';
const value=JSON.parse(await new Response(Deno.stdin.readable).text());let held=0;const calls:{label:string;upperCredits:number;state:string}[]=[];
try {const result=await discoverSources(new Firecrawl(Deno.env.get('FIRECRAWL_API_KEY')??'',true),value.draft,value.intent,{
 async reserve(label,upperCredits){if(held+upperCredits>12)return false;held+=upperCredits;calls.push({label,upperCredits,state:'reserved'});return true;},
 async settle(label){calls.find(c=>c.label===label)!.state='returned';},async unknown(label){calls.find(c=>c.label===label)!.state='unknown';}
},value.includeWebSearch??true);
console.log(JSON.stringify({result,calls,reservedUpperCredits:held}));

} catch(e) {console.log(JSON.stringify({error:e instanceof ProviderFault?e.state:'discovery_failed',httpStatus:e instanceof ProviderFault?e.httpStatus:null,calls,reservedUpperCredits:held}));Deno.exitCode=1;}
