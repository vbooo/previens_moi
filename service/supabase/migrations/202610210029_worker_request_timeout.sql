-- Free-provider Retry-After can exceed the former 45-second HTTP timeout.
-- Match the bounded worker request window; this does not retry provider POSTs.
create or replace function public.s3_tick() returns bigint language plpgsql security definer set search_path='' as $$
declare target text;secret text;stamp text:=extract(epoch from now())::bigint::text;nonce text:=gen_random_uuid()::text;signature text;begin
 select url into target from private.worker_schedule where id and enabled;
 if target is null then return null;end if;
 if target !~ '^https://[a-z0-9]+[.]supabase[.]co/functions/v1/worker$' and target<>'http://kong:8000/functions/v1/worker' then perform private.fail('invalid_input');end if;
 select decrypted_secret into secret from vault.decrypted_secrets where name='previens-moi-worker';
 if secret is null then return null;end if;
 signature:=encode(extensions.hmac('previens-moi-worker-v1.'||stamp||'.'||nonce,secret,'sha256'),'hex');
 -- pg_net queue can expose headers to database roles: enqueue only a one-use,
 -- short-lived MAC. Never a long-lived bearer or service-role key.
 return net.http_post(url:=target,body:='{}'::jsonb,headers:=jsonb_build_object('Content-Type','application/json','X-Worker-Time',stamp,'X-Worker-Nonce',nonce,'X-Worker-Signature',signature),timeout_milliseconds:=135000);
end $$;
