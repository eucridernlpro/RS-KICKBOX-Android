-- RS KICKBOXING v0.135 cumulative communication backend
-- Run this whole file once in Supabase SQL Editor before testing the new call permissions
-- and trainer-hosted multi-student video rooms.

-- RS KICKBOXING v0.133 direct-call cumulative Supabase update
-- Run this whole file once in Supabase SQL Editor before testing calls.
-- Includes signaling + trainer/student-only permissions + online-only start rule.

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


-- ============================================================
-- ONLINE-ONLY CALL RULE
-- ============================================================

-- RS KICKBOXING v0.132 - online-only trainer <-> student calls
-- Uses the existing chat presence rule: online = last_seen_at within 2 minutes.

create or replace function public.rs_call_peer_status(p_peer_id uuid)
returns table(
    self_online boolean,
    peer_online boolean,
    peer_id uuid,
    peer_email text,
    peer_name text,
    peer_avatar_path text,
    peer_last_seen_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        coalesce(me.last_seen_at > now()-interval '2 minutes',false) as self_online,
        coalesce(peer.last_seen_at > now()-interval '2 minutes',false) as peer_online,
        peer.id as peer_id,
        peer.email as peer_email,
        peer.display_name as peer_name,
        peer.avatar_path as peer_avatar_path,
        peer.last_seen_at as peer_last_seen_at
    from public.rs_profiles me
    join public.rs_profiles peer on peer.id=p_peer_id
    where me.id=(select auth.uid())
      and me.active=true
      and peer.active=true
      and (
          (me.role in ('trainer','admin') and peer.role='student')
          or
          (me.role='student' and peer.role in ('trainer','admin'))
      )
    limit 1;
$$;

revoke all on function public.rs_call_peer_status(uuid) from public,anon;
grant execute on function public.rs_call_peer_status(uuid) to authenticated;

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
    v_me_online boolean;
    v_peer_online boolean;
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

    select lower(role),coalesce(last_seen_at > now()-interval '2 minutes',false)
      into v_my_role,v_me_online
    from public.rs_profiles
    where id=v_uid and active=true;

    select lower(role),coalesce(last_seen_at > now()-interval '2 minutes',false)
      into v_peer_role,v_peer_online
    from public.rs_profiles
    where id=p_peer_id and active=true;

    if v_my_role='student' and v_peer_role in ('trainer','admin') then
        v_student:=v_uid;
    elsif v_peer_role='student' and v_my_role in ('trainer','admin') then
        v_student:=p_peer_id;
    else
        raise exception 'calls are only allowed between trainer and student'
            using errcode='42501';
    end if;

    if not coalesce(v_me_online,false) or not coalesce(v_peer_online,false) then
        raise exception 'both trainer and student must be online to start a call'
            using errcode='P0001';
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

revoke all on function public.rs_start_direct_call(uuid,text) from public,anon;
grant execute on function public.rs_start_direct_call(uuid,text) to authenticated;



-- ============================================================
-- STUDENT CALL PERMISSIONS + TRAINER GROUP VIDEO
-- ============================================================

-- RS KICKBOXING v0.136 - student call permissions + trainer-hosted multi video rooms

create table if not exists public.rs_student_call_permissions (
    student_id uuid primary key references public.rs_profiles(id) on delete cascade,
    audio_enabled boolean not null default false,
    video_enabled boolean not null default false,
    updated_by uuid references public.rs_profiles(id) on delete set null,
    updated_at timestamptz not null default now()
);

alter table public.rs_student_call_permissions enable row level security;

drop policy if exists "rs_student_call_permissions_staff_read" on public.rs_student_call_permissions;
create policy "rs_student_call_permissions_staff_read"
on public.rs_student_call_permissions for select to authenticated
using (
    exists(select 1 from public.rs_profiles me
           where me.id=(select auth.uid()) and me.active=true and me.role in ('trainer','admin'))
    or student_id=(select auth.uid())
);

create or replace function public.rs_set_student_call_permissions(
    p_student_id uuid,
    p_audio_enabled boolean,
    p_video_enabled boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
begin
    if not exists(
        select 1 from public.rs_profiles me
        where me.id=v_uid and me.active=true and me.role in ('trainer','admin')
    ) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if not exists(
        select 1 from public.rs_profiles s
        where s.id=p_student_id and s.active=true and s.role='student'
    ) then
        raise exception 'active student required' using errcode='22023';
    end if;

    insert into public.rs_student_call_permissions(student_id,audio_enabled,video_enabled,updated_by,updated_at)
    values(p_student_id,coalesce(p_audio_enabled,false),coalesce(p_video_enabled,false),v_uid,now())
    on conflict(student_id) do update
    set audio_enabled=excluded.audio_enabled,
        video_enabled=excluded.video_enabled,
        updated_by=v_uid,
        updated_at=now();
end;
$$;

create or replace function public.rs_student_call_permission_list()
returns table(
    student_id uuid,
    email text,
    display_name text,
    avatar_path text,
    online boolean,
    audio_enabled boolean,
    video_enabled boolean
)
language sql
stable
security definer
set search_path=''
as $$
    select
        p.id,
        p.email,
        p.display_name,
        p.avatar_path,
        coalesce(p.last_seen_at>now()-interval '2 minutes',false),
        coalesce(cp.audio_enabled,false),
        coalesce(cp.video_enabled,false)
    from public.rs_profiles p
    left join public.rs_student_call_permissions cp on cp.student_id=p.id
    where p.active=true and p.role='student'
      and exists(
          select 1 from public.rs_profiles me
          where me.id=(select auth.uid()) and me.active=true and me.role in ('trainer','admin')
      )
    order by coalesce(nullif(p.display_name,''),p.email);
$$;

create or replace function public.rs_my_student_call_permissions()
returns table(audio_enabled boolean,video_enabled boolean)
language sql
stable
security definer
set search_path=''
as $$
    select coalesce(cp.audio_enabled,false),coalesce(cp.video_enabled,false)
    from public.rs_profiles me
    left join public.rs_student_call_permissions cp on cp.student_id=me.id
    where me.id=(select auth.uid()) and me.active=true and me.role='student';
$$;

-- Replace direct-call start rule:
-- trainer<->student always allowed when both online.
-- student<->student only when BOTH students have the selected call type enabled.
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
    v_me_online boolean;
    v_peer_online boolean;
    v_my_audio boolean := false;
    v_my_video boolean := false;
    v_peer_audio boolean := false;
    v_peer_video boolean := false;
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

    select lower(role),coalesce(last_seen_at>now()-interval '2 minutes',false)
      into v_my_role,v_me_online
    from public.rs_profiles
    where id=v_uid and active=true;

    select lower(role),coalesce(last_seen_at>now()-interval '2 minutes',false)
      into v_peer_role,v_peer_online
    from public.rs_profiles
    where id=p_peer_id and active=true;

    if not coalesce(v_me_online,false) or not coalesce(v_peer_online,false) then
        raise exception 'both users must be online to start a call' using errcode='P0001';
    end if;

    if v_my_role='student' and v_peer_role in ('trainer','admin') then
        v_student:=v_uid;
    elsif v_peer_role='student' and v_my_role in ('trainer','admin') then
        v_student:=p_peer_id;
    elsif v_my_role='student' and v_peer_role='student' then
        select coalesce(audio_enabled,false),coalesce(video_enabled,false)
          into v_my_audio,v_my_video
        from public.rs_student_call_permissions where student_id=v_uid;
        select coalesce(audio_enabled,false),coalesce(video_enabled,false)
          into v_peer_audio,v_peer_video
        from public.rs_student_call_permissions where student_id=p_peer_id;

        if v_type='AUDIO' and not (coalesce(v_my_audio,false) and coalesce(v_peer_audio,false)) then
            raise exception 'student audio calls are blocked by trainer settings' using errcode='42501';
        end if;
        if v_type='VIDEO' and not (coalesce(v_my_video,false) and coalesce(v_peer_video,false)) then
            raise exception 'student video calls are blocked by trainer settings' using errcode='42501';
        end if;
        v_student:=v_uid;
    else
        raise exception 'call participants are not allowed' using errcode='42501';
    end if;

    if exists(
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

-- Multi-student trainer-hosted video rooms.
create table if not exists public.rs_video_rooms (
    id uuid primary key default gen_random_uuid(),
    host_id uuid not null references public.rs_profiles(id) on delete cascade,
    title text not null default 'RS Video Session',
    status text not null default 'OPEN' check(status in ('OPEN','ENDED','CANCELLED')),
    created_at timestamptz not null default now(),
    ended_at timestamptz
);

create table if not exists public.rs_video_room_members (
    room_id uuid not null references public.rs_video_rooms(id) on delete cascade,
    user_id uuid not null references public.rs_profiles(id) on delete cascade,
    role text not null check(role in ('HOST','STUDENT')),
    status text not null default 'INVITED' check(status in ('INVITED','JOINED','DECLINED','LEFT')),
    invited_at timestamptz not null default now(),
    joined_at timestamptz,
    left_at timestamptz,
    primary key(room_id,user_id)
);

create table if not exists public.rs_video_room_signals (
    id bigint generated always as identity primary key,
    room_id uuid not null references public.rs_video_rooms(id) on delete cascade,
    sender_id uuid not null references public.rs_profiles(id) on delete cascade,
    target_id uuid not null references public.rs_profiles(id) on delete cascade,
    signal_kind text not null check(signal_kind in ('OFFER','ANSWER','ICE')),
    payload jsonb not null,
    created_at timestamptz not null default now()
);

create index if not exists rs_video_room_signals_lookup
on public.rs_video_room_signals(room_id,target_id,id);

alter table public.rs_video_rooms enable row level security;
alter table public.rs_video_room_members enable row level security;
alter table public.rs_video_room_signals enable row level security;

drop policy if exists "rs_video_rooms_member_read" on public.rs_video_rooms;
create policy "rs_video_rooms_member_read"
on public.rs_video_rooms for select to authenticated
using(
    exists(select 1 from public.rs_video_room_members m
           where m.room_id=id and m.user_id=(select auth.uid()))
);

drop policy if exists "rs_video_room_members_read" on public.rs_video_room_members;
create policy "rs_video_room_members_read"
on public.rs_video_room_members for select to authenticated
using(user_id=(select auth.uid()));

drop policy if exists "rs_video_room_signals_read" on public.rs_video_room_signals;
create policy "rs_video_room_signals_read"
on public.rs_video_room_signals for select to authenticated
using(sender_id=(select auth.uid()) or target_id=(select auth.uid()));

create or replace function public.rs_create_video_room(
    p_title text,
    p_student_ids uuid[]
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_room uuid;
    v_student uuid;
begin
    if not exists(
        select 1 from public.rs_profiles me
        where me.id=v_uid and me.active=true and me.role in ('trainer','admin')
    ) then
        raise exception 'only trainer/admin may create multi video rooms' using errcode='42501';
    end if;

    if coalesce(array_length(p_student_ids,1),0)<1 then
        raise exception 'select at least one student' using errcode='22023';
    end if;
    if array_length(p_student_ids,1)>8 then
        raise exception 'maximum 8 students per trainer video room' using errcode='22023';
    end if;

    insert into public.rs_video_rooms(host_id,title,status)
    values(v_uid,left(coalesce(nullif(trim(p_title),''),'RS Video Session'),100),'OPEN')
    returning id into v_room;

    insert into public.rs_video_room_members(room_id,user_id,role,status,joined_at)
    values(v_room,v_uid,'HOST','JOINED',now());

    foreach v_student in array p_student_ids loop
        if not exists(
            select 1 from public.rs_profiles s
            where s.id=v_student
              and s.active=true
              and s.role='student'
              and s.last_seen_at>now()-interval '2 minutes'
        ) then
            raise exception 'all selected students must still be online' using errcode='P0001';
        end if;

        insert into public.rs_video_room_members(room_id,user_id,role,status)
        values(v_room,v_student,'STUDENT','INVITED')
        on conflict do nothing;
    end loop;

    return v_room;
end;
$$;

create or replace function public.rs_my_video_rooms()
returns table(
    room_id uuid,
    title text,
    room_status text,
    host_id uuid,
    host_name text,
    my_role text,
    my_status text,
    participant_count bigint,
    online_count bigint,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        r.id,r.title,r.status,r.host_id,
        coalesce(nullif(h.display_name,''),h.email,'RS Trainer'),
        me.role,me.status,
        (select count(*) from public.rs_video_room_members x where x.room_id=r.id),
        (select count(*) from public.rs_video_room_members x
         join public.rs_profiles p on p.id=x.user_id
         where x.room_id=r.id and p.last_seen_at>now()-interval '2 minutes'),
        r.created_at
    from public.rs_video_rooms r
    join public.rs_video_room_members me on me.room_id=r.id and me.user_id=(select auth.uid())
    join public.rs_profiles h on h.id=r.host_id
    where r.status='OPEN'
    order by r.created_at desc;
$$;

create or replace function public.rs_video_room_members(p_room_id uuid)
returns table(
    user_id uuid,
    display_name text,
    email text,
    avatar_path text,
    member_role text,
    member_status text,
    online boolean
)
language sql
stable
security definer
set search_path=''
as $$
    select p.id,p.display_name,p.email,p.avatar_path,m.role,m.status,
           coalesce(p.last_seen_at>now()-interval '2 minutes',false)
    from public.rs_video_room_members m
    join public.rs_profiles p on p.id=m.user_id
    where m.room_id=p_room_id
      and exists(select 1 from public.rs_video_room_members me
                 where me.room_id=p_room_id and me.user_id=(select auth.uid()))
    order by case when m.role='HOST' then 0 else 1 end,
             coalesce(nullif(p.display_name,''),p.email);
$$;

create or replace function public.rs_set_video_room_status(
    p_room_id uuid,
    p_status text
)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_status text := upper(trim(coalesce(p_status,'')));
    v_host uuid;
begin
    select host_id into v_host from public.rs_video_rooms where id=p_room_id;
    if v_host is null then raise exception 'room not found' using errcode='P0002'; end if;

    if v_status in ('ENDED','CANCELLED') then
        if v_uid<>v_host then raise exception 'only trainer host may end room' using errcode='42501'; end if;
        update public.rs_video_rooms
        set status=v_status,ended_at=coalesce(ended_at,now())
        where id=p_room_id;
        update public.rs_video_room_members
        set status=case when status='JOINED' then 'LEFT' else status end,
            left_at=case when status='JOINED' then now() else left_at end
        where room_id=p_room_id;
        return;
    end if;

    if v_status not in ('JOINED','DECLINED','LEFT') then
        raise exception 'invalid room member status' using errcode='22023';
    end if;

    update public.rs_video_room_members
    set status=v_status,
        joined_at=case when v_status='JOINED' then coalesce(joined_at,now()) else joined_at end,
        left_at=case when v_status='LEFT' then coalesce(left_at,now()) else left_at end
    where room_id=p_room_id and user_id=v_uid;
end;
$$;

create or replace function public.rs_add_video_room_signal(
    p_room_id uuid,
    p_target_id uuid,
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
    v_kind text := upper(trim(coalesce(p_signal_kind,'')));
    v_id bigint;
begin
    if not exists(select 1 from public.rs_video_room_members m
                  where m.room_id=p_room_id and m.user_id=v_uid and m.status='JOINED') then
        raise exception 'joined room member required' using errcode='42501';
    end if;
    if not exists(select 1 from public.rs_video_room_members m
                  where m.room_id=p_room_id and m.user_id=p_target_id and m.status in ('INVITED','JOINED')) then
        raise exception 'target is not a room member' using errcode='42501';
    end if;
    if v_kind not in ('OFFER','ANSWER','ICE') then
        raise exception 'invalid signal kind' using errcode='22023';
    end if;
    if p_payload is null or pg_column_size(p_payload)>65536 then
        raise exception 'invalid signal payload' using errcode='22023';
    end if;

    insert into public.rs_video_room_signals(room_id,sender_id,target_id,signal_kind,payload)
    values(p_room_id,v_uid,p_target_id,v_kind,p_payload)
    returning id into v_id;
    return v_id;
end;
$$;

create or replace function public.rs_video_room_signals_since(
    p_room_id uuid,
    p_after_id bigint default 0
)
returns table(
    id bigint,
    sender_id uuid,
    target_id uuid,
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
    if not exists(select 1 from public.rs_video_room_members m
                  where m.room_id=p_room_id and m.user_id=v_uid) then
        raise exception 'room membership required' using errcode='42501';
    end if;

    return query
    select s.id,s.sender_id,s.target_id,s.signal_kind,s.payload,s.created_at
    from public.rs_video_room_signals s
    where s.room_id=p_room_id
      and s.id>coalesce(p_after_id,0)
      and (s.target_id=v_uid or s.sender_id=v_uid)
    order by s.id asc;
end;
$$;

revoke all on function public.rs_set_student_call_permissions(uuid,boolean,boolean) from public,anon;
revoke all on function public.rs_student_call_permission_list() from public,anon;
revoke all on function public.rs_my_student_call_permissions() from public,anon;
revoke all on function public.rs_create_video_room(text,uuid[]) from public,anon;
revoke all on function public.rs_my_video_rooms() from public,anon;
revoke all on function public.rs_video_room_members(uuid) from public,anon;
revoke all on function public.rs_set_video_room_status(uuid,text) from public,anon;
revoke all on function public.rs_add_video_room_signal(uuid,uuid,text,jsonb) from public,anon;
revoke all on function public.rs_video_room_signals_since(uuid,bigint) from public,anon;

grant execute on function public.rs_set_student_call_permissions(uuid,boolean,boolean) to authenticated;
grant execute on function public.rs_student_call_permission_list() to authenticated;
grant execute on function public.rs_my_student_call_permissions() to authenticated;
grant execute on function public.rs_create_video_room(text,uuid[]) to authenticated;
grant execute on function public.rs_my_video_rooms() to authenticated;
grant execute on function public.rs_video_room_members(uuid) to authenticated;
grant execute on function public.rs_set_video_room_status(uuid,text) to authenticated;
grant execute on function public.rs_add_video_room_signal(uuid,uuid,text,jsonb) to authenticated;
grant execute on function public.rs_video_room_signals_since(uuid,bigint) to authenticated;


-- Student call contacts exposed only when trainer permissions allow it.
create or replace function public.rs_student_call_contacts()
returns table(
    user_id uuid,
    display_name text,
    email text,
    avatar_path text,
    online boolean,
    audio_allowed boolean,
    video_allowed boolean
)
language sql
stable
security definer
set search_path=''
as $$
    with mine as (
        select coalesce(cp.audio_enabled,false) audio_enabled,
               coalesce(cp.video_enabled,false) video_enabled
        from public.rs_profiles me
        left join public.rs_student_call_permissions cp on cp.student_id=me.id
        where me.id=(select auth.uid()) and me.active=true and me.role='student'
    )
    select
        p.id,p.display_name,p.email,p.avatar_path,
        coalesce(p.last_seen_at>now()-interval '2 minutes',false),
        (select audio_enabled from mine) and coalesce(cp.audio_enabled,false),
        (select video_enabled from mine) and coalesce(cp.video_enabled,false)
    from public.rs_profiles p
    left join public.rs_student_call_permissions cp on cp.student_id=p.id
    where p.active=true
      and p.role='student'
      and p.id<>(select auth.uid())
      and exists(select 1 from mine where audio_enabled or video_enabled)
      and (
          ((select audio_enabled from mine) and coalesce(cp.audio_enabled,false))
          or
          ((select video_enabled from mine) and coalesce(cp.video_enabled,false))
      )
    order by coalesce(nullif(p.display_name,''),p.email);
$$;

-- Peer status supports trainer<->student and trainer-approved student<->student calls.
create or replace function public.rs_call_peer_status(p_peer_id uuid)
returns table(
    self_online boolean,
    peer_online boolean,
    peer_id uuid,
    peer_email text,
    peer_name text,
    peer_avatar_path text,
    peer_last_seen_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        coalesce(me.last_seen_at > now()-interval '2 minutes',false),
        coalesce(peer.last_seen_at > now()-interval '2 minutes',false),
        peer.id,peer.email,peer.display_name,peer.avatar_path,peer.last_seen_at
    from public.rs_profiles me
    join public.rs_profiles peer on peer.id=p_peer_id
    left join public.rs_student_call_permissions mecp on mecp.student_id=me.id
    left join public.rs_student_call_permissions pcp on pcp.student_id=peer.id
    where me.id=(select auth.uid())
      and me.active=true and peer.active=true
      and (
          (me.role in ('trainer','admin') and peer.role='student')
          or
          (me.role='student' and peer.role in ('trainer','admin'))
          or
          (
              me.role='student' and peer.role='student'
              and (
                  (coalesce(mecp.audio_enabled,false) and coalesce(pcp.audio_enabled,false))
                  or
                  (coalesce(mecp.video_enabled,false) and coalesce(pcp.video_enabled,false))
              )
          )
      )
    limit 1;
$$;

revoke all on function public.rs_student_call_contacts() from public,anon;
grant execute on function public.rs_student_call_contacts() to authenticated;
revoke all on function public.rs_call_peer_status(uuid) from public,anon;
grant execute on function public.rs_call_peer_status(uuid) to authenticated;

