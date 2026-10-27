# 02 — Minigame 02: ONION ROLL

> Ref. 2.2.2 — Onion Roll (Gyroscope). Sensor-driven game, part of Season 1 library.

## Concept
An onion rides a supermarket checkout conveyor belt toward the end of the belt.
Fruits (groceries) ride toward the onion on the same belt. The player steers the
onion left/right to dodge them and survive as long as possible. The belt
accelerates continuously: the onion is inevitably carried off the end, and that
fall ends the run. There is no win state — score is pure survival time.

- Lose: the onion reaches the end of the belt and falls (run ends, score banked).
- The ramp is tuned so runs end **before 90s**; a perfect run lasts **≈ 80–85s**.
- Contact with a fruit is **not lethal** (blocking only — see Rules).

## Controls
- **Gyroscope — rate-based steering**: the onion's strafing speed follows the
  phone's angular velocity around the portrait axis (`ω_z`, rad/s) — tilt left to
  go left, tilt right to go right — through a deadband of `0.05 rad/s` (swallows
  sensor bias), gain `0.55` (screen-widths/s per rad/s), a 60 ms smoothing window
  and a comfort cap of `1.1` screen-widths/s. The onion **parks where the player
  leaves it**: a gyroscope cannot sense a static lean, so a "follow the tilt
  angle" model would either drift or rubber-band back to centre. Tilt axis =
  horizontal only (portrait). Verify axis/sign on device (`STEERING_SIGN`, one
  constant to flip; a flat hold may need the y-axis instead).
- No jump, attack, or extra buttons. No touch input in the release build.
- **Permission**: the Android gyroscope needs no runtime permission; the app only
  requires the sensor to exist (`SensorManager.TYPE_GYROSCOPE`).
- Desktop/emulator debug only: `A` / Left arrow, `D` / Right arrow — compiled
  behind `BuildConfig.DEBUG`, so release builds ignore them entirely.

## Format
- Portrait only, **top-down view over the belt**.
- Belt scrolls vertically; the onion rides toward the belt end; the player controls
  horizontal position only.
- Duration: run ends when the onion falls (tuned to 80–85s, hard cap 90s per
  global `02-gameplay-rules.md`).

## Rules & Scoring
- **One run = one attempt** (global 2-attempts/day rule; best of 2 counts).
- **Belt speed ramps continuously** over the run: fruits approach faster and the
  end of the belt arrives sooner. The ramp guarantees the fall before 90s.
- **Fruit collision = blocking only.** Fruits occupy space and the onion never
  enters them: while a fruit is alongside (same horizontal band), it caps or
  floors the onion's x-range — fruits on the right cap it, fruits on the left
  floor it, and being squeezed between two holds the onion in place. A fruit
  that would ride into the onion's space stops just short (`0.01` gap) and
  resumes once the onion dodges away. Blocking never touches the fall timer.
  No damage, no knockback, no instant death. Per decided design the run is
  passive: the belt ramp alone determines survival time (see Open Questions
  for the playtest caveat).
- **Score = survived time in centiseconds** (win-equivalent max ≈ 8500, hard
  ceiling 9000 at 90s). Daily best of the 2 attempts; the daily best ranks the day for position points (same mapping as `01-escape-poruba.md`).
- **No win state**: reaching 90s without falling is not expected; the run always
  ends in a fall.

## Entities
- **Onion**: the player character (flat-cartoon, simple face).
- **Fruits (starter set, passive riders)** — they ride the belt at belt speed,
  no self-driven behavior:
  - `CHERRY` — small, easy to thread between.
  - `APPLE` — medium, round, standard blocker.
  - `BANANA` — long, blocks a full lane width.

## Difficulty Ramp
Continuous time-based ramp (initial values live in `OnionRollEngine`, tune by
playtest) driving:
- **Fall window**: `progress = (elapsed / 82s)^1.6`; the run falls when
  `progress` reaches 1 — 82 s, the centre of the 80–85 s window — with a 90 s
  hard cap as a safety net. The end-of-belt line rises with `progress`.
- **Belt speed**: `0.22 * (1 + elapsed / 45s)^1.3` screen-heights/s — both the
  fruit approach speed and the belt scroll.
- **Fruit spawns**: first at 600 ms, then every `1.2s → 0.4s` (scaled by
  progress). One fruit per spawn while `progress < 0.45`, then 1–2 — never all
  three lanes, so one lane always stays open. Mix: `CHERRY` 45 % / `APPLE` 35 %
  / `BANANA` 20 %.
- Curves tuned so: early game readable, mid game busy, last ~10s dense enough
  that the fall lands in the 80–85s window.

## HUD (minimal)
- Elapsed time, top-left corner (e.g. `62.4s`), locale-independent formatting.
- **End-of-belt proximity gauge**, top-right, showing how close the onion is to
  the fall (the host's score overlay sits top-centre, so corners stay clear).
- Nothing else.

## Game Over Flow
- On falling off the end: short fall animation (700 ms; the score is frozen at
  the moment of the fall), then results: survived time +
  best (local). Retry respects global attempts; with 0 attempts left →
  lock + countdown screen (global rule).
- Fast loop: play → fall → retry almost instantly while attempts remain.

## Art Direction
- **Setting: supermarket checkout** — belt, scanner area, groceries, shelves backdrop.
- **Flat cartoon**: clean bold shapes, friendly colors, readable silhouettes;
  the onion must pop against the belt and fruits.
- Provisional procedural in-project sprites; code structured for easy replacement
  later (same approach as `01-escape-poruba.md`). No external assets required.

## Fallback (missing sensor / permission denied)
- Per global rule: **disable Play with a clear message** ("Onion Roll requires a
  gyroscope sensor") and never record fake scores.
- No touch fallback in release (touch is not an approved control for this game).

## Persistence (local-only MVP)
- Best survival time (centiseconds), attempts used today, per-game attempt count.

## Open Questions
- [x] Decided: gyroscope tilt left/right, top-down checkout view, passive fruits
  (apple/banana/cherry), blocking-only collisions, belt ramp kills before 90s
  (perfect ≈ 80–85s), no win state, score = survived centiseconds, passive run
  accepted (player input does not change the outcome by design).
- [ ] Playtest caveat: a fully passive run means most players post ≈ the same
  score. If the leaderboard stagnates, revisit (e.g. small ground loss on contact
  or per-run randomized ramp). Do not change before first playtest.
- [ ] Ramp tuning: initial values are in `OnionRollEngine` (fall at `82s` via
  `^1.6`, belt `0.22 * (1 + t/45s)^1.3`, spawns `1.2s → 0.4s`); confirm the
  80–85 s window and the feel by playtest.

## Implementation (Phase 1b)
- `feature/game/onion`: `OnionRollEngine` (pure rules, JVM unit tests),
  `OnionRollGame` (16 ms loop on `hostScope`, gyro → engine, centisecond
  scores, `onFinished(false)` after the fall animation), `OnionRollScreen`
  (Compose rendering + debug keys).
- Tests: `OnionRollEngineTest` + `OnionRollGameTest`.
