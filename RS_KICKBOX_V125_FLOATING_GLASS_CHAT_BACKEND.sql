-- RS KICKBOXING v0.125 Floating Glass Chat foundation
-- Presence + chat contacts + common file attachments. Safe to run more than once.

alter table public.rs_profiles
    add column if not exists last_seen_at timestamptz;

create index if not exists rs_profiles_last_seen_idx
on public.rs_profiles(last_seen_at)
where active=true;

create or replace function public.rs_touch_presence()
returns void
language plpgsql
security definer
set search_path=''
as $presence$
begin
    if (select auth.uid()) is null then
        raise exception 'authentication required' using errcode='42501';
    end if;
    update public.rs_profiles
    set last_seen_at=now(), updated_at=now()
    where id=(select auth.uid()) and active=true;
end;
$presence$;

create or replace function public.rs_chat_contacts()
returns table(
    user_id uuid,
    email text,
    display_name text,
    avatar_path text,
    role text,
    online boolean,
    last_seen_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $contacts$
    with me as (
        select p.role
        from public.rs_profiles p
        where p.id=(select auth.uid()) and p.active=true
        limit 1
    )
    select
        p.id,
        p.email,
        p.display_name,
        p.avatar_path,
        p.role,
        coalesce(p.last_seen_at > now()-interval '2 minutes',false) as online,
        p.last_seen_at
    from public.rs_profiles p, me
    where p.active=true
      and (
          (me.role in ('trainer','admin') and p.role='student')
          or
          (me.role='student' and p.role in ('trainer','admin'))
      )
    order by
        coalesce(p.last_seen_at > now()-interval '2 minutes',false) desc,
        lower(p.display_name),
        lower(p.email);
$contacts$;

revoke execute on function public.rs_touch_presence() from public,anon;
revoke execute on function public.rs_chat_contacts() from public,anon;
grant execute on function public.rs_touch_presence() to authenticated;
grant execute on function public.rs_chat_contacts() to authenticated;

-- Extend the already-live chat attachment contract to common files.
alter table public.rs_coach_messages
    drop constraint if exists rs_coach_messages_media_kind_check;
alter table public.rs_coach_messages
    add constraint rs_coach_messages_media_kind_check
    check (media_kind is null or media_kind in ('IMAGE','VIDEO','AUDIO','FILE'));

alter table public.rs_group_messages
    drop constraint if exists rs_group_messages_media_kind_check;
alter table public.rs_group_messages
    add constraint rs_group_messages_media_kind_check
    check (media_kind is null or media_kind in ('IMAGE','VIDEO','AUDIO','FILE'));

update storage.buckets
set file_size_limit=31457280,
    allowed_mime_types=array[
        'image/jpeg','image/png','image/webp','image/gif',
        'video/mp4','video/webm','video/quicktime','video/3gpp',
        'audio/mp4','audio/mpeg','audio/ogg','audio/wav','audio/x-m4a','audio/aac',
        'application/pdf','text/plain',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    ]
where id='rs-chat-media';

-- Recreate send functions so FILE is accepted.
create or replace function public.rs_send_coach_message_v2(
    p_student_id uuid,
    p_body text default '',
    p_media_path text default null,
    p_media_kind text default null,
    p_media_name text default null
)
returns uuid
language plpgsql security definer set search_path=''
as $coach_send$
declare
    v_uid uuid:=(select auth.uid());
    v_role text;
    v_id uuid;
    v_body text:=trim(coalesce(p_body,''));
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if length(v_body)>1200 then raise exception 'message too long' using errcode='22023'; end if;
    if v_body='' and p_media_path is null then raise exception 'message or attachment required' using errcode='22023'; end if;
    if p_media_kind is not null and p_media_kind not in ('IMAGE','VIDEO','AUDIO','FILE') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;
    if p_media_path is not null and p_media_path not like ('coach/'||p_student_id::text||'/%') then
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

    insert into public.rs_coach_messages(
        student_id,sender_id,sender_role,body,media_path,media_kind,media_name
    )
    values(
        p_student_id,v_uid,
        case when v_role='student' then 'student' else 'trainer' end,
        v_body,p_media_path,p_media_kind,p_media_name
    )
    returning id into v_id;
    return v_id;
end;
$coach_send$;

create or replace function public.rs_send_group_message(
    p_group_id uuid,
    p_body text default '',
    p_media_path text default null,
    p_media_kind text default null,
    p_media_name text default null
)
returns uuid
language plpgsql security definer set search_path=''
as $group_send$
declare
    v_uid uuid:=(select auth.uid());
    v_id uuid;
    v_body text:=trim(coalesce(p_body,''));
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if length(v_body)>1200 then raise exception 'message too long' using errcode='22023'; end if;
    if v_body='' and p_media_path is null then raise exception 'message or attachment required' using errcode='22023'; end if;
    if p_media_kind is not null and p_media_kind not in ('IMAGE','VIDEO','AUDIO','FILE') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;
    if p_media_path is not null and p_media_path not like ('group/'||p_group_id::text||'/%') then
        raise exception 'invalid group media path' using errcode='22023';
    end if;

    if not (
        (select private.rs_is_staff())
        or exists(
            select 1 from public.rs_group_memberships gm
            where gm.group_id=p_group_id and gm.student_id=v_uid
        )
    ) then
        raise exception 'group membership required' using errcode='42501';
    end if;

    insert into public.rs_group_messages(
        group_id,sender_id,body,media_path,media_kind,media_name
    )
    values(p_group_id,v_uid,v_body,p_media_path,p_media_kind,p_media_name)
    returning id into v_id;
    return v_id;
end;
$group_send$;

grant execute on function public.rs_send_coach_message_v2(uuid,text,text,text,text) to authenticated;
grant execute on function public.rs_send_group_message(uuid,text,text,text,text) to authenticated;
