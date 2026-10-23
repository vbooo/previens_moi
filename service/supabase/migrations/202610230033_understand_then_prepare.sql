-- Interpretation can run in the request; web discovery starts only after confirmation.
alter table private.general_work drop constraint general_work_phase_check;
alter table private.general_work add constraint general_work_phase_check check(phase in ('interpret','collect','decide','baseline'));

alter function public.s3_preview(text,text,jsonb,uuid) rename to s3_preview_before_workflow;
revoke all on function public.s3_preview_before_workflow(text,text,jsonb,uuid) from public,anon,authenticated,service_role;
create function public.s3_preview(action text,token text default null,body jsonb default null,resource uuid default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;fresh boolean;begin
 if action<>'understand' then return public.s3_preview_before_workflow(action,token,body,resource);end if;
 fresh:=not exists(select 1 from private.evaluations where id=(body->>'id')::uuid);
 r:=public.s3_preview_before_workflow('evaluate',token,body,resource);
 if fresh and r->>'status'='pending' then
  update private.general_work set context=context||'{"understandingOnly":true}'::jsonb where evaluation_id=(body->>'id')::uuid;
 elsif fresh and r->>'status'='ready' then
  -- The literal URL parser is immediate and performs no web call either.
  r:=jsonb_set(r,'{proposal,backgroundPreparation}','true');
  update private.evaluations set payload=r where id=(body->>'id')::uuid;
  update private.proposals set payload=r->'proposal' where id=(r->'proposal'->>'id')::uuid;
 end if;return r;
end $$;
revoke all on function public.s3_preview(text,text,jsonb,uuid) from public,anon,authenticated;
grant execute on function public.s3_preview(text,text,jsonb,uuid) to service_role;

create function public.s4_understanding_lease(target uuid,token text) returns jsonb language plpgsql security definer set search_path='' as $$
declare x private.general_work;begin
 perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
 if not exists(select 1 from private.general_budget where id and enabled and qualified) or not exists(select 1 from private.capacity where id and enabled) then return null;end if;
 select a.* into x from private.general_work a join private.evaluations e on e.id=a.evaluation_id join private.previews p on p.token_hash=e.token_hash where e.id=target and p.token_hash=encode(extensions.digest(token,'sha256'),'hex') and p.expires_at>now() and a.phase='interpret' and a.state='ready' and a.context->>'understandingOnly'='true' for update of a skip locked;
 if not found then return null;end if;
 if not exists(select 1 from private.general_budget where id and model_used+model_held+20000<=model_limit) then perform private.general_fail(x.id);return null;end if;
 update private.general_budget set model_held=model_held+20000 where id;
 update private.general_work set state='leased',lease=gen_random_uuid(),lease_until=now()+interval '120 seconds' where id=x.id returning * into x;
 insert into private.general_attempts(work_key,phase,lease,model_reserved,credits_reserved) values(x.id,x.phase,x.lease,20000,0);
 return to_jsonb(x)||jsonb_build_object('watchId',null);
end $$;
revoke all on function public.s4_understanding_lease(uuid,text) from public,anon,authenticated;
grant execute on function public.s4_understanding_lease(uuid,text) to service_role;

alter function private.general_fail(uuid) rename to general_fail_before_workflow;
revoke all on function private.general_fail_before_workflow(uuid) from public,anon,authenticated,service_role;
create function private.general_fail(target uuid) returns void language plpgsql security definer set search_path='' as $$
declare x private.general_work;begin
 select * into x from private.general_work where id=target;
 if x.job_id is not null then
  update private.watches w set plan=jsonb_set(w.plan,'{preparationFailure}',to_jsonb(case when x.state='ready' then 'quota' else coalesce(w.plan->>'preparationFailure','technical') end))
  from private.jobs j where j.id=x.job_id and w.id=j.watch_id and w.generation=x.generation and w.status='pending' and w.plan->>'backgroundPreparation'='true';
 end if;
 perform private.general_fail_before_workflow(target);
end $$;
revoke all on function private.general_fail(uuid) from public,anon,authenticated,service_role;

alter function public.s3_general_finish(uuid,uuid,jsonb,bigint,int) rename to s3_general_finish_before_workflow;
revoke all on function public.s3_general_finish_before_workflow(uuid,uuid,jsonb,bigint,int) from public,anon,authenticated,service_role;
-- PL/pgSQL qualifies parameters with its function name; preserve that binding after rename.
do $repair$ declare definition text;begin
 definition:=pg_get_functiondef('public.s3_general_finish_before_workflow(uuid,uuid,jsonb,bigint,integer)'::regprocedure);
 definition:=replace(replace(definition,'s3_general_finish.actual','s3_general_finish_before_workflow.actual'),'s3_general_finish.credits','s3_general_finish_before_workflow.credits');
 execute definition;
end $repair$;

create function public.s3_general_finish(target uuid,lease_id uuid,result jsonb,actual bigint,credits int) returns void language plpgsql security definer set search_path='' as $$
declare x private.general_work;a private.general_attempts;j private.jobs;w private.watches;p jsonb;begin
 perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
 select * into x from private.general_work where id=target and lease=lease_id and state='leased' and lease_until>now() for update;
 if found and x.kind='admission' and x.phase='interpret' and x.context->>'understandingOnly'='true' and result->'intent'->>'status'='understood' and result->'proposal'->>'backgroundPreparation'='true' then
  update private.general_work set phase='decide',context=context||jsonb_build_object('intent',result->'intent') where id=target;
  perform public.s3_general_finish_before_workflow(target,lease_id,jsonb_build_object('coverage',jsonb_build_object('status','observable'),'proposal',result->'proposal'),actual,credits);return;
 end if;
 if not found or x.kind<>'start' or x.phase<>'decide' or x.context->'plan'->>'backgroundPreparation' is distinct from 'true' or coalesce((result->>'failed')::boolean,false) then
  perform public.s3_general_finish_before_workflow(target,lease_id,result,actual,credits);return;
 end if;
 -- Settle this coverage assessment exactly once before scheduling its baseline.
 select * into a from private.general_attempts where work_key=target and lease=lease_id and state='reserved' for update;
 if not found then return;end if;
 if actual is not null and (actual<0 or actual>a.model_reserved) or credits is not null and (credits<0 or credits>a.credits_reserved) then update private.general_budget set enabled=false where id;perform private.general_fail(target);return;end if;
 if actual is not null then update private.general_budget set model_held=model_held-a.model_reserved,model_used=model_used+actual where id;end if;
 if credits is not null then update private.general_budget set credit_held=credit_held-a.credits_reserved,credit_used=credit_used+credits where id;end if;
 update private.general_attempts set actual=s3_general_finish.actual,credits=s3_general_finish.credits,state=case when s3_general_finish.actual is null or s3_general_finish.credits is null then 'unknown' else 'settled' end where work_key=target and phase=a.phase;
 select * into j from private.jobs where id=x.job_id for update;
 select * into w from private.watches where id=j.watch_id and generation=x.generation and status='pending' and exists(select 1 from private.accounts where id=owner_id and deleted_at is null) for update;
 if not found then update private.general_work set state='done',lease_until=null where id=target;return;end if;
 if result->'coverage'->>'status' is distinct from 'observable' or actual is null or credits is null then
  if actual is not null and credits is not null and result->'coverage'->>'status' in ('unobservable','uncertain') then update private.watches set plan=jsonb_set(plan,'{preparationFailure}','"source_unusable"') where id=w.id;end if;
  perform private.general_fail(target);return;
 end if;
 p:=jsonb_set(result->'preparedPlan','{preparationFailure}','null');
 if p is null or p->'draft'<>w.plan->'draft' or p->'criteria'<>w.plan->'criteria' or p->>'id'<>w.plan->>'id' or p->>'version'<>w.plan->>'version' or p->>'method'<>'web_search' or p->>'partial'<>'true' or p->>'intervalHours'<>'24' or jsonb_array_length(p->'sources')<1 then perform private.general_fail(target);return;end if;
 update private.watches set plan=p where id=w.id;
 update private.plan_versions set plan=p where watch_id=w.id and plan_id=(p->>'id')::uuid;
 update private.general_work set phase='baseline',state='ready',lease_until=null,context=context||jsonb_build_object('plan',p,'prepared',true) where id=target;
end $$;
revoke all on function public.s3_general_finish(uuid,uuid,jsonb,bigint,int) from public,anon,authenticated;
grant execute on function public.s3_general_finish(uuid,uuid,jsonb,bigint,int) to service_role;

-- Preparation notifications are lifecycle events, never invented findings.
create table private.preparation_notices(id uuid primary key default gen_random_uuid(),watch_id uuid not null references private.watches on delete cascade,device_id uuid not null references private.devices on delete cascade,generation bigint not null,watch_version bigint not null,event text not null check(event in ('ready','failed')),state text not null default 'ready' check(state in ('ready','leased','accepted','failed','unknown','cancelled')),lease uuid,lease_until timestamptz,created_at timestamptz not null default now(),unique(watch_id,generation,device_id));
alter table private.preparation_notices enable row level security;
create function private.preparation_changed() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if old.status='pending' and new.generation=old.generation and new.plan->>'backgroundPreparation'='true' and (new.status='active' or new.status='paused' and new.health='degraded') then
  insert into private.preparation_notices(watch_id,device_id,generation,watch_version,event) select new.id,d.id,new.generation,new.version,case when new.status='active' then 'ready' else 'failed' end from private.devices d join private.accounts a on a.id=d.owner_id where d.owner_id=new.owner_id and a.deleted_at is null on conflict do nothing;
 end if;return new;
end $$;
revoke all on function private.preparation_changed() from public,anon,authenticated,service_role;
create trigger preparation_changed after update of status on private.watches for each row execute function private.preparation_changed();
create function public.s4_preparation_delivery_allowed(delivery uuid,lease_id uuid) returns boolean language sql security definer set search_path='' as $$
 select exists(select 1 from private.preparation_notices n join private.watches w on w.id=n.watch_id join private.devices d on d.id=n.device_id join private.accounts a on a.id=w.owner_id where n.id=delivery and n.lease=lease_id and n.state='leased' and n.lease_until>now() and n.created_at>now()-interval '24 hours' and n.generation=w.generation and n.watch_version=w.version and d.owner_id=w.owner_id and a.deleted_at is null and (n.event='ready' and w.status='active' or n.event='failed' and w.status='paused' and w.health='degraded'));
$$;
create function public.s4_preparation_delivery_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare n private.preparation_notices;begin
 update private.preparation_notices set state='unknown' where state='leased' and lease_until<now();
 select * into n from private.preparation_notices where state='ready' order by created_at for update skip locked limit 1;
 if not found then return null;end if;
 update private.preparation_notices set state='leased',lease=gen_random_uuid(),lease_until=now()+interval '2 minutes' where id=n.id returning * into n;
 return (select jsonb_build_object('id',n.id,'lease',n.lease,'token',d.token,'kind','preparation','noticeId',n.id,'findingId','','watchId',w.id,'planVersion',w.plan->>'version','watchVersion',n.watch_version::text) from private.devices d join private.watches w on w.id=n.watch_id where d.id=n.device_id);
end $$;
create function public.s4_preparation_delivery_finish(delivery uuid,lease_id uuid,outcome text) returns void language plpgsql security definer set search_path='' as $$
begin
 if outcome not in ('accepted','failed','unknown','cancelled') then perform private.fail('invalid_input');end if;
 update private.preparation_notices set state=outcome,lease_until=null where id=delivery and lease=lease_id and state='leased';
end $$;
revoke all on function public.s4_preparation_delivery_allowed(uuid,uuid),public.s4_preparation_delivery_lease(),public.s4_preparation_delivery_finish(uuid,uuid,text) from public,anon,authenticated;
grant execute on function public.s4_preparation_delivery_allowed(uuid,uuid),public.s4_preparation_delivery_lease(),public.s4_preparation_delivery_finish(uuid,uuid,text) to service_role;

-- Lifecycle delivery identifiers carry no source text and expire with their devices.
alter function public.s3_purge() rename to s3_purge_before_workflow;
revoke all on function public.s3_purge_before_workflow() from public,anon,authenticated,service_role;
create function public.s3_purge() returns void language plpgsql security definer set search_path='' as $$
begin perform public.s3_purge_before_workflow();delete from private.preparation_notices where created_at<now()-interval '7 days';end $$;
revoke all on function public.s3_purge() from public,anon,authenticated;grant execute on function public.s3_purge() to service_role;
