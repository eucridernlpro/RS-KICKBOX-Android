-- RS KICKBOX backend foundation
-- Migration 0031: production cloud content library actions.

create or replace function public.rs_content_catalog()
returns table(
    id uuid,
    title text,
    category text,
    body text,
    access_tier text,
    published boolean,
    favorite boolean,
    last_opened_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        c.id,
        c.title,
        c.category,
        c.body,
        c.access_tier,
        c.published,
        exists(
            select 1
            from public.rs_content_favorites f
            where f.user_id=(select auth.uid())
              and f.content_id=c.id
        ) as favorite,
        (
            select max(h.opened_at)
            from public.rs_content_history h
            where h.user_id=(select auth.uid())
              and h.content_id=c.id
        ) as last_opened_at
    from public.rs_content c
    where
        (select private.rs_is_staff())
        or (
            c.published=true
            and exists(
                select 1
                from public.rs_profiles p
                where p.id=(select auth.uid())
                  and p.active=true
                  and (
                      c.access_tier='ALL'
                      or c.access_tier='BASIC'
                      or (c.access_tier='PRO' and p.plan in ('PRO','ELITE'))
                      or (c.access_tier='ELITE' and p.plan='ELITE')
                  )
            )
        )
    order by c.updated_at desc,c.created_at desc;
$$;

revoke execute on function public.rs_content_catalog() from public;
revoke execute on function public.rs_content_catalog() from anon;
grant execute on function public.rs_content_catalog() to authenticated;

create or replace function public.rs_toggle_content_favorite(
    p_content_id uuid,
    p_favorite boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if p_favorite then
        insert into public.rs_content_favorites(user_id,content_id)
        values(v_uid,p_content_id)
        on conflict(user_id,content_id) do nothing;
    else
        delete from public.rs_content_favorites
        where user_id=v_uid and content_id=p_content_id;
    end if;
end;
$$;

revoke execute on function public.rs_toggle_content_favorite(uuid,boolean) from public;
revoke execute on function public.rs_toggle_content_favorite(uuid,boolean) from anon;
grant execute on function public.rs_toggle_content_favorite(uuid,boolean) to authenticated;

create or replace function public.rs_record_content_open(p_content_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    insert into public.rs_content_history(user_id,content_id,opened_at)
    values(v_uid,p_content_id,now());

    delete from public.rs_content_history h
    where h.user_id=v_uid
      and h.id not in(
          select x.id
          from public.rs_content_history x
          where x.user_id=v_uid
          order by x.opened_at desc
          limit 100
      );
end;
$$;

revoke execute on function public.rs_record_content_open(uuid) from public;
revoke execute on function public.rs_record_content_open(uuid) from anon;
grant execute on function public.rs_record_content_open(uuid) to authenticated;

create or replace function public.rs_staff_create_content(
    p_title text,
    p_category text,
    p_body text,
    p_access_tier text,
    p_published boolean
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if trim(p_title)='' or trim(p_body)='' then
        raise exception 'title and content required' using errcode='22023';
    end if;
    if p_access_tier not in ('ALL','BASIC','PRO','ELITE') then
        raise exception 'invalid access tier' using errcode='22023';
    end if;

    insert into public.rs_content(
        title,category,body,access_tier,published,created_by
    )
    values(
        trim(p_title),
        coalesce(nullif(trim(p_category),''),'TECHNIQUE'),
        p_body,
        p_access_tier,
        p_published,
        (select auth.uid())
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_content(text,text,text,text,boolean) from public;
revoke execute on function public.rs_staff_create_content(text,text,text,text,boolean) from anon;
grant execute on function public.rs_staff_create_content(text,text,text,text,boolean) to authenticated;

create or replace function public.rs_staff_set_content_published(
    p_content_id uuid,
    p_published boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    update public.rs_content
    set published=p_published,updated_at=now()
    where id=p_content_id;

    if not found then
        raise exception 'content not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_content_published(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_content_published(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_content_published(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_content(p_content_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_content where id=p_content_id;
    if not found then
        raise exception 'content not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_content(uuid) from public;
revoke execute on function public.rs_staff_delete_content(uuid) from anon;
grant execute on function public.rs_staff_delete_content(uuid) to authenticated;
