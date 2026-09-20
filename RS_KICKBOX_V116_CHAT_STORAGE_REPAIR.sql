-- RS KICKBOXING v0.116 chat/storage repair
-- Safe to run more than once.
-- Repairs the missing rs-chat-media bucket, storage policies,
-- trainer chat delete/clear RPCs and quota-ledger cleanup.

-- 1) Ensure chat media columns/tables required by the Android app exist.
alter table public.rs_coach_messages
    add column if not exists media_path text,
    add column if not exists media_kind text,
    add column if not exists media_name text;

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

-- 2) Create/repair the private chat media bucket.
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
set name=excluded.name,
    public=excluded.public,
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

-- 3) Recreate the trainer/admin chat management RPCs used by the app.
create or replace function public.rs_staff_delete_coach_message(p_message_id uuid)
returns text
language plpgsql security definer set search_path=''
as $coach_delete$
declare v_path text;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_coach_messages
    where id=p_message_id
    returning media_path into v_path;

    if not found then
        raise exception 'message not found' using errcode='P0002';
    end if;

    return v_path;
end;
$coach_delete$;

create or replace function public.rs_staff_clear_coach_thread(p_student_id uuid)
returns table(media_path text)
language plpgsql security definer set search_path=''
as $coach_clear$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    return query
    delete from public.rs_coach_messages m
    where m.student_id=p_student_id
    returning m.media_path;
end;
$coach_clear$;

create or replace function public.rs_staff_delete_group_message(p_message_id uuid)
returns text
language plpgsql security definer set search_path=''
as $group_delete$
declare v_path text;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_group_messages
    where id=p_message_id
    returning media_path into v_path;

    if not found then
        raise exception 'message not found' using errcode='P0002';
    end if;

    return v_path;
end;
$group_delete$;

create or replace function public.rs_staff_clear_group_chat(p_group_id uuid)
returns table(media_path text)
language plpgsql security definer set search_path=''
as $group_clear$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    return query
    delete from public.rs_group_messages m
    where m.group_id=p_group_id
    returning m.media_path;
end;
$group_clear$;

revoke execute on function public.rs_staff_delete_coach_message(uuid) from public,anon;
revoke execute on function public.rs_staff_clear_coach_thread(uuid) from public,anon;
revoke execute on function public.rs_staff_delete_group_message(uuid) from public,anon;
revoke execute on function public.rs_staff_clear_group_chat(uuid) from public,anon;

grant execute on function public.rs_staff_delete_coach_message(uuid) to authenticated;
grant execute on function public.rs_staff_clear_coach_thread(uuid) to authenticated;
grant execute on function public.rs_staff_delete_group_message(uuid) to authenticated;
grant execute on function public.rs_staff_clear_group_chat(uuid) to authenticated;

-- 4) Keep the per-student quota ledger synchronized when media is removed.
create or replace function public.rs_remove_student_media_asset_by_path(
    p_storage_bucket text,
    p_storage_path text
) returns void
language plpgsql security definer set search_path=''
as $ledger_cleanup$
declare
    v_uid uuid := (select auth.uid());
    v_student_id uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select a.student_id
      into v_student_id
      from public.rs_student_media_assets a
     where a.storage_bucket=trim(p_storage_bucket)
       and a.storage_path=trim(p_storage_path)
     limit 1;

    if v_student_id is null then
        return;
    end if;

    if v_uid<>v_student_id and not (select private.rs_is_staff()) then
        raise exception 'student or staff access required' using errcode='42501';
    end if;

    delete from public.rs_student_media_assets a
     where a.storage_bucket=trim(p_storage_bucket)
       and a.storage_path=trim(p_storage_path);
end;
$ledger_cleanup$;

revoke execute on function public.rs_remove_student_media_asset_by_path(text,text) from public,anon;
grant execute on function public.rs_remove_student_media_asset_by_path(text,text) to authenticated;

comment on function public.rs_remove_student_media_asset_by_path(text,text) is
'Removes a tracked student-media quota row when its underlying chat/technique media is deleted.';

-- 5) Verification result: should return one row for rs-chat-media.
select id,name,public,file_size_limit,allowed_mime_types
from storage.buckets
where id='rs-chat-media';
