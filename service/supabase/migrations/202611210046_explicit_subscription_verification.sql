-- A manual check must read Google even when the monthly purchase was just checked.
-- Routine foreground reads and the worker retain the existing bounded cadence.
drop function public.s7_refresh_candidates(uuid);
create function public.s7_refresh_candidates(owner uuid default null, force_check boolean default false) returns jsonb language sql security definer set search_path='' as $$
 with selected as(select token_hash from private.billing_purchases where (s7_refresh_candidates.owner is null or owner_id=s7_refresh_candidates.owner)
 and ((s7_refresh_candidates.owner is not null and force_check) or
  ((verified_at<now()-interval '10 minutes' or not acknowledged or expires_at<now()+interval '5 minutes') and refresh_attempt_at<now()-interval '1 minute'))
 and (expires_at>now()-interval '60 days' or state='pending')
 order by (state<>'expired') desc,
  case when s7_refresh_candidates.owner is not null then expires_at end desc nulls last,
  verified_at for update skip locked limit 1), updated as(
 update private.billing_purchases p set refresh_attempt_at=now() from selected s where p.token_hash=s.token_hash returning p.owner_id,p.token)
 select coalesce(jsonb_agg(to_jsonb(u)),'[]') from updated u
$$;
revoke all on function public.s7_refresh_candidates(uuid,boolean) from public,anon,authenticated;
grant execute on function public.s7_refresh_candidates(uuid,boolean) to service_role;
