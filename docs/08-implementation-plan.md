# 08 — Parallel Implementation Plan (4 People, Multi-Module)

## Module Map (each person owns whole modules — no shared files)

```text
Games (one owner each — Phase 1b):
:feature:game:escape        P1 — Game 01 (touch drag, no sensors)
:feature:game:onion         P2 — Game 02 (gyroscope)
:feature:game:blink         P3 — Game 03 (front camera → :core:vision)
:feature:game:compass       P4 — Game 04 (magnetometer)
:feature:game:soda          P4 — Game 05 (accelerometer)

Shared (frozen in Phase 1a — built together, no cross-module edits afterwards):
:app                        shell: MainActivity, NavHost, DI, navigation routes,
                             camera-permission UX
:core:common                Game/GameScope contracts, theme, reusable UI, utils
:core:data                  Room + DataStore (local-first cache, offline) + Firestore
                             (shared scoreboard, Spark free plan): attempts (local),
                             daily bests (upload 1 write/day/UID), seasons
                             (deterministic 7-day windows, auto-archive), history,
                             DailyGameSelector (`daysSinceEpoch % gameCount`),
                             identity (Anonymous Auth UID + chosen nick).
                             Security rules: each UID writes only its own score
                             doc; ranking reads paginated (top 50).
:core:sensors               sensor interfaces (gyro, accelerometer, compass),
                             Android adapters, fakes, pure logic (shake detection,
                             heading filter), SensorAvailability
:core:vision                blink contract (EyeState/BlinkDetector) + fakes +
                             ML Kit adapter (CameraX + ML Kit face detection)
:feature:home               main screen (title, thumbnail, how-to, Play,
                             Seasons History button, attempts indicator, debug reset)
:feature:session            game host: timer, 1 run = 1 attempt, best-of-2,
                             lock + countdown, result screen
```

Ownership: **P1 Escape · P2 Onion · P3 Blink · P4 Compass + Soda.** Shared modules
are frozen after Phase 1a (built together); Phase 2 integration owners are assigned
when Phase 2 starts. Games 06–07 (still undefined) go to whoever finishes first.
One reviewer per PR.

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
  val hostScope: CoroutineScope // owned by :feature:session; cancelled on stop
  fun onScore(value: Long)  // raw game score (centiseconds or points) — daily best = max;
                            // it ranks the day only, season points come from position
  fun onFinished(win: Boolean)
}
```

## Game Hosting Contract (frozen before Phase 1b, lives in `:core:common`)

Lets the four developers work in parallel without touching shared navigation —
each `:feature:game:*` module exposes exactly one `GameEntry`, and `:app` lists all
of them once:

```kotlin
class GameEntry(                    // Adapter: logic + UI behind one stable type
  val game: Game,
  val screen: @Composable (GameScope) -> Unit, // same scope the host passed to start()
)
class GameRegistry(entries: List<GameEntry>) {  // Registry, built once in :app
  val all: List<GameEntry>
  fun find(gameId: String): GameEntry?   // resolves session/{gameId}; null = not found
  // duplicate Game.id at construction = IllegalArgumentException
}
```

- `:app` builds the `GameRegistry` from the five entries and the NavHost resolves
  `session/{gameId}` through `find(...)` — **no route hardcoded per game**.
- The per-game entry factories are **frozen** (each module owns its own):

```kotlin
fun escapePorubaEntry(): GameEntry                                  // touch-only
fun onionRollEntry(gyroscope: GyroscopeSensor): GameEntry
fun blinkSniperEntry(detector: BlinkDetector): GameEntry
fun compassChaosEntry(compass: CompassSensor): GameEntry
fun sodaShakeEntry(accelerometer: AccelerometerSensor): GameEntry
```

- `:feature:home` launcher (Phase 1b placeholder): `HomeScreen(games, onPlay)` lists the
  registered games and emits `onPlay(gameId)` — data down / events up, no NavHost inside
  home. The real daily home replaces the list in Phase 2.
- `:feature:session` provides `SessionGameScope`, the concrete `GameScope` for one run:
  `startRun()` starts the game once; `onScore` is forwarded only while the run is live;
  the **first** `onFinished` wins — the game is stopped once and `hostScope` is
  cancelled (sensor/camera listeners cannot leak); leaving the screen does the same
  cleanup. Games must not suspend after calling `onFinished`. The host renders
  `entry.screen(scope)`; timer/attempts/result UI is Phase 2.
- Game authors edit only their own module (game impl + screen + entry factory);
  `:app` and `:feature:session` are stable during Phase 1b.

## Shared Hardware Contracts (frozen in Phase 1a)

`:core:sensors` — games see Flows, never `SensorManager`. Adapter (Android impl),
Observer (Flow), Strategy (pure logic), DI (interfaces + fakes):

```kotlin
data class Vector3(val x: Float, val y: Float, val z: Float)
interface GyroscopeSensor     { fun readings(): Flow<Vector3> }  // rad/s
interface AccelerometerSensor { fun readings(): Flow<Vector3> }  // m/s²
interface CompassSensor       { fun headings(): Flow<Float> }    // degrees 0–360, smoothed
// Pure logic (JUnit-testable, no Android): ShakeDetector, HeadingFilter
// Fakes in the module for game tests; Android adapters via callbackFlow + awaitClose
```

`:core:vision` — camera pipeline behind one interface; ML Kit never leaks out:

```kotlin
enum class EyeState { OPEN, CLOSED }
interface BlinkDetector { fun eyeState(): Flow<EyeState> }
// MlKitBlinkDetector (CameraX + ML Kit face detection) is the only ML Kit consumer.
// Camera permission UX lives in :app, not here.
```

Repositories in `:core:data`: `AttemptRepository` (2/day, local-midnight reset),
`SeasonRepository` (7-day sum of daily bests, auto-archive), `HistoryRepository`.
Navigation: `home → session(gameId) → result`; exhausted attempts → lock + countdown.
Season points come only from daily positions (`(N−p+1)×25`, ties share, noon-to-noon
days, always live, no locks) — raw game scores (cs vs points) are never mixed or summed.

## Phases & Order

- **Phase 0 — Contracts (together, short).** Freeze the `Game` interface, repos,
  nav routes, minSdk 31. No game logic. Exit: contracts
  merged to `main`, everyone rebased. (A build+lint CI was created here, later
  removed by decision — see `04-tech-stack.md`.)
- **Phase 1a — Shared hardware contracts (together, short).** Freeze
  `:core:sensors` interfaces + fakes + pure logic, create `:core:vision` with the
  blink contract, extend `GameScope` with `hostScope`. No game logic. Exit:
  contracts merged to `main`, everyone rebased — games are unblocked.
- **Phase 1b — Parallel build.** Each person builds only their game module against
  the frozen contracts (sensors/vision via interfaces + fakes, plus `GameEntry` +
  `GameRegistry` + the `:feature:session` host). Games ship with procedural
  placeholders (no external assets). Exit: each game compiles standalone
  + own screen runs.
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
- Squash-merge, one reviewer. No CI (workflow removed) — the PR author runs
  `./gradlew testDebugUnitTest lintDebug` locally and confirms green in the PR
  description before requesting review.
- Unit tests only for now (fast); emulator instrumented tests come later for
  critical flows only. TDD stays mandatory (AGENTS.md §5) — just no automation.
- Each PR that changes behavior updates the matching `docs/` file in the same PR.

## AI Agent Rules

- Read `AGENTS.md` + your module's doc before coding; stay inside your modules.
- Docs-sync is mandatory (§4 of `AGENTS.md`): stale docs = bug.

## Definition of Done (per module)

Compiles via `:module:assembleDebug` · **tests written first (TDD) and green
(`:module:testDebugUnitTest`)** · portrait locked · attempts/timer rules
respected · missing-sensor blocks with message, no fake scores · docs updated.
