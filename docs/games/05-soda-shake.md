# 05 — Minigame 05: SODA SHAKE

## Concept
A physical speed test. The player shakes the phone as fast as possible to build pressure inside a virtual soda can. **The can pops at 20 shakes.** There is no time limit — every run ends in a pop.

> **Approved duration exception:** short variable-length single round (ends at 20 shakes, typically seconds), outside the standard 30–90s rule (see `docs/02-gameplay-rules.md`).

## Format & Camera
- Portrait only, 2D cartoon UI.
- Features a soda can and a highly visible pressure gauge.

## Controls
- **Accelerometer only**.
- Device shaking (detecting high-force peaks on X, Y, and Z axes).

## Rules
- Shake detection counts high-force peaks on X, Y, Z axes; each valid peak = 1 shake.
- The can pops at **20 shakes**, ending the run. No time limit, no fail state (giving up forfeits the attempt per global no-pause rule).
- Cheat guard (implementation): peaks must alternate direction / exceed a dead time so tiny vibrations don't count (tune in playtest).
  - Initial tuning in `SodaShakeEngine` (confirm by playtest): magnitude peak above
    `15 m/s²` (≈ 0.5 g over gravity), must settle back below `11 m/s²` before the
    next peak counts (hysteresis — a held reading counts once), minimum `120 ms`
    between counted shakes, and consecutive counted peaks must alternate the sign
    of the dominant axis (one-way tapping does not count).
- The run clock is a stopwatch from the start and freezes at the pop; a run that
  never pops never finishes, so an idle run records nothing.

## Scoring
- **Score = time to pop in centiseconds — lower wins** (`higherWins = false`). One run = one attempt (global 2-attempts/day, best = fastest); the daily best ranks the day for position points (never summed).

## Fallback (missing sensor / permission denied)
- Per global rule: **disable Play with a clear message** ("Soda Shake requires an accelerometer") and never record fake scores.
- Phase 1b: if the accelerometer flow fails, the run is **blocked in-game** —
  the screen shows the message, the score is zeroed and nothing is reported or
  recorded (a blocked run never calls `onFinished`). Home-level Play disabling
  lands in Phase 2.

## Persistence (local-first MVP)
- Best (fastest) pop time, attempts used today; daily best uploads once for the shared ranking.

## Difficulty Curve
- Flat by design: every run needs the same 20 valid shakes. Difficulty is pure physical speed — no rounds, no drain, no scaling. The pressure gauge fills 5% per valid shake.

## Open Questions
- [x] Decided: single short round ending at 20 shakes (duration exception); score = time-to-pop cs, lower wins; block + message fallback; local-first persistence.
- [ ] Playtest caveat: the cheat-guard tuning above (thresholds, 120 ms dead time,
  direction alternation) is a first pass — confirm it feels fair for real shaking
  and adjust in `SodaShakeEngine` only.

## Implementation (Phase 1b)
- `feature/game/soda`: `SodaShakeEngine` (pure rules, JVM unit tests),
  `SodaShakeGame` (16 ms loop on `hostScope`, accelerometer → engine, stopwatch
  centiseconds, `onFinished(true)` after the pop animation, sensor failure →
  blocked run), `SodaShakeScreen` (Compose rendering: can + pressure gauge +
  blocked message + debug key).
- Tests: `SodaShakeEngineTest` + `SodaShakeGameTest`.
- Debug-only affordance (emulator/desktop playtesting, gated on `BuildConfig.DEBUG`):
  **Space/Enter counts one shake** — the emulator cannot shake for real.