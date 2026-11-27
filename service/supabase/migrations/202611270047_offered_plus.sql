-- Explicit operator-managed entitlement, separate from verified Google purchases.
-- This migration changes no runtime budget, billing flag, account or grant.
alter table private.general_budget drop constraint general_budget_model_limit_check;
alter table private.general_budget add constraint general_budget_model_limit_check check(model_limit between 0 and 2900000);
alter table private.general_budget drop constraint general_budget_credit_limit_check;
alter table private.general_budget add constraint general_budget_credit_limit_check check(credit_limit between 0 and 435);

create table private.plus_grants(
 id uuid primary key default gen_random_uuid(),
 owner_id uuid not null unique references private.accounts on delete cascade,
 granted_at timestamptz not null default now(), revoked_at timestamptz
);
alter table private.plus_grants enable row level security;
revoke all on private.plus_grants from public,anon,authenticated,service_role;

create function private.offered_plus(owner uuid) returns boolean language sql stable set search_path='' as $$
 select exists(select 1 from private.plus_grants g join private.accounts a on a.id=g.owner_id
 where g.owner_id=owner and g.revoked_at is null and g.granted_at<=now() and a.deleted_at is null)
$$;
create or replace function private.has_plus(owner uuid) returns boolean language sql stable set search_path='' as $$
 select private.offered_plus(owner) or exists(select 1 from private.billing_purchases p where p.owner_id=owner and p.active and p.expires_at>now())
$$;
create or replace function private.plus_fresh(owner uuid) returns boolean language sql stable set search_path='' as $$
 select private.offered_plus(owner) or exists(select 1 from private.billing_purchases p,private.billing_policy b
 where b.id and b.enabled and b.monitoring_enabled and p.owner_id=owner and p.active and p.expires_at>now() and p.verified_at>now()-interval '15 minutes')
$$;

-- A normal quota period, not a purchase or a financial-budget renewal.
create function private.plus_period(owner uuid) returns uuid language plpgsql set search_path='' as $$
declare result uuid;g private.plus_grants;cycle bigint;begin
 select p.period_id into result from private.billing_purchases p where p.owner_id=owner and p.active and p.expires_at>now() order by p.verified_at desc limit 1;
 if result is not null then return result;end if;
 select * into g from private.plus_grants where owner_id=owner and revoked_at is null and granted_at<=now();
 if not found or not private.offered_plus(owner) then return null;end if;
 cycle:=floor(extract(epoch from (now()-g.granted_at))/2592000)::bigint;
 insert into private.billing_periods(owner_id,period_key,ends_at)
 values(owner,'offered:'||g.id::text||':'||cycle::text,g.granted_at+(cycle+1)*interval '30 days')
 on conflict(period_key) do nothing;
 select id into result from private.billing_periods where period_key='offered:'||g.id::text||':'||cycle::text;
 return result;
end $$;

-- Patch only the current admission/charging predicates, retaining transition guards.
do $patch$ declare d text;needle text:='select p.period_id into period from private.billing_purchases p where p.owner_id=u and p.active and p.expires_at>now() order by p.verified_at desc limit 1;';begin
 d:=pg_get_functiondef('public.s3_command(jsonb)'::regprocedure);
 if strpos(d,needle)=0 or strpos(d,'if not exists(select 1 from private.billing_policy where id and enabled and monitoring_enabled)')=0 then raise exception 'Unexpected subscription admission revision';end if;
 d:=replace(d,needle,'period:=private.plus_period(u);');
 d:=replace(d,'if not exists(select 1 from private.billing_policy where id and enabled and monitoring_enabled)',
 'if not (private.offered_plus(u) or exists(select 1 from private.billing_policy where id and enabled and monitoring_enabled))');execute d;
 d:=pg_get_functiondef('public.s7_analysis_access(uuid)'::regprocedure);
 if strpos(d,needle)=0 then raise exception 'Unexpected analysis allowance revision';end if;
 execute replace(d,needle,'period:=private.plus_period(u);');
end $patch$;
create or replace function private.reserve_trial_phase(target uuid,usd bigint,credits int) returns boolean language plpgsql security definer set search_path='' as $$
declare period uuid;paid boolean;begin
 select w.plan->>'accessTier'='plus',case when w.plan->>'accessTier'='plus' then private.plus_period(w.owner_id) end into paid,period
 from private.general_work x join private.jobs j on j.id=x.job_id join private.watches w on w.id=j.watch_id where x.id=target;
 if coalesce(paid,false) then
  if period is null or not exists(select 1 from private.billing_periods where id=period and model_used+model_held+usd<=2000000 and credit_used+credit_held+credits<=250 for update) then return false;end if;
  if not private.reserve_phase_before_subscription(target,usd,credits) then return false;end if;
  insert into private.billing_attempts select id,phase,period from private.general_work where id=target;
  update private.billing_periods set model_held=model_held+usd,credit_held=credit_held+credits where id=period;return true;
 end if;return private.reserve_phase_before_subscription(target,usd,credits);
end $$;

alter function public.s7_subscription() rename to s7_subscription_before_offered;
revoke all on function public.s7_subscription_before_offered() from public,anon,authenticated,service_role;
create function public.s7_subscription() returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.owner();result jsonb;period uuid;p private.billing_periods;begin
 result:=public.s7_subscription_before_offered();
 if private.offered_plus(u) and not exists(select 1 from private.billing_purchases where owner_id=u and active and expires_at>now()) then
  period:=private.plus_period(u);select * into p from private.billing_periods where id=period;
  result:=result||jsonb_build_object('status','offered','active',true,'canPurchase',false,'expiresAt',p.ends_at,'remainingCreations',greatest(0,5-p.creations));
 end if;return result;
end $$;
revoke all on function public.s7_subscription() from public,anon,service_role;
grant execute on function public.s7_subscription() to authenticated;

create function private.revoke_plus_grant(owner uuid) returns void language plpgsql security definer set search_path='' as $$begin
 perform 1 from private.capacity where id for update;
 update private.plus_grants set revoked_at=coalesce(revoked_at,now()) where owner_id=owner;
 perform private.stop_unentitled_plus();
end $$;
create function private.erase_plus_grant() returns trigger language plpgsql security definer set search_path='' as $$begin
 if old.deleted_at is null and new.deleted_at is not null then delete from private.plus_grants where owner_id=new.id;end if;return new;
end $$;
create trigger erase_plus_grant after update of deleted_at on private.accounts for each row execute function private.erase_plus_grant();
revoke all on function private.offered_plus(uuid),private.plus_period(uuid),private.revoke_plus_grant(uuid),private.erase_plus_grant() from public,anon,authenticated,service_role;
