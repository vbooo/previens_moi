async function equal(a:string,b:string) {
 const digest=async(v:string)=>new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(v)));
 const [x,y]=await Promise.all([digest(a),digest(b)]);let diff=0;for(let i=0;i<x.length;i++)diff|=x[i]^y[i];return diff===0;
}
export async function workerAuthorized(headers:Headers,secret:string,claim:(nonce:string)=>Promise<boolean>,now=Date.now()):Promise<boolean> {
 if(!secret)return false;
 if(headers.has('Authorization'))return await equal(headers.get('Authorization')!,'Bearer '+secret);
 const stamp=headers.get('X-Worker-Time')??'',nonce=headers.get('X-Worker-Nonce')??'',signature=headers.get('X-Worker-Signature')??'';
 if(!/^\d{10}$/.test(stamp)||Math.abs(now-Number(stamp)*1000)>90000||!/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(nonce)||!/^[0-9a-f]{64}$/.test(signature))return false;
 const enc=new TextEncoder(),key=await crypto.subtle.importKey('raw',enc.encode(secret),{name:'HMAC',hash:'SHA-256'},false,['sign']);
 const expected=Array.from(new Uint8Array(await crypto.subtle.sign('HMAC',key,enc.encode(`previens-moi-worker-v1.${stamp}.${nonce}`))),b=>b.toString(16).padStart(2,'0')).join('');
 return await equal(signature,expected)&&await claim(nonce);
}
