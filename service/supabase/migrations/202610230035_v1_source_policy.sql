-- V1 source policy applies only to newly confirmed plans. No user plan, budget
-- limit, used/held counter or provider configuration is reset by this migration.
create function public.s5_proposal_options(target uuid,token text,expected bigint,mode text) returns jsonb language plpgsql security definer set search_path='' as $$
declare p private.proposals;next_payload jsonb;begin
 if mode not in ('once','continuous') then perform private.fail('invalid_input');end if;
 select * into p from private.proposals where id=target and token_hash=encode(extensions.digest(token,'sha256'),'hex') for update;
 if not found then perform private.fail('not_found');end if;
 if p.claimed_by is not null then perform private.fail('command_conflict');end if;
 if p.expires_at<=now() then perform private.fail('proposal_expired');end if;
 -- Same desired option is an idempotent retry, even after a lost response.
 if p.payload->'draft'->>'mode'=mode then return p.payload;end if;
 if p.version<>expected then perform private.fail('version_conflict');end if;
 next_payload:=jsonb_set(jsonb_set(p.payload,'{draft,mode}',to_jsonb(mode)),'{version}',to_jsonb(p.version+1));
 update private.proposals set version=version+1,payload=next_payload where id=target;
 update private.evaluations set payload=jsonb_set(payload,'{proposal}',next_payload) where payload->'proposal'->>'id'=target::text;
 return next_payload;
end $$;
revoke all on function public.s5_proposal_options(uuid,text,bigint,text) from public,anon,authenticated;
grant execute on function public.s5_proposal_options(uuid,text,bigint,text) to service_role;

create or replace function public.s3_general_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare x private.general_work;j private.jobs;w private.watches;m private.general_memory;intent jsonb;expired record;usd bigint;credits int;begin
 perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
 for expired in select id from private.general_work where state='leased' and lease_until<now() order by id loop perform private.general_fail(expired.id);end loop;
 if not exists(select 1 from private.general_budget where id and enabled and qualified) or not exists(select 1 from private.capacity where id and enabled) or not exists(select 1 from private.method_qualifications where method='web_search' and qualified) then return null;end if;
 perform public.s3_enqueue_due();
 select q.* into j from private.jobs q join private.watches a on a.id=q.watch_id join private.accounts b on b.id=a.owner_id where q.state='ready' and q.kind in ('start','check') and q.available_at<=now() and q.generation=a.generation and a.status in ('pending','active') and b.deleted_at is null and a.plan->>'method'='web_search' and exists(select 1 from private.general_plans where plan_id=(a.plan->>'id')::uuid) and not exists(select 1 from private.general_work where job_id=q.id) order by q.available_at for update of q skip locked limit 1;
 if found then
  select * into w from private.watches where id=j.watch_id;select p.intent into intent from private.general_plans p where p.plan_id=(w.plan->>'id')::uuid;
  select * into m from private.general_memory where watch_id=w.id and generation=w.generation;
  update private.jobs set state='leased',attempts=attempts+1,lease_token=gen_random_uuid(),lease_until=now()+interval '10 minutes' where id=j.id;
  insert into private.general_work(job_id,kind,phase,generation,context) values(j.id,j.kind,'collect',j.generation,jsonb_build_object('draft',w.plan->'draft','intent',intent,'plan',w.plan,'memory',coalesce(m.state,jsonb_build_object('generation',j.generation,'baselineAt',null,'observedAt',null,'completed',false,'facts','[]'::jsonb)),'priorDocuments',coalesce(m.documents,'[]'::jsonb),'cycle',coalesce(m.cycle,0),'rediscover',w.plan->>'sourcePolicy'='stable_v1' and coalesce(m.cycle,0)>0 and mod(m.cycle,24)=0));
 end if;
 select a.* into x from private.general_work a where a.state='ready' and a.created_at>now()-interval '24 hours' order by a.created_at,a.id for update skip locked limit 1;
 if not found then return null;end if;
 usd:=case when x.phase='collect' then 0 else 20000 end;credits:=case when x.phase<>'collect' then 0 when x.context->'plan'->>'sourcePolicy'='stable_v1' then case when x.context->>'rediscover'='true' then 8 when jsonb_array_length(x.context->'plan'->'sources')=0 then 5 else jsonb_array_length(x.context->'plan'->'sources') end else 3 end;
 if not exists(select 1 from private.general_budget where id and model_used+model_held+usd<=model_limit and credit_used+credit_held+credits<=credit_limit) then
  perform private.general_fail(x.id);
  if x.job_id is not null then
   select * into j from private.jobs where id=x.job_id;select * into w from private.watches where id=j.watch_id and generation=x.generation for update;
   if found and w.status='active' then update private.watches set status='suspended_by_capacity',remote_state='stopped',next_check_at=null where id=w.id;update private.capacity set reserved=greatest(0,reserved-1) where id;end if;
   update private.commands set receipt=jsonb_set(receipt,'{error,code}','"quota_exceeded"') where owner_id=w.owner_id and id=j.command_id and receipt->>'state'='failed';
  end if;
  return null;
 end if;
 update private.general_budget set model_held=model_held+usd,credit_held=credit_held+credits where id;
 update private.general_work set state='leased',lease=gen_random_uuid(),lease_until=now()+interval '120 seconds' where id=x.id returning * into x;
 insert into private.general_attempts(work_key,phase,lease,model_reserved,credits_reserved) values(x.id,x.phase,x.lease,usd,credits);
 return to_jsonb(x)||jsonb_build_object('watchId',(select watch_id from private.jobs where id=x.job_id));
end $$;
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
   p:=jsonb_set(p,'{limits,globalAvailable}',to_jsonb((select greatest(0,slots-reserved) from private.capacity where id)));
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
create or replace function public.s3_general_finish(target uuid,lease_id uuid,result jsonb,actual bigint,credits int) returns void language plpgsql security definer set search_path='' as $$
declare x private.general_work;a private.general_attempts;j private.jobs;w private.watches;p jsonb;begin
 perform 1 from private.capacity where id for update;perform 1 from private.general_budget where id for update;
 select * into x from private.general_work where id=target and lease=lease_id and state='leased' and lease_until>now() for update;
 if found and x.kind='admission' and x.phase='interpret' and x.context->>'understandingOnly'='true' and result->'intent'->>'status'='understood' and result->'proposal'->>'backgroundPreparation'='true' then
  update private.general_work set phase='decide',context=context||jsonb_build_object('intent',result->'intent') where id=target;
  perform public.s3_general_finish_before_workflow(target,lease_id,jsonb_build_object('coverage',jsonb_build_object('status','observable'),'proposal',result->'proposal'),actual,credits);return;
 end if;
 if not found or (x.kind<>'start' and not(x.kind='check' and x.context->>'rediscover'='true')) or x.phase<>'decide' or x.context->'plan'->>'backgroundPreparation' is distinct from 'true' or coalesce((result->>'failed')::boolean,false) then
  perform public.s3_general_finish_before_workflow(target,lease_id,result,actual,credits);return;
 end if;
 -- Settle this coverage assessment exactly once before scheduling its baseline.
 select * into a from private.general_attempts where work_key=target and lease=lease_id and state='reserved' for update;
 if not found then return;end if;
 if actual is not null and (actual<0 or actual>a.model_reserved) or credits is not null and (credits<0 or credits>a.credits_reserved) then update private.general_budget set enabled=false where id;perform private.general_fail(target);return;end if;
 if actual is not null then update private.general_budget set model_held=model_held-a.model_reserved,model_used=model_used+actual where id;end if;
 if credits is not null then update private.general_budget set credit_held=credit_held-a.credits_reserved,credit_used=credit_used+credits where id;end if;
 update private.general_attempts set actual=s3_general_finish.actual,credits=s3_general_finish.credits,state=case when s3_general_finish.actual is null or s3_general_finish.credits is null then 'unknown' else 'settled' end where work_key=target and phase=a.phase;
 select * into j from private.jobs where id=x.job_id for update;
 select * into w from private.watches where id=j.watch_id and generation=x.generation and status in ('pending','active') and exists(select 1 from private.accounts where id=owner_id and deleted_at is null) for update;
 if not found then update private.general_work set state='done',lease_until=null where id=target;return;end if;
 if x.kind='check' and result->'coverage'->>'status' is distinct from 'observable' and actual is not null and credits is not null then
  update private.general_work set phase='baseline',state='ready',lease_until=null,context=context||jsonb_build_object('prepared',true,'retrievalHealthy',false,'documents',(select coalesce(jsonb_agg(d),'[]') from jsonb_array_elements(context->'documents') d where exists(select 1 from jsonb_array_elements(context->'plan'->'sources') s where s->>'url'=d->>'url'))) where id=target;return;
 end if;
 if result->'coverage'->>'status' is distinct from 'observable' or actual is null or credits is null then
  if actual is not null and credits is not null and result->'coverage'->>'status' in ('unobservable','uncertain') then update private.watches set plan=jsonb_set(plan,'{preparationFailure}','"source_unusable"') where id=w.id;end if;
  perform private.general_fail(target);return;
 end if;
 p:=jsonb_set(result->'preparedPlan','{preparationFailure}','null');
 if w.plan ? 'checkCoverage' then p:=jsonb_set(p,'{checkCoverage}',w.plan->'checkCoverage');end if;
 if p is null or p->'draft'<>w.plan->'draft' or p->'criteria'<>w.plan->'criteria' or p->>'id'<>w.plan->>'id' or p->>'version'<>w.plan->>'version' or p->>'method'<>'web_search' or p->>'partial'<>'true' or p->>'intervalHours'<>w.plan->>'intervalHours' or p->>'sourcePolicy' is distinct from w.plan->>'sourcePolicy' or jsonb_array_length(p->'sources') not between 1 and 3 then perform private.general_fail(target);return;end if;
 update private.watches set plan=p where id=w.id;
 update private.plan_versions set plan=p where watch_id=w.id and plan_id=(p->>'id')::uuid;
 update private.general_work set phase='baseline',state='ready',lease_until=null,context=context||jsonb_build_object('plan',p,'prepared',true,'documents',coalesce(result->'documents',x.context->'documents'),'retrievalHealthy',true) where id=target;
end $$;
create or replace function private.general_fail_before_workflow(target uuid) returns void language plpgsql security definer set search_path='' as $$
declare x private.general_work;j private.jobs;w private.watches;e private.evaluations;begin
 select * into x from private.general_work where id=target;
 if x.kind='admission' then
  select * into e from private.evaluations where id=x.evaluation_id;
  update private.evaluations set payload=jsonb_build_object('id',e.id,'status','failed','draftRevision',x.context->'draft'->'revision','updatedAt',now(),'error',jsonb_build_object('code','temporary_failure','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null)) where id=e.id;
 else
  select * into j from private.jobs where id=x.job_id;select * into w from private.watches where id=j.watch_id and generation=j.generation for update;
  if found and w.status in ('pending','active') then
   if j.kind='start' then
    update private.watches set status='paused',remote_state='stopped',health='degraded',last_failure_at=now(),next_check_at=null where id=w.id;
    update private.capacity set reserved=greatest(0,reserved-1) where id;
    update private.commands set receipt=jsonb_build_object('id',id,'state','failed','watchId',w.id,'updatedAt',now(),'remoteState','stopped','error',jsonb_build_object('code','temporary_failure','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null)) where owner_id=w.owner_id and id=j.command_id;
   else update private.watches set health='degraded',last_failure_at=now(),next_check_at=now()+((w.plan->>'intervalHours')||' hours')::interval where id=w.id;end if;
  end if;
  update private.jobs set state='done',lease_until=null where id=j.id;
 end if;
 update private.general_work set state='unknown',lease_until=null where id=target;
end $$;
create or replace function public.s4_preparation_delivery_allowed(delivery uuid,lease_id uuid) returns boolean language sql security definer set search_path='' as $$
 select exists(select 1 from private.preparation_notices n join private.watches w on w.id=n.watch_id join private.devices d on d.id=n.device_id join private.accounts a on a.id=w.owner_id where n.id=delivery and n.lease=lease_id and n.state='leased' and n.lease_until>now() and n.created_at>now()-interval '24 hours' and n.generation=w.generation and n.watch_version=w.version and d.owner_id=w.owner_id and a.deleted_at is null and (n.event='ready' and w.status in ('active','completed') or n.event='failed' and w.status='paused' and w.health='degraded'));
$$;

-- Exhaustion is a capacity state, not an unsupported user topic.
alter function public.s3_preview(text,text,jsonb,uuid) rename to s3_preview_before_v1;
revoke all on function public.s3_preview_before_v1(text,text,jsonb,uuid) from public,anon,authenticated,service_role;
create function public.s3_preview(action text,token text default null,body jsonb default null,resource uuid default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;begin
 r:=public.s3_preview_before_v1(action,token,body,resource);
 if action='understand' and r->>'status'='unsupported' and exists(select 1 from private.general_budget where id and enabled and qualified and model_used+model_held+20000>model_limit) then
  r:=jsonb_build_object('id',r->'id','status','failed','draftRevision',r->'draftRevision','updatedAt',now(),'error',jsonb_build_object('code','quota_exceeded','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null));
  update private.evaluations set payload=r where id=(body->>'id')::uuid;
 end if;return r;
end $$;
revoke all on function public.s3_preview(text,text,jsonb,uuid) from public,anon,authenticated;
grant execute on function public.s3_preview(text,text,jsonb,uuid) to service_role;
