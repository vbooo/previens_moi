create function private.recover_exhausted_jobs() returns void language plpgsql security definer set search_path='' as $$
begin
 perform 1 from private.capacity where id for update;
 update private.jobs set state=case when kind in ('start','check') then 'ambiguous' else 'failed' end,lease_until=null where state='leased' and lease_until<now() and attempts>=3;
 update private.watches w set remote_state='unknown',health='degraded',next_check_at=null where exists(select 1 from private.jobs j where j.watch_id=w.id and j.generation=w.generation and j.state in ('ambiguous','failed'));
 if exists(select 1 from private.jobs where state in ('ambiguous','failed')) then update private.capacity set enabled=false where id;end if;
end $$;
revoke all on function private.recover_exhausted_jobs() from public,anon,authenticated,service_role;
create or replace function public.s3_stop_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;begin
 perform private.recover_exhausted_jobs();
 select x.* into j from private.jobs x join private.watches candidate on candidate.id=x.watch_id
 where x.kind in ('stop','erase') and x.generation=candidate.generation and x.attempts<3 and x.available_at<=now()
 and (x.state='ready' or (x.state='leased' and x.lease_until<now())) order by x.available_at,x.id for update of x skip locked limit 1;
 if not found then return null;end if;
 update private.jobs set state='leased',attempts=attempts+1,lease_until=now()+interval '90 seconds',lease_token=gen_random_uuid() where id=j.id returning * into j;
 select * into w from private.watches where id=j.watch_id;
 return to_jsonb(j)||jsonb_build_object('pendingCreateId',(select p.id from private.jobs p where p.watch_id=w.id and p.kind='start' and p.state in ('leased','ambiguous') order by p.available_at desc limit 1),'providerId',w.provider_id,'erase',w.status='deleted','uncertainStart',w.provider_id is null and exists(select 1 from private.jobs p where p.watch_id=w.id and p.kind='start' and p.state in ('leased','ambiguous')));
end $$;


create or replace function public.s3_provider_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;attempt private.provider_attempts;begin
 perform private.recover_exhausted_jobs();
 select x.* into j from private.jobs x join private.watches candidate on candidate.id=x.watch_id join private.accounts a on a.id=candidate.owner_id
 where x.kind in ('start','check') and x.generation=candidate.generation and x.available_at<=now() and x.attempts<3
 and (x.state='ready' or (x.state='leased' and x.lease_until<now()))
 and candidate.status in ('pending','active') and a.deleted_at is null
 and exists(select 1 from private.method_qualifications q where q.method=candidate.plan->>'method' and q.qualified)
 and exists(select 1 from private.capacity where id and enabled)
 order by x.available_at,x.id for update of x skip locked limit 1;
 if not found then return null;end if;
 update private.jobs set state='leased',attempts=attempts+1,lease_until=now()+interval '120 seconds',lease_token=gen_random_uuid(),payload=payload||jsonb_build_object('startedAt',coalesce(payload->>'startedAt',available_at::text)) where id=j.id returning * into j;
 select * into w from private.watches where id=j.watch_id;select * into attempt from private.provider_attempts where job_id=j.id;
 return to_jsonb(j)||jsonb_build_object('plan',w.plan,'providerId',w.provider_id,'baselineAt',w.baseline_at,'attempt',case when attempt.job_id is null then null else to_jsonb(attempt) end);
end $$;

