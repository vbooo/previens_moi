create function private.page_cursor(at timestamptz, key uuid) returns text language sql stable set search_path='' as $$
 select replace(encode(convert_to(jsonb_build_object('at',at,'id',key)::text,'UTF8'),'base64'),E'\n','')
$$;
create function private.cursor_at(c text) returns timestamptz language plpgsql immutable set search_path='' as $$
begin return (convert_from(decode(c,'base64'),'UTF8')::jsonb->>'at')::timestamptz;
exception when others then perform private.fail('invalid_input');return null;end $$;
create function private.cursor_id(c text) returns uuid language plpgsql immutable set search_path='' as $$
begin return (convert_from(decode(c,'base64'),'UTF8')::jsonb->>'id')::uuid;
exception when others then perform private.fail('invalid_input');return null;end $$;
revoke all on function private.page_cursor(timestamptz,uuid),private.cursor_at(text),private.cursor_id(text) from public,anon,authenticated;
create or replace function public.s3_read(route text, resource uuid default null, page_limit int default 20, page_cursor text default null) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid; w private.watches; result jsonb; begin
 u:=private.owner();
 if page_limit not between 1 and 100 then perform private.fail('invalid_input'); end if;
 if page_cursor is not null then perform private.cursor_at(page_cursor);perform private.cursor_id(page_cursor);end if;
 -- Stable cursor: (created_at,id), never OFFSET.
 if route='account' then return jsonb_build_object('id',u,'language','fr','limits',private.limits());
 elsif route='command' then
  perform private.command_age(resource);
  select receipt into result from private.commands where owner_id=u and id=resource;
 elsif route='watch' then
  select private.watch_json(t) into result from private.watches t where t.id=resource and t.owner_id=u and status<>'deleted';
 elsif route='watches' then
  select jsonb_build_object('items',coalesce(jsonb_agg(private.watch_json(q) order by q.created_at,q.id),'[]'::jsonb),'nextCursor',case when count(*)=page_limit then (array_agg(private.page_cursor(q.created_at,q.id) order by q.created_at desc,q.id desc))[1] else null end,'serverTime',now()) into result
  from (select * from private.watches where owner_id=u and status<>'deleted' and (page_cursor is null or (created_at,id)>(private.cursor_at(page_cursor),private.cursor_id(page_cursor))) order by created_at,id limit page_limit) q;
 elsif route='finding' then
  select f.payload into result from private.findings f join private.watches t on t.id=f.watch_id where f.id=resource and f.owner_id=u and t.status<>'deleted' and f.created_at>now()-interval '90 days';
 elsif route='findings' then
  if not exists(select 1 from private.watches where id=resource and owner_id=u and status<>'deleted') then perform private.fail('not_found'); end if;
  select jsonb_build_object('items',coalesce(jsonb_agg(q.payload order by q.created_at,q.id),'[]'::jsonb),'nextCursor',case when count(*)=page_limit then (array_agg(private.page_cursor(q.created_at,q.id) order by q.created_at desc,q.id desc))[1] else null end,'serverTime',now()) into result
  from (select f.id,f.payload,f.created_at from private.findings f where f.watch_id=resource and f.owner_id=u and f.created_at>now()-interval '90 days' and (page_cursor is null or (f.created_at,f.id)>(private.cursor_at(page_cursor),private.cursor_id(page_cursor))) order by f.created_at,f.id limit page_limit) q;
 else perform private.fail('not_found'); end if;
 if result is null then perform private.fail('not_found'); end if;
 return result;
end $$;
