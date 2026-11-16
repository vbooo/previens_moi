-- A proposal can outlive a login. A paid proposal never grants paid access to
-- the next account using that preview token. Keep existing command replays.
do $guards$ declare d text;needle text;begin
 d:=pg_get_functiondef('public.s3_command(jsonb)'::regprocedure);
 needle:='if not private.has_plus(u) then';
 if strpos(d,needle)=0 then raise exception 'Unexpected subscription command revision';end if;
 execute replace(d,needle,needle||$body$
  if op in ('activate','replace') and exists(select 1 from private.proposals where id=(body->>'proposalId')::uuid and payload->>'accessTier'='plus') then perform private.fail('subscription_required');end if;
 $body$);
 -- Buying while preparation is running adopts that same watch and its pending
 -- work. In-flight trial reservations still settle in their original ledger.
 d:=pg_get_functiondef('public.s7_bind_purchase(uuid,text,jsonb,timestamptz)'::regprocedure);
 needle:='and status in (''active'',''paused'',''completed'',''suspended_by_capacity'');';
 if strpos(d,needle)=0 then raise exception 'Unexpected trial adoption revision';end if;
 d:=replace(d,needle,'and status in (''pending'',''active'',''paused'',''completed'',''suspended_by_capacity'');'||$body$
  update private.general_work x set context=jsonb_set(x.context,'{plan}',current_watch.plan)
   from private.jobs j join private.watches current_watch on current_watch.id=j.watch_id
   where x.job_id=j.id and current_watch.owner_id=owner and current_watch.plan->>'accessTier'='plus'
    and x.state in ('ready','leased') and x.context ? 'plan';
 $body$);
 execute d;
end $guards$;
-- A coverage answer can have been constructed before the purchase. Preserve
-- current access authority when accepting its otherwise unchanged source plan.
create or replace function public.s3_general_finish(target uuid,lease_id uuid,result jsonb,actual bigint,credits int)
returns void language plpgsql security definer set search_path='' as $$
begin
 perform private.stop_unentitled_plus();
 if result ? 'preparedPlan' and exists(select 1 from private.general_work x join private.jobs j on j.id=x.job_id join private.watches w on w.id=j.watch_id where x.id=target and w.plan->>'accessTier'='plus') then
  result:=jsonb_set(result,'{preparedPlan}',((result->'preparedPlan')-'trialDays')||'{"accessTier":"plus","intervalHours":24,"sourcePolicy":"stable_v1"}');
 end if;
 perform public.s3_general_finish_before_subscription(target,lease_id,result,actual,credits);
end $$;
revoke all on function public.s3_general_finish(uuid,uuid,jsonb,bigint,int) from public,anon,authenticated;
grant execute on function public.s3_general_finish(uuid,uuid,jsonb,bigint,int) to service_role;
