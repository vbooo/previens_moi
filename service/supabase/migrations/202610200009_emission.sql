-- Atomic emission claim serializes with pause/delete on the watch row. A stop
-- confirmation waits for claimed HTTP attempts; Android still rechecks at receipt.
alter table private.outbox drop constraint outbox_state_check;
alter table private.outbox add constraint outbox_state_check check(state in ('ready','leased','emitting','accepted','cancelled','failed','unknown'));
create or replace function public.s3_delivery_allowed(delivery uuid, lease uuid) returns boolean language plpgsql security definer set search_path='' as $$
declare watch uuid;begin
 select f.watch_id into watch from private.outbox o join private.findings f on f.id=o.finding_id where o.id=delivery;
 if watch is null then return false;end if;
 perform 1 from private.watches where id=watch for update;
 perform 1 from private.devices d join private.outbox o on o.device_id=d.id where o.id=delivery for update of d;
 update private.outbox o set state='emitting' from private.findings f,private.watches w,private.accounts a,private.devices d
 where o.id=delivery and o.lease_token=lease and o.state='leased' and o.lease_until>now() and f.id=o.finding_id and w.id=f.watch_id and a.id=w.owner_id and d.id=o.device_id
 and w.status in ('active','completed') and w.generation=o.generation and a.deleted_at is null and d.owner_id=w.owner_id;
 return found;
end $$;
create or replace function public.s3_delivery_finish(delivery uuid, lease uuid, outcome text) returns void language plpgsql security definer set search_path='' as $$
begin
 if outcome not in ('accepted','failed','unknown','cancelled') then perform private.fail('invalid_input');end if;
 update private.outbox set state=outcome,lease_until=null where id=delivery and lease_token=lease and state in ('leased','emitting');
end $$;
alter function public.s3_confirm_stop(uuid,uuid) rename to s3_confirm_stop_transaction;
revoke all on function public.s3_confirm_stop_transaction(uuid,uuid) from public,anon,authenticated,service_role;
create function public.s3_confirm_stop(job_id uuid, lease uuid) returns void language plpgsql security definer set search_path='' as $$
declare target uuid;begin
 perform 1 from private.capacity where id for update;
 select watch_id into target from private.jobs where id=job_id;
 perform 1 from private.watches where id=target for update;
 update private.outbox o set state='unknown' from private.findings f where f.id=o.finding_id and f.watch_id=target and o.state='emitting' and o.lease_until<=now();
 if exists(select 1 from private.outbox o join private.findings f on f.id=o.finding_id where f.watch_id=target and o.state='emitting') then perform private.fail('version_conflict');end if;
 perform public.s3_confirm_stop_transaction(job_id,lease);
end $$;
revoke all on function public.s3_confirm_stop(uuid,uuid) from public,anon,authenticated;
grant execute on function public.s3_confirm_stop(uuid,uuid) to service_role;
