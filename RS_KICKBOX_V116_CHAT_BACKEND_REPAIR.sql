-- RS KICKBOXING v0.116 chat backend repair
-- Safe to run more than once.
-- Creates/repairs chat storage, access policies, and quota-ledger cleanup.

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

create or replace function public.rs_remove_student_media_asset_by_path(
    p_storage_bucket text,
    p_storage_path text
) returns void
language plpgsql security definer set search_path=''
as $cleanup$
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
$cleanup$;

revoke execute on function public.rs_remove_student_media_asset_by_path(text,text) from public,anon;
grant execute on function public.rs_remove_student_media_asset_by_path(text,text) to authenticated;
