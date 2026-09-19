-- RS KICKBOXING backend foundation
-- Migration 0047: scalable storage quota profiles by Supabase tier and membership plan.

create table if not exists public.rs_storage_quota_config (
    singleton boolean primary key default true check(singleton),
    backend_tier text not null default 'FREE' check(backend_tier in ('FREE','PRO')),
    updated_by uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);

insert into public.rs_storage_quota_config(singleton,backend_tier)
values(true,'FREE')
on conflict(singleton) do nothing;

create table if not exists public.rs_storage_quota_profiles (
    backend_tier text not null check(backend_tier in ('FREE','PRO')),
    membership_plan text not null check(membership_plan in ('BASIC','PRO','ELITE')),
    default_limit_bytes bigint not null check(default_limit_bytes between 1048576 and 10737418240),
    primary key(backend_tier,membership_plan)
);

insert into public.rs_storage_quota_profiles(backend_tier,membership_plan,default_limit_bytes)
values
('FREE','BASIC',26214400),
('FREE','PRO',52428800),
('FREE','ELITE',104857600),
('PRO','BASIC',262144000),
('PRO','PRO',524288000),
('PRO','ELITE',1073741824)
on conflict(backend_tier,membership_plan) do nothing;

alter table public.rs_storage_quota_config enable row level security;
alter table public.rs_storage_quota_profiles enable row level security;
revoke all on public.rs_storage_quota_config,public.rs_storage_quota_profiles from anon,authenticated;
grant select,insert,update,delete on public.rs_storage_quota_config,public.rs_storage_quota_profiles to authenticated;

drop policy if exists "rs_storage_quota_config_staff" on public.rs_storage_quota_config;
create policy "rs_storage_quota_config_staff" on public.rs_storage_quota_config
for all to authenticated using ((select private.rs_is_staff())) with check ((select private.rs_is_staff()));

drop policy if exists "rs_storage_quota_profiles_staff" on public.rs_storage_quota_profiles;
create policy "rs_storage_quota_profiles_staff" on public.rs_storage_quota_profiles
for all to authenticated using ((select private.rs_is_staff())) with check ((select private.rs_is_staff()));

drop function if exists public.rs_staff_student_storage_usage();

create function public.rs_staff_student_storage_usage()
returns table(
    student_id uuid,
    email text,
    display_name text,
    membership_plan text,
    used_bytes bigint,
    limit_bytes bigint,
    asset_count bigint,
    warning_message text,
    custom_limit boolean
)
language sql stable security definer set search_path=''
as $$
    with tier as (
        select c.backend_tier from public.rs_storage_quota_config c where c.singleton=true
    )
    select
        p.id,
        p.email,
        p.display_name,
        coalesce(m.plan,'PRO')::text,
        coalesce(sum(a.byte_size),0)::bigint,
        coalesce(
            l.limit_bytes,
            q.default_limit_bytes,
            52428800
        )::bigint,
        count(a.id)::bigint,
        coalesce(l.warning_message,''),
        (l.student_id is not null)
    from public.rs_profiles p
    left join public.rs_memberships m on m.student_id=p.id
    left join public.rs_student_media_assets a on a.student_id=p.id
    left join public.rs_student_storage_limits l on l.student_id=p.id
    left join tier t on true
    left join public.rs_storage_quota_profiles q
      on q.backend_tier=coalesce(t.backend_tier,'FREE')
     and q.membership_plan=coalesce(m.plan,'PRO')
    where p.role='student' and (select private.rs_is_staff())
    group by p.id,p.email,p.display_name,m.plan,l.student_id,l.limit_bytes,l.warning_message,q.default_limit_bytes
    order by lower(p.display_name),lower(p.email);
$$;

create or replace function public.rs_staff_storage_quota_settings()
returns table(
    backend_tier text,
    free_basic bigint,
    free_pro bigint,
    free_elite bigint,
    pro_basic bigint,
    pro_pro bigint,
    pro_elite bigint
)
language sql stable security definer set search_path=''
as $$
    select
        coalesce((select c.backend_tier from public.rs_storage_quota_config c where c.singleton=true),'FREE'),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='FREE' and q.membership_plan='BASIC'),26214400),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='FREE' and q.membership_plan='PRO'),52428800),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='FREE' and q.membership_plan='ELITE'),104857600),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='PRO' and q.membership_plan='BASIC'),262144000),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='PRO' and q.membership_plan='PRO'),524288000),
        coalesce((select q.default_limit_bytes from public.rs_storage_quota_profiles q where q.backend_tier='PRO' and q.membership_plan='ELITE'),1073741824)
    where (select private.rs_is_staff());
$$;

create or replace function public.rs_staff_set_storage_backend_tier(p_backend_tier text)
returns void
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    if p_backend_tier not in ('FREE','PRO') then raise exception 'invalid backend tier' using errcode='22023'; end if;
    insert into public.rs_storage_quota_config(singleton,backend_tier,updated_by,updated_at)
    values(true,p_backend_tier,(select auth.uid()),now())
    on conflict(singleton) do update set
      backend_tier=excluded.backend_tier,updated_by=excluded.updated_by,updated_at=now();
end;
$$;

create or replace function public.rs_staff_set_storage_plan_default(
    p_backend_tier text,p_membership_plan text,p_limit_bytes bigint
) returns void
language plpgsql security definer set search_path=''
as $$
begin
    if not (select private.rs_is_staff()) then raise exception 'trainer/admin access required' using errcode='42501'; end if;
    if p_backend_tier not in ('FREE','PRO') or p_membership_plan not in ('BASIC','PRO','ELITE') then
        raise exception 'invalid storage profile' using errcode='22023';
    end if;
    insert into public.rs_storage_quota_profiles(backend_tier,membership_plan,default_limit_bytes)
    values(p_backend_tier,p_membership_plan,greatest(1048576,p_limit_bytes))
    on conflict(backend_tier,membership_plan) do update set default_limit_bytes=excluded.default_limit_bytes;
end;
$$;

revoke execute on function public.rs_staff_student_storage_usage() from public,anon;
revoke execute on function public.rs_staff_storage_quota_settings() from public,anon;
revoke execute on function public.rs_staff_set_storage_backend_tier(text) from public,anon;
revoke execute on function public.rs_staff_set_storage_plan_default(text,text,bigint) from public,anon;
grant execute on function public.rs_staff_student_storage_usage() to authenticated;
grant execute on function public.rs_staff_storage_quota_settings() to authenticated;
grant execute on function public.rs_staff_set_storage_backend_tier(text) to authenticated;
grant execute on function public.rs_staff_set_storage_plan_default(text,text,bigint) to authenticated;

comment on table public.rs_storage_quota_profiles is
'Soft student storage defaults by Supabase backend tier and RS membership. Per-student overrides in rs_student_storage_limits always take priority.';
