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
- **Gyroscope**: tilt phone left/right; the onion strafes sideways following the tilt.
  Smooth analog follow, no input lag. Tilt axis = horizontal only (portrait).
- No jump, attack, or extra buttons. No touch input in the release build.
- **Permission**: the Android gyroscope needs no runtime permission; the app only
  requires the sensor to exist (`SensorManager.TYPE_GYROSCOPE`).
- Desktop/emulator debug only: `A` / Left arrow, `D` / Right arrow (strip for release).

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
- **Fruit collision = blocking only.** Fruits occupy space; the onion bumps and
  slides along them. No damage, no knockback, no instant death. Per decided
  design the run is passive: the belt ramp alone determines survival time
  (see Open Questions for the playtest caveat).
- **Score = survived time in centiseconds** (win-equivalent max ≈ 8500, hard
  ceiling 9000 at 90s). Daily best of the 2 attempts; season = sum of daily bests
  (same mapping as `01-escape-poruba.md`).
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
Continuous time-based ramp (e.g. `beltSpeed = base * pow(1 + elapsed/T, k)`,
tuned by playtest) driving:
- Belt scroll speed (fruit approach speed).
- Fruit spawn rate / density on the belt.
- Time between fruit groups (less reaction time).
- Curves tuned so: early game readable, mid game busy, last ~10s dense enough
  that the fall lands in the 80–85s window.

## HUD (minimal)
- Small elapsed-time corner (e.g. `62.4s`).
- **End-of-belt proximity gauge** showing how close the onion is to the fall.
- Nothing else.

## Game Over Flow
- On falling off the end: short fall animation, then results: survived time +
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
- [ ] Ramp tuning: exact `base` speed / curve for the 80–85s perfect-run window.
