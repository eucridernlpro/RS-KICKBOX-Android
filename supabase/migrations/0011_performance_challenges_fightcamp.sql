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
