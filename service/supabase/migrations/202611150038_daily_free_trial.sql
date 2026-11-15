-- New free offer only. Runtime enablement/funding is a separate audited action.
-- Existing plans keep their confirmed cadence and are never auto-reactivated.
create table private.trial_policy(id boolean primary key default true check(id), enabled boolean not null default false);
insert into private.trial_policy(id) values(true);
alter table private.trial_policy enable row level security;
alter table private.general_budget drop constraint general_budget_model_limit_check;
alter table private.general_budget add constraint general_budget_model_limit_check check(model_limit between 0 and 300000);
alter table private.general_budget drop constraint general_budget_credit_limit_check;
alter table private.general_budget add constraint general_budget_credit_limit_check check(credit_limit between 0 and 200);
alter table private.general_budget add column model_committed bigint not null default 0 check(model_committed>=0),
 add column credit_committed int not null default 0 check(credit_committed>=0),
 add constraint general_budget_commitment_bounds check(model_used+model_held+model_committed<=model_limit and credit_used+credit_held+credit_committed<=credit_limit);

create table private.account_trials(
 owner_id uuid primary key references private.accounts on delete cascade,
 started_at timestamptz not null default now(), ends_at timestamptz not null default now()+interval '7 days',
 ended_at timestamptz, end_reason text check(end_reason in ('expired','quota')),
 model_limit bigint not null default 180000 check(model_limit=180000),
 model_used bigint not null default 0 check(model_used>=0), model_held bigint not null default 0 check(model_held>=0),
 credit_limit int not null default 30 check(credit_limit=30),
 credit_used int not null default 0 check(credit_used>=0),credit_held int not null default 0 check(credit_held>=0),
 check(ends_at=started_at+interval '7 days'), check((ended_at is null)=(end_reason is null)),
 check(model_used+model_held<=model_limit),check(credit_used+credit_held<=credit_limit)
);
create table private.trial_attempts(
 work_key uuid not null,phase text not null,owner_id uuid not null references private.account_trials on delete cascade,
 primary key(work_key,phase)
);
alter table private.account_trials enable row level security;
alter table private.trial_attempts enable row level security;

create function private.close_trial(target uuid,reason text) returns void language plpgsql security definer set search_path='' as $$
declare t private.account_trials;slots_used int;begin
 perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
 select * into t from private.account_trials where owner_id=target for update;
 if not found or t.ended_at is not null then return;end if;
 if reason not in ('expired','quota') then perform private.fail('invalid_input');end if;
 update private.general_budget set model_committed=model_committed-(t.model_limit-t.model_used-t.model_held),
  credit_committed=credit_committed-(t.credit_limit-t.credit_used-t.credit_held) where id;
 update private.account_trials set ended_at=now(),end_reason=reason where owner_id=target;
 select count(*) into slots_used from private.watches where owner_id=target and plan->>'trialDays'='7'
  and status in ('active','pending','paused') and remote_state<>'stopped';
 update private.capacity set reserved=greatest(0,reserved-slots_used) where id;
 update private.watches set status='suspended_by_capacity',remote_state='stopped',next_check_at=null,
  generation=generation+1,version=version+1 where owner_id=target and plan->>'trialDays'='7'
  and status in ('active','pending','paused');
 update private.jobs j set state='done',lease_until=null from private.watches w
  where j.watch_id=w.id and w.owner_id=target and w.plan->>'trialDays'='7' and j.state='ready';
 update private.outbox o set state='cancelled' from private.findings f join private.watches w on w.id=f.watch_id
  where o.finding_id=f.id and w.owner_id=target and w.plan->>'trialDays'='7' and o.state='ready';
 update private.commands c set receipt=jsonb_build_object('id',c.id,'state','failed','watchId',c.receipt->>'watchId',
  'updatedAt',now(),'remoteState','stopped','error',jsonb_build_object('code','free_trial_ended','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null))
  where c.owner_id=target and c.receipt->>'state'='pending' and exists(select 1 from private.watches w where w.id::text=c.receipt->>'watchId' and w.plan->>'trialDays'='7');
end $$;
create function private.expire_trials() returns void language plpgsql security definer set search_path='' as $$
declare t record;begin
 perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
 for t in select owner_id from private.account_trials where ended_at is null and ends_at<=now() order by owner_id loop
  perform private.close_trial(t.owner_id,'expired');
 end loop;
end $$;

-- A funded trial keeps future capacity out of the anonymous/legacy work pool.
-- Move a phase's committed allowance into the existing reservation atomically.
create function private.reserve_trial_phase(target uuid,usd bigint,credits int) returns boolean language plpgsql security definer set search_path='' as $$
declare owner uuid;t private.account_trials;is_trial boolean;begin
 select w.owner_id,w.plan->>'trialDays'='7' into owner,is_trial from private.general_work x
  join private.jobs j on j.id=x.job_id join private.watches w on w.id=j.watch_id where x.id=target;
 if coalesce(is_trial,false) then
  select * into t from private.account_trials where owner_id=owner for update;
  if not found or t.ended_at is not null then return false;end if;
  if t.ends_at<=now() then perform private.close_trial(owner,'expired');return false;end if;
  if t.model_used+t.model_held+usd>t.model_limit or t.credit_used+t.credit_held+credits>t.credit_limit then
   perform private.close_trial(owner,'quota');return false;
  end if;
  insert into private.trial_attempts(work_key,phase,owner_id) select id,phase,owner from private.general_work where id=target;
  update private.account_trials set model_held=model_held+usd,credit_held=credit_held+credits where owner_id=owner;
  update private.general_budget set model_committed=model_committed-usd,credit_committed=credit_committed-credits where id;
  return true;
 end if;
 return exists(select 1 from private.general_budget where id and model_used+model_held+model_committed+usd<=model_limit
  and credit_used+credit_held+credit_committed+credits<=credit_limit);
end $$;

create function private.settle_trial_phase() returns trigger language plpgsql security definer set search_path='' as $$
declare t private.account_trials;owner uuid;refund_model bigint:=0;refund_credits int:=0;begin
 if old.state<>'reserved' or new.state='reserved' then return new;end if;
 select owner_id into owner from private.trial_attempts where work_key=new.work_key and phase=new.phase;
 if not found then return new;end if;
 select * into t from private.account_trials where owner_id=owner for update;
 if not found then return new;end if;
 if new.actual is not null then
  update private.account_trials set model_held=model_held-new.model_reserved,model_used=model_used+new.actual where owner_id=owner;
  refund_model:=new.model_reserved-new.actual;
 end if;
 if new.credits is not null then
  update private.account_trials set credit_held=credit_held-new.credits_reserved,credit_used=credit_used+new.credits where owner_id=owner;
  refund_credits:=new.credits_reserved-new.credits;
 end if;
 -- Unknown portions remain held. Late completion after expiry never reopens an offer.
 if t.ended_at is null then
  update private.general_budget set model_committed=model_committed+refund_model,credit_committed=credit_committed+refund_credits where id;
 end if;
 return new;
end $$;
create trigger settle_trial_phase after update on private.general_attempts for each row execute function private.settle_trial_phase();
create function private.erase_trial_commitment() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if old.ended_at is null then
  update private.general_budget set model_committed=model_committed-(old.model_limit-old.model_used-old.model_held),
   credit_committed=credit_committed-(old.credit_limit-old.credit_used-old.credit_held) where id;
 end if;
 return old;
end $$;
create trigger erase_trial_commitment before delete on private.account_trials for each row execute function private.erase_trial_commitment();

-- Keep the existing lease/state machine and exactly-once cost settlement. Guard
-- replacements so an unexpected schema version fails migration, not silently.
do $patch$ declare definition text;needle text;begin
 definition:=pg_get_functiondef('public.s3_general_lease()'::regprocedure);
 needle:='if not exists(select 1 from private.general_budget where id and model_used+model_held+usd<=model_limit and credit_used+credit_held+credits<=credit_limit) then';
 if strpos(definition,needle)=0 then raise exception 'Unexpected general lease revision';end if;
 definition:=replace(definition,needle,'if not private.reserve_trial_phase(x.id,usd,credits) then');
 execute definition;
 definition:=pg_get_functiondef('public.s4_understanding_lease(uuid,text)'::regprocedure);
 needle:='if not exists(select 1 from private.general_budget where id and model_used+model_held+20000<=model_limit) then';
 if strpos(definition,needle)=0 then raise exception 'Unexpected understanding lease revision';end if;
 definition:=replace(definition,needle,'if not private.reserve_trial_phase(x.id,20000,0) then');
 definition:=replace(definition,'perform private.general_fail(x.id);return null;','perform private.general_fail(x.id);update private.evaluations set payload=jsonb_set(payload,''{error,code}'',''"quota_exceeded"'') where id=x.evaluation_id;return null;');execute definition;
 definition:=pg_get_functiondef('public.s3_general_finish_before_workflow(uuid,uuid,jsonb,bigint,integer)'::regprocedure);
 needle:='case when p->>''sourcePolicy''=''stable_v1'' then ''1'' else ''24'' end';
 if strpos(definition,needle)=0 then raise exception 'Unexpected proposal validation revision';end if;
 definition:=replace(definition,needle,'case when p->>''trialDays''=''7'' then ''24'' when p->>''sourcePolicy''=''stable_v1'' then ''1'' else ''24'' end');execute definition;
end $patch$;

alter function public.s3_preview(text,text,jsonb,uuid) rename to s3_preview_before_trial;
revoke all on function public.s3_preview_before_trial(text,text,jsonb,uuid) from public,anon,authenticated,service_role;
create function public.s3_preview(action text,token text default null,body jsonb default null,resource uuid default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;fresh boolean;begin
 fresh:=action in ('understand','evaluate') and not exists(select 1 from private.evaluations where id=(body->>'id')::uuid);
 r:=public.s3_preview_before_trial(action,token,body,resource);
 if not fresh or not exists(select 1 from private.trial_policy where id and enabled) then return r;end if;
 -- The old literal-monitor shortcut does not fit the new daily scrape envelope.
 -- URLs remain preferred inputs of the general pipeline, with criteria confirmed by the user.
 if r->>'status'='ready' and r->'proposal'->>'method'='known_page' then
  delete from private.proposals where id=(r->'proposal'->>'id')::uuid and claimed_by is null;
  r:=(r-'proposal')||'{"status":"pending"}'::jsonb;
  update private.evaluations set payload=r where id=(body->>'id')::uuid;
  insert into private.general_work(evaluation_id,kind,phase,context) values((body->>'id')::uuid,'admission','interpret',jsonb_build_object('draft',body->'draft','referenceNow',now(),'understandingOnly',action='understand','dailyTrial',true));
 elsif r->>'status'='pending' then
  update private.general_work set context=context||'{"dailyTrial":true}'::jsonb where evaluation_id=(body->>'id')::uuid;
 end if;
 return r;
end $$;
revoke all on function public.s3_preview(text,text,jsonb,uuid) from public,anon,authenticated;
grant execute on function public.s3_preview(text,text,jsonb,uuid) to service_role;

alter function public.s3_command(jsonb) rename to s3_command_before_trial;
revoke all on function public.s3_command_before_trial(jsonb) from public,anon,authenticated,service_role;
create function public.s3_command(body jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid;op text:=body->>'operation';p private.proposals;t private.account_trials;r jsonb;w private.watches;m private.general_memory;begin
 if not private.valid_command(body) then perform private.fail('invalid_input');end if;
 u:=private.owner();
 if exists(select 1 from private.commands where owner_id=u and id=(body->>'id')::uuid) then return public.s3_command_before_trial(body);end if;
 perform private.expire_trials();
 if op in ('activate','resume','replace') and exists(select 1 from private.trial_policy where id and enabled) and exists(select 1 from private.watches where owner_id=u and status in ('active','pending') and (op='activate' or id<>(body->>'watchId')::uuid)) then perform private.fail('active_watch_limit');end if;
 if op in ('activate','replace') then
  select * into p from private.proposals where id=(body->>'proposalId')::uuid and token_hash=encode(extensions.digest(body->>'proposalToken','sha256'),'hex');
  if found and p.payload->>'trialDays'='7' then
   if p.payload->>'intervalHours'<>'24' or p.payload->>'sourcePolicy'<>'stable_v1' or p.payload->>'method'<>'web_search' then perform private.fail('invalid_input');end if;
   select * into t from private.account_trials where owner_id=u for update;
   if found then
    if t.ended_at is not null or t.ends_at<=now() then perform private.fail('free_trial_ended');end if;
   else
    if not exists(select 1 from private.general_budget where id and enabled and qualified and model_used+model_held+model_committed+180000<=model_limit and credit_used+credit_held+credit_committed+30<=credit_limit) then perform private.fail('quota_exceeded');end if;
    insert into private.account_trials(owner_id) values(u);
    update private.general_budget set model_committed=model_committed+180000,credit_committed=credit_committed+30 where id;
   end if;
  elsif found and exists(select 1 from private.trial_policy where id and enabled) then perform private.fail('proposal_expired');end if;
 elsif op='resume' then
  select * into w from private.watches where id=(body->>'watchId')::uuid and owner_id=u;
  if found and w.plan->>'trialDays'='7' and exists(select 1 from private.account_trials where owner_id=u and (ended_at is not null or ends_at<=now())) then perform private.fail('free_trial_ended');end if;
 end if;
 r:=public.s3_command_before_trial(body);
 -- A healthy daily watch resumes its retained memory without a new paid preparation.
 if op='resume' then
  select * into w from private.watches where id=(body->>'watchId')::uuid and owner_id=u for update;
  select * into m from private.general_memory where watch_id=w.id;
  if w.plan->>'trialDays'='7' and w.status='pending' and m.state->>'baselineAt' is not null and jsonb_array_length(m.documents)>0 then
   update private.general_memory set generation=w.generation,state=jsonb_set(state,'{generation}',to_jsonb(w.generation)) where watch_id=w.id;
   update private.watches set status='active',remote_state='running',baseline_at=(m.state->>'baselineAt')::timestamptz,health='healthy',
    next_check_at=greatest(now(),w.last_success_at+interval '24 hours') where id=w.id;
   update private.jobs set state='done',lease_until=null where watch_id=w.id and generation=w.generation and kind='start' and state='ready';
   r:=jsonb_build_object('id',body->>'id','state','succeeded','watchId',w.id,'watchVersion',w.version,'deleted',false,'remoteState','running','updatedAt',now());
   update private.commands set receipt=r where owner_id=u and id=(body->>'id')::uuid;
  end if;
 end if;
 return r;
end $$;
revoke all on function public.s3_command(jsonb) from public,anon;
grant execute on function public.s3_command(jsonb) to authenticated;

alter function public.s3_general_finish(uuid,uuid,jsonb,bigint,int) rename to s3_general_finish_before_trial;
revoke all on function public.s3_general_finish_before_trial(uuid,uuid,jsonb,bigint,int) from public,anon,authenticated,service_role;
do $binding$ declare definition text;begin
 definition:=pg_get_functiondef('public.s3_general_finish_before_trial(uuid,uuid,jsonb,bigint,integer)'::regprocedure);
 definition:=replace(replace(definition,'s3_general_finish.actual','s3_general_finish_before_trial.actual'),'s3_general_finish.credits','s3_general_finish_before_trial.credits');execute definition;
end $binding$;
create function public.s3_general_finish(target uuid,lease_id uuid,result jsonb,actual bigint,credits int) returns void language plpgsql security definer set search_path='' as $$
begin perform private.expire_trials();perform public.s3_general_finish_before_trial(target,lease_id,result,actual,credits);end $$;
revoke all on function public.s3_general_finish(uuid,uuid,jsonb,bigint,int) from public,anon,authenticated;
grant execute on function public.s3_general_finish(uuid,uuid,jsonb,bigint,int) to service_role;

alter function public.s3_enqueue_due() rename to s3_enqueue_due_before_trial;
revoke all on function public.s3_enqueue_due_before_trial() from public,anon,authenticated,service_role;
create function public.s3_enqueue_due() returns int language plpgsql security definer set search_path='' as $$
begin perform private.expire_trials();return public.s3_enqueue_due_before_trial();end $$;
revoke all on function public.s3_enqueue_due() from public,anon,authenticated;
grant execute on function public.s3_enqueue_due() to service_role;

alter function public.s3_read(text,uuid,int,text) rename to s3_read_before_trial;
revoke all on function public.s3_read_before_trial(text,uuid,int,text) from public,anon,authenticated,service_role;
create function public.s3_read(route text,resource uuid default null,page_limit int default 20,page_cursor text default null) returns jsonb language plpgsql security definer set search_path='' as $$
begin perform private.owner();perform private.expire_trials();return public.s3_read_before_trial(route,resource,page_limit,page_cursor);end $$;
revoke all on function public.s3_read(text,uuid,int,text) from public,anon;
grant execute on function public.s3_read(text,uuid,int,text) to authenticated;
alter function private.watch_json(private.watches) rename to watch_json_before_trial;
create function private.watch_json(w private.watches) returns jsonb language sql stable set search_path='' as $$
 select private.watch_json_before_trial(w)||coalesce((select jsonb_build_object('trialEndsAt',t.ends_at,
  'trialEnded',t.ended_at is not null) from private.account_trials t where t.owner_id=w.owner_id and w.plan->>'trialDays'='7'),'{}'::jsonb)
$$;

revoke all on function private.close_trial(uuid,text),private.expire_trials(),private.reserve_trial_phase(uuid,bigint,int),private.settle_trial_phase(),private.erase_trial_commitment(),private.watch_json_before_trial(private.watches),private.watch_json(private.watches) from public,anon,authenticated,service_role;

-- Existing erasure marks an account before deleting its identity. Drop the new
-- personal ledger at that point; opaque in-flight charges remain globally held.
create function private.erase_trial_on_account_deletion() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if old.deleted_at is null and new.deleted_at is not null then delete from private.account_trials where owner_id=new.id;end if;
 return new;
end $$;
create trigger erase_trial_on_account_deletion after update of deleted_at on private.accounts for each row execute function private.erase_trial_on_account_deletion();
revoke all on function private.erase_trial_on_account_deletion() from public,anon,authenticated,service_role;

-- A signed-in client must not pay for a new interpretation after its trial ends.
create function public.s6_trial_access() returns boolean language plpgsql security definer set search_path='' as $$
declare u uuid;begin
 u:=private.owner();
 if exists(select 1 from private.account_trials where owner_id=u and (ended_at is not null or ends_at<=now())) then perform private.fail('free_trial_ended');end if;
 return true;
end $$;
revoke all on function public.s6_trial_access() from public,anon;
grant execute on function public.s6_trial_access() to authenticated;
