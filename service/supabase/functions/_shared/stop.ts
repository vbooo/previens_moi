import {Firecrawl} from './firecrawl.ts';
export interface StopJob {id:string;lease_token:string;providerId:string|null;uncertainStart:boolean;erase?:boolean;pendingCreateId?:string|null}
export interface StopPort {lease():Promise<StopJob|null>;confirm(job:StopJob):Promise<void>;retry(job:StopJob):Promise<void>;rememberRemote?(job:StopJob,id:string):Promise<void>}
export async function stopOne(port:StopPort,client:Firecrawl,enabled:boolean):Promise<string> {
 if(!enabled)return 'disabled';const job=await port.lease();if(!job)return 'empty';
 try {
  if(job.uncertainStart){
   if(!job.pendingCreateId||!port.rememberRemote)throw new Error('Unresolved creation');
   job.providerId=await client.findByJob(job.pendingCreateId);await port.rememberRemote(job,job.providerId);
  }
  if(job.providerId){if(job.erase)await client.erase(job.providerId);else await client.pause(job.providerId);}
  await port.confirm(job);return 'stopped';
 } catch {await port.retry(job);return 'pending';}
}
