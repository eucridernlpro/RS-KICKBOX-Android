-- RS KICKBOXING v0.144 - professional groups + moderation

alter table public.rs_groups
    add column if not exists thumbnail_path text,
    add column if not exists students_can_post boolean not null default true,
    add column if not exists students_can_media boolean not null default true,
    add column if not exists open_join boolean not null default true;

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values(
    'rs-group-artwork','rs-group-artwork',true,6291456,
    array['image/jpeg','image/png','image/webp']
)
on conflict(id) do update
set public=true,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "rs_group_artwork_staff_insert" on storage.objects;
create policy "rs_group_artwork_staff_insert"
on storage.objects for insert to authenticated
with check(bucket_id='rs-group-artwork' and (select private.rs_is_staff()));

drop policy if exists "rs_group_artwork_read" on storage.objects;
create policy "rs_group_artwork_read"
on storage.objects for select to authenticated
using(bucket_id='rs-group-artwork');

drop policy if exists "rs_group_artwork_staff_delete" on storage.objects;
create policy "rs_group_artwork_staff_delete"
on storage.objects for delete to authenticated
using(bucket_id='rs-group-artwork' and (select private.rs_is_staff()));

drop function if exists public.rs_group_catalog();
create function public.rs_group_catalog()
returns table(
    id uuid,
    name text,
    description text,
    active boolean,
    joined boolean,
    member_count integer,
    online_count integer,
    offline_count integer,
    thumbnail_path text,
    students_can_post boolean,
    students_can_media boolean,
    open_join boolean
)
language sql stable security definer set search_path=''
as $$
    select
        g.id,g.name,g.description,g.active,
        exists(
            select 1 from public.rs_group_memberships m
            where m.group_id=g.id and m.student_id=(select auth.uid())
        ),
        (select count(*)::integer from public.rs_group_memberships m2 where m2.group_id=g.id),
        (
            select count(*)::integer
            from public.rs_group_memberships m3
            join public.rs_profiles p3 on p3.id=m3.student_id
            where m3.group_id=g.id
              and p3.last_seen_at>now()-interval '2 minutes'
        ),
        (
            select count(*)::integer
            from public.rs_group_memberships m4
            join public.rs_profiles p4 on p4.id=m4.student_id
            where m4.group_id=g.id
              and not coalesce(p4.last_seen_at>now()-interval '2 minutes',false)
        ),
        g.thumbnail_path,
        g.students_can_post,
        g.students_can_media,
        g.open_join
    from public.rs_groups g
    where g.active=true or (select private.rs_is_staff())
    order by g.created_at desc;
$$;

revoke execute on function public.rs_group_catalog() from public,anon;
grant execute on function public.rs_group_catalog() to authenticated;

create or replace function public.rs_staff_create_group_v2(
    p_name text,
    p_description text,
    p_thumbnail_path text default '',
    p_students_can_post boolean default true,
    p_students_can_media boolean default true,
    p_open_join boolean default true
)
returns uuid
language plpgsql security definer set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    insert into public.rs_groups(
        name,description,active,created_by,thumbnail_path,
        students_can_post,students_can_media,open_join
    )
    values(
        left(trim(p_name),80),
        left(coalesce(p_description,''),500),
        true,(select auth.uid()),
        nullif(trim(coalesce(p_thumbnail_path,'')),''),
        coalesce(p_students_can_post,true),
        coalesce(p_students_can_media,true),
        coalesce(p_open_join,true)
    )
    returning id into v_id;
    return v_id;
end;
$$;

create or replace function public.rs_staff_update_group_v2(
    p_group_id uuid,
    p_name text,
    p_description text,
    p_thumbnail_path text default '',
    p_students_can_post boolean default true,
    p_students_can_media boolean default true,
    p_open_join boolean default true
)
returns void
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    update public.rs_groups
    set name=left(trim(p_name),80),
        description=left(coalesce(p_description,''),500),
        thumbnail_path=nullif(trim(coalesce(p_thumbnail_path,'')),''),
        students_can_post=coalesce(p_students_can_post,true),
        students_can_media=coalesce(p_students_can_media,true),
        open_join=coalesce(p_open_join,true)
    where id=p_group_id;
end;
$$;

create or replace function public.rs_staff_group_members(p_group_id uuid)
returns table(
    user_id uuid,
    display_name text,
    email text,
    avatar_path text,
    online boolean,
    joined_at timestamptz
)
language sql stable security definer set search_path=''
as $$
    select p.id,p.display_name,p.email,p.avatar_path,
           coalesce(p.last_seen_at>now()-interval '2 minutes',false),
           gm.joined_at
    from public.rs_group_memberships gm
    join public.rs_profiles p on p.id=gm.student_id
    where gm.group_id=p_group_id
      and (select private.rs_is_staff())
    order by coalesce(nullif(p.display_name,''),p.email);
$$;

create or replace function public.rs_staff_remove_group_member(
    p_group_id uuid,
    p_student_id uuid
)
returns void
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    delete from public.rs_group_memberships
    where group_id=p_group_id and student_id=p_student_id;
end;
$$;

create or replace function public.rs_set_my_group_membership(p_group_id uuid,p_join boolean)
returns void
language plpgsql security definer set search_path=''
as $$
declare v_uid uuid := (select auth.uid());
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;

    if p_join then
        if not exists(
            select 1 from public.rs_groups g
            where g.id=p_group_id and g.active=true and g.open_join=true
        ) then
            raise exception 'group unavailable or joining is closed' using errcode='42501';
        end if;
        insert into public.rs_group_memberships(group_id,student_id)
        values(p_group_id,v_uid)
        on conflict(group_id,student_id) do nothing;
    else
        delete from public.rs_group_memberships where group_id=p_group_id and student_id=v_uid;
    end if;
end;
$$;

-- Group message permissions are enforced on send.
create or replace function public.rs_send_group_message(
    p_group_id uuid,
    p_body text default '',
    p_media_path text default null,
    p_media_kind text default null,
    p_media_name text default null
)
returns uuid
language plpgsql security definer set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_id uuid;
    v_body text:=trim(coalesce(p_body,''));
    v_staff boolean:=(select private.rs_is_staff());
    v_can_post boolean;
    v_can_media boolean;
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if length(v_body)>1200 then raise exception 'message too long' using errcode='22023'; end if;

    if not (
        v_staff or exists(
            select 1 from public.rs_group_memberships gm
            where gm.group_id=p_group_id and gm.student_id=v_uid
        )
    ) then
        raise exception 'group membership required' using errcode='42501';
    end if;

    select students_can_post,students_can_media
      into v_can_post,v_can_media
    from public.rs_groups where id=p_group_id and active=true;

    if not v_staff then
        if v_body<>'' and not coalesce(v_can_post,false) then
            raise exception 'student messages are disabled for this group' using errcode='42501';
        end if;
        if p_media_path is not null and not coalesce(v_can_media,false) then
            raise exception 'student media is disabled for this group' using errcode='42501';
        end if;
    end if;

    if v_body='' and p_media_path is null then raise exception 'message or attachment required' using errcode='22023'; end if;
    if p_media_kind is not null and p_media_kind not in ('IMAGE','VIDEO','AUDIO') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;

    insert into public.rs_group_messages(group_id,sender_id,body,media_path,media_kind,media_name)
    values(p_group_id,v_uid,v_body,p_media_path,p_media_kind,p_media_name)
    returning id into v_id;
    return v_id;
end;
$$;

revoke all on function public.rs_staff_create_group_v2(text,text,text,boolean,boolean,boolean) from public,anon;
revoke all on function public.rs_staff_update_group_v2(uuid,text,text,text,boolean,boolean,boolean) from public,anon;
revoke all on function public.rs_staff_group_members(uuid) from public,anon;
revoke all on function public.rs_staff_remove_group_member(uuid,uuid) from public,anon;
grant execute on function public.rs_staff_create_group_v2(text,text,text,boolean,boolean,boolean) to authenticated;
grant execute on function public.rs_staff_update_group_v2(uuid,text,text,text,boolean,boolean,boolean) to authenticated;
grant execute on function public.rs_staff_group_members(uuid) to authenticated;
grant execute on function public.rs_staff_remove_group_member(uuid,uuid) to authenticated;
