-- RS KICKBOX v0.100 BACKEND UPDATE
-- Run once after the successful v0.86 backend update.
-- Adds chat media/group chat, cloud member services and global branding settings.



-- ============================================================
-- 0038_chat_media_group_chat.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0038: private chat media + real group chat.

alter table public.rs_coach_messages
    add column if not exists media_path text,
    add column if not exists media_kind text,
    add column if not exists media_name text;

alter table public.rs_coach_messages
    alter column body set default '';

alter table public.rs_coach_messages
    drop constraint if exists rs_coach_messages_body_check;

alter table public.rs_coach_messages
    add constraint rs_coach_messages_body_or_media_check
    check (
        (char_length(body) between 1 and 1200)
        or media_path is not null
    );

alter table public.rs_coach_messages
    drop constraint if exists rs_coach_messages_media_kind_check;

alter table public.rs_coach_messages
    add constraint rs_coach_messages_media_kind_check
    check (media_kind is null or media_kind in ('IMAGE','VIDEO'));

create table if not exists public.rs_group_messages (
    id uuid primary key default gen_random_uuid(),
    group_id uuid not null references public.rs_groups(id) on delete cascade,
    sender_id uuid not null references auth.users(id) on delete cascade,
    body text not null default '',
    media_path text,
    media_kind text check (media_kind is null or media_kind in ('IMAGE','VIDEO')),
    media_name text,
    created_at timestamptz not null default now(),
    constraint rs_group_messages_body_or_media_check
        check ((char_length(body) between 1 and 1200) or media_path is not null)
);

create index if not exists rs_group_messages_group_created_idx
on public.rs_group_messages(group_id,created_at);

alter table public.rs_group_messages enable row level security;
revoke all on table public.rs_group_messages from anon,authenticated;
grant select,insert on table public.rs_group_messages to authenticated;

drop policy if exists "rs_group_messages_select_member_or_staff" on public.rs_group_messages;
create policy "rs_group_messages_select_member_or_staff"
on public.rs_group_messages
for select
to authenticated
using (
    (select private.rs_is_staff())
    or exists(
        select 1
        from public.rs_group_memberships gm
        where gm.group_id=rs_group_messages.group_id
          and gm.student_id=(select auth.uid())
    )
);

drop policy if exists "rs_group_messages_insert_member_or_staff" on public.rs_group_messages;
create policy "rs_group_messages_insert_member_or_staff"
on public.rs_group_messages
for insert
to authenticated
with check (
    sender_id=(select auth.uid())
    and (
        (select private.rs_is_staff())
        or exists(
            select 1
            from public.rs_group_memberships gm
            where gm.group_id=rs_group_messages.group_id
              and gm.student_id=(select auth.uid())
        )
    )
);

insert into storage.buckets(
    id,name,public,file_size_limit,allowed_mime_types
)
values(
    'rs-chat-media',
    'rs-chat-media',
    false,
    31457280,
    array[
        'image/jpeg','image/png','image/webp','image/gif',
        'video/mp4','video/webm','video/quicktime','video/3gpp'
    ]
)
on conflict(id) do update
set public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "rs_chat_media_insert_authorized" on storage.objects;
create policy "rs_chat_media_insert_authorized"
on storage.objects
for insert
to authenticated
with check (
    bucket_id='rs-chat-media'
    and (
        (
            (storage.foldername(name))[1]='coach'
            and (
                (storage.foldername(name))[2]=(select auth.uid())::text
                or (select private.rs_is_staff())
            )
        )
        or
        (
            (storage.foldername(name))[1]='group'
            and (
                (select private.rs_is_staff())
                or exists(
                    select 1
                    from public.rs_group_memberships gm
                    where gm.group_id::text=(storage.foldername(name))[2]
                      and gm.student_id=(select auth.uid())
                )
            )
        )
    )
);

drop policy if exists "rs_chat_media_select_authorized" on storage.objects;
create policy "rs_chat_media_select_authorized"
on storage.objects
for select
to authenticated
using (
    bucket_id='rs-chat-media'
    and (
        (
            (storage.foldername(name))[1]='coach'
            and (
                (storage.foldername(name))[2]=(select auth.uid())::text
                or (select private.rs_is_staff())
            )
        )
        or
        (
            (storage.foldername(name))[1]='group'
            and (
                (select private.rs_is_staff())
                or exists(
                    select 1
                    from public.rs_group_memberships gm
                    where gm.group_id::text=(storage.foldername(name))[2]
                      and gm.student_id=(select auth.uid())
                )
            )
        )
    )
);

drop policy if exists "rs_chat_media_delete_authorized" on storage.objects;
create policy "rs_chat_media_delete_authorized"
on storage.objects
for delete
to authenticated
using (
    bucket_id='rs-chat-media'
    and (
        (select private.rs_is_staff())
        or (
            (storage.foldername(name))[1]='coach'
            and (storage.foldername(name))[2]=(select auth.uid())::text
        )
        or (
            (storage.foldername(name))[1]='group'
            and exists(
                select 1
                from public.rs_group_memberships gm
                where gm.group_id::text=(storage.foldername(name))[2]
                  and gm.student_id=(select auth.uid())
            )
        )
    )
);

create or replace function public.rs_coach_thread_messages(p_student_id uuid)
returns table (
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    sender_role text,
    body text,
    media_path text,
    media_kind text,
    media_name text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        m.id,
        m.student_id,
        p.email,
        p.display_name,
        m.sender_role,
        m.body,
        m.media_path,
        m.media_kind,
        m.media_name,
        m.created_at
    from public.rs_coach_messages m
    join public.rs_profiles p on p.id=m.student_id
    where m.student_id=p_student_id
      and (
          (select private.rs_is_staff())
          or (select auth.uid())=p_student_id
      )
    order by m.created_at asc;
$$;

revoke execute on function public.rs_coach_thread_messages(uuid) from public;
revoke execute on function public.rs_coach_thread_messages(uuid) from anon;
grant execute on function public.rs_coach_thread_messages(uuid) to authenticated;

create or replace function public.rs_send_coach_message_v2(
    p_student_id uuid,
    p_body text default '',
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
    v_role text;
    v_id uuid;
    v_body text:=trim(coalesce(p_body,''));
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if length(v_body)>1200 then
        raise exception 'message too long' using errcode='22023';
    end if;
    if v_body='' and p_media_path is null then
        raise exception 'message or attachment required' using errcode='22023';
    end if;
    if p_media_kind is not null and p_media_kind not in ('IMAGE','VIDEO') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;
    if p_media_path is not null
       and p_media_path not like ('coach/'||p_student_id::text||'/%') then
        raise exception 'invalid coach media path' using errcode='22023';
    end if;

    select p.role into v_role
    from public.rs_profiles p
    where p.id=v_uid and p.active=true;

    if v_role='student' and v_uid<>p_student_id then
        raise exception 'students may only message their own coach thread' using errcode='42501';
    elsif v_role not in ('student','trainer','admin') then
        raise exception 'active account required' using errcode='42501';
    end if;

    if not exists(
        select 1 from public.rs_profiles p
        where p.id=p_student_id and p.role='student' and p.active=true
    ) then
        raise exception 'active student not found' using errcode='P0002';
    end if;

    insert into public.rs_coach_messages(
        student_id,sender_id,sender_role,body,media_path,media_kind,media_name
    )
    values(
        p_student_id,
        v_uid,
        case when v_role='student' then 'student' else 'trainer' end,
        v_body,
        p_media_path,
        p_media_kind,
        p_media_name
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_send_coach_message_v2(uuid,text,text,text,text) from public;
revoke execute on function public.rs_send_coach_message_v2(uuid,text,text,text,text) from anon;
grant execute on function public.rs_send_coach_message_v2(uuid,text,text,text,text) to authenticated;

create or replace function public.rs_group_message_feed(p_group_id uuid)
returns table(
    id uuid,
    group_id uuid,
    sender_id uuid,
    sender_email text,
    sender_name text,
    body text,
    media_path text,
    media_kind text,
    media_name text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        m.id,
        m.group_id,
        m.sender_id,
        p.email,
        p.display_name,
        m.body,
        m.media_path,
        m.media_kind,
        m.media_name,
        m.created_at
    from public.rs_group_messages m
    join public.rs_profiles p on p.id=m.sender_id
    where m.group_id=p_group_id
      and (
          (select private.rs_is_staff())
          or exists(
              select 1 from public.rs_group_memberships gm
              where gm.group_id=p_group_id
                and gm.student_id=(select auth.uid())
          )
      )
    order by m.created_at asc;
$$;

revoke execute on function public.rs_group_message_feed(uuid) from public;
revoke execute on function public.rs_group_message_feed(uuid) from anon;
grant execute on function public.rs_group_message_feed(uuid) to authenticated;

create or replace function public.rs_send_group_message(
    p_group_id uuid,
    p_body text default '',
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
    v_body text:=trim(coalesce(p_body,''));
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;
    if length(v_body)>1200 then
        raise exception 'message too long' using errcode='22023';
    end if;
    if v_body='' and p_media_path is null then
        raise exception 'message or attachment required' using errcode='22023';
    end if;
    if p_media_kind is not null and p_media_kind not in ('IMAGE','VIDEO') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;
    if p_media_path is not null
       and p_media_path not like ('group/'||p_group_id::text||'/%') then
        raise exception 'invalid group media path' using errcode='22023';
    end if;

    if not (
        (select private.rs_is_staff())
        or exists(
            select 1 from public.rs_group_memberships gm
            where gm.group_id=p_group_id
              and gm.student_id=v_uid
        )
    ) then
        raise exception 'group membership required' using errcode='42501';
    end if;

    insert into public.rs_group_messages(
        group_id,sender_id,body,media_path,media_kind,media_name
    )
    values(
        p_group_id,v_uid,v_body,p_media_path,p_media_kind,p_media_name
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_send_group_message(uuid,text,text,text,text) from public;
revoke execute on function public.rs_send_group_message(uuid,text,text,text,text) from anon;
grant execute on function public.rs_send_group_message(uuid,text,text,text,text) to authenticated;



-- ============================================================
-- 0039_cloud_member_services.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0039: production cloud member services.

create or replace function public.rs_member_documents_feed()
returns table(
    id uuid,
    title text,
    body text,
    access_tier text,
    active boolean,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select d.id,d.title,d.body,d.access_tier,d.active,d.created_at
    from public.rs_club_documents d
    where
        (select private.rs_is_staff())
        or (
            d.active=true
            and exists(
                select 1
                from public.rs_profiles p
                where p.id=(select auth.uid())
                  and p.active=true
                  and (
                      d.access_tier='ALL'
                      or d.access_tier='BASIC'
                      or (d.access_tier='PRO' and p.plan in ('PRO','ELITE'))
                      or (d.access_tier='ELITE' and p.plan='ELITE')
                  )
            )
        )
    order by d.created_at desc;
$$;

revoke execute on function public.rs_member_documents_feed() from public;
revoke execute on function public.rs_member_documents_feed() from anon;
grant execute on function public.rs_member_documents_feed() to authenticated;

create or replace function public.rs_staff_create_document(
    p_title text,
    p_body text,
    p_access_tier text
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
    if trim(p_title)='' or trim(p_body)='' then
        raise exception 'title and document required' using errcode='22023';
    end if;
    if p_access_tier not in ('ALL','BASIC','PRO','ELITE') then
        raise exception 'invalid access tier' using errcode='22023';
    end if;

    insert into public.rs_club_documents(title,body,access_tier,active,created_by)
    values(trim(p_title),p_body,p_access_tier,true,(select auth.uid()))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_document(text,text,text) from public;
revoke execute on function public.rs_staff_create_document(text,text,text) from anon;
grant execute on function public.rs_staff_create_document(text,text,text) to authenticated;

create or replace function public.rs_staff_set_document_active(
    p_document_id uuid,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    update public.rs_club_documents
    set active=p_active,updated_at=now()
    where id=p_document_id;
    if not found then raise exception 'document not found' using errcode='P0002'; end if;
end;
$$;

revoke execute on function public.rs_staff_set_document_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_document_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_document_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_document(p_document_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_club_documents where id=p_document_id;
    if not found then raise exception 'document not found' using errcode='P0002'; end if;
end;
$$;

revoke execute on function public.rs_staff_delete_document(uuid) from public;
revoke execute on function public.rs_staff_delete_document(uuid) from anon;
grant execute on function public.rs_staff_delete_document(uuid) to authenticated;

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
        t.trainer_reply,
        t.status,
        t.created_at
    from public.rs_support_tickets t
    join public.rs_profiles p on p.id=t.student_id
    where t.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by t.created_at desc;
$$;

revoke execute on function public.rs_support_feed() from public;
revoke execute on function public.rs_support_feed() from anon;
grant execute on function public.rs_support_feed() to authenticated;

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
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if not exists(select 1 from public.rs_profiles p where p.id=v_uid and p.role='student' and p.active=true) then
        raise exception 'active student account required' using errcode='42501';
    end if;
    if trim(p_subject)='' or trim(p_message)='' then
        raise exception 'subject and message required' using errcode='22023';
    end if;

    insert into public.rs_support_tickets(student_id,subject,message)
    values(v_uid,trim(p_subject),trim(p_message))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_create_support_ticket(text,text) from public;
revoke execute on function public.rs_create_support_ticket(text,text) from anon;
grant execute on function public.rs_create_support_ticket(text,text) to authenticated;

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
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    if p_status not in ('OPEN','RESOLVED') then raise exception 'invalid support status' using errcode='22023'; end if;

    update public.rs_support_tickets
    set trainer_reply=coalesce(p_trainer_reply,''),
        status=p_status,
        updated_at=now()
    where id=p_ticket_id;

    if not found then raise exception 'support ticket not found' using errcode='P0002'; end if;
end;
$$;

revoke execute on function public.rs_staff_update_support_ticket(uuid,text,text) from public;
revoke execute on function public.rs_staff_update_support_ticket(uuid,text,text) from anon;
grant execute on function public.rs_staff_update_support_ticket(uuid,text,text) to authenticated;

create or replace function public.rs_staff_delete_support_ticket(p_ticket_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_support_tickets where id=p_ticket_id;
end;
$$;

revoke execute on function public.rs_staff_delete_support_ticket(uuid) from public;
revoke execute on function public.rs_staff_delete_support_ticket(uuid) from anon;
grant execute on function public.rs_staff_delete_support_ticket(uuid) to authenticated;

create or replace function public.rs_referral_feed()
returns table(
    id uuid,
    owner_id uuid,
    owner_email text,
    owner_name text,
    code text,
    uses integer,
    active boolean
)
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_code text;
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;

    if not (select private.rs_is_staff()) and not exists(
        select 1 from public.rs_referrals r where r.owner_id=v_uid
    ) then
        v_code:='RS'||upper(substr(replace(extensions.gen_random_uuid()::text,'-',''),1,7));
        insert into public.rs_referrals(owner_id,code,uses,active)
        values(v_uid,v_code,0,true)
        on conflict(owner_id) do nothing;
    end if;

    return query
    select r.id,r.owner_id,p.email,p.display_name,r.code,r.uses,r.active
    from public.rs_referrals r
    join public.rs_profiles p on p.id=r.owner_id
    where r.owner_id=v_uid
       or (select private.rs_is_staff())
    order by lower(p.display_name),lower(p.email);
end;
$$;

revoke execute on function public.rs_referral_feed() from public;
revoke execute on function public.rs_referral_feed() from anon;
grant execute on function public.rs_referral_feed() to authenticated;

create or replace function public.rs_staff_set_referral_active(
    p_referral_id uuid,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    update public.rs_referrals set active=p_active where id=p_referral_id;
    if not found then raise exception 'referral not found' using errcode='P0002'; end if;
end;
$$;

revoke execute on function public.rs_staff_set_referral_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_referral_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_referral_active(uuid,boolean) to authenticated;



-- ============================================================
-- 0040_cloud_brand_settings.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0040: global club branding and presentation settings.

create table if not exists public.rs_brand_settings (
    singleton boolean primary key default true check (singleton=true),
    header_name text not null default 'RS KICKBOX',
    login_title text not null default 'Premium cinematic kickboxing',
    login_subtitle text not null default 'TRAIN · LEARN · CONNECT · GROW',
    footer_text text not null default 'RS KICKBOX · TRAIN · LEARN · CONNECT · GROW',
    theme_name text not null default 'ELITE_GOLD'
        check (theme_name in ('ELITE_GOLD','CRIMSON_FIGHT_NIGHT','PLATINUM_PRO','EMERALD_PERFORMANCE')),
    login_form_opacity numeric(4,3) not null default 0.820
        check (login_form_opacity between 0.20 and 1.00),
    updated_by uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);

insert into public.rs_brand_settings(singleton)
values(true)
on conflict(singleton) do nothing;

alter table public.rs_brand_settings enable row level security;
revoke all on table public.rs_brand_settings from anon, authenticated;
grant select on table public.rs_brand_settings to anon, authenticated;
grant update on table public.rs_brand_settings to authenticated;

drop policy if exists "rs_brand_settings_public_read" on public.rs_brand_settings;
create policy "rs_brand_settings_public_read"
on public.rs_brand_settings
for select
to anon, authenticated
using(true);

drop policy if exists "rs_brand_settings_staff_update" on public.rs_brand_settings;
create policy "rs_brand_settings_staff_update"
on public.rs_brand_settings
for update
to authenticated
using((select private.rs_is_staff()))
with check((select private.rs_is_staff()));

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

    if p_theme_name not in ('ELITE_GOLD','CRIMSON_FIGHT_NIGHT','PLATINUM_PRO','EMERALD_PERFORMANCE') then
        raise exception 'invalid theme' using errcode='22023';
    end if;

    update public.rs_brand_settings
    set header_name=left(coalesce(nullif(trim(p_header_name),''),'RS KICKBOX'),80),
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

comment on table public.rs_brand_settings is
'Publicly readable club identity/presentation settings. Only active trainer/admin accounts may update.';



-- ============================================================
-- 0041_cloud_visual_assets.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0041: public cloud brand assets and visual placement metadata.

insert into storage.buckets(
    id,name,public,file_size_limit,allowed_mime_types
)
values(
    'rs-brand-assets',
    'rs-brand-assets',
    true,
    31457280,
    array[
        'image/jpeg','image/png','image/webp','image/gif',
        'video/mp4','video/webm','video/quicktime'
    ]
)
on conflict(id) do update
set public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

create table if not exists public.rs_visual_assets(
    slot_key text primary key,
    object_path text not null,
    media_kind text not null check(media_kind in ('IMAGE','GIF','VIDEO')),
    position text not null default 'CENTER'
        check(position in ('LEFT','CENTER','RIGHT','TOP','BOTTOM')),
    overlay_opacity numeric(4,3) not null default 0.550
        check(overlay_opacity between 0.0 and 0.88),
    updated_by uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);

alter table public.rs_visual_assets enable row level security;
revoke all on table public.rs_visual_assets from anon, authenticated;
grant select on table public.rs_visual_assets to anon, authenticated;

drop policy if exists "rs_visual_assets_public_read" on public.rs_visual_assets;
create policy "rs_visual_assets_public_read"
on public.rs_visual_assets
for select
to anon, authenticated
using(true);

drop policy if exists "rs_brand_assets_staff_insert" on storage.objects;
create policy "rs_brand_assets_staff_insert"
on storage.objects
for insert
to authenticated
with check(
    bucket_id='rs-brand-assets'
    and (select private.rs_is_staff())
);

drop policy if exists "rs_brand_assets_staff_update" on storage.objects;
create policy "rs_brand_assets_staff_update"
on storage.objects
for update
to authenticated
using(bucket_id='rs-brand-assets' and (select private.rs_is_staff()))
with check(bucket_id='rs-brand-assets' and (select private.rs_is_staff()));

drop policy if exists "rs_brand_assets_staff_delete" on storage.objects;
create policy "rs_brand_assets_staff_delete"
on storage.objects
for delete
to authenticated
using(bucket_id='rs-brand-assets' and (select private.rs_is_staff()));

create or replace function public.rs_staff_upsert_visual_asset(
    p_slot_key text,
    p_object_path text,
    p_media_kind text,
    p_position text,
    p_overlay_opacity numeric
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
    if p_media_kind not in ('IMAGE','GIF','VIDEO') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;
    if p_position not in ('LEFT','CENTER','RIGHT','TOP','BOTTOM') then
        raise exception 'invalid position' using errcode='22023';
    end if;

    insert into public.rs_visual_assets(
        slot_key,object_path,media_kind,position,overlay_opacity,updated_by,updated_at
    )
    values(
        left(trim(p_slot_key),120),
        trim(p_object_path),
        p_media_kind,
        p_position,
        greatest(0.0,least(0.88,p_overlay_opacity)),
        (select auth.uid()),
        now()
    )
    on conflict(slot_key) do update
    set object_path=excluded.object_path,
        media_kind=excluded.media_kind,
        position=excluded.position,
        overlay_opacity=excluded.overlay_opacity,
        updated_by=excluded.updated_by,
        updated_at=now();
end;
$$;

revoke execute on function public.rs_staff_upsert_visual_asset(text,text,text,text,numeric) from public;
revoke execute on function public.rs_staff_upsert_visual_asset(text,text,text,text,numeric) from anon;
grant execute on function public.rs_staff_upsert_visual_asset(text,text,text,text,numeric) to authenticated;

create or replace function public.rs_staff_update_visual_asset_metadata(
    p_slot_key text,
    p_position text,
    p_overlay_opacity numeric
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
    if p_position not in ('LEFT','CENTER','RIGHT','TOP','BOTTOM') then
        raise exception 'invalid position' using errcode='22023';
    end if;

    update public.rs_visual_assets
    set position=p_position,
        overlay_opacity=greatest(0.0,least(0.88,p_overlay_opacity)),
        updated_by=(select auth.uid()),
        updated_at=now()
    where slot_key=p_slot_key;
end;
$$;

revoke execute on function public.rs_staff_update_visual_asset_metadata(text,text,numeric) from public;
revoke execute on function public.rs_staff_update_visual_asset_metadata(text,text,numeric) from anon;
grant execute on function public.rs_staff_update_visual_asset_metadata(text,text,numeric) to authenticated;

create or replace function public.rs_staff_delete_visual_asset(p_slot_key text)
returns text
language plpgsql
security definer
set search_path=''
as $$
declare v_path text;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_visual_assets
    where slot_key=p_slot_key
    returning object_path into v_path;

    return v_path;
end;
$$;

revoke execute on function public.rs_staff_delete_visual_asset(text) from public;
revoke execute on function public.rs_staff_delete_visual_asset(text) from anon;
grant execute on function public.rs_staff_delete_visual_asset(text) to authenticated;
