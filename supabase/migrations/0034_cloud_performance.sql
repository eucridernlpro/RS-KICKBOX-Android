-- RS KICKBOX backend foundation
-- Migration 0034: cloud challenges, fight camp and performance state.

create or replace function public.rs_challenge_feed()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    title text,
    target integer,
    current integer,
    unit text,
    active boolean
)
language sql
stable
security definer
set search_path=''
as $$
    select c.id,c.student_id,p.email,p.display_name,c.title,c.target,c.current,c.unit,c.active
    from public.rs_challenges c
    join public.rs_profiles p on p.id=c.student_id
    where c.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by c.created_at desc;
$$;

revoke execute on function public.rs_challenge_feed() from public;
revoke execute on function public.rs_challenge_feed() from anon;
grant execute on function public.rs_challenge_feed() to authenticated;

create or replace function public.rs_staff_assign_challenge(
    p_student_id uuid,
    p_title text,
    p_target integer,
    p_unit text
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
    if trim(p_title)='' then
        raise exception 'challenge title required' using errcode='22023';
    end if;

    insert into public.rs_challenges(
        student_id,title,target,current,unit,active,assigned_by
    )
    values(
        p_student_id,
        trim(p_title),
        greatest(1,least(10000,p_target)),
        0,
        coalesce(nullif(trim(p_unit),''),'sessions'),
        true,
        (select auth.uid())
    )
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_assign_challenge(uuid,text,integer,text) from public;
revoke execute on function public.rs_staff_assign_challenge(uuid,text,integer,text) from anon;
grant execute on function public.rs_staff_assign_challenge(uuid,text,integer,text) to authenticated;

create or replace function public.rs_update_my_challenge_progress(
    p_challenge_id uuid,
    p_delta integer
)
returns integer
language plpgsql
security definer
set search_path=''
as $$
declare
    v_target integer;
    v_current integer;
begin
    select c.target,c.current
    into v_target,v_current
    from public.rs_challenges c
    where c.id=p_challenge_id
      and c.student_id=(select auth.uid())
      and c.active=true
    for update;

    if v_target is null then
        raise exception 'challenge not found' using errcode='P0002';
    end if;

    v_current:=greatest(0,least(v_target,v_current+p_delta));

    update public.rs_challenges
    set current=v_current,updated_at=now()
    where id=p_challenge_id;

    return v_current;
end;
$$;

revoke execute on function public.rs_update_my_challenge_progress(uuid,integer) from public;
revoke execute on function public.rs_update_my_challenge_progress(uuid,integer) from anon;
grant execute on function public.rs_update_my_challenge_progress(uuid,integer) to authenticated;

create or replace function public.rs_staff_set_challenge_active(
    p_challenge_id uuid,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    update public.rs_challenges
    set active=p_active,updated_at=now()
    where id=p_challenge_id;

    if not found then
        raise exception 'challenge not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_challenge_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_challenge_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_challenge_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_challenge(p_challenge_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    delete from public.rs_challenges where id=p_challenge_id;
end;
$$;

revoke execute on function public.rs_staff_delete_challenge(uuid) from public;
revoke execute on function public.rs_staff_delete_challenge(uuid) from anon;
grant execute on function public.rs_staff_delete_challenge(uuid) to authenticated;

create or replace function public.rs_fight_camp_feed()
returns table(
    student_id uuid,
    student_email text,
    student_name text,
    current_week integer,
    total_weeks integer,
    focus text,
    active boolean
)
language sql
stable
security definer
set search_path=''
as $$
    select f.student_id,p.email,p.display_name,f.current_week,f.total_weeks,f.focus,f.active
    from public.rs_fight_camps f
    join public.rs_profiles p on p.id=f.student_id
    where f.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by lower(p.display_name);
$$;

revoke execute on function public.rs_fight_camp_feed() from public;
revoke execute on function public.rs_fight_camp_feed() from anon;
grant execute on function public.rs_fight_camp_feed() to authenticated;

create or replace function public.rs_staff_set_fight_camp(
    p_student_id uuid,
    p_current_week integer,
    p_focus text,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    insert into public.rs_fight_camps(
        student_id,current_week,total_weeks,focus,active,managed_by
    )
    values(
        p_student_id,
        greatest(1,least(8,p_current_week)),
        8,
        left(coalesce(p_focus,''),500),
        p_active,
        (select auth.uid())
    )
    on conflict(student_id) do update
    set current_week=excluded.current_week,
        total_weeks=8,
        focus=excluded.focus,
        active=excluded.active,
        managed_by=(select auth.uid()),
        updated_at=now();
end;
$$;

revoke execute on function public.rs_staff_set_fight_camp(uuid,integer,text,boolean) from public;
revoke execute on function public.rs_staff_set_fight_camp(uuid,integer,text,boolean) from anon;
grant execute on function public.rs_staff_set_fight_camp(uuid,integer,text,boolean) to authenticated;
