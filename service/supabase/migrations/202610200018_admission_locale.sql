create or replace function public.s3_preview(action text,token text default null,body jsonb default null,resource uuid default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare old boolean;r jsonb;begin
 perform pg_advisory_xact_lock(30202026);
 old:=exists(select 1 from private.evaluations where id=(body->>'id')::uuid);
 r:=public.s3_preview_transaction(action,token,body,resource);
 if action='evaluate' and not old and r->>'status'='unsupported' and body->'draft'->>'language'='en' then
  r:=r||jsonb_build_object('explanation','Real interpretation and monitoring are not qualified yet. No watch has been activated.');
  update private.evaluations set payload=r where id=(body->>'id')::uuid;
 end if;
 if action='evaluate' and not old and exists(select 1 from private.admission_capacity where id and enabled and qualified and used_micro_usd+reserved_micro_usd+10000<=budget_micro_usd) then
  r:=(r-'reason'-'explanation')||jsonb_build_object('status','pending');
  update private.evaluations set payload=r where id=(body->>'id')::uuid;
  insert into private.admission_jobs(id,request) values((body->>'id')::uuid,body);
 end if;
 return r;
end $$;
