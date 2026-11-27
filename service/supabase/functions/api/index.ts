import {billingPolicy,verifyPlayPurchase,refreshSubscriptions} from '../_shared/billing-runtime.ts';
import {serve,json,body,rpc,uuid,Fault,authenticated} from '../_shared/http.ts';
serve(async req=>{
 const bearer=await authenticated(req);const url=new URL(req.url);const p=url.pathname.split('/').filter(Boolean);const path=p.slice(p.indexOf('api')+1);
 if(path[0]!=='v1')throw new Fault('not_found',404);
 if(path[1]==='subscription'){
  const owner=await rpc('s7_billing_owner',{},bearer) as string;
  if(req.method==='GET'&&path.length===2){if(url.searchParams.get('verify')==='true'){await refreshSubscriptions(owner,true);}else{try{await refreshSubscriptions(owner);}catch{console.warn(JSON.stringify({event:'subscription_refresh_unavailable'}));}}return json(await rpc('s7_subscription',{},bearer),200,'SubscriptionAccess');}
  if(req.method==='POST'&&path.length===3&&path[2]==='purchases'){
   const purchase=await body(req,'PurchaseVerification');await verifyPlayPurchase(purchase.purchaseToken,owner,await billingPolicy());
   return json(await rpc('s7_subscription',{},bearer),200,'SubscriptionAccess');
  }
  throw new Fault('not_found',404);
 }
 if(req.method==='POST' && path[1]==='commands' && path.length===2){const b=await body(req,'Command');const r=await rpc('s3_command',{body:b},bearer);return json(r,r.state==='pending'?202:200,'CommandReceipt');}
 if(req.method==='POST' && path[1]==='account' && path[2]==='deletion' && path.length===3){const b=await body(req,'DeletionRequest');return json(await rpc('s3_delete_account',{request_id:b.id},bearer),202,'DeletionReceipt');}
 if(path[1]==='devices' && path.length===3 && ['PUT','DELETE'].includes(req.method)){
   const b=req.method==='PUT'?await body(req,'DeviceRegistration'):null;
   await rpc('s3_device',{device:uuid(path[2]),body:b},bearer);return new Response(null,{status:204,headers:{'Cache-Control':'no-store'}});
 }
 if(req.method==='GET'){
  let route: string;let id: string|undefined;let schema: string;
  if(path.length===2 && path[1]==='account'){route='account';schema='Account';}
  else if(path.length===2 && path[1]==='watches'){route='watches';schema='WatchPage';}
  else if(path.length===3 && ['watches','findings','commands'].includes(path[1])){route=({watches:'watch',findings:'finding',commands:'command'} as Record<string,string>)[path[1]];id=uuid(path[2]);schema=({watch:'Watch',finding:'Finding',command:'CommandReceipt'} as Record<string,string>)[route];}
  else if(path.length===4 && path[1]==='watches' && path[3]==='findings'){route='findings';id=uuid(path[2]);schema='FindingPage';}
  else throw new Fault('not_found',404);
  const limit=Number(url.searchParams.get('limit')??20);if(!Number.isInteger(limit)||limit<1||limit>100)throw new Fault('invalid_input',400);
  const cursor=url.searchParams.get('cursor');if(cursor && !/^[A-Za-z0-9+/=]{1,512}$/.test(cursor))throw new Fault('invalid_input',400);
  return json(await rpc('s3_read',{route,resource:id??null,page_limit:limit,page_cursor:cursor},bearer),200,schema);
 }
 throw new Fault('not_found',404);
});
