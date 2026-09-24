-- RS KICKBOX backend
-- Migration 0064: make chat media expiry honor centralized trainer retention days.
-- Requires 0063_chat_permissions.sql.

create or replace function public.rs_expired_chat_media_paths()
returns table(media_path text)
language sql
stable
security definer
set search_path=''
as $$
    with cfg as (
        select greatest(1,least(30,coalesce(s.chat_media_retention_days,7))) as retention_days
        from public.rs_app_settings s
        where s.singleton=true
    ),
    visible_paths as (
        select distinct m.media_path
        from public.rs_coach_messages m,cfg
        where m.media_path is not null
          and m.created_at < now()-(cfg.retention_days||' days')::interval
          and ((select private.rs_is_staff()) or m.student_id=(select auth.uid()))

        union

        select distinct m.media_path
        from public.rs_group_messages m,cfg
        where m.media_path is not null
          and m.created_at < now()-(cfg.retention_days||' days')::interval
          and (
              (select private.rs_is_staff())
              or exists(
                  select 1
                  from public.rs_group_memberships gm
                  where gm.group_id=m.group_id
                    and gm.student_id=(select auth.uid())
              )
          )

        union

        select distinct c.media_path
        from public.rs_community_posts c,cfg
        where c.media_path is not null
          and c.created_at < now()-(cfg.retention_days||' days')::interval
          and (c.author_id=(select auth.uid()) or (select private.rs_is_staff()))

        union

        select distinct t.media_path
        from public.rs_support_tickets t,cfg
        where t.media_path is not null
          and t.created_at < now()-(cfg.retention_days||' days')::interval
          and (t.student_id=(select auth.uid()) or (select private.rs_is_staff()))

        union

        select distinct t.trainer_media_path
        from public.rs_support_tickets t,cfg
        where t.trainer_media_path is not null
          and t.created_at < now()-(cfg.retention_days||' days')::interval
          and (t.student_id=(select auth.uid()) or (select private.rs_is_staff()))
    )
    select v.media_path
    from visible_paths v
    where v.media_path is not null and v.media_path<>'';
$$;

create or replace function public.rs_clear_expired_chat_media_path(p_media_path text)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_allowed boolean:=false;
    v_retention_days integer:=7;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select greatest(1,least(30,coalesce(s.chat_media_retention_days,7)))
    into v_retention_days
    from public.rs_app_settings s
    where s.singleton=true;

    if (select private.rs_is_staff()) then
        v_allowed:=true;
    else
        v_allowed:=exists(
            select 1
            from public.rs_coach_messages m
            where m.media_path=p_media_path
              and m.student_id=v_uid
              and m.created_at < now()-(v_retention_days||' days')::interval
        )
        or exists(
            select 1
            from public.rs_group_messages m
            join public.rs_group_memberships gm
              on gm.group_id=m.group_id
             and gm.student_id=v_uid
            where m.media_path=p_media_path
              and m.created_at < now()-(v_retention_days||' days')::interval
        )
        or exists(
            select 1
            from public.rs_community_posts cp
            where cp.media_path=p_media_path
              and cp.author_id=v_uid
              and cp.created_at < now()-(v_retention_days||' days')::interval
        )
        or exists(
            select 1
            from public.rs_support_tickets st
            where (st.media_path=p_media_path or st.trainer_media_path=p_media_path)
              and st.student_id=v_uid
              and st.created_at < now()-(v_retention_days||' days')::interval
        );
    end if;

    if not v_allowed then
        raise exception 'media cleanup access denied' using errcode='42501';
    end if;

    update public.rs_coach_messages
    set media_path=null,media_kind=null,media_name=null
    where media_path=p_media_path
      and created_at < now()-(v_retention_days||' days')::interval;

    update public.rs_group_messages
    set media_path=null,media_kind=null,media_name=null
    where media_path=p_media_path
      and created_at < now()-(v_retention_days||' days')::interval;

    update public.rs_community_posts
    set media_path=null,media_kind=null,media_name=null
    where media_path=p_media_path
      and created_at < now()-(v_retention_days||' days')::interval;

    update public.rs_support_tickets
    set media_path=null,media_kind=null,media_name=null
    where media_path=p_media_path
      and created_at < now()-(v_retention_days||' days')::interval;

    update public.rs_support_tickets
    set trainer_media_path=null,trainer_media_kind=null,trainer_media_name=null
    where trainer_media_path=p_media_path
      and created_at < now()-(v_retention_days||' days')::interval;
end;
$$;

revoke all on function public.rs_expired_chat_media_paths() from public,anon;
revoke all on function public.rs_clear_expired_chat_media_path(text) from public,anon;
grant execute on function public.rs_expired_chat_media_paths() to authenticated;
grant execute on function public.rs_clear_expired_chat_media_path(text) to authenticated;

notify pgrst, 'reload schema';
