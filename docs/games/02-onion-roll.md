# 02 — Minigame 02: ONION ROLL

> Ref. 2.2.2 — Onion Roll (Gyroscope). Sensor-driven game, part of Season 1 library.

## Concept
An onion rides an **endless** supermarket checkout conveyor belt. Fruits
(groceries) ride toward the onion on the same belt. The player steers the onion
left/right to dodge them and survive as long as possible. Every fruit that hits
the onion head-on shoves it back toward the end roller at the bottom of the
screen; if it is shoved over the end, it falls and the run ends. The belt keeps
accelerating, so every run ends eventually. There is no win state — score is
pure survival time in whole seconds.

- Lose: the onion is knocked over the end roller and falls (run ends, score banked).
- **No time limit** (approved exception to the global 30–90 s rule, see
  `../02-gameplay-rules.md`): the ever-faster belt is what ends runs.
- Contact with a fruit is **not lethal**, but it **knocks the onion back** toward
  the end of the belt (see Rules) — dodging is what separates scores.

## Controls
- **Gyroscope — rate-based steering**: the onion's strafing speed follows the
  phone's left/right **tilt rate** — tilt left to go left, tilt right to go
  right. The tilt rate combines two gyro axes, `tiltRate = ω_z − ω_y` (rad/s,
  positive = lean left), so it works for any hold: an upright phone tilts around
  `z` (steering-wheel motion), a flat phone rolls around `y`, and a semi-upright
  hold mixes both. It then goes through a deadband of `0.05 rad/s` (swallows
  sensor bias), gain `1.4` (screen-widths/s per rad/s — ≈ 15° of tilt moves one
  lane, ≈ 35° crosses the belt), a 60 ms smoothing window and a comfort cap of
  `2.5` screen-widths/s. Gain/cap raised from `0.55`/`1.1` after the first
  device playtest (steering needed ≈ 85° to cross the belt). The onion **parks
  where the player leaves it**: a gyroscope cannot sense a static lean, so a
  "follow the tilt angle" model would either drift or rubber-band back to
  centre. Tilt axis = horizontal only (portrait). Sign lives in
  `STEERING_SIGN` (one constant to flip). Known limit: turning the whole body
  while holding the phone upright leaks a little into `ω_y`.
- No jump, attack, or extra buttons. No touch input in the release build.
- **Permission**: the Android gyroscope needs no runtime permission; the app only
  requires the sensor to exist (`SensorManager.TYPE_GYROSCOPE`).
- Desktop/emulator debug only: `A` / Left arrow, `D` / Right arrow — compiled
  behind `BuildConfig.DEBUG`, so release builds ignore them entirely.
- **Debug-only RESTART button** (bottom-right, `BuildConfig.DEBUG` only) for
  playtesting, visible the whole time — also after losing:
  - while running or during the "YOU LOST!" banner it restarts the run in
    place (still the host's scored attempt);
  - once the finish has been reported (the host has shut the run down) it
    starts an **unscored practice run** on the screen's own scope, labelled
    "PRACTICE · not scored"; nothing reaches the host, and it ends when the
    screen leaves.
  Hidden in release because restarting a
  bad run without spending an attempt would bypass the 2-attempts/day rule;
  the real "Play again" lives on the host result screen (`03-screens.md` 3.3).

## Format
- Portrait only, **top-down view over the belt**.
- Belt scrolls vertically toward a fixed end roller near the bottom of the
  screen (`y = 0.95`); the onion's home row is `y = 0.68`; the player controls
  horizontal position only.
- Duration: **endless** — the run ends only when the onion falls. No time cap
  (`Game.durationMs = Long.MAX_VALUE`). The engine keeps an optional
  `timeLimitMs` (default `null` = endless) used by tests only.

## Rules & Scoring
- **One run = one attempt** (global 2-attempts/day rule; best of 2 counts).
- **Belt speed ramps continuously and without limit**: fruits approach faster
  and faster, so runs always end in a fall.
- **Fruit collision = solid + knockback.** Fruits occupy space and the onion
  never enters their hitbox:
  - *Forgiving hitboxes*: every hitbox (onion and fruits) is **80 %** of the
    drawn size on each axis (`HITBOX_SCALE = 0.8`), so sprites may graze each
    other and a hit only lands when they clearly meet — e.g. a sprite 5 units
    wide collides as 4. Belt-edge clamping still uses the drawn size, so the
    onion never hangs off the belt.
  - *Sideways*: while a fruit is alongside (same horizontal band), it caps or
    floors the onion's x-range — fruits on the right cap it, fruits on the left
    floor it, and being squeezed between two holds the onion in place.
  - *Head-on*: a fruit riding into the onion keeps riding and **shoves the
    onion back** toward the end roller. The onion is pushed just clear of
    the fruit (`0.01` gap) and gets a backward kick of belt speed + `0.6`
    screen-heights/s that decays over ~80 ms, so each hit costs roughly
    `0.05–0.08` screen-heights of ground. A fruit that keeps coming hits again,
    so the player must dodge sideways.
  - *Recovery*: once clear, the onion walks back toward its home row
    (`y = 0.68`) at `0.04` screen-heights/s, never into a fruit.
  - No damage, no instant death — the run **falls as soon as the onion's
    centre is shoved past the end roller** (`onionY ≥ 0.95`; ≈ 4 unrecovered
    hits from the home row).
- **Score = survived time in whole seconds** (`elapsed / 1000`, frozen at the
  moment of the fall). No ceiling. Daily best of the 2 attempts; the daily best
  ranks the day for position points (same mapping as `01-escape-poruba.md`).
  Whole seconds mean more ties than centiseconds — accepted by design.
- **No win state**: the run always ends in a fall.

## Entities
- **Onion**: the player character (red onion, simple face).
- **Fruits (starter set, passive riders)** — they ride the belt at belt speed,
  no self-driven behavior:
  - `CHERRY` — small, easy to thread between.
  - `APPLE` — medium, round, standard blocker.
  - `BANANA` — long, blocks a full lane width.

## Difficulty Ramp
Continuous time-based ramp (initial values live in `OnionRollEngine`, tune by
playtest) driving:
- **Belt speed** (unbounded): `0.429 * (1 + elapsed / 40.9s)^1.3`
  screen-heights/s — both the fruit approach speed and the belt scroll. Base
  speed history: `0.22` → `0.286` (+30 %) → `0.429` (+50 % on top) after
  playtests asking for more difficulty. Acceleration made 10 % faster after the
  fourth playtest: the ramp time constant went `45s` → `40.9s` (`45 / 1.1`),
  starting speed unchanged.
- **Traffic intensity**: `intensity = min(1, (elapsed / 82s)^1.6)` — slow start,
  full density from 82 s on.
- **Fruit spawns**: first at 600 ms, then every `1.2s → 0.4s` (scaled by
  intensity). One fruit per spawn while `intensity < 0.45`, then 1–2 — never all
  three lanes, so one lane always stays open. Mix: `CHERRY` 45 % / `APPLE` 35 %
  / `BANANA` 20 %.
- Curves tuned so: early game readable, mid game busy, and the belt keeps
  speeding up until even perfect dodging cannot keep up.

## HUD (minimal)
- **Score counter only**: the whole seconds survived (= the score), big and
  bold, top-centre, in a dark pill with a drop shadow; it pops slightly each
  time a second ticks. The entry sets `drawsOwnHud = true`, so the host hides
  its placeholder `score: N` and `Run finished (win=…)` texts for this game
  (docs/08 — Game Hosting Contract); the "YOU LOST!" banner stays on screen
  after the finish. The old top-left elapsed label and the end-of-belt gauge
  were removed.
- **Danger glow** at the bottom: a red light rising from the end roller plus
  red warning lamps along the roller, always faintly on (the end is
  dangerous) and growing brighter, taller and faster-pulsing the closer the
  onion has been shoved to the roller:
  `danger = clamp((onionY − 0.68) / (0.95 − 0.68), 0, 1)`; glow alpha
  `(0.22 + 0.48·danger) · (0.875 + 0.125·sin(2π·f·t))` (dips to 75 % of the
  peak), pulse `f = 0.8 + 2.4·danger` Hz.

## Game Over Flow
- On falling off the end: short fall animation (700 ms; the onion drops with a
  gravity-like quadratic ease and tumbles up to 240°; the score is frozen at
  the moment of the fall). A **"YOU LOST!"** banner (English copy, per
  `03-screens.md`) fades in over the belt as the fall starts and stays up for
  **2 s in total** (fall + 1.3 s hold) before the game reports the finish, so
  the player always sees it. The banner shows the score (`Survived 62 s`). Then
  results: survived time + best (local). Retry respects global attempts; with
  0 attempts left → lock + countdown screen (global rule).
- Fast loop: play → fall → retry almost instantly while attempts remain.

## Art Direction
- **Setting: supermarket checkout** — belt, scanner area, groceries, shelves backdrop.
- **Stylised-realistic, procedurally drawn** (Compose Canvas, no external
  assets): soft gradients for volume, specular highlights and drop shadows so
  objects read as solid things sitting on the belt; the onion must pop against
  the belt and fruits.
  - **Belt**: dark ribbed rubber with lighting falloff, ribs scrolling with the
    belt speed; brushed-steel side rails; a fixed steel roller at the end of the
    belt with a dark drop below it.
  - **Onion**: red-onion bulb (purple radial gradient, skin veins, papery
    sprout tip, root tuft) with its face; tumbles as it falls.
  - **Apple**: red radial gradient, highlight, stem cavity, stem and leaf.
  - **Cherry**: a pair of glossy dark-red cherries joined by green stems.
  - **Banana**: curved yellow crescent with shading, ridge line and brown tips.
- Each sprite fills its entity box (`halfWidth`/`halfHeight` × screen size),
  so what you see is what collides (minus the hitbox margin). Box sizes are
  tuned to look round on a typical ≈ 1 : 1.9 phone play area.
- Drawing lives in `OnionRollArt.kt`, separate from the screen layout, so it
  can be swapped for bitmap sprites later.

## Fallback (missing sensor / permission denied)
- Per global rule: **disable Play with a clear message** ("Onion Roll requires a
  gyroscope sensor") and never record fake scores.
- No touch fallback in release (touch is not an approved control for this game).

## Persistence (local-only MVP)
- Best survival time (whole seconds), attempts used today, per-game attempt count.

## Open Questions
- [x] Decided: gyroscope tilt left/right, top-down checkout view, passive fruits
  (apple/banana/cherry), no win state.
- [x] Decided after the first device playtest: fruit contact **knocks the onion
  back** (ground loss) instead of blocking only, so dodging changes the score
  (replaces the earlier "passive run" design); steering gain/cap raised and the
  tilt rate reads both `ω_z` and `ω_y`.
- [x] Decided after the third playtest (2026-11-10): **endless belt** — the
  time-driven rising end-of-belt line (forced fall at 82 s, 90 s hard cap) is
  gone; the end roller is fixed and only knockback ends a run; score = whole
  seconds survived (was centiseconds); end-of-belt gauge removed; base belt
  speed `0.429`.
- [ ] Knockback tuning: kick (`+0.6` screen-heights/s, ~80 ms) and recovery
  (`0.04` screen-heights/s) are first guesses — confirm by playtest that a few
  hits hurt without making one mistake fatal.
- [ ] Ramp tuning: belt `0.429 * (1 + t/40.9s)^1.3` and spawns `1.2s → 0.4s` —
  confirm by playtest that typical runs stay short (the product targets quick
  sessions) even without a cap.

## Implementation (Phase 1b)
- `feature/game/onion`: `OnionRollEngine` (pure rules, JVM unit tests),
  `OnionRollGame` (16 ms loop on `hostScope`, gyro tilt rate → engine, whole-
  second scores, `onFinished(false)` after the fall animation and the 2 s
  "YOU LOST!" banner), `OnionRollScreen` (layout, HUD, banner + debug keys),
  `OnionRollArt` (procedural belt/fruit/onion sprites).
- `OnionRollVisuals` (pure presentation math: fall curves, face layout,
  loss-banner fade/pop, danger level and glow pulse).
- Tests: `OnionRollEngineTest` + `OnionRollGameTest` + `OnionRollVisualsTest`.
