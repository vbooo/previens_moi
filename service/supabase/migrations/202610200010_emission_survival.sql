-- In-flight attempts survive result/device cascades during account/watch deletion.
create table private.emissions(id uuid primary key,watch_id uuid not null,owner_id uuid not null,lease uuid not null,expires_at timestamptz not null,state text not null check(state in ('emitting','accepted','failed','unknown','cancelled')));
alter table private.emissions enable row level security;
alter table private.outbox add column leased_device_token text;
create function private.delivery_snapshot() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.state='leased' and old.state<>'leased' then select token into new.leased_device_token from private.devices where id=new.device_id;end if;
 if new.state='emitting' and old.state='leased' then
  if not exists(select 1 from private.devices where id=new.device_id and token=new.leased_device_token) then perform private.fail('version_conflict');end if;
  insert into private.emissions select new.id,f.watch_id,f.owner_id,new.lease_token,new.lease_until,'emitting' from private.findings f where f.id=new.finding_id;
 end if;
 return new;
end $$;
revoke all on function private.delivery_snapshot() from public,anon,authenticated;
create trigger delivery_snapshot before update on private.outbox for each row execute function private.delivery_snapshot();
create or replace function public.s3_delivery_finish(delivery uuid, lease uuid, outcome text) returns void language plpgsql security definer set search_path='' as $$
begin
 if outcome not in ('accepted','failed','unknown','cancelled') then perform private.fail('invalid_input');end if;
 update private.emissions set state=outcome where id=delivery and emissions.lease=s3_delivery_finish.lease and state='emitting';
 update private.outbox set state=outcome,lease_until=null where id=delivery and lease_token=lease and state in ('leased','emitting');
end $$;
create or replace function public.s3_confirm_stop(job_id uuid, lease uuid) returns void language plpgsql security definer set search_path='' as $$
declare target uuid;begin
 perform 1 from private.capacity where id for update;
 select watch_id into target from private.jobs where id=job_id;
 perform 1 from private.watches where id=target for update;
 update private.emissions set state='unknown' where watch_id=target and state='emitting' and expires_at<=now();
 if exists(select 1 from private.emissions where watch_id=target and state='emitting') then perform private.fail('version_conflict');end if;
 perform public.s3_confirm_stop_transaction(job_id,lease);
end $$;
-- The normal retention purge also removes terminal, content-free attempt records.
alter function public.s3_purge() rename to s3_retention_transaction;
revoke all on function public.s3_retention_transaction() from public,anon,authenticated,service_role;
create function public.s3_purge() returns void language plpgsql security definer set search_path='' as $$
begin
 perform public.s3_retention_transaction();
 delete from private.emissions where expires_at<now()-interval '14 days';
end $$;
revoke all on function public.s3_purge() from public,anon,authenticated;
grant execute on function public.s3_purge() to service_role;

create or replace function public.s3_erasure_candidates() returns jsonb language sql security definer set search_path='' as $$
 select coalesce(jsonb_agg(owner_id),'[]') from (
 select e.owner_id from private.erasure_ledger e where e.identity_deleted_at is null
 and not exists(select 1 from private.watches w where w.owner_id=e.owner_id and (w.remote_state<>'stopped' or w.provider_id is not null))
 and not exists(select 1 from private.jobs j join private.watches w on w.id=j.watch_id where w.owner_id=e.owner_id and j.kind='start' and j.state in ('leased','ambiguous'))
 and not exists(select 1 from private.emissions x where x.owner_id=e.owner_id and x.state='emitting' and x.expires_at>now())
 and not exists(select 1 from private.reservations r join private.watches w on w.id=r.watch_id where w.owner_id=e.owner_id and r.state='reserved')
 order by e.erased_at limit 10) eligible
$$;

create or replace function public.s3_erase_data(target uuid) returns void language plpgsql security definer set search_path='' as $$
begin
 perform 1 from private.capacity where id for update;
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
