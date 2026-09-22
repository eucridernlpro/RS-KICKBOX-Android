-- RS KICKBOXING v0.163 - persistent chat gallery + seven day temporary media cleanup
-- Safe to run repeatedly. Run after 0060.

create table if not exists public.rs_chat_gallery_items (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    gallery_path text not null,
    media_kind text not null,
    media_name text not null default '',
    source_path text not null default '',
    saved_at timestamptz not null default now()
);

alter table public.rs_chat_gallery_items enable row level security;
revoke all on table public.rs_chat_gallery_items from anon,authenticated;
grant select,insert,delete on table public.rs_chat_gallery_items to authenticated;

drop policy if exists "rs_chat_gallery_own_select" on public.rs_chat_gallery_items;
create policy "rs_chat_gallery_own_select"
on public.rs_chat_gallery_items
for select to authenticated
using(user_id=(select auth.uid()));

drop policy if exists "rs_chat_gallery_own_insert" on public.rs_chat_gallery_items;
create policy "rs_chat_gallery_own_insert"
on public.rs_chat_gallery_items
for insert to authenticated
with check(user_id=(select auth.uid()));

drop policy if exists "rs_chat_gallery_own_delete" on public.rs_chat_gallery_items;
create policy "rs_chat_gallery_own_delete"
on public.rs_chat_gallery_items
for delete to authenticated
using(user_id=(select auth.uid()));

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values(
    'rs-chat-gallery',
    'rs-chat-gallery',
    false,
    31457280,
    array[
        'image/jpeg','image/png','image/webp','image/gif',
        'video/mp4','video/webm','video/quicktime','video/3gpp',
        'audio/mp4','audio/mpeg','audio/ogg','audio/wav','audio/x-wav',
        'application/octet-stream'
    ]
)
on conflict(id) do update
set name=excluded.name,
    public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "rs_chat_gallery_storage_insert" on storage.objects;
create policy "rs_chat_gallery_storage_insert"
on storage.objects
for insert to authenticated
with check(
    bucket_id='rs-chat-gallery'
    and (storage.foldername(name))[1]=(select auth.uid())::text
);

drop policy if exists "rs_chat_gallery_storage_select" on storage.objects;
create policy "rs_chat_gallery_storage_select"
on storage.objects
for select to authenticated
using(
    bucket_id='rs-chat-gallery'
    and (storage.foldername(name))[1]=(select auth.uid())::text
);

drop policy if exists "rs_chat_gallery_storage_delete" on storage.objects;
create policy "rs_chat_gallery_storage_delete"
on storage.objects
for delete to authenticated
using(
    bucket_id='rs-chat-gallery'
    and (storage.foldername(name))[1]=(select auth.uid())::text
);

create or replace function public.rs_register_chat_gallery_item(
    p_gallery_path text,
    p_media_kind text,
    p_media_name text default '',
    p_source_path text default ''
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

    if p_gallery_path is null
       or p_gallery_path=''
       or split_part(p_gallery_path,'/',1)<>v_uid::text then
        raise exception 'invalid gallery path' using errcode='22023';
    end if;

    if p_media_kind not in ('IMAGE','VIDEO','AUDIO','FILE') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;

    insert into public.rs_chat_gallery_items(
        user_id,gallery_path,media_kind,media_name,source_path
    )
    values(
        v_uid,p_gallery_path,p_media_kind,coalesce(p_media_name,''),coalesce(p_source_path,'')
    )
    returning id into v_id;

    return v_id;
end;
$$;

create or replace function public.rs_chat_gallery_feed()
returns table(
    id uuid,
    gallery_path text,
    media_kind text,
    media_name text,
    source_path text,
    saved_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        g.id,g.gallery_path,g.media_kind,g.media_name,g.source_path,g.saved_at
    from public.rs_chat_gallery_items g
    where g.user_id=(select auth.uid())
    order by g.saved_at desc;
$$;

create or replace function public.rs_delete_chat_gallery_item(p_item_id uuid)
returns text
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_path text;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    delete from public.rs_chat_gallery_items
    where id=p_item_id
      and user_id=v_uid
    returning gallery_path into v_path;

    return coalesce(v_path,'');
end;
$$;

create or replace function public.rs_expired_chat_media_paths()
returns table(media_path text)
language sql
stable
security definer
set search_path=''
as $$
    with visible_paths as (
        select distinct m.media_path
        from public.rs_coach_messages m
        where m.media_path is not null
          and m.created_at < now()-interval '7 days'
          and (
              (select private.rs_is_staff())
              or m.student_id=(select auth.uid())
          )

        union

        select distinct m.media_path
        from public.rs_group_messages m
        where m.media_path is not null
          and m.created_at < now()-interval '7 days'
          and (
              (select private.rs_is_staff())
              or exists(
                  select 1
                  from public.rs_group_memberships gm
                  where gm.group_id=m.group_id
                    and gm.student_id=(select auth.uid())
              )
          )

        union

        select distinct c.media_path
        from public.rs_community_posts c
        where c.media_path is not null
          and c.created_at < now()-interval '7 days'
          and (
              c.author_id=(select auth.uid())
              or (select private.rs_is_staff())
          )

        union

        select distinct t.media_path
        from public.rs_support_tickets t
        where t.media_path is not null
          and t.created_at < now()-interval '7 days'
          and (
              t.student_id=(select auth.uid())
              or (select private.rs_is_staff())
          )
    )
    select v.media_path
    from visible_paths v
    where v.media_path is not null and v.media_path<>'';
$$;

create or replace function public.rs_clear_expired_chat_media_path(p_media_path text)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_allowed boolean:=false;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if (select private.rs_is_staff()) then
        v_allowed:=true;
    else
        v_allowed:=exists(
            select 1
            from public.rs_coach_messages m
            where m.media_path=p_media_path
              and m.student_id=v_uid
              and m.created_at < now()-interval '7 days'
        )
        or exists(
            select 1
            from public.rs_group_messages m
            join public.rs_group_memberships gm
              on gm.group_id=m.group_id
             and gm.student_id=v_uid
            where m.media_path=p_media_path
              and m.created_at < now()-interval '7 days'
        )
        or exists(
            select 1
            from public.rs_community_posts cp
            where cp.media_path=p_media_path
              and cp.author_id=v_uid
              and cp.created_at < now()-interval '7 days'
        )
        or exists(
            select 1
            from public.rs_support_tickets st
            where st.media_path=p_media_path
              and st.student_id=v_uid
              and st.created_at < now()-interval '7 days'
        );
    end if;

    if not v_allowed then
        raise exception 'media cleanup access denied' using errcode='42501';
    end if;

    update public.rs_coach_messages
    set media_path=null,media_kind=null,media_name=null
    where media_path=p_media_path
      and created_at < now()-interval '7 days';

    update public.rs_group_messages
    set media_path=null,media_kind=null,media_name=null
    where media_path=p_media_path
      and created_at < now()-interval '7 days';

    update public.rs_community_posts
    set media_path=null,media_kind=null,media_name=null
    where media_path=p_media_path
      and created_at < now()-interval '7 days';

    update public.rs_support_tickets
    set media_path=null,media_kind=null,media_name=null
    where media_path=p_media_path
      and created_at < now()-interval '7 days';
end;
$$;

revoke all on function public.rs_register_chat_gallery_item(text,text,text,text) from public,anon;
revoke all on function public.rs_chat_gallery_feed() from public,anon;
revoke all on function public.rs_delete_chat_gallery_item(uuid) from public,anon;
revoke all on function public.rs_expired_chat_media_paths() from public,anon;
revoke all on function public.rs_clear_expired_chat_media_path(text) from public,anon;

grant execute on function public.rs_register_chat_gallery_item(text,text,text,text) to authenticated;
grant execute on function public.rs_chat_gallery_feed() to authenticated;
grant execute on function public.rs_delete_chat_gallery_item(uuid) to authenticated;
grant execute on function public.rs_expired_chat_media_paths() to authenticated;
grant execute on function public.rs_clear_expired_chat_media_path(text) to authenticated;


-- Community and Support become first-class RS CHAT surfaces with the same media pipeline.
alter table public.rs_community_posts
    add column if not exists media_path text,
    add column if not exists media_kind text,
    add column if not exists media_name text;

alter table public.rs_support_tickets
    add column if not exists media_path text,
    add column if not exists media_kind text,
    add column if not exists media_name text;

create or replace function public.rs_community_feed_v2()
returns table(
    id uuid,
    author_id uuid,
    author_email text,
    author_name text,
    body text,
    active boolean,
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
        c.id,c.author_id,p.email,
        coalesce(s.display_name,p.display_name),
        c.body,c.active,
        c.media_path,c.media_kind,c.media_name,
        c.created_at
    from public.rs_community_posts c
    join public.rs_profiles p on p.id=c.author_id
    left join public.rs_social_profiles s on s.user_id=c.author_id
    where c.active=true
       or c.author_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by c.created_at asc;
$$;

create or replace function public.rs_create_community_post_v2(
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
    v_enabled boolean;
    v_body text:=trim(coalesce(p_body,''));
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if v_body='' and p_media_path is null then
        raise exception 'message or attachment required' using errcode='22023';
    end if;
    if length(v_body)>1000 then
        raise exception 'community message too long' using errcode='22023';
    end if;

    select s.community_posts_enabled into v_enabled
    from public.rs_app_settings s where s.singleton=true;
    if coalesce(v_enabled,true)=false then
        raise exception 'community posting disabled' using errcode='42501';
    end if;

    if p_media_path is not null
       and p_media_path not like ('community/'||v_uid::text||'/%') then
        raise exception 'invalid community media path' using errcode='22023';
    end if;

    insert into public.rs_community_posts(
        author_id,body,active,media_path,media_kind,media_name
    )
    values(
        v_uid,v_body,true,p_media_path,p_media_kind,p_media_name
    )
    returning id into v_id;

    return v_id;
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
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        t.id,t.student_id,p.email,p.display_name,
        t.subject,t.message,t.trainer_reply,t.status,
        t.media_path,t.media_kind,t.media_name,t.created_at
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
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if not exists(
        select 1 from public.rs_profiles p
        where p.id=v_uid and p.role='student' and p.active=true
    ) then
        raise exception 'active student account required' using errcode='42501';
    end if;
    if v_message='' and p_media_path is null then
        raise exception 'message or attachment required' using errcode='22023';
    end if;
    if p_media_path is not null
       and p_media_path not like ('support/'||v_uid::text||'/%') then
        raise exception 'invalid support media path' using errcode='22023';
    end if;

    insert into public.rs_support_tickets(
        student_id,subject,message,media_path,media_kind,media_name
    )
    values(
        v_uid,left(coalesce(nullif(trim(p_subject),''),'RS Support'),120),
        v_message,p_media_path,p_media_kind,p_media_name
    )
    returning id into v_id;

    return v_id;
end;
$$;

-- Expand the existing private chat-media bucket permissions to Community and Support.
update storage.buckets
set file_size_limit=31457280,
    allowed_mime_types=array[
        'image/jpeg','image/png','image/webp','image/gif',
        'video/mp4','video/webm','video/quicktime','video/3gpp',
        'audio/mp4','audio/mpeg','audio/ogg','audio/wav','audio/x-wav',
        'application/pdf','text/plain','application/octet-stream'
    ]
where id='rs-chat-media';

drop policy if exists "rs_chat_media_insert_authorized" on storage.objects;
create policy "rs_chat_media_insert_authorized"
on storage.objects
for insert to authenticated
with check(
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
                    select 1 from public.rs_group_memberships gm
                    where gm.group_id::text=(storage.foldername(name))[2]
                      and gm.student_id=(select auth.uid())
                )
            )
        )
        or
        (
            (storage.foldername(name))[1]='community'
            and (storage.foldername(name))[2]=(select auth.uid())::text
        )
        or
        (
            (storage.foldername(name))[1]='support'
            and (
                (storage.foldername(name))[2]=(select auth.uid())::text
                or (select private.rs_is_staff())
            )
        )
    )
);

drop policy if exists "rs_chat_media_select_authorized" on storage.objects;
create policy "rs_chat_media_select_authorized"
on storage.objects
for select to authenticated
using(
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
                    select 1 from public.rs_group_memberships gm
                    where gm.group_id::text=(storage.foldername(name))[2]
                      and gm.student_id=(select auth.uid())
                )
            )
        )
        or (storage.foldername(name))[1]='community'
        or
        (
            (storage.foldername(name))[1]='support'
            and (
                (storage.foldername(name))[2]=(select auth.uid())::text
                or (select private.rs_is_staff())
            )
        )
    )
);

drop policy if exists "rs_chat_media_delete_authorized" on storage.objects;
create policy "rs_chat_media_delete_authorized"
on storage.objects
for delete to authenticated
using(
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
                select 1 from public.rs_group_memberships gm
                where gm.group_id::text=(storage.foldername(name))[2]
                  and gm.student_id=(select auth.uid())
            )
        )
        or (
            (storage.foldername(name))[1]='community'
            and (storage.foldername(name))[2]=(select auth.uid())::text
        )
        or (
            (storage.foldername(name))[1]='support'
            and (storage.foldername(name))[2]=(select auth.uid())::text
        )
    )
);

revoke all on function public.rs_community_feed_v2() from public,anon;
revoke all on function public.rs_create_community_post_v2(text,text,text,text) from public,anon;
revoke all on function public.rs_support_feed_v2() from public,anon;
revoke all on function public.rs_create_support_ticket_v2(text,text,text,text,text) from public,anon;

grant execute on function public.rs_community_feed_v2() to authenticated;
grant execute on function public.rs_create_community_post_v2(text,text,text,text) to authenticated;
grant execute on function public.rs_support_feed_v2() to authenticated;
grant execute on function public.rs_create_support_ticket_v2(text,text,text,text,text) to authenticated;
