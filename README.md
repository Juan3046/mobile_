# RM Games

Short daily sensor-driven mobile games (Android native, Kotlin, portrait only).

- 1 daily game, 30–90s, 2 attempts/day (best counts).
- 7-day seasons + historical leaderboard + Seasons History.
- Minigames 01–02: Escape Poruba, Onion Roll (see `docs/games/`).

## Docs (source of truth)

- `AGENTS.md` — project context + mandatory docs-sync rule.
- `docs/README.md` — docs index.
- `docs/01-product-overview.md` … `docs/06-out-of-scope-roadmap.md`.
- `docs/games/` — one `.md` per minigame (index: `docs/games/README.md`).

Update the relevant `.md` in the same change whenever behavior, screens,
rules, stack, or workflow changes. English everywhere.

## How to Build

Requires **JDK 17**. Recent Android Studio bundles JBR (Java 25), which breaks
Kotlin 2.0.21 (`IllegalArgumentException: 25.0.3`) — point Gradle at a JDK 17:

- **Android Studio**: Settings → Build, Execution, Deployment → Build Tools → Gradle → **Gradle JDK → 17** (download one if no JDK 17 is listed). Do not keep the embedded JBR if it reports Java 23+.
- **Terminal**: export `JAVA_HOME` to a JDK 17 (e.g. Temurin 17) before running Gradle.

```sh
./gradlew testDebugUnitTest   # full unit suite — run locally before every PR (no CI)
```

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

Phase 0 + Phase 1a done: multi-module skeleton (12 modules), frozen contracts
(`Game`/`GameScope`, `:core:sensors`, `:core:vision`) and the game hosting scaffolding
(`GameEntry`/`GameRegistry`, session host, registry-driven nav), 64 unit tests.
Next: Phase 1b (parallel game build) — games are unblocked, per
`docs/08-implementation-plan.md`.
No CI — run `./gradlew testDebugUnitTest` locally before opening a PR.
