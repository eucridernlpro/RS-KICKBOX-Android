-- RS KICKBOXING backend
-- Migration 0048: enforce per-student soft storage quotas when media is registered.
-- Requires 0045 + 0046 + 0047.

create or replace function public.rs_register_student_media_asset(
    p_student_id uuid,
    p_storage_bucket text,
    p_storage_path text,
    p_media_kind text,
    p_source_area text,
    p_byte_size bigint
) returns uuid
language plpgsql security definer set search_path=''
as $$
declare
    v_uid uuid:=(select auth.uid());
    v_id uuid;
    v_backend_tier text;
    v_membership_plan text;
    v_limit_bytes bigint;
    v_used_bytes bigint;
    v_existing_bytes bigint;
    v_new_bytes bigint:=greatest(0,coalesce(p_byte_size,0));
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if v_uid<>p_student_id and not (select private.rs_is_staff()) then
        raise exception 'student or staff access required' using errcode='42501';
    end if;

    if p_media_kind not in ('VIDEO','IMAGE','GIF','OTHER') then
        raise exception 'invalid media kind' using errcode='22023';
    end if;

    if p_source_area not in ('HOMEWORK','TECHNIQUE','PRIVATE_LESSON','CHAT','OTHER') then
        raise exception 'invalid source area' using errcode='22023';
    end if;

    if trim(coalesce(p_storage_bucket,''))='' or trim(coalesce(p_storage_path,''))='' then
        raise exception 'storage location required' using errcode='22023';
    end if;

    select coalesce(c.backend_tier,'FREE')
      into v_backend_tier
      from public.rs_storage_quota_config c
     where c.singleton=true;

    v_backend_tier:=coalesce(v_backend_tier,'FREE');

    select coalesce(m.plan,'PRO')
      into v_membership_plan
      from public.rs_profiles p
      left join public.rs_memberships m on m.student_id=p.id
     where p.id=p_student_id;

    v_membership_plan:=coalesce(v_membership_plan,'PRO');

    select coalesce(
        (select l.limit_bytes
           from public.rs_student_storage_limits l
          where l.student_id=p_student_id),
        (select q.default_limit_bytes
           from public.rs_storage_quota_profiles q
          where q.backend_tier=v_backend_tier
            and q.membership_plan=v_membership_plan),
        52428800
    ) into v_limit_bytes;

    select coalesce(sum(a.byte_size),0)
      into v_used_bytes
      from public.rs_student_media_assets a
     where a.student_id=p_student_id;

    select coalesce(a.byte_size,0)
      into v_existing_bytes
      from public.rs_student_media_assets a
     where a.storage_path=trim(p_storage_path)
     limit 1;

    v_existing_bytes:=coalesce(v_existing_bytes,0);

    if (v_used_bytes-v_existing_bytes+v_new_bytes)>v_limit_bytes then
        raise exception 'student storage limit exceeded (% used + % incoming > % limit)',
            greatest(0,v_used_bytes-v_existing_bytes),v_new_bytes,v_limit_bytes
            using errcode='P0001';
    end if;

    insert into public.rs_student_media_assets(
        student_id,storage_bucket,storage_path,media_kind,source_area,byte_size,created_by
    )
    values(
        p_student_id,trim(p_storage_bucket),trim(p_storage_path),p_media_kind,p_source_area,
        v_new_bytes,v_uid
    )
    on conflict(storage_path) do update set
        student_id=excluded.student_id,
        storage_bucket=excluded.storage_bucket,
        media_kind=excluded.media_kind,
        source_area=excluded.source_area,
        byte_size=excluded.byte_size
    returning id into v_id;

    return v_id;
end;
$$;

revoke execute on function public.rs_register_student_media_asset(uuid,text,text,text,text,bigint) from public,anon;
grant execute on function public.rs_register_student_media_asset(uuid,text,text,text,text,bigint) to authenticated;



create or replace function public.rs_student_storage_capacity(
    p_student_id uuid,
    p_incoming_bytes bigint default 0
) returns table(
    used_bytes bigint,
    limit_bytes bigint,
    remaining_bytes bigint,
    allowed boolean,
    warning_message text
)
language plpgsql stable security definer set search_path=''
as $capacity$
declare
    v_uid uuid:=(select auth.uid());
    v_backend_tier text;
    v_membership_plan text;
    v_limit_bytes bigint;
    v_used_bytes bigint;
    v_warning text;
    v_incoming bigint:=greatest(0,coalesce(p_incoming_bytes,0));
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    if v_uid<>p_student_id and not (select private.rs_is_staff()) then
        raise exception 'student or staff access required' using errcode='42501';
    end if;

    select coalesce(c.backend_tier,'FREE')
      into v_backend_tier
      from public.rs_storage_quota_config c
     where c.singleton=true;
    v_backend_tier:=coalesce(v_backend_tier,'FREE');

    select coalesce(m.plan,'PRO')
      into v_membership_plan
      from public.rs_profiles p
      left join public.rs_memberships m on m.student_id=p.id
     where p.id=p_student_id;
    v_membership_plan:=coalesce(v_membership_plan,'PRO');

    select
        coalesce(
            l.limit_bytes,
            q.default_limit_bytes,
            52428800
        ),
        coalesce(l.warning_message,'')
      into v_limit_bytes,v_warning
      from (select 1) seed
      left join public.rs_student_storage_limits l on l.student_id=p_student_id
      left join public.rs_storage_quota_profiles q
        on q.backend_tier=v_backend_tier
       and q.membership_plan=v_membership_plan;

    select coalesce(sum(a.byte_size),0)
      into v_used_bytes
      from public.rs_student_media_assets a
     where a.student_id=p_student_id;

    return query
    select
        v_used_bytes,
        v_limit_bytes,
        greatest(0,v_limit_bytes-v_used_bytes),
        (v_used_bytes+v_incoming)<=v_limit_bytes,
        v_warning;
end;
$capacity$;

revoke execute on function public.rs_student_storage_capacity(uuid,bigint) from public,anon;
grant execute on function public.rs_student_storage_capacity(uuid,bigint) to authenticated;

comment on function public.rs_student_storage_capacity(uuid,bigint) is
'Returns effective student storage usage/capacity and whether an incoming upload fits before bytes are sent to storage.';

comment on function public.rs_register_student_media_asset(uuid,text,text,text,text,bigint) is
'Registers student-owned media only when the effective per-student soft storage quota has enough capacity. Per-student override takes priority over membership/Supabase-tier defaults.';
