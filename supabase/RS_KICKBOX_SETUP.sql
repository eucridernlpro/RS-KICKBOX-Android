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


-- Redemption lookup/finalization RPCs.
-- The invitation token is a high-entropy bearer secret. These functions expose
-- only the exact matching invite and atomically finalize a matching auth user.

create or replace function public.rs_lookup_student_invite(
    p_email text,
    p_token_hash text
)
returns table (
    id uuid,
    email text,
    display_name text,
    plan text,
    expires_at timestamptz,
    redeemed_at timestamptz,
    revoked_at timestamptz
)
language sql
security definer
set search_path = ''
as $$
    select
        i.id,
        i.email,
        i.display_name,
        i.plan,
        i.expires_at,
        i.redeemed_at,
        i.revoked_at
    from public.rs_student_invites i
    where i.email = lower(trim(p_email))
      and i.token_hash = p_token_hash
    limit 1;
$$;

revoke execute on function public.rs_lookup_student_invite(text,text) from public;
grant execute on function public.rs_lookup_student_invite(text,text) to anon, authenticated;

create or replace function public.rs_finalize_student_invite(
    p_invite_id uuid,
    p_email text,
    p_token_hash text,
    p_user_id uuid
)
returns table (
    email text,
    display_name text,
    plan text
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_invite public.rs_student_invites%rowtype;
    v_auth_email text;
begin
    select *
    into v_invite
    from public.rs_student_invites i
    where i.id = p_invite_id
      and i.email = lower(trim(p_email))
      and i.token_hash = p_token_hash
    for update;

    if v_invite.id is null then
        raise exception 'invitation not found' using errcode = 'P0002';
    end if;
    if v_invite.revoked_at is not null then
        raise exception 'invitation revoked' using errcode = 'P0001';
    end if;
    if v_invite.redeemed_at is not null then
        raise exception 'invitation already used' using errcode = 'P0001';
    end if;
    if v_invite.expires_at <= now() then
        raise exception 'invitation expired' using errcode = 'P0001';
    end if;

    select lower(u.email)
    into v_auth_email
    from auth.users u
    where u.id = p_user_id;

    if v_auth_email is null or v_auth_email <> v_invite.email then
        raise exception 'auth user does not match invitation' using errcode = '42501';
    end if;

    if (
        select count(*)
        from public.rs_profiles p
        where p.role='student' and p.active=true
    ) >= 100 then
        raise exception 'active student limit reached' using errcode = 'P0001';
    end if;

    insert into public.rs_profiles (
        id,email,display_name,role,plan,active
    )
    values (
        p_user_id,
        v_invite.email,
        v_invite.display_name,
        'student',
        v_invite.plan,
        true
    )
    on conflict (id) do update
    set
        email=excluded.email,
        display_name=excluded.display_name,
        role='student',
        plan=excluded.plan,
        active=true,
        updated_at=now();

    update public.rs_student_invites
    set redeemed_at=now()
    where id=v_invite.id;

    return query
    select v_invite.email,v_invite.display_name,v_invite.plan;
end;
$$;

revoke execute on function public.rs_finalize_student_invite(uuid,text,text,uuid) from public;
grant execute on function public.rs_finalize_student_invite(uuid,text,text,uuid) to anon, authenticated;


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


-- ============================================================
-- supabase/migrations/0020_profile_avatars.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0020: protected member profile avatars.

alter table public.rs_profiles
add column if not exists avatar_path text;

insert into storage.buckets (
    id,
    name,
    public,
    file_size_limit,
    allowed_mime_types
)
values (
    'rs-profile-images',
    'rs-profile-images',
    false,
    2097152,
    array['image/jpeg','image/png','image/webp']
)
on conflict (id) do update
set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "rs_profile_images_select_visible" on storage.objects;
create policy "rs_profile_images_select_visible"
on storage.objects
for select
to authenticated
using (
    bucket_id = 'rs-profile-images'
    and exists (
        select 1
        from public.rs_profiles p
        left join public.rs_social_profiles s on s.user_id = p.id
        where p.id::text = (storage.foldername(name))[1]
          and (
              p.id = (select auth.uid())
              or (select private.rs_is_staff())
              or coalesce(s.public_profile,false) = true
          )
    )
);

drop policy if exists "rs_profile_images_insert_own" on storage.objects;
create policy "rs_profile_images_insert_own"
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

drop policy if exists "rs_profile_images_update_own" on storage.objects;
create policy "rs_profile_images_update_own"
on storage.objects
for update
to authenticated
using (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
)
with check (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

drop policy if exists "rs_profile_images_delete_own" on storage.objects;
create policy "rs_profile_images_delete_own"
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create or replace function public.rs_set_my_avatar(p_avatar_path text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_expected text;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    v_expected := v_uid::text || '/avatar.jpg';

    if p_avatar_path is distinct from v_expected then
        raise exception 'invalid avatar path' using errcode='22023';
    end if;

    update public.rs_profiles
    set avatar_path = p_avatar_path,
        updated_at = now()
    where id = v_uid;
end;
$$;

revoke execute on function public.rs_set_my_avatar(text) from public;
revoke execute on function public.rs_set_my_avatar(text) from anon;
grant execute on function public.rs_set_my_avatar(text) to authenticated;

create or replace function public.rs_member_identity(p_email text)
returns table (
    id uuid,
    display_name text,
    avatar_path text
)
language sql
stable
security definer
set search_path = ''
as $$
    select p.id,p.display_name,p.avatar_path
    from public.rs_profiles p
    left join public.rs_social_profiles s on s.user_id=p.id
    where lower(p.email)=lower(trim(p_email))
      and (
          p.id=(select auth.uid())
          or (select private.rs_is_staff())
          or coalesce(s.public_profile,false)=true
      )
    limit 1;
$$;

revoke execute on function public.rs_member_identity(text) from public;
revoke execute on function public.rs_member_identity(text) from anon;
grant execute on function public.rs_member_identity(text) to authenticated;

comment on column public.rs_profiles.avatar_path is
'Protected Supabase Storage path for the member profile avatar.';

comment on function public.rs_member_identity(text) is
'Returns display identity/avatar only to the member, staff, or when the social profile is public.';


-- ============================================================
-- supabase/migrations/0021_cloud_classes_bookings.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0021: production class catalog and atomic booking actions.

create or replace function public.rs_class_catalog()
returns table (
    id uuid,
    title text,
    level text,
    starts_at timestamptz,
    duration_minutes integer,
    capacity integer,
    booking_open boolean,
    active boolean,
    booked_count integer,
    my_booking_status text
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        c.id,
        c.title,
        c.level,
        c.starts_at,
        c.duration_minutes,
        c.capacity,
        c.booking_open,
        c.active,
        (
            select count(*)::integer
            from public.rs_class_bookings b
            where b.class_id=c.id
              and b.status='booked'
        ) as booked_count,
        (
            select b2.status
            from public.rs_class_bookings b2
            where b2.class_id=c.id
              and b2.student_id=(select auth.uid())
            limit 1
        ) as my_booking_status
    from public.rs_classes c
    where c.active=true
       or (select private.rs_is_staff())
    order by c.starts_at asc;
$$;

revoke execute on function public.rs_class_catalog() from public;
revoke execute on function public.rs_class_catalog() from anon;
grant execute on function public.rs_class_catalog() to authenticated;

create or replace function public.rs_book_class(p_class_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_class public.rs_classes%rowtype;
    v_count integer;
    v_role text;
    v_active boolean;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select p.role,p.active
    into v_role,v_active
    from public.rs_profiles p
    where p.id=v_uid;

    if v_role <> 'student' or coalesce(v_active,false)=false then
        raise exception 'active student account required' using errcode='42501';
    end if;

    select *
    into v_class
    from public.rs_classes c
    where c.id=p_class_id
    for update;

    if v_class.id is null then
        raise exception 'class not found' using errcode='P0002';
    end if;
    if not v_class.active then
        raise exception 'class inactive' using errcode='P0001';
    end if;
    if not v_class.booking_open then
        raise exception 'booking closed' using errcode='P0001';
    end if;

    select count(*)
    into v_count
    from public.rs_class_bookings b
    where b.class_id=p_class_id
      and b.status='booked';

    if v_count >= v_class.capacity then
        raise exception 'class full' using errcode='P0001';
    end if;

    insert into public.rs_class_bookings (
        class_id,student_id,status,booked_at,cancelled_at
    )
    values (
        p_class_id,v_uid,'booked',now(),null
    )
    on conflict (class_id,student_id) do update
    set
        status='booked',
        booked_at=now(),
        cancelled_at=null;

    return 'booked';
end;
$$;

revoke execute on function public.rs_book_class(uuid) from public;
revoke execute on function public.rs_book_class(uuid) from anon;
grant execute on function public.rs_book_class(uuid) to authenticated;

create or replace function public.rs_cancel_class_booking(p_class_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    update public.rs_class_bookings
    set status='cancelled',
        cancelled_at=now()
    where class_id=p_class_id
      and student_id=v_uid
      and status <> 'cancelled';

    if not found then
        raise exception 'active booking not found' using errcode='P0002';
    end if;

    return 'cancelled';
end;
$$;

revoke execute on function public.rs_cancel_class_booking(uuid) from public;
revoke execute on function public.rs_cancel_class_booking(uuid) from anon;
grant execute on function public.rs_cancel_class_booking(uuid) to authenticated;

comment on function public.rs_class_catalog() is
'Authenticated class catalog with live booked count and caller booking state.';
comment on function public.rs_book_class(uuid) is
'Atomic student booking action with row lock and server-side capacity enforcement.';
comment on function public.rs_cancel_class_booking(uuid) is
'Cancels the authenticated student booking for one class.';


create or replace function public.rs_staff_create_class(
    p_title text,
    p_level text,
    p_starts_at timestamptz,
    p_duration_minutes integer,
    p_capacity integer
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if trim(p_title)='' then
        raise exception 'class title required' using errcode='22023';
    end if;
    if p_duration_minutes < 15 or p_duration_minutes > 300 then
        raise exception 'invalid duration' using errcode='22023';
    end if;
    if p_capacity < 1 or p_capacity > 100 then
        raise exception 'invalid capacity' using errcode='22023';
    end if;

    insert into public.rs_classes(
        title,level,starts_at,duration_minutes,capacity,
        booking_open,active,created_by
    )
    values(
        trim(p_title),
        coalesce(nullif(trim(p_level),''),'ALL LEVELS'),
        p_starts_at,
        p_duration_minutes,
        p_capacity,
        true,
        true,
        (select auth.uid())
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_class(text,text,timestamptz,integer,integer) from public;
revoke execute on function public.rs_staff_create_class(text,text,timestamptz,integer,integer) from anon;
grant execute on function public.rs_staff_create_class(text,text,timestamptz,integer,integer) to authenticated;

create or replace function public.rs_staff_set_class_state(
    p_class_id uuid,
    p_active boolean,
    p_booking_open boolean
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    update public.rs_classes
    set active=p_active,
        booking_open=p_booking_open,
        updated_at=now()
    where id=p_class_id;

    if not found then
        raise exception 'class not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_class_state(uuid,boolean,boolean) from public;
revoke execute on function public.rs_staff_set_class_state(uuid,boolean,boolean) from anon;
grant execute on function public.rs_staff_set_class_state(uuid,boolean,boolean) to authenticated;

create or replace function public.rs_staff_delete_class(p_class_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_classes
    where id=p_class_id;

    if not found then
        raise exception 'class not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_class(uuid) from public;
revoke execute on function public.rs_staff_delete_class(uuid) from anon;
grant execute on function public.rs_staff_delete_class(uuid) to authenticated;


-- ============================================================
-- supabase/migrations/0022_cloud_attendance.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0022: cloud attendance roster and trainer check-in actions.

create or replace function public.rs_attendance_roster(p_class_id uuid)
returns table (
    student_id uuid,
    display_name text,
    email text,
    booked boolean,
    present boolean,
    checked_in_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.id,
        p.display_name,
        p.email,
        exists(
            select 1
            from public.rs_class_bookings b
            where b.class_id=p_class_id
              and b.student_id=p.id
              and b.status='booked'
        ) as booked,
        coalesce(a.present,false) as present,
        a.checked_in_at
    from public.rs_profiles p
    left join public.rs_attendance a
      on a.class_id=p_class_id
     and a.student_id=p.id
    where p.role='student'
      and p.active=true
      and (select private.rs_is_staff())
    order by
        exists(
            select 1
            from public.rs_class_bookings b2
            where b2.class_id=p_class_id
              and b2.student_id=p.id
              and b2.status='booked'
        ) desc,
        lower(p.display_name),
        lower(p.email);
$$;

revoke execute on function public.rs_attendance_roster(uuid) from public;
revoke execute on function public.rs_attendance_roster(uuid) from anon;
grant execute on function public.rs_attendance_roster(uuid) to authenticated;

create or replace function public.rs_staff_set_attendance(
    p_class_id uuid,
    p_student_id uuid,
    p_present boolean,
    p_note text default ''
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if not exists(
        select 1 from public.rs_classes c where c.id=p_class_id
    ) then
        raise exception 'class not found' using errcode='P0002';
    end if;

    if not exists(
        select 1
        from public.rs_profiles p
        where p.id=p_student_id
          and p.role='student'
          and p.active=true
    ) then
        raise exception 'active student not found' using errcode='P0002';
    end if;

    insert into public.rs_attendance(
        class_id,
        student_id,
        present,
        checked_in_at,
        checked_in_by,
        note
    )
    values(
        p_class_id,
        p_student_id,
        p_present,
        case when p_present then now() else null end,
        (select auth.uid()),
        coalesce(p_note,'')
    )
    on conflict (class_id,student_id) do update
    set
        present=excluded.present,
        checked_in_at=excluded.checked_in_at,
        checked_in_by=(select auth.uid()),
        note=excluded.note;
end;
$$;

revoke execute on function public.rs_staff_set_attendance(uuid,uuid,boolean,text) from public;
revoke execute on function public.rs_staff_set_attendance(uuid,uuid,boolean,text) from anon;
grant execute on function public.rs_staff_set_attendance(uuid,uuid,boolean,text) to authenticated;

comment on function public.rs_attendance_roster(uuid) is
'Trainer/admin attendance roster with booking and check-in state for all active students.';
comment on function public.rs_staff_set_attendance(uuid,uuid,boolean,text) is
'Staff-only attendance upsert for one student and class.';


-- ============================================================
-- supabase/migrations/0023_cloud_memberships_access.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0023: production membership plans and student access control.

create or replace function public.rs_membership_plan_catalog()
returns table (
    code text,
    name text,
    monthly_cents integer,
    currency text,
    description text,
    active boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.code,
        p.name,
        p.monthly_cents,
        p.currency,
        p.description,
        p.active
    from public.rs_membership_plans p
    order by case p.code when 'BASIC' then 1 when 'PRO' then 2 else 3 end;
$$;

revoke execute on function public.rs_membership_plan_catalog() from public;
revoke execute on function public.rs_membership_plan_catalog() from anon;
grant execute on function public.rs_membership_plan_catalog() to authenticated;

create or replace function public.rs_staff_update_membership_plan(
    p_code text,
    p_monthly_cents integer,
    p_description text,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_code not in ('BASIC','PRO','ELITE') then
        raise exception 'invalid plan' using errcode='22023';
    end if;

    if p_monthly_cents < 0 then
        raise exception 'invalid monthly price' using errcode='22023';
    end if;

    update public.rs_membership_plans
    set
        monthly_cents=p_monthly_cents,
        description=coalesce(p_description,''),
        active=p_active,
        updated_by=(select auth.uid()),
        updated_at=now()
    where code=p_code;

    if not found then
        raise exception 'membership plan not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_update_membership_plan(text,integer,text,boolean) from public;
revoke execute on function public.rs_staff_update_membership_plan(text,integer,text,boolean) from anon;
grant execute on function public.rs_staff_update_membership_plan(text,integer,text,boolean) to authenticated;

create or replace function public.rs_staff_student_access_catalog()
returns table (
    id uuid,
    email text,
    display_name text,
    plan text,
    active boolean,
    membership_status text,
    amount_cents integer,
    current_period_end timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.id,
        p.email,
        p.display_name,
        p.plan,
        p.active,
        coalesce(m.status,'active') as membership_status,
        coalesce(m.amount_cents,mp.monthly_cents,0) as amount_cents,
        m.current_period_end
    from public.rs_profiles p
    left join public.rs_memberships m on m.student_id=p.id
    left join public.rs_membership_plans mp on mp.code=p.plan
    where p.role='student'
      and (select private.rs_is_staff())
    order by lower(p.display_name),lower(p.email);
$$;

revoke execute on function public.rs_staff_student_access_catalog() from public;
revoke execute on function public.rs_staff_student_access_catalog() from anon;
grant execute on function public.rs_staff_student_access_catalog() to authenticated;

create or replace function public.rs_staff_set_student_access(
    p_student_id uuid,
    p_plan text,
    p_active boolean,
    p_membership_status text default 'active'
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_amount integer;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_plan not in ('BASIC','PRO','ELITE') then
        raise exception 'invalid plan' using errcode='22023';
    end if;

    if p_membership_status not in ('active','paused','cancelled','past_due') then
        raise exception 'invalid membership status' using errcode='22023';
    end if;

    if not exists(
        select 1
        from public.rs_profiles p
        where p.id=p_student_id and p.role='student'
    ) then
        raise exception 'student not found' using errcode='P0002';
    end if;

    select mp.monthly_cents
    into v_amount
    from public.rs_membership_plans mp
    where mp.code=p_plan;

    update public.rs_profiles
    set
        plan=p_plan,
        active=p_active,
        updated_at=now()
    where id=p_student_id;

    insert into public.rs_memberships(
        student_id,
        plan,
        amount_cents,
        currency,
        status,
        updated_at
    )
    values(
        p_student_id,
        p_plan,
        coalesce(v_amount,0),
        'EUR',
        p_membership_status,
        now()
    )
    on conflict (student_id) do update
    set
        plan=excluded.plan,
        amount_cents=excluded.amount_cents,
        currency=excluded.currency,
        status=excluded.status,
        updated_at=now();
end;
$$;

revoke execute on function public.rs_staff_set_student_access(uuid,text,boolean,text) from public;
revoke execute on function public.rs_staff_set_student_access(uuid,text,boolean,text) from anon;
grant execute on function public.rs_staff_set_student_access(uuid,text,boolean,text) to authenticated;

create or replace function public.rs_my_membership()
returns table (
    plan text,
    active boolean,
    membership_status text,
    amount_cents integer,
    currency text,
    current_period_end timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.plan,
        p.active,
        coalesce(m.status,'active'),
        coalesce(m.amount_cents,mp.monthly_cents,0),
        coalesce(m.currency,mp.currency,'EUR'),
        m.current_period_end
    from public.rs_profiles p
    left join public.rs_memberships m on m.student_id=p.id
    left join public.rs_membership_plans mp on mp.code=p.plan
    where p.id=(select auth.uid())
    limit 1;
$$;

revoke execute on function public.rs_my_membership() from public;
revoke execute on function public.rs_my_membership() from anon;
grant execute on function public.rs_my_membership() to authenticated;


-- ============================================================
-- supabase/migrations/0024_cloud_coach_messaging.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0024: production private coach messaging.

create or replace function public.rs_coach_threads()
returns table (
    student_id uuid,
    student_email text,
    student_name text,
    last_message text,
    last_message_at timestamptz,
    unread_count integer
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.id,
        p.email,
        p.display_name,
        (
            select m.body
            from public.rs_coach_messages m
            where m.student_id=p.id
            order by m.created_at desc
            limit 1
        ) as last_message,
        (
            select m.created_at
            from public.rs_coach_messages m
            where m.student_id=p.id
            order by m.created_at desc
            limit 1
        ) as last_message_at,
        (
            select count(*)::integer
            from public.rs_coach_messages m
            where m.student_id=p.id
              and m.sender_role='student'
              and m.created_at > coalesce(
                  (
                      select r.last_read_at
                      from public.rs_coach_message_reads r
                      where r.user_id=(select auth.uid())
                        and r.student_id=p.id
                  ),
                  to_timestamp(0)
              )
        ) as unread_count
    from public.rs_profiles p
    where p.role='student'
      and p.active=true
      and (select private.rs_is_staff())
    order by last_message_at desc nulls last,lower(p.display_name);
$$;

revoke execute on function public.rs_coach_threads() from public;
revoke execute on function public.rs_coach_threads() from anon;
grant execute on function public.rs_coach_threads() to authenticated;

create or replace function public.rs_coach_thread_messages(p_student_id uuid)
returns table (
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    sender_role text,
    body text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        m.id,
        m.student_id,
        p.email,
        p.display_name,
        m.sender_role,
        m.body,
        m.created_at
    from public.rs_coach_messages m
    join public.rs_profiles p on p.id=m.student_id
    where m.student_id=p_student_id
      and (
          (select private.rs_is_staff())
          or (select auth.uid())=p_student_id
      )
    order by m.created_at asc;
$$;

revoke execute on function public.rs_coach_thread_messages(uuid) from public;
revoke execute on function public.rs_coach_thread_messages(uuid) from anon;
grant execute on function public.rs_coach_thread_messages(uuid) to authenticated;

create or replace function public.rs_send_coach_message(
    p_student_id uuid,
    p_body text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_role text;
    v_message_id uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if length(trim(p_body)) < 1 or length(trim(p_body)) > 1200 then
        raise exception 'message must contain 1 to 1200 characters' using errcode='22023';
    end if;

    select p.role into v_role
    from public.rs_profiles p
    where p.id=v_uid and p.active=true;

    if v_role='student' then
        if v_uid <> p_student_id then
            raise exception 'students may only message their own coach thread' using errcode='42501';
        end if;
    elsif v_role not in ('trainer','admin') then
        raise exception 'active member account required' using errcode='42501';
    end if;

    if not exists(
        select 1 from public.rs_profiles p
        where p.id=p_student_id and p.role='student' and p.active=true
    ) then
        raise exception 'active student not found' using errcode='P0002';
    end if;

    insert into public.rs_coach_messages(
        student_id,
        sender_id,
        sender_role,
        body
    )
    values(
        p_student_id,
        v_uid,
        case when v_role='student' then 'student' else 'trainer' end,
        trim(p_body)
    )
    returning id into v_message_id;

    return v_message_id;
end;
$$;

revoke execute on function public.rs_send_coach_message(uuid,text) from public;
revoke execute on function public.rs_send_coach_message(uuid,text) from anon;
grant execute on function public.rs_send_coach_message(uuid,text) to authenticated;

create or replace function public.rs_mark_coach_thread_read(p_student_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_role text;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select p.role into v_role
    from public.rs_profiles p
    where p.id=v_uid and p.active=true;

    if v_role='student' and v_uid<>p_student_id then
        raise exception 'students may only mark their own thread' using errcode='42501';
    end if;
    if v_role not in ('student','trainer','admin') then
        raise exception 'active member account required' using errcode='42501';
    end if;

    insert into public.rs_coach_message_reads(
        user_id,
        student_id,
        last_read_at
    )
    values(
        v_uid,
        p_student_id,
        now()
    )
    on conflict (user_id,student_id) do update
    set last_read_at=excluded.last_read_at;
end;
$$;

revoke execute on function public.rs_mark_coach_thread_read(uuid) from public;
revoke execute on function public.rs_mark_coach_thread_read(uuid) from anon;
grant execute on function public.rs_mark_coach_thread_read(uuid) to authenticated;

create or replace function public.rs_my_student_id()
returns table (student_id uuid)
language sql
stable
security definer
set search_path = ''
as $
    select p.id
    from public.rs_profiles p
    where p.id=(select auth.uid())
      and p.role='student'
      and p.active=true
    limit 1;
$;

revoke execute on function public.rs_my_student_id() from public;
revoke execute on function public.rs_my_student_id() from anon;
grant execute on function public.rs_my_student_id() to authenticated;


-- ============================================================
-- supabase/migrations/0025_cloud_training_media.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0025: protected cloud training media storage and catalog actions.

insert into storage.buckets (
    id,
    name,
    public,
    file_size_limit,
    allowed_mime_types
)
values (
    'rs-training-media',
    'rs-training-media',
    false,
    104857600,
    array[
        'video/mp4',
        'video/webm',
        'video/quicktime',
        'image/jpeg',
        'image/png',
        'image/webp',
        'image/gif'
    ]
)
on conflict (id) do update
set
    public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "rs_training_storage_staff_insert" on storage.objects;
create policy "rs_training_storage_staff_insert"
on storage.objects
for insert
to authenticated
with check (
    bucket_id='rs-training-media'
    and (select private.rs_is_staff())
);

drop policy if exists "rs_training_storage_visible_select" on storage.objects;
create policy "rs_training_storage_visible_select"
on storage.objects
for select
to authenticated
using (
    bucket_id='rs-training-media'
    and (
        (select private.rs_is_staff())
        or exists(
            select 1
            from public.rs_training_media m
            join public.rs_profiles p on p.id=(select auth.uid())
            where m.media_path=name
              and m.published=true
              and p.active=true
              and p.role='student'
              and (
                  m.access_tier='ALL'
                  or m.access_tier='BASIC'
                  or (m.access_tier='PRO' and p.plan in ('PRO','ELITE'))
                  or (m.access_tier='ELITE' and p.plan='ELITE')
              )
        )
    )
);

drop policy if exists "rs_training_storage_staff_update" on storage.objects;
create policy "rs_training_storage_staff_update"
on storage.objects
for update
to authenticated
using (
    bucket_id='rs-training-media'
    and (select private.rs_is_staff())
)
with check (
    bucket_id='rs-training-media'
    and (select private.rs_is_staff())
);

drop policy if exists "rs_training_storage_staff_delete" on storage.objects;
create policy "rs_training_storage_staff_delete"
on storage.objects
for delete
to authenticated
using (
    bucket_id='rs-training-media'
    and (select private.rs_is_staff())
);

create or replace function public.rs_staff_create_training_media(
    p_title text,
    p_category text,
    p_description text,
    p_media_path text,
    p_media_kind text,
    p_access_tier text,
    p_published boolean
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if trim(p_title)='' then
        raise exception 'title required' using errcode='22023';
    end if;
    if p_media_kind not in ('VIDEO','IMAGE','GIF') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;
    if p_access_tier not in ('ALL','BASIC','PRO','ELITE') then
        raise exception 'invalid access tier' using errcode='22023';
    end if;
    if trim(p_media_path)='' then
        raise exception 'media path required' using errcode='22023';
    end if;

    insert into public.rs_training_media(
        title,
        category,
        description,
        media_path,
        media_kind,
        access_tier,
        published,
        created_by
    )
    values(
        trim(p_title),
        coalesce(nullif(trim(p_category),''),'TECHNIQUE'),
        coalesce(p_description,''),
        trim(p_media_path),
        p_media_kind,
        p_access_tier,
        p_published,
        (select auth.uid())
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_training_media(text,text,text,text,text,text,boolean) from public;
revoke execute on function public.rs_staff_create_training_media(text,text,text,text,text,text,boolean) from anon;
grant execute on function public.rs_staff_create_training_media(text,text,text,text,text,text,boolean) to authenticated;

create or replace function public.rs_staff_set_training_media_published(
    p_media_id uuid,
    p_published boolean
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    update public.rs_training_media
    set published=p_published,
        updated_at=now()
    where id=p_media_id;

    if not found then
        raise exception 'training media not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_training_media_published(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_training_media_published(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_training_media_published(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_training_media(p_media_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_path text;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_training_media
    where id=p_media_id
    returning media_path into v_path;

    if v_path is null then
        raise exception 'training media not found' using errcode='P0002';
    end if;

    return v_path;
end;
$$;

revoke execute on function public.rs_staff_delete_training_media(uuid) from public;
revoke execute on function public.rs_staff_delete_training_media(uuid) from anon;
grant execute on function public.rs_staff_delete_training_media(uuid) to authenticated;

comment on policy "rs_training_storage_visible_select" on storage.objects is
'Allows private training-media object reads only to staff or students whose active plan permits the linked published catalog item.';


-- ============================================================
-- supabase/migrations/0026_cloud_notifications.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0026: production cloud notification center.

create or replace function public.rs_notification_feed()
returns table (
    id uuid,
    title text,
    message text,
    audience text,
    created_at timestamptz,
    read boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        n.id,
        n.title,
        n.message,
        n.audience,
        n.created_at,
        exists(
            select 1
            from public.rs_notification_reads r
            where r.notification_id=n.id
              and r.user_id=(select auth.uid())
        ) as read
    from public.rs_notifications n
    left join public.rs_profiles p on p.id=(select auth.uid())
    where n.active=true
      and (
          (select private.rs_is_staff())
          or n.audience='ALL'
          or (
              p.active=true
              and p.role='student'
              and n.audience=p.plan
          )
      )
    order by n.created_at desc;
$$;

revoke execute on function public.rs_notification_feed() from public;
revoke execute on function public.rs_notification_feed() from anon;
grant execute on function public.rs_notification_feed() to authenticated;

create or replace function public.rs_unread_notification_count()
returns integer
language sql
stable
security definer
set search_path = ''
as $$
    select count(*)::integer
    from public.rs_notifications n
    join public.rs_profiles p on p.id=(select auth.uid())
    where n.active=true
      and (
          (select private.rs_is_staff())
          or n.audience='ALL'
          or (
              p.active=true
              and p.role='student'
              and n.audience=p.plan
          )
      )
      and not exists(
          select 1
          from public.rs_notification_reads r
          where r.notification_id=n.id
            and r.user_id=(select auth.uid())
      );
$$;

revoke execute on function public.rs_unread_notification_count() from public;
revoke execute on function public.rs_unread_notification_count() from anon;
grant execute on function public.rs_unread_notification_count() to authenticated;

create or replace function public.rs_mark_notification_read(p_notification_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if not exists(
        select 1
        from public.rs_notifications n
        left join public.rs_profiles p on p.id=(select auth.uid())
        where n.id=p_notification_id
          and n.active=true
          and (
              (select private.rs_is_staff())
              or n.audience='ALL'
              or (
                  p.active=true
                  and p.role='student'
                  and n.audience=p.plan
              )
          )
    ) then
        raise exception 'notification not available' using errcode='P0002';
    end if;

    insert into public.rs_notification_reads(notification_id,user_id,read_at)
    values(p_notification_id,(select auth.uid()),now())
    on conflict (notification_id,user_id) do update
    set read_at=excluded.read_at;
end;
$$;

revoke execute on function public.rs_mark_notification_read(uuid) from public;
revoke execute on function public.rs_mark_notification_read(uuid) from anon;
grant execute on function public.rs_mark_notification_read(uuid) to authenticated;

create or replace function public.rs_staff_create_notification(
    p_title text,
    p_message text,
    p_audience text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if trim(p_title)='' or trim(p_message)='' then
        raise exception 'title and message required' using errcode='22023';
    end if;

    if p_audience not in ('ALL','BASIC','PRO','ELITE') then
        raise exception 'invalid audience' using errcode='22023';
    end if;

    insert into public.rs_notifications(
        title,message,audience,created_by,active
    )
    values(
        trim(p_title),
        trim(p_message),
        p_audience,
        (select auth.uid()),
        true
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_notification(text,text,text) from public;
revoke execute on function public.rs_staff_create_notification(text,text,text) from anon;
grant execute on function public.rs_staff_create_notification(text,text,text) to authenticated;

create or replace function public.rs_staff_delete_notification(p_notification_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_notifications
    where id=p_notification_id;

    if not found then
        raise exception 'notification not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_notification(uuid) from public;
revoke execute on function public.rs_staff_delete_notification(uuid) from anon;
grant execute on function public.rs_staff_delete_notification(uuid) to authenticated;


-- ============================================================
-- supabase/migrations/0027_cloud_events.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0027: production cloud events and atomic RSVP actions.

create or replace function public.rs_event_catalog()
returns table (
    id uuid,
    title text,
    when_label text,
    location text,
    capacity integer,
    active boolean,
    going_count integer,
    my_status text
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        e.id,
        e.title,
        e.when_label,
        e.location,
        e.capacity,
        e.active,
        (
            select count(*)::integer
            from public.rs_event_rsvps r
            where r.event_id=e.id
              and r.status='going'
        ) as going_count,
        (
            select r2.status
            from public.rs_event_rsvps r2
            where r2.event_id=e.id
              and r2.student_id=(select auth.uid())
            limit 1
        ) as my_status
    from public.rs_events e
    where e.active=true
       or (select private.rs_is_staff())
    order by e.starts_at asc nulls last,e.created_at desc;
$$;

revoke execute on function public.rs_event_catalog() from public;
revoke execute on function public.rs_event_catalog() from anon;
grant execute on function public.rs_event_catalog() to authenticated;

create or replace function public.rs_event_rsvp(p_event_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_event public.rs_events%rowtype;
    v_count integer;
    v_role text;
    v_active boolean;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select p.role,p.active
    into v_role,v_active
    from public.rs_profiles p
    where p.id=v_uid;

    if v_role<>'student' or coalesce(v_active,false)=false then
        raise exception 'active student account required' using errcode='42501';
    end if;

    select *
    into v_event
    from public.rs_events e
    where e.id=p_event_id
    for update;

    if v_event.id is null then
        raise exception 'event not found' using errcode='P0002';
    end if;
    if not v_event.active then
        raise exception 'event inactive' using errcode='P0001';
    end if;

    select count(*)
    into v_count
    from public.rs_event_rsvps r
    where r.event_id=p_event_id
      and r.status='going';

    if v_count>=v_event.capacity then
        raise exception 'event full' using errcode='P0001';
    end if;

    insert into public.rs_event_rsvps(event_id,student_id,status,created_at,updated_at)
    values(p_event_id,v_uid,'going',now(),now())
    on conflict (event_id,student_id) do update
    set status='going',updated_at=now();

    return 'going';
end;
$$;

revoke execute on function public.rs_event_rsvp(uuid) from public;
revoke execute on function public.rs_event_rsvp(uuid) from anon;
grant execute on function public.rs_event_rsvp(uuid) to authenticated;

create or replace function public.rs_cancel_event_rsvp(p_event_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
begin
    update public.rs_event_rsvps
    set status='cancelled',
        updated_at=now()
    where event_id=p_event_id
      and student_id=(select auth.uid())
      and status='going';

    if not found then
        raise exception 'active RSVP not found' using errcode='P0002';
    end if;

    return 'cancelled';
end;
$$;

revoke execute on function public.rs_cancel_event_rsvp(uuid) from public;
revoke execute on function public.rs_cancel_event_rsvp(uuid) from anon;
grant execute on function public.rs_cancel_event_rsvp(uuid) to authenticated;

create or replace function public.rs_staff_create_event(
    p_title text,
    p_when_label text,
    p_location text,
    p_capacity integer
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if trim(p_title)='' or trim(p_when_label)='' or trim(p_location)='' then
        raise exception 'title, date/time and location required' using errcode='22023';
    end if;
    if p_capacity<1 or p_capacity>500 then
        raise exception 'invalid event capacity' using errcode='22023';
    end if;

    insert into public.rs_events(
        title,when_label,location,capacity,active,created_by
    )
    values(
        trim(p_title),trim(p_when_label),trim(p_location),p_capacity,true,(select auth.uid())
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_event(text,text,text,integer) from public;
revoke execute on function public.rs_staff_create_event(text,text,text,integer) from anon;
grant execute on function public.rs_staff_create_event(text,text,text,integer) to authenticated;

create or replace function public.rs_staff_set_event_active(
    p_event_id uuid,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    update public.rs_events
    set active=p_active,
        updated_at=now()
    where id=p_event_id;

    if not found then
        raise exception 'event not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_event_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_event_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_event_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_event(p_event_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_events where id=p_event_id;

    if not found then
        raise exception 'event not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_event(uuid) from public;
revoke execute on function public.rs_staff_delete_event(uuid) from anon;
grant execute on function public.rs_staff_delete_event(uuid) to authenticated;


-- ============================================================
-- supabase/migrations/0028_cloud_private_lessons.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0028: production cloud private lessons.

create or replace function public.rs_private_lesson_catalog()
returns table (
    slot_id uuid,
    day_label text,
    time_label text,
    duration_minutes integer,
    active boolean,
    my_booking_id uuid,
    my_status text,
    my_note text
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        s.id,
        s.day_label,
        s.time_label,
        s.duration_minutes,
        s.active,
        (
            select b.id
            from public.rs_private_bookings b
            where b.slot_id=s.id
              and b.student_id=(select auth.uid())
              and b.status<>'cancelled'
            order by b.requested_at desc
            limit 1
        ) as my_booking_id,
        (
            select b.status
            from public.rs_private_bookings b
            where b.slot_id=s.id
              and b.student_id=(select auth.uid())
              and b.status<>'cancelled'
            order by b.requested_at desc
            limit 1
        ) as my_status,
        (
            select b.note
            from public.rs_private_bookings b
            where b.slot_id=s.id
              and b.student_id=(select auth.uid())
              and b.status<>'cancelled'
            order by b.requested_at desc
            limit 1
        ) as my_note
    from public.rs_private_slots s
    where s.active=true
       or (select private.rs_is_staff())
    order by s.starts_at asc nulls last,s.created_at asc;
$$;

revoke execute on function public.rs_private_lesson_catalog() from public;
revoke execute on function public.rs_private_lesson_catalog() from anon;
grant execute on function public.rs_private_lesson_catalog() to authenticated;

create or replace function public.rs_request_private_lesson(
    p_slot_id uuid,
    p_note text default ''
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_role text;
    v_active boolean;
    v_id uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select p.role,p.active
    into v_role,v_active
    from public.rs_profiles p
    where p.id=v_uid;

    if v_role<>'student' or coalesce(v_active,false)=false then
        raise exception 'active student account required' using errcode='42501';
    end if;

    if not exists(
        select 1 from public.rs_private_slots s
        where s.id=p_slot_id and s.active=true
    ) then
        raise exception 'private lesson slot not available' using errcode='P0002';
    end if;

    if exists(
        select 1
        from public.rs_private_bookings b
        where b.slot_id=p_slot_id
          and b.status='confirmed'
          and b.student_id<>v_uid
    ) then
        raise exception 'private lesson slot already confirmed' using errcode='P0001';
    end if;

    insert into public.rs_private_bookings(
        slot_id,student_id,note,status,requested_at,updated_at
    )
    values(
        p_slot_id,v_uid,left(coalesce(p_note,''),1000),'requested',now(),now()
    )
    on conflict (slot_id,student_id) do update
    set
        note=excluded.note,
        status='requested',
        requested_at=now(),
        updated_at=now()
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_request_private_lesson(uuid,text) from public;
revoke execute on function public.rs_request_private_lesson(uuid,text) from anon;
grant execute on function public.rs_request_private_lesson(uuid,text) to authenticated;

create or replace function public.rs_cancel_private_lesson_request(p_booking_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    update public.rs_private_bookings
    set status='cancelled',
        updated_at=now()
    where id=p_booking_id
      and student_id=(select auth.uid())
      and status in ('requested','confirmed','declined');

    if not found then
        raise exception 'private lesson request not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_cancel_private_lesson_request(uuid) from public;
revoke execute on function public.rs_cancel_private_lesson_request(uuid) from anon;
grant execute on function public.rs_cancel_private_lesson_request(uuid) to authenticated;

create or replace function public.rs_staff_private_lesson_requests()
returns table (
    booking_id uuid,
    slot_id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    note text,
    status text,
    day_label text,
    time_label text,
    duration_minutes integer
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        b.id,
        b.slot_id,
        b.student_id,
        p.email,
        p.display_name,
        b.note,
        b.status,
        s.day_label,
        s.time_label,
        s.duration_minutes
    from public.rs_private_bookings b
    join public.rs_profiles p on p.id=b.student_id
    join public.rs_private_slots s on s.id=b.slot_id
    where (select private.rs_is_staff())
      and b.status<>'cancelled'
    order by b.requested_at desc;
$$;

revoke execute on function public.rs_staff_private_lesson_requests() from public;
revoke execute on function public.rs_staff_private_lesson_requests() from anon;
grant execute on function public.rs_staff_private_lesson_requests() to authenticated;

create or replace function public.rs_staff_create_private_slot(
    p_day_label text,
    p_time_label text,
    p_duration_minutes integer
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if trim(p_day_label)='' or trim(p_time_label)='' then
        raise exception 'day and time required' using errcode='22023';
    end if;

    if p_duration_minutes<15 or p_duration_minutes>180 then
        raise exception 'invalid duration' using errcode='22023';
    end if;

    insert into public.rs_private_slots(
        trainer_id,day_label,time_label,duration_minutes,active
    )
    values(
        (select auth.uid()),
        trim(p_day_label),
        trim(p_time_label),
        p_duration_minutes,
        true
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_private_slot(text,text,integer) from public;
revoke execute on function public.rs_staff_create_private_slot(text,text,integer) from anon;
grant execute on function public.rs_staff_create_private_slot(text,text,integer) to authenticated;

create or replace function public.rs_staff_set_private_slot_active(
    p_slot_id uuid,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    update public.rs_private_slots
    set active=p_active,
        updated_at=now()
    where id=p_slot_id;

    if not found then
        raise exception 'private lesson slot not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_private_slot_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_private_slot_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_private_slot_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_private_slot(p_slot_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_private_slots where id=p_slot_id;

    if not found then
        raise exception 'private lesson slot not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_private_slot(uuid) from public;
revoke execute on function public.rs_staff_delete_private_slot(uuid) from anon;
grant execute on function public.rs_staff_delete_private_slot(uuid) to authenticated;

create or replace function public.rs_staff_set_private_booking_status(
    p_booking_id uuid,
    p_status text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_slot_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_status not in ('requested','confirmed','declined') then
        raise exception 'invalid booking status' using errcode='22023';
    end if;

    select b.slot_id into v_slot_id
    from public.rs_private_bookings b
    where b.id=p_booking_id
    for update;

    if v_slot_id is null then
        raise exception 'private lesson request not found' using errcode='P0002';
    end if;

    if p_status='confirmed' then
        update public.rs_private_bookings
        set status='declined',
            updated_at=now()
        where slot_id=v_slot_id
          and id<>p_booking_id
          and status in ('requested','confirmed');
    end if;

    update public.rs_private_bookings
    set status=p_status,
        updated_at=now()
    where id=p_booking_id;
end;
$$;

revoke execute on function public.rs_staff_set_private_booking_status(uuid,text) from public;
revoke execute on function public.rs_staff_set_private_booking_status(uuid,text) from anon;
grant execute on function public.rs_staff_set_private_booking_status(uuid,text) to authenticated;


-- ============================================================
-- supabase/migrations/0029_cloud_finance.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0029: production billing summary and invoice ledger actions.

create or replace function public.rs_my_billing_summary()
returns table (
    plan text,
    active boolean,
    membership_status text,
    amount_cents integer,
    currency text,
    current_period_end timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.plan,
        p.active,
        coalesce(m.status,'active'),
        coalesce(m.amount_cents,mp.monthly_cents,0),
        coalesce(m.currency,mp.currency,'EUR'),
        m.current_period_end
    from public.rs_profiles p
    left join public.rs_memberships m on m.student_id=p.id
    left join public.rs_membership_plans mp on mp.code=p.plan
    where p.id=(select auth.uid())
    limit 1;
$$;

revoke execute on function public.rs_my_billing_summary() from public;
revoke execute on function public.rs_my_billing_summary() from anon;
grant execute on function public.rs_my_billing_summary() to authenticated;

create or replace function public.rs_my_invoices()
returns table (
    id uuid,
    invoice_number text,
    period_label text,
    amount_cents integer,
    currency text,
    status text,
    due_at timestamptz,
    paid_at timestamptz,
    created_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        i.id,
        i.invoice_number,
        i.period_label,
        i.amount_cents,
        i.currency,
        i.status,
        i.due_at,
        i.paid_at,
        i.created_at
    from public.rs_invoices i
    where i.student_id=(select auth.uid())
    order by i.created_at desc;
$$;

revoke execute on function public.rs_my_invoices() from public;
revoke execute on function public.rs_my_invoices() from anon;
grant execute on function public.rs_my_invoices() to authenticated;

create or replace function public.rs_staff_invoice_catalog()
returns table (
    id uuid,
    invoice_number text,
    student_id uuid,
    student_email text,
    student_name text,
    period_label text,
    amount_cents integer,
    currency text,
    status text,
    due_at timestamptz,
    paid_at timestamptz,
    created_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        i.id,
        i.invoice_number,
        i.student_id,
        p.email,
        p.display_name,
        i.period_label,
        i.amount_cents,
        i.currency,
        i.status,
        i.due_at,
        i.paid_at,
        i.created_at
    from public.rs_invoices i
    join public.rs_profiles p on p.id=i.student_id
    where (select private.rs_is_staff())
    order by i.created_at desc;
$$;

revoke execute on function public.rs_staff_invoice_catalog() from public;
revoke execute on function public.rs_staff_invoice_catalog() from anon;
grant execute on function public.rs_staff_invoice_catalog() to authenticated;

create or replace function public.rs_staff_create_invoice(
    p_student_email text,
    p_period_label text,
    p_amount_cents integer,
    p_due_at timestamptz default null
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_student_id uuid;
    v_invoice_id uuid;
    v_number text;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if trim(p_period_label)='' then
        raise exception 'period required' using errcode='22023';
    end if;
    if p_amount_cents<0 then
        raise exception 'invalid amount' using errcode='22023';
    end if;

    select p.id into v_student_id
    from public.rs_profiles p
    where lower(p.email)=lower(trim(p_student_email))
      and p.role='student'
    limit 1;

    if v_student_id is null then
        raise exception 'student not found' using errcode='P0002';
    end if;

    v_number := 'RS-' || to_char(now(),'YYYYMMDDHH24MISS') || '-' || upper(substr(replace(gen_random_uuid()::text,'-',''),1,6));

    insert into public.rs_invoices(
        invoice_number,
        student_id,
        period_label,
        amount_cents,
        currency,
        status,
        due_at
    )
    values(
        v_number,
        v_student_id,
        trim(p_period_label),
        p_amount_cents,
        'EUR',
        'pending',
        p_due_at
    )
    returning id into v_invoice_id;

    return v_invoice_id;
end;
$$;

revoke execute on function public.rs_staff_create_invoice(text,text,integer,timestamptz) from public;
revoke execute on function public.rs_staff_create_invoice(text,text,integer,timestamptz) from anon;
grant execute on function public.rs_staff_create_invoice(text,text,integer,timestamptz) to authenticated;

create or replace function public.rs_staff_set_invoice_status(
    p_invoice_id uuid,
    p_status text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_invoice public.rs_invoices%rowtype;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_status not in ('pending','paid','void','overdue') then
        raise exception 'invalid invoice status' using errcode='22023';
    end if;

    select * into v_invoice
    from public.rs_invoices
    where id=p_invoice_id
    for update;

    if v_invoice.id is null then
        raise exception 'invoice not found' using errcode='P0002';
    end if;

    update public.rs_invoices
    set
        status=p_status,
        paid_at=case when p_status='paid' then coalesce(paid_at,now()) else null end,
        updated_at=now()
    where id=p_invoice_id;

    if p_status='paid' and not exists(
        select 1 from public.rs_payments p
        where p.invoice_id=p_invoice_id
          and p.status='succeeded'
    ) then
        insert into public.rs_payments(
            invoice_id,
            student_id,
            amount_cents,
            currency,
            method,
            status,
            provider,
            paid_at
        )
        values(
            p_invoice_id,
            v_invoice.student_id,
            v_invoice.amount_cents,
            v_invoice.currency,
            'manual',
            'succeeded',
            'RS KICKBOX admin',
            now()
        );
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_invoice_status(uuid,text) from public;
revoke execute on function public.rs_staff_set_invoice_status(uuid,text) from anon;
grant execute on function public.rs_staff_set_invoice_status(uuid,text) to authenticated;

create or replace function public.rs_staff_delete_invoice(p_invoice_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_invoices where id=p_invoice_id;

    if not found then
        raise exception 'invoice not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_invoice(uuid) from public;
revoke execute on function public.rs_staff_delete_invoice(uuid) from anon;
grant execute on function public.rs_staff_delete_invoice(uuid) to authenticated;


-- ============================================================
-- supabase/migrations/0030_cloud_technique_coach.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0030: protected cloud Technique Coach submissions.

insert into storage.buckets(
    id,name,public,file_size_limit,allowed_mime_types
)
values(
    'rs-technique-submissions',
    'rs-technique-submissions',
    false,
    52428800,
    array['video/mp4','video/webm','video/quicktime','video/3gpp']
)
on conflict(id) do update
set public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "rs_technique_storage_insert_own" on storage.objects;
create policy "rs_technique_storage_insert_own"
on storage.objects
for insert
to authenticated
with check(
    bucket_id='rs-technique-submissions'
    and (storage.foldername(name))[1]=(select auth.uid())::text
);

drop policy if exists "rs_technique_storage_select_own_or_staff" on storage.objects;
create policy "rs_technique_storage_select_own_or_staff"
on storage.objects
for select
to authenticated
using(
    bucket_id='rs-technique-submissions'
    and (
        (storage.foldername(name))[1]=(select auth.uid())::text
        or (select private.rs_is_staff())
    )
);

drop policy if exists "rs_technique_storage_delete_own_or_staff" on storage.objects;
create policy "rs_technique_storage_delete_own_or_staff"
on storage.objects
for delete
to authenticated
using(
    bucket_id='rs-technique-submissions'
    and (
        (storage.foldername(name))[1]=(select auth.uid())::text
        or (select private.rs_is_staff())
    )
);

create or replace function public.rs_staff_set_technique_review(
    p_submission_id uuid,
    p_favorite boolean,
    p_trainer_note text
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    update public.rs_technique_submissions
    set trainer_favorite=p_favorite,
        trainer_note=coalesce(p_trainer_note,''),
        updated_at=now()
    where id=p_submission_id;

    if not found then
        raise exception 'technique submission not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_technique_review(uuid,boolean,text) from public;
revoke execute on function public.rs_staff_set_technique_review(uuid,boolean,text) from anon;
grant execute on function public.rs_staff_set_technique_review(uuid,boolean,text) to authenticated;

create or replace function public.rs_technique_submission_catalog()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    technique text,
    media_path text,
    media_name text,
    student_summary text,
    trainer_note text,
    trainer_favorite boolean,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        s.id,
        s.student_id,
        p.email,
        p.display_name,
        s.technique,
        s.media_path,
        s.media_name,
        s.student_summary,
        s.trainer_note,
        s.trainer_favorite,
        s.created_at
    from public.rs_technique_submissions s
    join public.rs_profiles p on p.id=s.student_id
    where s.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by s.created_at desc;
$$;

revoke execute on function public.rs_technique_submission_catalog() from public;
revoke execute on function public.rs_technique_submission_catalog() from anon;
grant execute on function public.rs_technique_submission_catalog() to authenticated;


-- ============================================================
-- supabase/migrations/0030_cloud_account_privacy.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0030: production account/privacy request workflow.

create or replace function public.rs_my_account_requests()
returns table (
    id uuid,
    request_type text,
    status text,
    user_note text,
    staff_note text,
    requested_at timestamptz,
    completed_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        r.id,
        r.request_type,
        r.status,
        r.user_note,
        r.staff_note,
        r.requested_at,
        r.completed_at
    from public.rs_account_requests r
    where r.user_id=(select auth.uid())
    order by r.requested_at desc;
$$;

revoke execute on function public.rs_my_account_requests() from public;
revoke execute on function public.rs_my_account_requests() from anon;
grant execute on function public.rs_my_account_requests() to authenticated;

create or replace function public.rs_request_account_action(
    p_request_type text,
    p_user_note text default ''
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_id uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if p_request_type not in ('data_export','account_deletion') then
        raise exception 'invalid request type' using errcode='22023';
    end if;

    select r.id into v_id
    from public.rs_account_requests r
    where r.user_id=v_uid
      and r.request_type=p_request_type
      and r.status in ('requested','processing')
    order by r.requested_at desc
    limit 1;

    if v_id is not null then
        return v_id;
    end if;

    insert into public.rs_account_requests(
        user_id,
        request_type,
        status,
        user_note
    )
    values(
        v_uid,
        p_request_type,
        'requested',
        left(coalesce(p_user_note,''),1000)
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_request_account_action(text,text) from public;
revoke execute on function public.rs_request_account_action(text,text) from anon;
grant execute on function public.rs_request_account_action(text,text) to authenticated;

create or replace function public.rs_staff_account_requests()
returns table (
    id uuid,
    user_id uuid,
    email text,
    display_name text,
    request_type text,
    status text,
    user_note text,
    staff_note text,
    requested_at timestamptz,
    completed_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        r.id,
        r.user_id,
        p.email,
        p.display_name,
        r.request_type,
        r.status,
        r.user_note,
        r.staff_note,
        r.requested_at,
        r.completed_at
    from public.rs_account_requests r
    left join public.rs_profiles p on p.id=r.user_id
    where (select private.rs_is_staff())
    order by
        case r.status when 'requested' then 1 when 'processing' then 2 else 3 end,
        r.requested_at desc;
$$;

revoke execute on function public.rs_staff_account_requests() from public;
revoke execute on function public.rs_staff_account_requests() from anon;
grant execute on function public.rs_staff_account_requests() to authenticated;

create or replace function public.rs_staff_update_account_request(
    p_request_id uuid,
    p_status text,
    p_staff_note text default ''
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_status not in ('requested','processing','completed','rejected','cancelled') then
        raise exception 'invalid request status' using errcode='22023';
    end if;

    update public.rs_account_requests
    set
        status=p_status,
        staff_note=left(coalesce(p_staff_note,''),2000),
        completed_at=case when p_status in ('completed','rejected','cancelled') then now() else null end
    where id=p_request_id;

    if not found then
        raise exception 'account request not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_update_account_request(uuid,text,text) from public;
revoke execute on function public.rs_staff_update_account_request(uuid,text,text) from anon;
grant execute on function public.rs_staff_update_account_request(uuid,text,text) to authenticated;


-- ============================================================
-- supabase/migrations/0031_cloud_content_library.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0031: production cloud content library actions.

create or replace function public.rs_content_catalog()
returns table(
    id uuid,
    title text,
    category text,
    body text,
    access_tier text,
    published boolean,
    favorite boolean,
    last_opened_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        c.id,
        c.title,
        c.category,
        c.body,
        c.access_tier,
        c.published,
        exists(
            select 1
            from public.rs_content_favorites f
            where f.user_id=(select auth.uid())
              and f.content_id=c.id
        ) as favorite,
        (
            select max(h.opened_at)
            from public.rs_content_history h
            where h.user_id=(select auth.uid())
              and h.content_id=c.id
        ) as last_opened_at
    from public.rs_content c
    where
        (select private.rs_is_staff())
        or (
            c.published=true
            and exists(
                select 1
                from public.rs_profiles p
                where p.id=(select auth.uid())
                  and p.active=true
                  and (
                      c.access_tier='ALL'
                      or c.access_tier='BASIC'
                      or (c.access_tier='PRO' and p.plan in ('PRO','ELITE'))
                      or (c.access_tier='ELITE' and p.plan='ELITE')
                  )
            )
        )
    order by c.updated_at desc,c.created_at desc;
$$;

revoke execute on function public.rs_content_catalog() from public;
revoke execute on function public.rs_content_catalog() from anon;
grant execute on function public.rs_content_catalog() to authenticated;

create or replace function public.rs_toggle_content_favorite(
    p_content_id uuid,
    p_favorite boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if p_favorite then
        insert into public.rs_content_favorites(user_id,content_id)
        values(v_uid,p_content_id)
        on conflict(user_id,content_id) do nothing;
    else
        delete from public.rs_content_favorites
        where user_id=v_uid and content_id=p_content_id;
    end if;
end;
$$;

revoke execute on function public.rs_toggle_content_favorite(uuid,boolean) from public;
revoke execute on function public.rs_toggle_content_favorite(uuid,boolean) from anon;
grant execute on function public.rs_toggle_content_favorite(uuid,boolean) to authenticated;

create or replace function public.rs_record_content_open(p_content_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    insert into public.rs_content_history(user_id,content_id,opened_at)
    values(v_uid,p_content_id,now());

    delete from public.rs_content_history h
    where h.user_id=v_uid
      and h.id not in(
          select x.id
          from public.rs_content_history x
          where x.user_id=v_uid
          order by x.opened_at desc
          limit 100
      );
end;
$$;

revoke execute on function public.rs_record_content_open(uuid) from public;
revoke execute on function public.rs_record_content_open(uuid) from anon;
grant execute on function public.rs_record_content_open(uuid) to authenticated;

create or replace function public.rs_staff_create_content(
    p_title text,
    p_category text,
    p_body text,
    p_access_tier text,
    p_published boolean
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if trim(p_title)='' or trim(p_body)='' then
        raise exception 'title and content required' using errcode='22023';
    end if;
    if p_access_tier not in ('ALL','BASIC','PRO','ELITE') then
        raise exception 'invalid access tier' using errcode='22023';
    end if;

    insert into public.rs_content(
        title,category,body,access_tier,published,created_by
    )
    values(
        trim(p_title),
        coalesce(nullif(trim(p_category),''),'TECHNIQUE'),
        p_body,
        p_access_tier,
        p_published,
        (select auth.uid())
    )
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_content(text,text,text,text,boolean) from public;
revoke execute on function public.rs_staff_create_content(text,text,text,text,boolean) from anon;
grant execute on function public.rs_staff_create_content(text,text,text,text,boolean) to authenticated;

create or replace function public.rs_staff_set_content_published(
    p_content_id uuid,
    p_published boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    update public.rs_content
    set published=p_published,updated_at=now()
    where id=p_content_id;

    if not found then
        raise exception 'content not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_content_published(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_content_published(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_content_published(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_content(p_content_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_content where id=p_content_id;
    if not found then
        raise exception 'content not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_content(uuid) from public;
revoke execute on function public.rs_staff_delete_content(uuid) from anon;
grant execute on function public.rs_staff_delete_content(uuid) to authenticated;


-- ============================================================
-- supabase/migrations/0032_cloud_student_development.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0032: cloud student development workflows.

create or replace function public.rs_development_students()
returns table(
    id uuid,
    email text,
    display_name text,
    plan text,
    active boolean
)
language sql
stable
security definer
set search_path=''
as $$
    select p.id,p.email,p.display_name,p.plan,p.active
    from public.rs_profiles p
    where p.role='student'
      and p.active=true
      and (select private.rs_is_staff())
    order by lower(p.display_name),lower(p.email);
$$;

revoke execute on function public.rs_development_students() from public;
revoke execute on function public.rs_development_students() from anon;
grant execute on function public.rs_development_students() to authenticated;

create or replace function public.rs_homework_feed()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    title text,
    details text,
    due_label text,
    completed boolean,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select h.id,h.student_id,p.email,p.display_name,h.title,h.details,h.due_label,h.completed,h.created_at
    from public.rs_homework h
    join public.rs_profiles p on p.id=h.student_id
    where h.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by h.created_at desc;
$$;

revoke execute on function public.rs_homework_feed() from public;
revoke execute on function public.rs_homework_feed() from anon;
grant execute on function public.rs_homework_feed() to authenticated;

create or replace function public.rs_staff_assign_homework(
    p_student_id uuid,
    p_title text,
    p_details text,
    p_due_label text
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if trim(p_title)='' or trim(p_details)='' then
        raise exception 'title and details required' using errcode='22023';
    end if;

    insert into public.rs_homework(student_id,title,details,due_label,assigned_by)
    values(p_student_id,trim(p_title),trim(p_details),coalesce(p_due_label,''),(select auth.uid()))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_assign_homework(uuid,text,text,text) from public;
revoke execute on function public.rs_staff_assign_homework(uuid,text,text,text) from anon;
grant execute on function public.rs_staff_assign_homework(uuid,text,text,text) to authenticated;

create or replace function public.rs_set_homework_completed(
    p_homework_id uuid,
    p_completed boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    update public.rs_homework h
    set completed=p_completed,updated_at=now()
    where h.id=p_homework_id
      and (
          h.student_id=(select auth.uid())
          or (select private.rs_is_staff())
      );
    if not found then
        raise exception 'homework not found or not permitted' using errcode='42501';
    end if;
end;
$$;

revoke execute on function public.rs_set_homework_completed(uuid,boolean) from public;
revoke execute on function public.rs_set_homework_completed(uuid,boolean) from anon;
grant execute on function public.rs_set_homework_completed(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_homework(p_homework_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_homework where id=p_homework_id;
end;
$$;

revoke execute on function public.rs_staff_delete_homework(uuid) from public;
revoke execute on function public.rs_staff_delete_homework(uuid) from anon;
grant execute on function public.rs_staff_delete_homework(uuid) to authenticated;

create or replace function public.rs_coach_notes_feed()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    note text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select n.id,n.student_id,p.email,p.display_name,n.note,n.created_at
    from public.rs_coach_notes n
    join public.rs_profiles p on p.id=n.student_id
    where (select private.rs_is_staff())
    order by n.created_at desc;
$$;

revoke execute on function public.rs_coach_notes_feed() from public;
revoke execute on function public.rs_coach_notes_feed() from anon;
grant execute on function public.rs_coach_notes_feed() to authenticated;

create or replace function public.rs_staff_add_coach_note(p_student_id uuid,p_note text)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    insert into public.rs_coach_notes(student_id,note,created_by)
    values(p_student_id,trim(p_note),(select auth.uid()))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_add_coach_note(uuid,text) from public;
revoke execute on function public.rs_staff_add_coach_note(uuid,text) from anon;
grant execute on function public.rs_staff_add_coach_note(uuid,text) to authenticated;

create or replace function public.rs_staff_delete_coach_note(p_note_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_coach_notes where id=p_note_id;
end;
$$;

revoke execute on function public.rs_staff_delete_coach_note(uuid) from public;
revoke execute on function public.rs_staff_delete_coach_note(uuid) from anon;
grant execute on function public.rs_staff_delete_coach_note(uuid) to authenticated;

create or replace function public.rs_assessment_feed()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    punches integer,
    kicks integer,
    defense integer,
    footwork integer,
    combinations integer,
    conditioning integer,
    summary text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select a.id,a.student_id,p.email,p.display_name,a.punches,a.kicks,a.defense,a.footwork,a.combinations,a.conditioning,a.summary,a.created_at
    from public.rs_assessments a
    join public.rs_profiles p on p.id=a.student_id
    where a.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by a.created_at desc;
$$;

revoke execute on function public.rs_assessment_feed() from public;
revoke execute on function public.rs_assessment_feed() from anon;
grant execute on function public.rs_assessment_feed() to authenticated;

create or replace function public.rs_staff_add_assessment(
    p_student_id uuid,
    p_punches integer,
    p_kicks integer,
    p_defense integer,
    p_footwork integer,
    p_combinations integer,
    p_conditioning integer,
    p_summary text
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;

    insert into public.rs_assessments(
        student_id,punches,kicks,defense,footwork,combinations,conditioning,summary,assessed_by
    )
    values(
        p_student_id,
        greatest(0,least(100,p_punches)),
        greatest(0,least(100,p_kicks)),
        greatest(0,least(100,p_defense)),
        greatest(0,least(100,p_footwork)),
        greatest(0,least(100,p_combinations)),
        greatest(0,least(100,p_conditioning)),
        coalesce(p_summary,''),
        (select auth.uid())
    )
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_add_assessment(uuid,integer,integer,integer,integer,integer,integer,text) from public;
revoke execute on function public.rs_staff_add_assessment(uuid,integer,integer,integer,integer,integer,integer,text) from anon;
grant execute on function public.rs_staff_add_assessment(uuid,integer,integer,integer,integer,integer,integer,text) to authenticated;


-- ============================================================
-- supabase/migrations/0033_cloud_student_feature_controls.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0033: cloud operational controls and student route locks.

alter table public.rs_app_settings
add column if not exists disabled_student_routes text[] not null default '{}';

create or replace function public.rs_app_controls()
returns table(
    maintenance_enabled boolean,
    maintenance_message text,
    community_posts_enabled boolean,
    class_booking_enabled boolean,
    private_lessons_enabled boolean,
    referrals_enabled boolean,
    in_app_reminders_enabled boolean,
    retention_months integer,
    disabled_student_routes text[]
)
language sql
stable
security definer
set search_path=''
as $$
    select
        s.maintenance_enabled,
        s.maintenance_message,
        s.community_posts_enabled,
        s.class_booking_enabled,
        s.private_lessons_enabled,
        s.referrals_enabled,
        s.in_app_reminders_enabled,
        s.retention_months,
        s.disabled_student_routes
    from public.rs_app_settings s
    where s.singleton=true;
$$;

revoke execute on function public.rs_app_controls() from public;
revoke execute on function public.rs_app_controls() from anon;
grant execute on function public.rs_app_controls() to authenticated;

create or replace function public.rs_staff_update_app_controls(
    p_maintenance_enabled boolean,
    p_maintenance_message text,
    p_community_posts_enabled boolean,
    p_class_booking_enabled boolean,
    p_private_lessons_enabled boolean,
    p_referrals_enabled boolean,
    p_in_app_reminders_enabled boolean,
    p_retention_months integer,
    p_disabled_student_routes text[]
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_retention_months not in (12,24,36) then
        raise exception 'invalid retention period' using errcode='22023';
    end if;

    update public.rs_app_settings
    set
        maintenance_enabled=p_maintenance_enabled,
        maintenance_message=left(coalesce(p_maintenance_message,''),240),
        community_posts_enabled=p_community_posts_enabled,
        class_booking_enabled=p_class_booking_enabled,
        private_lessons_enabled=p_private_lessons_enabled,
        referrals_enabled=p_referrals_enabled,
        in_app_reminders_enabled=p_in_app_reminders_enabled,
        retention_months=p_retention_months,
        disabled_student_routes=coalesce(p_disabled_student_routes,'{}'::text[]),
        updated_by=(select auth.uid()),
        updated_at=now()
    where singleton=true;
end;
$$;

revoke execute on function public.rs_staff_update_app_controls(boolean,text,boolean,boolean,boolean,boolean,boolean,integer,text[]) from public;
revoke execute on function public.rs_staff_update_app_controls(boolean,text,boolean,boolean,boolean,boolean,boolean,integer,text[]) from anon;
grant execute on function public.rs_staff_update_app_controls(boolean,text,boolean,boolean,boolean,boolean,boolean,integer,text[]) to authenticated;


-- ============================================================
-- supabase/migrations/0034_cloud_performance.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0034: cloud challenges, fight camp and performance state.

create or replace function public.rs_challenge_feed()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    title text,
    target integer,
    current integer,
    unit text,
    active boolean
)
language sql
stable
security definer
set search_path=''
as $$
    select c.id,c.student_id,p.email,p.display_name,c.title,c.target,c.current,c.unit,c.active
    from public.rs_challenges c
    join public.rs_profiles p on p.id=c.student_id
    where c.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by c.created_at desc;
$$;

revoke execute on function public.rs_challenge_feed() from public;
revoke execute on function public.rs_challenge_feed() from anon;
grant execute on function public.rs_challenge_feed() to authenticated;

create or replace function public.rs_staff_assign_challenge(
    p_student_id uuid,
    p_title text,
    p_target integer,
    p_unit text
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if trim(p_title)='' then
        raise exception 'challenge title required' using errcode='22023';
    end if;

    insert into public.rs_challenges(
        student_id,title,target,current,unit,active,assigned_by
    )
    values(
        p_student_id,
        trim(p_title),
        greatest(1,least(10000,p_target)),
        0,
        coalesce(nullif(trim(p_unit),''),'sessions'),
        true,
        (select auth.uid())
    )
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_assign_challenge(uuid,text,integer,text) from public;
revoke execute on function public.rs_staff_assign_challenge(uuid,text,integer,text) from anon;
grant execute on function public.rs_staff_assign_challenge(uuid,text,integer,text) to authenticated;

create or replace function public.rs_update_my_challenge_progress(
    p_challenge_id uuid,
    p_delta integer
)
returns integer
language plpgsql
security definer
set search_path=''
as $$
declare
    v_target integer;
    v_current integer;
begin
    select c.target,c.current
    into v_target,v_current
    from public.rs_challenges c
    where c.id=p_challenge_id
      and c.student_id=(select auth.uid())
      and c.active=true
    for update;

    if v_target is null then
        raise exception 'challenge not found' using errcode='P0002';
    end if;

    v_current:=greatest(0,least(v_target,v_current+p_delta));

    update public.rs_challenges
    set current=v_current,updated_at=now()
    where id=p_challenge_id;

    return v_current;
end;
$$;

revoke execute on function public.rs_update_my_challenge_progress(uuid,integer) from public;
revoke execute on function public.rs_update_my_challenge_progress(uuid,integer) from anon;
grant execute on function public.rs_update_my_challenge_progress(uuid,integer) to authenticated;

create or replace function public.rs_staff_set_challenge_active(
    p_challenge_id uuid,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    update public.rs_challenges
    set active=p_active,updated_at=now()
    where id=p_challenge_id;

    if not found then
        raise exception 'challenge not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_challenge_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_challenge_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_challenge_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_challenge(p_challenge_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    delete from public.rs_challenges where id=p_challenge_id;
end;
$$;

revoke execute on function public.rs_staff_delete_challenge(uuid) from public;
revoke execute on function public.rs_staff_delete_challenge(uuid) from anon;
grant execute on function public.rs_staff_delete_challenge(uuid) to authenticated;

create or replace function public.rs_fight_camp_feed()
returns table(
    student_id uuid,
    student_email text,
    student_name text,
    current_week integer,
    total_weeks integer,
    focus text,
    active boolean
)
language sql
stable
security definer
set search_path=''
as $$
    select f.student_id,p.email,p.display_name,f.current_week,f.total_weeks,f.focus,f.active
    from public.rs_fight_camps f
    join public.rs_profiles p on p.id=f.student_id
    where f.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by lower(p.display_name);
$$;

revoke execute on function public.rs_fight_camp_feed() from public;
revoke execute on function public.rs_fight_camp_feed() from anon;
grant execute on function public.rs_fight_camp_feed() to authenticated;

create or replace function public.rs_staff_set_fight_camp(
    p_student_id uuid,
    p_current_week integer,
    p_focus text,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    insert into public.rs_fight_camps(
        student_id,current_week,total_weeks,focus,active,managed_by
    )
    values(
        p_student_id,
        greatest(1,least(8,p_current_week)),
        8,
        left(coalesce(p_focus,''),500),
        p_active,
        (select auth.uid())
    )
    on conflict(student_id) do update
    set current_week=excluded.current_week,
        total_weeks=8,
        focus=excluded.focus,
        active=excluded.active,
        managed_by=(select auth.uid()),
        updated_at=now();
end;
$$;

revoke execute on function public.rs_staff_set_fight_camp(uuid,integer,text,boolean) from public;
revoke execute on function public.rs_staff_set_fight_camp(uuid,integer,text,boolean) from anon;
grant execute on function public.rs_staff_set_fight_camp(uuid,integer,text,boolean) to authenticated;


-- ============================================================
-- supabase/migrations/0035_cloud_social_community.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0035: production social profiles, community and groups.

create or replace function public.rs_my_social_profile()
returns table(
    user_id uuid,
    email text,
    display_name text,
    bio text,
    training_goal text,
    public_profile boolean
)
language sql
stable
security definer
set search_path=''
as $$
    select
        p.id,
        p.email,
        coalesce(s.display_name,p.display_name),
        coalesce(s.bio,''),
        coalesce(s.training_goal,''),
        coalesce(s.public_profile,false)
    from public.rs_profiles p
    left join public.rs_social_profiles s on s.user_id=p.id
    where p.id=(select auth.uid())
    limit 1;
$$;

revoke execute on function public.rs_my_social_profile() from public;
revoke execute on function public.rs_my_social_profile() from anon;
grant execute on function public.rs_my_social_profile() to authenticated;

create or replace function public.rs_save_my_social_profile(
    p_display_name text,
    p_bio text,
    p_training_goal text,
    p_public_profile boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare v_uid uuid := (select auth.uid());
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;
    if trim(p_display_name)='' then
        raise exception 'display name required' using errcode='22023';
    end if;

    insert into public.rs_social_profiles(
        user_id,display_name,bio,training_goal,public_profile,updated_at
    )
    values(
        v_uid,
        left(trim(p_display_name),80),
        left(coalesce(p_bio,''),500),
        left(coalesce(p_training_goal,''),300),
        p_public_profile,
        now()
    )
    on conflict(user_id) do update
    set display_name=excluded.display_name,
        bio=excluded.bio,
        training_goal=excluded.training_goal,
        public_profile=excluded.public_profile,
        updated_at=now();

    update public.rs_profiles
    set display_name=left(trim(p_display_name),80),updated_at=now()
    where id=v_uid;
end;
$$;

revoke execute on function public.rs_save_my_social_profile(text,text,text,boolean) from public;
revoke execute on function public.rs_save_my_social_profile(text,text,text,boolean) from anon;
grant execute on function public.rs_save_my_social_profile(text,text,text,boolean) to authenticated;

create or replace function public.rs_community_feed()
returns table(
    id uuid,
    author_id uuid,
    author_email text,
    author_name text,
    body text,
    active boolean,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        c.id,c.author_id,p.email,
        coalesce(s.display_name,p.display_name),
        c.body,c.active,c.created_at
    from public.rs_community_posts c
    join public.rs_profiles p on p.id=c.author_id
    left join public.rs_social_profiles s on s.user_id=c.author_id
    where c.active=true
       or c.author_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by c.created_at desc;
$$;

revoke execute on function public.rs_community_feed() from public;
revoke execute on function public.rs_community_feed() from anon;
grant execute on function public.rs_community_feed() to authenticated;

create or replace function public.rs_create_community_post(p_body text)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_uid uuid := (select auth.uid()); v_id uuid; v_enabled boolean;
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if length(trim(p_body))<1 or length(trim(p_body))>1000 then
        raise exception 'post must contain 1 to 1000 characters' using errcode='22023';
    end if;

    select s.community_posts_enabled into v_enabled
    from public.rs_app_settings s where s.singleton=true;
    if coalesce(v_enabled,true)=false then
        raise exception 'community posting disabled' using errcode='42501';
    end if;

    insert into public.rs_community_posts(author_id,body,active)
    values(v_uid,trim(p_body),true)
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_create_community_post(text) from public;
revoke execute on function public.rs_create_community_post(text) from anon;
grant execute on function public.rs_create_community_post(text) to authenticated;

create or replace function public.rs_delete_community_post(p_post_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    delete from public.rs_community_posts c
    where c.id=p_post_id
      and (c.author_id=(select auth.uid()) or (select private.rs_is_staff()));
    if not found then raise exception 'post not found or not permitted' using errcode='42501'; end if;
end;
$$;

revoke execute on function public.rs_delete_community_post(uuid) from public;
revoke execute on function public.rs_delete_community_post(uuid) from anon;
grant execute on function public.rs_delete_community_post(uuid) to authenticated;

create or replace function public.rs_staff_set_community_post_active(p_post_id uuid,p_active boolean)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    update public.rs_community_posts set active=p_active where id=p_post_id;
end;
$$;

revoke execute on function public.rs_staff_set_community_post_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_community_post_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_community_post_active(uuid,boolean) to authenticated;

create or replace function public.rs_group_catalog()
returns table(
    id uuid,
    name text,
    description text,
    active boolean,
    joined boolean,
    member_count integer
)
language sql
stable
security definer
set search_path=''
as $$
    select
        g.id,g.name,g.description,g.active,
        exists(
            select 1 from public.rs_group_memberships m
            where m.group_id=g.id and m.student_id=(select auth.uid())
        ),
        (
            select count(*)::integer
            from public.rs_group_memberships m2
            where m2.group_id=g.id
        )
    from public.rs_groups g
    where g.active=true or (select private.rs_is_staff())
    order by g.created_at desc;
$$;

revoke execute on function public.rs_group_catalog() from public;
revoke execute on function public.rs_group_catalog() from anon;
grant execute on function public.rs_group_catalog() to authenticated;

create or replace function public.rs_set_my_group_membership(p_group_id uuid,p_join boolean)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare v_uid uuid := (select auth.uid());
begin
    if v_uid is null then raise exception 'authentication required' using errcode='42501'; end if;
    if p_join then
        if not exists(select 1 from public.rs_groups g where g.id=p_group_id and g.active=true) then
            raise exception 'group unavailable' using errcode='P0002';
        end if;
        insert into public.rs_group_memberships(group_id,student_id)
        values(p_group_id,v_uid)
        on conflict(group_id,student_id) do nothing;
    else
        delete from public.rs_group_memberships where group_id=p_group_id and student_id=v_uid;
    end if;
end;
$$;

revoke execute on function public.rs_set_my_group_membership(uuid,boolean) from public;
revoke execute on function public.rs_set_my_group_membership(uuid,boolean) from anon;
grant execute on function public.rs_set_my_group_membership(uuid,boolean) to authenticated;

create or replace function public.rs_staff_create_group(p_name text,p_description text)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare v_id uuid;
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    insert into public.rs_groups(name,description,active,created_by)
    values(left(trim(p_name),80),left(coalesce(p_description,''),500),true,(select auth.uid()))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_group(text,text) from public;
revoke execute on function public.rs_staff_create_group(text,text) from anon;
grant execute on function public.rs_staff_create_group(text,text) to authenticated;

create or replace function public.rs_staff_set_group_active(p_group_id uuid,p_active boolean)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    update public.rs_groups set active=p_active where id=p_group_id;
end;
$$;

revoke execute on function public.rs_staff_set_group_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_group_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_group_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_group(p_group_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    delete from public.rs_groups where id=p_group_id;
end;
$$;

revoke execute on function public.rs_staff_delete_group(uuid) from public;
revoke execute on function public.rs_staff_delete_group(uuid) from anon;
grant execute on function public.rs_staff_delete_group(uuid) to authenticated;


-- ============================================================
-- supabase/migrations/0036_cloud_sessions_qr_attendance.sql
-- ============================================================

-- RS KICKBOX backend foundation
-- Migration 0036: cloud training sessions and secure QR attendance.

create or replace function public.rs_training_session_catalog()
returns table(
    id uuid,
    title text,
    active boolean,
    blocks jsonb
)
language sql
stable
security definer
set search_path=''
as $$
    select
        s.id,
        s.title,
        s.active,
        coalesce(
            (
                select jsonb_agg(
                    jsonb_build_object(
                        'id',b.id,
                        'title',b.title,
                        'seconds',b.duration_seconds,
                        'instructions',b.instructions
                    )
                    order by b.sort_order
                )
                from public.rs_training_session_blocks b
                where b.session_id=s.id
            ),
            '[]'::jsonb
        ) as blocks
    from public.rs_training_sessions s
    where s.active=true
       or (select private.rs_is_staff())
    order by s.active desc,s.updated_at desc;
$$;

revoke execute on function public.rs_training_session_catalog() from public;
revoke execute on function public.rs_training_session_catalog() from anon;
grant execute on function public.rs_training_session_catalog() to authenticated;

create or replace function public.rs_staff_create_training_session(
    p_title text,
    p_blocks jsonb
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare
    v_id uuid;
    v_block jsonb;
    v_order integer:=0;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if trim(p_title)='' then
        raise exception 'session title required' using errcode='22023';
    end if;
    if jsonb_typeof(p_blocks)<>'array' or jsonb_array_length(p_blocks)<1 then
        raise exception 'at least one session block required' using errcode='22023';
    end if;

    update public.rs_training_sessions
    set active=false,updated_at=now()
    where active=true;

    insert into public.rs_training_sessions(title,active,created_by)
    values(left(trim(p_title),100),true,(select auth.uid()))
    returning id into v_id;

    for v_block in select value from jsonb_array_elements(p_blocks)
    loop
        insert into public.rs_training_session_blocks(
            session_id,sort_order,title,duration_seconds,instructions
        )
        values(
            v_id,
            v_order,
            left(coalesce(v_block->>'title','Block'),80),
            greatest(10,least(3600,coalesce((v_block->>'seconds')::integer,180))),
            left(coalesce(v_block->>'instructions',''),1000)
        );
        v_order:=v_order+1;
    end loop;

    return v_id;
end;
$$;

revoke execute on function public.rs_staff_create_training_session(text,jsonb) from public;
revoke execute on function public.rs_staff_create_training_session(text,jsonb) from anon;
grant execute on function public.rs_staff_create_training_session(text,jsonb) to authenticated;

create or replace function public.rs_staff_set_training_session_active(
    p_session_id uuid,
    p_active boolean
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_active then
        update public.rs_training_sessions
        set active=false,updated_at=now()
        where active=true and id<>p_session_id;
    end if;

    update public.rs_training_sessions
    set active=p_active,updated_at=now()
    where id=p_session_id;

    if not found then
        raise exception 'training session not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_training_session_active(uuid,boolean) from public;
revoke execute on function public.rs_staff_set_training_session_active(uuid,boolean) from anon;
grant execute on function public.rs_staff_set_training_session_active(uuid,boolean) to authenticated;

create or replace function public.rs_staff_delete_training_session(p_session_id uuid)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    delete from public.rs_training_sessions where id=p_session_id;
    if not found then
        raise exception 'training session not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_training_session(uuid) from public;
revoke execute on function public.rs_staff_delete_training_session(uuid) from anon;
grant execute on function public.rs_staff_delete_training_session(uuid) to authenticated;

create or replace function public.rs_staff_create_attendance_qr(
    p_class_id uuid,
    p_valid_minutes integer default 15
)
returns table(
    class_id uuid,
    token text,
    expires_at timestamptz
)
language plpgsql
security definer
set search_path=''
as $$
declare
    v_token text;
    v_hash text;
    v_expiry timestamptz;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;
    if not exists(select 1 from public.rs_classes c where c.id=p_class_id and c.active=true) then
        raise exception 'active class not found' using errcode='P0002';
    end if;

    update private.rs_attendance_tokens
    set revoked_at=now()
    where class_id=p_class_id
      and revoked_at is null
      and expires_at>now();

    v_token:=encode(extensions.gen_random_bytes(32),'hex');
    v_hash:=encode(extensions.digest(v_token,'sha256'),'hex');
    v_expiry:=now()+make_interval(mins=>greatest(2,least(60,p_valid_minutes)));

    insert into private.rs_attendance_tokens(
        class_id,token_hash,created_by,expires_at
    )
    values(
        p_class_id,v_hash,(select auth.uid()),v_expiry
    );

    return query select p_class_id,v_token,v_expiry;
end;
$$;

revoke execute on function public.rs_staff_create_attendance_qr(uuid,integer) from public;
revoke execute on function public.rs_staff_create_attendance_qr(uuid,integer) from anon;
grant execute on function public.rs_staff_create_attendance_qr(uuid,integer) to authenticated;

create or replace function public.rs_student_attendance_checkin(
    p_class_id uuid,
    p_token text
)
returns text
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_hash text;
    v_token_id uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;
    if not exists(
        select 1 from public.rs_profiles p
        where p.id=v_uid and p.role='student' and p.active=true
    ) then
        raise exception 'active student account required' using errcode='42501';
    end if;

    v_hash:=encode(extensions.digest(p_token,'sha256'),'hex');

    select t.id into v_token_id
    from private.rs_attendance_tokens t
    where t.class_id=p_class_id
      and t.token_hash=v_hash
      and t.revoked_at is null
      and t.expires_at>now()
    limit 1;

    if v_token_id is null then
        raise exception 'invalid or expired attendance QR' using errcode='P0001';
    end if;

    insert into public.rs_attendance(
        class_id,student_id,present,checked_in_at,checked_in_by,note
    )
    values(
        p_class_id,v_uid,true,now(),v_uid,'Student QR check-in'
    )
    on conflict(class_id,student_id) do update
    set present=true,
        checked_in_at=now(),
        checked_in_by=v_uid,
        note='Student QR check-in';

    return 'checked_in';
end;
$$;

revoke execute on function public.rs_student_attendance_checkin(uuid,text) from public;
revoke execute on function public.rs_student_attendance_checkin(uuid,text) from anon;
grant execute on function public.rs_student_attendance_checkin(uuid,text) to authenticated;
