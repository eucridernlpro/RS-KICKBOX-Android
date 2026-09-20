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
