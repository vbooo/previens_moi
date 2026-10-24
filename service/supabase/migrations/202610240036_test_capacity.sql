-- Explicitly authorized test extension, 2026-10-24. This migration makes the
-- admission policy configurable (maximum three active watches per account) and
-- raises the structural ceiling for already-free Firecrawl credits to 100.
-- It does NOT change runtime limits, spent/held counters, model spending caps,
-- user watches or plans. Runtime configuration is a separate audited operation.
alter table private.capacity add column active_per_account int not null default 1
 check(active_per_account between 1 and 3);
alter table private.general_budget drop constraint general_budget_credit_limit_check;
alter table private.general_budget add constraint general_budget_credit_limit_check
 check(credit_limit between 0 and 100);
create or replace function private.limits() returns jsonb language sql stable set search_path='' as $$
 select jsonb_build_object('activePerAccount',active_per_account,
 'globalAvailable',case when enabled then greatest(0,slots-reserved) else 0 end,
 'previewRemaining',0,'resetsAt',null) from private.capacity where id
$$;

-- Capacity is locked before either admission path: concurrent requests cannot
-- exceed the account or global limit. The original idempotency and fences remain.
create or replace function public.s3_command_transaction(body jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid; i uuid:=(body->>'id')::uuid; op text:=body->>'operation'; fp text; old private.commands; w private.watches; p private.proposals; cap private.capacity; r jsonb; next_plan jsonb; begin
 u:=private.owner();perform private.command_age(i);
 fp:=encode(extensions.digest((body-'proposalToken')::text||coalesce(encode(extensions.digest(body->>'proposalToken','sha256'),'hex'),''),'sha256'),'hex');
 select * into old from private.commands where owner_id=u and id=i;
 if found then if old.fingerprint<>fp then perform private.fail('command_conflict');end if;return old.receipt;end if;
 if op in ('activate','replace') then
  select * into p from private.proposals where id=(body->>'proposalId')::uuid for update;
  if not found or p.token_hash<>encode(extensions.digest(coalesce(body->>'proposalToken',''),'sha256'),'hex') then perform private.fail('not_found');end if;
  if p.claimed_by is not null then perform private.fail('command_conflict');end if;
  if p.expires_at<=now() then perform private.fail('proposal_expired');end if;
  if p.version<>(body->>'proposalVersion')::bigint then perform private.fail('version_conflict');end if;
  if (p.payload->>'partial')::boolean and not (body->>'acceptPartial')::boolean then perform private.fail('invalid_input');end if;
  next_plan:=p.payload;
 end if;
 select * into cap from private.capacity where id for update;
 if op='activate' then
  if not cap.enabled or cap.reserved>=cap.slots or (select count(*) from private.watches where owner_id=u and status in ('active','pending'))>=cap.active_per_account then perform private.fail('quota_exceeded');end if;
  if not exists(select 1 from private.method_qualifications where method=next_plan->>'method' and qualified) then perform private.fail('unsupported');end if;
  insert into private.watches(owner_id,plan,status,remote_state) values(u,next_plan,'pending','starting') returning * into w;
  update private.capacity set reserved=reserved+1 where id;
 else
  select * into w from private.watches where id=(body->>'watchId')::uuid and owner_id=u and status<>'deleted' for update;
  if not found then perform private.fail('not_found');end if;
  if w.version<>(body->>'expectedVersion')::bigint then perform private.fail('version_conflict');end if;
  if op in ('resume','replace') then
   if exists(select 1 from private.jobs where watch_id=w.id and state in ('ready','leased','ambiguous')) then perform private.fail('version_conflict');end if;
   if not cap.enabled then perform private.fail('quota_exceeded');end if;
   if op='resume' then next_plan:=w.plan;end if;
   if not exists(select 1 from private.method_qualifications where method=next_plan->>'method' and qualified) then perform private.fail('unsupported');end if;
   if w.status='paused' and w.remote_state='stopped' then
    if cap.reserved>=cap.slots or (select count(*) from private.watches where owner_id=u and status in ('active','pending') and id<>w.id)>=cap.active_per_account then perform private.fail('quota_exceeded');end if;
    update private.capacity set reserved=reserved+1 where id;
    update private.watches set status='pending',remote_state='starting',generation=generation+1,version=version+1,plan=next_plan,next_check_at=null,baseline_at=null,health='not_checked_yet' where id=w.id returning * into w;
   elsif op='replace' and w.status='active' and w.remote_state='running' then
    -- Keep the old confirmed plan visible until the old execution is stopped.
    update private.watches set remote_state='stopping',generation=generation+1,version=version+1,next_check_at=null where id=w.id returning * into w;
   else perform private.fail('version_conflict');end if;
  elsif op in ('pause','delete') then
   -- Supersede old user operations, while keeping uncertain provider work visible internally.
   update private.commands c set receipt=jsonb_build_object('id',c.id,'state','failed','watchId',w.id,'updatedAt',now(),'remoteState','unknown','error',jsonb_build_object('code','version_conflict','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null))
    where c.owner_id=u and c.receipt->>'watchId'=w.id::text and c.receipt->>'state'='pending';
   update private.watches set status=case when op='delete' then 'deleted' else 'paused' end,version=version+1,generation=generation+1,
    remote_state=case when remote_state='stopped' then 'stopped' else 'stopping' end,next_check_at=null where id=w.id returning * into w;
   update private.jobs set state='done' where watch_id=w.id and state='ready';
   update private.outbox o set state='cancelled' from private.findings f where o.finding_id=f.id and f.watch_id=w.id and o.state='ready';
   if op='delete' then delete from private.findings where watch_id=w.id;end if;
  else perform private.fail('invalid_input');end if;
 end if;
 if op in ('activate','replace') then update private.proposals set claimed_by=u where id=p.id;end if;
 if w.status='pending' then
  insert into private.plan_versions values(w.id,(w.plan->>'id')::uuid,(w.plan->>'version')::bigint,w.plan) on conflict do nothing;
  insert into private.jobs(watch_id,command_id,generation,kind) values(w.id,i,w.generation,'start');
 elsif w.remote_state<>'stopped' then
  insert into private.jobs(watch_id,command_id,generation,kind,payload) values(w.id,i,w.generation,'stop',case when op='replace' then jsonb_build_object('nextPlan',next_plan) else '{}'::jsonb end) on conflict do nothing;
 end if;
 r:=jsonb_build_object('id',i,'state',case when w.status='pending' or w.remote_state<>'stopped' then 'pending' else 'succeeded' end,'watchId',w.id,'updatedAt',now(),'remoteState',w.remote_state);
 if r->>'state'='succeeded' then r:=r||jsonb_build_object('watchVersion',case when op='delete' then null else w.version end,'deleted',op='delete');end if;
 insert into private.commands(owner_id,id,fingerprint,receipt) values(u,i,fp,r);return r;
end $$;
revoke all on function public.s3_command_transaction(jsonb) from public,anon,authenticated,service_role;


-- Publish the effective account policy in newly generated proposals.
create or replace function public.s3_general_finish_before_workflow(target uuid,lease_id uuid,result jsonb,actual bigint,credits int) returns void language plpgsql security definer set search_path='' as $$
declare x private.general_work;a private.general_attempts;j private.jobs;w private.watches;e private.evaluations;r jsonb;p jsonb;d jsonb;f jsonb;observed timestamptz;outcome text;begin
 perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
 select * into a from private.general_attempts where work_key=target and lease=lease_id for update;
 if not found or a.state<>'reserved' then return;end if;
 if actual is not null and (actual<0 or actual>a.model_reserved) or credits is not null and (credits<0 or credits>a.credits_reserved) then update private.general_budget set enabled=false where id;perform private.general_fail(target);return;end if;
 if actual is not null then update private.general_budget set model_held=model_held-a.model_reserved,model_used=model_used+actual where id;end if;
 if credits is not null then update private.general_budget set credit_held=credit_held-a.credits_reserved,credit_used=credit_used+credits where id;end if;
 update private.general_attempts set actual=s3_general_finish_before_workflow.actual,credits=s3_general_finish_before_workflow.credits,state=case when s3_general_finish_before_workflow.actual is null or s3_general_finish_before_workflow.credits is null then 'unknown' else 'settled' end where work_key=target and phase=a.phase;
 select * into x from private.general_work where id=target and lease=lease_id and state='leased' and lease_until>now() for update;
 if not found then return;end if;
 if coalesce((result->>'failed')::boolean,false) or octet_length(result::text)>200000 then perform private.general_fail(target);return;end if;
 if x.job_id is not null then
  select * into j from private.jobs where id=x.job_id for update;select * into w from private.watches where id=j.watch_id and generation=x.generation and status in ('pending','active') and exists(select 1 from private.accounts where id=owner_id and deleted_at is null) for update;
  if not found or not exists(select 1 from private.capacity where id and enabled) or not exists(select 1 from private.general_budget where id and enabled and qualified) then update private.general_work set state='done' where id=target;update private.jobs set state='done' where id=j.id;return;end if;
 end if;
 if x.phase='interpret' then
  if result->'intent'->>'status'='understood' then update private.general_work set context=context||result,phase='collect',state='ready',lease_until=null where id=target;return;end if;
  select * into e from private.evaluations where id=x.evaluation_id;
  r:=jsonb_build_object('id',e.id,'draftRevision',x.context->'draft'->'revision','updatedAt',now());
  if result->'intent'->>'status'='clarify' then r:=r||jsonb_build_object('status','clarification','questions',result->'intent'->'questions');
  elsif result->'intent'->>'status'='unsupported' then r:=r||jsonb_build_object('status','unsupported','reason','private_data','explanation',result->'intent'->>'explanation');else perform private.general_fail(target);return;end if;
  update private.evaluations set payload=r where id=e.id;
 elsif x.phase='collect' then
  if jsonb_typeof(result->'documents')<>'array' or (case when x.context->'plan'->>'sourcePolicy'='stable_v1' then jsonb_array_length(result->'documents') not between 1 and 6 else jsonb_array_length(result->'documents')<>1 or result->>'retrievalHealthy'<>'true' end) then perform private.general_fail(target);return;end if;
  if x.job_id is not null and x.context->'plan'->>'sourcePolicy'='stable_v1' then update private.watches set plan=jsonb_set(plan,'{checkCoverage}',result->'checkCoverage') where id=w.id;end if;
  update private.general_work set context=context||result,phase='decide',state='ready',lease_until=null where id=target;return;
 elsif x.kind='admission' then
  select * into e from private.evaluations where id=x.evaluation_id;r:=jsonb_build_object('id',e.id,'draftRevision',x.context->'draft'->'revision','updatedAt',now());
  if result->'coverage'->>'status'='observable' then
   p:=result->'proposal';
   if p is null or p->>'method'<>'web_search' or p->'draft'<>x.context->'draft' or p->>'partial'<>'true' or p->>'intervalHours'<>(case when p->>'sourcePolicy'='stable_v1' then '1' else '24' end) or p->'criteria'<>(select jsonb_agg(z->'text') from jsonb_array_elements(x.context->'intent'->'criteria') z) or (p->>'expiresAt')::timestamptz>now()+interval '35 minutes' then perform private.fail('invalid_input');end if;
   p:=jsonb_set(p,'{limits}',private.limits());
   insert into private.proposals(id,token_hash,version,payload,expires_at) values((p->>'id')::uuid,e.token_hash,1,p,(p->>'expiresAt')::timestamptz);
   insert into private.general_plans values((p->>'id')::uuid,x.context->'intent');r:=r||jsonb_build_object('status','ready','proposal',p);
  else r:=r||jsonb_build_object('status','unsupported','reason','unqualified_method','explanation',coalesce(result->'coverage'->>'reason','Sources non qualifiées.'));end if;
  update private.evaluations set payload=r where id=e.id;
 else
  observed:=(result->>'observedAt')::timestamptz;outcome:=result->'decision'->>'outcome';
  if observed<x.created_at or observed>now()+interval '5 minutes' or result->'decision'->'memory'->>'generation'<>x.generation::text or jsonb_array_length(result->'decision'->'memory'->'facts')>80 then perform private.fail('invalid_input');end if;
  if x.kind='start' then
   if outcome<>'baseline' then perform private.general_fail(target);return;end if;
   update private.watches set status='active',remote_state='running',provider_id=null,baseline_at=observed,health='healthy',last_success_at=observed,next_check_at=observed+((w.plan->>'intervalHours')||' hours')::interval where id=w.id;
   insert into private.checks values(w.id,w.generation,'general:'||target::text,'baseline',observed);
   if w.plan->>'sourcePolicy'='stable_v1' then
    for f in select z from jsonb_array_elements(result->'findings') z loop
     if f->'finding'->>'noveltyBasis'<>'initial' or f->'finding'->'criteriaSnapshot'<>w.plan->'criteria' or f->'finding'->>'planId'<>w.plan->>'id' or f->'finding'->>'watchId'<>w.id::text or f->>'key' !~ '^[0-9a-f]{64}$' or jsonb_array_length(f->'finding'->'evidence')<1 then perform private.fail('invalid_input');end if;
     if not exists(select 1 from private.findings where watch_id=w.id and fact_key=f->>'key' and payload->>'planId'=w.plan->>'id') then
      insert into private.findings(id,watch_id,owner_id,generation,fact_key,payload) values((f->'finding'->>'id')::uuid,w.id,w.owner_id,w.generation,f->>'key',f->'finding') on conflict do nothing;
     end if;
    end loop;
    if w.plan->'draft'->>'mode'='once' and jsonb_array_length(result->'findings')>0 then
     update private.watches set status='completed',remote_state='stopped',next_check_at=null where id=w.id;
     update private.capacity set reserved=greatest(0,reserved-1) where id;
    end if;
   end if;
   update private.commands set receipt=jsonb_build_object('id',id,'state','succeeded','watchId',w.id,'watchVersion',w.version,'deleted',false,'remoteState',(select remote_state from private.watches where id=w.id),'updatedAt',now()) where owner_id=w.owner_id and id=j.command_id;
  elsif outcome='finding' then
   if jsonb_array_length(result->'findings')<>1 then perform private.fail('invalid_input');end if;
   f:=result->'findings'->0;
   if f->>'key' !~ '^[0-9a-f]{64}$' or f->'finding'->'criteriaSnapshot'<>w.plan->'criteria' or f->'finding'->>'planId'<>w.plan->>'id' then perform private.fail('invalid_input');end if;
   perform public.s3_record_check(w.id,w.generation,'general:'||target::text,'finding',observed,f->>'key',f->'finding');
  else perform public.s3_record_check(w.id,w.generation,'general:'||target::text,case when outcome='no_change' then 'no_change' else 'failed' end,observed);end if;
  insert into private.general_memory(watch_id,generation,state,documents,cycle) values(w.id,w.generation,result->'decision'->'memory',result->'documents',coalesce((x.context->>'cycle')::int,0)+1)
  on conflict(watch_id) do update set generation=excluded.generation,state=excluded.state,documents=case when w.plan->>'sourcePolicy'='stable_v1' then excluded.documents else (select coalesce(jsonb_agg(z),'[]') from (select z from jsonb_array_elements(excluded.documents||coalesce((select jsonb_agg(v) from jsonb_array_elements(private.general_memory.documents) v where not exists(select 1 from jsonb_array_elements(excluded.documents) n where n->>'url'=v->>'url')),'[]')) z limit 6) q) end,cycle=excluded.cycle,updated_at=now();
  update private.watches set next_check_at=case when status='active' then observed+((w.plan->>'intervalHours')||' hours')::interval else null end where id=w.id;
  update private.jobs set state='done',lease_until=null where id=j.id;
 end if;
 update private.general_work set state='done',lease_until=null where id=target;
end $$;
