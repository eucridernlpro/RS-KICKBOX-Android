-- RS KICKBOX backend foundation
-- Migration 0042: cloud splash behavior settings.

alter table public.rs_brand_settings
    add column if not exists intro_enabled boolean not null default true,
    add column if not exists intro_every_launch boolean not null default true,
    add column if not exists intro_video_sound boolean not null default true,
    add column if not exists intro_skip_enabled boolean not null default true;

create or replace function public.rs_staff_save_intro_settings(
    p_intro_enabled boolean,
    p_intro_every_launch boolean,
    p_intro_video_sound boolean,
    p_intro_skip_enabled boolean
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

    update public.rs_brand_settings
    set intro_enabled=p_intro_enabled,
        intro_every_launch=p_intro_every_launch,
        intro_video_sound=p_intro_video_sound,
        intro_skip_enabled=p_intro_skip_enabled,
        updated_by=(select auth.uid()),
        updated_at=now()
    where singleton=true;
end;
$$;

revoke execute on function public.rs_staff_save_intro_settings(boolean,boolean,boolean,boolean) from public;
revoke execute on function public.rs_staff_save_intro_settings(boolean,boolean,boolean,boolean) from anon;
grant execute on function public.rs_staff_save_intro_settings(boolean,boolean,boolean,boolean) to authenticated;
