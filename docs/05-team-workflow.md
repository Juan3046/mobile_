# 05 — Team Workflow (4 People + AI Agents)

## Team
- 4 developers working with AI coding agents.
- Requirements phase first; **no implementation until requirements are frozen**.

## Version Control (GitHub)
- Repo: `https://github.com/Juan3046/mobile_.git` (connected, branch `main`).
- Flow: feature-per-module branches, small PRs, English names (e.g., `feature/daily-game-screen`).
- Tooling: GitHub CLI (`gh`) is installed and authenticated; use it to push branches and create/merge PRs (no direct pushes to `main`).
- No direct pushes to `main` (rule to enforce once repo exists).

## Parallel Development Plan
- **Status: Phase 1a** — new ownership: P1 Escape · P2 Onion · P3 Blink · P4 Compass + Soda. Shared hardware layers (`:core:sensors`, `:core:vision`) are frozen together, then games run in parallel (Phase 1b). See `08-implementation-plan.md`.

## Agent Rules
- Keep `docs/` and `AGENTS.md` in sync with every change (see `AGENTS.md` §4).
- Prefer editing existing files over creating new ones; keep changes small.
