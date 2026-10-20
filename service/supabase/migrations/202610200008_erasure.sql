-- Preserve remote identity linkage until account purge, including after confirmed stop.
create table private.provider_bindings(provider_id text primary key, owner_id uuid not null);
alter table private.provider_bindings enable row level security;
insert into private.provider_bindings select provider_id,owner_id from private.watches where provider_id is not null;
create function private.remember_provider() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.provider_id is not null then
  insert into private.provider_bindings values(new.provider_id,new.owner_id) on conflict do nothing;
  if not exists(select 1 from private.provider_bindings where provider_id=new.provider_id and owner_id=new.owner_id) then perform private.fail('command_conflict');end if;
 end if;
 return new;
end $$;
revoke all on function private.remember_provider() from public,anon,authenticated;
create trigger remember_provider after insert or update of provider_id on private.watches for each row execute function private.remember_provider();
-- Erasure is retried after crashes. Keep only the minimal restore-denial marker.
alter table private.erasure_ledger add column data_purged_at timestamptz, add column identity_deleted_at timestamptz;
create function public.s3_erasure_candidates() returns jsonb language sql security definer set search_path='' as $$
 select coalesce(jsonb_agg(owner_id),'[]') from (
 select e.owner_id from private.erasure_ledger e where e.identity_deleted_at is null
 and not exists(select 1 from private.watches w where w.owner_id=e.owner_id and (w.remote_state<>'stopped' or w.provider_id is not null))
 and not exists(select 1 from private.jobs j join private.watches w on w.id=j.watch_id where w.owner_id=e.owner_id and j.kind='start' and j.state in ('leased','ambiguous'))
 and not exists(select 1 from private.reservations r join private.watches w on w.id=r.watch_id where w.owner_id=e.owner_id and r.state='reserved')
 order by e.erased_at limit 10) eligible
$$;
create function public.s3_erase_data(target uuid) returns void language plpgsql security definer set search_path='' as $$
begin
 perform 1 from private.capacity where id for update;
 perform 1 from private.erasure_ledger where owner_id=target for update;
 if not found or not public.s3_erasure_candidates() @> jsonb_build_array(target) then perform private.fail('version_conflict');end if;
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
create function public.s3_erasure_identity_done(target uuid) returns void language plpgsql security definer set search_path='' as $$
begin
 if exists(select 1 from auth.users where id=target) then perform private.fail('version_conflict');end if;
 update private.erasure_ledger set identity_deleted_at=now() where owner_id=target and data_purged_at is not null;
 if not found then perform private.fail('version_conflict');end if;
end $$;
revoke all on function public.s3_erasure_candidates(),public.s3_erase_data(uuid),public.s3_erasure_identity_done(uuid) from public,anon,authenticated;
grant execute on function public.s3_erasure_candidates(),public.s3_erase_data(uuid),public.s3_erasure_identity_done(uuid) to service_role;
