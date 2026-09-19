-- RS KICKBOX backend foundation
-- Migration 0029: production billing summary and invoice ledger actions.

create or replace function public.rs_my_billing_summary()
returns table (
    plan text,
    active boolean,
    membership_status text,
    amount_cents integer,
    currency text,
    current_period_end timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.plan,
        p.active,
        coalesce(m.status,'active'),
        coalesce(m.amount_cents,mp.monthly_cents,0),
        coalesce(m.currency,mp.currency,'EUR'),
        m.current_period_end
    from public.rs_profiles p
    left join public.rs_memberships m on m.student_id=p.id
    left join public.rs_membership_plans mp on mp.code=p.plan
    where p.id=(select auth.uid())
    limit 1;
$$;

revoke execute on function public.rs_my_billing_summary() from public;
revoke execute on function public.rs_my_billing_summary() from anon;
grant execute on function public.rs_my_billing_summary() to authenticated;

create or replace function public.rs_my_invoices()
returns table (
    id uuid,
    invoice_number text,
    period_label text,
    amount_cents integer,
    currency text,
    status text,
    due_at timestamptz,
    paid_at timestamptz,
    created_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        i.id,
        i.invoice_number,
        i.period_label,
        i.amount_cents,
        i.currency,
        i.status,
        i.due_at,
        i.paid_at,
        i.created_at
    from public.rs_invoices i
    where i.student_id=(select auth.uid())
    order by i.created_at desc;
$$;

revoke execute on function public.rs_my_invoices() from public;
revoke execute on function public.rs_my_invoices() from anon;
grant execute on function public.rs_my_invoices() to authenticated;

create or replace function public.rs_staff_invoice_catalog()
returns table (
    id uuid,
    invoice_number text,
    student_id uuid,
    student_email text,
    student_name text,
    period_label text,
    amount_cents integer,
    currency text,
    status text,
    due_at timestamptz,
    paid_at timestamptz,
    created_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        i.id,
        i.invoice_number,
        i.student_id,
        p.email,
        p.display_name,
        i.period_label,
        i.amount_cents,
        i.currency,
        i.status,
        i.due_at,
        i.paid_at,
        i.created_at
    from public.rs_invoices i
    join public.rs_profiles p on p.id=i.student_id
    where (select private.rs_is_staff())
    order by i.created_at desc;
$$;

revoke execute on function public.rs_staff_invoice_catalog() from public;
revoke execute on function public.rs_staff_invoice_catalog() from anon;
grant execute on function public.rs_staff_invoice_catalog() to authenticated;

create or replace function public.rs_staff_create_invoice(
    p_student_email text,
    p_period_label text,
    p_amount_cents integer,
    p_due_at timestamptz default null
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_student_id uuid;
    v_invoice_id uuid;
    v_number text;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if trim(p_period_label)='' then
        raise exception 'period required' using errcode='22023';
    end if;
    if p_amount_cents<0 then
        raise exception 'invalid amount' using errcode='22023';
    end if;

    select p.id into v_student_id
    from public.rs_profiles p
    where lower(p.email)=lower(trim(p_student_email))
      and p.role='student'
    limit 1;

    if v_student_id is null then
        raise exception 'student not found' using errcode='P0002';
    end if;

    v_number := 'RS-' || to_char(now(),'YYYYMMDDHH24MISS') || '-' || upper(substr(replace(gen_random_uuid()::text,'-',''),1,6));

    insert into public.rs_invoices(
        invoice_number,
        student_id,
        period_label,
        amount_cents,
        currency,
        status,
        due_at
    )
    values(
        v_number,
        v_student_id,
        trim(p_period_label),
        p_amount_cents,
        'EUR',
        'pending',
        p_due_at
    )
    returning id into v_invoice_id;

    return v_invoice_id;
end;
$$;

revoke execute on function public.rs_staff_create_invoice(text,text,integer,timestamptz) from public;
revoke execute on function public.rs_staff_create_invoice(text,text,integer,timestamptz) from anon;
grant execute on function public.rs_staff_create_invoice(text,text,integer,timestamptz) to authenticated;

create or replace function public.rs_staff_set_invoice_status(
    p_invoice_id uuid,
    p_status text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_invoice public.rs_invoices%rowtype;
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    if p_status not in ('pending','paid','void','overdue') then
        raise exception 'invalid invoice status' using errcode='22023';
    end if;

    select * into v_invoice
    from public.rs_invoices
    where id=p_invoice_id
    for update;

    if v_invoice.id is null then
        raise exception 'invoice not found' using errcode='P0002';
    end if;

    update public.rs_invoices
    set
        status=p_status,
        paid_at=case when p_status='paid' then coalesce(paid_at,now()) else null end,
        updated_at=now()
    where id=p_invoice_id;

    if p_status='paid' and not exists(
        select 1 from public.rs_payments p
        where p.invoice_id=p_invoice_id
          and p.status='succeeded'
    ) then
        insert into public.rs_payments(
            invoice_id,
            student_id,
            amount_cents,
            currency,
            method,
            status,
            provider,
            paid_at
        )
        values(
            p_invoice_id,
            v_invoice.student_id,
            v_invoice.amount_cents,
            v_invoice.currency,
            'manual',
            'succeeded',
            'RS KICKBOX admin',
            now()
        );
    end if;
end;
$$;

revoke execute on function public.rs_staff_set_invoice_status(uuid,text) from public;
revoke execute on function public.rs_staff_set_invoice_status(uuid,text) from anon;
grant execute on function public.rs_staff_set_invoice_status(uuid,text) to authenticated;

create or replace function public.rs_staff_delete_invoice(p_invoice_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not (select private.rs_is_staff()) then
        raise exception 'trainer/admin access required' using errcode='42501';
    end if;

    delete from public.rs_invoices where id=p_invoice_id;

    if not found then
        raise exception 'invoice not found' using errcode='P0002';
    end if;
end;
$$;

revoke execute on function public.rs_staff_delete_invoice(uuid) from public;
revoke execute on function public.rs_staff_delete_invoice(uuid) from anon;
grant execute on function public.rs_staff_delete_invoice(uuid) to authenticated;
