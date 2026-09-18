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
