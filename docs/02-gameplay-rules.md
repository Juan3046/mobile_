# 02 — Gameplay Rules

## Session Model
- One playable game per day.
- **Daily reset: local midnight (00:00 / 12am device time).** New daily game + 2 fresh attempts.
- **Max 2 attempts per day per player.**
- **Score rule: best of 2 counts.** Only the highest of the 2 daily attempts counts for season + history.
- UI must give **feedback on remaining attempts** (e.g., "1 attempt left").
- **Exhausted state: lock + countdown.** When attempts are used up, Play is disabled; show last/best score + countdown to next daily game.
- **Reset attempts button**: debug-only, resets today's attempt counter only. Past scores and seasons are untouched. Lives on the main screen as a small `Reset attempts (debug)` control; easy to remove/disable for release.

## Duration & Format
- **All games last 30–90 seconds.**
- **Portrait orientation only.**
- **No pause.** Timer keeps running; leaving the game forfeits the attempt.
- Short rules, intuitive gameplay; each game shows a brief how-to-play.

## Controls
- Focus on **mobile sensors** where possible (camera, gyroscope, accelerometer), but touch-drag is allowed when the game design requires it (e.g. Escape Poruba).
- Touch as fallback only where strictly needed (to be specified per minigame).
- **Missing sensor / permission denied: block + message.** Disable Play for that game with a clear explanation; never record fake scores.

## Minigames
- Library of minigames TBD. Each minigame must define:
  - Title, thumbnail, short description.
  - Sensor(s) used + permissions required.
  - Duration (30–90s), scoring, win/lose conditions.
  - Fallback behavior if sensor unavailable/permission denied.

## Open Questions
- [x] Decided: best of 2 counts; lock + countdown when exhausted.
- [x] Decided: local-only MVP, so offline works by default.
- [ ] Minigame library (first season needs 7 defined games).
