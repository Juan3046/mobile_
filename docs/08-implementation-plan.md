# 08 — Parallel Implementation Plan (4 People, Multi-Module)

## Module Map (each person owns whole modules — no shared files)

```text
:app                        P1 — shell: MainActivity, NavHost, Hilt DI, navigation routes
:core:common                P1 — theme, reusable UI, utils, Game/GameResult contracts
:core:data                  P1 — Room + DataStore (local-first cache, offline) + Firestore
                             (shared scoreboard, Spark free plan): attempts (local),
                             daily bests (upload 1 write/day/UID), seasons
                             (deterministic 7-day windows, auto-archive), history,
                             DailyGameSelector (`daysSinceEpoch % gameCount`),
                             identity (Anonymous Auth UID + chosen nick).
                             Security rules: each UID writes only its own score
                             doc; ranking reads paginated (top 50).
:core:sensors               P4 — wrappers: gyro, accelerometer shake, magnetometer
                             compass, camera blink (CameraX + ML Kit),
                             common SensorAvailability API
:feature:home               P2 — main screen (title, thumbnail, how-to, Play,
                             Seasons History button, attempts indicator, debug reset)
:feature:session            P2 — game host: timer, 1 run = 1 attempt, best-of-2,
                             lock + countdown, result screen
:feature:game:escape        P3 — Game 01 (touch drag)
:feature:game:onion         P3 — Game 02 (gyroscope)
:feature:game:blink         P4 — Game 03 (front camera)
:feature:game:compass       P4 — Game 04 (magnetometer)
:feature:game:soda          P4 — Game 05 (accelerometer)
```

Ownership: P1 core · P2 flows · P3 games 01–02 · P4 sensors + games 03–05.
Games 06–07 (still undefined) go to whoever finishes first. One reviewer per PR.

## Contract (frozen in Phase 0, lives in `:core:common`)

```kotlin
interface Game {
  val id: String          // "01-escape-poruba"
  val title: String
  val description: String // how-to-play
  val durationMs: Long    // standard 30_000–90_000 (documented exceptions only)
  val higherWins: Boolean // false for lowest-wins games (e.g. 05 soda time-to-pop)
  fun start(scope: GameScope)
  fun stop()
}
interface GameScope {
  fun onScore(value: Long)  // raw game score (centiseconds or points) — daily best = max;
                            // it ranks the day only, season points come from position
  fun onFinished(win: Boolean)
}
```

Repositories in `:core:data`: `AttemptRepository` (2/day, local-midnight reset),
`SeasonRepository` (7-day sum of daily bests, auto-archive), `HistoryRepository`.
Navigation: `home → session(gameId) → result`; exhausted attempts → lock + countdown.
Season points come only from daily positions (`(N−p+1)×25`, ties share, noon-to-noon
days, always live, no locks) — raw game scores (cs vs points) are never mixed or summed.

## Phases & Order

- **Phase 0 — Contracts (together, short).** Freeze the `Game` interface, repos,
  nav routes, minSdk 31, CI (build + lint on PR). No game logic. Exit: contracts
  merged to `main`, everyone rebased.
- **Phase 1 — Parallel build.** Each person builds only their modules against the
  contracts, using fakes for the rest. Games ship with procedural placeholders
  (no external assets). Exit: each module compiles standalone + own screen runs.
- **Phase 2 — Integration.** Wire daily selector → session → results → seasons
  rollover → history. Playtest tuning (ramps, thresholds, fairness check for 01).
  Exit: full daily loop works offline, 2 attempts enforced.
- **Phase 3 — Polish.** Debug reset removal plan, missing-sensor messages,
  performance, release build.

## Branch & PR Rules (no stepping on each other)

- Never push to `main`. Branch per module: `feature/<module>-<what>`
  (e.g. `feature/game-onion-ramp`).
- One module per PR, small PRs, English. Rebase onto `main` before opening.
- No cross-module edits without the owner's OK in the PR.
- Squash-merge, one reviewer, CI green required.
- Each PR that changes behavior updates the matching `docs/` file in the same PR.

## AI Agent Rules

- Read `AGENTS.md` + your module's doc before coding; stay inside your modules.
- Docs-sync is mandatory (§4 of `AGENTS.md`): stale docs = bug.

## Definition of Done (per module)

Compiles via `:module:assembleDebug` · **tests written first (TDD) and green
(`:module:testDebugUnitTest`)** · portrait locked · attempts/timer rules
respected · missing-sensor blocks with message, no fake scores · docs updated.
