-- RS KICKBOXING backend foundation
-- Migration 0046: private-lesson plans, storage cleanup and trainer chat administration.
-- Additive only. Existing messages, bookings and media continue to work.

alter table public.rs_student_media_assets
    add column if not exists storage_bucket text not null default 'rs-student-media';

create index if not exists rs_student_media_assets_cleanup_idx
on public.rs_student_media_assets(student_id,byte_size desc,created_at asc);

create or replace function public.rs_register_student_media_asset(
    p_student_id uuid,
    p_storage_bucket text,
    p_storage_path text,
    p_media_kind text,
    p_source_area text,
    p_byte_size bigint
) returns uuid
language plpgsql security definer set search_path=''
as $$
declare v_uid uuid:=(select auth.uid()); v_id uuid;
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if v_uid<>p_student_id and not (select private.rs_is_staff()) then
        raise exception 'student or staff access required' using errcode='42501';
    end if;
    if p_media_kind not in ('VIDEO','IMAGE','GIF','OTHER') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;
    if p_source_area not in ('HOMEWORK','TECHNIQUE','PRIVATE_LESSON','CHAT','OTHER') then
        raise exception 'invalid source area' using errcode='22023';
    end if;
    if trim(coalesce(p_storage_bucket,''))='' or trim(coalesce(p_storage_path,''))='' then
        raise exception 'storage location required' using errcode='22023';
    end if;

    insert into public.rs_student_media_assets(
        student_id,storage_bucket,storage_path,media_kind,source_area,byte_size,created_by
    )
    values(
        p_student_id,trim(p_storage_bucket),trim(p_storage_path),p_media_kind,p_source_area,
        greatest(0,coalesce(p_byte_size,0)),v_uid
    )
    on conflict(storage_path) do update set
        storage_bucket=excluded.storage_bucket,
        media_kind=excluded.media_kind,
        source_area=excluded.source_area,
        byte_size=excluded.byte_size
    returning id into v_id;
    return v_id;
end;
$$;

create or replace function public.rs_staff_student_media_assets(p_student_id uuid)
returns table(
    id uuid,
    student_id uuid,
    storage_bucket text,
    storage_path text,
    media_kind text,
    source_area text,
    byte_size bigint,
    created_at timestamptz
)
language sql stable security definer set search_path=''
as $$
    select a.id,a.student_id,a.storage_bucket,a.storage_path,a.media_kind,a.source_area,a.byte_size,a.created_at
    from public.rs_student_media_assets a
    where a.student_id=p_student_id and (select private.rs_is_staff())
    order by a.byte_size desc,a.created_at asc;
$$;

create or replace function public.rs_staff_remove_student_media_asset(p_asset_id uuid)
returns table(storage_bucket text,storage_path text)
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    return query
    delete from public.rs_student_media_assets a
    where a.id=p_asset_id
    returning a.storage_bucket,a.storage_path;
end;
$$;

create table if not exists public.rs_private_lesson_training (
    booking_id uuid primary key references public.rs_private_bookings(id) on delete cascade,
    template_id uuid not null references public.rs_training_templates(id) on delete restrict,
    homework_id uuid references public.rs_homework(id) on delete set null,
    assigned_by uuid not null references auth.users(id) on delete restrict,
    assigned_at timestamptz not null default now()
);

alter table public.rs_private_lesson_training enable row level security;
revoke all on public.rs_private_lesson_training from anon,authenticated;
grant select,insert,update,delete on public.rs_private_lesson_training to authenticated;

drop policy if exists "rs_private_lesson_training_select" on public.rs_private_lesson_training;
create policy "rs_private_lesson_training_select" on public.rs_private_lesson_training
for select to authenticated using (
    (select private.rs_is_staff())
    or exists(
        select 1 from public.rs_private_bookings b
        where b.id=booking_id and b.student_id=(select auth.uid())
    )
);

drop policy if exists "rs_private_lesson_training_staff_write" on public.rs_private_lesson_training;
create policy "rs_private_lesson_training_staff_write" on public.rs_private_lesson_training
for all to authenticated using ((select private.rs_is_staff())) with check ((select private.rs_is_staff()));

create or replace function public.rs_staff_assign_private_lesson_training(
    p_booking_id uuid,p_template_id uuid,p_due_label text default ''
) returns uuid
language plpgsql security definer set search_path=''
as $$
declare v_student_id uuid; v_homework_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    select b.student_id into v_student_id
    from public.rs_private_bookings b
    where b.id=p_booking_id and b.status='confirmed';
    if v_student_id is null then
        raise exception 'confirmed private lesson required' using errcode='P0002';
    end if;

    select public.rs_staff_assign_training_template(v_student_id,p_template_id,coalesce(p_due_label,''))
    into v_homework_id;

    insert into public.rs_private_lesson_training(booking_id,template_id,homework_id,assigned_by,assigned_at)
    values(p_booking_id,p_template_id,v_homework_id,(select auth.uid()),now())
    on conflict(booking_id) do update set
        template_id=excluded.template_id,
        homework_id=excluded.homework_id,
        assigned_by=excluded.assigned_by,
        assigned_at=now();

    return v_homework_id;
end;
$$;

create or replace function public.rs_private_lesson_training_feed()
returns table(
    booking_id uuid,
    template_id uuid,
    template_title text,
    homework_id uuid,
    assigned_at timestamptz
)
language sql stable security definer set search_path=''
as $$
    select x.booking_id,x.template_id,t.title,x.homework_id,x.assigned_at
    from public.rs_private_lesson_training x
    join public.rs_private_bookings b on b.id=x.booking_id
    join public.rs_training_templates t on t.id=x.template_id
    where (select private.rs_is_staff()) or b.student_id=(select auth.uid())
    order by x.assigned_at desc;
$$;

create or replace function public.rs_staff_edit_coach_message(p_message_id uuid,p_body text)
returns void
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    if length(trim(coalesce(p_body,'')))>1200 then raise exception 'message too long' using errcode='22023'; end if;
    update public.rs_coach_messages
    set body=trim(coalesce(p_body,''))
    where id=p_message_id
      and (trim(coalesce(p_body,''))<>'' or media_path is not null);
    if not found then raise exception 'message not found or empty message not allowed' using errcode='P0002'; end if;
end;
$$;

create or replace function public.rs_staff_delete_coach_message(p_message_id uuid)
returns text
language plpgsql security definer set search_path=''
as $$
declare v_path text;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_coach_messages where id=p_message_id returning media_path into v_path;
    if not found then raise exception 'message not found' using errcode='P0002'; end if;
    return v_path;
end;
$$;

create or replace function public.rs_staff_clear_coach_thread(p_student_id uuid)
returns table(media_path text)
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    return query
    delete from public.rs_coach_messages m
    where m.student_id=p_student_id
    returning m.media_path;
end;
$$;

create or replace function public.rs_staff_edit_group_message(p_message_id uuid,p_body text)
returns void
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    if length(trim(coalesce(p_body,'')))>1200 then raise exception 'message too long' using errcode='22023'; end if;
    update public.rs_group_messages
    set body=trim(coalesce(p_body,''))
    where id=p_message_id
      and (trim(coalesce(p_body,''))<>'' or media_path is not null);
    if not found then raise exception 'message not found or empty message not allowed' using errcode='P0002'; end if;
end;
$$;

create or replace function public.rs_staff_delete_group_message(p_message_id uuid)
returns text
language plpgsql security definer set search_path=''
as $$
declare v_path text;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_group_messages where id=p_message_id returning media_path into v_path;
    if not found then raise exception 'message not found' using errcode='P0002'; end if;
    return v_path;
end;
$$;

create or replace function public.rs_staff_clear_group_chat(p_group_id uuid)
returns table(media_path text)
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    return query
    delete from public.rs_group_messages m
    where m.group_id=p_group_id
    returning m.media_path;
end;
$$;

revoke execute on function public.rs_register_student_media_asset(uuid,text,text,text,text,bigint) from public,anon;
revoke execute on function public.rs_staff_student_media_assets(uuid) from public,anon;
revoke execute on function public.rs_staff_remove_student_media_asset(uuid) from public,anon;
revoke execute on function public.rs_staff_assign_private_lesson_training(uuid,uuid,text) from public,anon;
revoke execute on function public.rs_private_lesson_training_feed() from public,anon;
revoke execute on function public.rs_staff_edit_coach_message(uuid,text) from public,anon;
revoke execute on function public.rs_staff_delete_coach_message(uuid) from public,anon;
revoke execute on function public.rs_staff_clear_coach_thread(uuid) from public,anon;
revoke execute on function public.rs_staff_edit_group_message(uuid,text) from public,anon;
revoke execute on function public.rs_staff_delete_group_message(uuid) from public,anon;
revoke execute on function public.rs_staff_clear_group_chat(uuid) from public,anon;

grant execute on function public.rs_register_student_media_asset(uuid,text,text,text,text,bigint) to authenticated;
grant execute on function public.rs_staff_student_media_assets(uuid) to authenticated;
grant execute on function public.rs_staff_remove_student_media_asset(uuid) to authenticated;
grant execute on function public.rs_staff_assign_private_lesson_training(uuid,uuid,text) to authenticated;
grant execute on function public.rs_private_lesson_training_feed() to authenticated;
grant execute on function public.rs_staff_edit_coach_message(uuid,text) to authenticated;
grant execute on function public.rs_staff_delete_coach_message(uuid) to authenticated;
grant execute on function public.rs_staff_clear_coach_thread(uuid) to authenticated;
grant execute on function public.rs_staff_edit_group_message(uuid,text) to authenticated;
grant execute on function public.rs_staff_delete_group_message(uuid) to authenticated;
grant execute on function public.rs_staff_clear_group_chat(uuid) to authenticated;
