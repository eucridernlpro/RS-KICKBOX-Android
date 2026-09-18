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
