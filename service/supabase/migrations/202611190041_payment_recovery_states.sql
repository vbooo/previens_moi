-- A past expiry removes entitlement, but hold/paused remain recoverable states.
-- Reading an old expired token must not hide the current recoverable subscription.
create or replace function public.s7_subscription() returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid;p private.billing_purchases;t private.account_trials;remaining int:=5;b private.billing_policy;begin
 u:=private.owner();perform private.stop_unentitled_plus();select * into b from private.billing_policy where id;
 select * into p from private.billing_purchases where owner_id=u
 order by (active and expires_at>now()) desc,
  (state in ('pending','grace','hold','paused')) desc,expires_at desc nulls last,verified_at desc limit 1;
 if p.period_id is not null then select greatest(0,5-creations) into remaining from private.billing_periods where id=p.period_id;end if;
 select * into t from private.account_trials where owner_id=u;
 return jsonb_build_object('status',case when p.token_hash is null then 'none'
  when p.state in ('active','cancelled') and p.expires_at<=now() then 'expired' else p.state end,
  'active',private.has_plus(u),'expiresAt',p.expires_at,'remainingCreations',remaining,'canPurchase',b.enabled,
  'accountId',encode(extensions.digest('previens-moi:play:'||u::text,'sha256'),'hex'),'trialEndsAt',t.ends_at,'trialEnded',t.ended_at is not null,
  'packageName',b.package_name,'productId','previens_moi_plus','basePlanId','monthly');
end $$;
-- Manual checks refresh the latest current purchase before historical expired ones.
-- Background reconciliation still visits the oldest verification among live states.
create or replace function public.s7_refresh_candidates(owner uuid default null) returns jsonb language sql security definer set search_path='' as $$
 with selected as(select token_hash from private.billing_purchases where (s7_refresh_candidates.owner is null or owner_id=s7_refresh_candidates.owner)
 and (verified_at<now()-interval '10 minutes' or not acknowledged or expires_at<now()+interval '5 minutes') and refresh_attempt_at<now()-interval '1 minute'
 and (expires_at>now()-interval '60 days' or state='pending')
 order by (state<>'expired') desc,
  case when s7_refresh_candidates.owner is not null then expires_at end desc nulls last,
  verified_at for update skip locked limit 1), updated as(
 update private.billing_purchases p set refresh_attempt_at=now() from selected s where p.token_hash=s.token_hash returning p.owner_id,p.token)
 select coalesce(jsonb_agg(to_jsonb(u)),'[]') from updated u
$$;
