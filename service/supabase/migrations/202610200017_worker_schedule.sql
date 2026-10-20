-- Service scheduling, unrelated to Codex reminders. Disabled until explicit configuration.
create extension if not exists pg_net with schema extensions;
create table private.worker_schedule(id boolean primary key default true check(id),enabled boolean not null default false,url text not null default '');
insert into private.worker_schedule(id) values(true);
alter table private.worker_schedule enable row level security;
create table private.worker_nonces(nonce uuid primary key,created_at timestamptz not null default now());
alter table private.worker_nonces enable row level security;
create function public.s3_worker_nonce(value uuid) returns boolean language plpgsql security definer set search_path='' as $$
begin
 delete from private.worker_nonces where created_at<now()-interval '5 minutes';
 insert into private.worker_nonces(nonce) values(value) on conflict do nothing;return found;
end $$;
create function public.s3_tick() returns bigint language plpgsql security definer set search_path='' as $$
declare target text;secret text;stamp text:=extract(epoch from now())::bigint::text;nonce text:=gen_random_uuid()::text;signature text;begin
 select url into target from private.worker_schedule where id and enabled;
 if target is null then return null;end if;
 if target !~ '^https://[a-z0-9]+[.]supabase[.]co/functions/v1/worker$' and target<>'http://kong:8000/functions/v1/worker' then perform private.fail('invalid_input');end if;
 select decrypted_secret into secret from vault.decrypted_secrets where name='previens-moi-worker';
 if secret is null then return null;end if;
 signature:=encode(extensions.hmac('previens-moi-worker-v1.'||stamp||'.'||nonce,secret,'sha256'),'hex');
 -- pg_net queue can expose headers to database roles: enqueue only a one-use,
 -- short-lived MAC. Never a long-lived bearer or service-role key.
 return net.http_post(url:=target,body:='{}'::jsonb,headers:=jsonb_build_object('Content-Type','application/json','X-Worker-Time',stamp,'X-Worker-Nonce',nonce,'X-Worker-Signature',signature),timeout_milliseconds:=45000);
end $$;
revoke all on function public.s3_tick(),public.s3_worker_nonce(uuid) from public,anon,authenticated;
grant execute on function public.s3_tick(),public.s3_worker_nonce(uuid) to service_role;
select cron.schedule('previens-moi-worker','* * * * *','select public.s3_tick()');
