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
