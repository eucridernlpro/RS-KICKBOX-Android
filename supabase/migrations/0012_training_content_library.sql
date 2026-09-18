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
