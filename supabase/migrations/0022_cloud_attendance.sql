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
