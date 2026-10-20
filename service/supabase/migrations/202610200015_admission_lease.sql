create or replace function public.s3_admission_lease() returns jsonb language plpgsql security definer set search_path='' as $$
declare j private.admission_jobs;begin
 perform 1 from private.admission_capacity where id for update;
 update private.admission_jobs set state='unknown' where state='leased' and lease_until<now();
 update private.evaluations e set payload=jsonb_build_object('id',e.id,'status','failed','draftRevision',(e.payload->>'draftRevision')::bigint,'updatedAt',now(),'error',jsonb_build_object('code','temporary_failure','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope',null)) where exists(select 1 from private.admission_jobs expired where expired.id=e.id and expired.state='unknown') and e.payload->>'status'='pending';
 if not exists(select 1 from private.admission_capacity where id and enabled and qualified and used_micro_usd+reserved_micro_usd+10000<=budget_micro_usd) then return null;end if;
 select x.* into j from private.admission_jobs x join private.evaluations e on e.id=x.id where x.state='ready' and e.created_at>now()-interval '24 hours' order by e.created_at for update of x skip locked limit 1;
 if not found then return null;end if;
 update private.admission_capacity set reserved_micro_usd=reserved_micro_usd+10000 where id;
 update private.admission_jobs set state='leased',lease=gen_random_uuid(),lease_until=now()+interval '60 seconds',reserved_micro_usd=10000 where id=j.id returning * into j;
 return to_jsonb(j);
end $$;
