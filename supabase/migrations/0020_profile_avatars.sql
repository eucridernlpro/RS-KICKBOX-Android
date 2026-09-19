-- RS KICKBOX backend foundation
-- Migration 0020: protected member profile avatars.

alter table public.rs_profiles
add column if not exists avatar_path text;

insert into storage.buckets (
    id,
    name,
    public,
    file_size_limit,
    allowed_mime_types
)
values (
    'rs-profile-images',
    'rs-profile-images',
    false,
    2097152,
    array['image/jpeg','image/png','image/webp']
)
on conflict (id) do update
set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "rs_profile_images_select_visible" on storage.objects;
create policy "rs_profile_images_select_visible"
on storage.objects
for select
to authenticated
using (
    bucket_id = 'rs-profile-images'
    and exists (
        select 1
        from public.rs_profiles p
        left join public.rs_social_profiles s on s.user_id = p.id
        where p.id::text = (storage.foldername(name))[1]
          and (
              p.id = (select auth.uid())
              or (select private.rs_is_staff())
              or coalesce(s.public_profile,false) = true
          )
    )
);

drop policy if exists "rs_profile_images_insert_own" on storage.objects;
create policy "rs_profile_images_insert_own"
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

drop policy if exists "rs_profile_images_update_own" on storage.objects;
create policy "rs_profile_images_update_own"
on storage.objects
for update
to authenticated
using (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
)
with check (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

drop policy if exists "rs_profile_images_delete_own" on storage.objects;
create policy "rs_profile_images_delete_own"
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'rs-profile-images'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create or replace function public.rs_set_my_avatar(p_avatar_path text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
    v_expected text;
begin
    if v_uid is null then
        raise exception 'authentication required' using errcode='42501';
    end if;

    v_expected := v_uid::text || '/avatar.jpg';

    if p_avatar_path is distinct from v_expected then
        raise exception 'invalid avatar path' using errcode='22023';
    end if;

    update public.rs_profiles
    set avatar_path = p_avatar_path,
        updated_at = now()
    where id = v_uid;
end;
$$;

revoke execute on function public.rs_set_my_avatar(text) from public;
revoke execute on function public.rs_set_my_avatar(text) from anon;
grant execute on function public.rs_set_my_avatar(text) to authenticated;

create or replace function public.rs_member_identity(p_email text)
returns table (
    id uuid,
    display_name text,
    avatar_path text
)
language sql
stable
security definer
set search_path = ''
as $$
    select p.id,p.display_name,p.avatar_path
    from public.rs_profiles p
    left join public.rs_social_profiles s on s.user_id=p.id
    where lower(p.email)=lower(trim(p_email))
      and (
          p.id=(select auth.uid())
          or (select private.rs_is_staff())
          or coalesce(s.public_profile,false)=true
      )
    limit 1;
$$;

revoke execute on function public.rs_member_identity(text) from public;
revoke execute on function public.rs_member_identity(text) from anon;
grant execute on function public.rs_member_identity(text) to authenticated;

comment on column public.rs_profiles.avatar_path is
'Protected Supabase Storage path for the member profile avatar.';

comment on function public.rs_member_identity(text) is
'Returns display identity/avatar only to the member, staff, or when the social profile is public.';
