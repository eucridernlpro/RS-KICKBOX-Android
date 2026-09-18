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
