-- RS KICKBOXING
-- Migration 0065: Community + Support media transport repair.
-- Safe to run repeatedly. Keeps existing coach/group media access intact.

alter table if exists public.rs_community_posts
    add column if not exists media_path text,
    add column if not exists media_kind text,
    add column if not exists media_name text;

alter table if exists public.rs_support_tickets
    add column if not exists media_path text,
    add column if not exists media_kind text,
    add column if not exists media_name text,
    add column if not exists trainer_media_path text,
    add column if not exists trainer_media_kind text,
    add column if not exists trainer_media_name text;

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values(
    'rs-chat-media','rs-chat-media',false,31457280,
    array[
        'image/jpeg','image/png','image/webp','image/gif',
        'video/mp4','video/webm','video/quicktime','video/3gpp',
        'audio/mp4','audio/mpeg','audio/ogg','audio/wav','audio/x-wav','audio/x-m4a','audio/aac',
        'application/pdf','text/plain','application/octet-stream',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    ]
)
on conflict(id) do update
set public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "rs_chat_media_insert_authorized" on storage.objects;
create policy "rs_chat_media_insert_authorized"
on storage.objects for insert to authenticated
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
        or (
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
        or (
            (storage.foldername(name))[1]='community'
            and (storage.foldername(name))[2]=(select auth.uid())::text
        )
        or (
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
on storage.objects for select to authenticated
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
        or (
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
        or (
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
on storage.objects for delete to authenticated
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

drop function if exists public.rs_community_feed_v2();
create function public.rs_community_feed_v2()
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
language sql stable security definer set search_path=''
as $$
    select
        c.id,c.author_id,p.email,
        coalesce(s.display_name,p.display_name),
        c.body,c.active,c.media_path,c.media_kind,c.media_name,c.created_at
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
language plpgsql security definer set search_path=''
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
    if p_media_kind is not null and p_media_kind not in ('IMAGE','VIDEO','AUDIO','FILE') then
        raise exception 'invalid media kind' using errcode='22023';
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

    insert into public.rs_community_posts(author_id,body,active,media_path,media_kind,media_name)
    values(v_uid,v_body,true,p_media_path,p_media_kind,p_media_name)
    returning id into v_id;
    return v_id;
end;
$$;

drop function if exists public.rs_support_feed_v2();
create function public.rs_support_feed_v2()
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
    trainer_media_path text,
    trainer_media_kind text,
    trainer_media_name text,
    created_at timestamptz
)
language sql stable security definer set search_path=''
as $$
    select
        t.id,t.student_id,p.email,p.display_name,
        t.subject,t.message,coalesce(t.trainer_reply,''),t.status,
        t.media_path,t.media_kind,t.media_name,
        t.trainer_media_path,t.trainer_media_kind,t.trainer_media_name,
        t.created_at
    from public.rs_support_tickets t
    join public.rs_profiles p on p.id=t.student_id
    where t.student_id=(select auth.uid()) or (select private.rs_is_staff())
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
language plpgsql security definer set search_path=''
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
    if p_media_kind is not null and p_media_kind not in ('IMAGE','VIDEO','AUDIO','FILE') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;
    if p_media_path is not null
       and p_media_path not like ('support/'||v_uid::text||'/%') then
        raise exception 'invalid support media path' using errcode='22023';
    end if;

    insert into public.rs_support_tickets(student_id,subject,message,media_path,media_kind,media_name)
    values(v_uid,left(coalesce(nullif(trim(p_subject),''),'RS Support'),120),
           v_message,p_media_path,p_media_kind,p_media_name)
    returning id into v_id;
    return v_id;
end;
$$;

revoke all on function public.rs_community_feed_v2() from public,anon;
revoke all on function public.rs_create_community_post_v2(text,text,text,text) from public,anon;
revoke all on function public.rs_support_feed_v2() from public,anon;
revoke all on function public.rs_create_support_ticket_v2(text,text,text,text,text) from public,anon;

grant execute on function public.rs_community_feed_v2() to authenticated;
grant execute on function public.rs_create_community_post_v2(text,text,text,text) to authenticated;
grant execute on function public.rs_support_feed_v2() to authenticated;
grant execute on function public.rs_create_support_ticket_v2(text,text,text,text,text) to authenticated;

notify pgrst, 'reload schema';
