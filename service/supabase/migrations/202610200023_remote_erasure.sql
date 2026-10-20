alter table private.provider_bindings add column erased_at timestamptz;
-- Unsettled financial bounds must survive personal-data erasure without retaining its plan/owner.
create table private.unsettled_costs(id uuid primary key,units bigint not null,created_at timestamptz not null default now());
alter table private.unsettled_costs enable row level security;
create function public.s3_remote_erasure_pending() returns jsonb language sql security definer set search_path='' as $$
 select coalesce(jsonb_agg(provider_id),'[]') from (select provider_id from private.provider_bindings where erased_at is null and owner_id in(select owner_id from private.erasure_ledger) order by provider_id limit 10) t
$$;
create function public.s3_remote_erased(provider text) returns void language sql security definer set search_path='' as $$
 update private.provider_bindings set erased_at=coalesce(erased_at,now()) where provider_id=provider and owner_id in(select owner_id from private.erasure_ledger)
$$;
revoke all on function public.s3_remote_erasure_pending(),public.s3_remote_erased(text) from public,anon,authenticated;
grant execute on function public.s3_remote_erasure_pending(),public.s3_remote_erased(text) to service_role;

create or replace function public.s3_erasure_candidates() returns jsonb language sql security definer set search_path='' as $$
 select coalesce(jsonb_agg(owner_id),'[]') from (
 select e.owner_id from private.erasure_ledger e where e.identity_deleted_at is null
 and not exists(select 1 from private.watches w where w.owner_id=e.owner_id and (w.remote_state<>'stopped' or w.provider_id is not null))
 and not exists(select 1 from private.jobs j join private.watches w on w.id=j.watch_id where w.owner_id=e.owner_id and j.kind='start' and j.state in ('leased','ambiguous'))
 and not exists(select 1 from private.emissions x where x.owner_id=e.owner_id and x.state='emitting' and x.expires_at>now())
 and not exists(select 1 from private.provider_bindings b where b.owner_id=e.owner_id and b.erased_at is null)
 order by e.erased_at limit 10) eligible
$$;


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
 insert into private.unsettled_costs(id,units) select r.id,r.units from private.reservations r join private.watches w on w.id=r.watch_id where w.owner_id=target and r.state='reserved' on conflict do nothing;
 delete from private.reservations where watch_id in(select id from private.watches where owner_id=target);
 delete from private.watches where owner_id=target;
 delete from private.commands where owner_id=target;
 delete from private.evaluations where token_hash in(select token_hash from private.proposals where claimed_by=target);
 delete from private.proposals where claimed_by=target;
 delete from private.provider_bindings where owner_id=target;
 delete from private.accounts where id=target;
 update private.erasure_ledger set data_purged_at=coalesce(data_purged_at,now()) where owner_id=target;
end $$;

create or replace function public.s3_stop_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;begin
 select x.* into j from private.jobs x join private.watches w on w.id=x.watch_id
 where x.kind in ('stop','erase') and x.generation=w.generation and x.attempts<3 and x.available_at<=now()
 and (x.state='ready' or (x.state='leased' and x.lease_until<now())) order by x.available_at,x.id for update of x skip locked limit 1;
 if not found then return null;end if;
 update private.jobs set state='leased',attempts=attempts+1,lease_until=now()+interval '90 seconds',lease_token=gen_random_uuid() where id=j.id returning * into j;
 select * into w from private.watches where id=j.watch_id;
 return to_jsonb(j)||jsonb_build_object('providerId',w.provider_id,'erase',w.status='deleted','uncertainStart',w.provider_id is null and exists(select 1 from private.jobs p where p.watch_id=w.id and p.kind='start' and p.state in ('leased','ambiguous')));
end $$;
