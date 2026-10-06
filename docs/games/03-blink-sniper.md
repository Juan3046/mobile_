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
- **Shot Mechanism**: Blinking fires an arrow instantly toward the center crosshair.
- **Cooldown**: A short firing cooldown (e.g. 300 ms) prevents accidental multi-firing from a single prolonged blink.
- **Permission**: Requires runtime `android.permission.CAMERA`.
- **Desktop / Emulator Debug Only**: Spacebar or Mouse Click fires an arrow (stripped from release builds).

## Format
- **Orientation**: Portrait only.
- **Camera View**: 2D arcade shooting gallery with targets scrolling horizontally from left to right.
- **Camera Feedback Overlay**: Small real-time camera video thumbnail in a top corner showing the player's face and eye tracking state.
- **Duration**: Fixed **60-second timer** (respecting the global 30–90s rule).

## Rules & Scoring
- **One run = one attempt** (global 2-attempts/day rule; best of 2 counts).
- **Hit Detection**:
  - Distance from arrow impact (center of crosshair) to target center determines score.
  - Outer ring hit: **+1 point**.
  - Inner bullseye hit: **+2 points**.
  - Miss (no target at crosshair): 0 points, arrow wasted.
- **Bomb / Hazard Target**:
  - Identified visually (e.g. red skull / bomb icon on target).
  - Shooting a bomb target results in **instant Game Over**. The run ends immediately, but the player **keeps all points earned up to that moment**.
- **Target Movement**: All targets travel horizontally from left to right across multiple height lanes at varying speeds.
- **Target Expiration**: Targets crossing off-screen without being shot simply exit without penalty.
- **Win / Finish State**: Timer reaches 0.0s without hitting a bomb target (`TIME'S UP!`).
- **Score Mapping**: Score = Total Points accumulated during the run. Daily best = max of up to 2 attempts; season score = sum of daily bests.

## Target Types
1. **Standard Target**: Classic red and white concentric rings. Outer ring = +1 pt, Center bullseye = +2 pts.
2. **Fast / Small Target**: Smaller or faster moving target for higher precision challenge (+1 pt outer, +2 pts bullseye).
3. **Bomb Target**: Dark red / black with bomb or hazard symbol. Shooting this causes instant Game Over (keeping current points).

## Difficulty & Progression
Continuous time-based ramp over the 60-second run:
- **0–20s (Phase 1 - Warmup)**: Low target density, predictable speeds, single/double lanes, no bombs.
- **20–40s (Phase 2 - Intermediate)**: Increased target density, multi-lane heights, varying speeds, occasional bomb targets spawn among standard targets.
- **40–60s (Phase 3 - High Intensity)**: High target density, fast targets, frequent bomb targets placed close to standard targets requiring precise blink timing.

## HUD (minimal)
- **Top Left**: Score counter (e.g. `SCORE: 18`).
- **Top Right**: Countdown timer (e.g. `TIME: 42.5s`).
- **Top Corner Box**: Small real-time camera video preview.
- **Center**: Fixed crosshair / reticle overlay.

## Game Over & Results Flow
- **On Shooting a Bomb**: Explosion animation, camera shake, `BOOM! Game Over` message + final score (retained points).
- **On Timer Expiration (60s)**: `TIME'S UP!` screen + final score summary.
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
- [ ] ML Kit eye detection threshold tuning (e.g. `< 0.25` open probability) during initial implementation playtests.
