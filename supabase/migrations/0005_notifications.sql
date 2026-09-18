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
