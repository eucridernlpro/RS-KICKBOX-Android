-- RS KICKBOX backend foundation
-- Migration 0013: membership plan definitions.

create table if not exists public.rs_membership_plans (
    code text primary key check (code in ('BASIC','PRO','ELITE')),
    name text not null,
    monthly_cents integer not null check (monthly_cents >= 0),
    currency text not null default 'EUR',
    description text not null default '',
    active boolean not null default true,
    updated_by uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);

insert into public.rs_membership_plans(code,name,monthly_cents,currency,description,active)
values
    ('BASIC','RS BASIC',2900,'EUR','Core training library, classes and member access.',true),
    ('PRO','RS PRO',4900,'EUR','Expanded coaching, advanced content and full member tools.',true),
    ('ELITE','RS ELITE',6900,'EUR','Premium access for advanced coaching and exclusive content.',true)
on conflict (code) do nothing;

alter table public.rs_membership_plans enable row level security;
revoke all on table public.rs_membership_plans from anon, authenticated;
grant select on table public.rs_membership_plans to authenticated;
grant update on table public.rs_membership_plans to authenticated;

drop policy if exists "rs_membership_plans_select_authenticated" on public.rs_membership_plans;
create policy "rs_membership_plans_select_authenticated"
on public.rs_membership_plans
for select
to authenticated
using (true);

drop policy if exists "rs_membership_plans_staff_update" on public.rs_membership_plans;
create policy "rs_membership_plans_staff_update"
on public.rs_membership_plans
for update
to authenticated
using ((select private.rs_is_staff()))
with check ((select private.rs_is_staff()));

comment on table public.rs_membership_plans is
'Trainer/admin-configurable BASIC, PRO and ELITE plan pricing and descriptions. Student profile plan codes continue to use these stable codes.';
