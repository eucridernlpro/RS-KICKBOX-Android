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
