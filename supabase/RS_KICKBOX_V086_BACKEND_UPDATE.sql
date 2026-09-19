-- RS KICKBOX v0.86 ADVANCED TEST BACKEND UPDATE
-- Run once in Supabase SQL Editor before testing the v0.86 APK.
-- Contains migrations 0020 through 0037 only; it does not recreate the original project.



-- ============================================================
-- 0020_profile_avatars.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0020: protected member profile avatars.

alter table public.rs_profiles
add column if not exists avatar_path text;

insert into storage.buckets (
    id,
    name,
    public,
    file_size_limit,
    allowed_mime_types
)
values (
    'rs-profile-images',
    'rs-profile-images',
    false,
    2097152,
    array['image/jpeg','image/png','image/webp']
)
on conflict (id) do update
set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "rs_profile_images_select_visible" on storage.objects;
create policy "rs_profile_images_select_visible"
on storage.objects
for select
to authenticated
using (
    bucket_id = 'rs-profile-images'
    and exists (
        select 1
        from public.rs_profiles p
        left join public.rs_social_profiles s on s.user_id = p.id
        where p.id::text = (storage.foldername(name))[1]
          and (
              p.id = (select auth.uid())
              or (select private.rs_is_staff())
              or coalesce(s.public_profile,false) = true
          )
    )
);

drop policy if exists "rs_profile_images_insert_own" on storage.objects;
create policy "rs_profile_images_insert_own"
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

drop policy if exists "rs_profile_images_update_own" on storage.objects;
create policy "rs_profile_images_update_own"
on storage.objects
for update
to authenticated
using (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
)
with check (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

drop policy if exists "rs_profile_images_delete_own" on storage.objects;
create policy "rs_profile_images_delete_own"
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create or replace function public.rs_set_my_avatar(p_avatar_path text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_expected text;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    v_expected := v_uid::text || '/avatar.jpg';

    if p_avatar_path is distinct from v_expected then
        raise exception 'invalid avatar path' using errcode='22023';
    end if;

    update public.rs_profiles
    set avatar_path = p_avatar_path,
        updated_at = now()
    where id = v_uid;
end;
$$;

revoke execute on function public.rs_set_my_avatar(text) from public;
revoke execute on function public.rs_set_my_avatar(text) from anon;
grant execute on function public.rs_set_my_avatar(text) to authenticated;

create or replace function public.rs_member_identity(p_email text)
returns table (
    id uuid,
    display_name text,
    avatar_path text
)
language sql
stable
security definer
set search_path = ''
as $$
    select p.id,p.display_name,p.avatar_path
    from public.rs_profiles p
    left join public.rs_social_profiles s on s.user_id=p.id
    where lower(p.email)=lower(trim(p_email))
      and (
          p.id=(select auth.uid())
          or (select private.rs_is_staff())
          or coalesce(s.public_profile,false)=true
      )
    limit 1;
$$;

revoke execute on function public.rs_member_identity(text) from public;
revoke execute on function public.rs_member_identity(text) from anon;
grant execute on function public.rs_member_identity(text) to authenticated;

comment on column public.rs_profiles.avatar_path is
'Protected Supabase Storage path for the member profile avatar.';

comment on function public.rs_member_identity(text) is
'Returns display identity/avatar only to the member, staff, or when the social profile is public.';



-- ============================================================
-- 0021_cloud_classes_bookings.sql
-- ============================================================

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



-- ============================================================
-- 0022_cloud_attendance.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0022: cloud attendance roster and trainer check-in actions.

create or replace function public.rs_attendance_roster(p_class_id uuid)
returns table (
    student_id uuid,
    display_name text,
    email text,
    booked boolean,
    present boolean,
    checked_in_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.id,
        p.display_name,
        p.email,
        exists(
            select 1
            from public.rs_class_bookings b
            where b.class_id=p_class_id
              and b.student_id=p.id
              and b.status='booked'
        ) as booked,
        coalesce(a.present,false) as present,
        a.checked_in_at
    from public.rs_profiles p
    left join public.rs_attendance a
      on a.class_id=p_class_id
     and a.student_id=p.id
    where p.role='student'
      and p.active=true
      and (select private.rs_is_staff())
    order by
        exists(
            select 1
            from public.rs_class_bookings b2
            where b2.class_id=p_class_id
              and b2.student_id=p.id
              and b2.status='booked'
        ) desc,
        lower(p.display_name),
        lower(p.email);
$$;

revoke execute on function public.rs_attendance_roster(uuid) from public;
revoke execute on function public.rs_attendance_roster(uuid) from anon;
grant execute on function public.rs_attendance_roster(uuid) to authenticated;

create or replace function public.rs_staff_set_attendance(
    p_class_id uuid,
    p_student_id uuid,
    p_present boolean,
    p_note text default ''
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

    if not exists(
        select 1 from public.rs_classes c where c.id=p_class_id
    ) then
        raise exception 'class not found' using errcode='P0002';
    end if;

    if not exists(
        select 1
        from public.rs_profiles p
        where p.id=p_student_id
          and p.role='student'
          and p.active=true
    ) then
        raise exception 'active student not found' using errcode='P0002';
    end if;

    insert into public.rs_attendance(
        class_id,
        student_id,
        present,
        checked_in_at,
        checked_in_by,
        note
    )
    values(
        p_class_id,
        p_student_id,
        p_present,
        case when p_present then now() else null end,
        (select auth.uid()),
        coalesce(p_note,'')
    )
    on conflict (class_id,student_id) do update
    set
        present=excluded.present,
        checked_in_at=excluded.checked_in_at,
        checked_in_by=(select auth.uid()),
        note=excluded.note;
end;
$$;

revoke execute on function public.rs_staff_set_attendance(uuid,uuid,boolean,text) from public;
revoke execute on function public.rs_staff_set_attendance(uuid,uuid,boolean,text) from anon;
grant execute on function public.rs_staff_set_attendance(uuid,uuid,boolean,text) to authenticated;

comment on function public.rs_attendance_roster(uuid) is
'Trainer/admin attendance roster with booking and check-in state for all active students.';
comment on function public.rs_staff_set_attendance(uuid,uuid,boolean,text) is
'Staff-only attendance upsert for one student and class.';



-- ============================================================
-- 0023_cloud_memberships_access.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0023: production membership plans and student access control.

create or replace function public.rs_membership_plan_catalog()
returns table (
    code text,
    name text,
    monthly_cents integer,
    currency text,
    description text,
    active boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.code,
        p.name,
        p.monthly_cents,
        p.currency,
        p.description,
        p.active
    from public.rs_membership_plans p
    order by case p.code when 'BASIC' then 1 when 'PRO' then 2 else 3 end;
$$;

revoke execute on function public.rs_membership_plan_catalog() from public;
revoke execute on function public.rs_membership_plan_catalog() from anon;
grant execute on function public.rs_membership_plan_catalog() to authenticated;

create or replace function public.rs_staff_update_membership_plan(
    p_code text,
    p_monthly_cents integer,
    p_description text,
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

    if p_code not in ('BASIC','PRO','ELITE') then
        raise exception 'invalid plan' using errcode='22023';
    end if;

    if p_monthly_cents < 0 then
        raise exception 'invalid monthly price' using errcode='22023';
    end if;

    update public.rs_membership_plans
    set
        monthly_cents=p_monthly_cents,
        description=coalesce(p_description,''),
        active=p_active,
        updated_by=(select auth.uid()),
        updated_at=now()
    where code=p_code;

    if not found then
        raise exception 'membership plan not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_update_membership_plan(text,integer,text,boolean) from public;
revoke execute on function public.rs_staff_update_membership_plan(text,integer,text,boolean) from anon;
grant execute on function public.rs_staff_update_membership_plan(text,integer,text,boolean) to authenticated;

create or replace function public.rs_staff_student_access_catalog()
returns table (
    id uuid,
    email text,
    display_name text,
    plan text,
    active boolean,
    membership_status text,
    amount_cents integer,
    current_period_end timestamptz
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
        p.plan,
        p.active,
        coalesce(m.status,'active') as membership_status,
        coalesce(m.amount_cents,mp.monthly_cents,0) as amount_cents,
        m.current_period_end
    from public.rs_profiles p
    left join public.rs_memberships m on m.student_id=p.id
    left join public.rs_membership_plans mp on mp.code=p.plan
    where p.role='student'
      and (select private.rs_is_staff())
    order by lower(p.display_name),lower(p.email);
$$;

revoke execute on function public.rs_staff_student_access_catalog() from public;
revoke execute on function public.rs_staff_student_access_catalog() from anon;
grant execute on function public.rs_staff_student_access_catalog() to authenticated;

create or replace function public.rs_staff_set_student_access(
    p_student_id uuid,
    p_plan text,
    p_active boolean,
    p_membership_status text default 'active'
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_amount integer;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_plan not in ('BASIC','PRO','ELITE') then
        raise exception 'invalid plan' using errcode='22023';
    end if;

    if p_membership_status not in ('active','paused','cancelled','past_due') then
        raise exception 'invalid membership status' using errcode='22023';
    end if;

    if not exists(
        select 1
        from public.rs_profiles p
        where p.id=p_student_id and p.role='student'
    ) then
        raise exception 'student not found' using errcode='P0002';
    end if;

    select mp.monthly_cents
    into v_amount
    from public.rs_membership_plans mp
    where mp.code=p_plan;

    update public.rs_profiles
    set
        plan=p_plan,
        active=p_active,
        updated_at=now()
    where id=p_student_id;

    insert into public.rs_memberships(
        student_id,
        plan,
        amount_cents,
        currency,
        status,
        updated_at
    )
    values(
        p_student_id,
        p_plan,
        coalesce(v_amount,0),
        'EUR',
        p_membership_status,
        now()
    )
    on conflict (student_id) do update
    set
        plan=excluded.plan,
        amount_cents=excluded.amount_cents,
        currency=excluded.currency,
        status=excluded.status,
        updated_at=now();
end;
$$;

revoke execute on function public.rs_staff_set_student_access(uuid,text,boolean,text) from public;
revoke execute on function public.rs_staff_set_student_access(uuid,text,boolean,text) from anon;
grant execute on function public.rs_staff_set_student_access(uuid,text,boolean,text) to authenticated;

create or replace function public.rs_my_membership()
returns table (
    plan text,
    active boolean,
    membership_status text,
    amount_cents integer,
    currency text,
    current_period_end timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.plan,
        p.active,
        coalesce(m.status,'active'),
        coalesce(m.amount_cents,mp.monthly_cents,0),
        coalesce(m.currency,mp.currency,'EUR'),
        m.current_period_end
    from public.rs_profiles p
    left join public.rs_memberships m on m.student_id=p.id
    left join public.rs_membership_plans mp on mp.code=p.plan
    where p.id=(select auth.uid())
    limit 1;
$$;

revoke execute on function public.rs_my_membership() from public;
revoke execute on function public.rs_my_membership() from anon;
grant execute on function public.rs_my_membership() to authenticated;



-- ============================================================
-- 0024_cloud_coach_messaging.sql
-- ============================================================

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
as $
    select p.id
    from public.rs_profiles p
    where p.id=(select auth.uid())
      and p.role='student'
      and p.active=true
    limit 1;
$;

revoke execute on function public.rs_my_student_id() from public;
revoke execute on function public.rs_my_student_id() from anon;
grant execute on function public.rs_my_student_id() to authenticated;

