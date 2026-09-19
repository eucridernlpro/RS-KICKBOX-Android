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
    can_read_full boolean
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
        ) as can_read_full
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
