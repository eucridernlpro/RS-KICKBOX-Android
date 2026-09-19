-- RS KICKBOX backend foundation
-- Migration 0035: production social profiles, community and groups.

create or replace function public.rs_my_social_profile()
returns table(
    user_id uuid,
    email text,
    display_name text,
    bio text,
    training_goal text,
    public_profile boolean
)
language sql
stable
security definer
set search_path=''
as $$
    select
        p.id,
        p.email,
        coalesce(s.display_name,p.display_name),
        coalesce(s.bio,''),
        coalesce(s.training_goal,''),
        coalesce(s.public_profile,false)
    from public.rs_profiles p
    left join public.rs_social_profiles s on s.user_id=p.id
    where p.id=(select auth.uid())
    limit 1;
$$;

revoke execute on function public.rs_my_social_profile() from public;
revoke execute on function public.rs_my_social_profile() from anon;
grant execute on function public.rs_my_social_profile() to authenticated;

create or replace function public.rs_save_my_social_profile(
    p_display_name text,
    p_bio text,
    p_training_goal text,
    p_public_profile boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare v_uid uuid := (select auth.uid());
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;
    if trim(p_display_name)='' then
        raise exception 'display name required' using errcode='22023';
    end if;

    insert into public.rs_social_profiles(
        user_id,display_name,bio,training_goal,public_profile,updated_at
    )
    values(
        v_uid,
        left(trim(p_display_name),80),
        left(coalesce(p_bio,''),500),
        left(coalesce(p_training_goal,''),300),
        p_public_profile,
        now()
    )
    on conflict(user_id) do update
    set display_name=excluded.display_name,
        bio=excluded.bio,
        training_goal=excluded.training_goal,
        public_profile=excluded.public_profile,
        updated_at=now();

    update public.rs_profiles
    set display_name=left(trim(p_display_name),80),updated_at=now()
    where id=v_uid;
end;
$$;

revoke execute on function public.rs_save_my_social_profile(text,text,text,boolean) from public;
revoke execute on function public.rs_save_my_social_profile(text,text,text,boolean) from anon;
grant execute on function public.rs_save_my_social_profile(text,text,text,boolean) to authenticated;

create or replace function public.rs_community_feed()
returns table(
    id uuid,
    author_id uuid,
    author_email text,
    author_name text,
    body text,
    active boolean,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        c.id,c.author_id,p.email,
        coalesce(s.display_name,p.display_name),
        c.body,c.active,c.created_at
    from public.rs_community_posts c
    join public.rs_profiles p on p.id=c.author_id
    left join public.rs_social_profiles s on s.user_id=c.author_id
    where c.active=true
       or c.author_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by c.created_at desc;
$$;

revoke execute on function public.rs_community_feed() from public;
revoke execute on function public.rs_community_feed() from anon;
grant execute on function public.rs_community_feed() to authenticated;

create or replace function public.rs_create_community_post(p_body text)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_uid uuid := (select auth.uid()); v_id uuid; v_enabled boolean;
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if length(trim(p_body))<1 or length(trim(p_body))>1000 then
        raise exception 'post must contain 1 to 1000 characters' using errcode='22023';
    end if;

    select s.community_posts_enabled into v_enabled
    from public.rs_app_settings s where s.singleton=true;
    if coalesce(v_enabled,true)=false then
        raise exception 'community posting disabled' using errcode='42501';
    end if;

    insert into public.rs_community_posts(author_id,body,active)
    values(v_uid,trim(p_body),true)
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_create_community_post(text) from public;
revoke execute on function public.rs_create_community_post(text) from anon;
grant execute on function public.rs_create_community_post(text) to authenticated;

create or replace function public.rs_delete_community_post(p_post_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    delete from public.rs_community_posts c
    where c.id=p_post_id
      and (c.author_id=(select auth.uid()) or (select private.rs_is_staff()));
    if not found then raise exception 'post not found or not permitted' using errcode='42501'; end if;
end;
$$;

revoke execute on function public.rs_delete_community_post(uuid) from public;
revoke execute on function public.rs_delete_community_post(uuid) from anon;
grant execute on function public.rs_delete_community_post(uuid) to authenticated;

create or replace function public.rs_staff_set_community_post_active(p_post_id uuid,p_active boolean)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    update public.rs_community_posts set active=p_active where id=p_post_id;
end;
$$;

revoke execute on function public.rs_staff_set_community_post_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_community_post_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_community_post_active(uuid,boolean) to authenticated;

create or replace function public.rs_group_catalog()
returns table(
    id uuid,
    name text,
    description text,
    active boolean,
    joined boolean,
    member_count integer
)
language sql
stable
security definer
set search_path=''
as $$
    select
        g.id,g.name,g.description,g.active,
        exists(
            select 1 from public.rs_group_memberships m
            where m.group_id=g.id and m.student_id=(select auth.uid())
        ),
        (
            select count(*)::integer
            from public.rs_group_memberships m2
            where m2.group_id=g.id
        )
    from public.rs_groups g
    where g.active=true or (select private.rs_is_staff())
    order by g.created_at desc;
$$;

revoke execute on function public.rs_group_catalog() from public;
revoke execute on function public.rs_group_catalog() from anon;
grant execute on function public.rs_group_catalog() to authenticated;

create or replace function public.rs_set_my_group_membership(p_group_id uuid,p_join boolean)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare v_uid uuid := (select auth.uid());
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if p_join then
        if not exists(select 1 from public.rs_groups g where g.id=p_group_id and g.active=true) then
            raise exception 'group unavailable' using errcode='P0002';
        end if;
        insert into public.rs_group_memberships(group_id,student_id)
        values(p_group_id,v_uid)
        on conflict(group_id,student_id) do nothing;
    else
        delete from public.rs_group_memberships where group_id=p_group_id and student_id=v_uid;
    end if;
end;
$$;

revoke execute on function public.rs_set_my_group_membership(uuid,boolean) from public;
revoke execute on function public.rs_set_my_group_membership(uuid,boolean) from anon;
grant execute on function public.rs_set_my_group_membership(uuid,boolean) to authenticated;

create or replace function public.rs_staff_create_group(p_name text,p_description text)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    insert into public.rs_groups(name,description,active,created_by)
    values(left(trim(p_name),80),left(coalesce(p_description,''),500),true,(select auth.uid()))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_group(text,text) from public;
revoke execute on function public.rs_staff_create_group(text,text) from anon;
grant execute on function public.rs_staff_create_group(text,text) to authenticated;

create or replace function public.rs_staff_set_group_active(p_group_id uuid,p_active boolean)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    update public.rs_groups set active=p_active where id=p_group_id;
end;
$$;

revoke execute on function public.rs_staff_set_group_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_group_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_group_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_group(p_group_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_groups where id=p_group_id;
end;
$$;

revoke execute on function public.rs_staff_delete_group(uuid) from public;
revoke execute on function public.rs_staff_delete_group(uuid) from anon;
grant execute on function public.rs_staff_delete_group(uuid) to authenticated;
