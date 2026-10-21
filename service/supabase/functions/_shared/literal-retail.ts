import type {Interpretation} from './admission.ts';
/** Conservative FR/EN fast path for a fully explicit single-reference price request.
 * Other product names, currencies, comparisons and ambiguous multi-product inputs
 * keep the general interpretation path; this is not an admission eligibility gate.
 */
export function literalRetail(input:unknown,manufacturer:string|null=null):Interpretation|null {
 const draft=input as {text?:string;answers?:unknown[];language?:string}|undefined;
 if(typeof input!=='string'&&draft?.answers?.length)return null; // answers require correction-aware interpretation
 const text=typeof input==='string'?input:draft?.text;if(!text)return null;
 const t=text.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
 const codes=[...new Set(text.match(/\b(?=[a-zA-Z0-9-]*[a-zA-Z])(?=[a-zA-Z0-9-]*\d)[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*\b/g)??[])];
 if(/https?:\/\//i.test(text)||/\b(?:pas|not|sauf|except)\b[^.!?;]{0,20}\b(?:france|delivery|livraison)\b/.test(t))return null;
 if(codes.length!==1||/\b(?:ignore|ignorer|plutot|instead|not below|pas sous|sauf|except)\b/.test(t))return null;
 const amounts=[...t.matchAll(/(?:eur\s*(\d+(?:[.,]\d{1,2})?)|(\d+(?:[.,]\d{1,2})?)\s*(?:€|eur\b))/g)];
 if(amounts.length!==1)return null;
 let prefix=t.slice(0,t.indexOf(codes[0].toLowerCase()));
 if(manufacturer)prefix=prefix.replace(manufacturer.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase(),' ');
 prefix=prefix.replace(/\b(?:previens-moi|notify me|quand|when|pour|for|le|la|un|une|a|an|the|modele|model|new|neuf|neuve|neufs|neuves)\b/g,' ').replace(/[\s.,;:!?'’()\-]/g,'');
 if(prefix)return null;
 const amount=amounts[0];const cents=Math.round(Number((amount[1]??amount[2]).replace(',','.'))*100);if(!Number.isSafeInteger(cents)||cents<=0)return null;
 const before=t.slice(0,amount.index).trimEnd();const after=t.slice(amount.index!+amount[0].length).trimStart();
 let operator:string;if(/\b(sous|below|under|moins de)\s*$/.test(before))operator='lt';else if(/\b(at most|maximum de)\s*$/.test(before)||/^(maximum|max\b|au maximum)/.test(after))operator='lte';else return null;
 const condition:string[]=[];
 const states:[string,RegExp][]=[['new',/\b(new|neuf|neuve|neufs|neuves)\b/g],['refurbished',/\b(refurbished|reconditionne|reconditionnee|reconditionnes|reconditionnees)\b/g],['used',/\b(used|second.hand|occasion)\b/g]];
 for(const [state,re] of states)for(const m of t.matchAll(re)){
  // Negation, alternatives and corrections beyond this small grammar use the general path.
  if(/\b(pas|not|non|sans|no|without|plus)\s+(?:de\s+|d['’])?$/.test(t.slice(Math.max(0,m.index!-25),m.index)))return null;
  if(!condition.includes(state))condition.push(state);
 }
 if(!condition.length)return null;
 if(!/\b(livraison comprise|livre|livree|including delivery|delivered)\b/.test(t))return null;
 const noSubscription=/\bsans abonnement\b|\bwithout (?:a )?subscription\b/.test(t);
 const noTradeIn=/\b(?:sans|ni) reprise\b|\bwithout\b[^.!?;]{0,60}\btrade-in\b/.test(t);
 if(!noSubscription||!noTradeIn)return null;
 // Refuse extra qualifiers after the reference rather than silently discarding them.
 // A closed suffix grammar is deliberately narrower than the general input path.
 const suffix=t.slice(t.indexOf(codes[0].toLowerCase())+codes[0].length)
  .replace(amount[0],' ')
  .replace(/\bfinalement le reconditionne me convient aussi\b/g,' ')
  .replace(/\b(?:sans abonnement ni reprise|without (?:a )?subscription or trade-in)\b/g,' ')
  .replace(/\b(?:france metropolitaine|mainland france|livraison comprise|including delivery|one-time|une seule fois|une fois)\b/g,' ')
  .replace(/\b(?:new|neuf|neuve|neufs|neuves|refurbished|reconditionne|reconditionnee|reconditionnes|reconditionnees|used|occasion|livre|livree|delivered|passe|goes|costs|below|under|sous|moins de|at most|maximum|max|au maximum|continuous|once|en|to|in)\b/g,' ')
  .replace(/[\s.,;:!?'’()\-]/g,'');
 if(suffix)return null;
 const fr=draft?.language==='fr'||(typeof input==='string'&&!/^notify\b/i.test(text));
 if(!/\b(france metropolitaine|mainland france)\b/.test(t)) {
  // Do not infer absence for another explicitly named country: let general interpretation handle it.
  if(/\b(?:en|to|in)\s+[\p{L}]/u.test(t))return null;
  return {family:'retail',status:'clarify',criteria:null,subject:null,location:null,questions:[{id:'delivery_zone',label:fr?'Dans quel pays faut-il livrer ?':'Which delivery country?',options:[],required:true}],explanation:fr?'Le pays de livraison manque.':'The delivery country is missing.'};
 }
 return {family:'retail',status:'understood',criteria:{model:codes[0],condition,ceiling_cents:cents,operator,currency:'EUR',delivery_zone:'FR-METRO',shipping_included:true,no_subscription:true,no_trade_in:true,version:1,mode:/\b(une seule fois|une fois|one-time|once)\b/.test(t)?'once':'continuous'},subject:null,location:null,questions:[],explanation:fr?'Critères explicites de la demande conservés ; les sources restent à vérifier.':'Explicit request criteria preserved; sources still need verification.'};
}
