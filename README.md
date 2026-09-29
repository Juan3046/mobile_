# RM Games

Short daily sensor-driven mobile games (Android native, Kotlin, portrait only).

- 1 daily game, 30–90s, 2 attempts/day (best counts).
- 7-day seasons + historical leaderboard + Seasons History.
- Minigame 01: Escape Poruba.

## Docs (source of truth)

- `AGENTS.md` — project context + mandatory docs-sync rule.
- `docs/README.md` — docs index.
- `docs/01-product-overview.md` … `docs/07-minigame-escape-poruba.md`.

Update the relevant `.md` in the same change whenever behavior, screens,
rules, stack, or workflow changes. English everywhere.

## How to Collaborate (4 + AI agents)

1. Clone `main`, never push directly to it.
2. Create a branch per module: `feature/<module-name>` (e.g. `feature/daily-game-screen`).
3. Keep PRs small, one module each, in English.
4. Before coding, read `AGENTS.md` + the relevant `docs/` file.
5. After merge, pull `main` before starting the next branch.

```sh
git clone https://github.com/Juan3046/mobile_.git
git checkout -b feature/my-module
# work, then:
git add <files>
git commit -m "feat(scope): what changed"
git push -u origin feature/my-module
# open a PR to main on GitHub
```

## Status

Requirements phase — no app implementation yet. Parallel implementation plan: pending.
