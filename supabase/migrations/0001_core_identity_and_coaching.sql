-- RS KICKBOX backend foundation
-- Migration 0001: identity, private invitations and technique-coach history.
-- Apply only after a Supabase project is connected.

create schema if not exists private;

create table if not exists public.rs_profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text not null,
    display_name text not null default '',
    role text not null default 'student' check (role in ('student','trainer','admin')),
    plan text not null default 'PRO' check (plan in ('BASIC','PRO','ELITE')),
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_profiles_role_idx on public.rs_profiles(role);
create index if not exists rs_profiles_email_lower_idx on public.rs_profiles(lower(email));

alter table public.rs_profiles enable row level security;
revoke all on table public.rs_profiles from anon, authenticated;
grant select on table public.rs_profiles to authenticated;

create or replace function private.rs_is_staff()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.rs_profiles p
        where p.id = (select auth.uid())
          and p.active = true
          and p.role in ('trainer','admin')
    );
$$;

revoke execute on function private.rs_is_staff() from public;
revoke execute on function private.rs_is_staff() from anon;
grant usage on schema private to authenticated;
grant execute on function private.rs_is_staff() to authenticated;

drop policy if exists "rs_profiles_select_own_or_staff" on public.rs_profiles;
create policy "rs_profiles_select_own_or_staff"
on public.rs_profiles
for select
to authenticated
using (
    (select auth.uid()) = id
    or (select private.rs_is_staff())
);

create table if not exists public.rs_student_invites (
    id uuid primary key default gen_random_uuid(),
    email text not null,
    display_name text not null,
    plan text not null default 'PRO' check (plan in ('BASIC','PRO','ELITE')),
    token_hash text not null unique,
    created_by uuid not null references auth.users(id) on delete restrict,
    expires_at timestamptz not null,
    redeemed_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists rs_student_invites_email_lower_idx on public.rs_student_invites(lower(email));
create index if not exists rs_student_invites_created_by_idx on public.rs_student_invites(created_by);
create index if not exists rs_student_invites_expires_at_idx on public.rs_student_invites(expires_at);

alter table public.rs_student_invites enable row level security;
revoke all on table public.rs_student_invites from anon, authenticated;

-- Deliberately no client insert/update/delete grants here.
-- Invitation token creation/redeeming will be handled by an authenticated
-- Edge Function/server path so raw invitation tokens are never stored in this table.

create table if not exists public.rs_technique_submissions (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null references auth.users(id) on delete cascade,
    technique text not null,
    media_path text not null,
    media_name text not null default '',
    student_summary text not null default '',
    trainer_note text not null default '',
    trainer_favorite boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_technique_submissions_student_idx on public.rs_technique_submissions(student_id);
create index if not exists rs_technique_submissions_created_idx on public.rs_technique_submissions(created_at desc);

alter table public.rs_technique_submissions enable row level security;
revoke all on table public.rs_technique_submissions from anon, authenticated;
grant select, insert, delete on table public.rs_technique_submissions to authenticated;
grant update on table public.rs_technique_submissions to authenticated;

drop policy if exists "rs_technique_select_own_or_staff" on public.rs_technique_submissions;
create policy "rs_technique_select_own_or_staff"
on public.rs_technique_submissions
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_technique_insert_own" on public.rs_technique_submissions;
create policy "rs_technique_insert_own"
on public.rs_technique_submissions
for insert
to authenticated
with check (
    (select auth.uid()) = student_id
);

drop policy if exists "rs_technique_delete_own_or_staff" on public.rs_technique_submissions;
create policy "rs_technique_delete_own_or_staff"
on public.rs_technique_submissions
for delete
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_technique_update_staff_only" on public.rs_technique_submissions;
create policy "rs_technique_update_staff_only"
on public.rs_technique_submissions
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

comment on table public.rs_student_invites is
'Private enrollment invitations. Store only a one-way token hash; raw tokens belong in the server response/QR and must never be persisted here.';

comment on table public.rs_technique_submissions is
'Technique Coach submission metadata. Actual video objects belong in private storage and are referenced by media_path.';
