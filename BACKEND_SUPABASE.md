# RS KICKBOX — Supabase handoff

This repository is prepared for a future Supabase connection but remains in LOCAL_ACCEPTANCE mode until real project credentials are supplied at build time.

## Do not create the account yet unless ChatGPT asks for it

The Android acceptance UI can continue to be developed without a Supabase project. When the backend stage starts, use the official dashboard:

https://supabase.com/dashboard

Create one project for RS KICKBOX. After the project is ready, open the project's **Connect** dialog and obtain:

- Project URL
- Publishable key

Do **not** send or put a service_role key, secret key, database password, access token, or personal password in the Android source or GitHub repository.

The Android build already reads these optional Gradle properties:

- SUPABASE_URL
- SUPABASE_PUBLISHABLE_KEY

If they are absent, the app stays in LOCAL_ACCEPTANCE mode.

## Prepared backend assets

The repository now contains:

- `supabase/migrations/0001_core_identity_and_coaching.sql`
- `supabase/migrations/0002_classes_bookings_attendance.sql`
- `supabase/migrations/0003_memberships_invoices_payments.sql`
- `supabase/migrations/0004_account_privacy_requests.sql`
- `supabase/functions/create-student-invite/index.ts`
- `supabase/functions/redeem-student-invite/index.ts`
- `supabase/functions/_shared/rs.ts`
- `supabase/config.toml`

## Planned backend order

1. Create the Supabase project only when the Android acceptance build is ready for backend connection.
2. Apply all migrations in `supabase/migrations/` in numeric order.
3. Create the first trainer/admin Auth user and matching `rs_profiles` row.
4. Deploy `create-student-invite` and `redeem-student-invite`.
5. Test invitation creation, one-time redemption, expiry/revocation and the 100-active-student limit.
6. Connect Android Auth + database access using the Project URL and publishable key.
7. Move technique videos to private cloud storage and metadata to `rs_technique_submissions`.
8. Migrate classes, bookings, attendance, memberships, invoices and payments from local acceptance storage.
9. Add RLS and Edge Function integration tests before production release.

## Edge Function authentication

- `create-student-invite` requires a signed-in user JWT and checks that the caller is an active trainer/admin.
- `redeem-student-invite` is callable before login, but requires the project publishable key and a high-entropy one-time invitation token.
- Raw invitation tokens are returned only to the trainer/app flow and are stored in the database only as SHA-256 hashes.
- The Edge Functions use Supabase secret credentials only in their server environment.
- `supabase/config.toml` keeps JWT verification enabled for trainer invite creation and disables the platform JWT requirement only for invite redemption because the student is not authenticated yet.

## Deployment checkpoint

When the Supabase project exists, the intended CLI flow is:

```bash
supabase link --project-ref <project-ref>
supabase db push
supabase functions deploy create-student-invite
supabase functions deploy redeem-student-invite
```

Do not create a local `.env` containing production credentials unless required for local testing. Supabase env files and signing files are excluded by `.gitignore`.

## Security rules

- Publishable key may be used in the Android client.
- Secret/service-role credentials stay server-side only.
- RLS is enabled on exposed tables before client access.
- Raw student invitation tokens are never stored in the database; only their one-way hash is stored.
- Trainer/admin privileges are enforced by backend authorization, not by UI buttons.
