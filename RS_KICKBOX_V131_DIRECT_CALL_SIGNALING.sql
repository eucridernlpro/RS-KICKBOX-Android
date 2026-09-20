-- RS KICKBOXING v0.131 - trainer <-> student direct-call signaling foundation
-- Supabase stores call state + WebRTC signaling only. Live audio/video is NOT stored here.

create table if not exists public.rs_calls (
    id uuid primary key default gen_random_uuid(),
    caller_id uuid not null references public.rs_profiles(id) on delete cascade,
    callee_id uuid not null references public.rs_profiles(id) on delete cascade,
    student_id uuid not null references public.rs_profiles(id) on delete cascade,
    call_type text not null check (call_type in ('AUDIO','VIDEO')),
    status text not null default 'RINGING'
        check (status in ('RINGING','ACCEPTED','DECLINED','MISSED','ENDED','CANCELLED')),
    created_at timestamptz not null default now(),
    answered_at timestamptz,
    ended_at timestamptz,
    constraint rs_calls_not_self check (caller_id<>callee_id)
);

create index if not exists rs_calls_caller_idx on public.rs_calls(caller_id,created_at desc);
create index if not exists rs_calls_callee_idx on public.rs_calls(callee_id,created_at desc);
create index if not exists rs_calls_student_idx on public.rs_calls(student_id,created_at desc);

alter table public.rs_calls enable row level security;

drop policy if exists "rs_calls_participants_read" on public.rs_calls;
create policy "rs_calls_participants_read"
on public.rs_calls for select to authenticated
using ((select auth.uid())=caller_id or (select auth.uid())=callee_id);

create table if not exists public.rs_call_signals (
    id bigint generated always as identity primary key,
    call_id uuid not null references public.rs_calls(id) on delete cascade,
    sender_id uuid not null references public.rs_profiles(id) on delete cascade,
    signal_kind text not null check (signal_kind in ('OFFER','ANSWER','ICE')),
    payload jsonb not null,
    created_at timestamptz not null default now()
);

create index if not exists rs_call_signals_call_idx
on public.rs_call_signals(call_id,id);

alter table public.rs_call_signals enable row level security;

drop policy if exists "rs_call_signals_participants_read" on public.rs_call_signals;
create policy "rs_call_signals_participants_read"
on public.rs_call_signals for select to authenticated
using (
    exists (
        select 1 from public.rs_calls c
        where c.id=call_id
          and ((select auth.uid())=c.caller_id or (select auth.uid())=c.callee_id)
    )
);

create or replace function public.rs_start_direct_call(
    p_peer_id uuid,
    p_call_type text
)
returns table(call_id uuid)
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_my_role text;
    v_peer_role text;
    v_student uuid;
    v_type text := upper(trim(coalesce(p_call_type,'')));
    v_call uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;
    if p_peer_id is null or p_peer_id=v_uid then
        raise exception 'invalid call peer' using errcode='22023';
    end if;
    if v_type not in ('AUDIO','VIDEO') then
        raise exception 'invalid call type' using errcode='22023';
    end if;

    select lower(role) into v_my_role
    from public.rs_profiles where id=v_uid and active=true;
    select lower(role) into v_peer_role
    from public.rs_profiles where id=p_peer_id and active=true;

    if v_my_role='student' and v_peer_role in ('trainer','admin') then
        v_student:=v_uid;
    elsif v_peer_role='student' and v_my_role in ('trainer','admin') then
        v_student:=p_peer_id;
    else
        raise exception 'calls are only allowed between trainer and student'
            using errcode='42501';
    end if;

    if exists (
        select 1 from public.rs_calls c
        where (c.caller_id=v_uid or c.callee_id=v_uid)
          and c.status in ('RINGING','ACCEPTED')
    ) then
        raise exception 'you already have an active call' using errcode='P0001';
    end if;

    insert into public.rs_calls(caller_id,callee_id,student_id,call_type,status)
    values(v_uid,p_peer_id,v_student,v_type,'RINGING')
    returning id into v_call;

    return query select v_call;
end;
$$;

create or replace function public.rs_call_inbox()
returns table(
    id uuid,
    caller_id uuid,
    callee_id uuid,
    student_id uuid,
    call_type text,
    status text,
    peer_id uuid,
    peer_name text,
    peer_email text,
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
        c.id,c.caller_id,c.callee_id,c.student_id,c.call_type,c.status,
        case when c.caller_id=(select auth.uid()) then c.callee_id else c.caller_id end as peer_id,
        coalesce(nullif(p.display_name,''),nullif(p.email,''),'RS Member') as peer_name,
        coalesce(p.email,'') as peer_email,
        c.created_at,c.answered_at,c.ended_at
    from public.rs_calls c
    join public.rs_profiles p
      on p.id=case when c.caller_id=(select auth.uid()) then c.callee_id else c.caller_id end
    where (c.caller_id=(select auth.uid()) or c.callee_id=(select auth.uid()))
    order by c.created_at desc
    limit 50;
$$;

create or replace function public.rs_set_call_status(
    p_call_id uuid,
    p_status text
)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_call public.rs_calls%rowtype;
    v_status text := upper(trim(coalesce(p_status,'')));
begin
    select * into v_call from public.rs_calls where id=p_call_id for update;
    if v_call.id is null then raise exception 'call not found' using errcode='P0002'; end if;
    if v_uid not in (v_call.caller_id,v_call.callee_id) then
        raise exception 'not a call participant' using errcode='42501';
    end if;
    if v_status not in ('ACCEPTED','DECLINED','MISSED','ENDED','CANCELLED') then
        raise exception 'invalid call status' using errcode='22023';
    end if;
    if v_status='ACCEPTED' and v_uid<>v_call.callee_id then
        raise exception 'only the receiver can accept a call' using errcode='42501';
    end if;

    update public.rs_calls
    set status=v_status,
        answered_at=case when v_status='ACCEPTED' then coalesce(answered_at,now()) else answered_at end,
        ended_at=case when v_status in ('DECLINED','MISSED','ENDED','CANCELLED') then coalesce(ended_at,now()) else ended_at end
    where id=p_call_id;
end;
$$;

create or replace function public.rs_add_call_signal(
    p_call_id uuid,
    p_signal_kind text,
    p_payload jsonb
)
returns bigint
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_call public.rs_calls%rowtype;
    v_kind text := upper(trim(coalesce(p_signal_kind,'')));
    v_id bigint;
begin
    select * into v_call from public.rs_calls where id=p_call_id;
    if v_call.id is null then raise exception 'call not found' using errcode='P0002'; end if;
    if v_uid not in (v_call.caller_id,v_call.callee_id) then
        raise exception 'not a call participant' using errcode='42501';
    end if;
    if v_kind not in ('OFFER','ANSWER','ICE') then
        raise exception 'invalid signal kind' using errcode='22023';
    end if;
    if p_payload is null or pg_column_size(p_payload)>65536 then
        raise exception 'invalid signal payload' using errcode='22023';
    end if;

    insert into public.rs_call_signals(call_id,sender_id,signal_kind,payload)
    values(p_call_id,v_uid,v_kind,p_payload)
    returning id into v_id;
    return v_id;
end;
$$;

create or replace function public.rs_call_signals_since(
    p_call_id uuid,
    p_after_id bigint default 0
)
returns table(
    id bigint,
    sender_id uuid,
    signal_kind text,
    payload jsonb,
    created_at timestamptz
)
language plpgsql
stable
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
begin
    if not exists(
        select 1 from public.rs_calls c
        where c.id=p_call_id and v_uid in (c.caller_id,c.callee_id)
    ) then
        raise exception 'not a call participant' using errcode='42501';
    end if;

    return query
    select s.id,s.sender_id,s.signal_kind,s.payload,s.created_at
    from public.rs_call_signals s
    where s.call_id=p_call_id and s.id>coalesce(p_after_id,0)
    order by s.id asc;
end;
$$;

create or replace function public.rs_student_call_peer()
returns table(
    user_id uuid,
    display_name text,
    email text
)
language sql
stable
security definer
set search_path=''
as $$
    select p.id,p.display_name,p.email
    from public.rs_profiles p
    where p.active=true
      and p.role in ('trainer','admin')
      and exists(
          select 1 from public.rs_profiles me
          where me.id=(select auth.uid()) and me.active=true and me.role='student'
      )
    order by case when p.role='trainer' then 0 else 1 end,p.created_at asc
    limit 1;
$$;

revoke all on function public.rs_start_direct_call(uuid,text) from public,anon;
revoke all on function public.rs_call_inbox() from public,anon;
revoke all on function public.rs_set_call_status(uuid,text) from public,anon;
revoke all on function public.rs_add_call_signal(uuid,text,jsonb) from public,anon;
revoke all on function public.rs_call_signals_since(uuid,bigint) from public,anon;
revoke all on function public.rs_student_call_peer() from public,anon;

grant execute on function public.rs_start_direct_call(uuid,text) to authenticated;
grant execute on function public.rs_call_inbox() to authenticated;
grant execute on function public.rs_set_call_status(uuid,text) to authenticated;
grant execute on function public.rs_add_call_signal(uuid,text,jsonb) to authenticated;
grant execute on function public.rs_call_signals_since(uuid,bigint) to authenticated;
grant execute on function public.rs_student_call_peer() to authenticated;
