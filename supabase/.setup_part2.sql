
-- ============================================================
-- supabase/migrations/0011_performance_challenges_fightcamp.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0011: challenges and fight camp.

create table if not exists public.rs_challenges (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    target integer not null check (target between 1 and 10000),
    current integer not null default 0 check (current >= 0),
    unit text not null default 'sessions',
    active boolean not null default true,
    assigned_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_challenges_student_idx
on public.rs_challenges(student_id,created_at desc);

alter table public.rs_challenges enable row level security;
revoke all on table public.rs_challenges from anon, authenticated;
grant select, insert, update, delete on table public.rs_challenges to authenticated;

drop policy if exists "rs_challenges_select_own_or_staff" on public.rs_challenges;
create policy "rs_challenges_select_own_or_staff"
on public.rs_challenges
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_challenges_staff_insert" on public.rs_challenges;
create policy "rs_challenges_staff_insert"
on public.rs_challenges
for insert
to authenticated
with check ((select private.rs_is_staff()));

drop policy if exists "rs_challenges_update_own_or_staff" on public.rs_challenges;
create policy "rs_challenges_update_own_or_staff"
on public.rs_challenges
for update
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
)
with check (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_challenges_staff_delete" on public.rs_challenges;
create policy "rs_challenges_staff_delete"
on public.rs_challenges
for delete
to authenticated
using ((select private.rs_is_staff()));

create table if not exists public.rs_fight_camps (
    student_id uuid primary key references auth.users(id) on delete cascade,
    current_week integer not null default 1 check (current_week between 1 and 8),
    total_weeks integer not null default 8 check (total_weeks = 8),
    focus text not null default '',
    active boolean not null default true,
    managed_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.rs_fight_camps enable row level security;
revoke all on table public.rs_fight_camps from anon, authenticated;
grant select, insert, update, delete on table public.rs_fight_camps to authenticated;

drop policy if exists "rs_fight_camps_select_own_or_staff" on public.rs_fight_camps;
create policy "rs_fight_camps_select_own_or_staff"
on public.rs_fight_camps
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_fight_camps_staff_write" on public.rs_fight_camps;
create policy "rs_fight_camps_staff_write"
on public.rs_fight_camps
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

comment on table public.rs_challenges is
'Trainer-assigned student challenges. Students may update their own progress; challenge authoring remains staff-controlled.';

comment on table public.rs_fight_camps is
'One active 8-week fight-camp plan per student, managed by staff.';

-- ============================================================
-- supabase/migrations/0012_training_content_library.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0012: searchable training content, favorites and reading history.

create table if not exists public.rs_content (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    category text not null default 'TECHNIQUE',
    body text not null,
    access_tier text not null default 'ALL' check (access_tier in ('ALL','BASIC','PRO','ELITE')),
    published boolean not null default true,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_content_published_idx on public.rs_content(published,created_at desc);
create index if not exists rs_content_category_idx on public.rs_content(category);

alter table public.rs_content enable row level security;
revoke all on table public.rs_content from anon, authenticated;
grant select on table public.rs_content to authenticated;
grant insert, update, delete on table public.rs_content to authenticated;

drop policy if exists "rs_content_select_visible" on public.rs_content;
create policy "rs_content_select_visible"
on public.rs_content
for select
to authenticated
using (
    (select private.rs_is_staff())
    or (
        published = true
        and (
            access_tier = 'ALL'
            or access_tier = (
                select p.plan from public.rs_profiles p
                where p.id = (select auth.uid()) and p.active = true
                limit 1
            )
            or (
                access_tier = 'BASIC'
                and (select p.plan from public.rs_profiles p where p.id=(select auth.uid()) limit 1) in ('BASIC','PRO','ELITE')
            )
            or (
                access_tier = 'PRO'
                and (select p.plan from public.rs_profiles p where p.id=(select auth.uid()) limit 1) in ('PRO','ELITE')
            )
        )
    )
);

drop policy if exists "rs_content_staff_write" on public.rs_content;
create policy "rs_content_staff_write"
on public.rs_content
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists public.rs_content_favorites (
    user_id uuid not null references auth.users(id) on delete cascade,
    content_id uuid not null references public.rs_content(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key(user_id,content_id)
);

alter table public.rs_content_favorites enable row level security;
revoke all on table public.rs_content_favorites from anon, authenticated;
grant select, insert, delete on table public.rs_content_favorites to authenticated;

drop policy if exists "rs_content_favorites_own" on public.rs_content_favorites;
create policy "rs_content_favorites_own"
on public.rs_content_favorites
for all
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create table if not exists public.rs_content_history (
    id bigint generated always as identity primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    content_id uuid not null references public.rs_content(id) on delete cascade,
    opened_at timestamptz not null default now()
);

create index if not exists rs_content_history_user_idx
on public.rs_content_history(user_id,opened_at desc);

alter table public.rs_content_history enable row level security;
revoke all on table public.rs_content_history from anon, authenticated;
grant select, insert, delete on table public.rs_content_history to authenticated;

drop policy if exists "rs_content_history_own" on public.rs_content_history;
create policy "rs_content_history_own"
on public.rs_content_history
for all
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

comment on table public.rs_content is
'Trainer-authored searchable training content filtered by subscription level.';

comment on table public.rs_content_favorites is
'Per-student saved/favorite content.';

comment on table public.rs_content_history is
'Per-student content-open history for the Training History view.';

-- ============================================================
-- supabase/migrations/0013_membership_plan_definitions.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0013: membership plan definitions.

create table if not exists public.rs_membership_plans (
    code text primary key check (code in ('BASIC','PRO','ELITE')),
    name text not null,
    monthly_cents integer not null check (monthly_cents >= 0),
    currency text not null default 'EUR',
    description text not null default '',
    active boolean not null default true,
    updated_by uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);

insert into public.rs_membership_plans(code,name,monthly_cents,currency,description,active)
values
    ('BASIC','RS BASIC',2900,'EUR','Core training library, classes and member access.',true),
    ('PRO','RS PRO',4900,'EUR','Expanded coaching, advanced content and full member tools.',true),
    ('ELITE','RS ELITE',6900,'EUR','Premium access for advanced coaching and exclusive content.',true)
on conflict (code) do nothing;

alter table public.rs_membership_plans enable row level security;
revoke all on table public.rs_membership_plans from anon, authenticated;
grant select on table public.rs_membership_plans to authenticated;
grant update on table public.rs_membership_plans to authenticated;

drop policy if exists "rs_membership_plans_select_authenticated" on public.rs_membership_plans;
create policy "rs_membership_plans_select_authenticated"
on public.rs_membership_plans
for select
to authenticated
using (true);

drop policy if exists "rs_membership_plans_staff_update" on public.rs_membership_plans;
create policy "rs_membership_plans_staff_update"
on public.rs_membership_plans
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

comment on table public.rs_membership_plans is
'Trainer/admin-configurable BASIC, PRO and ELITE plan pricing and descriptions. Student profile plan codes continue to use these stable codes.';

-- ============================================================
-- supabase/migrations/0014_social_community_groups.sql
-- ============================================================

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

-- ============================================================
-- supabase/migrations/0015_member_services.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0015: club documents, support tickets and referrals.

create table if not exists public.rs_club_documents (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    body text not null,
    access_tier text not null default 'ALL' check (access_tier in ('ALL','BASIC','PRO','ELITE')),
    active boolean not null default true,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.rs_club_documents enable row level security;
revoke all on table public.rs_club_documents from anon, authenticated;
grant select on table public.rs_club_documents to authenticated;
grant insert, update, delete on table public.rs_club_documents to authenticated;

drop policy if exists "rs_club_documents_select_visible" on public.rs_club_documents;
create policy "rs_club_documents_select_visible"
on public.rs_club_documents
for select
to authenticated
using (
    (select private.rs_is_staff())
    or (
        active = true
        and (
            access_tier = 'ALL'
            or access_tier = (
                select p.plan from public.rs_profiles p where p.id=(select auth.uid()) and p.active=true limit 1
            )
            or (
                access_tier='BASIC'
                and (select p.plan from public.rs_profiles p where p.id=(select auth.uid()) limit 1) in ('BASIC','PRO','ELITE')
            )
            or (
                access_tier='PRO'
                and (select p.plan from public.rs_profiles p where p.id=(select auth.uid()) limit 1) in ('PRO','ELITE')
            )
        )
    )
);

drop policy if exists "rs_club_documents_staff_write" on public.rs_club_documents;
create policy "rs_club_documents_staff_write"
on public.rs_club_documents
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists public.rs_support_tickets (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null references auth.users(id) on delete cascade,
    subject text not null,
    message text not null,
    trainer_reply text not null default '',
    status text not null default 'OPEN' check (status in ('OPEN','RESOLVED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_support_tickets_student_idx on public.rs_support_tickets(student_id,created_at desc);

alter table public.rs_support_tickets enable row level security;
revoke all on table public.rs_support_tickets from anon, authenticated;
grant select, insert, update, delete on table public.rs_support_tickets to authenticated;

drop policy if exists "rs_support_tickets_select_own_or_staff" on public.rs_support_tickets;
create policy "rs_support_tickets_select_own_or_staff"
on public.rs_support_tickets
for select
to authenticated
using ((select auth.uid()) = student_id or (select private.rs_is_staff()));

drop policy if exists "rs_support_tickets_insert_own" on public.rs_support_tickets;
create policy "rs_support_tickets_insert_own"
on public.rs_support_tickets
for insert
to authenticated
with check ((select auth.uid()) = student_id);

drop policy if exists "rs_support_tickets_staff_update" on public.rs_support_tickets;
create policy "rs_support_tickets_staff_update"
on public.rs_support_tickets
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

drop policy if exists "rs_support_tickets_staff_delete" on public.rs_support_tickets;
create policy "rs_support_tickets_staff_delete"
on public.rs_support_tickets
for delete
to authenticated
using ((select private.rs_is_staff()));

create table if not exists public.rs_referrals (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null unique references auth.users(id) on delete cascade,
    code text not null unique,
    uses integer not null default 0 check (uses >= 0),
    active boolean not null default true,
    created_at timestamptz not null default now()
);

alter table public.rs_referrals enable row level security;
revoke all on table public.rs_referrals from anon, authenticated;
grant select, insert on table public.rs_referrals to authenticated;
grant update on table public.rs_referrals to authenticated;

drop policy if exists "rs_referrals_select_own_or_staff" on public.rs_referrals;
create policy "rs_referrals_select_own_or_staff"
on public.rs_referrals
for select
to authenticated
using ((select auth.uid()) = owner_id or (select private.rs_is_staff()));

drop policy if exists "rs_referrals_insert_own" on public.rs_referrals;
create policy "rs_referrals_insert_own"
on public.rs_referrals
for insert
to authenticated
with check ((select auth.uid()) = owner_id);

drop policy if exists "rs_referrals_staff_update" on public.rs_referrals;
create policy "rs_referrals_staff_update"
on public.rs_referrals
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

comment on table public.rs_club_documents is
'Trainer-published club documents filtered by membership tier.';

comment on table public.rs_support_tickets is
'Private student support requests with trainer reply and resolution state.';

comment on table public.rs_referrals is
'Per-user referral code and usage count.';

-- ============================================================
-- supabase/migrations/0016_sessions_qr_attendance.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0016: trainer-built sessions and QR attendance tokens.

create table if not exists public.rs_training_sessions (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    active boolean not null default false,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists rs_training_sessions_one_active
on public.rs_training_sessions((active))
where active = true;

alter table public.rs_training_sessions enable row level security;
revoke all on table public.rs_training_sessions from anon, authenticated;
grant select on table public.rs_training_sessions to authenticated;
grant insert, update, delete on table public.rs_training_sessions to authenticated;

drop policy if exists "rs_training_sessions_select" on public.rs_training_sessions;
create policy "rs_training_sessions_select"
on public.rs_training_sessions
for select
to authenticated
using (active = true or (select private.rs_is_staff()));

drop policy if exists "rs_training_sessions_staff_write" on public.rs_training_sessions;
create policy "rs_training_sessions_staff_write"
on public.rs_training_sessions
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists public.rs_training_session_blocks (
    id uuid primary key default gen_random_uuid(),
    session_id uuid not null references public.rs_training_sessions(id) on delete cascade,
    sort_order integer not null default 0,
    title text not null,
    duration_seconds integer not null check (duration_seconds between 10 and 3600),
    instructions text not null default ''
);

create index if not exists rs_training_session_blocks_session_idx
on public.rs_training_session_blocks(session_id,sort_order);

alter table public.rs_training_session_blocks enable row level security;
revoke all on table public.rs_training_session_blocks from anon, authenticated;
grant select on table public.rs_training_session_blocks to authenticated;
grant insert, update, delete on table public.rs_training_session_blocks to authenticated;

drop policy if exists "rs_training_session_blocks_select" on public.rs_training_session_blocks;
create policy "rs_training_session_blocks_select"
on public.rs_training_session_blocks
for select
to authenticated
using (
    exists(
        select 1 from public.rs_training_sessions s
        where s.id = session_id
          and (s.active = true or (select private.rs_is_staff()))
    )
);

drop policy if exists "rs_training_session_blocks_staff_write" on public.rs_training_session_blocks;
create policy "rs_training_session_blocks_staff_write"
on public.rs_training_session_blocks
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists private.rs_attendance_tokens (
    id uuid primary key default gen_random_uuid(),
    class_id uuid not null references public.rs_classes(id) on delete cascade,
    token_hash text not null unique,
    created_by uuid not null references auth.users(id) on delete restrict,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists rs_attendance_tokens_class_idx
on private.rs_attendance_tokens(class_id,expires_at);

comment on table private.rs_attendance_tokens is
'Server-only one-time/short-lived QR attendance tokens. Raw QR codes should never be stored; production check-in will validate hashes server-side and write rs_attendance.';

-- ============================================================
-- supabase/migrations/0017_academy_progress.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0017: Academy progress.

create table if not exists public.rs_academy_progress (
    student_id uuid not null references auth.users(id) on delete cascade,
    track_code text not null,
    completed_lessons integer not null default 0 check (completed_lessons between 0 and 100),
    updated_at timestamptz not null default now(),
    primary key(student_id,track_code)
);

alter table public.rs_academy_progress enable row level security;
revoke all on table public.rs_academy_progress from anon, authenticated;
grant select, insert, update on table public.rs_academy_progress to authenticated;

drop policy if exists "rs_academy_progress_select_own_or_staff" on public.rs_academy_progress;
create policy "rs_academy_progress_select_own_or_staff"
on public.rs_academy_progress
for select
to authenticated
using ((select auth.uid()) = student_id or (select private.rs_is_staff()));

drop policy if exists "rs_academy_progress_insert_own" on public.rs_academy_progress;
create policy "rs_academy_progress_insert_own"
on public.rs_academy_progress
for insert
to authenticated
with check ((select auth.uid()) = student_id);

drop policy if exists "rs_academy_progress_update_own" on public.rs_academy_progress;
create policy "rs_academy_progress_update_own"
on public.rs_academy_progress
for update
to authenticated
using ((select auth.uid()) = student_id)
with check ((select auth.uid()) = student_id);

comment on table public.rs_academy_progress is
'Per-student completion state for RS Academy learning tracks.';

-- ============================================================
-- supabase/migrations/0018_training_media.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0017: trainer-managed training media catalog.

create table if not exists public.rs_training_media (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    category text not null default 'TECHNIQUE',
    description text not null default '',
    media_path text not null,
    media_kind text not null check (media_kind in ('VIDEO','IMAGE','GIF')),
    access_tier text not null default 'ALL' check (access_tier in ('ALL','BASIC','PRO','ELITE')),
    published boolean not null default true,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_training_media_visible_idx
on public.rs_training_media(published,access_tier,created_at desc);

alter table public.rs_training_media enable row level security;
revoke all on table public.rs_training_media from anon, authenticated;
grant select on table public.rs_training_media to authenticated;
grant insert, update, delete on table public.rs_training_media to authenticated;

drop policy if exists "rs_training_media_select_visible" on public.rs_training_media;
create policy "rs_training_media_select_visible"
on public.rs_training_media
for select
to authenticated
using (
    (select private.rs_is_staff())
    or (
        published = true
        and (
            access_tier = 'ALL'
            or access_tier = (
                select p.plan from public.rs_profiles p
                where p.id = (select auth.uid()) and p.active = true
                limit 1
            )
            or (
                access_tier = 'BASIC'
                and (select p.plan from public.rs_profiles p where p.id=(select auth.uid()) limit 1)
                    in ('BASIC','PRO','ELITE')
            )
            or (
                access_tier = 'PRO'
                and (select p.plan from public.rs_profiles p where p.id=(select auth.uid()) limit 1)
                    in ('PRO','ELITE')
            )
        )
    )
);

drop policy if exists "rs_training_media_staff_write" on public.rs_training_media;
create policy "rs_training_media_staff_write"
on public.rs_training_media
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

comment on table public.rs_training_media is
'Trainer-managed image/video/GIF catalog. media_path should point to private object storage; production delivery should use authenticated signed access rather than permanent public URLs.';

-- ============================================================
-- supabase/migrations/0019_app_operational_settings.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0018: global trainer-controlled application settings.

create table if not exists public.rs_app_settings (
    singleton boolean primary key default true check (singleton = true),
    maintenance_enabled boolean not null default false,
    maintenance_message text not null default 'RS KICKBOX maintenance notice: some services may be temporarily limited.',
    community_posts_enabled boolean not null default true,
    class_booking_enabled boolean not null default true,
    private_lessons_enabled boolean not null default true,
    referrals_enabled boolean not null default true,
    in_app_reminders_enabled boolean not null default true,
    retention_months integer not null default 24 check (retention_months in (12,24,36)),
    updated_by uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);

insert into public.rs_app_settings(singleton)
values (true)
on conflict (singleton) do nothing;

alter table public.rs_app_settings enable row level security;
revoke all on table public.rs_app_settings from anon, authenticated;
grant select on table public.rs_app_settings to authenticated;
grant update on table public.rs_app_settings to authenticated;

drop policy if exists "rs_app_settings_read_authenticated" on public.rs_app_settings;
create policy "rs_app_settings_read_authenticated"
on public.rs_app_settings
for select
to authenticated
using (true);

drop policy if exists "rs_app_settings_staff_update" on public.rs_app_settings;
create policy "rs_app_settings_staff_update"
on public.rs_app_settings
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

comment on table public.rs_app_settings is
'Singleton global operational controls for maintenance notices and student-facing feature availability. Only staff may update; authenticated users may read current controls.';
