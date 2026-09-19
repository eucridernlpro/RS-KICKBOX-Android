-- RS KICKBOXING v0.113 cumulative Supabase update
-- Combines migrations 0045 + 0046 + 0047 in required order.
-- Intended for projects where these three migrations are not yet confirmed applied.
-- Existing migrations were written to be repeat-safe where practical; review Supabase SQL output before treating backend changes as live.

-- ============================================================================
-- SOURCE: supabase/migrations/0045_training_plans_storage.sql
-- ============================================================================

-- RS KICKBOXING backend foundation
-- Migration 0045: reusable training plans, homework media references and student storage accounting.
-- Additive only. Existing homework/training media/chat flows remain compatible.

-- Visible brand rename only. Preserve any custom trainer-set brand name.
update public.rs_brand_settings
set header_name='RS KICKBOXING', updated_at=now()
where header_name='RS KICKBOX';

alter table public.rs_training_media
    add column if not exists technique_tags text[] not null default '{}',
    add column if not exists ai_reference boolean not null default false;

create index if not exists rs_training_media_ai_reference_idx
on public.rs_training_media(ai_reference,published);

create table if not exists public.rs_training_templates (
    id uuid primary key default gen_random_uuid(),
    title text not null check (char_length(trim(title)) between 1 and 120),
    description text not null default '',
    category text not null default 'GENERAL',
    active boolean not null default true,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.rs_training_template_steps (
    id uuid primary key default gen_random_uuid(),
    template_id uuid not null references public.rs_training_templates(id) on delete cascade,
    step_order integer not null default 1 check(step_order between 1 and 200),
    title text not null check (char_length(trim(title)) between 1 and 120),
    instructions text not null default '',
    sets_reps_time text not null default '',
    media_id uuid references public.rs_training_media(id) on delete set null,
    created_at timestamptz not null default now()
);

create index if not exists rs_training_template_steps_order_idx
on public.rs_training_template_steps(template_id,step_order,id);

create table if not exists public.rs_homework_steps (
    id uuid primary key default gen_random_uuid(),
    homework_id uuid not null references public.rs_homework(id) on delete cascade,
    step_order integer not null default 1 check(step_order between 1 and 200),
    title text not null check (char_length(trim(title)) between 1 and 120),
    instructions text not null default '',
    sets_reps_time text not null default '',
    media_id uuid references public.rs_training_media(id) on delete set null,
    completed boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_homework_steps_order_idx
on public.rs_homework_steps(homework_id,step_order,id);

alter table public.rs_training_templates enable row level security;
alter table public.rs_training_template_steps enable row level security;
alter table public.rs_homework_steps enable row level security;

revoke all on public.rs_training_templates, public.rs_training_template_steps, public.rs_homework_steps from anon;
revoke all on public.rs_training_templates, public.rs_training_template_steps, public.rs_homework_steps from authenticated;
grant select,insert,update,delete on public.rs_training_templates, public.rs_training_template_steps, public.rs_homework_steps to authenticated;

drop policy if exists "rs_training_templates_staff_all" on public.rs_training_templates;
create policy "rs_training_templates_staff_all" on public.rs_training_templates
for all to authenticated using ((select private.rs_is_staff())) with check ((select private.rs_is_staff()));

drop policy if exists "rs_training_template_steps_staff_all" on public.rs_training_template_steps;
create policy "rs_training_template_steps_staff_all" on public.rs_training_template_steps
for all to authenticated using ((select private.rs_is_staff())) with check ((select private.rs_is_staff()));

drop policy if exists "rs_homework_steps_select_own_or_staff" on public.rs_homework_steps;
create policy "rs_homework_steps_select_own_or_staff" on public.rs_homework_steps
for select to authenticated using (
    (select private.rs_is_staff())
    or exists(
        select 1 from public.rs_homework h
        where h.id=homework_id and h.student_id=(select auth.uid())
    )
);

drop policy if exists "rs_homework_steps_staff_insert" on public.rs_homework_steps;
create policy "rs_homework_steps_staff_insert" on public.rs_homework_steps
for insert to authenticated with check ((select private.rs_is_staff()));

drop policy if exists "rs_homework_steps_update_own_or_staff" on public.rs_homework_steps;
create policy "rs_homework_steps_update_own_or_staff" on public.rs_homework_steps
for update to authenticated
using (
    (select private.rs_is_staff())
    or exists(
        select 1 from public.rs_homework h
        where h.id=homework_id and h.student_id=(select auth.uid())
    )
)
with check (
    (select private.rs_is_staff())
    or exists(
        select 1 from public.rs_homework h
        where h.id=homework_id and h.student_id=(select auth.uid())
    )
);

drop policy if exists "rs_homework_steps_staff_delete" on public.rs_homework_steps;
create policy "rs_homework_steps_staff_delete" on public.rs_homework_steps
for delete to authenticated using ((select private.rs_is_staff()));

create table if not exists public.rs_student_storage_limits (
    student_id uuid primary key references auth.users(id) on delete cascade,
    limit_bytes bigint not null default 52428800 check(limit_bytes between 1048576 and 10737418240),
    warning_message text not null default '',
    updated_by uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);

create table if not exists public.rs_student_media_assets (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null references auth.users(id) on delete cascade,
    storage_path text not null unique,
    media_kind text not null check(media_kind in ('VIDEO','IMAGE','GIF','OTHER')),
    source_area text not null default 'OTHER'
        check(source_area in ('HOMEWORK','TECHNIQUE','PRIVATE_LESSON','CHAT','OTHER')),
    byte_size bigint not null default 0 check(byte_size >= 0),
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now()
);

create index if not exists rs_student_media_assets_student_idx
on public.rs_student_media_assets(student_id,created_at desc);

alter table public.rs_student_storage_limits enable row level security;
alter table public.rs_student_media_assets enable row level security;

revoke all on public.rs_student_storage_limits, public.rs_student_media_assets from anon;
revoke all on public.rs_student_storage_limits, public.rs_student_media_assets from authenticated;
grant select,insert,update,delete on public.rs_student_storage_limits, public.rs_student_media_assets to authenticated;

drop policy if exists "rs_student_storage_limits_staff_only" on public.rs_student_storage_limits;
create policy "rs_student_storage_limits_staff_only" on public.rs_student_storage_limits
for all to authenticated using ((select private.rs_is_staff())) with check ((select private.rs_is_staff()));

drop policy if exists "rs_student_media_assets_select" on public.rs_student_media_assets;
create policy "rs_student_media_assets_select" on public.rs_student_media_assets
for select to authenticated using (
    student_id=(select auth.uid()) or (select private.rs_is_staff())
);

drop policy if exists "rs_student_media_assets_insert" on public.rs_student_media_assets;
create policy "rs_student_media_assets_insert" on public.rs_student_media_assets
for insert to authenticated with check (
    student_id=(select auth.uid()) or (select private.rs_is_staff())
);

drop policy if exists "rs_student_media_assets_delete" on public.rs_student_media_assets;
create policy "rs_student_media_assets_delete" on public.rs_student_media_assets
for delete to authenticated using (
    student_id=(select auth.uid()) or (select private.rs_is_staff())
);

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values(
    'rs-student-media','rs-student-media',false,31457280,
    array['video/mp4','video/webm','video/quicktime','image/jpeg','image/png','image/webp','image/gif']
)
on conflict(id) do update set
    public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "rs_student_media_storage_select" on storage.objects;
create policy "rs_student_media_storage_select" on storage.objects
for select to authenticated using (
    bucket_id='rs-student-media'
    and (
        (select private.rs_is_staff())
        or (storage.foldername(name))[1]=(select auth.uid())::text
    )
);

drop policy if exists "rs_student_media_storage_insert" on storage.objects;
create policy "rs_student_media_storage_insert" on storage.objects
for insert to authenticated with check (
    bucket_id='rs-student-media'
    and (
        (select private.rs_is_staff())
        or (storage.foldername(name))[1]=(select auth.uid())::text
    )
);

drop policy if exists "rs_student_media_storage_delete" on storage.objects;
create policy "rs_student_media_storage_delete" on storage.objects
for delete to authenticated using (
    bucket_id='rs-student-media'
    and (
        (select private.rs_is_staff())
        or (storage.foldername(name))[1]=(select auth.uid())::text
    )
);

create or replace function public.rs_staff_create_training_template(
    p_title text,p_description text,p_category text
) returns uuid
language plpgsql security definer set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    insert into public.rs_training_templates(title,description,category,created_by)
    values(trim(p_title),coalesce(p_description,''),coalesce(nullif(trim(p_category),''),'GENERAL'),(select auth.uid()))
    returning id into v_id;
    return v_id;
end;
$$;

create or replace function public.rs_staff_add_training_template_step(
    p_template_id uuid,p_order integer,p_title text,p_instructions text,p_sets_reps_time text,p_media_id uuid
) returns uuid
language plpgsql security definer set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    insert into public.rs_training_template_steps(template_id,step_order,title,instructions,sets_reps_time,media_id)
    values(p_template_id,greatest(1,least(200,p_order)),trim(p_title),coalesce(p_instructions,''),coalesce(p_sets_reps_time,''),p_media_id)
    returning id into v_id;
    return v_id;
end;
$$;

create or replace function public.rs_training_templates_feed()
returns table(id uuid,title text,description text,category text,active boolean,created_at timestamptz)
language sql stable security definer set search_path=''
as $$
    select t.id,t.title,t.description,t.category,t.active,t.created_at
    from public.rs_training_templates t
    where (select private.rs_is_staff())
    order by t.updated_at desc,t.created_at desc;
$$;

create or replace function public.rs_training_template_steps_feed(p_template_id uuid)
returns table(id uuid,template_id uuid,step_order integer,title text,instructions text,sets_reps_time text,media_id uuid,media_title text,media_kind text)
language sql stable security definer set search_path=''
as $$
    select s.id,s.template_id,s.step_order,s.title,s.instructions,s.sets_reps_time,s.media_id,m.title,m.media_kind
    from public.rs_training_template_steps s
    left join public.rs_training_media m on m.id=s.media_id
    where s.template_id=p_template_id and (select private.rs_is_staff())
    order by s.step_order,s.id;
$$;

create or replace function public.rs_staff_assign_training_template(
    p_student_id uuid,p_template_id uuid,p_due_label text
) returns uuid
language plpgsql security definer set search_path=''
as $$
declare v_homework_id uuid; v_template public.rs_training_templates%rowtype;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    select * into v_template from public.rs_training_templates where id=p_template_id and active=true;
    if not found then raise exception 'training template not found' using errcode='P0002'; end if;

    insert into public.rs_homework(student_id,title,details,due_label,assigned_by)
    values(p_student_id,v_template.title,v_template.description,coalesce(p_due_label,''),(select auth.uid()))
    returning id into v_homework_id;

    insert into public.rs_homework_steps(homework_id,step_order,title,instructions,sets_reps_time,media_id)
    select v_homework_id,step_order,title,instructions,sets_reps_time,media_id
    from public.rs_training_template_steps
    where template_id=p_template_id
    order by step_order,id;

    return v_homework_id;
end;
$$;

create or replace function public.rs_homework_steps_feed(p_homework_id uuid)
returns table(id uuid,homework_id uuid,step_order integer,title text,instructions text,sets_reps_time text,media_id uuid,media_title text,media_kind text,completed boolean)
language sql stable security definer set search_path=''
as $$
    select s.id,s.homework_id,s.step_order,s.title,s.instructions,s.sets_reps_time,s.media_id,m.title,m.media_kind,s.completed
    from public.rs_homework_steps s
    join public.rs_homework h on h.id=s.homework_id
    left join public.rs_training_media m on m.id=s.media_id
    where s.homework_id=p_homework_id
      and (h.student_id=(select auth.uid()) or (select private.rs_is_staff()))
    order by s.step_order,s.id;
$$;

create or replace function public.rs_set_homework_step_completed(p_step_id uuid,p_completed boolean)
returns void
language plpgsql security definer set search_path=''
as $$
begin
    update public.rs_homework_steps s
    set completed=p_completed,updated_at=now()
    from public.rs_homework h
    where s.id=p_step_id and h.id=s.homework_id
      and (h.student_id=(select auth.uid()) or (select private.rs_is_staff()));
    if not found then raise exception 'homework step not found or not permitted' using errcode='42501'; end if;
end;
$$;

create or replace function public.rs_staff_student_storage_usage()
returns table(student_id uuid,email text,display_name text,used_bytes bigint,limit_bytes bigint,asset_count bigint,warning_message text)
language sql stable security definer set search_path=''
as $$
    select
        p.id,
        p.email,
        p.display_name,
        coalesce(sum(a.byte_size),0)::bigint,
        coalesce(l.limit_bytes,52428800)::bigint,
        count(a.id)::bigint,
        coalesce(l.warning_message,'')
    from public.rs_profiles p
    left join public.rs_student_media_assets a on a.student_id=p.id
    left join public.rs_student_storage_limits l on l.student_id=p.id
    where p.role='student' and (select private.rs_is_staff())
    group by p.id,p.email,p.display_name,l.limit_bytes,l.warning_message
    order by lower(p.display_name),lower(p.email);
$$;

create or replace function public.rs_staff_set_student_storage_limit(
    p_student_id uuid,p_limit_bytes bigint,p_warning_message text
) returns void
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    insert into public.rs_student_storage_limits(student_id,limit_bytes,warning_message,updated_by,updated_at)
    values(p_student_id,greatest(1048576,p_limit_bytes),coalesce(p_warning_message,''),(select auth.uid()),now())
    on conflict(student_id) do update set
        limit_bytes=excluded.limit_bytes,
        warning_message=excluded.warning_message,
        updated_by=excluded.updated_by,
        updated_at=now();
end;
$$;

create or replace function public.rs_staff_mark_training_media_ai_reference(
    p_media_id uuid,p_ai_reference boolean,p_technique_tags text[]
) returns void
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    update public.rs_training_media
    set ai_reference=p_ai_reference,
        technique_tags=coalesce(p_technique_tags,'{}'::text[]),
        updated_at=now()
    where id=p_media_id;
    if not found then raise exception 'training media not found' using errcode='P0002'; end if;
end;
$$;

revoke execute on function public.rs_staff_create_training_template(text,text,text) from public,anon;
revoke execute on function public.rs_staff_add_training_template_step(uuid,integer,text,text,text,uuid) from public,anon;
revoke execute on function public.rs_training_templates_feed() from public,anon;
revoke execute on function public.rs_training_template_steps_feed(uuid) from public,anon;
revoke execute on function public.rs_staff_assign_training_template(uuid,uuid,text) from public,anon;
revoke execute on function public.rs_homework_steps_feed(uuid) from public,anon;
revoke execute on function public.rs_set_homework_step_completed(uuid,boolean) from public,anon;
revoke execute on function public.rs_staff_student_storage_usage() from public,anon;
revoke execute on function public.rs_staff_set_student_storage_limit(uuid,bigint,text) from public,anon;
revoke execute on function public.rs_staff_mark_training_media_ai_reference(uuid,boolean,text[]) from public,anon;

grant execute on function public.rs_staff_create_training_template(text,text,text) to authenticated;
grant execute on function public.rs_staff_add_training_template_step(uuid,integer,text,text,text,uuid) to authenticated;
grant execute on function public.rs_training_templates_feed() to authenticated;
grant execute on function public.rs_training_template_steps_feed(uuid) to authenticated;
grant execute on function public.rs_staff_assign_training_template(uuid,uuid,text) to authenticated;
grant execute on function public.rs_homework_steps_feed(uuid) to authenticated;
grant execute on function public.rs_set_homework_step_completed(uuid,boolean) to authenticated;
grant execute on function public.rs_staff_student_storage_usage() to authenticated;
grant execute on function public.rs_staff_set_student_storage_limit(uuid,bigint,text) to authenticated;
grant execute on function public.rs_staff_mark_training_media_ai_reference(uuid,boolean,text[]) to authenticated;

comment on table public.rs_training_templates is
'Reusable trainer-authored training plans. Assigning a template snapshots its steps into homework so later template edits do not rewrite past assignments.';
comment on table public.rs_student_media_assets is
'Metadata ledger for student-specific media used for quota/cleanup. Shared trainer catalog media is not duplicated or charged per student.';

-- ============================================================================
-- SOURCE: supabase/migrations/0046_media_cleanup_private_lesson_chat_admin.sql
-- ============================================================================

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
      and sender_role in ('trainer','admin')
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
    update public.rs_group_messages m
    set body=trim(coalesce(p_body,''))
    where m.id=p_message_id
      and m.sender_id=(select auth.uid())
      and (trim(coalesce(p_body,''))<>'' or m.media_path is not null);
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

-- ============================================================================
-- SOURCE: supabase/migrations/0047_storage_quota_profiles.sql
-- ============================================================================

-- RS KICKBOXING backend foundation
-- Migration 0047: scalable storage quota profiles by Supabase tier and membership plan.

create table if not exists public.rs_storage_quota_config (
    singleton boolean primary key default true check(singleton),
    backend_tier text not null default 'FREE' check(backend_tier in ('FREE','PRO')),
    updated_by uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);

insert into public.rs_storage_quota_config(singleton,backend_tier)
values(true,'FREE')
on conflict(singleton) do nothing;

create table if not exists public.rs_storage_quota_profiles (
    backend_tier text not null check(backend_tier in ('FREE','PRO')),
    membership_plan text not null check(membership_plan in ('BASIC','PRO','ELITE')),
    default_limit_bytes bigint not null check(default_limit_bytes between 1048576 and 10737418240),
    primary key(backend_tier,membership_plan)
);

insert into public.rs_storage_quota_profiles(backend_tier,membership_plan,default_limit_bytes)
values
('FREE','BASIC',26214400),
('FREE','PRO',52428800),
('FREE','ELITE',104857600),
('PRO','BASIC',262144000),
('PRO','PRO',524288000),
('PRO','ELITE',1073741824)
on conflict(backend_tier,membership_plan) do nothing;

alter table public.rs_storage_quota_config enable row level security;
alter table public.rs_storage_quota_profiles enable row level security;
revoke all on public.rs_storage_quota_config,public.rs_storage_quota_profiles from anon,authenticated;
grant select,insert,update,delete on public.rs_storage_quota_config,public.rs_storage_quota_profiles to authenticated;

drop policy if exists "rs_storage_quota_config_staff" on public.rs_storage_quota_config;
create policy "rs_storage_quota_config_staff" on public.rs_storage_quota_config
for all to authenticated using ((select private.rs_is_staff())) with check ((select private.rs_is_staff()));

drop policy if exists "rs_storage_quota_profiles_staff" on public.rs_storage_quota_profiles;
create policy "rs_storage_quota_profiles_staff" on public.rs_storage_quota_profiles
for all to authenticated using ((select private.rs_is_staff())) with check ((select private.rs_is_staff()));

drop function if exists public.rs_staff_student_storage_usage();

create function public.rs_staff_student_storage_usage()
returns table(
    student_id uuid,
    email text,
    display_name text,
    membership_plan text,
    used_bytes bigint,
    limit_bytes bigint,
    asset_count bigint,
    warning_message text,
    custom_limit boolean
)
language sql stable security definer set search_path=''
as $$
    with tier as (
        select c.backend_tier from public.rs_storage_quota_config c where c.singleton=true
    )
    select
        p.id,
        p.email,
        p.display_name,
        coalesce(m.plan,'PRO')::text,
        coalesce(sum(a.byte_size),0)::bigint,
        coalesce(
            l.limit_bytes,
            q.default_limit_bytes,
            52428800
        )::bigint,
        count(a.id)::bigint,
        coalesce(l.warning_message,''),
        (l.student_id is not null)
    from public.rs_profiles p
    left join public.rs_memberships m on m.student_id=p.id
    left join public.rs_student_media_assets a on a.student_id=p.id
    left join public.rs_student_storage_limits l on l.student_id=p.id
    left join tier t on true
    left join public.rs_storage_quota_profiles q
      on q.backend_tier=coalesce(t.backend_tier,'FREE')
     and q.membership_plan=coalesce(m.plan,'PRO')
    where p.role='student' and (select private.rs_is_staff())
    group by p.id,p.email,p.display_name,m.plan,l.student_id,l.limit_bytes,l.warning_message,q.default_limit_bytes
    order by lower(p.display_name),lower(p.email);
$$;

create or replace function public.rs_staff_storage_quota_settings()
returns table(
    backend_tier text,
    free_basic bigint,
    free_pro bigint,
    free_elite bigint,
    pro_basic bigint,
    pro_pro bigint,
    pro_elite bigint
)
language sql stable security definer set search_path=''
as $$
    select
        coalesce((select c.backend_tier from public.rs_storage_quota_config c where c.singleton=true),'FREE'),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='FREE' and q.membership_plan='BASIC'),26214400),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='FREE' and q.membership_plan='PRO'),52428800),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='FREE' and q.membership_plan='ELITE'),104857600),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='PRO' and q.membership_plan='BASIC'),262144000),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='PRO' and q.membership_plan='PRO'),524288000),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='PRO' and q.membership_plan='ELITE'),1073741824)
    where (select private.rs_is_staff());
$$;

create or replace function public.rs_staff_set_storage_backend_tier(p_backend_tier text)
returns void
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    if p_backend_tier not in ('FREE','PRO') then raise exception 'invalid backend tier' using errcode='22023'; end if;
    insert into public.rs_storage_quota_config(singleton,backend_tier,updated_by,updated_at)
    values(true,p_backend_tier,(select auth.uid()),now())
    on conflict(singleton) do update set
      backend_tier=excluded.backend_tier,updated_by=excluded.updated_by,updated_at=now();
end;
$$;

create or replace function public.rs_staff_set_storage_plan_default(
    p_backend_tier text,p_membership_plan text,p_limit_bytes bigint
) returns void
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    if p_backend_tier not in ('FREE','PRO') or p_membership_plan not in ('BASIC','PRO','ELITE') then
        raise exception 'invalid storage profile' using errcode='22023';
    end if;
    insert into public.rs_storage_quota_profiles(backend_tier,membership_plan,default_limit_bytes)
    values(p_backend_tier,p_membership_plan,greatest(1048576,p_limit_bytes))
    on conflict(backend_tier,membership_plan) do update set default_limit_bytes=excluded.default_limit_bytes;
end;
$$;

revoke execute on function public.rs_staff_student_storage_usage() from public,anon;
revoke execute on function public.rs_staff_storage_quota_settings() from public,anon;
revoke execute on function public.rs_staff_set_storage_backend_tier(text) from public,anon;
revoke execute on function public.rs_staff_set_storage_plan_default(text,text,bigint) from public,anon;
grant execute on function public.rs_staff_student_storage_usage() to authenticated;
grant execute on function public.rs_staff_storage_quota_settings() to authenticated;
grant execute on function public.rs_staff_set_storage_backend_tier(text) to authenticated;
grant execute on function public.rs_staff_set_storage_plan_default(text,text,bigint) to authenticated;

comment on table public.rs_storage_quota_profiles is
'Soft student storage defaults by Supabase backend tier and RS membership. Per-student overrides in rs_student_storage_limits always take priority.';
