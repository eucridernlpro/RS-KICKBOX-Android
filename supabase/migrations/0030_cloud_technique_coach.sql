-- RS KICKBOX backend foundation
-- Migration 0030: protected cloud Technique Coach submissions.

insert into storage.buckets(
    id,name,public,file_size_limit,allowed_mime_types
)
values(
    'rs-technique-submissions',
    'rs-technique-submissions',
    false,
    52428800,
    array['video/mp4','video/webm','video/quicktime','video/3gpp']
)
on conflict(id) do update
set public=excluded.public,
    file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "rs_technique_storage_insert_own" on storage.objects;
create policy "rs_technique_storage_insert_own"
on storage.objects
for insert
to authenticated
with check(
    bucket_id='rs-technique-submissions'
    and (storage.foldername(name))[1]=(select auth.uid())::text
);

drop policy if exists "rs_technique_storage_select_own_or_staff" on storage.objects;
create policy "rs_technique_storage_select_own_or_staff"
on storage.objects
for select
to authenticated
using(
    bucket_id='rs-technique-submissions'
    and (
        (storage.foldername(name))[1]=(select auth.uid())::text
        or (select private.rs_is_staff())
    )
);

drop policy if exists "rs_technique_storage_delete_own_or_staff" on storage.objects;
create policy "rs_technique_storage_delete_own_or_staff"
on storage.objects
for delete
to authenticated
using(
    bucket_id='rs-technique-submissions'
    and (
        (storage.foldername(name))[1]=(select auth.uid())::text
        or (select private.rs_is_staff())
    )
);

create or replace function public.rs_staff_set_technique_review(
    p_submission_id uuid,
    p_favorite boolean,
    p_trainer_note text
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

    update public.rs_technique_submissions
    set trainer_favorite=p_favorite,
        trainer_note=coalesce(p_trainer_note,''),
        updated_at=now()
    where id=p_submission_id;

    if not found then
        raise exception 'technique submission not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_technique_review(uuid,boolean,text) from public;
revoke execute on function public.rs_staff_set_technique_review(uuid,boolean,text) from anon;
grant execute on function public.rs_staff_set_technique_review(uuid,boolean,text) to authenticated;

create or replace function public.rs_technique_submission_catalog()
returns table(
    id uuid,
    student_id uuid,
    student_email text,
    student_name text,
    technique text,
    media_path text,
    media_name text,
    student_summary text,
    trainer_note text,
    trainer_favorite boolean,
    created_at timestamptz
)
language sql
stable
security definer
set search_path=''
as $$
    select
        s.id,
        s.student_id,
        p.email,
        p.display_name,
        s.technique,
        s.media_path,
        s.media_name,
        s.student_summary,
        s.trainer_note,
        s.trainer_favorite,
        s.created_at
    from public.rs_technique_submissions s
    join public.rs_profiles p on p.id=s.student_id
    where s.student_id=(select auth.uid())
       or (select private.rs_is_staff())
    order by s.created_at desc;
$$;

revoke execute on function public.rs_technique_submission_catalog() from public;
revoke execute on function public.rs_technique_submission_catalog() from anon;
grant execute on function public.rs_technique_submission_catalog() to authenticated;
