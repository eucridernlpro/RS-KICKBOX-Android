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

## Planned backend order

1. Apply migrations in `supabase/migrations/`.
2. Create Auth configuration and trainer/admin accounts.
3. Deploy server-side private invitation creation/redemption.
4. Connect Android Auth + PostgREST.
5. Move technique-video metadata to Postgres and media to private cloud storage.
6. Add classes, bookings, attendance, payments, notifications and remaining modules.
7. Add RLS tests before production release.

## Security rules

- Publishable key may be used in the Android client.
- Secret/service-role credentials stay server-side only.
- RLS is enabled on exposed tables before client access.
- Raw student invitation tokens are never stored in the database; only their one-way hash is stored.
- Trainer/admin privileges are enforced by backend authorization, not by UI buttons.
