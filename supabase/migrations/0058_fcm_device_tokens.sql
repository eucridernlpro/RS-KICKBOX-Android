-- RS KICKBOXING v0.155 - Firebase Cloud Messaging device tokens
-- Safe to run repeatedly.

create table if not exists public.rs_fcm_device_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.rs_profiles(id) on delete cascade,
    token text not null unique,
    device_label text not null default 'Android',
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists rs_fcm_device_tokens_user_idx
on public.rs_fcm_device_tokens(user_id,active,updated_at desc);

alter table public.rs_fcm_device_tokens enable row level security;

drop policy if exists "rs_fcm_tokens_own_read" on public.rs_fcm_device_tokens;
create policy "rs_fcm_tokens_own_read"
on public.rs_fcm_device_tokens for select to authenticated
using(user_id=(select auth.uid()));

create or replace function public.rs_register_fcm_token(
    p_token text,
    p_device_label text default 'Android'
)
returns void
language plpgsql
security definer
set search_path=''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_token text := trim(coalesce(p_token,''));
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;
    if length(v_token)<20 then
        raise exception 'invalid FCM token' using errcode='22023';
    end if;

    insert into public.rs_fcm_device_tokens(user_id,token,device_label,active,updated_at)
    values(
        v_uid,
        v_token,
        left(coalesce(nullif(trim(p_device_label),''),'Android'),100),
        true,
        now()
    )
    on conflict(token) do update
    set user_id=excluded.user_id,
        device_label=excluded.device_label,
        active=true,
        updated_at=now();
end;
$$;

create or replace function public.rs_unregister_fcm_token(p_token text)
returns void
language sql
security definer
set search_path=''
as $$
    update public.rs_fcm_device_tokens
    set active=false,updated_at=now()
    where user_id=(select auth.uid())
      and token=trim(coalesce(p_token,''));
$$;

revoke all on function public.rs_register_fcm_token(text,text) from public,anon;
revoke all on function public.rs_unregister_fcm_token(text) from public,anon;
grant execute on function public.rs_register_fcm_token(text,text) to authenticated;
grant execute on function public.rs_unregister_fcm_token(text) to authenticated;
