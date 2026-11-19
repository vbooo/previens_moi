-- A small private inbox, also usable before sign-in. No arbitrary mail recipient.
create table private.support_messages (
 id uuid primary key, fingerprint text not null, sender_hash text not null,
 category text not null check(category in ('question','problem','idea')),
 message text, reply_email text, app_version text not null, language text not null,
 state text not null default 'queued' check(state in ('queued','sending','sent','failed')),
 created_at timestamptz not null default now(), attempted_at timestamptz
);
alter table private.support_messages enable row level security;
revoke all on private.support_messages from public,anon,authenticated;
create index support_created on private.support_messages(created_at);
create function public.support_submit(target uuid, sender text, payload jsonb) returns jsonb
language plpgsql security definer set search_path='' as $$
declare f text;existing private.support_messages;begin
 perform pg_advisory_xact_lock(734212);
 delete from private.support_messages where created_at<now()-interval '30 days';
 f:=encode(extensions.digest(payload::text,'sha256'),'hex');
 select * into existing from private.support_messages where id=target;
 if found then
  if existing.fingerprint<>f then raise exception 'invalid_input';end if;
  return jsonb_build_object('id',target,'state','received');
 end if;
 if length(sender)<>64 or payload->>'category' not in ('question','problem','idea')
  or length(trim(payload->>'message')) not between 1 and 2000
  or length(coalesce(payload->>'replyEmail',''))>254
  or length(payload->>'appVersion') not between 1 and 40
  or payload->>'language' not in ('fr','en') then raise exception 'invalid_input';end if;
 if (select count(*) from private.support_messages where created_at>now()-interval '24 hours')>=20
  or (select count(*) from private.support_messages where sender_hash=sender and created_at>now()-interval '24 hours')>=3
 then raise exception 'quota_exceeded';end if;
 insert into private.support_messages(id,fingerprint,sender_hash,category,message,reply_email,app_version,language)
 values(target,f,sender,payload->>'category',trim(payload->>'message'),nullif(payload->>'replyEmail',''),payload->>'appVersion',payload->>'language');
 return jsonb_build_object('id',target,'state','received');
end $$;
create function public.support_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare item private.support_messages;begin
 -- Ambiguous SMTP attempts are kept for manual review, never blindly sent twice.
 update private.support_messages set state='failed' where state='sending' and attempted_at<now()-interval '5 minutes';
 select * into item from private.support_messages where state='queued' order by created_at for update skip locked limit 1;
 if not found then return null;end if;
 update private.support_messages set state='sending',attempted_at=now() where id=item.id;
 return jsonb_build_object('id',item.id,'category',item.category,'message',item.message,'replyEmail',item.reply_email,'appVersion',item.app_version,'language',item.language);
end $$;
create function public.support_finish(target uuid, delivered boolean) returns void language plpgsql security definer set search_path='' as $$
begin
 update private.support_messages set state=case when delivered then 'sent' else 'failed' end,
 message=case when delivered then null else message end,reply_email=case when delivered then null else reply_email end
 where id=target and state='sending';
end $$;
revoke all on function public.support_submit(uuid,text,jsonb),public.support_lease(),public.support_finish(uuid,boolean) from public,anon,authenticated;
grant execute on function public.support_submit(uuid,text,jsonb),public.support_lease(),public.support_finish(uuid,boolean) to service_role;
