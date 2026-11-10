# 03 — Minigame 03: BLINK SNIPER

> Ref. 2.2.1 — Blink Sniper (Camera). Sensor-driven game, part of Season 1 library.

## Concept
A stationary crosshair is fixed in the center of the screen. Targets ("dianas") pass continuously across the screen horizontally from left to right at various heights and speeds. The app uses the front-facing camera to detect when the player blinks. Every time a blink is detected, an arrow is fired instantly at the crosshair location.

- **Objective**: Score as many points as possible within 60 seconds by shooting targets with timed blinks.
- **Normal target hit**: +1 point.
- **Bullseye hit (center)**: +2 points.
- **Bomb / Hazard target hit**: Instant Game Over (run ends immediately; player retains points accumulated up to that moment).
- **Run end**: Fixed 60-second timer expires or a bomb target is shot.

## Controls & Sensors
- **Front-Facing Camera (`CameraX` + ML Kit Face Detection)**:
  - Detects eye-open probability (`leftEyeOpenProbability` and `rightEyeOpenProbability`).
  - A blink is triggered when both eye probabilities drop below the detection threshold (e.g. $< 0.25$) and then return above it (or instantly upon eye closure with a minimum cooldown).
- **Shot Mechanism**: Blinking fires an arrow instantly toward the center crosshair. The engine resolves the shot on the **OPEN → CLOSED edge**, so a single prolonged blink fires once.
- **Cooldown**: Firing cooldown fixed at **300 ms**; a blink inside the window is ignored (`ShotOutcome.IGNORED`).
- **Permission**: Requires runtime `android.permission.CAMERA`.
- **Desktop / Emulator Debug Only**: Spacebar or Mouse Click fires an arrow (stripped from release builds). *Not implemented in Phase 1b — tests drive the detector through `FakeBlinkDetector` instead; see Open Questions.*

## Format
- **Orientation**: Portrait only.
- **Camera View**: 2D arcade shooting gallery with targets scrolling horizontally from left to right across five height lanes (`y = 0.14 / 0.32 / 0.50 / 0.68 / 0.86`, normalized).
- **Camera Feedback Overlay**: Small box in a top corner showing the **eye tracking state** (open eye / closed eye + `CAM` label). *Provisional: a real video thumbnail needs a `:core:vision` change, which is frozen during Phase 1b — see Open Questions.*
- **Duration**: Fixed **60-second timer** (respecting the global 30–90s rule).

## Rules & Scoring
- **One run = one attempt** (global 2-attempts/day rule; best of 2 counts).
- **Hit Detection** (crosshair fixed at 0.5 / 0.5 of the gallery box; **decided**):
  - Only the **center lane** (`y = 0.50`) can ever be hit. The crosshair does not move, so targets on the other lanes are **visual decoys** — they raise the density and the reading challenge but cannot be scored.
  - Distance from the crosshair to the target center determines the score for hittable targets.
  - Inner bullseye hit (≤ 0.030 gallery widths): **+2 points**.
  - Outer ring hit (≤ 0.070 gallery widths): **+1 point**.
  - Miss (nothing in range, or cooldown active): 0 points, arrow wasted.
- **Bomb / Hazard Target**:
  - Identified visually (e.g. red skull / bomb icon on target).
  - Shooting a bomb target results in **instant Game Over**. The run ends immediately, but the player **keeps all points earned up to that moment**.
- **Target Movement**: All targets travel horizontally from left to right across multiple height lanes at varying speeds.
- **Target Expiration**: Targets crossing off-screen without being shot simply exit without penalty.
- **Win / Finish State**: Timer reaches 0.0s without hitting a bomb target (`TIME'S UP!`).
- **Score Mapping**: Score = Total Points accumulated during the run. Daily best = max of up to 2 attempts; the daily best ranks the day for position points (never summed).

## Target Types
1. **Standard Target**: Classic red and white concentric rings. Outer radius = 0.070, bullseye = 0.030 gallery widths (+1 / +2 pts).
2. **Fast / Small Target**: Smaller and 1.35× faster, so it needs tighter timing. Outer radius = 0.050, bullseye = 0.020 (+1 / +2 pts).
3. **Bomb Target**: Dark red / black with a hazard cross. Shooting this causes instant Game Over (keeping current points).

## Difficulty & Progression
Continuous time-based ramp over the 60-second run (all values are `BlinkSniperConfig` defaults):

- **0–20s (Phase 1 - Warmup)**: spawn interval ramps within 1400 → ~1107 ms, speed within 0.28 → 0.37 gallery widths/s, **no bombs** (`bombChance = 0.0`).
- **20–40s (Phase 2 - Intermediate)**: spawn interval ~1107 → ~813 ms, speed ~0.37 → 0.46, **12% bomb chance** per spawn.
- **40–60s (Phase 3 - High Intensity)**: spawn interval ~813 → 520 ms, speed ~0.46 → 0.55, **30% bomb chance** per spawn.

Shared modifiers: per-target speed jitter ×0.85–1.15, 25% chance of a Fast/Small target, 50% of spawns land on the crosshair lane (the rest are decoys), hard cap of 14 simultaneous targets, targets spawn just off the left edge (`x = -0.12`) and are dropped past `x = 1.12`.

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
- **Provisional Graphics**: Procedural vector / Canvas shapes (concentric circles, crosshairs, bomb icon) so code compiles and runs without external asset files.

## Fallback (Missing Sensor / Permission Denied)
- Per global rules: If `CAMERA` permission is denied or no front camera is available, **disable Play** with a clear explanation screen:
  *"Blink Sniper requires camera permission and a front-facing camera to play."*
- Never record fake scores. No touch fallback in release build.

## Persistence (Local-only MVP)
- Save locally: Best score (points), targets hit, attempts used today, timestamp.

## Open Questions
- [x] Decided: Fixed 60-second timer; retain accumulated points on bomb hit; real-time camera thumbnail in corner; targets travel left-to-right across lanes; instant raycast arrow shot on blink detection; score = total points earned.
- [x] Decided (Phase 1b): **only the center lane is hittable** — the crosshair is fixed, so the other lanes are visual decoys. This keeps the shot honest (blink timing is the skill) instead of requiring an aim mechanism the blink input cannot provide.
- [ ] **Real camera preview in the corner.** The current build shows a placeholder eye-state box because `:core:vision` only exposes `EyeState`/`BlinkDetector`; adding a CameraX preview surface means changing a frozen shared module. Decide whether to extend `:core:vision` (owner: Phase 1a) or accept the eye-state placeholder for the MVP.
- [ ] ML Kit eye detection threshold tuning (e.g. `< 0.25` open probability) during initial implementation playtests.
