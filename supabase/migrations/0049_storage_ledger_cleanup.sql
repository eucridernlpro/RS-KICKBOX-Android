-- RS KICKBOXING backend
-- Migration 0049: keep student storage quota ledger synchronized with media deletion.
-- Requires 0045-0048.

create or replace function public.rs_remove_student_media_asset_by_path(
    p_storage_bucket text,
    p_storage_path text
) returns void
language plpgsql security definer set search_path=''
as $cleanup$
declare
    v_uid uuid := (select auth.uid());
    v_student_id uuid;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    select a.student_id
      into v_student_id
      from public.rs_student_media_assets a
     where a.storage_bucket=trim(p_storage_bucket)
       and a.storage_path=trim(p_storage_path)
     limit 1;

    if v_student_id is null then
        return;
    end if;

    if v_uid<>v_student_id and not (select private.rs_is_staff()) then
        raise exception 'student or staff access required' using errcode='42501';
    end if;

    delete from public.rs_student_media_assets a
     where a.storage_bucket=trim(p_storage_bucket)
       and a.storage_path=trim(p_storage_path);
end;
$cleanup$;

revoke execute on function public.rs_remove_student_media_asset_by_path(text,text) from public,anon;
grant execute on function public.rs_remove_student_media_asset_by_path(text,text) to authenticated;

comment on function public.rs_remove_student_media_asset_by_path(text,text) is
'Removes a tracked student media quota-ledger row by storage bucket/path for the owning student or staff.';
