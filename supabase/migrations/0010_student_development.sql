-- RS KICKBOX backend foundation
-- Migration 0010: homework, private coach notes and assessments.

create table if not exists public.rs_homework (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    details text not null,
    due_label text not null default '',
    completed boolean not null default false,
    assigned_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_homework_student_idx on public.rs_homework(student_id,created_at desc);

alter table public.rs_homework enable row level security;
revoke all on table public.rs_homework from anon, authenticated;
grant select, insert, update, delete on table public.rs_homework to authenticated;

drop policy if exists "rs_homework_select_own_or_staff" on public.rs_homework;
create policy "rs_homework_select_own_or_staff"
on public.rs_homework
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_homework_staff_insert" on public.rs_homework;
create policy "rs_homework_staff_insert"
on public.rs_homework
for insert
to authenticated
with check ((select private.rs_is_staff()));

drop policy if exists "rs_homework_student_or_staff_update" on public.rs_homework;
create policy "rs_homework_student_or_staff_update"
on public.rs_homework
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

drop policy if exists "rs_homework_staff_delete" on public.rs_homework;
create policy "rs_homework_staff_delete"
on public.rs_homework
for delete
to authenticated
using ((select private.rs_is_staff()));

create table if not exists public.rs_coach_notes (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null references auth.users(id) on delete cascade,
    note text not null check (char_length(note) between 1 and 1500),
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now()
);

create index if not exists rs_coach_notes_student_idx on public.rs_coach_notes(student_id,created_at desc);

alter table public.rs_coach_notes enable row level security;
revoke all on table public.rs_coach_notes from anon, authenticated;
grant select, insert, delete on table public.rs_coach_notes to authenticated;

drop policy if exists "rs_coach_notes_staff_only_select" on public.rs_coach_notes;
create policy "rs_coach_notes_staff_only_select"
on public.rs_coach_notes
for select
to authenticated
using ((select private.rs_is_staff()));

drop policy if exists "rs_coach_notes_staff_insert" on public.rs_coach_notes;
create policy "rs_coach_notes_staff_insert"
on public.rs_coach_notes
for insert
to authenticated
with check ((select private.rs_is_staff()));

drop policy if exists "rs_coach_notes_staff_delete" on public.rs_coach_notes;
create policy "rs_coach_notes_staff_delete"
on public.rs_coach_notes
for delete
to authenticated
using ((select private.rs_is_staff()));

create table if not exists public.rs_assessments (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null references auth.users(id) on delete cascade,
    punches integer not null check (punches between 0 and 100),
    kicks integer not null check (kicks between 0 and 100),
    defense integer not null check (defense between 0 and 100),
    footwork integer not null check (footwork between 0 and 100),
    combinations integer not null check (combinations between 0 and 100),
    conditioning integer not null check (conditioning between 0 and 100),
    summary text not null default '',
    assessed_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now()
);

create index if not exists rs_assessments_student_idx on public.rs_assessments(student_id,created_at desc);

alter table public.rs_assessments enable row level security;
revoke all on table public.rs_assessments from anon, authenticated;
grant select, insert, delete on table public.rs_assessments to authenticated;

drop policy if exists "rs_assessments_select_own_or_staff" on public.rs_assessments;
create policy "rs_assessments_select_own_or_staff"
on public.rs_assessments
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_assessments_staff_insert" on public.rs_assessments;
create policy "rs_assessments_staff_insert"
on public.rs_assessments
for insert
to authenticated
with check ((select private.rs_is_staff()));

drop policy if exists "rs_assessments_staff_delete" on public.rs_assessments;
create policy "rs_assessments_staff_delete"
on public.rs_assessments
for delete
to authenticated
using ((select private.rs_is_staff()));

comment on table public.rs_homework is
'Trainer-assigned homework. Students may update their own completion state; content authoring remains staff-controlled.';

comment on table public.rs_coach_notes is
'Private staff-only development notes. These are intentionally not visible to students.';

comment on table public.rs_assessments is
'Trainer-recorded skill assessments that drive the student progress view.';
