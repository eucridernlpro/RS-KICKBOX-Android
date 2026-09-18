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
