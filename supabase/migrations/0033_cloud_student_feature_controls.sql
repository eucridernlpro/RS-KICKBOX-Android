-- RS KICKBOX backend foundation
-- Migration 0033: cloud operational controls and student route locks.

alter table public.rs_app_settings
add column if not exists disabled_student_routes text[] not null default '{}';

create or replace function public.rs_app_controls()
returns table(
    maintenance_enabled boolean,
    maintenance_message text,
    community_posts_enabled boolean,
    class_booking_enabled boolean,
    private_lessons_enabled boolean,
    referrals_enabled boolean,
    in_app_reminders_enabled boolean,
    retention_months integer,
    disabled_student_routes text[]
)
language sql
stable
security definer
set search_path=''
as $$
    select
        s.maintenance_enabled,
        s.maintenance_message,
        s.community_posts_enabled,
        s.class_booking_enabled,
        s.private_lessons_enabled,
        s.referrals_enabled,
        s.in_app_reminders_enabled,
        s.retention_months,
        s.disabled_student_routes
    from public.rs_app_settings s
    where s.singleton=true;
$$;

revoke execute on function public.rs_app_controls() from public;
revoke execute on function public.rs_app_controls() from anon;
grant execute on function public.rs_app_controls() to authenticated;

create or replace function public.rs_staff_update_app_controls(
    p_maintenance_enabled boolean,
    p_maintenance_message text,
    p_community_posts_enabled boolean,
    p_class_booking_enabled boolean,
    p_private_lessons_enabled boolean,
    p_referrals_enabled boolean,
    p_in_app_reminders_enabled boolean,
    p_retention_months integer,
    p_disabled_student_routes text[]
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

    if p_retention_months not in (12,24,36) then
        raise exception 'invalid retention period' using errcode='22023';
    end if;

    update public.rs_app_settings
    set
        maintenance_enabled=p_maintenance_enabled,
        maintenance_message=left(coalesce(p_maintenance_message,''),240),
        community_posts_enabled=p_community_posts_enabled,
        class_booking_enabled=p_class_booking_enabled,
        private_lessons_enabled=p_private_lessons_enabled,
        referrals_enabled=p_referrals_enabled,
        in_app_reminders_enabled=p_in_app_reminders_enabled,
        retention_months=p_retention_months,
        disabled_student_routes=coalesce(p_disabled_student_routes,'{}'::text[]),
        updated_by=(select auth.uid()),
        updated_at=now()
    where singleton=true;
end;
$$;

revoke execute on function public.rs_staff_update_app_controls(boolean,text,boolean,boolean,boolean,boolean,boolean,integer,text[]) from public;
revoke execute on function public.rs_staff_update_app_controls(boolean,text,boolean,boolean,boolean,boolean,boolean,integer,text[]) from anon;
grant execute on function public.rs_staff_update_app_controls(boolean,text,boolean,boolean,boolean,boolean,boolean,integer,text[]) to authenticated;
