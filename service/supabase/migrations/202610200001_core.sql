-- All business data is private: only the versioned Edge API is exposed.
create schema if not exists private;
revoke all on schema private from public, anon;
create table private.accounts (
 id uuid primary key references auth.users(id), deleted_at timestamptz,
 created_at timestamptz not null default now()
);
create table private.previews (
 token_hash text primary key, expires_at timestamptz not null default now()+interval '24 hours',
 created_at timestamptz not null default now()
);
create table private.evaluations (
 id uuid primary key, token_hash text not null references private.previews,
 fingerprint text not null, payload jsonb not null, created_at timestamptz not null default now()
);
create table private.proposals (
 id uuid primary key, token_hash text not null references private.previews,
 version bigint not null check(version>0), payload jsonb not null,
 expires_at timestamptz not null, claimed_by uuid references private.accounts
);
create table private.watches (
 id uuid primary key default gen_random_uuid(), owner_id uuid not null references private.accounts,
 version bigint not null default 1 check(version>0), generation bigint not null default 1,
 plan jsonb not null, status text not null check(status in ('pending','active','paused','completed','suspended_by_capacity','deleted')),
 remote_state text not null check(remote_state in ('starting','running','stopping','stopped','unknown')),
 health text not null default 'not_checked_yet', baseline_at timestamptz,
 last_success_at timestamptz, last_failure_at timestamptz, next_check_at timestamptz,
 provider_id text unique, created_at timestamptz not null default now()
);
create table private.commands (
 owner_id uuid not null references private.accounts, id uuid not null,
 fingerprint text not null, receipt jsonb not null, created_at timestamptz not null default now(),
 primary key(owner_id,id)
);
create table private.jobs (
 id uuid primary key default gen_random_uuid(), watch_id uuid not null references private.watches,
 command_id uuid, generation bigint not null, kind text not null check(kind in ('start','stop','check','erase')),
 state text not null default 'ready' check(state in ('ready','leased','done','ambiguous','failed')),
 attempts int not null default 0 check(attempts between 0 and 3),
 available_at timestamptz not null default now(), lease_until timestamptz, lease_token uuid,
 unique(watch_id,generation,kind)
);
create table private.capacity (
 id boolean primary key default true check(id), enabled boolean not null default false,
 slots int not null default 0 check(slots>=0), reserved int not null default 0 check(reserved>=0),
 budget_units bigint not null default 0 check(budget_units>=0),
 used_units bigint not null default 0 check(used_units>=0),
 reserved_units bigint not null default 0 check(reserved_units>=0),
 check(reserved<=slots), check(used_units+reserved_units<=budget_units)
);
insert into private.capacity(id) values(true);
create table private.reservations (
 id uuid primary key, watch_id uuid not null references private.watches,
 units bigint not null check(units>0), state text not null default 'reserved' check(state in ('reserved','consumed','released')),
 actual_units bigint check(actual_units>=0), created_at timestamptz not null default now()
);
create table private.inbox (
 event_id text primary key, provider_id text not null, check_id text not null,
 payload jsonb not null, received_at timestamptz not null default now(),
 unique(provider_id,check_id,event_id)
);
create table private.checks (
 watch_id uuid not null references private.watches, generation bigint not null, check_id text not null,
 outcome text not null check(outcome in ('baseline','no_change','finding','failed','quota','rejected')),
 observed_at timestamptz not null, primary key(watch_id,generation,check_id)
);
create table private.findings (
 id uuid primary key default gen_random_uuid(), watch_id uuid not null references private.watches,
 owner_id uuid not null references private.accounts, generation bigint not null,
 fact_key text not null, payload jsonb not null, created_at timestamptz not null default now(),
 unique(watch_id,generation,fact_key)
);
create table private.devices (
 id uuid primary key, owner_id uuid not null references private.accounts, token text not null unique,
 language text not null check(language in ('fr','en')), updated_at timestamptz not null default now()
);
create table private.outbox (
 id uuid primary key default gen_random_uuid(), finding_id uuid not null references private.findings on delete cascade,
 device_id uuid not null references private.devices on delete cascade, generation bigint not null,
 state text not null default 'ready' check(state in ('ready','leased','accepted','cancelled','failed','unknown')),
 attempts int not null default 0, lease_token uuid, lease_until timestamptz,
 created_at timestamptz not null default now(), unique(finding_id,device_id)
);
create table private.erasure_ledger (owner_id uuid primary key, erased_at timestamptz not null default now());
-- RLS remains enforced for caller-scoped SQL as well as explicit API ownership.
alter table private.accounts enable row level security;
create policy owner on private.accounts for select to authenticated using(id=auth.uid() and deleted_at is null);
alter table private.watches enable row level security;
create policy owner on private.watches for select to authenticated using(owner_id=auth.uid() and status<>'deleted' and exists(select 1 from private.accounts where id=auth.uid()));
alter table private.findings enable row level security;
create policy owner on private.findings for select to authenticated using(owner_id=auth.uid() and exists(select 1 from private.watches where id=watch_id));
grant usage on schema private to authenticated;
grant select on private.accounts, private.watches, private.findings to authenticated;
-- Every other table is inaccessible to API roles, even if a future API schema is exposed.
do $$ declare t text; begin
 foreach t in array array['previews','evaluations','proposals','commands','jobs','capacity','reservations','inbox','checks','devices','outbox','erasure_ledger'] loop
 execute format('alter table private.%I enable row level security',t);
 end loop;
end $$;

create function private.fail(code text) returns void language plpgsql set search_path='' as $$ begin raise exception using message=code, errcode='P0001'; end $$;
create function private.owner() returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid := auth.uid(); begin
 if u is null or not exists(select 1 from auth.users where id=u) then perform private.fail('session_required'); end if;
 insert into private.accounts(id) values(u) on conflict do nothing;
 perform 1 from private.accounts where id=u and deleted_at is null for update;
 if not found then perform private.fail('session_required'); end if;
 return u;
end $$;
create function private.watch_json(w private.watches) returns jsonb language sql stable set search_path='' as $$
 select jsonb_build_object('id',w.id,'version',w.version,'plan',w.plan,'status',w.status,'health',w.health,
 'lastSuccessAt',w.last_success_at,'lastFailureAt',w.last_failure_at,'baselineAt',w.baseline_at,
 'nextCheckAt',w.next_check_at,'pendingCommandId',(select j.command_id from private.jobs j where j.watch_id=w.id and j.state in ('ready','leased','ambiguous') order by available_at desc limit 1),'remoteState',w.remote_state)
$$;
create function private.limits() returns jsonb language sql stable set search_path='' as $$
 select jsonb_build_object('activePerAccount',1,'globalAvailable',case when enabled then slots-reserved else 0 end,'previewRemaining',0,'resetsAt',null) from private.capacity where id
$$;
-- UUIDv7 age is checked before reuse; tombstones/claimed proposals remain after receipt pruning.
create function private.command_age(i uuid) returns void language plpgsql set search_path='' as $$
declare h text:=replace(i::text,'-',''); ms bigint; begin
 if substr(h,13,1)<>'7' or substr(h,17,1) not in ('8','9','a','b') then perform private.fail('invalid_input'); end if;
 ms:=('x'||substr(h,1,12))::bit(48)::bigint;
 if to_timestamp(ms/1000.0)>now()+interval '5 minutes' then perform private.fail('invalid_input'); end if;
 if to_timestamp(ms/1000.0)<now()-interval '14 days' then perform private.fail('command_expired'); end if;
end $$;

create function public.s3_command(body jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid; i uuid:=(body->>'id')::uuid; op text:=body->>'operation'; fp text; old private.commands; w private.watches; p private.proposals; cap private.capacity; r jsonb; wid uuid; begin
 u:=private.owner(); perform private.command_age(i);
 -- Never retain the transport secret in receipts/fingerprints; keep its hash to detect a changed credential.
 fp:=encode(extensions.digest((body-'proposalToken')::text||coalesce(encode(extensions.digest(body->>'proposalToken','sha256'),'hex'),''),'sha256'),'hex');
 select * into old from private.commands where owner_id=u and id=i;
 if found then
  if old.fingerprint<>fp then perform private.fail('command_conflict'); end if;
  return old.receipt;
 end if;
 if op in ('activate','replace') then
  select * into p from private.proposals where id=(body->>'proposalId')::uuid for update;
  if not found or p.token_hash<>encode(extensions.digest(coalesce(body->>'proposalToken',''),'sha256'),'hex') then perform private.fail('not_found'); end if;
  if p.claimed_by is not null then perform private.fail('command_conflict'); end if;
  if p.expires_at<=now() then perform private.fail('proposal_expired'); end if;
  if p.version<>(body->>'proposalVersion')::bigint then perform private.fail('version_conflict'); end if;
  if (p.payload->>'partial')::boolean and coalesce((body->>'acceptPartial')::boolean,false)=false then perform private.fail('invalid_input'); end if;
 end if;
 -- Acquire the same global lock before any watch lock (worker follows this order).
 select * into cap from private.capacity where id for update;
 if op='activate' then
  if not cap.enabled or cap.reserved>=cap.slots or exists(select 1 from private.watches where owner_id=u and status in ('active','pending')) then perform private.fail('quota_exceeded'); end if;
  insert into private.watches(owner_id,plan,status,remote_state) values(u,p.payload,'pending','starting') returning * into w;
  update private.capacity set reserved=reserved+1 where id;
 else
  select * into w from private.watches where id=(body->>'watchId')::uuid and owner_id=u and status<>'deleted' for update;
  if not found then perform private.fail('not_found'); end if;
  if w.version<>(body->>'expectedVersion')::bigint then perform private.fail('version_conflict'); end if;
  if op not in ('pause','delete') then perform private.fail('unsupported'); end if;
  -- Occupied slot is kept until remote stop is actually confirmed.
  update private.watches set status=case when op='delete' then 'deleted' else 'paused' end,
   version=version+1,generation=generation+1,remote_state=case when remote_state='stopped' then 'stopped' else 'stopping' end,next_check_at=null
   where id=w.id returning * into w;
  update private.jobs set state='done' where watch_id=w.id and kind<>'stop' and state='ready';
  update private.outbox o set state='cancelled' from private.findings f where o.finding_id=f.id and f.watch_id=w.id and o.state='ready';
  if op='delete' then delete from private.findings where watch_id=w.id; end if;
 end if;
 if op='activate' then
  update private.proposals set claimed_by=u where id=p.id;
  insert into private.jobs(watch_id,command_id,generation,kind) values(w.id,i,w.generation,'start');
 elsif w.remote_state<>'stopped' then
  insert into private.jobs(watch_id,command_id,generation,kind) values(w.id,i,w.generation,'stop') on conflict do nothing;
 end if;
 r:=jsonb_build_object('id',i,'state',case when op='activate' or w.remote_state<>'stopped' then 'pending' else 'succeeded' end,'watchId',w.id,'updatedAt',now(),'remoteState',w.remote_state);
 if r->>'state'='succeeded' then r:=r||jsonb_build_object('watchVersion',case when op='delete' then null else w.version end,'deleted',op='delete'); end if;
 insert into private.commands(owner_id,id,fingerprint,receipt) values(u,i,fp,r);
 return r;
end $$;

create function public.s3_read(route text, resource uuid default null, page_limit int default 20, page_cursor text default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid; w private.watches; result jsonb; begin
 u:=private.owner();
 if page_limit not between 1 and 100 then perform private.fail('invalid_input'); end if;
 -- Cursor is an opaque UUID boundary, ordered by UUID; never OFFSET with unstable pages.
 if route='account' then return jsonb_build_object('id',u,'language','fr','limits',private.limits());
 elsif route='command' then
  perform private.command_age(resource);
  select receipt into result from private.commands where owner_id=u and id=resource;
 elsif route='watch' then
  select private.watch_json(t) into result from private.watches t where t.id=resource and t.owner_id=u and status<>'deleted';
 elsif route='watches' then
  select jsonb_build_object('items',coalesce(jsonb_agg(private.watch_json(q) order by q.id),'[]'::jsonb),'nextCursor',case when count(*)=page_limit then max(q.id::text) else null end,'serverTime',now()) into result
  from (select * from private.watches where owner_id=u and status<>'deleted' and (page_cursor is null or id>page_cursor::uuid) order by id limit page_limit) q;
 elsif route='finding' then
  select f.payload into result from private.findings f join private.watches t on t.id=f.watch_id where f.id=resource and f.owner_id=u and t.status<>'deleted' and f.created_at>now()-interval '90 days';
 elsif route='findings' then
  if not exists(select 1 from private.watches where id=resource and owner_id=u and status<>'deleted') then perform private.fail('not_found'); end if;
  select jsonb_build_object('items',coalesce(jsonb_agg(q.payload order by q.id),'[]'::jsonb),'nextCursor',case when count(*)=page_limit then max(q.id::text) else null end,'serverTime',now()) into result
  from (select f.id,f.payload from private.findings f where f.watch_id=resource and f.owner_id=u and f.created_at>now()-interval '90 days' and (page_cursor is null or f.id>page_cursor::uuid) order by f.id limit page_limit) q;
 else perform private.fail('not_found'); end if;
 if result is null then perform private.fail('not_found'); end if;
 return result;
end $$;

create function public.s3_device(device uuid, body jsonb default null) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.owner(); begin
 if body is null then delete from private.devices where id=device and owner_id=u; return; end if;
 if length(body->>'fcmToken') not between 16 and 4096 or body->>'language' not in ('fr','en') then perform private.fail('invalid_input'); end if;
 -- A fresh authenticated registration transfers a rotated installation/token; never expose the previous owner.
 delete from private.devices where token=body->>'fcmToken' and (owner_id<>u or id<>device);
 insert into private.devices(id,owner_id,token,language) values(device,u,body->>'fcmToken',body->>'language') on conflict(id) do update set owner_id=u,token=excluded.token,language=excluded.language,updated_at=now();
end $$;

-- Anonymous admission has a hard shared bound. No paid provider call is reachable here.
create function public.s3_preview(action text, token text default null, body jsonb default null, resource uuid default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare h text:=encode(extensions.digest(coalesce(token,''),'sha256'),'hex'); r jsonb; secret text; cnt int; begin
 perform pg_advisory_xact_lock(30202026);
 if action='session' then
  select count(*) into cnt from private.previews where created_at>now()-interval '1 hour';
  if cnt>=50 then perform private.fail('rate_limited'); end if;
  secret:=encode(extensions.gen_random_bytes(32),'hex');
  insert into private.previews(token_hash) values(encode(extensions.digest(secret,'sha256'),'hex'));
  return jsonb_build_object('token',secret,'expiresAt',now()+interval '24 hours');
 end if;
 if not exists(select 1 from private.previews where token_hash=h and expires_at>now()) then perform private.fail('preview_required'); end if;
 if action='evaluate' then
  select payload into r from private.evaluations where id=(body->>'id')::uuid and token_hash=h and fingerprint=encode(extensions.digest(body::text,'sha256'),'hex');
  if found then return r; end if;
  if exists(select 1 from private.evaluations where id=(body->>'id')::uuid) then perform private.fail('command_conflict'); end if;
  if (select count(*) from private.evaluations where token_hash=h)>=5 then perform private.fail('rate_limited'); end if;
  if length(body->'draft'->>'text') not between 1 and 2000 then perform private.fail('invalid_input'); end if;
  r:=jsonb_build_object('id',body->>'id','draftRevision',(body->'draft'->>'revision')::bigint,'updatedAt',now(),'status','unsupported','reason','unqualified_method','explanation','La compréhension et la surveillance réelles ne sont pas encore qualifiées. Aucune surveillance activée.');
  insert into private.evaluations(id,token_hash,fingerprint,payload) values((body->>'id')::uuid,h,encode(extensions.digest(body::text,'sha256'),'hex'),r);
 elsif action='evaluation' then select payload into r from private.evaluations where id=resource and token_hash=h;
 elsif action='proposal' then
  select payload into r from private.proposals where id=resource and token_hash=h and claimed_by is null;
  if r is not null and (r->>'expiresAt')::timestamptz<=now() then perform private.fail('proposal_expired'); end if;
 else perform private.fail('not_found'); end if;
 if r is null then perform private.fail('not_found'); end if;
 return r;
end $$;

create function public.s3_delete_account(request_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.owner(); amr jsonb; recent boolean:=false; w private.watches; begin
 -- JWT amr is signed by Auth. A refresh iat must not count as recent OTP authentication.
 for amr in select * from jsonb_array_elements(coalesce(auth.jwt()->'amr','[]')) loop
  if amr->>'method'='otp' and to_timestamp((amr->>'timestamp')::double precision)>now()-interval '5 minutes' then recent:=true; end if;
 end loop;
 if not recent then perform private.fail('reauthentication_required'); end if;
 perform 1 from private.capacity where id for update;
 update private.accounts set deleted_at=now() where id=u;
 insert into private.erasure_ledger(owner_id) values(u) on conflict do nothing;
 delete from private.devices where owner_id=u;
 delete from private.findings where owner_id=u;
 for w in update private.watches set status='deleted',version=version+1,generation=generation+1,next_check_at=null,
  remote_state=case when remote_state='stopped' then 'stopped' else 'stopping' end where owner_id=u returning * loop
  insert into private.jobs(watch_id,generation,kind) values(w.id,w.generation,'erase') on conflict do nothing;
 end loop;
 return jsonb_build_object('id',request_id,'state','accepted','accessRevokedAt',now(),'description','Accès fermé ; effacement et arrêt distant à confirmer.');
end $$;

-- Explicit grants only; no PUBLIC execution of SECURITY DEFINER functions.
revoke all on all functions in schema private from public, anon, authenticated;
revoke all on function public.s3_command(jsonb), public.s3_read(text,uuid,int,text), public.s3_device(uuid,jsonb), public.s3_preview(text,text,jsonb,uuid), public.s3_delete_account(uuid) from public, anon;
grant execute on function public.s3_command(jsonb),public.s3_read(text,uuid,int,text),public.s3_device(uuid,jsonb),public.s3_delete_account(uuid) to authenticated;
grant execute on function public.s3_preview(text,text,jsonb,uuid) to service_role;

revoke all on function public.s3_preview(text,text,jsonb,uuid) from authenticated;
