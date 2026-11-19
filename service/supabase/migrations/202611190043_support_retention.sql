-- Keep receipts until an explicit retention policy is approved.
-- This disables request-time age deletion and schedules no cleanup.
create or replace function public.support_submit(target uuid, sender text, payload jsonb) returns jsonb
language plpgsql security definer set search_path='' as $$
declare f text;existing private.support_messages;begin
 perform pg_advisory_xact_lock(734212);
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
