-- Dedicated cleanup lease cannot accidentally claim a start/check while execution is closed.
create function public.s3_stop_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;begin
 select x.* into j from private.jobs x join private.watches w on w.id=x.watch_id
 where x.kind in ('stop','erase') and x.generation=w.generation and x.attempts<3 and x.available_at<=now()
 and (x.state='ready' or (x.state='leased' and x.lease_until<now())) order by x.available_at,x.id for update of x skip locked limit 1;
 if not found then return null;end if;
 update private.jobs set state='leased',attempts=attempts+1,lease_until=now()+interval '90 seconds',lease_token=gen_random_uuid() where id=j.id returning * into j;
 select * into w from private.watches where id=j.watch_id;
 return to_jsonb(j)||jsonb_build_object('providerId',w.provider_id,'uncertainStart',exists(select 1 from private.jobs p where p.watch_id=w.id and p.kind='start' and p.state in ('leased','ambiguous')));
end $$;
create function public.s3_stop_retry(job_id uuid,lease uuid) returns void language plpgsql security definer set search_path='' as $$
begin
 update private.jobs set state=case when attempts>=3 then 'failed' else 'ready' end,lease_until=null,available_at=now()+interval '30 seconds'
 where id=job_id and lease_token=lease and state='leased' and kind in ('stop','erase');
 update private.watches w set remote_state='unknown',health='degraded',next_check_at=null where exists(select 1 from private.jobs j where j.id=job_id and j.watch_id=w.id and j.state='failed');
 if exists(select 1 from private.jobs where id=job_id and state='failed') then update private.capacity set enabled=false where id;end if;
end $$;
revoke all on function public.s3_stop_lease(),public.s3_stop_retry(uuid,uuid) from public,anon,authenticated;
grant execute on function public.s3_stop_lease(),public.s3_stop_retry(uuid,uuid) to service_role;
