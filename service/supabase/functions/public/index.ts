import {generalOne} from '../_shared/general-worker.ts';
import {Firecrawl} from '../_shared/firecrawl.ts';
import {serve,json,body,rpc,uuid,Fault} from '../_shared/http.ts';
serve(async req=>{
 const p=new URL(req.url).pathname.split('/').filter(Boolean);const start=p.indexOf('public');const path=p.slice(start+1);
 if(path[0]!=='v1')throw new Fault('not_found',404);
 if(req.method==='GET' && path[1]==='health' && path.length===2)return json({status:'ok',apiVersion:'1'},200,'Health');
 if(req.method==='POST' && path[1]==='preview-sessions' && path.length===2)return json(await rpc('s3_preview',{action:'session'}),201,'PreviewSession');
 const token=req.headers.get('X-Preview-Token');if(!token || token.length>256)throw new Fault('preview_required',401);
 if(req.method==='POST' && path[1]==='understandings' && path.length===2){
  const b=await body(req,'EvaluationRequest');let value:any=await rpc('s3_preview',{action:'understand',token,body:b});
  if(value.status==='pending'){
   await generalOne({async lease(){return await rpc('s4_understanding_lease',{target:b.id,token});},async finish(work,result,actual,credits){await rpc('s3_general_finish',{target:work.id,lease_id:work.lease,result,actual,credits});}},Deno.env.get('OPENAI_API_KEY')??'',new Firecrawl('',false),Deno.env.get('GENERAL_EXECUTION_ENABLED')==='true');
   value=await rpc('s3_preview',{action:'evaluation',token,resource:b.id});
  }
  return json(value,value.status==='pending'?202:200,'Evaluation');
 }
 if(req.method==='POST' && path[1]==='evaluations' && path.length===2){const b=await body(req,'EvaluationRequest');return json(await rpc('s3_preview',{action:'evaluate',token,body:b}),202,'Evaluation');}
 if(req.method==='GET' && path.length===3 && ['evaluations','proposals'].includes(path[1]))return json(await rpc('s3_preview',{action:path[1]==='evaluations'?'evaluation':'proposal',token,resource:uuid(path[2])}),200,path[1]==='evaluations'?'Evaluation':'Proposal');
 throw new Fault('not_found',404);
});
