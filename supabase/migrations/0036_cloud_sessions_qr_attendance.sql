-- RS KICKBOX backend foundation
-- Migration 0036: cloud training sessions and secure QR attendance.

create or replace function public.rs_training_session_catalog()
returns table(
    id uuid,
    title text,
    active boolean,
    blocks jsonb
)
language sql
stable
security definer
set search_path=''
as $$
    select
        s.id,
        s.title,
        s.active,
        coalesce(
            (
                select jsonb_agg(
                    jsonb_build_object(
                        'id',b.id,
                        'title',b.title,
                        'seconds',b.duration_seconds,
                        'instructions',b.instructions
                    )
                    order by b.sort_order
                )
                from public.rs_training_session_blocks b
                where b.session_id=s.id
            ),
            '[]'::jsonb
        ) as blocks
    from public.rs_training_sessions s
    where s.active=true
       or (select private.rs_is_staff())
    order by s.active desc,s.updated_at desc;
$$;

revoke execute on function public.rs_training_session_catalog() from public;
revoke execute on function public.rs_training_session_catalog() from anon;
grant execute on function public.rs_training_session_catalog() to authenticated;

create or replace function public.rs_staff_create_training_session(
    p_title text,
    p_blocks jsonb
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare
    v_id uuid;
    v_block jsonb;
    v_order integer:=0;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if trim(p_title)='' then
        raise exception 'session title required' using errcode='22023';
    end if;
    if jsonb_typeof(p_blocks)<>'array' or jsonb_array_length(p_blocks)<1 then
        raise exception 'at least one session block required' using errcode='22023';
    end if;

    update public.rs_training_sessions
    set active=false,updated_at=now()
    where active=true;

    insert into public.rs_training_sessions(title,active,created_by)
    values(left(trim(p_title),100),true,(select auth.uid()))
    returning id into v_id;

    for v_block in select value from jsonb_array_elements(p_blocks)
    loop
        insert into public.rs_training_session_blocks(
            session_id,sort_order,title,duration_seconds,instructions
        )
        values(
            v_id,
            v_order,
            left(coalesce(v_block->>'title','Block'),80),
            greatest(10,least(3600,coalesce((v_block->>'seconds')::integer,180))),
            left(coalesce(v_block->>'instructions',''),1000)
        );
        v_order:=v_order+1;
    end loop;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_training_session(text,jsonb) from public;
revoke execute on function public.rs_staff_create_training_session(text,jsonb) from anon;
grant execute on function public.rs_staff_create_training_session(text,jsonb) to authenticated;

create or replace function public.rs_staff_set_training_session_active(
    p_session_id uuid,
    p_active boolean
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

    if p_active then
        update public.rs_training_sessions
        set active=false,updated_at=now()
        where active=true and id<>p_session_id;
    end if;

    update public.rs_training_sessions
    set active=p_active,updated_at=now()
    where id=p_session_id;

    if not found then
        raise exception 'training session not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_training_session_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_training_session_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_training_session_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_training_session(p_session_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    delete from public.rs_training_sessions where id=p_session_id;
    if not found then
        raise exception 'training session not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_training_session(uuid) from public;
revoke execute on function public.rs_staff_delete_training_session(uuid) from anon;
grant execute on function public.rs_staff_delete_training_session(uuid) to authenticated;

create or replace function public.rs_staff_create_attendance_qr(
    p_class_id uuid,
    p_valid_minutes integer default 15
)
returns table(
    class_id uuid,
    token text,
    expires_at timestamptz
)
language plpgsql
security definer
set search_path=''
as $$
declare
    v_token text;
    v_hash text;
    v_expiry timestamptz;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if not exists(select 1 from public.rs_classes c where c.id=p_class_id and c.active=true) then
        raise exception 'active class not found' using errcode='P0002';
    end if;

    update private.rs_attendance_tokens
    set revoked_at=now()
    where class_id=p_class_id
      and revoked_at is null
      and expires_at>now();

    v_token:=encode(extensions.gen_random_bytes(32),'hex');
    v_hash:=encode(extensions.digest(v_token,'sha256'),'hex');
    v_expiry:=now()+make_interval(mins=>greatest(2,least(60,p_valid_minutes)));

    insert into private.rs_attendance_tokens(
        class_id,token_hash,created_by,expires_at
    )
    values(
        p_class_id,v_hash,(select auth.uid()),v_expiry
    );

    return query select p_class_id,v_token,v_expiry;
end;
$$;

revoke execute on function public.rs_staff_create_attendance_qr(uuid,integer) from public;
revoke execute on function public.rs_staff_create_attendance_qr(uuid,integer) from anon;
grant execute on function public.rs_staff_create_attendance_qr(uuid,integer) to authenticated;

create or replace function public.rs_student_attendance_checkin(
    p_class_id uuid,
    p_token text
)
returns text
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_hash text;
    v_token_id uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;
    if not exists(
        select 1 from public.rs_profiles p
        where p.id=v_uid and p.role='student' and p.active=true
    ) then
        raise exception 'active student account required' using errcode='42501';
    end if;

    v_hash:=encode(extensions.digest(p_token,'sha256'),'hex');

    select t.id into v_token_id
    from private.rs_attendance_tokens t
    where t.class_id=p_class_id
      and t.token_hash=v_hash
      and t.revoked_at is null
      and t.expires_at>now()
    limit 1;

    if v_token_id is null then
        raise exception 'invalid or expired attendance QR' using errcode='P0001';
    end if;

    insert into public.rs_attendance(
        class_id,student_id,present,checked_in_at,checked_in_by,note
    )
    values(
        p_class_id,v_uid,true,now(),v_uid,'Student QR check-in'
    )
    on conflict(class_id,student_id) do update
    set present=true,
        checked_in_at=now(),
        checked_in_by=v_uid,
        note='Student QR check-in';

    return 'checked_in';
end;
$$;

revoke execute on function public.rs_student_attendance_checkin(uuid,text) from public;
revoke execute on function public.rs_student_attendance_checkin(uuid,text) from anon;
grant execute on function public.rs_student_attendance_checkin(uuid,text) to authenticated;
