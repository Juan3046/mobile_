# 02 — Gameplay Rules

## Session Model
- One playable game per day.
- **Daily reset: 12:00 noon local time.** New daily game + 2 fresh attempts. Playable at any moment of the day.
- **Max 2 attempts per day per player.**
- **Score rule: best of 2 counts.** Only the highest of the 2 daily attempts counts for season + history.
- **Daily best ranks the day:** the best attempt's raw game score determines the day's position among all players; the position converts to season points via `(N−p+1)×25` (see `01-product-overview.md`). Raw scores are never summed across games.
- **Shared ranking upload:** the daily best is written to Firestore once per day per player (each UID writes only its own doc); attempts enforcement stays local.
- UI must give **feedback on remaining attempts** (e.g., "1 attempt left").
- **Exhausted state: lock + countdown.** When attempts are used up, Play is disabled; show last/best score + countdown to next daily game.
- **Practice runs (debug only)**: the Main screen's debug launcher can open any game; a game that is not today's runs as a practice run — no attempt used, no score recorded — so stored scores and the Seasons history only ever contain real daily runs.
- **Reset attempts button**: debug-only, resets today's attempt counter only. Past scores and seasons are untouched. Lives on the main screen as a small `Reset attempts (debug)` control; easy to remove/disable for release.

## Duration & Format
- **Standard games last 30–90 seconds.**
- **Approved exception: Soda Shake (05)** — short variable-length single round (ends at 20 shakes).
- **Approved exception: Compass Chaos (04)** — fixed 20s round.
- **Approved exception: Poruba Casino (01)** — unlimited duration; run ends when balance
  drops below the minimum 10-banana wager.
- **Approved exception: Onion Roll (02)** — endless survival run with no time
  cap (approved after playtest, 2026-11-10); the ever-accelerating belt ends
  every run. Score = whole seconds survived. See `games/02-onion-roll.md`.
- **Portrait orientation only.**
- **No pause.** Timer keeps running; leaving the game forfeits the attempt.
- Short rules, intuitive gameplay; each game shows a brief how-to-play.

## Audio
- **App-level audio is in the MVP** (plan 09 Decision 1, decided 2026-11-18): one menu music loop + short UI sounds (click, 3-2-1 tick, "go", new-best fanfare). Dedicated per-game soundtracks stay post-MVP (`06`).
- **Synthesized in code** (`:core:audio` `SoundBook`): no audio files, so no licences to track (Decision 6). Any sound can later be replaced by a licensed file without touching screens.
- Rules (`AudioDirector`): music plays only on menu screens (Main, Settings, intro, Result) at the Settings volume (0 % = off); it fades out during the countdown and the run (games keep their own sounds) and returns on the Result; it pauses in the background and respects audio focus (another app's sound silences or ducks it). UI sounds obey the Sound effects switch; haptics obey Vibration. **Games' own sounds and vibrations obey the same switches**: they read `LocalUiFeedback.current.sfxEnabled` / `.hapticsEnabled` (Onion Roll does; any new game with its own feedback must too).
- All audio work runs on its own thread, never the main thread.

## Controls
- Focus on **mobile sensors/camera** (camera, gyroscope, accelerometer); touch controls are allowed only as a documented game-specific exception.
- Touch as fallback only where strictly needed (to be specified per minigame).
- **Missing sensor / permission denied: block + message.** Disable Play for that game with a clear explanation; never record fake scores.

## Minigames
- Library of minigames in `games/`. Each minigame must define:
  - Title, thumbnail, short description.
  - Sensor(s) used + permissions required.
  - Duration (standard 30–90s; exceptions only if documented and approved), scoring, win/lose conditions.
  - Fallback behavior if sensor unavailable/permission denied.

## Open Questions
- [x] Decided: best of 2 counts; lock + countdown when exhausted.
- [x] Decided: local-only MVP, so offline works by default.
- [ ] Minigame library (first season needs 7 defined games).
