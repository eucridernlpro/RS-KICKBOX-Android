-- RS KICKBOX backend foundation
-- Migration 0040: global club branding and presentation settings.

create table if not exists public.rs_brand_settings (
    singleton boolean primary key default true check (singleton=true),
    header_name text not null default 'RS KICKBOX',
    login_title text not null default 'Premium cinematic kickboxing',
    login_subtitle text not null default 'TRAIN · LEARN · CONNECT · GROW',
    footer_text text not null default 'RS KICKBOX · TRAIN · LEARN · CONNECT · GROW',
    theme_name text not null default 'ELITE_GOLD'
        check (theme_name in ('ELITE_GOLD','CRIMSON_FIGHT_NIGHT','PLATINUM_PRO','EMERALD_PERFORMANCE')),
    login_form_opacity numeric(4,3) not null default 0.820
        check (login_form_opacity between 0.20 and 1.00),
    updated_by uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);

insert into public.rs_brand_settings(singleton)
values(true)
on conflict(singleton) do nothing;

alter table public.rs_brand_settings enable row level security;
revoke all on table public.rs_brand_settings from anon, authenticated;
grant select on table public.rs_brand_settings to anon, authenticated;
grant update on table public.rs_brand_settings to authenticated;

drop policy if exists "rs_brand_settings_public_read" on public.rs_brand_settings;
create policy "rs_brand_settings_public_read"
on public.rs_brand_settings
for select
to anon, authenticated
using(true);

drop policy if exists "rs_brand_settings_staff_update" on public.rs_brand_settings;
create policy "rs_brand_settings_staff_update"
on public.rs_brand_settings
for update
to authenticated
using((select private.rs_is_staff()))
with check((select private.rs_is_staff()));

create or replace function public.rs_staff_save_brand_settings(
    p_header_name text,
    p_login_title text,
    p_login_subtitle text,
    p_footer_text text,
    p_theme_name text,
    p_login_form_opacity numeric
)
returns void
language plpgsql
security definer
set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_theme_name not in ('ELITE_GOLD','CRIMSON_FIGHT_NIGHT','PLATINUM_PRO','EMERALD_PERFORMANCE') then
        raise exception 'invalid theme' using errcode='22023';
    end if;

    update public.rs_brand_settings
    set header_name=left(coalesce(nullif(trim(p_header_name),''),'RS KICKBOX'),80),
        login_title=left(coalesce(p_login_title,''),140),
        login_subtitle=left(coalesce(p_login_subtitle,''),180),
        footer_text=left(coalesce(p_footer_text,''),180),
        theme_name=p_theme_name,
        login_form_opacity=greatest(0.20,least(1.00,p_login_form_opacity)),
        updated_by=(select auth.uid()),
        updated_at=now()
    where singleton=true;
end;
$$;

revoke execute on function public.rs_staff_save_brand_settings(text,text,text,text,text,numeric) from public;
revoke execute on function public.rs_staff_save_brand_settings(text,text,text,text,text,numeric) from anon;
grant execute on function public.rs_staff_save_brand_settings(text,text,text,text,text,numeric) to authenticated;

comment on table public.rs_brand_settings is
'Publicly readable club identity/presentation settings. Only active trainer/admin accounts may update.';
