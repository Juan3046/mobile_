# 07 — Minigame 07: SOUND BARRIER

> Sensor-driven game (microphone), part of Season 1 library.

## Concept
A ball drifts through a corridor of moving barriers while the screen advances on
its own. Every barrier is a pair of bars with a gap between them, and the ball has
to be sitting inside that gap by the time the barrier has swept past it. The ball
**rises while the microphone hears sound and falls while it hears nothing**, so the
player literally shouts or claps to climb and goes quiet to drop.

- **Thumbnail / preview**: a small round ball against a dark corridor, a pair of
  glowing bars closing in, and a live sound-level bar at the bottom edge.
- **How to play**: hold a note, clap or shout to lift the ball into the gap; stay
  silent to let it drop. Time the noise so the ball is between the bars when they
  arrive.
- **Objective**: pass as many barriers as possible within 60 seconds.
- **Pass**: **+1 point**, awarded only when the ball is inside the gap at the
  instant the barrier finishes passing it.
- **Fail**: a bar that touches the ball carries it **backwards (left)**; the run
  ends only when the ball is pushed completely off the screen (banner
  `LEFT BEHIND!`). Points already earned are kept.
- **Hitboxes**: the ball and both bars of every barrier have hitboxes. The ball
  occupies `playerRadius` around its centre on each normalized axis, and the bars
  occupy everything except their gap — grazing the rim of an opening counts as
  touching it, which pushes the ball back rather than ending the run.

## Controls & Sensors
- **Microphone (`android.permission.RECORD_AUDIO`)**:
  - The game reads the input **level** only; no audio is recorded, encoded or
    stored (see [Persistence](#persistence-local-only-mvp)).
  - Level at or above the noise threshold → the ball accelerates **upward**.
  - Below the threshold → gravity pulls the ball **downward**.
- **Boundaries**: the floor and the ceiling make the ball **bounce back** instead
  of pinning it. On contact the vertical speed is reversed and scaled by the
  restitution factor (`bounceRestitution`, **0.3** by default since the 2026-11-21 playtest; was 0.8: the ball keeps 30 %
  of its speed, so a fall settles after a couple of rebounds). Running out of sound
  never kills on its own; the ball only loses when a bar pushes it off the screen.
- **Permission**: requires runtime `android.permission.RECORD_AUDIO`.
- **Desktop / Emulator Debug Only**: holding `Space` counts as sound, releasing it
  as silence (stripped from release builds).

## Format
- **Orientation**: portrait only.
- **Duration**: fixed **60-second timer** (within the global 30–90s rule).
- **Scroll style**: the barrier track travels right-to-left on its own; the ball
  starts at the centre of the screen and moves vertically — a bar that touches it
  pushes it backwards (left) instead. No camera view.

## Rules & Scoring
- **One run = one attempt** (global 2-attempts/day rule; best of 2 counts).
- **Pass detection**: a barrier has passed the ball when its **trailing edge**
  crosses the ball's horizontal position. At that instant:
  - ball centre inside the gap → **+1 point**, run continues;
  - otherwise → **no point** (a ball outside its gap is still being carried
    backwards by that bar, so it cannot have finished passing it cleanly).
- **Contact**: the ball's hitbox touching a bar at **any moment** carries the ball
  **backwards (left)** for as long as they touch — contact never ends the run on
  the spot. Slipping into the gap is the way out; a ball carried past the left
  edge of the screen ends the run (`LEFT BEHIND!`), points banked. The player has
  the whole length of the sweep to line the ball up with the opening.
- **Target movement**: barriers travel right to left; each one has its own gap
  size and vertical placement.
- **Win / Finish State**: the timer reaches `0.0s` (`TIME'S UP!`) — the run ends as
  a win and the score is the number of barriers passed.
- **Score Mapping**: score = barriers passed. Daily best = max of up to 2
  attempts; the daily best ranks the day for position points (never summed).

## Difficulty & Progression
Continuous time-based ramp over the 60-second run:

- **0–20s (Warmup)**: base scroll speed, generous gaps, gap centres stay near the
  middle band of the screen.
- **20–40s (Intermediate)**: scroll speed increases, gaps tighten, gap centres
  drift further apart vertically.
- **40–60s (High Intensity)**: fastest scroll, tightest gaps, largest vertical
  swings between consecutive barriers.

## HUD (minimal)
- **Top Left**: score counter (e.g. `SCORE: 18`).
- **Top Right**: countdown timer (e.g. `TIME: 42.5s`).
- **Bottom**: live sound-level indicator showing whether the microphone currently
  counts as loud (this doubles as the "is the mic hearing me?" feedback).

## Game Over & Results Flow
- **On a miss (a bar pushes the ball off the screen)**: the corridor freezes, the
  `LEFT BEHIND!` banner shows and the final score is displayed (retained points).
  The session host then offers **PLAY AGAIN** and **HOME** (docs/03-screens.md —
  Game Screen).
- **On Timer Expiration (60s)**: `TIME'S UP!` + final score summary.
- **Retry**: respects remaining daily attempts (lock + countdown if 0 remain).

## Art Direction
- **Theme**: neon sound corridor / waveform tunnel.
- **Color Palette**: deep background, cyan ball, magenta barriers, amber sound bar.
- **Provisional Graphics**: procedural vector / Canvas shapes (circle ball, rounded
  rectangle bars, level meter) so the code compiles and runs without external asset
  files.

## Fallback (Missing Sensor / Permission Denied)
- Per global rules: if `RECORD_AUDIO` is denied or no microphone is available,
  **disable Play** with a clear explanation:
  *"Sound Barrier requires microphone permission to play."*
- Never record fake scores. No touch fallback in release build.

## Persistence (Local-only MVP)
- Save locally: best score (barriers passed), attempts used today, timestamp.
- **No audio ever leaves memory**: only the live loud/quiet flag feeds the physics;
  nothing is recorded to disk and nothing is uploaded.

## Tuning (device playtest 2026-11-21: "extremely bad")
- The first defaults reacted to quiet talk and room noise (loud at RMS ≥ 0.05) and threw the
  ball across the screen (gravity 2.2, thrust 3.4, terminal speeds 1.6 / 1.4 screen-heights/s,
  bounce 0.8).
- Now: **loud at RMS ≥ 0.15** (a clear voice, hum or clap near the phone) and, once loud, it
  stays loud until the level drops under **0.09** (hysteresis: no flicker at the edge).
  Gravity **1.0**, thrust **1.5**, terminal speeds **0.55 / 0.5**, bounce **0.3**: one second
  of steady sound or silence moves the ball well under half the screen.
- The sound meter is scaled so the loud threshold sits at its **middle mark**: past the line
  = the ball rises.
- Tests: `SoundTuningTest`.

## Open Questions
- [x] Noise threshold calibration — see "Tuning" below (playtest 2026-11-21).
- [ ] Whether a barrier that is *never* reached before `TIME'S UP!` should score.
