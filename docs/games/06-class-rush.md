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

## Controls (touch — approved exception)
- **Tap anywhere** to hop one row forward.
- **Swipe left / right** to step one column sideways; **swipe down** to hop one row back
  (swipe up also hops forward). A finger move under 28 dp is a tap.
- **Why touch** (decided 2026-11-20, documented exception to the sensor rule in
  `../02-gameplay-rules.md`): the first versions used accelerometer tilt (18°, then a filtered
  15° with an on-screen indicator). The device playtest showed it was too hard: sideways tilts
  were missed when the phone did not pass through neutral, and coming back from a forward tilt
  overshot into unwanted back hops. Lane crossing needs precise, instant hops.
- Debug builds: arrow keys / WASD too (emulator).

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
- None needed: touch only, no sensor and no permission.

## Feedback
- A short, light vibration on each hop; a longer one when the run ends early. Honours
  Settings → Vibration.

## Implementation
- `feature/game/rush`: `ClassRushEngine` (pure rules: lanes, hops, hazards, timer, score;
  JVM tests), `swipeHop` (finger gesture → hop; JVM tests), `ClassRushGame` (16 ms loop on
  `hostScope`), `ClassRushScreen` (Canvas: lanes, cars, tram + warning light, floes, student,
  HUD with time left and rows; tap/swipe input), `ClassRushThumbnail`.
- **Collisions use exactly the drawn shapes** (playtest 2026-11-20: the first hit box was
  0.3 cell wider than the student, so cars "hit" before touching). Student body radius =
  `PLAYER_HALF` (0.26 cell); cars and trams collide with their drawn body (`CAR_INSET`, 0.08
  cell in from each end); you stand on a floe up to its drawn edge (`FLOE_INSET`). Hops animate
  in 60–70 ms so what you see matches where the engine puts you. Debug builds log how each run
  ended (`ClassRush`).
- Game id `06-class-rush`; accent colour `GameAccents.Rush`.
