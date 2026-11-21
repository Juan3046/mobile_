# AGENTS.md — RM Games

## 1. Project Context (Brief)

**RM Games** is a mobile app (Android native) offering **one short daily game at a time**.
Games are short (30–90s; documented exceptions in `docs/02-gameplay-rules.md`), portrait-only, with intuitive rules, controlled mainly via
mobile sensors: **camera, gyroscope, accelerometer, magnetometer, microphone** (touch only as a
documented exception).

Key product rules:
- 2 attempts per day per player, with visible remaining-attempts feedback.
- Historical leaderboard + seasons. Each season lasts **7 days**.
- Main screen: game title, thumbnail, short how-to-play, Play button.
- Debug/test-only reset button for attempts (to ease testing).
- MVP excludes: friend-group multiplayer and per-minigame music (see `docs/06-out-of-scope-roadmap.md`).

Full requirements live in `docs/`. That documentation is the source of truth.

## 2. Tech Stack

- **Android Studio + Kotlin** (native Android, portrait only).
- **GitHub** for version control (repo: `https://github.com/Juan3046/mobile_.git`, branch `main`).
- **Entire project in English**, including code, comments, branch names, and docs.

See `docs/04-tech-stack.md`.

## 3. Team & Workflow Context

- Team of **4 developers + AI coding agents**.
- Work in **independent modules** to avoid merge conflicts; the parallel development
  plan lives in `docs/08-implementation-plan.md` (module map, ownership P1–P4, phases).
- GitHub flow: small PRs per module, English names, no direct pushes to `main` once the repo exists.
- **Current phase: Phase 3 — Polish** (since 2026-11-21). Phases 0–2 are merged: the
  frozen `Game`/`GameScope` contracts, shared hardware (`:core:sensors`, `:core:vision`),
  the hosting scaffolding (`GameEntry`/`GameRegistry`, `SessionGameScope`, registry-driven
  nav), the Phase 2 app shell (Main, session/result, Settings, Seasons history, nick
  onboarding, `RmTheme` + UI kit, `:core:data`, `:core:audio`, app chrome — tracked in
  `docs/09-phase2-shell-plan.md`) and the online layer (`:core:online`: Firebase nicks +
  daily ranking).
- **Seven playable games** (01 Poruba Casino, 02 Onion Roll, 03 Blink Sniper, 04 Compass
  Chaos, 05 Soda Shake, 06 Class Rush, 07 Sound Barrier) — one per day, each played once per
  7-day season. Specs in `docs/games/`; ownership in `docs/08-implementation-plan.md`.
- **Phase 3 work**: device playtesting and tuning, performance (camera games), release APK
  (R8, one stable signing key — no Google Play, APKs are shared directly; see
  `docs/04-tech-stack.md`), and small polish from UI/code reviews. New features stay small
  and must not break the daily loop or the shared ranking.
- **Ownership still applies**: game work happens only inside your own game module; the shell
  (`:app`, `:feature:home|session|settings|ranking|onboarding`, `:core:common` theme/UI kit,
  `:core:data`, `:core:audio`, `:core:online`) is owned by **P2**. Build screens with
  `RmTheme` + the UI kit (visual spec: `design-system/`, Direction A "Chunky Cabinet").
  Touch controls are allowed only as a documented exception (`docs/02-gameplay-rules.md`).
- **Adding or removing a game reshuffles the current season's daily order**: ship it to
  everyone at the same time, ideally when a new season starts (Monday 12:00).

## 4. Mandatory Rule: Keep Docs in Sync
**In every development phase, constantly align any change with the app documentation:**

1. Before coding, read the relevant file(s) in `docs/` + this file.
2. If a change affects behavior, screens, rules, stack, or workflow, **update the corresponding `.md` first or in the same change**.
3. Keep changes small and traceable: one logical doc update per change.
4. Never leave docs stale: if code and docs disagree, treat it as a bug and fix the docs (or flag it to the user).
5. Use English in all docs and code.

## 5. Mandatory Rule: TDD — Test Every Change

**Every code change ships with tests. No untested logic is merged.**

- Red → green → refactor for all new logic (repos, selectors, scoring, difficulty ramps, sensor wrappers).
- Unit tests mandatory (JUnit + MockK); UI/instrumented tests where the module needs them (Espresso).
- Run the full test suite locally (`./gradlew testDebugUnitTest`, JDK 17 — see "How to Build" in `README.md`) before opening a PR; red tests block the PR.
- Bug fixes include a failing-then-passing regression test.

Docs index: `docs/README.md`.
