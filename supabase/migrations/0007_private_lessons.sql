-- RS KICKBOX backend foundation
-- Migration 0007: trainer private availability and student lesson requests.

create table if not exists public.rs_private_slots (
    id uuid primary key default gen_random_uuid(),
    trainer_id uuid not null references auth.users(id) on delete cascade,
    starts_at timestamptz,
    day_label text not null default '',
    time_label text not null default '',
    duration_minutes integer not null default 60 check (duration_minutes between 15 and 180),
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_private_slots_trainer_idx on public.rs_private_slots(trainer_id);
create index if not exists rs_private_slots_starts_idx on public.rs_private_slots(starts_at);

alter table public.rs_private_slots enable row level security;
revoke all on table public.rs_private_slots from anon, authenticated;
grant select, insert, update, delete on table public.rs_private_slots to authenticated;

drop policy if exists "rs_private_slots_select_authenticated" on public.rs_private_slots;
create policy "rs_private_slots_select_authenticated"
on public.rs_private_slots
for select
to authenticated
using (active = true or (select private.rs_is_staff()));

drop policy if exists "rs_private_slots_staff_write" on public.rs_private_slots;
create policy "rs_private_slots_staff_write"
on public.rs_private_slots
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists public.rs_private_bookings (
    id uuid primary key default gen_random_uuid(),
    slot_id uuid not null references public.rs_private_slots(id) on delete cascade,
    student_id uuid not null references auth.users(id) on delete cascade,
    note text not null default '',
    status text not null default 'requested' check (status in ('requested','confirmed','declined','cancelled')),
    requested_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(slot_id,student_id)
);

create index if not exists rs_private_bookings_student_idx on public.rs_private_bookings(student_id);
create index if not exists rs_private_bookings_slot_idx on public.rs_private_bookings(slot_id);
create index if not exists rs_private_bookings_status_idx on public.rs_private_bookings(status);

create unique index if not exists rs_private_bookings_one_confirmed_per_slot
on public.rs_private_bookings(slot_id)
where status = 'confirmed';

alter table public.rs_private_bookings enable row level security;
revoke all on table public.rs_private_bookings from anon, authenticated;
grant select, insert, update, delete on table public.rs_private_bookings to authenticated;

drop policy if exists "rs_private_bookings_select_own_or_staff" on public.rs_private_bookings;
create policy "rs_private_bookings_select_own_or_staff"
on public.rs_private_bookings
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_private_bookings_insert_own" on public.rs_private_bookings;
create policy "rs_private_bookings_insert_own"
on public.rs_private_bookings
for insert
to authenticated
with check (
    (select auth.uid()) = student_id
    and status = 'requested'
);

drop policy if exists "rs_private_bookings_update_own_or_staff" on public.rs_private_bookings;
create policy "rs_private_bookings_update_own_or_staff"
on public.rs_private_bookings
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

drop policy if exists "rs_private_bookings_delete_own_or_staff" on public.rs_private_bookings;
create policy "rs_private_bookings_delete_own_or_staff"
on public.rs_private_bookings
for delete
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

comment on table public.rs_private_slots is
'Trainer-managed one-to-one lesson availability.';

comment on table public.rs_private_bookings is
'Student private-lesson requests with trainer confirmation state.';
