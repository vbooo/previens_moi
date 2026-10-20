alter table private.jobs drop constraint jobs_watch_id_generation_kind_key;
create unique index one_lifecycle_job on private.jobs(watch_id,generation,kind) where kind<>'check';
create unique index one_current_check_job on private.jobs(watch_id,generation) where kind='check' and state in ('ready','leased','ambiguous');
create function public.s3_enqueue_due() returns int language plpgsql security definer set search_path='' as $$
declare n int;begin
 perform 1 from private.capacity where id and enabled for update;if not found then return 0;end if;
 insert into private.jobs(watch_id,generation,kind)
 select w.id,w.generation,'check' from private.watches w join private.accounts a on a.id=w.owner_id
 where w.status='active' and w.remote_state='running' and w.next_check_at<=now() and a.deleted_at is null
 and exists(select 1 from private.method_qualifications q where q.method=w.plan->>'method' and q.qualified)
 and not exists(select 1 from private.jobs j where j.watch_id=w.id and j.generation=w.generation and j.state in ('ready','leased','ambiguous'))
 order by w.next_check_at limit 10 on conflict do nothing;
 get diagnostics n=row_count;return n;
end $$;
create function public.s3_provider_event(provider text,check_key text) returns jsonb language sql security definer set search_path='' as $$
 select payload from private.inbox where provider_id=provider and check_id=check_key and payload->>'type'='monitor.check.completed' order by received_at limit 1
$$;
create function public.s3_provider_complete(job_id uuid,lease uuid,observed timestamptz,outcome text,credits bigint) returns void language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;attempt private.provider_attempts;begin
 perform 1 from private.capacity where id for update;
 select * into j from private.jobs where id=job_id and lease_token=lease and state='leased' and lease_until>now() for update;
 if not found then perform private.fail('version_conflict');end if;
 select * into w from private.watches where id=j.watch_id and generation=j.generation for update;
 if not found then perform private.fail('version_conflict');end if;
 select * into attempt from private.provider_attempts where provider_attempts.job_id=j.id and phase='poll';
 if not found or observed<attempt.run_intent_at or observed>now()+interval '5 minutes' then perform private.fail('invalid_input');end if;
 if credits<0 or credits>2 then
  update private.capacity set enabled=false where id;
  update private.jobs set state='ambiguous' where id=j.id;
  update private.watches set health='degraded',remote_state='unknown',next_check_at=null where id=w.id;return;
 end if;
 perform public.s3_settle(j.id,credits);
 if j.kind='start' and outcome='baseline' then
  perform public.s3_confirm_start(j.id,lease,attempt.monitor_id,observed);
 elsif j.kind='check' and outcome in ('no_change','failed') then
  perform public.s3_record_check(w.id,w.generation,attempt.check_id,outcome,observed);
  update private.watches set next_check_at=case when status='active' then now()+((plan->>'intervalHours')||' hours')::interval else null end where id=w.id;
 else perform private.fail('invalid_input');end if;
 update private.jobs set state='done',lease_until=null where id=j.id;
 update private.provider_attempts set phase='done' where provider_attempts.job_id=j.id;
end $$;
revoke all on function public.s3_enqueue_due(),public.s3_provider_event(text,text),public.s3_provider_complete(uuid,uuid,timestamptz,text,bigint) from public,anon,authenticated;
grant execute on function public.s3_enqueue_due(),public.s3_provider_event(text,text),public.s3_provider_complete(uuid,uuid,timestamptz,text,bigint) to service_role;
