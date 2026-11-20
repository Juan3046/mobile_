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
- One tilt = one hop; keep it tilted to repeat (every 0.35 s). Return to neutral to re-arm.
- Neutral is calibrated from how the player holds the phone when the run starts, and slowly
  follows it while the phone rests in the neutral zone.
- Thresholds (tune by playtest): hop at ≥ 18° away from neutral, re-arm under 9°. Sideways
  takes priority only when it is the larger tilt.
- Debug builds: arrow keys / WASD hop (emulator only).

## Lanes & hazards
- **Pavement**: always safe.
- **Road**: cars of 1–2 cells drive left or right at a steady speed. Touching one ends the run.
- **Tram track**: a long, fast tram passes now and then. A red warning light blinks 1.2 s
  before it enters the screen. Touching it ends the run.
- **Pond**: ice floes of 2–3 cells drift sideways. Stand on a floe (it carries you); landing
  in the water or being carried off the edge ends the run.
- **Too slow**: 6 s without reaching a new row ends the run ("The lecture started without you").

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
