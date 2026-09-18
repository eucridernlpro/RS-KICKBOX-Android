-- RS KICKBOX — FULL FRESH PROJECT SETUP
-- Generated from the ordered migrations in this repository.
-- Run this only on a fresh RS KICKBOX Supabase project.
-- Do not paste secret/service-role keys into SQL.


-- ============================================================
-- supabase/migrations/0001_core_identity_and_coaching.sql
-- ============================================================

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

-- Deliberately no direct client insert/update/delete grants here.
-- Staff invitation creation goes through a SECURITY DEFINER RPC so the raw
-- invitation token is never stored, while authenticated staff can create
-- the protected invite row without relying on a service-role PostgREST insert.

create or replace function public.rs_create_student_invite(
    p_email text,
    p_display_name text,
    p_plan text,
    p_token_hash text,
    p_expires_at timestamptz
)
returns table (
    id uuid,
    email text,
    display_name text,
    plan text,
    expires_at timestamptz
)
language plpgsql
security definer
set search_path = ''
as $
declare
    v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode = '42501';
    end if;

    if p_plan not in ('BASIC','PRO','ELITE') then
        raise exception 'invalid plan' using errcode = '22023';
    end if;

    insert into public.rs_student_invites (
        email,
        display_name,
        plan,
        token_hash,
        created_by,
        expires_at
    )
    values (
        lower(trim(p_email)),
        trim(p_display_name),
        p_plan,
        p_token_hash,
        (select auth.uid()),
        p_expires_at
    )
    returning rs_student_invites.id into v_id;

    return query
    select i.id,i.email,i.display_name,i.plan,i.expires_at
    from public.rs_student_invites i
    where i.id=v_id;
end;
$;

revoke execute on function public.rs_create_student_invite(text,text,text,text,timestamptz) from public;
revoke execute on function public.rs_create_student_invite(text,text,text,text,timestamptz) from anon;
grant execute on function public.rs_create_student_invite(text,text,text,text,timestamptz) to authenticated;

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

-- ============================================================
-- supabase/migrations/0002_classes_bookings_attendance.sql
-- ============================================================

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

-- ============================================================
-- supabase/migrations/0003_memberships_invoices_payments.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0003: memberships, invoices and payment records.

create table if not exists public.rs_memberships (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null unique references auth.users(id) on delete cascade,
    plan text not null default 'PRO' check (plan in ('BASIC','PRO','ELITE')),
    amount_cents integer not null default 4900 check (amount_cents >= 0),
    currency text not null default 'EUR',
    status text not null default 'active' check (status in ('active','paused','cancelled','past_due')),
    current_period_end timestamptz,
    provider text,
    provider_customer_id text,
    provider_subscription_id text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_memberships_status_idx on public.rs_memberships(status);

alter table public.rs_memberships enable row level security;
revoke all on table public.rs_memberships from anon, authenticated;
grant select on table public.rs_memberships to authenticated;
grant insert, update, delete on table public.rs_memberships to authenticated;

drop policy if exists "rs_memberships_select_own_or_staff" on public.rs_memberships;
create policy "rs_memberships_select_own_or_staff"
on public.rs_memberships
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_memberships_staff_write" on public.rs_memberships;
create policy "rs_memberships_staff_write"
on public.rs_memberships
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists public.rs_invoices (
    id uuid primary key default gen_random_uuid(),
    invoice_number text not null unique,
    student_id uuid not null references auth.users(id) on delete cascade,
    period_label text not null,
    amount_cents integer not null check (amount_cents >= 0),
    currency text not null default 'EUR',
    status text not null default 'pending' check (status in ('pending','paid','void','overdue')),
    due_at timestamptz,
    paid_at timestamptz,
    provider text,
    provider_invoice_id text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_invoices_student_idx on public.rs_invoices(student_id);
create index if not exists rs_invoices_status_idx on public.rs_invoices(status);
create index if not exists rs_invoices_created_idx on public.rs_invoices(created_at desc);

alter table public.rs_invoices enable row level security;
revoke all on table public.rs_invoices from anon, authenticated;
grant select on table public.rs_invoices to authenticated;
grant insert, update, delete on table public.rs_invoices to authenticated;

drop policy if exists "rs_invoices_select_own_or_staff" on public.rs_invoices;
create policy "rs_invoices_select_own_or_staff"
on public.rs_invoices
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_invoices_staff_write" on public.rs_invoices;
create policy "rs_invoices_staff_write"
on public.rs_invoices
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists public.rs_payments (
    id uuid primary key default gen_random_uuid(),
    invoice_id uuid references public.rs_invoices(id) on delete set null,
    student_id uuid not null references auth.users(id) on delete cascade,
    amount_cents integer not null check (amount_cents >= 0),
    currency text not null default 'EUR',
    method text not null,
    status text not null default 'pending' check (status in ('pending','succeeded','failed','refunded')),
    provider text,
    provider_payment_id text,
    paid_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists rs_payments_student_idx on public.rs_payments(student_id);
create index if not exists rs_payments_invoice_idx on public.rs_payments(invoice_id);
create index if not exists rs_payments_status_idx on public.rs_payments(status);

alter table public.rs_payments enable row level security;
revoke all on table public.rs_payments from anon, authenticated;
grant select on table public.rs_payments to authenticated;
grant insert, update, delete on table public.rs_payments to authenticated;

drop policy if exists "rs_payments_select_own_or_staff" on public.rs_payments;
create policy "rs_payments_select_own_or_staff"
on public.rs_payments
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_payments_staff_write" on public.rs_payments;
create policy "rs_payments_staff_write"
on public.rs_payments
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

comment on table public.rs_memberships is
'Membership state. Provider webhook updates must be performed server-side, never trusted from the Android client.';

comment on table public.rs_invoices is
'Invoice ledger visible to the owning student and staff. Production status changes come from staff/server workflows.';

comment on table public.rs_payments is
'Payment records. Provider secrets and webhook verification stay server-side.';

-- ============================================================
-- supabase/migrations/0004_account_privacy_requests.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0004: privacy export and account deletion requests.

create table if not exists public.rs_account_requests (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    request_type text not null check (request_type in ('data_export','account_deletion')),
    status text not null default 'requested' check (status in ('requested','processing','completed','rejected','cancelled')),
    user_note text not null default '',
    staff_note text not null default '',
    requested_at timestamptz not null default now(),
    completed_at timestamptz
);

create index if not exists rs_account_requests_user_idx on public.rs_account_requests(user_id);
create index if not exists rs_account_requests_status_idx on public.rs_account_requests(status);
create index if not exists rs_account_requests_requested_idx on public.rs_account_requests(requested_at desc);

alter table public.rs_account_requests enable row level security;
revoke all on table public.rs_account_requests from anon, authenticated;
grant select, insert on table public.rs_account_requests to authenticated;
grant update on table public.rs_account_requests to authenticated;

drop policy if exists "rs_account_requests_select_own_or_staff" on public.rs_account_requests;
create policy "rs_account_requests_select_own_or_staff"
on public.rs_account_requests
for select
to authenticated
using (
    (select auth.uid()) = user_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_account_requests_insert_own" on public.rs_account_requests;
create policy "rs_account_requests_insert_own"
on public.rs_account_requests
for insert
to authenticated
with check (
    (select auth.uid()) = user_id
    and status = 'requested'
);

drop policy if exists "rs_account_requests_staff_update" on public.rs_account_requests;
create policy "rs_account_requests_staff_update"
on public.rs_account_requests
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

comment on table public.rs_account_requests is
'Tracks GDPR/privacy data-export and account-deletion requests. Production account deletion itself should be executed server-side after policy/legal checks.';

-- ============================================================
-- supabase/migrations/0005_notifications.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0005: club notifications and per-user read state.

create table if not exists public.rs_notifications (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    message text not null,
    audience text not null default 'ALL' check (audience in ('ALL','BASIC','PRO','ELITE')),
    created_by uuid not null references auth.users(id) on delete restrict,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

create index if not exists rs_notifications_created_idx on public.rs_notifications(created_at desc);
create index if not exists rs_notifications_audience_idx on public.rs_notifications(audience);

alter table public.rs_notifications enable row level security;
revoke all on table public.rs_notifications from anon, authenticated;
grant select on table public.rs_notifications to authenticated;
grant insert, update, delete on table public.rs_notifications to authenticated;

drop policy if exists "rs_notifications_select_visible" on public.rs_notifications;
create policy "rs_notifications_select_visible"
on public.rs_notifications
for select
to authenticated
using (
    active = true
    and (
        (select private.rs_is_staff())
        or audience = 'ALL'
        or audience = (
            select p.plan
            from public.rs_profiles p
            where p.id = (select auth.uid())
              and p.active = true
            limit 1
        )
    )
);

drop policy if exists "rs_notifications_staff_insert" on public.rs_notifications;
create policy "rs_notifications_staff_insert"
on public.rs_notifications
for insert
to authenticated
with check ((select private.rs_is_staff()));

drop policy if exists "rs_notifications_staff_update" on public.rs_notifications;
create policy "rs_notifications_staff_update"
on public.rs_notifications
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

drop policy if exists "rs_notifications_staff_delete" on public.rs_notifications;
create policy "rs_notifications_staff_delete"
on public.rs_notifications
for delete
to authenticated
using ((select private.rs_is_staff()));

create table if not exists public.rs_notification_reads (
    notification_id uuid not null references public.rs_notifications(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    read_at timestamptz not null default now(),
    primary key(notification_id,user_id)
);

create index if not exists rs_notification_reads_user_idx on public.rs_notification_reads(user_id);

alter table public.rs_notification_reads enable row level security;
revoke all on table public.rs_notification_reads from anon, authenticated;
grant select, insert, delete on table public.rs_notification_reads to authenticated;

drop policy if exists "rs_notification_reads_select_own" on public.rs_notification_reads;
create policy "rs_notification_reads_select_own"
on public.rs_notification_reads
for select
to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists "rs_notification_reads_insert_own" on public.rs_notification_reads;
create policy "rs_notification_reads_insert_own"
on public.rs_notification_reads
for insert
to authenticated
with check ((select auth.uid()) = user_id);

drop policy if exists "rs_notification_reads_delete_own" on public.rs_notification_reads;
create policy "rs_notification_reads_delete_own"
on public.rs_notification_reads
for delete
to authenticated
using ((select auth.uid()) = user_id);

comment on table public.rs_notifications is
'Trainer/admin club notifications filtered by membership plan through RLS. Push delivery will be added separately.';

comment on table public.rs_notification_reads is
'Per-user notification read state.';

-- ============================================================
-- supabase/migrations/0006_events_rsvps.sql
-- ============================================================

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

-- ============================================================
-- supabase/migrations/0007_private_lessons.sql
-- ============================================================

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

-- ============================================================
-- supabase/migrations/0008_coach_messaging.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0008: private coach messaging and read receipts.

create table if not exists public.rs_coach_messages (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null references auth.users(id) on delete cascade,
    sender_id uuid not null references auth.users(id) on delete cascade,
    sender_role text not null check (sender_role in ('student','trainer','admin')),
    body text not null check (char_length(body) between 1 and 1200),
    created_at timestamptz not null default now()
);

create index if not exists rs_coach_messages_student_idx
on public.rs_coach_messages(student_id, created_at);

alter table public.rs_coach_messages enable row level security;
revoke all on table public.rs_coach_messages from anon, authenticated;
grant select, insert on table public.rs_coach_messages to authenticated;

drop policy if exists "rs_coach_messages_select_thread" on public.rs_coach_messages;
create policy "rs_coach_messages_select_thread"
on public.rs_coach_messages
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_coach_messages_insert_student" on public.rs_coach_messages;
create policy "rs_coach_messages_insert_student"
on public.rs_coach_messages
for insert
to authenticated
with check (
    (select auth.uid()) = sender_id
    and (select auth.uid()) = student_id
    and sender_role = 'student'
);

drop policy if exists "rs_coach_messages_insert_staff" on public.rs_coach_messages;
create policy "rs_coach_messages_insert_staff"
on public.rs_coach_messages
for insert
to authenticated
with check (
    (select auth.uid()) = sender_id
    and sender_role in ('trainer','admin')
    and (select private.rs_is_staff())
);

create table if not exists public.rs_coach_message_reads (
    user_id uuid not null references auth.users(id) on delete cascade,
    student_id uuid not null references auth.users(id) on delete cascade,
    last_read_at timestamptz not null default now(),
    primary key(user_id,student_id)
);

alter table public.rs_coach_message_reads enable row level security;
revoke all on table public.rs_coach_message_reads from anon, authenticated;
grant select, insert, update on table public.rs_coach_message_reads to authenticated;

drop policy if exists "rs_coach_reads_select_own_or_staff" on public.rs_coach_message_reads;
create policy "rs_coach_reads_select_own_or_staff"
on public.rs_coach_message_reads
for select
to authenticated
using (
    (select auth.uid()) = user_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_coach_reads_insert_own" on public.rs_coach_message_reads;
create policy "rs_coach_reads_insert_own"
on public.rs_coach_message_reads
for insert
to authenticated
with check (
    (select auth.uid()) = user_id
    and (
        user_id = student_id
        or (select private.rs_is_staff())
    )
);

drop policy if exists "rs_coach_reads_update_own" on public.rs_coach_message_reads;
create policy "rs_coach_reads_update_own"
on public.rs_coach_message_reads
for update
to authenticated
using ((select auth.uid()) = user_id)
with check (
    (select auth.uid()) = user_id
    and (
        user_id = student_id
        or (select private.rs_is_staff())
    )
);

comment on table public.rs_coach_messages is
'Immutable private coach/student messages. Message rows are insert/select only; edits and client-side deletes are intentionally not granted.';

comment on table public.rs_coach_message_reads is
'Per-user read cursor for each private student coaching thread.';

-- ============================================================
-- supabase/migrations/0009_promotions_books.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0009: promotions, books and controlled book access.

create table if not exists public.rs_promotions (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    image_path text not null,
    external_url text not null,
    active boolean not null default true,
    sort_order integer not null default 0,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_promotions_active_sort_idx
on public.rs_promotions(active,sort_order,created_at desc);

alter table public.rs_promotions enable row level security;
revoke all on table public.rs_promotions from anon, authenticated;
grant select on table public.rs_promotions to authenticated;
grant insert, update, delete on table public.rs_promotions to authenticated;

drop policy if exists "rs_promotions_select_active" on public.rs_promotions;
create policy "rs_promotions_select_active"
on public.rs_promotions
for select
to authenticated
using (active = true or (select private.rs_is_staff()));

drop policy if exists "rs_promotions_staff_write" on public.rs_promotions;
create policy "rs_promotions_staff_write"
on public.rs_promotions
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists public.rs_books (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    cover_path text,
    amazon_url text,
    preview_path text,
    full_path text,
    access_tier text not null default 'PRO' check (access_tier in ('ALL','BASIC','PRO','ELITE','PRIVATE')),
    active boolean not null default true,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.rs_books enable row level security;
revoke all on table public.rs_books from anon, authenticated;
grant select on table public.rs_books to authenticated;
grant insert, update, delete on table public.rs_books to authenticated;

drop policy if exists "rs_books_select_authenticated" on public.rs_books;
create policy "rs_books_select_authenticated"
on public.rs_books
for select
to authenticated
using (active = true or (select private.rs_is_staff()));

drop policy if exists "rs_books_staff_write" on public.rs_books;
create policy "rs_books_staff_write"
on public.rs_books
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

create table if not exists public.rs_book_grants (
    book_id uuid not null references public.rs_books(id) on delete cascade,
    student_id uuid not null references auth.users(id) on delete cascade,
    granted_by uuid not null references auth.users(id) on delete restrict,
    reason text not null default 'manual',
    created_at timestamptz not null default now(),
    primary key(book_id,student_id)
);

create index if not exists rs_book_grants_student_idx on public.rs_book_grants(student_id);

alter table public.rs_book_grants enable row level security;
revoke all on table public.rs_book_grants from anon, authenticated;
grant select on table public.rs_book_grants to authenticated;
grant insert, delete on table public.rs_book_grants to authenticated;

drop policy if exists "rs_book_grants_select_own_or_staff" on public.rs_book_grants;
create policy "rs_book_grants_select_own_or_staff"
on public.rs_book_grants
for select
to authenticated
using (
    (select auth.uid()) = student_id
    or (select private.rs_is_staff())
);

drop policy if exists "rs_book_grants_staff_write" on public.rs_book_grants;
create policy "rs_book_grants_staff_write"
on public.rs_book_grants
for all
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

comment on table public.rs_promotions is
'Trainer-managed clickable promotion thumbnails. image_path points to managed media storage; external_url is the click destination.';

comment on table public.rs_books is
'Book metadata and access tier. Preview/full PDF objects should be stored in private storage; full_path must never be treated as a public URL.';

comment on table public.rs_book_grants is
'Manual per-student full-book grants that override subscription access. Production file delivery should use server-validated signed URLs.';

-- ============================================================
-- supabase/migrations/0010_student_development.sql
-- ============================================================

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
