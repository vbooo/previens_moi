import {admissionRequest,parseAdmissionResponse,type Interpretation} from './admission.ts';
export interface AdmissionJob {id:string;lease:string;request:{draft:{revision:number;language:string;text:string;answers:unknown[]}}}
export interface AdmissionPort {lease():Promise<AdmissionJob|null>;finish(job:AdmissionJob,result:unknown,interpretation:Interpretation|null,actualMicroUsd:number|null):Promise<void>}
export async function interpretOne(port:AdmissionPort,key:string,enabled:boolean,request:typeof fetch=fetch):Promise<string> {
 if(!enabled||!key)return 'disabled';const job=await port.lease();if(!job)return 'empty';
 const base={id:job.id,draftRevision:job.request.draft.revision,updatedAt:new Date().toISOString()};
 let parsed:Interpretation|null=null;let actual:number|null=null;let result:unknown;
 try {
  const body=admissionRequest(job.request.draft);
  const response=await request('https://api.openai.com/v1/responses',{method:'POST',headers:{Authorization:'Bearer '+key,'Content-Type':'application/json'},body,signal:AbortSignal.timeout(25000),redirect:'error'});
  if(!response.ok){await response.body?.cancel();throw new Error('Admission unavailable');}
  const value=await response.json();const usage=value.usage;
  if(!Number.isSafeInteger(usage?.input_tokens)||usage.input_tokens<0||!Number.isSafeInteger(usage?.output_tokens)||usage.output_tokens<0)throw new Error('Unknown usage');
  actual=Math.ceil(usage.input_tokens*.4+usage.output_tokens*1.6);
  parsed=parseAdmissionResponse(value);
  if(parsed.status==='clarify')result={...base,status:'clarification',questions:parsed.questions};
  else {
   // Understanding alone cannot make a proposal. Independent source/quality gates remain closed.
   const fr=job.request.draft.language==='fr';
   result={...base,status:'unsupported',reason:parsed.family==='private'?'private_data':'unqualified_method',explanation:parsed.family==='private'?(fr?'Les données de votre compte privé ne sont pas accessibles à cette surveillance Web.':'This web service cannot access your private account data.'):(fr?'Demande comprise. Les sources, leur couverture et la méthode doivent encore être vérifiées avant de proposer une surveillance.':'Request understood. Sources, coverage and method must be verified before a watch can be proposed.')};
  }
 } catch {
  result={...base,status:'failed',error:{code:'temporary_failure',requestId:crypto.randomUUID(),retryAfterSeconds:null,scope:null}};
 }
 await port.finish(job,result,parsed,actual);return parsed?'interpreted':'failed';
}
