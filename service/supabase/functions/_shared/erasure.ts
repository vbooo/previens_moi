/** Idempotent erasure: database marker blocks JWTs throughout retries. */
export interface ErasurePort {
 candidates():Promise<string[]>;
 purge(owner:string):Promise<void>;
 deleteIdentity(owner:string):Promise<void>;
 complete(owner:string):Promise<void>;
}
export async function eraseAccounts(port:ErasurePort):Promise<{completed:number;pending:number}> {
 let completed=0,pending=0;
 for(const owner of await port.candidates()) {
  try {await port.purge(owner);await port.deleteIdentity(owner);await port.complete(owner);completed++;}
  catch {pending++;} // No e-mail, account id or upstream diagnostics in logs/responses.
 }
 return {completed,pending};
}

import {Firecrawl} from './firecrawl.ts';
export interface RemoteErasurePort {pending():Promise<string[]>;done(providerId:string):Promise<void>}
export async function eraseRemote(port:RemoteErasurePort,client:Firecrawl,enabled:boolean):Promise<{completed:number;pending:number}> {
 let completed=0,pending=0;if(!enabled)return {completed,pending};
 for(const id of await port.pending()) {try{await client.erase(id);await port.done(id);completed++;}catch{pending++;}}
 return {completed,pending};
}
