create or replace function public.s3_admission_finish(target uuid,lease_id uuid,result jsonb,interpretation jsonb,actual bigint default null) returns void language plpgsql security definer set search_path='' as $$
declare j private.admission_jobs;begin
 perform 1 from private.admission_capacity where id for update;
 select * into j from private.admission_jobs where id=target and lease=lease_id and state='leased' and lease_until>now() for update;
 if not found then perform private.fail('version_conflict');end if;
 if result->>'id' is distinct from target::text or result->>'status' is null or result->>'status' not in ('clarification','unsupported','failed') or result->'draftRevision' is distinct from j.request->'draft'->'revision' then perform private.fail('invalid_input');end if;
 -- No ready proposal can be manufactured by a model or anonymous client.
 if actual is not null and (actual<0 or actual>j.reserved_micro_usd) then
  update private.admission_capacity set enabled=false where id;
  update private.admission_jobs set state='unknown' where id=target;
  update private.evaluations set payload=jsonb_build_object('id',target,'draftRevision',j.request->'draft'->'revision','updatedAt',now(),'status','failed','error',jsonb_build_object('code','quota_exceeded','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope','provider')) where id=target;
  return;
 end if;
 update private.admission_jobs set state=case when actual is null then 'unknown' else 'done' end,interpretation=s3_admission_finish.interpretation,actual_micro_usd=actual where id=target;
 if actual is not null then update private.admission_capacity set reserved_micro_usd=reserved_micro_usd-j.reserved_micro_usd,used_micro_usd=used_micro_usd+actual where id;end if;
 update private.evaluations set payload=result where id=target;
end $$;

create or replace function public.s3_erase_data(target uuid) returns void language plpgsql security definer set search_path='' as $$
begin
 perform 1 from private.capacity where id for update;
 perform 1 from private.watches where owner_id=target order by id for update;
 perform 1 from private.erasure_ledger where owner_id=target for update;
 if not found or not public.s3_erasure_candidates() @> jsonb_build_array(target) then perform private.fail('version_conflict');end if;
 delete from private.emissions where owner_id=target;
 delete from private.devices where owner_id=target;
 delete from private.findings where owner_id=target;
 delete from private.inbox where provider_id in(select provider_id from private.provider_bindings where owner_id=target);
 delete from private.checks where watch_id in(select id from private.watches where owner_id=target);
 delete from private.plan_versions where watch_id in(select id from private.watches where owner_id=target);
 delete from private.jobs where watch_id in(select id from private.watches where owner_id=target);
 delete from private.reservations where watch_id in(select id from private.watches where owner_id=target);
 delete from private.watches where owner_id=target;
 delete from private.commands where owner_id=target;
 delete from private.evaluations where token_hash in(select token_hash from private.proposals where claimed_by=target);
 delete from private.proposals where claimed_by=target;
 delete from private.provider_bindings where owner_id=target;
 delete from private.accounts where id=target;
 update private.erasure_ledger set data_purged_at=coalesce(data_purged_at,now()) where owner_id=target;
end $$;

create or replace function public.s3_stop_retry(job_id uuid,lease uuid) returns void language plpgsql security definer set search_path='' as $$
begin
 perform 1 from private.capacity where id for update;
 update private.jobs set state=case when attempts>=3 then 'failed' else 'ready' end,lease_until=null,available_at=now()+interval '30 seconds'
 where id=job_id and lease_token=lease and state='leased' and kind in ('stop','erase');
 update private.watches w set remote_state='unknown',health='degraded',next_check_at=null where exists(select 1 from private.jobs j where j.id=job_id and j.watch_id=w.id and j.state='failed');
 if exists(select 1 from private.jobs where id=job_id and state='failed') then update private.capacity set enabled=false where id;end if;
end $$;

-- Account closure uses the same watch-before-device order as emission claims.
alter function public.s3_delete_account(uuid) rename to s3_delete_account_transaction;
revoke all on function public.s3_delete_account_transaction(uuid) from public,anon,authenticated,service_role;
create function public.s3_delete_account(request_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.owner();begin
 perform 1 from private.capacity where id for update;
 perform 1 from private.watches where owner_id=u order by id for update;
 return public.s3_delete_account_transaction(request_id);
end $$;
revoke all on function public.s3_delete_account(uuid) from public,anon;
grant execute on function public.s3_delete_account(uuid) to authenticated;
