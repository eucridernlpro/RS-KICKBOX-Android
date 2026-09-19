-- RS KICKBOX backend foundation
-- Migration 0027: production cloud events and atomic RSVP actions.

create or replace function public.rs_event_catalog()
returns table (
    id uuid,
    title text,
    when_label text,
    location text,
    capacity integer,
    active boolean,
    going_count integer,
    my_status text
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        e.id,
        e.title,
        e.when_label,
        e.location,
        e.capacity,
        e.active,
        (
            select count(*)::integer
            from public.rs_event_rsvps r
            where r.event_id=e.id
              and r.status='going'
        ) as going_count,
        (
            select r2.status
            from public.rs_event_rsvps r2
            where r2.event_id=e.id
              and r2.student_id=(select auth.uid())
            limit 1
        ) as my_status
    from public.rs_events e
    where e.active=true
       or (select private.rs_is_staff())
    order by e.starts_at asc nulls last,e.created_at desc;
$$;

revoke execute on function public.rs_event_catalog() from public;
revoke execute on function public.rs_event_catalog() from anon;
grant execute on function public.rs_event_catalog() to authenticated;

create or replace function public.rs_event_rsvp(p_event_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_event public.rs_events%rowtype;
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

    if v_role<>'student' or coalesce(v_active,false)=false then
        raise exception 'active student account required' using errcode='42501';
    end if;

    select *
    into v_event
    from public.rs_events e
    where e.id=p_event_id
    for update;

    if v_event.id is null then
        raise exception 'event not found' using errcode='P0002';
    end if;
    if not v_event.active then
        raise exception 'event inactive' using errcode='P0001';
    end if;

    select count(*)
    into v_count
    from public.rs_event_rsvps r
    where r.event_id=p_event_id
      and r.status='going';

    if v_count>=v_event.capacity then
        raise exception 'event full' using errcode='P0001';
    end if;

    insert into public.rs_event_rsvps(event_id,student_id,status,created_at,updated_at)
    values(p_event_id,v_uid,'going',now(),now())
    on conflict (event_id,student_id) do update
    set status='going',updated_at=now();

    return 'going';
end;
$$;

revoke execute on function public.rs_event_rsvp(uuid) from public;
revoke execute on function public.rs_event_rsvp(uuid) from anon;
grant execute on function public.rs_event_rsvp(uuid) to authenticated;

create or replace function public.rs_cancel_event_rsvp(p_event_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
begin
    update public.rs_event_rsvps
    set status='cancelled',
        updated_at=now()
    where event_id=p_event_id
      and student_id=(select auth.uid())
      and status='going';

    if not found then
        raise exception 'active RSVP not found' using errcode='P0002';
    end if;

    return 'cancelled';
end;
$$;

revoke execute on function public.rs_cancel_event_rsvp(uuid) from public;
revoke execute on function public.rs_cancel_event_rsvp(uuid) from anon;
grant execute on function public.rs_cancel_event_rsvp(uuid) to authenticated;

create or replace function public.rs_staff_create_event(
    p_title text,
    p_when_label text,
    p_location text,
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

    if trim(p_title)='' or trim(p_when_label)='' or trim(p_location)='' then
        raise exception 'title, date/time and location required' using errcode='22023';
    end if;
    if p_capacity<1 or p_capacity>500 then
        raise exception 'invalid event capacity' using errcode='22023';
    end if;

    insert into public.rs_events(
        title,when_label,location,capacity,active,created_by
    )
    values(
        trim(p_title),trim(p_when_label),trim(p_location),p_capacity,true,(select auth.uid())
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_event(text,text,text,integer) from public;
revoke execute on function public.rs_staff_create_event(text,text,text,integer) from anon;
grant execute on function public.rs_staff_create_event(text,text,text,integer) to authenticated;

create or replace function public.rs_staff_set_event_active(
    p_event_id uuid,
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

    update public.rs_events
    set active=p_active,
        updated_at=now()
    where id=p_event_id;

    if not found then
        raise exception 'event not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_event_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_event_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_event_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_event(p_event_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_events where id=p_event_id;

    if not found then
        raise exception 'event not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_event(uuid) from public;
revoke execute on function public.rs_staff_delete_event(uuid) from anon;
grant execute on function public.rs_staff_delete_event(uuid) to authenticated;
