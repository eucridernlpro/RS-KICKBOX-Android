-- RS KICKBOX backend foundation
-- Migration 0021: production class catalog and atomic booking actions.

create or replace function public.rs_class_catalog()
returns table (
    id uuid,
    title text,
    level text,
    starts_at timestamptz,
    duration_minutes integer,
    capacity integer,
    booking_open boolean,
    active boolean,
    booked_count integer,
    my_booking_status text
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        c.id,
        c.title,
        c.level,
        c.starts_at,
        c.duration_minutes,
        c.capacity,
        c.booking_open,
        c.active,
        (
            select count(*)::integer
            from public.rs_class_bookings b
            where b.class_id=c.id
              and b.status='booked'
        ) as booked_count,
        (
            select b2.status
            from public.rs_class_bookings b2
            where b2.class_id=c.id
              and b2.student_id=(select auth.uid())
            limit 1
        ) as my_booking_status
    from public.rs_classes c
    where c.active=true
       or (select private.rs_is_staff())
    order by c.starts_at asc;
$$;

revoke execute on function public.rs_class_catalog() from public;
revoke execute on function public.rs_class_catalog() from anon;
grant execute on function public.rs_class_catalog() to authenticated;

create or replace function public.rs_book_class(p_class_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_class public.rs_classes%rowtype;
    v_count integer;
    v_role text;
    v_active boolean;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select p.role,p.active
    into v_role,v_active
    from public.rs_profiles p
    where p.id=v_uid;

    if v_role <> 'student' or coalesce(v_active,false)=false then
        raise exception 'active student account required' using errcode='42501';
    end if;

    select *
    into v_class
    from public.rs_classes c
    where c.id=p_class_id
    for update;

    if v_class.id is null then
        raise exception 'class not found' using errcode='P0002';
    end if;
    if not v_class.active then
        raise exception 'class inactive' using errcode='P0001';
    end if;
    if not v_class.booking_open then
        raise exception 'booking closed' using errcode='P0001';
    end if;

    select count(*)
    into v_count
    from public.rs_class_bookings b
    where b.class_id=p_class_id
      and b.status='booked';

    if v_count >= v_class.capacity then
        raise exception 'class full' using errcode='P0001';
    end if;

    insert into public.rs_class_bookings (
        class_id,student_id,status,booked_at,cancelled_at
    )
    values (
        p_class_id,v_uid,'booked',now(),null
    )
    on conflict (class_id,student_id) do update
    set
        status='booked',
        booked_at=now(),
        cancelled_at=null;

    return 'booked';
end;
$$;

revoke execute on function public.rs_book_class(uuid) from public;
revoke execute on function public.rs_book_class(uuid) from anon;
grant execute on function public.rs_book_class(uuid) to authenticated;

create or replace function public.rs_cancel_class_booking(p_class_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    update public.rs_class_bookings
    set status='cancelled',
        cancelled_at=now()
    where class_id=p_class_id
      and student_id=v_uid
      and status <> 'cancelled';

    if not found then
        raise exception 'active booking not found' using errcode='P0002';
    end if;

    return 'cancelled';
end;
$$;

revoke execute on function public.rs_cancel_class_booking(uuid) from public;
revoke execute on function public.rs_cancel_class_booking(uuid) from anon;
grant execute on function public.rs_cancel_class_booking(uuid) to authenticated;

comment on function public.rs_class_catalog() is
'Authenticated class catalog with live booked count and caller booking state.';
comment on function public.rs_book_class(uuid) is
'Atomic student booking action with row lock and server-side capacity enforcement.';
comment on function public.rs_cancel_class_booking(uuid) is
'Cancels the authenticated student booking for one class.';


create or replace function public.rs_staff_create_class(
    p_title text,
    p_level text,
    p_starts_at timestamptz,
    p_duration_minutes integer,
    p_capacity integer
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if trim(p_title)='' then
        raise exception 'class title required' using errcode='22023';
    end if;
    if p_duration_minutes < 15 or p_duration_minutes > 300 then
        raise exception 'invalid duration' using errcode='22023';
    end if;
    if p_capacity < 1 or p_capacity > 100 then
        raise exception 'invalid capacity' using errcode='22023';
    end if;

    insert into public.rs_classes(
        title,level,starts_at,duration_minutes,capacity,
        booking_open,active,created_by
    )
    values(
        trim(p_title),
        coalesce(nullif(trim(p_level),''),'ALL LEVELS'),
        p_starts_at,
        p_duration_minutes,
        p_capacity,
        true,
        true,
        (select auth.uid())
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_class(text,text,timestamptz,integer,integer) from public;
revoke execute on function public.rs_staff_create_class(text,text,timestamptz,integer,integer) from anon;
grant execute on function public.rs_staff_create_class(text,text,timestamptz,integer,integer) to authenticated;

create or replace function public.rs_staff_set_class_state(
    p_class_id uuid,
    p_active boolean,
    p_booking_open boolean
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

    update public.rs_classes
    set active=p_active,
        booking_open=p_booking_open,
        updated_at=now()
    where id=p_class_id;

    if not found then
        raise exception 'class not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_class_state(uuid,boolean,boolean) from public;
revoke execute on function public.rs_staff_set_class_state(uuid,boolean,boolean) from anon;
grant execute on function public.rs_staff_set_class_state(uuid,boolean,boolean) to authenticated;

create or replace function public.rs_staff_delete_class(p_class_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_classes
    where id=p_class_id;

    if not found then
        raise exception 'class not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_class(uuid) from public;
revoke execute on function public.rs_staff_delete_class(uuid) from anon;
grant execute on function public.rs_staff_delete_class(uuid) to authenticated;
