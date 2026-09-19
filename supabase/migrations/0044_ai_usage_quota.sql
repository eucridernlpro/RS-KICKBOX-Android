-- RS KICKBOX backend foundation
-- Migration 0044: server-enforced daily quota for paid external AI operations.

create table if not exists public.rs_ai_usage_daily (
    user_id uuid not null references auth.users(id) on delete cascade,
    usage_date date not null default current_date,
    feature text not null,
    calls integer not null default 0 check (calls >= 0),
    updated_at timestamptz not null default now(),
    primary key (user_id, usage_date, feature)
);

alter table public.rs_ai_usage_daily enable row level security;

revoke all on table public.rs_ai_usage_daily from public;
revoke all on table public.rs_ai_usage_daily from anon;
revoke all on table public.rs_ai_usage_daily from authenticated;

create or replace function public.rs_consume_ai_quota(
    p_feature text,
    p_limit integer
)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_active boolean;
    v_calls integer;
    v_limit integer := greatest(1, least(coalesce(p_limit, 20), 100));
    v_feature text := left(coalesce(nullif(trim(p_feature), ''), 'unknown'), 80);
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select p.active
    into v_active
    from public.rs_profiles p
    where p.id=v_uid;

    if coalesce(v_active,false)=false then
        raise exception 'active account required' using errcode='42501';
    end if;

    insert into public.rs_ai_usage_daily (
        user_id, usage_date, feature, calls, updated_at
    )
    values (
        v_uid, current_date, v_feature, 1, now()
    )
    on conflict (user_id, usage_date, feature) do update
    set
        calls=public.rs_ai_usage_daily.calls+1,
        updated_at=now()
    where public.rs_ai_usage_daily.calls < v_limit
    returning calls into v_calls;

    if v_calls is null then
        raise exception 'daily ai limit reached' using errcode='P0001';
    end if;

    return v_calls;
end;
$$;

revoke execute on function public.rs_consume_ai_quota(text,integer) from public;
revoke execute on function public.rs_consume_ai_quota(text,integer) from anon;
grant execute on function public.rs_consume_ai_quota(text,integer) to authenticated;

comment on table public.rs_ai_usage_daily is
'Private server-side daily usage counters for external AI features.';

comment on function public.rs_consume_ai_quota(text,integer) is
'Atomically consumes one authenticated AI quota unit and raises when the daily limit is reached.';
