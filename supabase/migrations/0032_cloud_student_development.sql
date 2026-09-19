-- RS KICKBOX backend foundation
-- Migration 0032: cloud student development workflows.

create or replace function public.rs_development_students()
returns table(
    id uuid,
    email text,
    display_name text,
    plan text,
    active boolean
)
language sql
stable
security definer
set search_path=''
as $$
    select p.id,p.email,p.display_name,p.plan,p.active
    from public.rs_profiles p
    where p.role='student'
      and p.active=true
      and (select private.rs_is_staff())
    order by lower(p.display_name),lower(p.email);
$$;

revoke execute on function public.rs_development_students() from public;
revoke execute on function public.rs_development_students() from anon;
grant execute on function public.rs_development_students() to authenticated;

create or replace function public.rs_homework_feed()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    title text,
    details text,
    due_label text,
    completed boolean,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select h.id,h.student_id,p.email,p.display_name,h.title,h.details,h.due_label,h.completed,h.created_at
    from public.rs_homework h
    join public.rs_profiles p on p.id=h.student_id
    where h.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by h.created_at desc;
$$;

revoke execute on function public.rs_homework_feed() from public;
revoke execute on function public.rs_homework_feed() from anon;
grant execute on function public.rs_homework_feed() to authenticated;

create or replace function public.rs_staff_assign_homework(
    p_student_id uuid,
    p_title text,
    p_details text,
    p_due_label text
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if trim(p_title)='' or trim(p_details)='' then
        raise exception 'title and details required' using errcode='22023';
    end if;

    insert into public.rs_homework(student_id,title,details,due_label,assigned_by)
    values(p_student_id,trim(p_title),trim(p_details),coalesce(p_due_label,''),(select auth.uid()))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_assign_homework(uuid,text,text,text) from public;
revoke execute on function public.rs_staff_assign_homework(uuid,text,text,text) from anon;
grant execute on function public.rs_staff_assign_homework(uuid,text,text,text) to authenticated;

create or replace function public.rs_set_homework_completed(
    p_homework_id uuid,
    p_completed boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    update public.rs_homework h
    set completed=p_completed,updated_at=now()
    where h.id=p_homework_id
      and (
          h.student_id=(select auth.uid())
          or (select private.rs_is_staff())
      );
    if not found then
        raise exception 'homework not found or not permitted' using errcode='42501';
    end if;
end;
$$;

revoke execute on function public.rs_set_homework_completed(uuid,boolean) from public;
revoke execute on function public.rs_set_homework_completed(uuid,boolean) from anon;
grant execute on function public.rs_set_homework_completed(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_homework(p_homework_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_homework where id=p_homework_id;
end;
$$;

revoke execute on function public.rs_staff_delete_homework(uuid) from public;
revoke execute on function public.rs_staff_delete_homework(uuid) from anon;
grant execute on function public.rs_staff_delete_homework(uuid) to authenticated;

create or replace function public.rs_coach_notes_feed()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    note text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select n.id,n.student_id,p.email,p.display_name,n.note,n.created_at
    from public.rs_coach_notes n
    join public.rs_profiles p on p.id=n.student_id
    where (select private.rs_is_staff())
    order by n.created_at desc;
$$;

revoke execute on function public.rs_coach_notes_feed() from public;
revoke execute on function public.rs_coach_notes_feed() from anon;
grant execute on function public.rs_coach_notes_feed() to authenticated;

create or replace function public.rs_staff_add_coach_note(p_student_id uuid,p_note text)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    insert into public.rs_coach_notes(student_id,note,created_by)
    values(p_student_id,trim(p_note),(select auth.uid()))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_add_coach_note(uuid,text) from public;
revoke execute on function public.rs_staff_add_coach_note(uuid,text) from anon;
grant execute on function public.rs_staff_add_coach_note(uuid,text) to authenticated;

create or replace function public.rs_staff_delete_coach_note(p_note_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_coach_notes where id=p_note_id;
end;
$$;

revoke execute on function public.rs_staff_delete_coach_note(uuid) from public;
revoke execute on function public.rs_staff_delete_coach_note(uuid) from anon;
grant execute on function public.rs_staff_delete_coach_note(uuid) to authenticated;

create or replace function public.rs_assessment_feed()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    punches integer,
    kicks integer,
    defense integer,
    footwork integer,
    combinations integer,
    conditioning integer,
    summary text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select a.id,a.student_id,p.email,p.display_name,a.punches,a.kicks,a.defense,a.footwork,a.combinations,a.conditioning,a.summary,a.created_at
    from public.rs_assessments a
    join public.rs_profiles p on p.id=a.student_id
    where a.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by a.created_at desc;
$$;

revoke execute on function public.rs_assessment_feed() from public;
revoke execute on function public.rs_assessment_feed() from anon;
grant execute on function public.rs_assessment_feed() to authenticated;

create or replace function public.rs_staff_add_assessment(
    p_student_id uuid,
    p_punches integer,
    p_kicks integer,
    p_defense integer,
    p_footwork integer,
    p_combinations integer,
    p_conditioning integer,
    p_summary text
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;

    insert into public.rs_assessments(
        student_id,punches,kicks,defense,footwork,combinations,conditioning,summary,assessed_by
    )
    values(
        p_student_id,
        greatest(0,least(100,p_punches)),
        greatest(0,least(100,p_kicks)),
        greatest(0,least(100,p_defense)),
        greatest(0,least(100,p_footwork)),
        greatest(0,least(100,p_combinations)),
        greatest(0,least(100,p_conditioning)),
        coalesce(p_summary,''),
        (select auth.uid())
    )
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_add_assessment(uuid,integer,integer,integer,integer,integer,integer,text) from public;
revoke execute on function public.rs_staff_add_assessment(uuid,integer,integer,integer,integer,integer,integer,text) from anon;
grant execute on function public.rs_staff_add_assessment(uuid,integer,integer,integer,integer,integer,integer,text) to authenticated;
