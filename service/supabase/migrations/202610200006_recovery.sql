create or replace function public.s3_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.jobs;begin
 update private.jobs set state='ambiguous',lease_until=null where kind='start' and state='leased' and lease_until<now();
 update private.jobs set state='failed',lease_until=null where state='leased' and lease_until<now() and attempts>=3;
 update private.watches w set health='degraded',remote_state='unknown',next_check_at=null
 where exists(select 1 from private.jobs x where x.watch_id=w.id and x.generation=w.generation and x.state in ('ambiguous','failed'));
 if exists(select 1 from private.jobs where kind in ('stop','erase') and state='failed') then update private.capacity set enabled=false where id;end if;
 select x.* into j from private.jobs x join private.watches w on w.id=x.watch_id
 where (x.state='ready' or (x.state='leased' and x.lease_until<now() and x.kind<>'start')) and x.available_at<=now() and x.attempts<3 and x.generation=w.generation
 and (x.kind in ('stop','erase') or (exists(select 1 from private.capacity where id and enabled) and exists(select 1 from private.accounts where id=w.owner_id and deleted_at is null) and w.status in ('pending','active')))
 order by x.available_at,x.id for update of x skip locked limit 1;
 if not found then return null;end if;
 update private.jobs set state='leased',attempts=attempts+1,lease_until=now()+interval '60 seconds',lease_token=gen_random_uuid() where id=j.id returning * into j;
 return to_jsonb(j);
end $$;
create or replace function public.s3_purge() returns void language plpgsql security definer set search_path='' as $$
begin
 delete from private.findings where created_at<now()-interval '90 days';
 delete from private.inbox where received_at<now()-interval '14 days';
 delete from private.checks where observed_at<now()-interval '14 days';
 delete from private.commands where created_at<now()-interval '14 days';
 delete from private.jobs where state in ('done','failed') and available_at<now()-interval '14 days';
 delete from private.evaluations where created_at<now()-interval '24 hours';
 delete from private.proposals where expires_at<now() and claimed_by is null;
 update private.proposals set payload='{}'::jsonb where expires_at<now() and claimed_by is not null;
 delete from private.previews p where expires_at<now() and not exists(select 1 from private.proposals where token_hash=p.token_hash) and not exists(select 1 from private.evaluations where token_hash=p.token_hash);
 update private.watches set plan='{}'::jsonb where status='deleted' and remote_state='stopped';
 delete from private.plan_versions v using private.watches w where v.watch_id=w.id and w.status='deleted' and w.remote_state='stopped';
 -- Unresolved remote work must be escalated; this purge is not proof of full account erasure.
end $$;
