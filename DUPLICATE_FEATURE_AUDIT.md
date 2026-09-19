# RS KICKBOX — Duplicate / Similar Feature Audit

Status: **NO DELETIONS APPROVED**  
Rule: do not remove routes, screens, source files or data migrations without explicit owner approval.

## Active application entry point

`MainActivity` launches:

`RsKickboxV21App(...)`

This is the current production application shell.

## User-visible consolidation already completed

The current V21 dashboard no longer exposes these as duplicate tiles:

### Workout / Home Training
- Primary visible route: `home_training`
- Current screen: `RsWorkoutHomeHubV89`
- Hub includes:
  - Home Training
  - Workout Generator
- Legacy alias still accepted: `workout`
- Recommendation: **keep alias for compatibility**, do not show it as a separate tile.

### Knowledge Vault / Search / Favorites / History
- Primary visible route: `vault`
- Current screen: `RsKnowledgeHubV89`
- Hub includes tabs:
  - Browse
  - Search
  - Favorites
  - History
- Legacy standalone routes still accepted:
  - `search`
  - `favorites`
  - `history`
- Recommendation: **keep aliases for compatibility**, do not show separate dashboard tiles.

### Attendance / QR Attendance
- Primary trainer route: `attendance`
- Current screen: `RsAttendanceCenterV89`
- Hub includes roster + QR attendance.
- Legacy alias: `qr_attendance`
- Recommendation: **keep alias for compatibility**, no duplicate dashboard tile.

### Content Manager / Lesson Editor
- Primary trainer route: `content`
- Both `content` and `lesson_editor` currently resolve to `RsContentManagerV48`.
- Recommendation: keep `content` as canonical route. Keep `lesson_editor` alias until approved for removal.

### Session Builder aliases
- Student route `session` opens Session Player.
- Trainer route `session_builder` opens Session Builder.
- If a trainer reaches `session`, it also resolves to Session Builder.
- Recommendation: keep current role-aware behavior; do not create another visible trainer tile.

## Similar names that are NOT duplicates

Do **not** merge/delete these merely because the names are similar:

- `music` and `music_admin`: same persistent player foundation, but role-specific entry semantics.
- `events` and `events_admin`: student event experience vs trainer management.
- `progress` and `progress_admin`: student progress view vs trainer progress management.
- `challenges` and `challenge_admin`: student challenge participation vs trainer assignment.
- `fightcamp` and `fightcamp_admin`: student camp view vs trainer management.
- `book`: same route intentionally renders different trainer/student screen.
- `settings`: same route intentionally renders trainer operations or student privacy by role.
- `classes`: same route intentionally renders trainer manager or student booking screen.
- `media`: same route intentionally renders role-aware media experience.
- `notifications`: role-aware publishing/inbox behavior.
- `community` and `groups`: public/member feed and group spaces are separate concepts.

## Legacy source-shell candidates for later archival review

The repository contains older application generations from before V21. Examples include:

- `V09App.kt`
- `V10ControlApp.kt`
- `V16Core.kt`
- `V17Shell.kt`
- `V18App.kt`
- `FinalApp.kt`

These are **not automatically safe to delete** because older files can still contain reusable functions referenced by the active V21 application.

Professional cleanup procedure before any deletion:
1. map every symbol referenced from each candidate file;
2. check references from V21+ production files;
3. move any still-used utility/component into a current module;
4. compile full Android app;
5. only then request explicit deletion approval.

## Approval options for a future cleanup

No action has been taken. When the owner is ready, approval can be given separately for:

- A. Remove only obsolete route aliases after confirming no saved/deep links use them.
- B. Archive/remove old inactive application shells while preserving referenced utilities.
- C. Keep all compatibility code until after first Play Store production release.

Current recommendation for v0.105: **Option C — keep compatibility code until after the first stable production release.**
