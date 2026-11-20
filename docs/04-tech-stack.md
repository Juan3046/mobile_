# 04 — Tech Stack

- **IDE**: Android Studio.
- **Language**: Kotlin (native Android).
- **Version control**: GitHub (`https://github.com/Juan3046/mobile_.git`, branch `main`).
- **GitHub tooling**: GitHub CLI (`gh`) for branch/PR workflow — create branches, open and merge PRs from the terminal.
- **Language of project**: English for code, comments, branches, and docs.
- **Storage (MVP)**: local-first (Room/DataStore cache, offline by default) + **Firebase Spark plan (free, no billing)** for the shared scoreboard.
- **Backend (free only)**: Firestore (50K reads / 20K writes per day quota) + Anonymous Auth. No Cloud Functions, no phone auth (both require Blaze) — seasons and daily game are resolved deterministically on-device.
- **UI**: Jetpack Compose + Material 3, themed by `RmTheme` in `:core:common` (`com.rmgames.core.common.theme`) — Direction A "Chunky Cabinet", dark-first with a light variant; spec in `design-system/`. Screens use `RmTheme.colors / dimens / motion` and `MaterialTheme` roles, never raw colour or size literals, and build from the UI kit in `com.rmgames.core.common.ui` (`RmButton`, `GameCard`, `AttemptsPips`, …).
- **App chrome** (`:app`): adaptive launcher icon (vector layers + monochrome), AndroidX SplashScreen (`androidx.core:core-splashscreen`) kept until saved data loads, edge-to-edge (`enableEdgeToEdge` + safe-drawing insets on the root surface).
- **Fonts**: Bungee, Rubik and JetBrains Mono, bundled in `:core:common` `res/font` under the SIL OFL 1.1 (see `CREDITS.md`).
- **Testing (TDD mandatory)**: JUnit + MockK for unit tests, Espresso for UI tests. No CI — tests run locally (`:module:testDebugUnitTest`) and must be green before opening a PR.

## Data layer (local now, cloud-ready)
- Repositories (`:core:data`): `AttemptRepository`, `ScoreRepository`, `SettingsRepository`. Screens call them synchronously; the state lives in memory (instant, offline-first) and every change is written through in the background.
- **Where data lives is one seam: `SnapshotStore<T>`** (`read()` / `write()` of the whole snapshot). Today each repository uses `DataStoreSnapshotStore` (one entry in the app's Preferences DataStore, `rm_games.preferences_pb`), encoded by a versioned `SnapshotCodec` (`v1|…`; unreadable data → defaults, never a crash).
- **Moving something to the cloud** = implement `SnapshotStore<T>` on the remote API (e.g. a Firestore document per player), or a store that writes local + remote and reads the newest, and pass it to the repository in `AppContainer` (`:app`). Screens, rules and tests stay the same; the codecs can be reused for the remote payload.
- Attempts stay local by product rule (docs/02); daily bests (→ shared ranking, stream E) and settings are the cloud candidates. Daily bests keep the full history in the same DataStore entry (no Room — plan 09 PR 10 decision), and personal bests are computed from it.
- Storage failures are logged and never block play: the in-memory state stays authoritative and the next write retries.

## Firebase (online features)
- Project on the **Spark plan** with **Anonymous Auth** and **Firestore**. Adapters live in `:core:online` (`OnlineNickRegistry` over `AnonymousAuth` + `NickDirectory`, tested with fakes; thin Firebase classes).
- **Config is per developer**: download `google-services.json` for the Android app `com.rmgames` and put it at `app/google-services.json` (git-ignored). With it, the build applies the google-services plugin and `AppContainer` uses Firebase; without it the app builds and runs offline with the local stand-ins.
- **Security rules** are versioned in `firestore.rules` (deploy: console → Firestore → Rules, or `firebase deploy --only firestore:rules` with `firebase.json`). Nicks: created once by their owner under the lowercase key, never updated or deleted, at most one per account (`users/{uid}` names it; written in the same transaction, checked with `getAfter`); everything else denied by default.

## Constraints
- Native Android only (no cross-platform in MVP).
- Portrait orientation locked.
- Sensor APIs: CameraX (blink detection via ML Kit face detection), gyroscope, accelerometer, magnetometer (per minigame).
- Min SDK / target SDK: minSdk API 31 (Android 12) per user decision; targetSdk 34 (set in `:app`; bump to the latest stable before release).

## Open Questions
- [x] Decided: local-only storage (Room/DataStore); minSdk API 31 (Android 12); **no CI** — GitHub Actions workflow removed (user decision); tests are run manually before each PR.
- [ ] Target SDK / compile SDK at implementation time?
