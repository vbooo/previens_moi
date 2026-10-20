create or replace function public.s3_provider_checkpoint(job_id uuid,lease uuid,event text,value jsonb default '{}'::jsonb) returns boolean language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;begin
 perform 1 from private.capacity where id for update;
 select * into j from private.jobs where id=job_id and lease_token=lease and state='leased' and lease_until>now() for update;
 if not found then
  if event='unknown' and exists(select 1 from private.jobs where id=job_id and lease_token=lease and state='ambiguous') then return true;end if;
  perform private.fail('version_conflict');
 end if;
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
