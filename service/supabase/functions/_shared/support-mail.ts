import nodemailer from 'npm:nodemailer@10.6.0';
import {deliverSupport} from './support.ts';
import {rpc} from './http.ts';
export async function supportMailOne() {
 const raw=Deno.env.get('SUPPORT_MAIL_CONFIG_B64');if(!raw)return 'disabled';
 const config=JSON.parse(atob(raw));
 if(!config.host || !config.user || !config.password || !config.from || !config.recipient)throw new Error('Support mail configuration missing');
 const transport=nodemailer.createTransport({host:config.host,port:465,secure:true,
  auth:{user:config.user,pass:config.password},connectionTimeout:10000,greetingTimeout:10000,socketTimeout:15000,
  logger:false,debug:false,disableFileAccess:true,disableUrlAccess:true});
 try{return await deliverSupport({async lease(){return await rpc('support_lease',{});},async finish(id,delivered){await rpc('support_finish',{target:id,delivered});}},async m=>{
  await transport.sendMail({from:{name:'Préviens-moi — assistance',address:config.from},to:config.recipient,
   replyTo:m.replyEmail??undefined,messageId:`<support-${m.id}@previensmoi.app>`,
   subject:`[Préviens-moi] ${m.category==='problem'?'Problème':m.category==='idea'?'Idée / avis':'Question'}`,
   text:`${m.message}\n\n---\nVersion : ${m.appVersion}\nLangue : ${m.language}\nRéférence : ${m.id}`});
 });}finally{transport.close();}
}
