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
