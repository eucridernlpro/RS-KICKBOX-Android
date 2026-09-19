-- RS KICKBOX v0.98 BACKEND UPDATE
-- Run once after the successful v0.86 backend update.
-- Adds protected image/short-video attachments to private coach chat
-- and real member-only group chat.

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

