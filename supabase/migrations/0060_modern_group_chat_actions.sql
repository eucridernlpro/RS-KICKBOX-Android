-- RS KICKBOXING v0.162 - modern group chat actions
-- Run after 0059. Safe to run repeatedly.

create table if not exists public.rs_group_message_hidden (
    user_id uuid not null references auth.users(id) on delete cascade,
    message_id uuid not null references public.rs_group_messages(id) on delete cascade,
    hidden_at timestamptz not null default now(),
    primary key(user_id,message_id)
);

alter table public.rs_group_message_hidden enable row level security;
drop policy if exists "rs_group_message_hidden_own" on public.rs_group_message_hidden;
create policy "rs_group_message_hidden_own"
on public.rs_group_message_hidden for all to authenticated
using(user_id=(select auth.uid()))
with check(user_id=(select auth.uid()));

alter table public.rs_group_messages
    add column if not exists reply_to uuid references public.rs_group_messages(id) on delete set null;

create or replace function public.rs_hide_group_message(p_message_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_group uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select m.group_id into v_group
    from public.rs_group_messages m
    where m.id=p_message_id;

    if v_group is null then
        raise exception 'message not found' using errcode='P0002';
    end if;

    if not (
        (select private.rs_is_staff())
        or exists(
            select 1
            from public.rs_group_memberships gm
            where gm.group_id=v_group
              and gm.student_id=v_uid
        )
    ) then
        raise exception 'group message access denied' using errcode='42501';
    end if;

    insert into public.rs_group_message_hidden(user_id,message_id)
    values(v_uid,p_message_id)
    on conflict do nothing;
end;
$$;

create or replace function public.rs_edit_group_message(p_message_id uuid,p_body text)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_sender uuid;
    v_clean text:=trim(coalesce(p_body,''));
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;
    if length(v_clean)<1 or length(v_clean)>1200 then
        raise exception 'message must contain 1 to 1200 characters' using errcode='22023';
    end if;

    select m.sender_id into v_sender
    from public.rs_group_messages m
    where m.id=p_message_id;

    if v_sender is null then
        raise exception 'message not found' using errcode='P0002';
    end if;

    if v_sender<>v_uid and not (select private.rs_is_staff()) then
        raise exception 'only the sender or staff may edit this message' using errcode='42501';
    end if;

    update public.rs_group_messages
    set body=v_clean
    where id=p_message_id;
end;
$$;

create or replace function public.rs_delete_group_message_for_everyone(p_message_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_sender uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select m.sender_id into v_sender
    from public.rs_group_messages m
    where m.id=p_message_id;

    if v_sender is null then
        return;
    end if;

    if v_sender<>v_uid and not (select private.rs_is_staff()) then
        raise exception 'only the sender or staff may delete for everyone' using errcode='42501';
    end if;

    delete from public.rs_group_messages where id=p_message_id;
end;
$$;

create or replace function public.rs_group_message_feed_v2(p_group_id uuid)
returns table(
    id uuid,
    group_id uuid,
    sender_id uuid,
    sender_email text,
    sender_name text,
    body text,
    media_path text,
    media_kind text,
    media_name text,
    reply_to uuid,
    reply_body text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        m.id,
        m.group_id,
        m.sender_id,
        p.email,
        p.display_name,
        m.body,
        m.media_path,
        m.media_kind,
        m.media_name,
        m.reply_to,
        left(coalesce(parent.body,''),180) as reply_body,
        m.created_at
    from public.rs_group_messages m
    join public.rs_profiles p on p.id=m.sender_id
    left join public.rs_group_messages parent on parent.id=m.reply_to
    where m.group_id=p_group_id
      and (
          (select private.rs_is_staff())
          or exists(
              select 1
              from public.rs_group_memberships gm
              where gm.group_id=p_group_id
                and gm.student_id=(select auth.uid())
          )
      )
      and not exists(
          select 1
          from public.rs_group_message_hidden h
          where h.user_id=(select auth.uid())
            and h.message_id=m.id
      )
    order by m.created_at asc;
$$;

create or replace function public.rs_send_group_message_v2(
    p_group_id uuid,
    p_body text default '',
    p_media_path text default null,
    p_media_kind text default null,
    p_media_name text default null,
    p_reply_to uuid default null
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare
    v_id uuid;
begin
    if p_reply_to is not null and not exists(
        select 1
        from public.rs_group_messages x
        where x.id=p_reply_to
          and x.group_id=p_group_id
    ) then
        raise exception 'reply target is not in this group' using errcode='22023';
    end if;

    v_id:=public.rs_send_group_message(
        p_group_id,
        p_body,
        p_media_path,
        p_media_kind,
        p_media_name
    );

    if p_reply_to is not null then
        update public.rs_group_messages
        set reply_to=p_reply_to
        where id=v_id;
    end if;

    return v_id;
end;
$$;

revoke all on function public.rs_hide_group_message(uuid) from public,anon;
revoke all on function public.rs_edit_group_message(uuid,text) from public,anon;
revoke all on function public.rs_delete_group_message_for_everyone(uuid) from public,anon;
revoke all on function public.rs_group_message_feed_v2(uuid) from public,anon;
revoke all on function public.rs_send_group_message_v2(uuid,text,text,text,text,uuid) from public,anon;

grant execute on function public.rs_hide_group_message(uuid) to authenticated;
grant execute on function public.rs_edit_group_message(uuid,text) to authenticated;
grant execute on function public.rs_delete_group_message_for_everyone(uuid) to authenticated;
grant execute on function public.rs_group_message_feed_v2(uuid) to authenticated;
grant execute on function public.rs_send_group_message_v2(uuid,text,text,text,text,uuid) to authenticated;
