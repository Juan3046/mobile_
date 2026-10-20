# 04 — Tech Stack

- **IDE**: Android Studio.
- **Language**: Kotlin (native Android).
- **Version control**: GitHub (`https://github.com/Juan3046/mobile_.git`, branch `main`).
- **GitHub tooling**: GitHub CLI (`gh`) for branch/PR workflow — create branches, open and merge PRs from the terminal.
- **Language of project**: English for code, comments, branches, and docs.
- **Storage (MVP)**: local-first (Room/DataStore cache, offline by default) + **Firebase Spark plan (free, no billing)** for the shared scoreboard.
- **Backend (free only)**: Firestore (50K reads / 20K writes per day quota) + Anonymous Auth. No Cloud Functions, no phone auth (both require Blaze) — seasons and daily game are resolved deterministically on-device.
- **Testing (TDD mandatory)**: JUnit + MockK for unit tests, Espresso for UI tests. No CI — tests run locally (`:module:testDebugUnitTest`) and must be green before opening a PR.

## Constraints
- Native Android only (no cross-platform in MVP).
- Portrait orientation locked.
- Sensor APIs: CameraX (blink detection via ML Kit face detection), gyroscope, accelerometer, magnetometer (per minigame).
- Min SDK / target SDK: minSdk API 31 (Android 12) per user decision; targetSdk TBD (latest stable at implementation time).

## Open Questions
- [x] Decided: local-only storage (Room/DataStore); minSdk API 31 (Android 12); **no CI** — GitHub Actions workflow removed (user decision); tests are run manually before each PR.
- [ ] Target SDK / compile SDK at implementation time?
