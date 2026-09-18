-- RS KICKBOX backend foundation
-- Migration 0002: classes, bookings and attendance.

create table if not exists public.rs_classes (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    level text not null default 'ALL LEVELS',
    starts_at timestamptz not null,
    duration_minutes integer not null default 60 check (duration_minutes between 15 and 300),
    capacity integer not null default 16 check (capacity between 1 and 100),
    booking_open boolean not null default true,
    active boolean not null default true,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_classes_starts_at_idx on public.rs_classes(starts_at);
create index if not exists rs_classes_active_idx on public.rs_classes(active);

alter table public.rs_classes enable row level security;
revoke all on table public.rs_classes from anon, authenticated;
grant select on table public.rs_classes to authenticated;
grant insert, update, delete on table public.rs_classes to authenticated;

drop policy if exists "rs_classes_select_authenticated" on public.rs_classes;
create policy "rs_classes_select_authenticated"
on public.rs_classes
for select
to authenticated
using (true);

drop policy if exists "rs_classes_staff_insert" on public.rs_classes;
create policy "rs_classes_staff_insert"
on public.rs_classes
for insert
to authenticated
with check ((select private.rs_is_staff()));

drop policy if exists "rs_classes_staff_update" on public.rs_classes;
create policy "rs_classes_staff_update"
on public.rs_classes
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

drop policy if exists "rs_classes_staff_delete" on public.rs_classes;
create policy "rs_classes_staff_delete"
on public.rs_classes
for delete
to authenticated
using ((select private.rs_is_staff()));

create table if not exists public.rs_class_bookings (
    id uuid primary key default gen_random_uuid(),
    class_id uuid not null references public.rs_classes(id) on delete cascade,
    student_id uuid not null references auth.users(id) on delete cascade,
    status text not null default 'booked' check (status in ('booked','cancelled','waitlist')),
    booked_at timestamptz not null default now(),
    cancelled_at timestamptz,
    unique(class_id,student_id)
);

create index if not exists rs_class_bookings_class_idx on public.rs_class_bookings(class_id);
create index if not exists rs_class_bookings_student_idx on public.rs_class_bookings(student_id);

alter table public.rs_class_bookings enable row level security;
revoke all on table public.rs_class_bookings from anon, authenticated;
grant select, insert, update, delete on table public.rs_class_bookings to authenticated;

drop policy if exists "rs_bookings_select_own_or_staff" on public.rs_class_bookings;
create policy "rs_bookings_select_own_or_staff"
on public.rs_class_bookings
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_bookings_insert_own" on public.rs_class_bookings;
create policy "rs_bookings_insert_own"
on public.rs_class_bookings
for insert
to authenticated
with check ((select auth.uid()) = student_id);

drop policy if exists "rs_bookings_update_own_or_staff" on public.rs_class_bookings;
create policy "rs_bookings_update_own_or_staff"
on public.rs_class_bookings
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

drop policy if exists "rs_bookings_delete_own_or_staff" on public.rs_class_bookings;
create policy "rs_bookings_delete_own_or_staff"
on public.rs_class_bookings
for delete
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

create table if not exists public.rs_attendance (
    id uuid primary key default gen_random_uuid(),
    class_id uuid not null references public.rs_classes(id) on delete cascade,
    student_id uuid not null references auth.users(id) on delete cascade,
    present boolean not null default false,
    checked_in_at timestamptz,
    checked_in_by uuid references auth.users(id) on delete set null,
    note text not null default '',
    unique(class_id,student_id)
);

create index if not exists rs_attendance_class_idx on public.rs_attendance(class_id);
create index if not exists rs_attendance_student_idx on public.rs_attendance(student_id);

alter table public.rs_attendance enable row level security;
revoke all on table public.rs_attendance from anon, authenticated;
grant select on table public.rs_attendance to authenticated;
grant insert, update, delete on table public.rs_attendance to authenticated;

drop policy if exists "rs_attendance_select_own_or_staff" on public.rs_attendance;
create policy "rs_attendance_select_own_or_staff"
on public.rs_attendance
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_attendance_staff_insert" on public.rs_attendance;
create policy "rs_attendance_staff_insert"
on public.rs_attendance
for insert
to authenticated
with check ((select private.rs_is_staff()));

drop policy if exists "rs_attendance_staff_update" on public.rs_attendance;
create policy "rs_attendance_staff_update"
on public.rs_attendance
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

drop policy if exists "rs_attendance_staff_delete" on public.rs_attendance;
create policy "rs_attendance_staff_delete"
on public.rs_attendance
for delete
to authenticated
using ((select private.rs_is_staff()));

comment on table public.rs_classes is
'Trainer-managed class schedule. Booking capacity enforcement will be performed transactionally server-side before production.';

comment on table public.rs_class_bookings is
'Student class bookings. Production booking creation must verify capacity and booking state atomically.';

comment on table public.rs_attendance is
'Trainer-managed attendance records linked to classes and authenticated students.';
