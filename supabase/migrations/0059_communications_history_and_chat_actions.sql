-- RS KICKBOXING v0.156 - communications history + private chat message actions
-- Run after 0058. Safe to run repeatedly.

-- Per-user hidden messages ("Delete for me").
create table if not exists public.rs_coach_message_hidden (
    user_id uuid not null references auth.users(id) on delete cascade,
    message_id uuid not null references public.rs_coach_messages(id) on delete cascade,
    hidden_at timestamptz not null default now(),
    primary key(user_id,message_id)
);
alter table public.rs_coach_message_hidden enable row level security;
drop policy if exists "rs_coach_message_hidden_own" on public.rs_coach_message_hidden;
create policy "rs_coach_message_hidden_own"
on public.rs_coach_message_hidden for all to authenticated
using(user_id=(select auth.uid()))
with check(user_id=(select auth.uid()));

alter table public.rs_coach_messages
    add column if not exists reply_to uuid references public.rs_coach_messages(id) on delete set null;

create or replace function public.rs_hide_coach_message(p_message_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=(select auth.uid());
begin
  if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
  if not exists(
    select 1
    from public.rs_coach_messages m
    join public.rs_profiles s on s.id=m.student_id
    where m.id=p_message_id
      and ((select private.rs_is_staff()) or s.id=v_uid)
  ) then
    raise exception 'message access denied' using errcode='42501';
  end if;
  insert into public.rs_coach_message_hidden(user_id,message_id)
  values(v_uid,p_message_id)
  on conflict do nothing;
end;
$$;

create or replace function public.rs_delete_coach_message_for_everyone(p_message_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=(select auth.uid());
  v_sender uuid;
begin
  select sender_id into v_sender from public.rs_coach_messages where id=p_message_id;
  if v_sender is null then return; end if;
  if v_sender<>v_uid and not (select private.rs_is_staff()) then
    raise exception 'only the sender or staff may delete for everyone' using errcode='42501';
  end if;
  delete from public.rs_coach_messages where id=p_message_id;
end;
$$;

create or replace function public.rs_coach_thread_messages_v3(p_student_id uuid)
returns table (
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    sender_role text,
    sender_id uuid,
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
        m.id,m.student_id,p.email,p.display_name,m.sender_role,m.sender_id,m.body,
        m.media_path,m.media_kind,m.media_name,m.reply_to,
        left(coalesce(parent.body,''),180) as reply_body,
        m.created_at
    from public.rs_coach_messages m
    join public.rs_profiles p on p.id=m.student_id
    left join public.rs_coach_messages parent on parent.id=m.reply_to
    where m.student_id=p_student_id
      and ((select private.rs_is_staff()) or (select auth.uid())=p_student_id)
      and not exists(
        select 1 from public.rs_coach_message_hidden h
        where h.user_id=(select auth.uid()) and h.message_id=m.id
      )
    order by m.created_at asc;
$$;

create or replace function public.rs_send_coach_message_v3(
    p_student_id uuid,
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

    select p.role into v_role from public.rs_profiles p where p.id=v_uid and p.active=true;
    if v_role='student' and v_uid<>p_student_id then
      raise exception 'students may only message their own coach thread' using errcode='42501';
    elsif v_role not in ('student','trainer','admin') then
      raise exception 'active account required' using errcode='42501';
    end if;

    if p_reply_to is not null and not exists(
      select 1 from public.rs_coach_messages x where x.id=p_reply_to and x.student_id=p_student_id
    ) then
      raise exception 'reply target is not in this chat' using errcode='22023';
    end if;

    insert into public.rs_coach_messages(
      student_id,sender_id,sender_role,body,media_path,media_kind,media_name,reply_to
    ) values(
      p_student_id,v_uid,case when v_role='student' then 'student' else 'trainer' end,
      v_body,p_media_path,p_media_kind,p_media_name,p_reply_to
    ) returning id into v_id;
    return v_id;
end;
$$;

-- Unified direct-call history. Existing rs_calls already stores all call states.
create or replace function public.rs_call_history()
returns table(
    id uuid,
    peer_id uuid,
    peer_name text,
    peer_email text,
    call_type text,
    status text,
    direction text,
    created_at timestamptz,
    answered_at timestamptz,
    ended_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
  select
    c.id,
    case when c.caller_id=(select auth.uid()) then c.callee_id else c.caller_id end,
    coalesce(nullif(p.display_name,''),p.email,'RS Member'),
    coalesce(p.email,''),
    c.call_type,
    c.status,
    case when c.caller_id=(select auth.uid()) then 'OUTGOING' else 'INCOMING' end,
    c.created_at,c.answered_at,c.ended_at
  from public.rs_calls c
  join public.rs_profiles p
    on p.id=case when c.caller_id=(select auth.uid()) then c.callee_id else c.caller_id end
  where (select auth.uid()) in (c.caller_id,c.callee_id)
  order by c.created_at desc
  limit 100;
$$;

revoke all on function public.rs_hide_coach_message(uuid) from public,anon;
revoke all on function public.rs_delete_coach_message_for_everyone(uuid) from public,anon;
revoke all on function public.rs_coach_thread_messages_v3(uuid) from public,anon;
revoke all on function public.rs_send_coach_message_v3(uuid,text,text,text,text,uuid) from public,anon;
revoke all on function public.rs_call_history() from public,anon;
grant execute on function public.rs_hide_coach_message(uuid) to authenticated;
grant execute on function public.rs_delete_coach_message_for_everyone(uuid) to authenticated;
grant execute on function public.rs_coach_thread_messages_v3(uuid) to authenticated;
grant execute on function public.rs_send_coach_message_v3(uuid,text,text,text,text,uuid) to authenticated;
grant execute on function public.rs_call_history() to authenticated;
