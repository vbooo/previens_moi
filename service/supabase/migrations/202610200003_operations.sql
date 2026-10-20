create function private.valid_command(b jsonb) returns boolean language plpgsql immutable set search_path='' as $$
declare op text:=b->>'operation';required text[];k text; begin
 if jsonb_typeof(b)<>'object' then return false;end if;
 required:=case op
  when 'activate' then array['id','operation','proposalId','proposalVersion','proposalToken','acceptPartial']
  when 'replace' then array['id','operation','watchId','expectedVersion','proposalId','proposalVersion','proposalToken','acceptPartial']
  when 'pause' then array['id','operation','watchId','expectedVersion']
  when 'resume' then array['id','operation','watchId','expectedVersion']
  when 'delete' then array['id','operation','watchId','expectedVersion'] else null end;
 if required is null or not b ?& required or b-required<>'{}'::jsonb then return false;end if;
 foreach k in array required loop
  if k in ('expectedVersion','proposalVersion') then
   if jsonb_typeof(b->k)<>'number' or b->>k !~ '^[1-9][0-9]{0,9}$' or (b->>k)::bigint>2147483647 then return false;end if;
  elsif k='acceptPartial' then if jsonb_typeof(b->k)<>'boolean' then return false;end if;
  elsif jsonb_typeof(b->k)<>'string' then return false;
  elsif k in ('id','watchId','proposalId') and b->>k !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$' then return false;
  elsif k='proposalToken' and length(b->>k) not between 32 and 256 then return false;
  end if;
 end loop;
 return true;
end $$;
revoke all on function private.valid_command(jsonb) from public,anon,authenticated;
-- Keep the original transaction implementation private. The public RPC validates even when
-- called directly through PostgREST, so Edge validation is not a security boundary.
alter function public.s3_command(jsonb) rename to s3_command_transaction;
revoke all on function public.s3_command_transaction(jsonb) from public,anon,authenticated,service_role;
create function public.s3_command(body jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
begin
 if not private.valid_command(body) then perform private.fail('invalid_input');end if;
 return public.s3_command_transaction(body);
end $$;
revoke all on function public.s3_command(jsonb) from public,anon;
grant execute on function public.s3_command(jsonb) to authenticated;

create function public.s3_replay_erasure() returns void language plpgsql security definer set search_path='' as $$
begin
 update private.accounts a set deleted_at=coalesce(a.deleted_at,e.erased_at) from private.erasure_ledger e where a.id=e.owner_id;
 delete from private.devices where owner_id in(select owner_id from private.erasure_ledger);
 delete from private.findings where owner_id in(select owner_id from private.erasure_ledger);
 update private.watches set status='deleted',next_check_at=null,health='degraded',
  generation=generation+case when status='deleted' then 0 else 1 end,
  remote_state=case when remote_state='stopped' then 'stopped' else 'unknown' end
 where owner_id in(select owner_id from private.erasure_ledger);
end $$;
revoke all on function public.s3_replay_erasure() from public,anon,authenticated;
grant execute on function public.s3_replay_erasure() to service_role;
-- Only retention is scheduled; there is deliberately no external-provider cron enabled.
create extension if not exists pg_cron;
select cron.schedule('previens-moi-retention','17 * * * *','select public.s3_purge()');
