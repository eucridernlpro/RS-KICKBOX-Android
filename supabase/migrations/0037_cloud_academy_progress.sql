-- RS KICKBOX backend foundation
-- Migration 0037: cloud RS Academy progress.

create or replace function public.rs_academy_progress_feed()
returns table(
    track_code text,
    completed_lessons integer
)
language sql
stable
security definer
set search_path=''
as $$
    select a.track_code,a.completed_lessons
    from public.rs_academy_progress a
    where a.student_id=(select auth.uid())
    order by a.track_code;
$$;

revoke execute on function public.rs_academy_progress_feed() from public;
revoke execute on function public.rs_academy_progress_feed() from anon;
grant execute on function public.rs_academy_progress_feed() to authenticated;

create or replace function public.rs_set_academy_progress(
    p_track_code text,
    p_completed_lessons integer
)
returns integer
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_done integer:=greatest(0,least(100,p_completed_lessons));
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if not exists(
        select 1 from public.rs_profiles p
        where p.id=v_uid and p.role='student' and p.active=true
    ) then
        raise exception 'active student account required' using errcode='42501';
    end if;

    insert into public.rs_academy_progress(
        student_id,track_code,completed_lessons,updated_at
    )
    values(
        v_uid,left(trim(p_track_code),80),v_done,now()
    )
    on conflict(student_id,track_code) do update
    set completed_lessons=excluded.completed_lessons,
        updated_at=now();

    return v_done;
end;
$$;

revoke execute on function public.rs_set_academy_progress(text,integer) from public;
revoke execute on function public.rs_set_academy_progress(text,integer) from anon;
grant execute on function public.rs_set_academy_progress(text,integer) to authenticated;
