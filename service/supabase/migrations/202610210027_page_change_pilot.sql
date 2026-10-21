-- First real scope: literal page-text changes explicitly requested by URL.
-- This is not qualification of general search or semantic relevance.
create table private.page_change_pilot(id boolean primary key default true check(id),enabled boolean not null default false,interval_hours int not null default 1 check(interval_hours between 1 and 24));
insert into private.page_change_pilot(id) values(true);
alter table private.page_change_pilot enable row level security;
alter function public.s3_preview(text,text,jsonb,uuid) rename to s3_preview_interpretation;
revoke all on function public.s3_preview_interpretation(text,text,jsonb,uuid) from public,anon,authenticated,service_role;
create function public.s3_preview(action text,token text default null,body jsonb default null,resource uuid default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare old boolean; r jsonb; d jsonb:=body->'draft'; source text; fr boolean:=d->>'language'='fr'; p jsonb; pid uuid; exp timestamptz; interval_hours int; available int;begin
 old:=exists(select 1 from private.evaluations where id=(body->>'id')::uuid);
 r:=public.s3_preview_interpretation(action,token,body,resource);
 if action<>'evaluate' or old or r->>'status'<>'unsupported' or coalesce(jsonb_array_length(d->'answers'),0)<>0 then return r;end if;
 select pp.interval_hours,greatest(c.slots-c.reserved,0) into interval_hours,available from private.page_change_pilot pp cross join private.capacity c where pp.id and pp.enabled and c.id and c.enabled and exists(select 1 from private.method_qualifications where method='known_page' and qualified);
 if not found then return r;end if;
 if fr and d->>'text' ~* '^Préviens-moi de tout changement sur https://[^[:space:]]+$' then source:=regexp_replace(d->>'text','^Préviens-moi de tout changement sur ','','i');
 elsif not fr and d->>'text' ~* '^Notify me of any change on https://[^[:space:]]+$' then source:=regexp_replace(d->>'text','^Notify me of any change on ','','i');
 else return r;end if;
 if length(source)>500 or source !~ '^https://[a-zA-Z][a-zA-Z0-9.-]*[.][a-zA-Z]{2,}(/[!$&''()*+,;=:@%a-zA-Z0-9._~/?-]*)?$' or source ~* '^https://[^/]+[.](local|internal|localhost|invalid|test)(/|$)' then return r;end if;
 pid:=gen_random_uuid();exp:=now()+interval '30 minutes';
 p:=jsonb_build_object('id',pid,'version',1,'draft',d,'title',case when fr then 'Changements du texte de la page' else 'Changes to page text' end,
 'criteria',jsonb_build_array(case when fr then 'Ajouts et suppressions de texte sur la page indiquée.' else 'Text additions and removals on the specified page.' end),
 'exclusions',jsonb_build_array(case when fr then 'Images, apparence et espace privé non couverts.' else 'Images, visual layout and private areas are not covered.' end),
 'coverage',case when fr then 'Essai limité à cette URL publique. La première lecture établit un état initial silencieux ; les lectures suivantes comparent le texte. La recherche générale de sources reste en préparation.' else 'Pilot limited to this public URL. The first read establishes a silent baseline; later reads compare text. General source discovery remains in preparation.' end,
 'partial',true,'sources',jsonb_build_array(jsonb_build_object('url',source,'label',case when fr then 'Page publique demandée' else 'Requested public page' end)),
 'method','known_page','intervalHours',interval_hours,
 'limitations',jsonb_build_array(case when fr then 'Activation après une première lecture réussie. Les pages protégées ou inaccessibles peuvent être refusées.' else 'Activation requires a successful initial read. Protected or inaccessible pages may be rejected.' end,
 case when fr then 'Modifications limitées à 20 lignes et 1 000 caractères de preuve ; sinon le suivi signale un contrôle dégradé.' else 'Changes are limited to 20 lines and 1,000 evidence characters; larger changes produce a degraded check.' end,
 case when fr then 'La même modification textuelle exacte ne déclenche pas deux notifications. La date de publication reste inconnue.' else 'The same exact text change does not send two notifications. Publication date remains unknown.' end),
 'limits',jsonb_build_object('activePerAccount',1,'globalAvailable',available,'previewRemaining',greatest(5-(select count(*) from private.evaluations where token_hash=encode(extensions.digest(token,'sha256'),'hex')),0),'resetsAt',null),
 'expiresAt',to_char(exp at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'));
 insert into private.proposals(id,token_hash,version,payload,expires_at) values(pid,encode(extensions.digest(token,'sha256'),'hex'),1,p,exp);
 r:=jsonb_build_object('id',body->>'id','draftRevision',(d->>'revision')::bigint,'updatedAt',to_char(now() at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),'status','ready','proposal',p);
 update private.evaluations set payload=r where id=(body->>'id')::uuid;return r;
end $$;
revoke all on function public.s3_preview(text,text,jsonb,uuid) from public,anon,authenticated;
grant execute on function public.s3_preview(text,text,jsonb,uuid) to service_role;

-- A finding and its budget settlement belong to the same completion transaction.
drop function public.s3_provider_complete(uuid,uuid,timestamptz,text,bigint);
create function public.s3_provider_complete(job_id uuid,lease uuid,observed timestamptz,outcome text,credits bigint,fact_key text default null,finding jsonb default null) returns void language plpgsql security definer set search_path='' as $$
declare j private.jobs;w private.watches;attempt private.provider_attempts;begin
 perform 1 from private.capacity where id for update;
 select * into j from private.jobs where id=job_id and lease_token=lease and state='leased' and lease_until>now() for update;
 if not found then perform private.fail('version_conflict');end if;
 select * into w from private.watches where id=j.watch_id and generation=j.generation for update;
 if not found then perform private.fail('version_conflict');end if;
 select * into attempt from private.provider_attempts where provider_attempts.job_id=j.id and phase='poll';
 if not found or observed<attempt.run_intent_at or observed>now()+interval '5 minutes' then perform private.fail('invalid_input');end if;
 if credits<0 or credits>2 then
  update private.capacity set enabled=false where id;
  update private.jobs set state='ambiguous' where id=j.id;
  update private.watches set health='degraded',remote_state='unknown',next_check_at=null where id=w.id;return;
 end if;
 if outcome='finding' and (w.plan->'criteria' not in ('["Ajouts et suppressions de texte sur la page indiquée."]'::jsonb,'["Text additions and removals on the specified page."]'::jsonb) or w.plan->>'method'<>'known_page' or jsonb_array_length(w.plan->'sources')<>1 or finding is null or fact_key !~ '^[0-9a-f]{64}$' or finding->'criteriaSnapshot'<>w.plan->'criteria' or finding->>'planId'<>w.plan->>'id' or finding->>'noveltyBasis'<>'baseline_change' or (finding->>'observedAt')::timestamptz<>observed or exists(select 1 from jsonb_array_elements(finding->'evidence') e where e->>'url'<>w.plan->'sources'->0->>'url')) then perform private.fail('invalid_input');end if;
 perform public.s3_settle(j.id,credits);
 if j.kind='start' and outcome='baseline' then perform public.s3_confirm_start(j.id,lease,attempt.monitor_id,observed);
 elsif j.kind='check' and outcome in ('no_change','failed','finding') then
  perform public.s3_record_check(w.id,w.generation,attempt.check_id,outcome,observed,fact_key,finding);
  update private.watches set next_check_at=case when status='active' then now()+((plan->>'intervalHours')||' hours')::interval else null end where id=w.id;
 else perform private.fail('invalid_input');end if;
 update private.jobs set state='done',lease_until=null where id=j.id;
 update private.provider_attempts set phase='done' where provider_attempts.job_id=j.id;
end $$;
revoke all on function public.s3_provider_complete(uuid,uuid,timestamptz,text,bigint,text,jsonb) from public,anon,authenticated;
grant execute on function public.s3_provider_complete(uuid,uuid,timestamptz,text,bigint,text,jsonb) to service_role;
