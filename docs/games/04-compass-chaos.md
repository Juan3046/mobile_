# 04 — Minigame 04: COMPASS CHAOS

## Concept
A fast-paced directional challenge. The player must physically spin their body and device to face a target cardinal direction (N, S, E, W) before the time runs out.

> **Approved duration exception:** fixed 20s round, outside the standard 30–90s rule (see `docs/02-gameplay-rules.md`).

## Format & Camera
- Portrait only, 2D minimalist UI.
- Central compass needle and a highlighted target zone.
- **Fixed 20s round.** Random directions are requested back-to-back until time expires.

## Controls
- **Magnetometer only**.
- No touch input during gameplay.

## Rules
- The game calls out a random direction (e.g., "POINT EAST!").
- Align the phone's top edge with that magnetic direction within the window to score; a missed window simply scores nothing and the next direction is called.
- Alignment (implementation): the heading must sit **strictly inside ±45°** of the
  called direction (a full cardinal quadrant; the exact boundary never scores, so
  two neighbouring quadrants cannot double-count one position). The window is
  sized when the direction is called and runs out with no pause — see Difficulty Curve.
- No game over — the run always lasts the full 20s. Most correct directions wins.

## Scoring
- **Score = correct directions in 20s** (higher is better). One run = one attempt (global 2-attempts/day, best of 2); the daily best ranks the day for position points (never summed).

## Fallback (missing sensor / permission denied)
- Per global rule: **disable Play with a clear message** ("Compass Chaos requires a magnetometer") and never record fake scores.
- Phase 1b: if the magnetometer flow fails, the run is **blocked in-game** —
  the screen shows the message, the score is zeroed and nothing is reported or
  recorded (a blocked run never calls `onFinished`). Home-level Play disabling
  lands in Phase 2.

## Persistence (local-first MVP)
- Best turns survived, attempts used today; daily best uploads once for the shared ranking.

## Difficulty Curve (time-based, fits the 20s round)
- 0–7s: 3.0s per direction, 90-degree turns.
- 7–14s: 2.0s per direction, 180-degree turns introduced.
- 14–20s (Hell Mode): 1.2s per direction, rapid unpredictable changes.
- Implementation (confirm by playtest): the next direction is drawn from the
  current one's neighbours — before 7s uniformly from ±90°, from 7s uniformly
  from ±90°/180°; "rapid unpredictable" keeps that full mix and only tightens
  the window to 1.2s.

## Open Questions
- [x] Decided: fixed 20s round (duration exception); score = correct directions, miss = no point, no game over; magnetometer block + message fallback; local-first persistence.
- [ ] Playtest caveat: the ±45° alignment tolerance and the turn mixes above are
  a first pass — confirm they feel fair for real spinning and adjust in
  `CompassChaosEngine` only.

## Implementation (Phase 1b)
- `feature/game/compass`: `CompassChaosEngine` (pure rules, JVM unit tests),
  `CompassChaosGame` (16 ms loop on `hostScope`, magnetometer → engine, point
  scores, `onFinished(true)` when the fixed round expires, sensor failure →
  blocked run), `CompassChaosScreen` (Compose rendering: compass ring + needle +
  target zone + window bar + blocked message + debug keys).
- Tests: `CompassChaosEngineTest` + `CompassChaosGameTest`.
- Debug-only affordance (emulator/desktop playtesting, gated on `BuildConfig.DEBUG`):
  **hold Left/Right (or A/D) to spin the needle at 180°/s** — the emulator cannot
  turn a body for real.