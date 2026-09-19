-- RS KICKBOXING backend foundation
-- Migration 0045: reusable training plans, homework media references and student storage accounting.
-- Additive only. Existing homework/training media/chat flows remain compatible.

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
