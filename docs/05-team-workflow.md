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
- **Status: Phase 0 DONE** — Android skeleton (11 modules), frozen `Game` contract, local repos, 33 unit tests green, CI workflow. See `08-implementation-plan.md`. Team can now build Phase 1 in parallel.

## Agent Rules
- Keep `docs/` and `AGENTS.md` in sync with every change (see `AGENTS.md` §4).
- Prefer editing existing files over creating new ones; keep changes small.
