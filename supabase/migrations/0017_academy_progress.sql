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
