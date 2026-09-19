-- RS KICKBOX backend foundation
-- Migration 0028: production cloud private lessons.

create or replace function public.rs_private_lesson_catalog()
returns table (
    slot_id uuid,
    day_label text,
    time_label text,
    duration_minutes integer,
    active boolean,
    my_booking_id uuid,
    my_status text,
    my_note text
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        s.id,
        s.day_label,
        s.time_label,
        s.duration_minutes,
        s.active,
        (
            select b.id
            from public.rs_private_bookings b
            where b.slot_id=s.id
              and b.student_id=(select auth.uid())
              and b.status<>'cancelled'
            order by b.requested_at desc
            limit 1
        ) as my_booking_id,
        (
            select b.status
            from public.rs_private_bookings b
            where b.slot_id=s.id
              and b.student_id=(select auth.uid())
              and b.status<>'cancelled'
            order by b.requested_at desc
            limit 1
        ) as my_status,
        (
            select b.note
            from public.rs_private_bookings b
            where b.slot_id=s.id
              and b.student_id=(select auth.uid())
              and b.status<>'cancelled'
            order by b.requested_at desc
            limit 1
        ) as my_note
    from public.rs_private_slots s
    where s.active=true
       or (select private.rs_is_staff())
    order by s.starts_at asc nulls last,s.created_at asc;
$$;

revoke execute on function public.rs_private_lesson_catalog() from public;
revoke execute on function public.rs_private_lesson_catalog() from anon;
grant execute on function public.rs_private_lesson_catalog() to authenticated;

create or replace function public.rs_request_private_lesson(
    p_slot_id uuid,
    p_note text default ''
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_role text;
    v_active boolean;
    v_id uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select p.role,p.active
    into v_role,v_active
    from public.rs_profiles p
    where p.id=v_uid;

    if v_role<>'student' or coalesce(v_active,false)=false then
        raise exception 'active student account required' using errcode='42501';
    end if;

    if not exists(
        select 1 from public.rs_private_slots s
        where s.id=p_slot_id and s.active=true
    ) then
        raise exception 'private lesson slot not available' using errcode='P0002';
    end if;

    if exists(
        select 1
        from public.rs_private_bookings b
        where b.slot_id=p_slot_id
          and b.status='confirmed'
          and b.student_id<>v_uid
    ) then
        raise exception 'private lesson slot already confirmed' using errcode='P0001';
    end if;

    insert into public.rs_private_bookings(
        slot_id,student_id,note,status,requested_at,updated_at
    )
    values(
        p_slot_id,v_uid,left(coalesce(p_note,''),1000),'requested',now(),now()
    )
    on conflict (slot_id,student_id) do update
    set
        note=excluded.note,
        status='requested',
        requested_at=now(),
        updated_at=now()
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_request_private_lesson(uuid,text) from public;
revoke execute on function public.rs_request_private_lesson(uuid,text) from anon;
grant execute on function public.rs_request_private_lesson(uuid,text) to authenticated;

create or replace function public.rs_cancel_private_lesson_request(p_booking_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    update public.rs_private_bookings
    set status='cancelled',
        updated_at=now()
    where id=p_booking_id
      and student_id=(select auth.uid())
      and status in ('requested','confirmed','declined');

    if not found then
        raise exception 'private lesson request not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_cancel_private_lesson_request(uuid) from public;
revoke execute on function public.rs_cancel_private_lesson_request(uuid) from anon;
grant execute on function public.rs_cancel_private_lesson_request(uuid) to authenticated;

create or replace function public.rs_staff_private_lesson_requests()
returns table (
    booking_id uuid,
    slot_id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    note text,
    status text,
    day_label text,
    time_label text,
    duration_minutes integer
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        b.id,
        b.slot_id,
        b.student_id,
        p.email,
        p.display_name,
        b.note,
        b.status,
        s.day_label,
        s.time_label,
        s.duration_minutes
    from public.rs_private_bookings b
    join public.rs_profiles p on p.id=b.student_id
    join public.rs_private_slots s on s.id=b.slot_id
    where (select private.rs_is_staff())
      and b.status<>'cancelled'
    order by b.requested_at desc;
$$;

revoke execute on function public.rs_staff_private_lesson_requests() from public;
revoke execute on function public.rs_staff_private_lesson_requests() from anon;
grant execute on function public.rs_staff_private_lesson_requests() to authenticated;

create or replace function public.rs_staff_create_private_slot(
    p_day_label text,
    p_time_label text,
    p_duration_minutes integer
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

    if trim(p_day_label)='' or trim(p_time_label)='' then
        raise exception 'day and time required' using errcode='22023';
    end if;

    if p_duration_minutes<15 or p_duration_minutes>180 then
        raise exception 'invalid duration' using errcode='22023';
    end if;

    insert into public.rs_private_slots(
        trainer_id,day_label,time_label,duration_minutes,active
    )
    values(
        (select auth.uid()),
        trim(p_day_label),
        trim(p_time_label),
        p_duration_minutes,
        true
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_private_slot(text,text,integer) from public;
revoke execute on function public.rs_staff_create_private_slot(text,text,integer) from anon;
grant execute on function public.rs_staff_create_private_slot(text,text,integer) to authenticated;

create or replace function public.rs_staff_set_private_slot_active(
    p_slot_id uuid,
    p_active boolean
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

    update public.rs_private_slots
    set active=p_active,
        updated_at=now()
    where id=p_slot_id;

    if not found then
        raise exception 'private lesson slot not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_private_slot_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_private_slot_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_private_slot_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_private_slot(p_slot_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_private_slots where id=p_slot_id;

    if not found then
        raise exception 'private lesson slot not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_private_slot(uuid) from public;
revoke execute on function public.rs_staff_delete_private_slot(uuid) from anon;
grant execute on function public.rs_staff_delete_private_slot(uuid) to authenticated;

create or replace function public.rs_staff_set_private_booking_status(
    p_booking_id uuid,
    p_status text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_slot_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_status not in ('requested','confirmed','declined') then
        raise exception 'invalid booking status' using errcode='22023';
    end if;

    select b.slot_id into v_slot_id
    from public.rs_private_bookings b
    where b.id=p_booking_id
    for update;

    if v_slot_id is null then
        raise exception 'private lesson request not found' using errcode='P0002';
    end if;

    if p_status='confirmed' then
        update public.rs_private_bookings
        set status='declined',
            updated_at=now()
        where slot_id=v_slot_id
          and id<>p_booking_id
          and status in ('requested','confirmed');
    end if;

    update public.rs_private_bookings
    set status=p_status,
        updated_at=now()
    where id=p_booking_id;
end;
$$;

revoke execute on function public.rs_staff_set_private_booking_status(uuid,text) from public;
revoke execute on function public.rs_staff_set_private_booking_status(uuid,text) from anon;
grant execute on function public.rs_staff_set_private_booking_status(uuid,text) to authenticated;
