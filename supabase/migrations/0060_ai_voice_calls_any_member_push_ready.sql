-- RS KICKBOXING v0.178+ AI voice-call reliability update
-- Aligns direct-call SQL with push calling and widens trainer-hosted video rooms
-- from student-only invites to active RS members.

-- ------------------------------------------------------------
-- 1) Direct calls: authenticated caller may ring an eligible peer even when
-- presence is stale/offline. FCM / call monitor handles background delivery.
-- ------------------------------------------------------------
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

    select lower(role) into v_my_role
    from public.rs_profiles
    where id=v_uid and active=true;

    select lower(role) into v_peer_role
    from public.rs_profiles
    where id=p_peer_id and active=true;

    if v_my_role is null or v_peer_role is null then
        raise exception 'active call participants required' using errcode='42501';
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
    elsif v_my_role in ('trainer','admin') and v_peer_role in ('trainer','admin') then
        -- Staff-to-staff calls are useful for RS operations and AI voice commands.
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

grant execute on function public.rs_start_direct_call(uuid,text) to authenticated;

-- ------------------------------------------------------------
-- 2) Group video: retain trainer/admin hosting, but allow any active RS member
-- as an invitee. Push notifications can wake invitees outside the app.
-- ------------------------------------------------------------
alter table public.rs_video_room_members
    drop constraint if exists rs_video_room_members_role_check;

alter table public.rs_video_room_members
    add constraint rs_video_room_members_role_check
    check(role in ('HOST','STUDENT','MEMBER'));

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
    v_member uuid;
begin
    if not exists(
        select 1 from public.rs_profiles me
        where me.id=v_uid and me.active=true and me.role in ('trainer','admin')
    ) then
        raise exception 'only trainer/admin may create multi video rooms' using errcode='42501';
    end if;

    if coalesce(array_length(p_student_ids,1),0)<1 then
        raise exception 'select at least one member' using errcode='22023';
    end if;
    if array_length(p_student_ids,1)>8 then
        raise exception 'maximum 8 invited members per trainer video room' using errcode='22023';
    end if;

    insert into public.rs_video_rooms(host_id,title,status)
    values(v_uid,left(coalesce(nullif(trim(p_title),''),'RS Video Session'),100),'OPEN')
    returning id into v_room;

    insert into public.rs_video_room_members(room_id,user_id,role,status,joined_at)
    values(v_room,v_uid,'HOST','JOINED',now());

    foreach v_member in array p_student_ids loop
        if v_member=v_uid then
            continue;
        end if;

        if not exists(
            select 1 from public.rs_profiles p
            where p.id=v_member and p.active=true
        ) then
            raise exception 'all selected participants must have active RS accounts' using errcode='P0001';
        end if;

        insert into public.rs_video_room_members(room_id,user_id,role,status)
        values(v_room,v_member,'MEMBER','INVITED')
        on conflict do nothing;
    end loop;

    return v_room;
end;
$$;

grant execute on function public.rs_create_video_room(text,uuid[]) to authenticated;
