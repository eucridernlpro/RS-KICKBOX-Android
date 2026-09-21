-- RS KICKBOXING v0.151 - repair video-room permissions on existing projects.
-- Safe to run repeatedly.

grant usage on schema public to authenticated;

revoke all on function public.rs_my_video_rooms() from public,anon;
revoke all on function public.rs_video_room_members(uuid) from public,anon;
revoke all on function public.rs_set_video_room_status(uuid,text) from public,anon;
revoke all on function public.rs_add_video_room_signal(uuid,uuid,text,jsonb) from public,anon;
revoke all on function public.rs_video_room_signals_since(uuid,bigint) from public,anon;
revoke all on function public.rs_create_video_room(text,uuid[]) from public,anon;

grant execute on function public.rs_my_video_rooms() to authenticated;
grant execute on function public.rs_video_room_members(uuid) to authenticated;
grant execute on function public.rs_set_video_room_status(uuid,text) to authenticated;
grant execute on function public.rs_add_video_room_signal(uuid,uuid,text,jsonb) to authenticated;
grant execute on function public.rs_video_room_signals_since(uuid,bigint) to authenticated;
grant execute on function public.rs_create_video_room(text,uuid[]) to authenticated;

-- Room data is intentionally exposed through security-definer RPCs only.
-- Remove any legacy direct-table read policy to avoid recursive RLS evaluation.
drop policy if exists "rs_video_room_members_read" on public.rs_video_room_members;

-- Make the room-list RPC explicitly verify an active authenticated profile.
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
         where x.room_id=r.id
           and x.status in ('INVITED','JOINED')
           and p.last_seen_at>now()-interval '2 minutes'),
        r.created_at
    from public.rs_video_rooms r
    join public.rs_video_room_members me
      on me.room_id=r.id and me.user_id=(select auth.uid())
    join public.rs_profiles h on h.id=r.host_id
    join public.rs_profiles self on self.id=(select auth.uid())
    where self.active=true
      and r.status='OPEN'
    order by r.created_at desc;
$$;

revoke all on function public.rs_my_video_rooms() from public,anon;
grant execute on function public.rs_my_video_rooms() to authenticated;
