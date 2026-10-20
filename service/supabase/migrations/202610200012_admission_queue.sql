-- Financial execution gate is independent of model interpretation and method qualification.
create table private.admission_capacity(id boolean primary key default true check(id),enabled boolean not null default false,qualified boolean not null default false,budget_micro_usd bigint not null default 0 check(budget_micro_usd between 0 and 100000),reserved_micro_usd bigint not null default 0 check(reserved_micro_usd>=0),used_micro_usd bigint not null default 0 check(used_micro_usd>=0),check(reserved_micro_usd+used_micro_usd<=budget_micro_usd));
insert into private.admission_capacity(id) values(true);
alter table private.admission_capacity enable row level security;
create table private.admission_jobs(id uuid primary key references private.evaluations on delete cascade,request jsonb not null,state text not null default 'ready' check(state in ('ready','leased','done','unknown')),lease uuid,lease_until timestamptz,reserved_micro_usd bigint not null default 0,actual_micro_usd bigint,interpretation jsonb);
alter table private.admission_jobs enable row level security;
alter function public.s3_preview(text,text,jsonb,uuid) rename to s3_preview_transaction;
revoke all on function public.s3_preview_transaction(text,text,jsonb,uuid) from public,anon,authenticated,service_role;
create function public.s3_preview(action text,token text default null,body jsonb default null,resource uuid default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare old boolean;r jsonb;begin
 perform pg_advisory_xact_lock(30202026);
 old:=exists(select 1 from private.evaluations where id=(body->>'id')::uuid);
 r:=public.s3_preview_transaction(action,token,body,resource);
 if action='evaluate' and not old and exists(select 1 from private.admission_capacity where id and enabled and qualified and used_micro_usd+reserved_micro_usd+10000<=budget_micro_usd) then
  r:=(r-'reason'-'explanation')||jsonb_build_object('status','pending');
  update private.evaluations set payload=r where id=(body->>'id')::uuid;
  insert into private.admission_jobs(id,request) values((body->>'id')::uuid,body);
 end if;
 return r;
end $$;
create function public.s3_admission_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.admission_jobs;begin
 perform 1 from private.admission_capacity where id for update;
 update private.admission_jobs set state='unknown' where state='leased' and lease_until<now();
 update private.evaluations e set payload=jsonb_build_object('id',e.id,'status','failed','draftRevision',(e.payload->>'draftRevision')::bigint,'updatedAt',now(),'error',jsonb_build_object('code','temporary_failure','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null)) where exists(select 1 from private.admission_jobs j where j.id=e.id and j.state='unknown') and e.payload->>'status'='pending';
 if not exists(select 1 from private.admission_capacity where id and enabled and qualified and used_micro_usd+reserved_micro_usd+10000<=budget_micro_usd) then return null;end if;
 select x.* into j from private.admission_jobs x join private.evaluations e on e.id=x.id where x.state='ready' and e.created_at>now()-interval '24 hours' order by e.created_at for update of x skip locked limit 1;
 if not found then return null;end if;
 update private.admission_capacity set reserved_micro_usd=reserved_micro_usd+10000 where id;
 update private.admission_jobs set state='leased',lease=gen_random_uuid(),lease_until=now()+interval '60 seconds',reserved_micro_usd=10000 where id=j.id returning * into j;
 return to_jsonb(j);
end $$;
create function public.s3_admission_finish(target uuid,lease_id uuid,result jsonb,interpretation jsonb,actual bigint default null) returns void language plpgsql security definer set search_path='' as $$
declare j private.admission_jobs;begin
 perform 1 from private.admission_capacity where id for update;
 select * into j from private.admission_jobs where id=target and lease=lease_id and state='leased' and lease_until>now() for update;
 if not found then perform private.fail('version_conflict');end if;
 if result->>'id'<>target::text or result->>'status' not in ('clarification','unsupported','failed') or result->'draftRevision'<>j.request->'draft'->'revision' then perform private.fail('invalid_input');end if;
 -- No ready proposal can be manufactured by a model or anonymous client.
 if actual is not null and (actual<0 or actual>j.reserved_micro_usd) then
  update private.admission_capacity set enabled=false where id;
  perform private.fail('quota_exceeded');
 end if;
 update private.admission_jobs set state=case when actual is null then 'unknown' else 'done' end,interpretation=s3_admission_finish.interpretation,actual_micro_usd=actual where id=target;
 if actual is not null then update private.admission_capacity set reserved_micro_usd=reserved_micro_usd-j.reserved_micro_usd,used_micro_usd=used_micro_usd+actual where id;end if;
 update private.evaluations set payload=result where id=target;
end $$;
revoke all on function public.s3_preview(text,text,jsonb,uuid) from public,anon;
grant execute on function public.s3_preview(text,text,jsonb,uuid) to authenticated,service_role;
revoke all on function public.s3_admission_lease(),public.s3_admission_finish(uuid,uuid,jsonb,jsonb,bigint) from public,anon,authenticated;
grant execute on function public.s3_admission_lease(),public.s3_admission_finish(uuid,uuid,jsonb,jsonb,bigint) to service_role;
