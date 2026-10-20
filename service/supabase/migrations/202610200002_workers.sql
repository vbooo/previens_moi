-- Internal operations are callable only with the server service role.
create function public.s3_inbox(event_id text, provider_id text, check_id text, payload jsonb) returns void language plpgsql security definer set search_path='' as $$
begin
 if not exists(select 1 from private.watches w where w.provider_id=s3_inbox.provider_id and status in ('active','pending')) then perform private.fail('not_found'); end if;
 insert into private.inbox values(event_id,provider_id,check_id,payload,now()) on conflict do nothing;
 if not found and exists(select 1 from private.inbox i where i.event_id=s3_inbox.event_id and (i.provider_id<>s3_inbox.provider_id or i.check_id<>s3_inbox.check_id or i.payload<>s3_inbox.payload)) then perform private.fail('command_conflict');end if;
 -- Reconciliation through the authenticated provider API is required before qualification.
end $$;
create function public.s3_reserve(reservation_id uuid, target uuid, units bigint) returns void language plpgsql security definer set search_path='' as $$
declare c private.capacity; r private.reservations; begin
 if units<=0 then perform private.fail('invalid_input'); end if;
 select * into c from private.capacity where id for update;
 select * into r from private.reservations where id=reservation_id;
 if found then
  if r.watch_id<>target or r.units<>s3_reserve.units then perform private.fail('command_conflict'); end if;return;
 end if;
 if not c.enabled or c.used_units+c.reserved_units+units>c.budget_units then perform private.fail('quota_exceeded'); end if;
 if not exists(select 1 from private.watches w join private.accounts a on a.id=w.owner_id where w.id=target and w.status in ('pending','active') and a.deleted_at is null) then perform private.fail('not_found'); end if;
 update private.capacity set reserved_units=reserved_units+units where id;
 insert into private.reservations(id,watch_id,units) values(reservation_id,target,units);
end $$;
create function public.s3_settle(reservation_id uuid, actual bigint) returns void language plpgsql security definer set search_path='' as $$
declare r private.reservations; c private.capacity; begin
 select * into c from private.capacity where id for update;
 select * into r from private.reservations where id=reservation_id for update;
 if not found or actual<0 then perform private.fail('invalid_input'); end if;
 if r.state<>'reserved' then
  if r.actual_units is distinct from actual then perform private.fail('command_conflict');end if;return;
 end if;
 if actual>r.units then
  -- Unexpected spend freezes execution. Preserve the reservation for manual reconciliation.
  update private.capacity set enabled=false where id;
  update private.watches set health='degraded',next_check_at=null where id=r.watch_id;
  return;
 end if;
 update private.capacity set reserved_units=reserved_units-r.units,used_units=used_units+actual where id;
 update private.reservations set state=case when actual=0 then 'released' else 'consumed' end,actual_units=actual where id=reservation_id;
end $$;
create function public.s3_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.jobs; begin
 -- A lost create response must be reconciled, never blindly retried.
 update private.jobs set state='ambiguous',lease_until=null where kind='start' and state='leased' and lease_until<now();
 update private.jobs set state='failed',lease_until=null where state='leased' and lease_until<now() and attempts>=3;
 select * into j from private.jobs where (state='ready' or (state='leased' and lease_until<now() and kind<>'start')) and available_at<=now() and attempts<3 order by available_at,id for update skip locked limit 1;
 if not found then return null; end if;
 update private.jobs set state='leased',attempts=attempts+1,lease_until=now()+interval '60 seconds',lease_token=gen_random_uuid() where id=j.id returning * into j;
 return to_jsonb(j);
end $$;
-- Only an adapter that has independently reconciled baseline/remote state may acknowledge.
create function public.s3_confirm_stop(job_id uuid, lease uuid) returns void language plpgsql security definer set search_path='' as $$
declare j private.jobs; w private.watches; begin
 perform 1 from private.capacity where id for update;
 select * into j from private.jobs where id=job_id and kind in ('stop','erase') and state='leased' and lease_token=lease and lease_until>now() for update;
 if not found then perform private.fail('version_conflict');end if;
 select * into w from private.watches where id=j.watch_id for update;
 if w.generation<>j.generation then perform private.fail('version_conflict');end if;
 if w.remote_state<>'stopped' then update private.capacity set reserved=reserved-1 where id;end if;
 update private.watches set remote_state='stopped',provider_id=null where id=w.id;
 update private.jobs set state='done',lease_until=null where id=j.id;
 update private.commands set receipt=jsonb_build_object('id',id,'state','succeeded','watchId',w.id,'watchVersion',case when w.status='deleted' then null else w.version end,'deleted',w.status='deleted','remoteState','stopped','updatedAt',now()) where owner_id=w.owner_id and id=j.command_id;
end $$;
-- Atomic durable result + per-device intention; payload must have passed adapter qualification.
create function public.s3_record_check(target uuid, gen bigint, check_key text, outcome text, observed timestamptz, fact_key text default null, finding jsonb default null) returns uuid language plpgsql security definer set search_path='' as $$
declare w private.watches; f uuid; begin
 select * into w from private.watches where id=target for update;
 if not found or w.status<>'active' or w.generation<>gen or exists(select 1 from private.accounts where id=w.owner_id and deleted_at is not null) then return null;end if;
 if observed>now()+interval '5 minutes' or observed<coalesce(w.last_success_at,w.baseline_at) then return null;end if;
 insert into private.checks(watch_id,generation,check_id,outcome,observed_at) values(target,gen,check_key,outcome,observed) on conflict do nothing;
 if not found then return null;end if;
 if outcome in ('failed','quota') then update private.watches set last_failure_at=observed,health='degraded' where id=target;return null;end if;
 if outcome not in ('no_change','finding') then perform private.fail('invalid_input');end if;
 update private.watches set last_success_at=observed,health='healthy' where id=target;
 if outcome='finding' then
  if finding is null or length(fact_key)=0 or finding->>'watchId'<>target::text or (finding->>'planVersion')::bigint<>(w.plan->>'version')::bigint or jsonb_array_length(finding->'evidence')<1 then perform private.fail('invalid_input');end if;
  f:=(finding->>'id')::uuid;
  insert into private.findings(id,watch_id,owner_id,generation,fact_key,payload) values(f,target,w.owner_id,gen,fact_key,finding) on conflict on constraint findings_watch_id_generation_fact_key_key do nothing;
  if not found then return null;end if;
  insert into private.outbox(finding_id,device_id,generation) select f,id,gen from private.devices where owner_id=w.owner_id on conflict do nothing;
  if w.plan->'draft'->>'mode'='once' then
   update private.watches set status='completed',next_check_at=null,remote_state='stopping' where id=target;
   insert into private.jobs(watch_id,generation,kind) values(target,gen,'stop') on conflict do nothing;
  end if;
 end if;
 return f;
end $$;
-- Lease contains identifiers only; authorization is checked again by the sender before HTTP.
create function public.s3_delivery_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare o private.outbox; r jsonb; begin
 update private.outbox set state='unknown' where state='leased' and lease_until<now();
 select x.* into o from private.outbox x join private.findings f on f.id=x.finding_id join private.watches w on w.id=f.watch_id join private.accounts a on a.id=w.owner_id join private.devices d on d.id=x.device_id
 where x.state='ready' and x.attempts<3 and x.created_at>now()-interval '24 hours' and w.status in ('active','completed') and w.generation=x.generation and a.deleted_at is null and d.owner_id=w.owner_id for update of x,w skip locked limit 1;
 if not found then return null;end if;
 update private.outbox set state='leased',attempts=attempts+1,lease_until=now()+interval '30 seconds',lease_token=gen_random_uuid() where id=o.id returning * into o;
 select jsonb_build_object('id',o.id,'lease',o.lease_token,'token',d.token,'findingId',f.id,'watchId',w.id,'planVersion',w.plan->>'version') into r from private.findings f join private.watches w on w.id=f.watch_id join private.devices d on d.id=o.device_id where f.id=o.finding_id;
 return r;
end $$;
create function public.s3_delivery_allowed(delivery uuid, lease uuid) returns boolean language sql security definer set search_path='' as $$
 select exists(select 1 from private.outbox o join private.findings f on f.id=o.finding_id join private.watches w on w.id=f.watch_id join private.accounts a on a.id=w.owner_id join private.devices d on d.id=o.device_id where o.id=delivery and o.lease_token=lease and o.state='leased' and o.lease_until>now() and w.status in ('active','completed') and w.generation=o.generation and a.deleted_at is null and d.owner_id=w.owner_id)
$$;
create function public.s3_delivery_finish(delivery uuid, lease uuid, outcome text) returns void language plpgsql security definer set search_path='' as $$
begin
 if outcome not in ('accepted','failed','unknown','cancelled') then perform private.fail('invalid_input');end if;
 update private.outbox set state=outcome,lease_until=null where id=delivery and lease_token=lease and state='leased';
end $$;
create function public.s3_purge() returns void language plpgsql security definer set search_path='' as $$
begin
 delete from private.findings where created_at<now()-interval '90 days';
 delete from private.inbox where received_at<now()-interval '14 days';
 delete from private.checks where observed_at<now()-interval '14 days';
 delete from private.commands where created_at<now()-interval '14 days';
 delete from private.evaluations where created_at<now()-interval '24 hours';
 delete from private.proposals where expires_at<now() and claimed_by is null;
 update private.proposals set payload='{}'::jsonb where expires_at<now() and claimed_by is not null;
 update private.watches set plan='{}'::jsonb where status='deleted' and remote_state='stopped';
 delete from private.previews p where expires_at<now() and not exists(select 1 from private.proposals where token_hash=p.token_hash) and not exists(select 1 from private.evaluations where token_hash=p.token_hash);
 -- Minimal erasure ledger intentionally retained for restore replay, never exposed to users.
end $$;
revoke all on function public.s3_inbox(text,text,text,jsonb),public.s3_reserve(uuid,uuid,bigint),public.s3_settle(uuid,bigint),public.s3_lease(),public.s3_confirm_stop(uuid,uuid),public.s3_record_check(uuid,bigint,text,text,timestamptz,text,jsonb),public.s3_delivery_lease(),public.s3_delivery_allowed(uuid,uuid),public.s3_delivery_finish(uuid,uuid,text),public.s3_purge() from public,anon,authenticated;
grant execute on function public.s3_inbox(text,text,text,jsonb),public.s3_reserve(uuid,uuid,bigint),public.s3_settle(uuid,bigint),public.s3_lease(),public.s3_confirm_stop(uuid,uuid),public.s3_record_check(uuid,bigint,text,text,timestamptz,text,jsonb),public.s3_delivery_lease(),public.s3_delivery_allowed(uuid,uuid),public.s3_delivery_finish(uuid,uuid,text),public.s3_purge() to service_role;
