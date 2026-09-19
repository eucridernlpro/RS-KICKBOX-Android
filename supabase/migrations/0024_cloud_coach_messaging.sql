-- RS KICKBOX backend foundation
-- Migration 0024: production private coach messaging.

create or replace function public.rs_coach_threads()
returns table (
    student_id uuid,
    student_email text,
    student_name text,
    last_message text,
    last_message_at timestamptz,
    unread_count integer
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.id,
        p.email,
        p.display_name,
        (
            select m.body
            from public.rs_coach_messages m
            where m.student_id=p.id
            order by m.created_at desc
            limit 1
        ) as last_message,
        (
            select m.created_at
            from public.rs_coach_messages m
            where m.student_id=p.id
            order by m.created_at desc
            limit 1
        ) as last_message_at,
        (
            select count(*)::integer
            from public.rs_coach_messages m
            where m.student_id=p.id
              and m.sender_role='student'
              and m.created_at > coalesce(
                  (
                      select r.last_read_at
                      from public.rs_coach_message_reads r
                      where r.user_id=(select auth.uid())
                        and r.student_id=p.id
                  ),
                  to_timestamp(0)
              )
        ) as unread_count
    from public.rs_profiles p
    where p.role='student'
      and p.active=true
      and (select private.rs_is_staff())
    order by last_message_at desc nulls last,lower(p.display_name);
$$;

revoke execute on function public.rs_coach_threads() from public;
revoke execute on function public.rs_coach_threads() from anon;
grant execute on function public.rs_coach_threads() to authenticated;

create or replace function public.rs_coach_thread_messages(p_student_id uuid)
returns table (
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    sender_role text,
    body text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        m.id,
        m.student_id,
        p.email,
        p.display_name,
        m.sender_role,
        m.body,
        m.created_at
    from public.rs_coach_messages m
    join public.rs_profiles p on p.id=m.student_id
    where m.student_id=p_student_id
      and (
          (select private.rs_is_staff())
          or (select auth.uid())=p_student_id
      )
    order by m.created_at asc;
$$;

revoke execute on function public.rs_coach_thread_messages(uuid) from public;
revoke execute on function public.rs_coach_thread_messages(uuid) from anon;
grant execute on function public.rs_coach_thread_messages(uuid) to authenticated;

create or replace function public.rs_send_coach_message(
    p_student_id uuid,
    p_body text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_role text;
    v_message_id uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if length(trim(p_body)) < 1 or length(trim(p_body)) > 1200 then
        raise exception 'message must contain 1 to 1200 characters' using errcode='22023';
    end if;

    select p.role into v_role
    from public.rs_profiles p
    where p.id=v_uid and p.active=true;

    if v_role='student' then
        if v_uid <> p_student_id then
            raise exception 'students may only message their own coach thread' using errcode='42501';
        end if;
    elsif v_role not in ('trainer','admin') then
        raise exception 'active member account required' using errcode='42501';
    end if;

    if not exists(
        select 1 from public.rs_profiles p
        where p.id=p_student_id and p.role='student' and p.active=true
    ) then
        raise exception 'active student not found' using errcode='P0002';
    end if;

    insert into public.rs_coach_messages(
        student_id,
        sender_id,
        sender_role,
        body
    )
    values(
        p_student_id,
        v_uid,
        case when v_role='student' then 'student' else 'trainer' end,
        trim(p_body)
    )
    returning id into v_message_id;

    return v_message_id;
end;
$$;

revoke execute on function public.rs_send_coach_message(uuid,text) from public;
revoke execute on function public.rs_send_coach_message(uuid,text) from anon;
grant execute on function public.rs_send_coach_message(uuid,text) to authenticated;

create or replace function public.rs_mark_coach_thread_read(p_student_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_role text;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select p.role into v_role
    from public.rs_profiles p
    where p.id=v_uid and p.active=true;

    if v_role='student' and v_uid<>p_student_id then
        raise exception 'students may only mark their own thread' using errcode='42501';
    end if;
    if v_role not in ('student','trainer','admin') then
        raise exception 'active member account required' using errcode='42501';
    end if;

    insert into public.rs_coach_message_reads(
        user_id,
        student_id,
        last_read_at
    )
    values(
        v_uid,
        p_student_id,
        now()
    )
    on conflict (user_id,student_id) do update
    set last_read_at=excluded.last_read_at;
end;
$$;

revoke execute on function public.rs_mark_coach_thread_read(uuid) from public;
revoke execute on function public.rs_mark_coach_thread_read(uuid) from anon;
grant execute on function public.rs_mark_coach_thread_read(uuid) to authenticated;

create or replace function public.rs_my_student_id()
returns table (student_id uuid)
language sql
stable
security definer
set search_path = ''
as $$
    select p.id
    from public.rs_profiles p
    where p.id=(select auth.uid())
      and p.role='student'
      and p.active=true
    limit 1;
$$;

revoke execute on function public.rs_my_student_id() from public;
revoke execute on function public.rs_my_student_id() from anon;
grant execute on function public.rs_my_student_id() to authenticated;
