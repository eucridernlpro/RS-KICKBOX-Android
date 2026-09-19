-- RS KICKBOX backend foundation
-- Migration 0025: protected cloud training media storage and catalog actions.

insert into storage.buckets (
    id,
    name,
    public,
    file_size_limit,
    allowed_mime_types
)
values (
    'rs-training-media',
    'rs-training-media',
    false,
    104857600,
    array[
        'video/mp4',
        'video/webm',
        'video/quicktime',
        'image/jpeg',
        'image/png',
        'image/webp',
        'image/gif'
    ]
)
on conflict (id) do update
set
    public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "rs_training_storage_staff_insert" on storage.objects;
create policy "rs_training_storage_staff_insert"
on storage.objects
for insert
to authenticated
with check (
    bucket_id='rs-training-media'
    and (select private.rs_is_staff())
);

drop policy if exists "rs_training_storage_visible_select" on storage.objects;
create policy "rs_training_storage_visible_select"
on storage.objects
for select
to authenticated
using (
    bucket_id='rs-training-media'
    and (
        (select private.rs_is_staff())
        or exists(
            select 1
            from public.rs_training_media m
            join public.rs_profiles p on p.id=(select auth.uid())
            where m.media_path=name
              and m.published=true
              and p.active=true
              and p.role='student'
              and (
                  m.access_tier='ALL'
                  or m.access_tier='BASIC'
                  or (m.access_tier='PRO' and p.plan in ('PRO','ELITE'))
                  or (m.access_tier='ELITE' and p.plan='ELITE')
              )
        )
    )
);

drop policy if exists "rs_training_storage_staff_update" on storage.objects;
create policy "rs_training_storage_staff_update"
on storage.objects
for update
to authenticated
using (
    bucket_id='rs-training-media'
    and (select private.rs_is_staff())
)
with check (
    bucket_id='rs-training-media'
    and (select private.rs_is_staff())
);

drop policy if exists "rs_training_storage_staff_delete" on storage.objects;
create policy "rs_training_storage_staff_delete"
on storage.objects
for delete
to authenticated
using (
    bucket_id='rs-training-media'
    and (select private.rs_is_staff())
);

create or replace function public.rs_staff_create_training_media(
    p_title text,
    p_category text,
    p_description text,
    p_media_path text,
    p_media_kind text,
    p_access_tier text,
    p_published boolean
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if trim(p_title)='' then
        raise exception 'title required' using errcode='22023';
    end if;
    if p_media_kind not in ('VIDEO','IMAGE','GIF') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;
    if p_access_tier not in ('ALL','BASIC','PRO','ELITE') then
        raise exception 'invalid access tier' using errcode='22023';
    end if;
    if trim(p_media_path)='' then
        raise exception 'media path required' using errcode='22023';
    end if;

    insert into public.rs_training_media(
        title,
        category,
        description,
        media_path,
        media_kind,
        access_tier,
        published,
        created_by
    )
    values(
        trim(p_title),
        coalesce(nullif(trim(p_category),''),'TECHNIQUE'),
        coalesce(p_description,''),
        trim(p_media_path),
        p_media_kind,
        p_access_tier,
        p_published,
        (select auth.uid())
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_training_media(text,text,text,text,text,text,boolean) from public;
revoke execute on function public.rs_staff_create_training_media(text,text,text,text,text,text,boolean) from anon;
grant execute on function public.rs_staff_create_training_media(text,text,text,text,text,text,boolean) to authenticated;

create or replace function public.rs_staff_set_training_media_published(
    p_media_id uuid,
    p_published boolean
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

    update public.rs_training_media
    set published=p_published,
        updated_at=now()
    where id=p_media_id;

    if not found then
        raise exception 'training media not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_training_media_published(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_training_media_published(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_training_media_published(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_training_media(p_media_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_path text;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_training_media
    where id=p_media_id
    returning media_path into v_path;

    if v_path is null then
        raise exception 'training media not found' using errcode='P0002';
    end if;

    return v_path;
end;
$$;

revoke execute on function public.rs_staff_delete_training_media(uuid) from public;
revoke execute on function public.rs_staff_delete_training_media(uuid) from anon;
grant execute on function public.rs_staff_delete_training_media(uuid) to authenticated;

comment on policy "rs_training_storage_visible_select" on storage.objects is
'Allows private training-media object reads only to staff or students whose active plan permits the linked published catalog item.';
