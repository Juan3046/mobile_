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
- **Any shake counts** — up/down, side to side, front/back or mixed (playtest 2026-11-20:
  the first tuning required alternating direction on the dominant axis with gravity
  included, which dropped up/down shakes and felt broken).
  Detection in `SodaShakeEngine` (tune by playtest): gravity is removed with a low-pass
  estimate (tilting never counts); the remaining hand movement must exceed `7 m/s²`
  (≈ 0.7 g), calm down below `4 m/s²` before the next peak counts (hysteresis — one
  jolt counts once), and counted shakes are at least `90 ms` apart. Every sensor reading
  is checked (not one per frame), so fast shakes are not missed.
  If the movement stays at shake level for 400 ms without a pause (impossible for a real
  back-and-forth shake), the gravity reference is re-seeded: a run that starts mid-shake
  recovers in under half a second instead of missing its first shakes.
- **Feedback**: every counted shake gives a short, light vibration; the pop a longer
  one. Honours Settings → Vibration. The buzz is far below the shake threshold, so it
  never counts as a shake.
- The run clock is a stopwatch from the start and freezes at the pop; a run that
  never pops never finishes, so an idle run records nothing.

## Scoring
- **Score = time to pop in centiseconds — lower wins** (`higherWins = false`). One run = one attempt (global 2-attempts/day, best = fastest); the daily best ranks the day for position points (never summed).

## Fallback (missing sensor / permission denied)
- Per global rule: **disable Play with a clear message** ("Soda Shake requires an accelerometer") and never record fake scores.
- Phase 1b: if the accelerometer flow fails, the run is **blocked in-game** —
  the screen shows the message, the score is zeroed and nothing is reported or
  recorded (a blocked run never calls `onFinished`). Home-level Play disabling is
  done (Phase 3, docs/03 §3.1 "Missing sensor"): a phone without the sensor never starts a run.

## Persistence (local-first MVP)
- Best (fastest) pop time, attempts used today; daily best uploads once for the shared ranking.

## Difficulty Curve
- Flat by design: every run needs the same 20 valid shakes. Difficulty is pure physical speed — no rounds, no drain, no scaling. The pressure gauge fills 5% per valid shake.

## Open Questions
- [x] Decided: single short round ending at 20 shakes (duration exception); score = time-to-pop cs, lower wins; block + message fallback; local-first persistence.
- [x] Re-tuned after playtest (2026-11-20): any shake counts, gravity removed, vibration per shake.

## Implementation (Phase 1b)
- `feature/game/soda`: `SodaShakeEngine` (pure rules, JVM unit tests),
  `SodaShakeGame` (16 ms loop on `hostScope`, accelerometer → engine, stopwatch
  centiseconds, `onFinished(true)` after the pop animation, sensor failure →
  blocked run), `SodaShakeScreen` (Compose rendering: can + pressure gauge +
  blocked message + debug key).
- Tests: `SodaShakeEngineTest` + `SodaShakeGameTest`.
- Debug-only affordance (emulator/desktop playtesting, gated on `BuildConfig.DEBUG`):
  **Space/Enter counts one shake** — the emulator cannot shake for real.