-- RS KICKBOX backend foundation
-- Migration 0026: production cloud notification center.

create or replace function public.rs_notification_feed()
returns table (
    id uuid,
    title text,
    message text,
    audience text,
    created_at timestamptz,
    read boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        n.id,
        n.title,
        n.message,
        n.audience,
        n.created_at,
        exists(
            select 1
            from public.rs_notification_reads r
            where r.notification_id=n.id
              and r.user_id=(select auth.uid())
        ) as read
    from public.rs_notifications n
    left join public.rs_profiles p on p.id=(select auth.uid())
    where n.active=true
      and (
          (select private.rs_is_staff())
          or n.audience='ALL'
          or (
              p.active=true
              and p.role='student'
              and n.audience=p.plan
          )
      )
    order by n.created_at desc;
$$;

revoke execute on function public.rs_notification_feed() from public;
revoke execute on function public.rs_notification_feed() from anon;
grant execute on function public.rs_notification_feed() to authenticated;

create or replace function public.rs_unread_notification_count()
returns integer
language sql
stable
security definer
set search_path = ''
as $$
    select count(*)::integer
    from public.rs_notifications n
    join public.rs_profiles p on p.id=(select auth.uid())
    where n.active=true
      and (
          (select private.rs_is_staff())
          or n.audience='ALL'
          or (
              p.active=true
              and p.role='student'
              and n.audience=p.plan
          )
      )
      and not exists(
          select 1
          from public.rs_notification_reads r
          where r.notification_id=n.id
            and r.user_id=(select auth.uid())
      );
$$;

revoke execute on function public.rs_unread_notification_count() from public;
revoke execute on function public.rs_unread_notification_count() from anon;
grant execute on function public.rs_unread_notification_count() to authenticated;

create or replace function public.rs_mark_notification_read(p_notification_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if not exists(
        select 1
        from public.rs_notifications n
        left join public.rs_profiles p on p.id=(select auth.uid())
        where n.id=p_notification_id
          and n.active=true
          and (
              (select private.rs_is_staff())
              or n.audience='ALL'
              or (
                  p.active=true
                  and p.role='student'
                  and n.audience=p.plan
              )
          )
    ) then
        raise exception 'notification not available' using errcode='P0002';
    end if;

    insert into public.rs_notification_reads(notification_id,user_id,read_at)
    values(p_notification_id,(select auth.uid()),now())
    on conflict (notification_id,user_id) do update
    set read_at=excluded.read_at;
end;
$$;

revoke execute on function public.rs_mark_notification_read(uuid) from public;
revoke execute on function public.rs_mark_notification_read(uuid) from anon;
grant execute on function public.rs_mark_notification_read(uuid) to authenticated;

create or replace function public.rs_staff_create_notification(
    p_title text,
    p_message text,
    p_audience text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if trim(p_title)='' or trim(p_message)='' then
        raise exception 'title and message required' using errcode='22023';
    end if;

    if p_audience not in ('ALL','BASIC','PRO','ELITE') then
        raise exception 'invalid audience' using errcode='22023';
    end if;

    insert into public.rs_notifications(
        title,message,audience,created_by,active
    )
    values(
        trim(p_title),
        trim(p_message),
        p_audience,
        (select auth.uid()),
        true
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_notification(text,text,text) from public;
revoke execute on function public.rs_staff_create_notification(text,text,text) from anon;
grant execute on function public.rs_staff_create_notification(text,text,text) to authenticated;

create or replace function public.rs_staff_delete_notification(p_notification_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_notifications
    where id=p_notification_id;

    if not found then
        raise exception 'notification not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_notification(uuid) from public;
revoke execute on function public.rs_staff_delete_notification(uuid) from anon;
grant execute on function public.rs_staff_delete_notification(uuid) to authenticated;
