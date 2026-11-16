-- Commerce remains OFF until Play configuration and a separate operating budget
-- are qualified. Existing trial grants, used/held amounts and watches stay intact.
create table private.billing_policy(id boolean primary key default true check(id),enabled boolean not null default false,
 test_only boolean not null default true,monitoring_enabled boolean not null default false,
 package_name text not null default 'fr.previensmoi.app');
insert into private.billing_policy(id) values(true);
alter table private.account_trials drop constraint account_trials_end_reason_check;
alter table private.account_trials add constraint account_trials_end_reason_check check(end_reason in ('expired','quota','upgraded'));
create table private.billing_owners(token_hash text primary key,owner_id uuid references private.accounts on delete set null);
create table private.billing_periods(id uuid primary key default gen_random_uuid(),owner_id uuid not null references private.accounts on delete cascade,
 period_key text not null unique,ends_at timestamptz not null,analyses int not null default 0 check(analyses between 0 and 25),creations int not null default 0 check(creations between 0 and 5),
 model_used bigint not null default 0,model_held bigint not null default 0,credit_used int not null default 0,credit_held int not null default 0,
 check(model_used>=0 and model_held>=0 and model_used+model_held<=2000000),check(credit_used>=0 and credit_held>=0 and credit_used+credit_held<=250));
create table private.billing_purchases(token_hash text primary key references private.billing_owners,owner_id uuid not null references private.accounts on delete cascade,
 token text not null,period_id uuid references private.billing_periods,state text not null check(state in ('active','cancelled','grace','hold','paused','expired','pending')),
 active boolean not null,expires_at timestamptz,acknowledged boolean not null,test boolean not null,verified_at timestamptz not null,
 refresh_attempt_at timestamptz not null default now());
create table private.billing_stops(watch_id uuid primary key references private.watches on delete cascade,resume boolean not null);
alter table private.billing_stops enable row level security;
create table private.billing_creations(owner_id uuid not null references private.accounts on delete cascade,command_id uuid not null,period_id uuid not null references private.billing_periods on delete cascade,primary key(owner_id,command_id));
create table private.billing_analyses(owner_id uuid not null references private.accounts on delete cascade,evaluation_id uuid not null,period_id uuid not null references private.billing_periods on delete cascade,primary key(owner_id,evaluation_id));
alter table private.billing_analyses enable row level security;
create table private.billing_attempts(work_key uuid not null,phase text not null,period_id uuid not null references private.billing_periods on delete cascade,primary key(work_key,phase));
alter table private.billing_policy enable row level security;alter table private.billing_owners enable row level security;
alter table private.billing_periods enable row level security;alter table private.billing_purchases enable row level security;
alter table private.billing_creations enable row level security;alter table private.billing_attempts enable row level security;

create function private.has_plus(owner uuid) returns boolean language sql stable set search_path='' as $$
 select exists(select 1 from private.billing_purchases p where p.owner_id=owner and p.active and p.expires_at>now())
$$;
create function private.plus_fresh(owner uuid) returns boolean language sql stable set search_path='' as $$
 select exists(select 1 from private.billing_purchases p,private.billing_policy b where b.id and b.enabled and b.monitoring_enabled
  and p.owner_id=owner and p.active and p.expires_at>now() and p.verified_at>now()-interval '15 minutes')
$$;
create function private.account_active_limit(owner uuid) returns int language sql stable set search_path='' as $$
 select case when private.has_plus(owner) then 2 else (select active_per_account from private.capacity where id) end
$$;
create function private.stop_unentitled_plus() returns void language plpgsql security definer set search_path='' as $$
declare w private.watches;begin
 perform 1 from private.capacity where id for update;
 for w in select * from private.watches where plan->>'accessTier'='plus' and status in ('pending','active','paused') and not private.has_plus(owner_id) for update loop
  insert into private.billing_stops values(w.id,w.status in ('active','pending') and w.baseline_at is not null) on conflict do nothing;
  if w.status in ('active','pending') and w.remote_state<>'stopped' then update private.capacity set reserved=greatest(0,reserved-1) where id;end if;
  update private.watches set status='suspended_by_capacity',remote_state='stopped',next_check_at=null,generation=generation+1,version=version+1 where id=w.id;
  update private.jobs set state='done',lease_until=null where watch_id=w.id and state='ready';
  update private.outbox o set state='cancelled' from private.findings f where o.finding_id=f.id and f.watch_id=w.id and o.state='ready';
  update private.commands c set receipt=jsonb_build_object('id',c.id,'state','failed','watchId',w.id,'updatedAt',now(),'remoteState','stopped','error',jsonb_build_object('code','subscription_required','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null)) where c.owner_id=w.owner_id and c.receipt->>'watchId'=w.id::text and c.receipt->>'state'='pending';
 end loop;
end $$;
create function public.s7_billing_owner() returns uuid language sql security definer set search_path='' as $$select private.owner()$$;
create function public.s7_subscription() returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid;p private.billing_purchases;t private.account_trials;remaining int:=5;b private.billing_policy;begin
 u:=private.owner();perform private.stop_unentitled_plus();select * into b from private.billing_policy where id;
 select * into p from private.billing_purchases where owner_id=u order by (active and expires_at>now()) desc,verified_at desc limit 1;
 if p.period_id is not null then select greatest(0,5-creations) into remaining from private.billing_periods where id=p.period_id;end if;
 select * into t from private.account_trials where owner_id=u;
 return jsonb_build_object('status',case when p.token_hash is null then 'none' when p.expires_at<=now() then 'expired' else p.state end,
  'active',private.has_plus(u),'expiresAt',p.expires_at,'remainingCreations',remaining,'canPurchase',b.enabled,
  'accountId',encode(extensions.digest('previens-moi:play:'||u::text,'sha256'),'hex'),'trialEndsAt',t.ends_at,'trialEnded',t.ended_at is not null,
  'packageName',b.package_name,'productId','previens_moi_plus','basePlanId','monthly');
end $$;
create function public.s7_billing_policy() returns jsonb language sql security definer set search_path='' as $$select to_jsonb(b) from private.billing_policy b where id$$;
create function public.s7_bind_purchase(owner uuid,purchase_token text,metadata jsonb,observed_at timestamptz) returns void language plpgsql security definer set search_path='' as $$
declare h text:=encode(extensions.digest(purchase_token,'sha256'),'hex');p private.billing_purchases;period uuid;period_key text;t private.account_trials;linked text;w private.watches;m private.general_memory;begin
 perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
 if not exists(select 1 from private.billing_policy where id and enabled and (not test_only or metadata->>'test'='true')) then perform private.fail('billing_unavailable');end if;
 if metadata->>'accountId'<>encode(extensions.digest('previens-moi:play:'||owner::text,'sha256'),'hex') or observed_at>now()+interval '2 minutes' then perform private.fail('purchase_account_mismatch');end if;
 if not exists(select 1 from private.accounts where id=owner and deleted_at is null) then perform private.fail('access_denied');end if;
 insert into private.billing_owners values(h,owner) on conflict do nothing;
 if not exists(select 1 from private.billing_owners where token_hash=h and owner_id=owner) then perform private.fail('purchase_account_mismatch');end if;
 linked:=metadata->>'linkedToken';
 if linked is not null and exists(select 1 from private.billing_owners where token_hash=encode(extensions.digest(linked,'sha256'),'hex') and owner_id is distinct from owner) then perform private.fail('purchase_account_mismatch');end if;
 select * into p from private.billing_purchases where token_hash=h for update;
 if found and p.verified_at>observed_at then return;end if;
 if metadata->>'active'='true' then
  period_key:=encode(extensions.digest(h||':'||(metadata->>'orderId'),'sha256'),'hex');
  insert into private.billing_periods(owner_id,period_key,ends_at) values(owner,period_key,(metadata->>'expiresAt')::timestamptz) on conflict on constraint billing_periods_period_key_key do update set ends_at=greatest(private.billing_periods.ends_at,excluded.ends_at) returning id into period;
 end if;
 insert into private.billing_purchases(token_hash,owner_id,token,period_id,state,active,expires_at,acknowledged,test,verified_at)
 values(h,owner,purchase_token,coalesce(period,p.period_id),metadata->>'state',(metadata->>'active')::boolean,(metadata->>'expiresAt')::timestamptz,(metadata->>'acknowledged')::boolean,(metadata->>'test')::boolean,observed_at)
 on conflict(token_hash) do update set period_id=excluded.period_id,state=excluded.state,active=excluded.active,expires_at=excluded.expires_at,acknowledged=excluded.acknowledged,test=excluded.test,verified_at=excluded.verified_at,refresh_attempt_at=now();
 if linked is not null then update private.billing_purchases set active=false,state='expired' where token_hash=encode(extensions.digest(linked,'sha256'),'hex') and owner_id=owner;end if;
 -- Upgrading an existing daily trial preserves its sources and baseline. Release
 -- its unused financial commitment; never resurrect completed or failed watches.
 if metadata->>'active'='true' then
  update private.watches set plan=(plan-'trialDays')||'{"accessTier":"plus"}',status=case when status='suspended_by_capacity' then 'paused' else status end,version=version+1 where owner_id=owner and plan->>'trialDays'='7' and status in ('active','paused','completed','suspended_by_capacity');
  select * into t from private.account_trials where owner_id=owner for update;
  if found and t.ended_at is null then
   update private.general_budget set model_committed=model_committed-(t.model_limit-t.model_used-t.model_held),credit_committed=credit_committed-(t.credit_limit-t.credit_used-t.credit_held) where id;
   update private.account_trials set ended_at=now(),end_reason='upgraded' where owner_id=owner;
  end if;
 end if;
 if metadata->>'active'='true' and exists(select 1 from private.billing_policy where id and monitoring_enabled) then
  for w in select a.* from private.watches a join private.billing_stops b on b.watch_id=a.id where a.owner_id=owner and b.resume and a.status='suspended_by_capacity' for update of a loop
   if not exists(select 1 from private.capacity where id and enabled and reserved<slots) or (select count(*) from private.watches where owner_id=owner and status in ('active','pending'))>=2 then exit;end if;
   select * into m from private.general_memory where watch_id=w.id;
   if m.state->>'baselineAt' is null or jsonb_array_length(m.documents)=0 then continue;end if;
   update private.watches set status='active',remote_state='running',generation=generation+1,version=version+1,next_check_at=greatest(now(),last_success_at+interval '24 hours') where id=w.id returning * into w;
   update private.general_memory set generation=w.generation,state=jsonb_set(state,'{generation}',to_jsonb(w.generation)) where watch_id=w.id;
   update private.capacity set reserved=reserved+1 where id;delete from private.billing_stops where watch_id=w.id;
  end loop;
 end if;
 perform private.stop_unentitled_plus();
end $$;
create function public.s7_acknowledged(token_hash text) returns void language sql security definer set search_path='' as $$update private.billing_purchases set acknowledged=true where billing_purchases.token_hash=s7_acknowledged.token_hash$$;
create function public.s7_refresh_candidates(owner uuid default null) returns jsonb language sql security definer set search_path='' as $$
 with selected as(select token_hash from private.billing_purchases where (s7_refresh_candidates.owner is null or owner_id=s7_refresh_candidates.owner)
 and (verified_at<now()-interval '10 minutes' or not acknowledged or expires_at<now()+interval '5 minutes') and refresh_attempt_at<now()-interval '1 minute'
 and (expires_at>now()-interval '60 days' or state='pending') order by verified_at for update skip locked limit 1), updated as(
 update private.billing_purchases p set refresh_attempt_at=now() from selected s where p.token_hash=s.token_hash returning p.owner_id,p.token)
 select coalesce(jsonb_agg(to_jsonb(u)),'[]') from updated u
$$;
create function public.s7_purchase_owner(token_hash text) returns uuid language sql security definer set search_path='' as $$select owner_id from private.billing_purchases where billing_purchases.token_hash=s7_purchase_owner.token_hash$$;
create function public.s7_prepare_proposal(target uuid,token text) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.owner();p private.proposals;begin
 select * into p from private.proposals where id=target and token_hash=encode(extensions.digest(token,'sha256'),'hex') for update;
 if not found then perform private.fail('not_found');end if;
 if private.has_plus(u) and p.claimed_by is null then
  p.payload:=jsonb_set((p.payload-'trialDays')||'{"accessTier":"plus","intervalHours":24,"sourcePolicy":"stable_v1"}','{limits,activePerAccount}','2');
  update private.proposals set payload=p.payload where id=p.id;
  update private.evaluations set payload=jsonb_set(payload,'{proposal}',p.payload) where payload->'proposal'->>'id'=p.id::text;
 end if;return p.payload;
end $$;
alter function public.s6_trial_access() rename to s6_trial_access_before_subscription;
create function public.s6_trial_access() returns boolean language plpgsql security definer set search_path='' as $$begin if private.has_plus(private.owner()) then return true;end if;return public.s6_trial_access_before_subscription();end $$;

-- Account-specific paid limits replace only the core admission predicate.
do $patch$ declare d text;begin
 d:=pg_get_functiondef('public.s3_command_transaction(jsonb)'::regprocedure);
 if strpos(d,'>=cap.active_per_account')=0 then raise exception 'Unexpected command capacity revision';end if;
 execute replace(d,'>=cap.active_per_account','>=private.account_active_limit(u)');
 d:=pg_get_functiondef('public.s3_general_finish_before_workflow(uuid,uuid,jsonb,bigint,integer)'::regprocedure);
 if strpos(d,'p->>''trialDays''=''7''')=0 then raise exception 'Unexpected proposal cadence revision';end if;
 execute replace(d,'p->>''trialDays''=''7''','(p->>''trialDays''=''7'' or p->>''accessTier''=''plus'')');
end $patch$;
alter function public.s3_command(jsonb) rename to s3_command_before_subscription;
create function public.s3_command(body jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.owner();op text:=body->>'operation';r jsonb;period uuid;w private.watches;m private.general_memory;begin
 if not private.valid_command(body) then perform private.fail('invalid_input');end if;
 perform 1 from private.capacity where id for update;perform private.stop_unentitled_plus();
 if exists(select 1 from private.commands where owner_id=u and id=(body->>'id')::uuid) then return public.s3_command_before_subscription(body);end if;
 if not private.has_plus(u) then
  if op='resume' and exists(select 1 from private.watches where id=(body->>'watchId')::uuid and owner_id=u and plan->>'accessTier'='plus') then perform private.fail('subscription_required');end if;
  return public.s3_command_before_subscription(body);
 end if;
 if op in ('pause','delete','resume') then delete from private.billing_stops b using private.watches target where b.watch_id=target.id and target.id=(body->>'watchId')::uuid and target.owner_id=u;end if;
 if op in ('activate','replace','resume') then
  if not exists(select 1 from private.billing_policy where id and enabled and monitoring_enabled) then perform private.fail('billing_unavailable');end if;
  if (select count(*) from private.watches where owner_id=u and status in ('active','pending') and (op='activate' or id<>(body->>'watchId')::uuid))>=2 then perform private.fail('active_watch_limit');end if;
 end if;
 if op in ('activate','replace') then
  perform public.s7_prepare_proposal((body->>'proposalId')::uuid,body->>'proposalToken');
  select p.period_id into period from private.billing_purchases p where p.owner_id=u and p.active and p.expires_at>now() order by p.verified_at desc limit 1;
  perform 1 from private.billing_periods where id=period and creations<5 for update;
  if not found then perform private.fail('monthly_creation_limit');end if;
 elsif op='resume' then
  if not exists(select 1 from private.watches where id=(body->>'watchId')::uuid and owner_id=u and plan->>'method'='web_search' and jsonb_array_length(plan->'sources')<=3) then perform private.fail('unsupported_client');end if;
  update private.watches set plan=(plan-'trialDays')||'{"accessTier":"plus","intervalHours":24,"sourcePolicy":"stable_v1"}' where id=(body->>'watchId')::uuid and owner_id=u and plan->>'method'='web_search';
 end if;
 r:=public.s3_command_before_trial(body);
 if op='resume' then
  select * into w from private.watches where id=(body->>'watchId')::uuid and owner_id=u for update;
  select * into m from private.general_memory where watch_id=w.id;
  if w.status='pending' and m.state->>'baselineAt' is not null and jsonb_array_length(m.documents)>0 then
   update private.general_memory set generation=w.generation,state=jsonb_set(state,'{generation}',to_jsonb(w.generation)) where watch_id=w.id;
   update private.watches set status='active',remote_state='running',baseline_at=(m.state->>'baselineAt')::timestamptz,health='healthy',next_check_at=greatest(now(),w.last_success_at+interval '24 hours') where id=w.id;
   update private.jobs set state='done',lease_until=null where watch_id=w.id and generation=w.generation and kind='start' and state='ready';
   r:=jsonb_build_object('id',body->>'id','state','succeeded','watchId',w.id,'watchVersion',w.version,'deleted',false,'remoteState','running','updatedAt',now());
   update private.commands set receipt=r where owner_id=u and id=(body->>'id')::uuid;
  end if;
 end if;
 if op in ('activate','replace') then
  insert into private.billing_creations values(u,(body->>'id')::uuid,period);
  update private.billing_periods set creations=creations+1 where id=period;
 end if;
 return r;
end $$;

-- Charge paid periods in addition to (never instead of) the unchanged global
-- financial stop. No paid allowance increases the currently authorized 2 EUR.
alter function private.reserve_trial_phase(uuid,bigint,int) rename to reserve_phase_before_subscription;
create function private.reserve_trial_phase(target uuid,usd bigint,credits int) returns boolean language plpgsql security definer set search_path='' as $$
declare period uuid;paid boolean;begin
 select w.plan->>'accessTier'='plus',p.period_id into paid,period from private.general_work x join private.jobs j on j.id=x.job_id join private.watches w on w.id=j.watch_id
 left join lateral(select period_id from private.billing_purchases where owner_id=w.owner_id and active and expires_at>now() order by verified_at desc limit 1) p on true where x.id=target;
 if coalesce(paid,false) then
  if period is null or not exists(select 1 from private.billing_periods where id=period and model_used+model_held+usd<=2000000 and credit_used+credit_held+credits<=250 for update) then return false;end if;
  if not private.reserve_phase_before_subscription(target,usd,credits) then return false;end if;
  insert into private.billing_attempts select id,phase,period from private.general_work where id=target;
  update private.billing_periods set model_held=model_held+usd,credit_held=credit_held+credits where id=period;return true;
 end if;return private.reserve_phase_before_subscription(target,usd,credits);
end $$;
create function private.settle_paid_phase() returns trigger language plpgsql security definer set search_path='' as $$
declare period uuid;begin
 if old.state<>'reserved' or new.state='reserved' then return new;end if;
 select period_id into period from private.billing_attempts where work_key=new.work_key and phase=new.phase;
 if new.actual is not null then update private.billing_periods set model_held=model_held-new.model_reserved,model_used=model_used+new.actual where id=period;end if;
 if new.credits is not null then update private.billing_periods set credit_held=credit_held-new.credits_reserved,credit_used=credit_used+new.credits where id=period;end if;
 return new;
end $$;
create trigger settle_paid_phase after update on private.general_attempts for each row execute function private.settle_paid_phase();
alter function public.s3_enqueue_due() rename to s3_enqueue_due_before_subscription;
create function public.s3_enqueue_due() returns int language plpgsql security definer set search_path='' as $$begin perform private.stop_unentitled_plus();return public.s3_enqueue_due_before_subscription();end $$;
alter function public.s3_read(text,uuid,int,text) rename to s3_read_before_subscription;
create function public.s3_read(route text,resource uuid default null,page_limit int default 20,page_cursor text default null) returns jsonb language plpgsql security definer set search_path='' as $$begin perform private.owner();perform private.stop_unentitled_plus();return public.s3_read_before_subscription(route,resource,page_limit,page_cursor);end $$;
alter function public.s3_general_finish(uuid,uuid,jsonb,bigint,int) rename to s3_general_finish_before_subscription;
create function public.s3_general_finish(target uuid,lease_id uuid,result jsonb,actual bigint,credits int) returns void language plpgsql security definer set search_path='' as $$begin perform private.stop_unentitled_plus();perform public.s3_general_finish_before_subscription(target,lease_id,result,actual,credits);end $$;
alter function private.watch_json(private.watches) rename to watch_json_before_subscription;
create function private.watch_json(w private.watches) returns jsonb language sql stable set search_path='' as $$select private.watch_json_before_subscription(w)||case when w.plan->>'accessTier'='plus' then jsonb_build_object('subscriptionEnded',not private.has_plus(w.owner_id)) else '{}'::jsonb end$$;

create function private.erase_billing_data() returns trigger language plpgsql security definer set search_path='' as $$begin
 if old.deleted_at is null and new.deleted_at is not null then
  update private.billing_owners set owner_id=null where owner_id=new.id;
  delete from private.billing_purchases where owner_id=new.id;delete from private.billing_periods where owner_id=new.id;
 end if;return new;
end $$;
create trigger erase_billing_data after update of deleted_at on private.accounts for each row execute function private.erase_billing_data();

-- Only paid work needs fresh Google state; a provider outage delays it instead
-- of consuming resources using a stale or revoked purchase.
create function private.plus_work_allowed(target uuid) returns boolean language sql stable set search_path='' as $$
 select coalesce((select w.plan->>'accessTier' is distinct from 'plus' or private.plus_fresh(w.owner_id) from private.general_work x join private.jobs j on j.id=x.job_id join private.watches w on w.id=j.watch_id where x.id=target),true)
$$;
do $lease$ declare d text;begin
 d:=pg_get_functiondef('public.s3_general_lease()'::regprocedure);
 if strpos(d,'a.plan->>''method''=''web_search''')=0 or strpos(d,'a.state=''ready''')=0 then raise exception 'Unexpected general lease revision';end if;
 d:=replace(d,'a.plan->>''method''=''web_search''','a.plan->>''method''=''web_search'' and (a.plan->>''accessTier'' is distinct from ''plus'' or private.plus_fresh(a.owner_id))');
 d:=replace(d,'a.state=''ready''','a.state=''ready'' and private.plus_work_allowed(a.id)');execute d;
end $lease$;
create function public.s7_analysis_access(evaluation_id uuid) returns boolean language plpgsql security definer set search_path='' as $$
declare u uuid:=private.owner();period uuid;begin
 if not private.has_plus(u) then return public.s6_trial_access_before_subscription();end if;
 perform 1 from private.capacity where id for update;
 if exists(select 1 from private.billing_analyses where owner_id=u and billing_analyses.evaluation_id=s7_analysis_access.evaluation_id) then return true;end if;
 select p.period_id into period from private.billing_purchases p where p.owner_id=u and p.active and p.expires_at>now() order by p.verified_at desc limit 1;
 update private.billing_periods set analyses=analyses+1 where id=period and analyses<25;
 if not found then perform private.fail('monthly_analysis_limit');end if;
 insert into private.billing_analyses values(u,evaluation_id,period);return true;
end $$;

-- A default-deny grant list. All renamed/internal entry points remain inaccessible.
do $grants$ declare f record;begin
 for f in select p.oid::regprocedure signature from pg_proc p join pg_namespace n on n.oid=p.pronamespace where
  (n.nspname='private' and p.proname in ('plus_work_allowed','has_plus','plus_fresh','account_active_limit','stop_unentitled_plus','reserve_trial_phase','reserve_phase_before_subscription','settle_paid_phase','watch_json','watch_json_before_subscription','erase_billing_data'))
  or(n.nspname='public' and (p.proname like 's7_%' or p.proname in ('s6_trial_access_before_subscription','s3_command_before_subscription','s3_enqueue_due_before_subscription','s3_read_before_subscription','s3_general_finish_before_subscription')))
 loop execute format('revoke all on function %s from public,anon,authenticated,service_role',f.signature);end loop;
end $grants$;
revoke all on function public.s6_trial_access(),public.s3_command(jsonb),public.s3_read(text,uuid,int,text),public.s3_enqueue_due(),public.s3_general_finish(uuid,uuid,jsonb,bigint,int) from public,anon,authenticated,service_role;
grant execute on function public.s7_analysis_access(uuid),public.s7_billing_owner(),public.s7_subscription(),public.s7_prepare_proposal(uuid,text),public.s6_trial_access(),public.s3_command(jsonb),public.s3_read(text,uuid,int,text) to authenticated;
grant execute on function public.s7_billing_policy(),public.s7_bind_purchase(uuid,text,jsonb,timestamptz),public.s7_acknowledged(text),public.s7_refresh_candidates(uuid),public.s7_purchase_owner(text),public.s3_enqueue_due(),public.s3_general_finish(uuid,uuid,jsonb,bigint,int) to service_role;
