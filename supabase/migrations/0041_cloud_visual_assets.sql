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
