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
