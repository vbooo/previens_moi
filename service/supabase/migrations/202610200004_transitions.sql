alter table private.jobs add column payload jsonb not null default '{}';
create table private.plan_versions(watch_id uuid not null references private.watches, plan_id uuid not null, version bigint not null, plan jsonb not null, primary key(watch_id,plan_id,version));
alter table private.plan_versions enable row level security;
create table private.method_qualifications(method text primary key, qualified boolean not null default false);
insert into private.method_qualifications(method) values('web_search'),('known_page'),('public_feed'),('public_api');
alter table private.method_qualifications enable row level security;
create or replace function public.s3_command_transaction(body jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid; i uuid:=(body->>'id')::uuid; op text:=body->>'operation'; fp text; old private.commands; w private.watches; p private.proposals; cap private.capacity; r jsonb; next_plan jsonb; begin
 u:=private.owner();perform private.command_age(i);
 fp:=encode(extensions.digest((body-'proposalToken')::text||coalesce(encode(extensions.digest(body->>'proposalToken','sha256'),'hex'),''),'sha256'),'hex');
 select * into old from private.commands where owner_id=u and id=i;
 if found then if old.fingerprint<>fp then perform private.fail('command_conflict');end if;return old.receipt;end if;
 if op in ('activate','replace') then
  select * into p from private.proposals where id=(body->>'proposalId')::uuid for update;
  if not found or p.token_hash<>encode(extensions.digest(coalesce(body->>'proposalToken',''),'sha256'),'hex') then perform private.fail('not_found');end if;
  if p.claimed_by is not null then perform private.fail('command_conflict');end if;
  if p.expires_at<=now() then perform private.fail('proposal_expired');end if;
  if p.version<>(body->>'proposalVersion')::bigint then perform private.fail('version_conflict');end if;
  if (p.payload->>'partial')::boolean and not (body->>'acceptPartial')::boolean then perform private.fail('invalid_input');end if;
  next_plan:=p.payload;
 end if;
 select * into cap from private.capacity where id for update;
 if op='activate' then
  if not cap.enabled or cap.reserved>=cap.slots or exists(select 1 from private.watches where owner_id=u and status in ('active','pending')) then perform private.fail('quota_exceeded');end if;
  if not exists(select 1 from private.method_qualifications where method=next_plan->>'method' and qualified) then perform private.fail('unsupported');end if;
  insert into private.watches(owner_id,plan,status,remote_state) values(u,next_plan,'pending','starting') returning * into w;
  update private.capacity set reserved=reserved+1 where id;
 else
  select * into w from private.watches where id=(body->>'watchId')::uuid and owner_id=u and status<>'deleted' for update;
  if not found then perform private.fail('not_found');end if;
  if w.version<>(body->>'expectedVersion')::bigint then perform private.fail('version_conflict');end if;
  if op in ('resume','replace') then
   if exists(select 1 from private.jobs where watch_id=w.id and state in ('ready','leased','ambiguous')) then perform private.fail('version_conflict');end if;
   if not cap.enabled then perform private.fail('quota_exceeded');end if;
   if op='resume' then next_plan:=w.plan;end if;
   if not exists(select 1 from private.method_qualifications where method=next_plan->>'method' and qualified) then perform private.fail('unsupported');end if;
   if w.status='paused' and w.remote_state='stopped' then
    if cap.reserved>=cap.slots or exists(select 1 from private.watches where owner_id=u and status in ('active','pending') and id<>w.id) then perform private.fail('quota_exceeded');end if;
    update private.capacity set reserved=reserved+1 where id;
    update private.watches set status='pending',remote_state='starting',generation=generation+1,version=version+1,plan=next_plan,next_check_at=null,baseline_at=null,health='not_checked_yet' where id=w.id returning * into w;
   elsif op='replace' and w.status='active' and w.remote_state='running' then
    -- Keep the old confirmed plan visible until the old execution is stopped.
    update private.watches set remote_state='stopping',generation=generation+1,version=version+1,next_check_at=null where id=w.id returning * into w;
   else perform private.fail('version_conflict');end if;
  elsif op in ('pause','delete') then
   -- Supersede old user operations, while keeping uncertain provider work visible internally.
   update private.commands c set receipt=jsonb_build_object('id',c.id,'state','failed','watchId',w.id,'updatedAt',now(),'remoteState','unknown','error',jsonb_build_object('code','version_conflict','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null))
    where c.owner_id=u and c.receipt->>'watchId'=w.id::text and c.receipt->>'state'='pending';
   update private.watches set status=case when op='delete' then 'deleted' else 'paused' end,version=version+1,generation=generation+1,
    remote_state=case when remote_state='stopped' then 'stopped' else 'stopping' end,next_check_at=null where id=w.id returning * into w;
   update private.jobs set state='done' where watch_id=w.id and state='ready';
   update private.outbox o set state='cancelled' from private.findings f where o.finding_id=f.id and f.watch_id=w.id and o.state='ready';
   if op='delete' then delete from private.findings where watch_id=w.id;end if;
  else perform private.fail('invalid_input');end if;
 end if;
 if op in ('activate','replace') then update private.proposals set claimed_by=u where id=p.id;end if;
 if w.status='pending' then
  insert into private.plan_versions values(w.id,(w.plan->>'id')::uuid,(w.plan->>'version')::bigint,w.plan) on conflict do nothing;
  insert into private.jobs(watch_id,command_id,generation,kind) values(w.id,i,w.generation,'start');
 elsif w.remote_state<>'stopped' then
  insert into private.jobs(watch_id,command_id,generation,kind,payload) values(w.id,i,w.generation,'stop',case when op='replace' then jsonb_build_object('nextPlan',next_plan) else '{}'::jsonb end) on conflict do nothing;
 end if;
 r:=jsonb_build_object('id',i,'state',case when w.status='pending' or w.remote_state<>'stopped' then 'pending' else 'succeeded' end,'watchId',w.id,'updatedAt',now(),'remoteState',w.remote_state);
 if r->>'state'='succeeded' then r:=r||jsonb_build_object('watchVersion',case when op='delete' then null else w.version end,'deleted',op='delete');end if;
 insert into private.commands(owner_id,id,fingerprint,receipt) values(u,i,fp,r);return r;
end $$;
revoke all on function public.s3_command_transaction(jsonb) from public,anon,authenticated,service_role;

create function public.s3_confirm_start(job_id uuid, lease uuid, provider text, baseline timestamptz) returns void language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;begin
 perform 1 from private.capacity where id and enabled for update;
 if not found then perform private.fail('quota_exceeded');end if;
 select * into j from private.jobs where id=job_id and kind='start' and state='leased' and lease_token=lease and lease_until>now() for update;
 if not found then perform private.fail('version_conflict');end if;
 select * into w from private.watches where id=j.watch_id and generation=j.generation and status='pending' for update;
 if not found then perform private.fail('version_conflict');end if;
 if not exists(select 1 from private.accounts where id=w.owner_id and deleted_at is null) or not exists(select 1 from private.method_qualifications where method=w.plan->>'method' and qualified) then perform private.fail('access_denied');end if;
 if length(provider) not between 1 and 200 or baseline>now()+interval '5 minutes' or baseline<j.available_at then perform private.fail('invalid_input');end if;
 update private.watches set status='active',remote_state='running',provider_id=provider,baseline_at=baseline,health='healthy',last_success_at=baseline,next_check_at=baseline+((plan->>'intervalHours')||' hours')::interval where id=w.id;
 update private.jobs set state='done',lease_until=null where id=j.id;
 insert into private.checks values(w.id,w.generation,'baseline:'||provider,'baseline',baseline) on conflict do nothing;
 update private.commands set receipt=jsonb_build_object('id',id,'state','succeeded','watchId',w.id,'watchVersion',w.version,'deleted',false,'remoteState','running','updatedAt',now()) where owner_id=w.owner_id and id=j.command_id;
end $$;
revoke all on function public.s3_confirm_start(uuid,uuid,text,timestamptz) from public,anon,authenticated;
grant execute on function public.s3_confirm_start(uuid,uuid,text,timestamptz) to service_role;

create or replace function public.s3_confirm_stop(job_id uuid, lease uuid) returns void language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;begin
 perform 1 from private.capacity where id for update;
 select * into j from private.jobs where id=job_id and kind in ('stop','erase') and state='leased' and lease_token=lease and lease_until>now() for update;
 if not found then perform private.fail('version_conflict');end if;
 select * into w from private.watches where id=j.watch_id for update;
 if w.generation<>j.generation then perform private.fail('version_conflict');end if;
 update private.jobs set state='done',lease_until=null where id=j.id;
 if j.payload ? 'nextPlan' then
  update private.watches set status='pending',remote_state='starting',provider_id=null,plan=j.payload->'nextPlan',baseline_at=null,health='not_checked_yet' where id=w.id returning * into w;
  insert into private.plan_versions values(w.id,(w.plan->>'id')::uuid,(w.plan->>'version')::bigint,w.plan) on conflict do nothing;
  insert into private.jobs(watch_id,command_id,generation,kind) values(w.id,j.command_id,w.generation,'start');
  update private.commands set receipt=jsonb_build_object('id',id,'state','pending','watchId',w.id,'remoteState','starting','updatedAt',now()) where owner_id=w.owner_id and id=j.command_id;
 else
  if w.remote_state<>'stopped' then update private.capacity set reserved=reserved-1 where id;end if;
  update private.watches set remote_state='stopped',provider_id=null where id=w.id;
  update private.commands set receipt=jsonb_build_object('id',id,'state','succeeded','watchId',w.id,'watchVersion',case when w.status='deleted' then null else w.version end,'deleted',w.status='deleted','remoteState','stopped','updatedAt',now()) where owner_id=w.owner_id and id=j.command_id;
 end if;
end $$;
