# AGENTS.md — RM Games

## 1. Project Context (Brief)

**RM Games** is a mobile app (Android native) offering **one short daily game at a time**.
Games are short (30–90s), portrait-only, with intuitive rules, controlled mainly via
mobile sensors: **camera, gyroscope, accelerometer**.

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
- **Current phase: Phase 1b (parallel game build)** — Phase 0 (frozen `Game`/`GameScope`)
  and Phase 1a (shared hardware in `:core:sensors` + `:core:vision`) are merged, plus the
  hosting scaffolding: `GameEntry`/`GameRegistry` contracts in `:core:common`, the
  `SessionGameScope` host in `:feature:session`, and registry-driven nav in `:app`
  (see "Game Hosting Contract" in `docs/08-implementation-plan.md`). Each developer
  owns one game module (ownership map in `docs/08-implementation-plan.md`), tests first
  (§5), placeholders until real gameplay lands. Game work happens **only inside your own
  module** — never edit `:app`, `:feature:session` or shared navigation. Re-read the
  plan before coding.

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
