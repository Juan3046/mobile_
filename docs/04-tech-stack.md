# 04 — Tech Stack

- **IDE**: Android Studio.
- **Language**: Kotlin (native Android).
- **Version control**: GitHub (repo to be connected later; agent will then push changes).
- **Language of project**: English for code, comments, branches, and docs.
- **Storage (MVP)**: local-only — Room/DataStore on device for attempts, daily bests, seasons, history. No server; offline works by default.

## Constraints
- Native Android only (no cross-platform in MVP).
- Portrait orientation locked.
- Sensor APIs: CameraX / Camera2, gyroscope, accelerometer (exact choice per minigame, TBD).
- Min SDK / target SDK: minSdk API 31 (Android 12) per user decision; targetSdk TBD (latest stable at implementation time).

## Open Questions
- [x] Decided: local-only storage (Room/DataStore); minSdk API 31 (Android 12); basic GitHub Actions CI (build + lint on PR).
- [ ] Target SDK / compile SDK at implementation time?
