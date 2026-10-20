create function public.s3_stop_remember(job_id uuid,lease uuid,provider text) returns void language plpgsql security definer set search_path='' as $$
declare j private.jobs;begin
 perform 1 from private.capacity where id for update;
 select * into j from private.jobs where id=job_id and lease_token=lease and state='leased' and kind in ('stop','erase') and lease_until>now() for update;
 if not found or length(provider) not between 1 and 200 then perform private.fail('version_conflict');end if;
 update private.watches set provider_id=provider where id=j.watch_id and generation=j.generation and (provider_id is null or provider_id=provider);
 if not found then perform private.fail('version_conflict');end if;
end $$;
revoke all on function public.s3_stop_remember(uuid,uuid,text) from public,anon,authenticated;
grant execute on function public.s3_stop_remember(uuid,uuid,text) to service_role;

create or replace function public.s3_stop_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;begin
 select x.* into j from private.jobs x join private.watches w on w.id=x.watch_id
 where x.kind in ('stop','erase') and x.generation=w.generation and x.attempts<3 and x.available_at<=now()
 and (x.state='ready' or (x.state='leased' and x.lease_until<now())) order by x.available_at,x.id for update of x skip locked limit 1;
 if not found then return null;end if;
 update private.jobs set state='leased',attempts=attempts+1,lease_until=now()+interval '90 seconds',lease_token=gen_random_uuid() where id=j.id returning * into j;
 select * into w from private.watches where id=j.watch_id;
 return to_jsonb(j)||jsonb_build_object('pendingCreateId',(select p.id from private.jobs p where p.watch_id=w.id and p.kind='start' and p.state in ('leased','ambiguous') order by p.available_at desc limit 1),'providerId',w.provider_id,'erase',w.status='deleted','uncertainStart',w.provider_id is null and exists(select 1 from private.jobs p where p.watch_id=w.id and p.kind='start' and p.state in ('leased','ambiguous')));
end $$;

create or replace function public.s3_confirm_stop(job_id uuid, lease uuid) returns void language plpgsql security definer set search_path='' as $$
declare target uuid;begin
 perform 1 from private.capacity where id for update;
 select watch_id into target from private.jobs where id=job_id;
 perform 1 from private.watches where id=target for update;
 update private.emissions set state='unknown' where watch_id=target and state='emitting' and expires_at<=now();
 if exists(select 1 from private.emissions where watch_id=target and state='emitting') then perform private.fail('version_conflict');end if;
 if exists(select 1 from private.watches where id=target and provider_id is not null) then
  update private.jobs set state='done',lease_until=null where watch_id=target and kind='start' and state in ('leased','ambiguous');
 end if;
 perform public.s3_confirm_stop_transaction(job_id,lease);
end $$;

create or replace function public.s3_replay_erasure() returns void language plpgsql security definer set search_path='' as $$
begin
 perform 1 from private.capacity where id for update;
 update private.erasure_ledger e set data_purged_at=null,identity_deleted_at=null where exists(select 1 from auth.users u where u.id=e.owner_id) or exists(select 1 from private.accounts a where a.id=e.owner_id);
 perform 1 from private.watches where owner_id in(select owner_id from private.erasure_ledger) order by id for update;
 update private.accounts a set deleted_at=coalesce(a.deleted_at,e.erased_at) from private.erasure_ledger e where a.id=e.owner_id;
 delete from private.devices where owner_id in(select owner_id from private.erasure_ledger);
 delete from private.findings where owner_id in(select owner_id from private.erasure_ledger);
 update private.watches set status='deleted',next_check_at=null,health='degraded',
  generation=generation+case when status='deleted' then 0 else 1 end,
  remote_state=case when remote_state='stopped' and provider_id is null then 'stopped' else 'unknown' end
 where owner_id in(select owner_id from private.erasure_ledger);
 insert into private.jobs(watch_id,generation,kind)
 select id,generation,'erase' from private.watches where owner_id in(select owner_id from private.erasure_ledger) and remote_state<>'stopped'
 on conflict do nothing;
end $$;
