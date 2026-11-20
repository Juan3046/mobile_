# 06 — Minigame 06: CLASS RUSH

## Concept
A student is late for a lecture at VŠB in Poruba and has to get across the campus
neighbourhood as far as possible in 60 seconds: streets with cars, tram tracks and a frozen
pond with drifting ice floes. Each lane crossed is one row. Getting hit, falling into the
pond or standing still for too long ends the run early.

An original take on the lane-crossing arcade genre: own name, character, setting and simple
procedural art (no assets, no references to other games).

## Format
- Portrait only. Top-down grid, 7 columns wide; the view follows the student upwards.
- Fixed **60-second** run (inside the standard 30–90 s rule).
- Procedural lanes per run (seeded): the first rows are safe pavement, then a mix of
  pavement, road, tram track and pond. Lanes get faster the further you go.

## Controls (accelerometer)
- **Tilt the phone forward** (top edge away from you) to hop one row forward.
- **Tilt it back** (top edge towards you) to hop one row back.
- **Tilt left / right** to step one column sideways.
- **One tilt = one hop**: come back to the middle to hop again (holding never repeats).
- Neutral is the pose held while the run starts (~0.4 s of readings) and slowly follows the
  hand while it rests in the middle.
- Readings are low-passed to gravity, so a jolt of the hand is not a tilt. A hop needs ≥ 15°
  on one clear axis (the other axis at most 1/1.4 of it: diagonals are ignored); coming back
  under 8° re-arms. Tuning history: the first version (18°/9°, raw readings, auto-repeat every
  0.35 s) felt "very bad" in the 2026-11-20 playtest.
- **Tilt indicator** (bottom right): the dot is the phone's tilt, the ring is where a hop
  fires, the inner disc is the middle that re-arms.
- Debug builds log the angles to Logcat (`RushTilt`) for tuning.
- Debug builds: arrow keys / WASD hop (emulator only).

## Lanes & hazards
- **Pavement**: always safe.
- **Road**: cars of 1–2 cells drive left or right at a steady speed. Touching one ends the run.
- **Tram track**: a long, fast tram passes now and then. A red warning light blinks 1.2 s
  before it enters the screen. Touching it ends the run.
- **Pond**: ice floes of 2–3 cells drift sideways. Stand on a floe (it carries you); landing
  in the water or being carried off the edge ends the run.
- **Too slow**: 8 s without reaching a new row ends the run ("The lecture started without you").

## Scoring
- **Score = rows advanced (the furthest row reached)**, higher wins (`higherWins = true`).
- A run ends at 60 s (a win) or earlier on a hit / fall / idle (not a win); the score keeps
  the furthest row either way.
- One run = one attempt; the daily best ranks the day for position points.

## Fallback
- Missing accelerometer: the Main screen disables Play with a message (docs/03 §3.1); if the
  sensor fails mid-run, the run is blocked and nothing is recorded.

## Feedback
- A short, light vibration on each hop; a longer one when the run ends early. Honours
  Settings → Vibration.

## Implementation
- `feature/game/rush`: `ClassRushEngine` (pure rules: lanes, hops, hazards, timer, score;
  JVM tests), `TiltHopDetector` (accelerometer → hops; JVM tests), `ClassRushGame` (16 ms loop
  on `hostScope`), `ClassRushScreen` (Canvas: lanes, cars, tram + warning light, floes,
  student, HUD with time left and rows), `ClassRushThumbnail`.
- Game id `06-class-rush`; accent colour `GameAccents.Rush`.
