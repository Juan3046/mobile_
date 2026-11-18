# 03 — Minigame 03: BLINK SNIPER

> Ref. 2.2.1 — Blink Sniper (Camera). Sensor-driven game, part of Season 1 library.

## Concept
A stationary crosshair is fixed in the center of the screen. Targets ("dianas") pass continuously across the screen horizontally from left to right along the crosshair line, at varying speeds, so every target sweeps straight through the crosshair. The app uses the front-facing camera to detect when the player blinks. Every time a blink is detected, an arrow is fired instantly at the crosshair location.

- **Objective**: Score as many points as possible within 60 seconds by shooting targets with timed blinks.
- **Normal target hit**: +1 point.
- **Bullseye hit (center)**: +2 points.
- **Bomb / Hazard target hit**: Instant Game Over (run ends immediately; player retains points accumulated up to that moment).
- **Run end**: Fixed 60-second timer expires or a bomb target is shot.

## Controls & Sensors
- **Front-Facing Camera (`CameraX` + ML Kit Face Detection)**:
  - Detects eye-open probability (`leftEyeOpenProbability` and `rightEyeOpenProbability`).
  - A blink is triggered when **both** eye probabilities drop below the detection threshold and then return above it (or instantly upon eye closure with a minimum cooldown).
  - **Decided (playtest)**: the threshold stays at **0.5**, and the face counts as *open* as soon as **either** eye is at or above it. ML Kit reports the two eyes very unevenly off a webcam — an eye at `0.03` while the other reads `1.00`, for seconds at a time — so requiring *both* eyes to be open used to report open faces as closed and swallow the blink that followed. Consequence: a wink (one eye only) is **not** a trigger.
  - **Decided (playtest)**: frames are analysed at **320×240**. ML Kit's per-frame time is what caps the sampling rate, and sampling has to stay faster than a blink (200–400 ms) or the closed-eye state is never observed and no shot fires. The 640×480 default managed only **2.78 samples/s** (median 327 ms between them) and caught **6** blinks in a 48.6 s run; at 320×240 it manages **8.57 samples/s** (median 88 ms) and catches **14**.
- **Shot Mechanism**: Blinking fires an arrow instantly toward the center crosshair. The engine resolves the shot on the **OPEN → CLOSED edge**, so a single prolonged blink fires once.
- **Cooldown**: Firing cooldown fixed at **300 ms**; a blink inside the window is ignored (`ShotOutcome.IGNORED`).
- **Permission**: Requires runtime `android.permission.CAMERA`.
- **Desktop / Emulator Debug Only**: Spacebar or Mouse Click fires an arrow (stripped from release builds). *Not implemented in Phase 1b — tests drive the detector through `FakeBlinkDetector` instead; see Open Questions.*

## Format
- **Orientation**: Portrait only.
- **Camera View**: 2D arcade shooting gallery with targets scrolling horizontally from left to right along a single line (`y = 0.50`, normalized), passing straight through the fixed crosshair.
- **Camera Feedback Overlay**: Small box in a top corner showing the **eye tracking state** (open eye / closed eye + `CAM` label). *Provisional: a real video thumbnail needs a `:core:vision` change, which is frozen during Phase 1b — see Open Questions.*
- **Duration**: Fixed **60-second timer** (respecting the global 30–90s rule).

## Rules & Scoring
- **One run = one attempt** (global 2-attempts/day rule; best of 2 counts).
- **Hit Detection** (crosshair fixed at 0.5 / 0.5 of the gallery box; **decided**):
  - Every target travels along `y = 0.50`, straight through the crosshair, so it is always hittable. **Decided after playtest**: the crosshair does not move, so a target anywhere else could not be aimed at — there are **no decoy lanes**, only targets and bombs.
  - Distance from the crosshair to the target center determines the score for hittable targets.
  - Inner bullseye hit (≤ 0.030 gallery widths): **+2 points**.
  - Outer ring hit (≤ 0.070 gallery widths): **+1 point**.
  - Miss (nothing in range, or cooldown active): 0 points, arrow wasted.
  - **Hit Feedback**: a scoring shot pops `+1` / `+2` over the crosshair (mint for a bullseye, gold for the outer ring) plus an expanding shockwave at the impact point, both driven by `HitFeedback` (`points`, `x`, `y`, `atMs`, `sequence`) and shown for 700 ms (pop) / 450 ms (shockwave). The `sequence` increments per hit so the pop restarts instead of replaying a finished one; misses and bombs show no scoring feedback.
- **Bomb / Hazard Target**:
  - Identified visually (e.g. red skull / bomb icon on target).
  - Shooting a bomb target results in **instant Game Over**. The run ends immediately, but the player **keeps all points earned up to that moment**.
- **Target Movement**: All targets travel horizontally from left to right along the crosshair line at varying speeds.
- **Target Expiration**: Targets crossing off-screen without being shot simply exit without penalty.
- **Win / Finish State**: Timer reaches 0.0s without hitting a bomb target (`TIME'S UP!`).
- **Score Mapping**: Score = Total Points accumulated during the run. Daily best = max of up to 2 attempts; the daily best ranks the day for position points (never summed).

## Target Types
1. **Standard Target**: Classic red and white concentric rings. Outer radius = 0.070, bullseye = 0.030 gallery widths (+1 / +2 pts).
2. **Fast / Small Target**: Smaller and 1.35× faster, so it needs tighter timing. Outer radius = 0.050, bullseye = 0.020 (+1 / +2 pts).
3. **Bomb Target**: Dark red body with a black core, a bold yellow hazard cross and a breathing orange halo (900 ms cycle) so it stands out from the standard targets at a glance. Shooting this causes instant Game Over (keeping current points). The halo sits outside the hit radius: it is a warning, not part of the target.

## Difficulty & Progression
Continuous time-based ramp over the 60-second run (all values are `BlinkSniperConfig` defaults):

- **0–20s (Phase 1 - Warmup)**: spawn interval ramps within 1400 → ~1107 ms, speed within 0.28 → 0.37 gallery widths/s, **no bombs** (`bombChance = 0.0`).
- **20–40s (Phase 2 - Intermediate)**: spawn interval ~1107 → ~813 ms, speed ~0.37 → 0.46, **12% bomb chance** per spawn.
- **40–60s (Phase 3 - High Intensity)**: spawn interval ~813 → 520 ms, speed ~0.46 → 0.55, **30% bomb chance** per spawn.

Shared modifiers: per-target speed jitter ×0.85–1.15, 25% chance of a Fast/Small target, every spawn travels along the crosshair line (no decoy lanes), hard cap of 14 simultaneous targets, targets spawn just off the left edge (`x = -0.12`) and are dropped past `x = 1.12`.

## HUD (minimal)
- **Top Left**: Score counter (e.g. `SCORE: 18`).
- **Top Right**: Countdown timer (e.g. `TIME: 42.5s`), tenth-of-a-second resolution, rounded up from the engine clock.
- **Top Corner Box**: Eye tracking state preview (see Format).
- **Center**: Fixed crosshair / reticle overlay.

## Game Over & Results Flow
- **On Shooting a Bomb**: `BOOM! Game Over` message + final score (retained points). Explosion animation and camera shake are post-MVP polish (provisional build shows the message only).
- **On Timer Expiration (60s)**: `TIME'S UP!` screen + final score summary (a run with no bomb hit counts as a win).
- **Retry**: Respects remaining daily attempts (lock + countdown if 0 attempts remain).

## Art Direction
- **Theme**: Carnival / arcade shooting gallery.
- **Color Palette**: Bright contrasting colors for targets against a dark gallery background.
- **Provisional Graphics**: Procedural vector / Canvas shapes (concentric circles, crosshairs, a hazard cross with a breathing halo on bombs, floating `+N` hit pops and impact shockwaves) so code compiles and runs without external asset files.

## Fallback (Missing Sensor / Permission Denied)
- Per global rules: If `CAMERA` permission is denied or no front camera is available, **disable Play** with a clear explanation screen:
  *"Blink Sniper requires camera permission and a front-facing camera to play."*
- Never record fake scores. No touch fallback in release build.

## Persistence (Local-only MVP)
- Save locally: Best score (points), targets hit, attempts used today, timestamp.

## Open Questions
- [x] Decided: Fixed 60-second timer; retain accumulated points on bomb hit; real-time camera thumbnail in corner; targets travel left-to-right across lanes; instant raycast arrow shot on blink detection; score = total points earned.
- [x] Decided (Phase 1b, revised after playtest): **every target travels along the crosshair line** — the crosshair is fixed, so decoy lanes could never be aimed at and read as a bug rather than as a challenge. The shot stays honest (blink timing is the skill) without needing an aim mechanism the blink input cannot provide.
- [ ] **Real camera preview in the corner.** The current build shows a placeholder eye-state box because `:core:vision` only exposes `EyeState`/`BlinkDetector`; adding a CameraX preview surface means changing a frozen shared module. Decide whether to extend `:core:vision` (owner: Phase 1a) or accept the eye-state placeholder for the MVP.
- [x] Decided (emulator playtests, 2026-11-17): threshold stays at **0.5** under the "both eyes below it" rule. Measured webcam readings — open faces logged as `1.00 / 0.03`, `0.98 / 0.23`, `1.00 / 0.11` (the old "both eyes must be open" rule reported all of them CLOSED) versus real blinks at `0.26 / 0.17`, `0.33 / 0.40`, `0.06 / 0.45` — mean a `< 0.25` threshold would have swallowed the `0.26` and `0.33` blinks, while `0.5` separates the two populations cleanly.
