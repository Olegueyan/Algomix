# Algomix Implementation Batches

This file turns the requirements specification and mockups into code batches that can be assigned to coding agents.
The current application already has a working native Android/OpenGL Rubik cube viewer; the batches below build the
DDD, persistence, cloud, scan, and full UI around it without replacing the existing renderer prematurely.

## Current Baseline

- Android native Views/XML with ViewBinding and Material theme.
- One Gradle module: `:app`.
- Existing code covers a solved 3x3 cube render model, OpenGL renderer, camera orientation, pinch zoom, and touch input.
- Missing implementation: notation domain, move execution, shared app state, local persistence, cloud sync, library,
  timer, settings, scan, PDF export, and the mockup screens.
- Verification blocker seen during planning: Gradle needs a Java 21 toolchain; this machine currently exposes a JRE 25.

## Cloud Data Contract

Supabase stores durable user data only. The current cube state and in-progress session are local cache data.

Remote tables:

- `profiles`: Supabase user id, first name, last name, display email, timestamps.
- `collections`: user-owned top-level containers with `owner_id`, `updated_at`, and `deleted_at`.
- `algorithm_sheets`: sheets inside collections.
- `algorithms`: ordered algorithms inside sheets with normalized move sequences.
- `scrambles`: named scramble sequences inside collections.
- `tags`: user-owned tag catalog.
- `sheet_tags` and `scramble_tags`: tag join tables.
- `timer_entries`: solve duration in milliseconds and solve date.
- `user_preferences`: durable preferences such as app appearance and cube theme.

Local-only data:

- Current cube state, active screen/mode, play index, editor undo/redo stacks, active timer state, scan draft, and camera
  framing unless later required for visual restore.

Sync rules:

- Supabase is an adapter behind DDD ports; Firebase or another provider must be swappable without rewriting domain or UI.
- Use Supabase Auth email/password.
- Use publishable keys only on-device; never ship secret or service role keys.
- RLS must enforce `owner_id = auth.uid()`.
- Local mutation succeeds locally first, then enters an outbox for background cloud sync.
- Conflicts use last-write-wins based on `updated_at` and tombstone-aware `deleted_at`.
- `Vider le cloud` deletes the remote dataset only, keeps local data, and marks existing local rows as not eligible for
  re-upload unless the user modifies them again.
- `Recuperer` merges remote and local data with the same LWW rules.

## Batch 1 - Technical Preparation

Goal: make the project ready for DDD, persistence, CameraX, and Supabase without changing the UI behavior yet.

Tasks:

- Install or configure a Java 21 JDK/toolchain.
- Add dependencies for lifecycle/viewmodel, coroutines, Room, DataStore, CameraX, WorkManager, Kotlin serialization,
  Supabase Auth/PostgREST, and Ktor Android.
- Add Android permissions for `INTERNET` and `CAMERA`.
- Keep `:app` as the only module.
- Do not migrate the current OpenGL renderer in this batch.

Acceptance:

- Existing unit tests still pass after Java 21 is available.
- The app manifest contains the permissions required by later cloud and scan batches.

## Batch 2 - DDD Foundation

Goal: define boundaries before feature implementation.

Tasks:

- Add shared `AppResult` and `AppError` types.
- Add application ports:
  `CubeSessionRepository`, `LibraryRepository`, `TimerRepository`, `SettingsRepository`,
  `CloudAuthGateway`, `CloudSyncGateway`, `PdfExporter`, and `CubeScanner`.
- Add a manual `AppContainer` placeholder for dependency assembly.
- Keep infrastructure implementations stubbed or absent until their dedicated batches.

Acceptance:

- Domain and application packages do not depend on Android UI classes.
- Ports expose domain types, not Supabase/Room DTOs.

## Batch 3 - Rubik Domain

Goal: add a testable Rubik domain that can later drive the renderer.

Tasks:

- Add `CubeState`, `FaceletCube`, `Move`, `MoveSequence`, `PlaybackState`, and `EditingSession`.
- Implement notation parsing for face turns, wide moves, slice moves, and cube rotations, including prime and double turns.
- Implement move execution for a 3x3 facelet cube.
- Implement scramble generation with no immediate same-axis repetitions.
- Implement cube validation: correct sticker counts and solved-state validation first; full solvability validation is a
  later hardening target inside Scan N1.

Acceptance:

- Unit tests cover parsing `R U R' U'`, wide move aliases (`Rw`/`r`), slice moves, rotations, invalid tokens, scramble
  length, and solved-state validation.

## Batch 4 - Local Persistence

Goal: persist durable data in Room and session data in DataStore.

Tasks:

- Add Room entities and DAOs for collections, sheets, algorithms, scrambles, tags, joins, timer entries, sync metadata,
  and an outbox.
- Add DataStore serializer/preferences for `UserPreferences` and `LocalSessionSnapshot`.
- Add local repository implementations behind ports.
- Add migrations from version 1 onward.

Acceptance:

- CRUD repository tests pass for library and timer data.
- Relaunch simulation restores local session data without cloud access.

## Batch 5 - Navigation And Shared App State

Goal: turn `MainActivity` into the app shell from the mockups.

Tasks:

- Add bottom navigation for Accueil, Bibliotheque, Timer, and Parametres.
- Add ViewModels for shared cube state, home, library, timer, and settings.
- Hoist the cube state out of `RubikCubeView`; the view should render a provided state and remain reusable.
- Keep the OpenGL camera/touch code intact.

Acceptance:

- Switching tabs preserves the shared cube state.
- Existing Rubik rendering tests remain green.

## Batch 6 - Home Screens

Goal: implement the four Accueil modes from the mockups.

References:

- `Blueprint/Assets/Mockups/Exports/home-visualization.png`
- `Blueprint/Assets/Mockups/Exports/home-free.png`
- `Blueprint/Assets/Mockups/Exports/home-play.png`
- `Blueprint/Assets/Mockups/Exports/home-edit.png`

Tasks:

- Add Visualisation, Libre, Play, and Edition mode controls.
- Add move keyboard grouped by Cube Rotations, Face Turns, Slice Moves, and Wide Moves.
- Add scan and scramble actions.
- Add load popup for algorithms and scrambles.
- Add Play controls: previous, next, speed, auto, loop, reset.
- Add Edition controls: save, undo, redo, suppress, delete all.

Acceptance:

- AC-02, AC-03, and AC-04 pass with ViewModel tests and manual UI checks.

## Batch 7 - Library And PDF

Goal: implement durable content organization.

References:

- `library-overview.png`
- `library-sheet-preview.png`
- `library-scramble-create.png`
- `popup-create-collection.png`
- `popup-save-edit.png`
- `popup-load-item.png`
- `popup-tags-manage.png`
- `popup-rename.png`
- `popup-delete-confirm.png`

Tasks:

- Add collection, sheet, algorithm, scramble, and tag CRUD.
- Add search, type filters, and tag filters.
- Add save-from-edition flow, including collection creation in the save dialog.
- Add algorithm code import with parser validation.
- Add local PDF export for a sheet and Android share intent.

Acceptance:

- AC-05 and AC-06 pass.
- Invalid imports leave the target sheet unchanged.

## Batch 8 - Timer

Goal: implement solve timing and history.

Reference:

- `timer-overview.png`

Tasks:

- Add timer start/pause/reset/save.
- Reject zero or invalid durations.
- Persist `TimerEntry` rows locally and enqueue cloud sync.

Acceptance:

- AC-07 and TST-03 pass.

## Batch 9 - Settings And Cloud UX

Goal: implement settings and cloud flows.

References:

- `settings-level-1.png`
- `settings-level-2.png`
- `settings-level-3.png`
- `popup-auth-login.png`
- `popup-create-account.png`
- `popup-auth-password.png`

Tasks:

- Add app appearance toggle and persistent cube theme choice.
- Implement Filled first, then Sticker sur noir and Carbone.
- Add profile display and password change flow.
- Add login, create account, recover, and purge cloud actions with explicit feedback.

Acceptance:

- AC-08 passes.
- Cloud operations without a valid session are refused with a visible message.

## Batch 10 - Supabase Sync

Goal: make cloud sync real behind existing ports.

Tasks:

- Add Supabase SQL migrations and RLS policies.
- Add Supabase DTOs/mappers inside infrastructure only.
- Implement `CloudAuthGateway` and `CloudSyncGateway`.
- Implement outbox processing through WorkManager.
- Implement LWW merge and remote-only purge.

Acceptance:

- Fake-cloud sync tests cover mutation upload, retrieval merge, conflict resolution, tombstones, and purge remote-only.
- Manual smoke test works against a real Supabase project.

## Batch 11 - Scan N1

Goal: capture and reconstruct a cube from the real camera with user correction.

Reference:

- `popup-scan-preview.png`

Tasks:

- Add CameraX preview with 3x3 grid overlay.
- Capture six faces in a guided order.
- Extract average cell colors and map them to the six cube colors.
- Add manual correction before validation.
- Validate sticker counts and cube consistency before applying to shared cube state.

Acceptance:

- AC-01 passes.
- Invalid or incomplete scans are blocked with explicit feedback.

## Batch 12 - Hardening

Goal: finish acceptance coverage.

Tasks:

- Run AC-01 through AC-10 and TST-01 through TST-04.
- Verify mockup parity on common phone sizes.
- Check accessibility labels for icon-only controls.
- Add empty, loading, offline, and error states.
- Confirm no UI operation applies destructive cloud changes without authentication and confirmation.

Acceptance:

- All local tests pass with Java 21.
- Manual smoke tests pass for render, scan, library, timer, settings, auth, sync, and PDF export.
