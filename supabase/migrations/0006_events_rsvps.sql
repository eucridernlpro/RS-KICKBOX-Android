-- RS KICKBOX backend foundation
-- Migration 0006: events and per-student RSVPs.

create table if not exists public.rs_events (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    starts_at timestamptz,
    when_label text not null default '',
    location text not null default '',
    capacity integer not null default 20 check (capacity between 1 and 500),
    active boolean not null default true,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_events_active_idx on public.rs_events(active);
create index if not exists rs_events_starts_at_idx on public.rs_events(starts_at);

alter table public.rs_events enable row level security;
revoke all on table public.rs_events from anon, authenticated;
grant select on table public.rs_events to authenticated;
grant insert, update, delete on table public.rs_events to authenticated;

drop policy if exists "rs_events_select_authenticated" on public.rs_events;
create policy "rs_events_select_authenticated"
on public.rs_events
for select
to authenticated
using (active = true or (select private.rs_is_staff()));

drop policy if exists "rs_events_staff_insert" on public.rs_events;
create policy "rs_events_staff_insert"
on public.rs_events
for insert
to authenticated
with check ((select private.rs_is_staff()));

drop policy if exists "rs_events_staff_update" on public.rs_events;
create policy "rs_events_staff_update"
on public.rs_events
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

drop policy if exists "rs_events_staff_delete" on public.rs_events;
create policy "rs_events_staff_delete"
on public.rs_events
for delete
to authenticated
using ((select private.rs_is_staff()));

create table if not exists public.rs_event_rsvps (
    event_id uuid not null references public.rs_events(id) on delete cascade,
    student_id uuid not null references auth.users(id) on delete cascade,
    status text not null default 'going' check (status in ('going','cancelled')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key(event_id,student_id)
);

create index if not exists rs_event_rsvps_student_idx on public.rs_event_rsvps(student_id);

alter table public.rs_event_rsvps enable row level security;
revoke all on table public.rs_event_rsvps from anon, authenticated;
grant select, insert, update, delete on table public.rs_event_rsvps to authenticated;

drop policy if exists "rs_event_rsvps_select_own_or_staff" on public.rs_event_rsvps;
create policy "rs_event_rsvps_select_own_or_staff"
on public.rs_event_rsvps
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_event_rsvps_insert_own" on public.rs_event_rsvps;
create policy "rs_event_rsvps_insert_own"
on public.rs_event_rsvps
for insert
to authenticated
with check ((select auth.uid()) = student_id);

drop policy if exists "rs_event_rsvps_update_own_or_staff" on public.rs_event_rsvps;
create policy "rs_event_rsvps_update_own_or_staff"
on public.rs_event_rsvps
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

drop policy if exists "rs_event_rsvps_delete_own_or_staff" on public.rs_event_rsvps;
create policy "rs_event_rsvps_delete_own_or_staff"
on public.rs_event_rsvps
for delete
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

comment on table public.rs_events is
'Trainer/admin-managed club events. Production RSVP capacity enforcement should be performed transactionally server-side.';

comment on table public.rs_event_rsvps is
'Per-student event RSVP state.';
