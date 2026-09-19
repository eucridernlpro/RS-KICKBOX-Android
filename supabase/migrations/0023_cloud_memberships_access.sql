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
