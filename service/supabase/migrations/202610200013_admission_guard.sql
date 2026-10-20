create or replace function public.s3_admission_finish(target uuid,lease_id uuid,result jsonb,interpretation jsonb,actual bigint default null) returns void language plpgsql security definer set search_path='' as $$
declare j private.admission_jobs;begin
 perform 1 from private.admission_capacity where id for update;
 select * into j from private.admission_jobs where id=target and lease=lease_id and state='leased' and lease_until>now() for update;
 if not found then perform private.fail('version_conflict');end if;
 if result->>'id' is distinct from target::text or result->>'status' is null or result->>'status' not in ('clarification','unsupported','failed') or result->'draftRevision' is distinct from j.request->'draft'->'revision' then perform private.fail('invalid_input');end if;
 -- No ready proposal can be manufactured by a model or anonymous client.
 if actual is not null and (actual<0 or actual>j.reserved_micro_usd) then
  update private.admission_capacity set enabled=false where id;
  update private.admission_jobs set state='unknown' where id=target;
  update private.evaluations set payload=jsonb_build_object('id',target,'draftRevision',j.request->'draft'->'revision','updatedAt',now(),'status','failed','error',jsonb_build_object('code','quota_exceeded','requestId',gen_random_uuid(),'retryAfterSeconds',null,'scope','admission')) where id=target;
  return;
 end if;
 update private.admission_jobs set state=case when actual is null then 'unknown' else 'done' end,interpretation=s3_admission_finish.interpretation,actual_micro_usd=actual where id=target;
 if actual is not null then update private.admission_capacity set reserved_micro_usd=reserved_micro_usd-j.reserved_micro_usd,used_micro_usd=used_micro_usd+actual where id;end if;
 update private.evaluations set payload=result where id=target;
end $$;
