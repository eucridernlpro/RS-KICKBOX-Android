-- RS KICKBOX backend foundation
-- Migration 0039: production cloud member services.

create or replace function public.rs_member_documents_feed()
returns table(
    id uuid,
    title text,
    body text,
    access_tier text,
    active boolean,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select d.id,d.title,d.body,d.access_tier,d.active,d.created_at
    from public.rs_club_documents d
    where
        (select private.rs_is_staff())
        or (
            d.active=true
            and exists(
                select 1
                from public.rs_profiles p
                where p.id=(select auth.uid())
                  and p.active=true
                  and (
                      d.access_tier='ALL'
                      or d.access_tier='BASIC'
                      or (d.access_tier='PRO' and p.plan in ('PRO','ELITE'))
                      or (d.access_tier='ELITE' and p.plan='ELITE')
                  )
            )
        )
    order by d.created_at desc;
$$;

revoke execute on function public.rs_member_documents_feed() from public;
revoke execute on function public.rs_member_documents_feed() from anon;
grant execute on function public.rs_member_documents_feed() to authenticated;

create or replace function public.rs_staff_create_document(
    p_title text,
    p_body text,
    p_access_tier text
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
        raise exception 'title and document required' using errcode='22023';
    end if;
    if p_access_tier not in ('ALL','BASIC','PRO','ELITE') then
        raise exception 'invalid access tier' using errcode='22023';
    end if;

    insert into public.rs_club_documents(title,body,access_tier,active,created_by)
    values(trim(p_title),p_body,p_access_tier,true,(select auth.uid()))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_document(text,text,text) from public;
revoke execute on function public.rs_staff_create_document(text,text,text) from anon;
grant execute on function public.rs_staff_create_document(text,text,text) to authenticated;

create or replace function public.rs_staff_set_document_active(
    p_document_id uuid,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    update public.rs_club_documents
    set active=p_active,updated_at=now()
    where id=p_document_id;
    if not found then raise exception 'document not found' using errcode='P0002'; end if;
end;
$$;

revoke execute on function public.rs_staff_set_document_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_document_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_document_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_document(p_document_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_club_documents where id=p_document_id;
    if not found then raise exception 'document not found' using errcode='P0002'; end if;
end;
$$;

revoke execute on function public.rs_staff_delete_document(uuid) from public;
revoke execute on function public.rs_staff_delete_document(uuid) from anon;
grant execute on function public.rs_staff_delete_document(uuid) to authenticated;

create or replace function public.rs_support_feed()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    subject text,
    message text,
    trainer_reply text,
    status text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        t.id,
        t.student_id,
        p.email,
        p.display_name,
        t.subject,
        t.message,
        t.trainer_reply,
        t.status,
        t.created_at
    from public.rs_support_tickets t
    join public.rs_profiles p on p.id=t.student_id
    where t.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by t.created_at desc;
$$;

revoke execute on function public.rs_support_feed() from public;
revoke execute on function public.rs_support_feed() from anon;
grant execute on function public.rs_support_feed() to authenticated;

create or replace function public.rs_create_support_ticket(
    p_subject text,
    p_message text
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_id uuid;
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if not exists(select 1 from public.rs_profiles p where p.id=v_uid and p.role='student' and p.active=true) then
        raise exception 'active student account required' using errcode='42501';
    end if;
    if trim(p_subject)='' or trim(p_message)='' then
        raise exception 'subject and message required' using errcode='22023';
    end if;

    insert into public.rs_support_tickets(student_id,subject,message)
    values(v_uid,trim(p_subject),trim(p_message))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_create_support_ticket(text,text) from public;
revoke execute on function public.rs_create_support_ticket(text,text) from anon;
grant execute on function public.rs_create_support_ticket(text,text) to authenticated;

create or replace function public.rs_staff_update_support_ticket(
    p_ticket_id uuid,
    p_trainer_reply text,
    p_status text
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    if p_status not in ('OPEN','RESOLVED') then raise exception 'invalid support status' using errcode='22023'; end if;

    update public.rs_support_tickets
    set trainer_reply=coalesce(p_trainer_reply,''),
        status=p_status,
        updated_at=now()
    where id=p_ticket_id;

    if not found then raise exception 'support ticket not found' using errcode='P0002'; end if;
end;
$$;

revoke execute on function public.rs_staff_update_support_ticket(uuid,text,text) from public;
revoke execute on function public.rs_staff_update_support_ticket(uuid,text,text) from anon;
grant execute on function public.rs_staff_update_support_ticket(uuid,text,text) to authenticated;

create or replace function public.rs_staff_delete_support_ticket(p_ticket_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_support_tickets where id=p_ticket_id;
end;
$$;

revoke execute on function public.rs_staff_delete_support_ticket(uuid) from public;
revoke execute on function public.rs_staff_delete_support_ticket(uuid) from anon;
grant execute on function public.rs_staff_delete_support_ticket(uuid) to authenticated;

create or replace function public.rs_referral_feed()
returns table(
    id uuid,
    owner_id uuid,
    owner_email text,
    owner_name text,
    code text,
    uses integer,
    active boolean
)
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_code text;
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;

    if not (select private.rs_is_staff()) and not exists(
        select 1 from public.rs_referrals r where r.owner_id=v_uid
    ) then
        v_code:='RS'||upper(substr(replace(extensions.gen_random_uuid()::text,'-',''),1,7));
        insert into public.rs_referrals(owner_id,code,uses,active)
        values(v_uid,v_code,0,true)
        on conflict(owner_id) do nothing;
    end if;

    return query
    select r.id,r.owner_id,p.email,p.display_name,r.code,r.uses,r.active
    from public.rs_referrals r
    join public.rs_profiles p on p.id=r.owner_id
    where r.owner_id=v_uid
       or (select private.rs_is_staff())
    order by lower(p.display_name),lower(p.email);
end;
$$;

revoke execute on function public.rs_referral_feed() from public;
revoke execute on function public.rs_referral_feed() from anon;
grant execute on function public.rs_referral_feed() to authenticated;

create or replace function public.rs_staff_set_referral_active(
    p_referral_id uuid,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    update public.rs_referrals set active=p_active where id=p_referral_id;
    if not found then raise exception 'referral not found' using errcode='P0002'; end if;
end;
$$;

revoke execute on function public.rs_staff_set_referral_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_referral_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_referral_active(uuid,boolean) to authenticated;
