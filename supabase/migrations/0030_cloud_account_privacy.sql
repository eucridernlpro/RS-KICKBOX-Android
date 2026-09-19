-- RS KICKBOX backend foundation
-- Migration 0030: production account/privacy request workflow.

create or replace function public.rs_my_account_requests()
returns table (
    id uuid,
    request_type text,
    status text,
    user_note text,
    staff_note text,
    requested_at timestamptz,
    completed_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        r.id,
        r.request_type,
        r.status,
        r.user_note,
        r.staff_note,
        r.requested_at,
        r.completed_at
    from public.rs_account_requests r
    where r.user_id=(select auth.uid())
    order by r.requested_at desc;
$$;

revoke execute on function public.rs_my_account_requests() from public;
revoke execute on function public.rs_my_account_requests() from anon;
grant execute on function public.rs_my_account_requests() to authenticated;

create or replace function public.rs_request_account_action(
    p_request_type text,
    p_user_note text default ''
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_id uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if p_request_type not in ('data_export','account_deletion') then
        raise exception 'invalid request type' using errcode='22023';
    end if;

    select r.id into v_id
    from public.rs_account_requests r
    where r.user_id=v_uid
      and r.request_type=p_request_type
      and r.status in ('requested','processing')
    order by r.requested_at desc
    limit 1;

    if v_id is not null then
        return v_id;
    end if;

    insert into public.rs_account_requests(
        user_id,
        request_type,
        status,
        user_note
    )
    values(
        v_uid,
        p_request_type,
        'requested',
        left(coalesce(p_user_note,''),1000)
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_request_account_action(text,text) from public;
revoke execute on function public.rs_request_account_action(text,text) from anon;
grant execute on function public.rs_request_account_action(text,text) to authenticated;

create or replace function public.rs_staff_account_requests()
returns table (
    id uuid,
    user_id uuid,
    email text,
    display_name text,
    request_type text,
    status text,
    user_note text,
    staff_note text,
    requested_at timestamptz,
    completed_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        r.id,
        r.user_id,
        p.email,
        p.display_name,
        r.request_type,
        r.status,
        r.user_note,
        r.staff_note,
        r.requested_at,
        r.completed_at
    from public.rs_account_requests r
    left join public.rs_profiles p on p.id=r.user_id
    where (select private.rs_is_staff())
    order by
        case r.status when 'requested' then 1 when 'processing' then 2 else 3 end,
        r.requested_at desc;
$$;

revoke execute on function public.rs_staff_account_requests() from public;
revoke execute on function public.rs_staff_account_requests() from anon;
grant execute on function public.rs_staff_account_requests() to authenticated;

create or replace function public.rs_staff_update_account_request(
    p_request_id uuid,
    p_status text,
    p_staff_note text default ''
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_status not in ('requested','processing','completed','rejected','cancelled') then
        raise exception 'invalid request status' using errcode='22023';
    end if;

    update public.rs_account_requests
    set
        status=p_status,
        staff_note=left(coalesce(p_staff_note,''),2000),
        completed_at=case when p_status in ('completed','rejected','cancelled') then now() else null end
    where id=p_request_id;

    if not found then
        raise exception 'account request not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_update_account_request(uuid,text,text) from public;
revoke execute on function public.rs_staff_update_account_request(uuid,text,text) from anon;
grant execute on function public.rs_staff_update_account_request(uuid,text,text) to authenticated;
