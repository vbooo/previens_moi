-- A ledger marker also blocks access when a stale/partial restore omitted the account row.
create or replace function private.owner() returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid:=auth.uid();begin
 if u is null or not exists(select 1 from auth.users where id=u) or exists(select 1 from private.erasure_ledger where owner_id=u) then perform private.fail('session_required');end if;
 insert into private.accounts(id) values(u) on conflict do nothing;
 perform 1 from private.accounts where id=u and deleted_at is null for update;
 if not found then perform private.fail('session_required');end if;
 return u;
end $$;
