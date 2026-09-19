-- RS KICKBOX v0.105 BACKEND UPDATE
-- Run once AFTER the successful RS_KICKBOX_V098_BACKEND_UPDATE.sql.
-- This delta intentionally contains only migrations 0040-0043:
-- global brand settings, shared visual assets, shared intro/splash settings,
-- and cloud promotions/book management.
-- Generated for the v0.105 Android release line.



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


-- ============================================================
-- 0042_cloud_intro_settings.sql
-- ============================================================

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


-- ============================================================
-- 0043_cloud_promotions_books.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0043: cloud promotions and secure trainer book delivery.

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values(
    'rs-promotions',
    'rs-promotions',
    true,
    10485760,
    array['image/jpeg','image/png','image/webp']
)
on conflict(id) do update
set public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values(
    'rs-books',
    'rs-books',
    false,
    52428800,
    array['image/jpeg','image/png','image/webp','application/pdf']
)
on conflict(id) do update
set public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "rs_promotions_media_staff_insert" on storage.objects;
create policy "rs_promotions_media_staff_insert"
on storage.objects for insert to authenticated
with check(bucket_id='rs-promotions' and (select private.rs_is_staff()));

drop policy if exists "rs_promotions_media_staff_update" on storage.objects;
create policy "rs_promotions_media_staff_update"
on storage.objects for update to authenticated
using(bucket_id='rs-promotions' and (select private.rs_is_staff()))
with check(bucket_id='rs-promotions' and (select private.rs_is_staff()));

drop policy if exists "rs_promotions_media_staff_delete" on storage.objects;
create policy "rs_promotions_media_staff_delete"
on storage.objects for delete to authenticated
using(bucket_id='rs-promotions' and (select private.rs_is_staff()));

drop policy if exists "rs_books_media_staff_insert" on storage.objects;
create policy "rs_books_media_staff_insert"
on storage.objects for insert to authenticated
with check(bucket_id='rs-books' and (select private.rs_is_staff()));

drop policy if exists "rs_books_media_staff_update" on storage.objects;
create policy "rs_books_media_staff_update"
on storage.objects for update to authenticated
using(bucket_id='rs-books' and (select private.rs_is_staff()))
with check(bucket_id='rs-books' and (select private.rs_is_staff()));

drop policy if exists "rs_books_media_staff_delete" on storage.objects;
create policy "rs_books_media_staff_delete"
on storage.objects for delete to authenticated
using(bucket_id='rs-books' and (select private.rs_is_staff()));

drop policy if exists "rs_books_media_member_select" on storage.objects;
create policy "rs_books_media_member_select"
on storage.objects for select to authenticated
using(
    bucket_id='rs-books'
    and (
        (select private.rs_is_staff())
        or name like 'cover/%'
        or name like 'preview/%'
        or (
            name like 'full/%'
            and exists(
                select 1
                from public.rs_books b
                join public.rs_profiles p on p.id=(select auth.uid())
                where b.active=true
                  and b.full_path=name
                  and p.active=true
                  and p.role='student'
                  and (
                    b.access_tier='ALL'
                    or b.access_tier='BASIC'
                    or (b.access_tier='PRO' and p.plan in ('PRO','ELITE'))
                    or (b.access_tier='ELITE' and p.plan='ELITE')
                    or exists(
                        select 1 from public.rs_book_grants g
                        where g.book_id=b.id and g.student_id=p.id
                    )
                  )
            )
        )
    )
);

create or replace function public.rs_promotion_feed()
returns table(
    id uuid,
    title text,
    image_path text,
    external_url text,
    active boolean,
    sort_order integer
)
language sql
stable
security definer
set search_path=''
as $$
    select p.id,p.title,p.image_path,p.external_url,p.active,p.sort_order
    from public.rs_promotions p
    where p.active=true or (select private.rs_is_staff())
    order by p.sort_order,p.created_at desc;
$$;

revoke execute on function public.rs_promotion_feed() from public;
revoke execute on function public.rs_promotion_feed() from anon;
grant execute on function public.rs_promotion_feed() to authenticated;

create or replace function public.rs_staff_create_promotion(
    p_title text,
    p_image_path text,
    p_external_url text
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
    if trim(p_title)='' or trim(p_image_path)='' or trim(p_external_url)='' then
        raise exception 'promotion title image and URL required' using errcode='22023';
    end if;
    insert into public.rs_promotions(title,image_path,external_url,active,created_by)
    values(left(trim(p_title),120),trim(p_image_path),trim(p_external_url),true,(select auth.uid()))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_promotion(text,text,text) from public;
revoke execute on function public.rs_staff_create_promotion(text,text,text) from anon;
grant execute on function public.rs_staff_create_promotion(text,text,text) to authenticated;

create or replace function public.rs_staff_set_promotion_active(
    p_promotion_id uuid,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    update public.rs_promotions
    set active=p_active,updated_at=now()
    where id=p_promotion_id;
end;
$$;

revoke execute on function public.rs_staff_set_promotion_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_promotion_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_promotion_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_promotion(p_promotion_id uuid)
returns text
language plpgsql
security definer
set search_path=''
as $$
declare v_path text;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_promotions
    where id=p_promotion_id
    returning image_path into v_path;
    return v_path;
end;
$$;

revoke execute on function public.rs_staff_delete_promotion(uuid) from public;
revoke execute on function public.rs_staff_delete_promotion(uuid) from anon;
grant execute on function public.rs_staff_delete_promotion(uuid) to authenticated;

create or replace function public.rs_book_feed()
returns table(
    id uuid,
    title text,
    cover_path text,
    amazon_url text,
    preview_path text,
    full_path text,
    access_tier text,
    can_read_full boolean,
    gifted_emails text[]
)
language sql
stable
security definer
set search_path=''
as $$
    select
        b.id,
        b.title,
        b.cover_path,
        b.amazon_url,
        b.preview_path,
        case
            when (select private.rs_is_staff()) then b.full_path
            when exists(
                select 1
                from public.rs_profiles p
                where p.id=(select auth.uid())
                  and p.active=true
                  and (
                    b.access_tier='ALL'
                    or b.access_tier='BASIC'
                    or (b.access_tier='PRO' and p.plan in ('PRO','ELITE'))
                    or (b.access_tier='ELITE' and p.plan='ELITE')
                    or exists(
                        select 1 from public.rs_book_grants g
                        where g.book_id=b.id and g.student_id=p.id
                    )
                  )
            ) then b.full_path
            else null
        end as full_path,
        b.access_tier,
        (
            (select private.rs_is_staff())
            or exists(
                select 1
                from public.rs_profiles p
                where p.id=(select auth.uid())
                  and p.active=true
                  and (
                    b.access_tier='ALL'
                    or b.access_tier='BASIC'
                    or (b.access_tier='PRO' and p.plan in ('PRO','ELITE'))
                    or (b.access_tier='ELITE' and p.plan='ELITE')
                    or exists(
                        select 1 from public.rs_book_grants g
                        where g.book_id=b.id and g.student_id=p.id
                    )
                  )
            )
        ) as can_read_full,
        case
            when (select private.rs_is_staff()) then coalesce(
                (
                    select array_agg(p.email order by lower(p.email))
                    from public.rs_book_grants g
                    join public.rs_profiles p on p.id=g.student_id
                    where g.book_id=b.id
                ),
                array[]::text[]
            )
            else array[]::text[]
        end as gifted_emails
    from public.rs_books b
    where b.active=true or (select private.rs_is_staff())
    order by b.updated_at desc
    limit 1;
$$;

revoke execute on function public.rs_book_feed() from public;
revoke execute on function public.rs_book_feed() from anon;
grant execute on function public.rs_book_feed() to authenticated;

create or replace function public.rs_staff_save_book(
    p_book_id uuid,
    p_title text,
    p_cover_path text,
    p_amazon_url text,
    p_preview_path text,
    p_full_path text,
    p_access_tier text,
    p_gifted_emails text[]
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare
    v_id uuid;
    v_email text;
    v_student uuid;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    if p_access_tier not in ('ALL','BASIC','PRO','ELITE','PRIVATE') then raise exception 'invalid access tier' using errcode='22023'; end if;

    if p_book_id is null then
        update public.rs_books set active=false,updated_at=now() where active=true;
        insert into public.rs_books(
            title,cover_path,amazon_url,preview_path,full_path,access_tier,active,created_by
        ) values(
            left(coalesce(nullif(trim(p_title),''),'Trainer Book'),160),
            nullif(trim(p_cover_path),''),
            nullif(trim(p_amazon_url),''),
            nullif(trim(p_preview_path),''),
            nullif(trim(p_full_path),''),
            p_access_tier,true,(select auth.uid())
        ) returning id into v_id;
    else
        update public.rs_books
        set title=left(coalesce(nullif(trim(p_title),''),'Trainer Book'),160),
            cover_path=nullif(trim(p_cover_path),''),
            amazon_url=nullif(trim(p_amazon_url),''),
            preview_path=nullif(trim(p_preview_path),''),
            full_path=nullif(trim(p_full_path),''),
            access_tier=p_access_tier,
            active=true,
            updated_at=now()
        where id=p_book_id
        returning id into v_id;
    end if;

    delete from public.rs_book_grants where book_id=v_id;

    foreach v_email in array coalesce(p_gifted_emails,array[]::text[])
    loop
        select p.id into v_student
        from public.rs_profiles p
        where lower(p.email)=lower(trim(v_email))
          and p.role='student'
          and p.active=true
        limit 1;

        if v_student is not null then
            insert into public.rs_book_grants(book_id,student_id,granted_by,reason)
            values(v_id,v_student,(select auth.uid()),'manual')
            on conflict(book_id,student_id) do nothing;
        end if;
        v_student:=null;
    end loop;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_save_book(uuid,text,text,text,text,text,text,text[]) from public;
revoke execute on function public.rs_staff_save_book(uuid,text,text,text,text,text,text,text[]) from anon;
grant execute on function public.rs_staff_save_book(uuid,text,text,text,text,text,text,text[]) to authenticated;
