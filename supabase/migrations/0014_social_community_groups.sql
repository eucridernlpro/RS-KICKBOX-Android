-- RS KICKBOX backend foundation
-- Migration 0014: social profiles, community posts and groups.

create table if not exists public.rs_social_profiles (
    user_id uuid primary key references auth.users(id) on delete cascade,
    display_name text not null,
    bio text not null default '',
    training_goal text not null default '',
    public_profile boolean not null default false,
    updated_at timestamptz not null default now()
);

alter table public.rs_social_profiles enable row level security;
revoke all on table public.rs_social_profiles from anon, authenticated;
grant select, insert, update on table public.rs_social_profiles to authenticated;

drop policy if exists "rs_social_profiles_select_visible" on public.rs_social_profiles;
create policy "rs_social_profiles_select_visible"
on public.rs_social_profiles
for select
to authenticated
using (
    (select auth.uid()) = user_id
    or public_profile = true
    or (select private.rs_is_staff())
);

drop policy if exists "rs_social_profiles_insert_own" on public.rs_social_profiles;
create policy "rs_social_profiles_insert_own"
on public.rs_social_profiles
for insert
to authenticated
with check ((select auth.uid()) = user_id);

drop policy if exists "rs_social_profiles_update_own" on public.rs_social_profiles;
create policy "rs_social_profiles_update_own"
on public.rs_social_profiles
for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create table if not exists public.rs_community_posts (
    id uuid primary key default gen_random_uuid(),
    author_id uuid not null references auth.users(id) on delete cascade,
    body text not null check (char_length(body) between 1 and 1000),
    active boolean not null default true,
    created_at timestamptz not null default now()
);

create index if not exists rs_community_posts_created_idx
on public.rs_community_posts(created_at desc);

alter table public.rs_community_posts enable row level security;
revoke all on table public.rs_community_posts from anon, authenticated;
grant select, insert, update, delete on table public.rs_community_posts to authenticated;

drop policy if exists "rs_community_posts_select_active" on public.rs_community_posts;
create policy "rs_community_posts_select_active"
on public.rs_community_posts
for select
to authenticated
using (active = true or (select private.rs_is_staff()) or (select auth.uid()) = author_id);

drop policy if exists "rs_community_posts_insert_own" on public.rs_community_posts;
create policy "rs_community_posts_insert_own"
on public.rs_community_posts
for insert
to authenticated
with check ((select auth.uid()) = author_id);

drop policy if exists "rs_community_posts_author_delete" on public.rs_community_posts;
create policy "rs_community_posts_author_delete"
on public.rs_community_posts
for delete
to authenticated
using ((select auth.uid()) = author_id or (select private.rs_is_staff()));

drop policy if exists "rs_community_posts_staff_update" on public.rs_community_posts;
create policy "rs_community_posts_staff_update"
on public.rs_community_posts
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists public.rs_groups (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    description text not null default '',
    active boolean not null default true,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now()
);

alter table public.rs_groups enable row level security;
revoke all on table public.rs_groups from anon, authenticated;
grant select, insert, update, delete on table public.rs_groups to authenticated;

drop policy if exists "rs_groups_select_visible" on public.rs_groups;
create policy "rs_groups_select_visible"
on public.rs_groups
for select
to authenticated
using (active = true or (select private.rs_is_staff()));

drop policy if exists "rs_groups_staff_write" on public.rs_groups;
create policy "rs_groups_staff_write"
on public.rs_groups
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists public.rs_group_memberships (
    group_id uuid not null references public.rs_groups(id) on delete cascade,
    student_id uuid not null references auth.users(id) on delete cascade,
    joined_at timestamptz not null default now(),
    primary key(group_id,student_id)
);

create index if not exists rs_group_memberships_student_idx
on public.rs_group_memberships(student_id);

alter table public.rs_group_memberships enable row level security;
revoke all on table public.rs_group_memberships from anon, authenticated;
grant select, insert, delete on table public.rs_group_memberships to authenticated;

drop policy if exists "rs_group_memberships_select_own_or_staff" on public.rs_group_memberships;
create policy "rs_group_memberships_select_own_or_staff"
on public.rs_group_memberships
for select
to authenticated
using ((select auth.uid()) = student_id or (select private.rs_is_staff()));

drop policy if exists "rs_group_memberships_insert_own" on public.rs_group_memberships;
create policy "rs_group_memberships_insert_own"
on public.rs_group_memberships
for insert
to authenticated
with check ((select auth.uid()) = student_id);

drop policy if exists "rs_group_memberships_delete_own" on public.rs_group_memberships;
create policy "rs_group_memberships_delete_own"
on public.rs_group_memberships
for delete
to authenticated
using ((select auth.uid()) = student_id);

comment on table public.rs_social_profiles is
'Student-controlled profile fields with explicit public/private visibility.';

comment on table public.rs_community_posts is
'Member community posts. Authors can delete their own posts; staff can moderate active state.';

comment on table public.rs_groups is
'Trainer/admin-managed training groups.';

comment on table public.rs_group_memberships is
'Student-owned group memberships.';
