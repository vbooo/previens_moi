-- A durable attempt separates creation, run intention and result polling.
create table private.provider_attempts(job_id uuid primary key references private.jobs on delete cascade,phase text not null default 'create' check(phase in ('create','run','poll','done','unknown')),monitor_id text,check_id text,credits_before bigint,run_intent_at timestamptz,created_at timestamptz not null default now());
alter table private.provider_attempts enable row level security;
create function public.s3_provider_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;attempt private.provider_attempts;begin
 select x.* into j from private.jobs x join private.watches w on w.id=x.watch_id join private.accounts a on a.id=w.owner_id
 where x.kind in ('start','check') and x.generation=w.generation and x.available_at<=now() and x.attempts<3
 and (x.state='ready' or (x.state='leased' and x.lease_until<now()))
 and w.status in ('pending','active') and a.deleted_at is null
 and exists(select 1 from private.method_qualifications q where q.method=w.plan->>'method' and q.qualified)
 and exists(select 1 from private.capacity where id and enabled)
 order by x.available_at,x.id for update of x skip locked limit 1;
 if not found then return null;end if;
 update private.jobs set state='leased',attempts=attempts+1,lease_until=now()+interval '120 seconds',lease_token=gen_random_uuid(),payload=payload||jsonb_build_object('startedAt',coalesce(payload->>'startedAt',available_at::text)) where id=j.id returning * into j;
 select * into w from private.watches where id=j.watch_id;select * into attempt from private.provider_attempts where job_id=j.id;
 return to_jsonb(j)||jsonb_build_object('plan',w.plan,'providerId',w.provider_id,'baselineAt',w.baseline_at,'attempt',case when attempt.job_id is null then null else to_jsonb(attempt) end);
end $$;
create function public.s3_provider_allowed(job_id uuid,lease uuid) returns boolean language sql security definer set search_path='' as $$
 select exists(select 1 from private.jobs j join private.watches w on w.id=j.watch_id join private.accounts a on a.id=w.owner_id where j.id=job_id and j.lease_token=lease and j.lease_until>now() and j.state='leased' and w.generation=j.generation and w.status in ('pending','active') and a.deleted_at is null and exists(select 1 from private.capacity where id and enabled) and exists(select 1 from private.method_qualifications q where q.method=w.plan->>'method' and q.qualified))
$$;
create function public.s3_provider_checkpoint(job_id uuid,lease uuid,event text,value jsonb default '{}'::jsonb) returns boolean language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;begin
 perform 1 from private.capacity where id for update;
 select * into j from private.jobs where id=job_id and lease_token=lease and state='leased' and lease_until>now() for update;
 if not found then perform private.fail('version_conflict');end if;
 select * into w from private.watches where id=j.watch_id for update;
 if event='begin' then
  insert into private.provider_attempts(job_id,phase,monitor_id) values(j.id,case when j.kind='start' then 'create' else 'run' end,case when j.kind='check' then w.provider_id else null end) on conflict do nothing;return found;
 elsif event='remember' then
  if length(value->>'id') not between 1 and 200 then perform private.fail('invalid_input');end if;
  update private.provider_attempts set monitor_id=value->>'id',phase='run' where provider_attempts.job_id=j.id;
  -- Save a late create acknowledgement even after pause; cleanup needs its remote ID.
  if w.provider_id is null then update private.watches set provider_id=value->>'id' where id=w.id;
  elsif w.provider_id<>value->>'id' then perform private.fail('command_conflict');end if;
 elsif event='reserve' then
  if not public.s3_provider_allowed(j.id,lease) then perform private.fail('version_conflict');end if;
  perform public.s3_reserve(j.id,w.id,2);
  update private.provider_attempts set credits_before=coalesce(credits_before,(value->>'before')::bigint) where provider_attempts.job_id=j.id;
 elsif event='intent' then
  if not public.s3_provider_allowed(j.id,lease) then perform private.fail('version_conflict');end if;
  update private.provider_attempts set run_intent_at=now() where provider_attempts.job_id=j.id and run_intent_at is null;
  if not found then perform private.fail('command_conflict');end if;
 elsif event='queued' then
  update private.provider_attempts set check_id=value->>'id',phase='poll' where provider_attempts.job_id=j.id;
 elsif event='later' then
  update private.jobs set state='ready',attempts=0,available_at=now()+interval '15 seconds',lease_until=null where id=j.id;
 elsif event='unknown' then
  update private.provider_attempts set phase='unknown' where provider_attempts.job_id=j.id;
  update private.jobs set state='ambiguous',lease_until=null where id=j.id;
  update private.watches set remote_state='unknown',health='degraded',next_check_at=null where id=w.id;
  update private.capacity set enabled=false where id;
 else perform private.fail('invalid_input');end if;
 return true;
end $$;
-- A successful baseline belongs to this durable attempt, not its latest polling timestamp.
create or replace function public.s3_confirm_start(job_id uuid, lease uuid, provider text, baseline timestamptz) returns void language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;begin
 perform 1 from private.capacity where id and enabled for update;
 if not found then perform private.fail('quota_exceeded');end if;
 select * into j from private.jobs where id=job_id and kind='start' and state='leased' and lease_token=lease and lease_until>now() for update;
 if not found then perform private.fail('version_conflict');end if;
 select * into w from private.watches where id=j.watch_id and generation=j.generation and status='pending' for update;
 if not found then perform private.fail('version_conflict');end if;
 if not exists(select 1 from private.accounts where id=w.owner_id and deleted_at is null) or not exists(select 1 from private.method_qualifications where method=w.plan->>'method' and qualified) then perform private.fail('access_denied');end if;
 if length(provider) not between 1 and 200 or baseline>now()+interval '5 minutes' or baseline<coalesce((j.payload->>'startedAt')::timestamptz,j.available_at) then perform private.fail('invalid_input');end if;
 update private.watches set status='active',remote_state='running',provider_id=provider,baseline_at=baseline,health='healthy',last_success_at=baseline,next_check_at=baseline+((plan->>'intervalHours')||' hours')::interval where id=w.id;
 update private.jobs set state='done',lease_until=null where id=j.id;
 insert into private.checks values(w.id,w.generation,'baseline:'||provider,'baseline',baseline) on conflict do nothing;
 update private.commands set receipt=jsonb_build_object('id',id,'state','succeeded','watchId',w.id,'watchVersion',w.version,'deleted',false,'remoteState','running','updatedAt',now()) where owner_id=w.owner_id and id=j.command_id;
end $$;

revoke all on function public.s3_provider_lease(),public.s3_provider_allowed(uuid,uuid),public.s3_provider_checkpoint(uuid,uuid,text,jsonb) from public,anon,authenticated;
grant execute on function public.s3_provider_lease(),public.s3_provider_allowed(uuid,uuid),public.s3_provider_checkpoint(uuid,uuid,text,jsonb) to service_role;
