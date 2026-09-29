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
- **GitHub** for version control (repo to be connected later).
- **Entire project in English**, including code, comments, branch names, and docs.

See `docs/04-tech-stack.md`.

## 3. Team & Workflow Context

- Team of **4 developers + AI coding agents**.
- Work in **independent modules** to avoid merge conflicts; a parallel development plan
  will be defined once requirements are frozen (see `docs/05-team-workflow.md`).
- GitHub flow: small PRs per module, English names, no direct pushes to `main` once the repo exists.
- **Do not implement code yet.** Current phase is requirements gathering only.

## 4. Mandatory Rule: Keep Docs in Sync

**In every development phase, constantly align any change with the app documentation:**

1. Before coding, read the relevant file(s) in `docs/` + this file.
2. If a change affects behavior, screens, rules, stack, or workflow, **update the corresponding `.md` first or in the same change**.
3. Keep changes small and traceable: one logical doc update per change.
4. Never leave docs stale: if code and docs disagree, treat it as a bug and fix the docs (or flag it to the user).
5. Use English in all docs and code.

Docs index: `docs/README.md`.
