create or replace function public.s3_replay_erasure() returns void language plpgsql security definer set search_path='' as $$
begin
 perform 1 from private.capacity where id for update;
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
