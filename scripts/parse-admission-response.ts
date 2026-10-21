import {parseAdmissionResponse} from '../service/supabase/functions/_shared/admission.ts';
const {response,input}=JSON.parse(await new Response(Deno.stdin.readable).text());
console.log(JSON.stringify(parseAdmissionResponse(response,input)));
