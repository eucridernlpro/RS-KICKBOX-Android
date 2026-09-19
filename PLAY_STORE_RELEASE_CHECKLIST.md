# RS KICKBOX — Play Store Release Checklist

Current Android line: **v0.110.0** · **versionCode 110**  
Package: **com.rskickbox.app**  
Target SDK: **36**

## Already implemented in source

- Premium black / metallic-gold RS KICKBOX visual system.
- Separate trainer/admin and student experiences.
- Supabase authentication and cloud-backed modules where configured.
- Student feature-access controls with Student Dashboard + App Guide filtering.
- Persistent Media3 music playback with MediaSessionService, notification controls, mini-player, metadata, wake-mode hardening, named playlists and in-app volume control.
- Private coach chat media + member-only group chat.
- Cloud branding, theme, visual assets, splash/intro, promotions and trainer book, with resume/foreground visual refresh for multi-device testing.
- Cloud member services: documents, support and referrals.
- Account password recovery/logout state repair, Android/Google Password Manager autofill semantics, remembered successful email and rotation-safe authenticated Activity state.
- Trainer Privacy Requests center for secure review/status handling of member data-export and account-deletion requests.
- Nine-language UI framework with cloud/error/localization cleanup across active modules, including splash, visual studio, profile/privacy, social, media and music controls.
- AI Technique Coach:
  - local video import/private storage;
  - 20-second input limit;
  - sampled-frame AI path through authenticated Supabase Edge Function;
  - OpenAI secret remains server-side;
  - automatic structured-coaching fallback if AI is unavailable;
  - server-side daily AI quota to control external AI cost/abuse.
- Adaptive RS crown launcher icon fallback.
- Android network hardening: cleartext HTTP disabled.
- Android app backup disabled for sensitive private/session data.
- Optional release-signing Gradle configuration.
- Manual GitHub workflow for signed release AAB creation.
- Public-ready legal pages in `docs/`:
  - Privacy Policy;
  - Account Deletion;
  - Terms of Use.
- GitHub Pages deployment workflow prepared.
- v0.105 Supabase delta SQL prepared for the confirmed v0.98 backend state.

## Backend deployment still required

### 1. Apply v0.105 SQL delta
Run once in Supabase SQL Editor:

`supabase/RS_KICKBOX_V105_BACKEND_UPDATE.sql`

This contains migrations 0040–0044 only and assumes the successful v0.98 update is already applied.

### 2. Deploy Technique Coach Edge Function

```bash
supabase functions deploy analyze-technique
```

### 3. Add the AI provider secret in Supabase

```bash
supabase secrets set OPENAI_API_KEY="<your key>"
```

Optional model override:

```bash
supabase secrets set RS_TECHNIQUE_AI_MODEL="gpt-5.6-luna"
```

Never place the OpenAI key in Android source, Gradle properties, GitHub source, screenshots or chat.

## Legal hosting still required

GitHub Pages workflow is ready, but repository Pages must first be enabled manually:

**GitHub repository → Settings → Pages → Build and deployment → Source → GitHub Actions**

Expected URLs after successful deployment:

- Privacy: `https://eucridernlpro.github.io/RS-KICKBOX-Android/privacy-policy.html`
- Account deletion: `https://eucridernlpro.github.io/RS-KICKBOX-Android/account-deletion.html`
- Terms: `https://eucridernlpro.github.io/RS-KICKBOX-Android/terms.html`

After Pages is enabled, test all three URLs from a logged-out browser.

## Release signing still required

Create or use the final Android upload/release keystore and add these GitHub Actions secrets:

- `RS_ANDROID_KEYSTORE_BASE64`
- `RS_ANDROID_KEYSTORE_PASSWORD`
- `RS_ANDROID_KEY_ALIAS`
- `RS_ANDROID_KEY_PASSWORD`
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`

Then manually run:

**Actions → Build RS KICKBOX Signed AAB → Run workflow**

Expected artifact:
`app/build/outputs/bundle/release/app-release.aab`

Do not commit the keystore.

## Google Play Data Safety inventory to verify in Play Console

The final Data Safety answers must match the live production configuration. Review at least these categories:

- Account identifiers: email, user ID, display name.
- Profile data: profile photo, role, membership plan.
- User-generated content: private messages, group messages, support messages, uploaded media.
- App activity / training data: bookings, attendance, homework, progress, assessments, challenges, badges, Fight Camp, session activity.
- Financial / membership records: plan, invoices and payment status. Current RS KICKBOX source is an invoice/status ledger and does not itself process or store payment-card credentials; re-check this if a real payment provider is added later.
- Photos/videos: selected training and technique-review media.
- Optional AI input: sampled frames from a user-selected technique video when the AI Technique Coach is invoked.
- Device-local preferences: language, theme, player state and settings.

Current Android source contains no advertising SDK.

## Account deletion policy checks

Before release, verify:

- In-app deletion request path works for authenticated users.
- External deletion page is publicly reachable without login.
- Trainer/admin Privacy Requests center can review and track requests.
- Support process can verify account ownership.
- Backend deletion procedure removes associated user data where legally permitted.
- Any retained records have a documented legitimate reason and retention period.
- Play Console Data Safety deletion questions match the real process.

## Physical-device QC required before store upload

Run at least:

- Android phone with compact display.
- Modern large-screen Android phone.
- Screen off / lock screen during music playback.
- App removed from recent apps while music is playing.
- Notification media controls: pause, play, next, previous.
- Login → logout → normal login screen.
- Password reset deep link → new password → logout → normal login.
- Trainer disables student feature → hidden from Dashboard, drawer and Student App Guide.
- Language switch through all nine languages on login, dashboard, App Guide, privacy, chat, cloud member services and Technique Coach.
- Splash enabled / disabled / every-launch settings.
- Trainer global brand/theme/background/splash change → second device sync.
- Group chat text/image/short-video send + receive.
- Private coach chat attachment send + receive.
- Documents / Support / Referrals cloud sync.
- Promotions / Book cloud sync.
- AI Technique Coach with:
  - Edge Function + OpenAI secret configured;
  - Edge Function unavailable, confirming safe fallback.
- Privacy / deletion / terms links from inside the app.

## Play Console tasks still required

- Create or select the app entry for `com.rskickbox.app`.
- Upload signed AAB.
- Complete App access instructions for reviewer login.
- Complete Data Safety form.
- Enter public Privacy Policy URL.
- Enter public Account Deletion URL.
- Complete foreground-service/media-playback declarations if requested by Play Console.
- Complete content rating.
- Declare target audience and age groups accurately.
- Add store listing icon, feature graphic, screenshots and descriptions.
- Complete testing-track requirements applicable to the developer account.
- Run Pre-launch report and repair any crashes/ANRs/accessibility/layout issues before production.

## Current release blockers

1. v0.105 Supabase delta not yet confirmed as applied.
2. `analyze-technique` Edge Function not yet confirmed as deployed.
3. `OPENAI_API_KEY` Supabase secret not yet confirmed.
4. GitHub Pages not enabled yet.
5. Final Android signing keystore/secrets not yet configured.
6. Physical-device matrix QC not yet completed.
7. Authorized server-side account-deletion/retention procedure is not yet fully automated/verified.
8. Play Console forms/listing/reviewer access not yet completed.

Do not call the project production-ready until all blockers above are verified.
