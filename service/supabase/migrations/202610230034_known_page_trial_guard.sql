-- The old URL pilot has a separate finite credit envelope. Reject a new
-- activation/restart before provider work when its next reservation cannot fit.
-- Never increase/reset that allowance or block independent general preparation.
alter function public.s3_command(jsonb) rename to s3_command_before_workflow;
revoke all on function public.s3_command_before_workflow(jsonb) from public,anon,authenticated,service_role;
create function public.s3_command(body jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid;known boolean;begin
 if not private.valid_command(body) then perform private.fail('invalid_input');end if;
 u:=private.owner();
 if exists(select 1 from private.commands where owner_id=u and id=(body->>'id')::uuid) then return public.s3_command_before_workflow(body);end if;
 if body->>'operation' in ('activate','replace') then known:=exists(select 1 from private.proposals where id=(body->>'proposalId')::uuid and token_hash=encode(extensions.digest(body->>'proposalToken','sha256'),'hex') and payload->>'method'='known_page');
 elsif body->>'operation'='resume' then known:=exists(select 1 from private.watches where id=(body->>'watchId')::uuid and owner_id=u and plan->>'method'='known_page');end if;
 if known and not exists(select 1 from private.capacity where id and used_units+reserved_units+2<=budget_units) then perform private.fail('quota_exceeded');end if;
 return public.s3_command_before_workflow(body);
end $$;
revoke all on function public.s3_command(jsonb) from public,anon;
grant execute on function public.s3_command(jsonb) to authenticated;
