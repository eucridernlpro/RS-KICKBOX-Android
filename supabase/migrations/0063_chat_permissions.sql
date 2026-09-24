-- RS KICKBOX backend
-- Migration 0063: centralized trainer chat permissions.
-- Safe to run after 0062. Adds only new columns/RPCs and preserves existing defaults.

alter table if exists public.rs_app_settings
    add column if not exists chat_student_images_enabled boolean not null default true,
    add column if not exists chat_student_videos_enabled boolean not null default true,
    add column if not exists chat_student_audio_enabled boolean not null default true,
    add column if not exists chat_student_files_enabled boolean not null default true,
    add column if not exists chat_private_calls_enabled boolean not null default true,
    add column if not exists chat_group_media_enabled boolean not null default true,
    add column if not exists chat_community_media_enabled boolean not null default true,
    add column if not exists chat_auto_media_preview_enabled boolean not null default true,
    add column if not exists chat_gallery_enabled boolean not null default true,
    add column if not exists chat_media_retention_days integer not null default 7;

create or replace function public.rs_chat_permissions()
returns table(
    student_images_enabled boolean,
    student_videos_enabled boolean,
    student_audio_enabled boolean,
    student_files_enabled boolean,
    private_calls_enabled boolean,
    group_media_enabled boolean,
    community_media_enabled boolean,
    auto_media_preview_enabled boolean,
    gallery_enabled boolean,
    media_retention_days integer
)
language sql
stable
security definer
set search_path=''
as $$
    select
        s.chat_student_images_enabled,
        s.chat_student_videos_enabled,
        s.chat_student_audio_enabled,
        s.chat_student_files_enabled,
        s.chat_private_calls_enabled,
        s.chat_group_media_enabled,
        s.chat_community_media_enabled,
        s.chat_auto_media_preview_enabled,
        s.chat_gallery_enabled,
        greatest(1,least(30,s.chat_media_retention_days))
    from public.rs_app_settings s
    where s.singleton=true;
$$;

create or replace function public.rs_staff_update_chat_permissions(
    p_student_images_enabled boolean,
    p_student_videos_enabled boolean,
    p_student_audio_enabled boolean,
    p_student_files_enabled boolean,
    p_private_calls_enabled boolean,
    p_group_media_enabled boolean,
    p_community_media_enabled boolean,
    p_auto_media_preview_enabled boolean,
    p_gallery_enabled boolean,
    p_media_retention_days integer
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

    update public.rs_app_settings
    set chat_student_images_enabled=coalesce(p_student_images_enabled,true),
        chat_student_videos_enabled=coalesce(p_student_videos_enabled,true),
        chat_student_audio_enabled=coalesce(p_student_audio_enabled,true),
        chat_student_files_enabled=coalesce(p_student_files_enabled,true),
        chat_private_calls_enabled=coalesce(p_private_calls_enabled,true),
        chat_group_media_enabled=coalesce(p_group_media_enabled,true),
        chat_community_media_enabled=coalesce(p_community_media_enabled,true),
        chat_auto_media_preview_enabled=coalesce(p_auto_media_preview_enabled,true),
        chat_gallery_enabled=coalesce(p_gallery_enabled,true),
        chat_media_retention_days=greatest(1,least(30,coalesce(p_media_retention_days,7)))
    where singleton=true;
end;
$$;

revoke all on function public.rs_chat_permissions() from public,anon;
revoke all on function public.rs_staff_update_chat_permissions(boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,integer) from public,anon;
grant execute on function public.rs_chat_permissions() to authenticated;
grant execute on function public.rs_staff_update_chat_permissions(boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,integer) to authenticated;

notify pgrst, 'reload schema';
