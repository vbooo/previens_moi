-- General search is independently closed until live qualification. USD0.10 maximum
-- is part of the existing USD0.40 experiment envelope, never a monthly allowance.
create table private.general_budget(id boolean primary key default true check(id),enabled boolean not null default false,qualified boolean not null default false,qualification_preview_hash text check(qualification_preview_hash is null or length(qualification_preview_hash)=64),model_limit bigint not null default 0 check(model_limit between 0 and 100000),model_used bigint not null default 0,model_held bigint not null default 0,credit_limit int not null default 0 check(credit_limit between 0 and 40),credit_used int not null default 0,credit_held int not null default 0,check(model_used>=0 and model_held>=0 and model_used+model_held<=model_limit),check(credit_used>=0 and credit_held>=0 and credit_used+credit_held<=credit_limit));
insert into private.general_budget(id) values(true);
create table private.general_plans(plan_id uuid primary key references private.proposals on delete cascade,intent jsonb not null);
create table private.general_memory(watch_id uuid primary key references private.watches on delete cascade,generation bigint not null,state jsonb not null,documents jsonb not null default '[]',cycle int not null default 0,updated_at timestamptz not null default now());
create table private.general_work(id uuid primary key default gen_random_uuid(),evaluation_id uuid references private.evaluations on delete cascade,job_id uuid unique references private.jobs on delete cascade,kind text not null check(kind in ('admission','start','check')),phase text not null check(phase in ('interpret','collect','decide')),state text not null default 'ready' check(state in ('ready','leased','done','unknown')),context jsonb not null,generation bigint not null default 0,lease uuid,lease_until timestamptz,created_at timestamptz not null default now(),check((evaluation_id is null)<>(job_id is null)),unique(evaluation_id));
-- Opaque accounting survives personal-data erasure; no text, owner or URL here.
create table private.general_attempts(work_key uuid not null,phase text not null,lease uuid not null,model_reserved bigint not null,credits_reserved int not null,actual bigint,credits int,state text not null default 'reserved' check(state in ('reserved','settled','unknown')),primary key(work_key,phase));
do $$ declare t text;begin foreach t in array array['general_budget','general_plans','general_memory','general_work','general_attempts'] loop execute format('alter table private.%I enable row level security',t);end loop;end $$;

alter function public.s3_preview(text,text,jsonb,uuid) rename to s3_preview_before_general;
revoke all on function public.s3_preview_before_general(text,text,jsonb,uuid) from public,anon,authenticated,service_role;
create function public.s3_preview(action text,token text default null,body jsonb default null,resource uuid default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;old boolean;begin
 old:=exists(select 1 from private.evaluations where id=(body->>'id')::uuid);
 r:=public.s3_preview_before_general(action,token,body,resource);
 if action='evaluate' and not old and r->>'status'='unsupported' and exists(select 1 from private.general_budget where id and enabled and qualified and model_used+model_held+20000<=model_limit and (qualification_preview_hash is null or qualification_preview_hash=encode(extensions.digest(token,'sha256'),'hex'))) and exists(select 1 from private.method_qualifications where method='web_search' and qualified) then
  r:=(r-'reason'-'explanation')||jsonb_build_object('status','pending');update private.evaluations set payload=r where id=(body->>'id')::uuid;
  insert into private.general_work(evaluation_id,kind,phase,context) values((body->>'id')::uuid,'admission','interpret',jsonb_build_object('draft',body->'draft','referenceNow',to_char(now() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')));
 end if;return r;
end $$;
revoke all on function public.s3_preview(text,text,jsonb,uuid) from public,anon,authenticated;
grant execute on function public.s3_preview(text,text,jsonb,uuid) to service_role;

create function private.general_fail(target uuid) returns void language plpgsql security definer set search_path='' as $$
declare x private.general_work;j private.jobs;w private.watches;e private.evaluations;begin
 select * into x from private.general_work where id=target;
 if x.kind='admission' then
  select * into e from private.evaluations where id=x.evaluation_id;
  update private.evaluations set payload=jsonb_build_object('id',e.id,'status','failed','draftRevision',x.context->'draft'->'revision','updatedAt',now(),'error',jsonb_build_object('code','temporary_failure','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null)) where id=e.id;
 else
  select * into j from private.jobs where id=x.job_id;select * into w from private.watches where id=j.watch_id and generation=j.generation for update;
  if found and w.status in ('pending','active') then
   if j.kind='start' then
    update private.watches set status='paused',remote_state='stopped',health='degraded',last_failure_at=now(),next_check_at=null where id=w.id;
    update private.capacity set reserved=greatest(0,reserved-1) where id;
    update private.commands set receipt=jsonb_build_object('id',id,'state','failed','watchId',w.id,'updatedAt',now(),'remoteState','stopped','error',jsonb_build_object('code','temporary_failure','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null)) where owner_id=w.owner_id and id=j.command_id;
   else update private.watches set health='degraded',last_failure_at=now(),next_check_at=now()+interval '24 hours' where id=w.id;end if;
  end if;
  update private.jobs set state='done',lease_until=null where id=j.id;
 end if;
 update private.general_work set state='unknown',lease_until=null where id=target;
end $$;
revoke all on function private.general_fail(uuid) from public,anon,authenticated,service_role;

create function public.s3_general_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare x private.general_work;j private.jobs;w private.watches;m private.general_memory;intent jsonb;expired record;usd bigint;credits int;begin
 perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
 for expired in select id from private.general_work where state='leased' and lease_until<now() order by id loop perform private.general_fail(expired.id);end loop;
 if not exists(select 1 from private.general_budget where id and enabled and qualified) or not exists(select 1 from private.capacity where id and enabled) or not exists(select 1 from private.method_qualifications where method='web_search' and qualified) then return null;end if;
 perform public.s3_enqueue_due();
 select q.* into j from private.jobs q join private.watches a on a.id=q.watch_id join private.accounts b on b.id=a.owner_id where q.state='ready' and q.kind in ('start','check') and q.available_at<=now() and q.generation=a.generation and a.status in ('pending','active') and b.deleted_at is null and a.plan->>'method'='web_search' and exists(select 1 from private.general_plans where plan_id=(a.plan->>'id')::uuid) and not exists(select 1 from private.general_work where job_id=q.id) order by q.available_at for update of q skip locked limit 1;
 if found then
  select * into w from private.watches where id=j.watch_id;select p.intent into intent from private.general_plans p where p.plan_id=(w.plan->>'id')::uuid;
  select * into m from private.general_memory where watch_id=w.id and generation=w.generation;
  update private.jobs set state='leased',attempts=attempts+1,lease_token=gen_random_uuid(),lease_until=now()+interval '10 minutes' where id=j.id;
  insert into private.general_work(job_id,kind,phase,generation,context) values(j.id,j.kind,'collect',j.generation,jsonb_build_object('draft',w.plan->'draft','intent',intent,'plan',w.plan,'memory',coalesce(m.state,jsonb_build_object('generation',j.generation,'baselineAt',null,'observedAt',null,'completed',false,'facts','[]'::jsonb)),'priorDocuments',coalesce(m.documents,'[]'::jsonb),'cycle',coalesce(m.cycle,0)));
 end if;
 select a.* into x from private.general_work a where a.state='ready' and a.created_at>now()-interval '24 hours' order by a.created_at,a.id for update skip locked limit 1;
 if not found then return null;end if;
 usd:=case when x.phase='collect' then 0 else 20000 end;credits:=case when x.phase='collect' then 3 else 0 end;
 if not exists(select 1 from private.general_budget where id and model_used+model_held+usd<=model_limit and credit_used+credit_held+credits<=credit_limit) then
  perform private.general_fail(x.id);
  if x.job_id is not null then
   select * into j from private.jobs where id=x.job_id;select * into w from private.watches where id=j.watch_id and generation=x.generation for update;
   if found and w.status='active' then update private.watches set status='suspended_by_capacity',remote_state='stopped',next_check_at=null where id=w.id;update private.capacity set reserved=greatest(0,reserved-1) where id;end if;
   update private.commands set receipt=jsonb_set(receipt,'{error,code}','"quota_exceeded"') where owner_id=w.owner_id and id=j.command_id and receipt->>'state'='failed';
  end if;
  return null;
 end if;
 update private.general_budget set model_held=model_held+usd,credit_held=credit_held+credits where id;
 update private.general_work set state='leased',lease=gen_random_uuid(),lease_until=now()+interval '120 seconds' where id=x.id returning * into x;
 insert into private.general_attempts(work_key,phase,lease,model_reserved,credits_reserved) values(x.id,x.phase,x.lease,usd,credits);
 return to_jsonb(x)||jsonb_build_object('watchId',(select watch_id from private.jobs where id=x.job_id));
end $$;

create function public.s3_general_finish(target uuid,lease_id uuid,result jsonb,actual bigint,credits int) returns void language plpgsql security definer set search_path='' as $$
declare x private.general_work;a private.general_attempts;j private.jobs;w private.watches;e private.evaluations;r jsonb;p jsonb;d jsonb;f jsonb;observed timestamptz;outcome text;begin
 perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
 select * into a from private.general_attempts where work_key=target and lease=lease_id for update;
 if not found or a.state<>'reserved' then return;end if;
 if actual is not null and (actual<0 or actual>a.model_reserved) or credits is not null and (credits<0 or credits>a.credits_reserved) then update private.general_budget set enabled=false where id;perform private.general_fail(target);return;end if;
 if actual is not null then update private.general_budget set model_held=model_held-a.model_reserved,model_used=model_used+actual where id;end if;
 if credits is not null then update private.general_budget set credit_held=credit_held-a.credits_reserved,credit_used=credit_used+credits where id;end if;
 update private.general_attempts set actual=s3_general_finish.actual,credits=s3_general_finish.credits,state=case when s3_general_finish.actual is null or s3_general_finish.credits is null then 'unknown' else 'settled' end where work_key=target and phase=a.phase;
 select * into x from private.general_work where id=target and lease=lease_id and state='leased' and lease_until>now() for update;
 if not found then return;end if;
 if coalesce((result->>'failed')::boolean,false) or octet_length(result::text)>200000 then perform private.general_fail(target);return;end if;
 if x.job_id is not null then
  select * into j from private.jobs where id=x.job_id for update;select * into w from private.watches where id=j.watch_id and generation=x.generation and status in ('pending','active') and exists(select 1 from private.accounts where id=owner_id and deleted_at is null) for update;
  if not found or not exists(select 1 from private.capacity where id and enabled) or not exists(select 1 from private.general_budget where id and enabled and qualified) then update private.general_work set state='done' where id=target;update private.jobs set state='done' where id=j.id;return;end if;
 end if;
 if x.phase='interpret' then
  if result->'intent'->>'status'='understood' then update private.general_work set context=context||result,phase='collect',state='ready',lease_until=null where id=target;return;end if;
  select * into e from private.evaluations where id=x.evaluation_id;
  r:=jsonb_build_object('id',e.id,'draftRevision',x.context->'draft'->'revision','updatedAt',now());
  if result->'intent'->>'status'='clarify' then r:=r||jsonb_build_object('status','clarification','questions',result->'intent'->'questions');
  elsif result->'intent'->>'status'='unsupported' then r:=r||jsonb_build_object('status','unsupported','reason','private_data','explanation',result->'intent'->>'explanation');else perform private.general_fail(target);return;end if;
  update private.evaluations set payload=r where id=e.id;
 elsif x.phase='collect' then
  if jsonb_typeof(result->'documents')<>'array' or jsonb_array_length(result->'documents')<>1 or result->>'retrievalHealthy'<>'true' then perform private.general_fail(target);return;end if;
  update private.general_work set context=context||result,phase='decide',state='ready',lease_until=null where id=target;return;
 elsif x.kind='admission' then
  select * into e from private.evaluations where id=x.evaluation_id;r:=jsonb_build_object('id',e.id,'draftRevision',x.context->'draft'->'revision','updatedAt',now());
  if result->'coverage'->>'status'='observable' then
   p:=result->'proposal';
   if p is null or p->>'method'<>'web_search' or p->'draft'<>x.context->'draft' or p->>'partial'<>'true' or p->>'intervalHours'<>'24' or p->'criteria'<>(select jsonb_agg(z->'text') from jsonb_array_elements(x.context->'intent'->'criteria') z) or (p->>'expiresAt')::timestamptz>now()+interval '35 minutes' then perform private.fail('invalid_input');end if;
   p:=jsonb_set(p,'{limits,globalAvailable}',to_jsonb((select greatest(0,slots-reserved) from private.capacity where id)));
   insert into private.proposals(id,token_hash,version,payload,expires_at) values((p->>'id')::uuid,e.token_hash,1,p,(p->>'expiresAt')::timestamptz);
   insert into private.general_plans values((p->>'id')::uuid,x.context->'intent');r:=r||jsonb_build_object('status','ready','proposal',p);
  else r:=r||jsonb_build_object('status','unsupported','reason','unqualified_method','explanation',coalesce(result->'coverage'->>'reason','Sources non qualifiées.'));end if;
  update private.evaluations set payload=r where id=e.id;
 else
  observed:=(result->>'observedAt')::timestamptz;outcome:=result->'decision'->>'outcome';
  if observed<x.created_at or observed>now()+interval '5 minutes' or result->'decision'->'memory'->>'generation'<>x.generation::text or jsonb_array_length(result->'decision'->'memory'->'facts')>80 then perform private.fail('invalid_input');end if;
  if x.kind='start' then
   if outcome<>'baseline' then perform private.general_fail(target);return;end if;
   update private.watches set status='active',remote_state='running',provider_id=null,baseline_at=observed,health='healthy',last_success_at=observed,next_check_at=observed+interval '24 hours' where id=w.id;
   insert into private.checks values(w.id,w.generation,'general:'||target::text,'baseline',observed);
   update private.commands set receipt=jsonb_build_object('id',id,'state','succeeded','watchId',w.id,'watchVersion',w.version,'deleted',false,'remoteState','running','updatedAt',now()) where owner_id=w.owner_id and id=j.command_id;
  elsif outcome='finding' then
   if jsonb_array_length(result->'findings')<>1 then perform private.fail('invalid_input');end if;
   f:=result->'findings'->0;
   if f->>'key' !~ '^[0-9a-f]{64}$' or f->'finding'->'criteriaSnapshot'<>w.plan->'criteria' or f->'finding'->>'planId'<>w.plan->>'id' then perform private.fail('invalid_input');end if;
   perform public.s3_record_check(w.id,w.generation,'general:'||target::text,'finding',observed,f->>'key',f->'finding');
  else perform public.s3_record_check(w.id,w.generation,'general:'||target::text,case when outcome='no_change' then 'no_change' else 'failed' end,observed);end if;
  insert into private.general_memory(watch_id,generation,state,documents,cycle) values(w.id,w.generation,result->'decision'->'memory',result->'documents',coalesce((x.context->>'cycle')::int,0)+1)
  on conflict(watch_id) do update set generation=excluded.generation,state=excluded.state,documents=(select coalesce(jsonb_agg(z),'[]') from (select z from jsonb_array_elements(excluded.documents||coalesce((select jsonb_agg(v) from jsonb_array_elements(private.general_memory.documents) v where not exists(select 1 from jsonb_array_elements(excluded.documents) n where n->>'url'=v->>'url')),'[]')) z limit 6) q),cycle=excluded.cycle,updated_at=now();
  update private.watches set next_check_at=case when status='active' then observed+interval '24 hours' else null end where id=w.id;
  update private.jobs set state='done',lease_until=null where id=j.id;
 end if;
 update private.general_work set state='done',lease_until=null where id=target;
end $$;
revoke all on function public.s3_general_lease(),public.s3_general_finish(uuid,uuid,jsonb,bigint,int) from public,anon,authenticated;
grant execute on function public.s3_general_lease(),public.s3_general_finish(uuid,uuid,jsonb,bigint,int) to service_role;

-- These watches use authenticated fetch/search calls, not remote monitor objects.
-- Existing stop transactions still enforce delivery quiescence and generation checks.
alter function public.s3_stop_lease() rename to s3_stop_lease_before_general;
revoke all on function public.s3_stop_lease_before_general() from public,anon,authenticated,service_role;
create function public.s3_stop_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;begin r:=public.s3_stop_lease_before_general();if exists(select 1 from private.watches w join private.general_plans p on p.plan_id=(w.plan->>'id')::uuid where w.id=(r->>'watch_id')::uuid and w.plan->>'method'='web_search') then r:=r||jsonb_build_object('uncertainStart',false,'pendingCreateId',null);end if;return r;end $$;
revoke all on function public.s3_stop_lease() from public,anon,authenticated;grant execute on function public.s3_stop_lease() to service_role;

create or replace function public.s3_provider_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;attempt private.provider_attempts;begin
 perform private.recover_exhausted_jobs();
 select x.* into j from private.jobs x join private.watches candidate on candidate.id=x.watch_id join private.accounts a on a.id=candidate.owner_id
 where x.kind in ('start','check') and x.generation=candidate.generation and x.available_at<=now() and x.attempts<3
 and (x.state='ready' or (x.state='leased' and x.lease_until<now()))
 and candidate.plan->>'method'='known_page' and candidate.status in ('pending','active') and a.deleted_at is null
 and exists(select 1 from private.method_qualifications q where q.method=candidate.plan->>'method' and q.qualified)
 and exists(select 1 from private.capacity where id and enabled)
 order by x.available_at,x.id for update of x skip locked limit 1;
 if not found then return null;end if;
 update private.jobs set state='leased',attempts=attempts+1,lease_until=now()+interval '120 seconds',lease_token=gen_random_uuid(),payload=payload||jsonb_build_object('startedAt',coalesce(payload->>'startedAt',available_at::text)) where id=j.id returning * into j;
 select * into w from private.watches where id=j.watch_id;select * into attempt from private.provider_attempts where job_id=j.id;
 return to_jsonb(j)||jsonb_build_object('plan',w.plan,'providerId',w.provider_id,'baselineAt',w.baseline_at,'attempt',case when attempt.job_id is null then null else to_jsonb(attempt) end);
end $$;

-- Working page bodies expire; opaque deduplication/accounting is kept separately.
alter function public.s3_purge() rename to s3_purge_before_general;
revoke all on function public.s3_purge_before_general() from public,anon,authenticated,service_role;
create function public.s3_purge() returns void language plpgsql security definer set search_path='' as $$
begin
 perform public.s3_purge_before_general();
 update private.general_memory set documents='[]'::jsonb where updated_at<now()-interval '14 days';
 update private.general_work set context='{}'::jsonb where state in ('done','unknown') and created_at<now()-interval '14 days';
end $$;
revoke all on function public.s3_purge() from public,anon,authenticated;grant execute on function public.s3_purge() to service_role;

-- Surface trial exhaustion before accepting a new activation/resume command.
alter function public.s3_command(jsonb) rename to s3_command_before_general;
revoke all on function public.s3_command_before_general(jsonb) from public,anon,authenticated,service_role;
create function public.s3_command(body jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid;is_general boolean;begin
 if not private.valid_command(body) then perform private.fail('invalid_input');end if;
 u:=private.owner();
 if body->>'operation' in ('activate','replace') then is_general:=exists(select 1 from private.general_plans where plan_id=(body->>'proposalId')::uuid);
 elsif body->>'operation'='resume' then is_general:=exists(select 1 from private.watches w join private.general_plans p on p.plan_id=(w.plan->>'id')::uuid where w.id=(body->>'watchId')::uuid and w.owner_id=u);end if;
 if is_general then
  perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
  if not exists(select 1 from private.general_budget where id and enabled and qualified and model_used+model_held+20000<=model_limit and credit_used+credit_held+3<=credit_limit) then perform private.fail('quota_exceeded');end if;
  if body->>'operation'='resume' then update private.watches set status='paused' where id=(body->>'watchId')::uuid and owner_id=u and status='suspended_by_capacity' and remote_state='stopped' and version=(body->>'expectedVersion')::bigint;end if;
 end if;
 return public.s3_command_before_general(body);
end $$;
revoke all on function public.s3_command(jsonb) from public,anon;
grant execute on function public.s3_command(jsonb) to authenticated;
