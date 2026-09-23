-- RS KICKBOX backend repair
-- Migration 0062: theme expansion + Support Chat recovery.

-- ---------------------------------------------------------------------------
-- 1) Expand global RS themes.
-- ---------------------------------------------------------------------------

alter table if exists public.rs_brand_settings
    drop constraint if exists rs_brand_settings_theme_name_check;

alter table if exists public.rs_brand_settings
    add constraint rs_brand_settings_theme_name_check
    check (
        theme_name in (
            'ELITE_GOLD',
            'CRIMSON_FIGHT_NIGHT',
            'PLATINUM_PRO',
            'EMERALD_PERFORMANCE',
            'ROYAL_SAPPHIRE',
            'PURPLE_LEGACY',
            'ICE_TITANIUM',
            'INFERNO_NEON'
        )
    );

create or replace function public.rs_staff_save_brand_settings(
    p_header_name text,
    p_login_title text,
    p_login_subtitle text,
    p_footer_text text,
    p_theme_name text,
    p_login_form_opacity numeric
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

    if p_theme_name not in (
        'ELITE_GOLD',
        'CRIMSON_FIGHT_NIGHT',
        'PLATINUM_PRO',
        'EMERALD_PERFORMANCE',
        'ROYAL_SAPPHIRE',
        'PURPLE_LEGACY',
        'ICE_TITANIUM',
        'INFERNO_NEON'
    ) then
        raise exception 'invalid theme' using errcode='22023';
    end if;

    update public.rs_brand_settings
    set header_name=left(coalesce(nullif(trim(p_header_name),''),'RS KICKBOXING'),80),
        login_title=left(coalesce(p_login_title,''),140),
        login_subtitle=left(coalesce(p_login_subtitle,''),180),
        footer_text=left(coalesce(p_footer_text,''),180),
        theme_name=p_theme_name,
        login_form_opacity=greatest(0.20,least(1.00,p_login_form_opacity)),
        updated_by=(select auth.uid()),
        updated_at=now()
    where singleton=true;
end;
$$;

revoke execute on function public.rs_staff_save_brand_settings(text,text,text,text,text,numeric) from public;
revoke execute on function public.rs_staff_save_brand_settings(text,text,text,text,text,numeric) from anon;
grant execute on function public.rs_staff_save_brand_settings(text,text,text,text,text,numeric) to authenticated;

-- ---------------------------------------------------------------------------
-- 2) Repair Support Chat schema and RPCs.
-- ---------------------------------------------------------------------------

alter table if exists public.rs_support_tickets
    add column if not exists media_path text,
    add column if not exists media_kind text,
    add column if not exists media_name text,
    add column if not exists trainer_media_path text,
    add column if not exists trainer_media_kind text,
    add column if not exists trainer_media_name text;

create or replace function public.rs_support_feed()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    subject text,
    message text,
    trainer_reply text,
    status text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        t.id,
        t.student_id,
        p.email,
        p.display_name,
        t.subject,
        t.message,
        coalesce(t.trainer_reply,''),
        t.status,
        t.created_at
    from public.rs_support_tickets t
    join public.rs_profiles p on p.id=t.student_id
    where t.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by t.created_at asc;
$$;

create or replace function public.rs_create_support_ticket(
    p_subject text,
    p_message text
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_id uuid;
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
    if trim(coalesce(p_subject,''))='' or trim(coalesce(p_message,''))='' then
        raise exception 'subject and message required' using errcode='22023';
    end if;

    insert into public.rs_support_tickets(student_id,subject,message)
    values(v_uid,left(trim(p_subject),120),trim(p_message))
    returning id into v_id;

    return v_id;
end;
$$;

create or replace function public.rs_staff_update_support_ticket(
    p_ticket_id uuid,
    p_trainer_reply text,
    p_status text
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
    if p_status not in ('OPEN','RESOLVED') then
        raise exception 'invalid support status' using errcode='22023';
    end if;

    update public.rs_support_tickets
    set trainer_reply=coalesce(p_trainer_reply,''),
        status=p_status,
        updated_at=now()
    where id=p_ticket_id;

    if not found then
        raise exception 'support ticket not found' using errcode='P0002';
    end if;
end;
$$;

create or replace function public.rs_staff_delete_support_ticket(p_ticket_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    delete from public.rs_support_tickets where id=p_ticket_id;
end;
$$;

create or replace function public.rs_support_feed_v2()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    subject text,
    message text,
    trainer_reply text,
    status text,
    media_path text,
    media_kind text,
    media_name text,
    trainer_media_path text,
    trainer_media_kind text,
    trainer_media_name text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        t.id,t.student_id,p.email,p.display_name,
        t.subject,t.message,coalesce(t.trainer_reply,''),t.status,
        t.media_path,t.media_kind,t.media_name,
        t.trainer_media_path,t.trainer_media_kind,t.trainer_media_name,
        t.created_at
    from public.rs_support_tickets t
    join public.rs_profiles p on p.id=t.student_id
    where t.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by t.created_at asc;
$$;

create or replace function public.rs_create_support_ticket_v2(
    p_subject text default 'RS Support',
    p_message text default '',
    p_media_path text default null,
    p_media_kind text default null,
    p_media_name text default null
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_id uuid;
    v_message text:=trim(coalesce(p_message,''));
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
    if v_message='' and p_media_path is null then
        raise exception 'message or attachment required' using errcode='22023';
    end if;

    insert into public.rs_support_tickets(
        student_id,subject,message,media_path,media_kind,media_name
    )
    values(
        v_uid,
        left(coalesce(nullif(trim(p_subject),''),'RS Support'),120),
        v_message,
        p_media_path,
        p_media_kind,
        p_media_name
    )
    returning id into v_id;

    return v_id;
end;
$$;

create or replace function public.rs_staff_update_support_ticket_v2(
    p_ticket_id uuid,
    p_trainer_reply text,
    p_status text,
    p_media_path text default null,
    p_media_kind text default null,
    p_media_name text default null
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
    if p_status not in ('OPEN','RESOLVED') then
        raise exception 'invalid support status' using errcode='22023';
    end if;

    update public.rs_support_tickets
    set trainer_reply=coalesce(p_trainer_reply,''),
        status=p_status,
        trainer_media_path=p_media_path,
        trainer_media_kind=p_media_kind,
        trainer_media_name=p_media_name,
        updated_at=now()
    where id=p_ticket_id;

    if not found then
        raise exception 'support ticket not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_support_feed() from public,anon;
revoke execute on function public.rs_create_support_ticket(text,text) from public,anon;
revoke execute on function public.rs_staff_update_support_ticket(uuid,text,text) from public,anon;
revoke execute on function public.rs_staff_delete_support_ticket(uuid) from public,anon;
revoke execute on function public.rs_support_feed_v2() from public,anon;
revoke execute on function public.rs_create_support_ticket_v2(text,text,text,text,text) from public,anon;
revoke execute on function public.rs_staff_update_support_ticket_v2(uuid,text,text,text,text,text) from public,anon;

grant execute on function public.rs_support_feed() to authenticated;
grant execute on function public.rs_create_support_ticket(text,text) to authenticated;
grant execute on function public.rs_staff_update_support_ticket(uuid,text,text) to authenticated;
grant execute on function public.rs_staff_delete_support_ticket(uuid) to authenticated;
grant execute on function public.rs_support_feed_v2() to authenticated;
grant execute on function public.rs_create_support_ticket_v2(text,text,text,text,text) to authenticated;
grant execute on function public.rs_staff_update_support_ticket_v2(uuid,text,text,text,text,text) to authenticated;

notify pgrst, 'reload schema';
