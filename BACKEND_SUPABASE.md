# RS KICKBOX — Supabase handoff

This repository is now configured for the RS KICKBOX Supabase project using the public mobile Project URL + publishable key. The app now uses Supabase-backed production paths for authentication and multiple shared modules when the backend is configured; local fallbacks remain only where explicitly retained.

## Current backend state

A Supabase project is already connected to the Android build through the public mobile Project URL + publishable key. Never commit private server credentials.

Do **not** send or put a service_role key, secret key, database password, access token, or personal password in the Android source or GitHub repository.

The Android build already reads these optional Gradle properties:

- SUPABASE_URL
- SUPABASE_PUBLISHABLE_KEY

If they are absent, the app stays in LOCAL_ACCEPTANCE mode.

## Prepared backend assets

The repository now contains the ordered migration set through `0043_cloud_promotions_books.sql`, the cumulative setup/update files, and these Edge Functions:

- `supabase/functions/create-student-invite/index.ts`
- `supabase/functions/redeem-student-invite/index.ts`
- `supabase/functions/analyze-technique/index.ts`
- `supabase/functions/_shared/rs.ts`
- `supabase/config.toml`

For the currently confirmed project state (v0.98 backend update already applied), use **only**:
- `supabase/RS_KICKBOX_V105_BACKEND_UPDATE.sql`

That v0.105 delta contains migrations 0040–0044 only, so it does not unnecessarily rerun the already-applied chat/member-service migrations.

## Planned backend order

1. Create the Supabase project only when the Android acceptance build is ready for backend connection.
2. For a brand-new project, open Supabase SQL Editor and run `supabase/RS_KICKBOX_SETUP.sql` once. It contains all ordered migrations 0001–0019. For later incremental deployments, use the individual files in `supabase/migrations/`.
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


## v0.105 deployment checkpoint

From the confirmed v0.98 backend state:

1. Open the Supabase SQL Editor and run `supabase/RS_KICKBOX_V105_BACKEND_UPDATE.sql` once.
2. Deploy the authenticated Technique Coach function:
   ```bash
   supabase functions deploy analyze-technique
   ```
3. Configure the OpenAI API key only as a Supabase server secret:
   ```bash
   supabase secrets set OPENAI_API_KEY="<your OpenAI API key>"
   ```
4. Optional: override the cost-conscious default model:
   ```bash
   supabase secrets set RS_TECHNIQUE_AI_MODEL="gpt-5.6-luna"
   ```
5. Keep `OPENAI_API_KEY` out of Gradle properties, Android resources, GitHub source, screenshots, and chat messages.
6. Test the Edge Function while authenticated. If the function or secret is unavailable, Android intentionally falls back to the structured local coaching preview rather than exposing an error or secret.

The Technique Coach sends a small set of compressed sampled JPEG frames, not an OpenAI key. The Edge Function validates the signed-in RS KICKBOX account before contacting OpenAI.
