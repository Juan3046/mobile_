# 02 — Gameplay Rules

## Session Model
- One playable game per day.
- **Daily reset: local midnight (00:00 / 12am device time).** New daily game + 2 fresh attempts.
- **Max 2 attempts per day per player.**
- **Score rule: best of 2 counts.** Only the highest of the 2 daily attempts counts for season + history.
- **Daily best ranks the day:** the best attempt's raw game score determines the day's position among all players; the position converts to season points via `(N−p+1)×25` (see `01-product-overview.md`). Raw scores are never summed across games.
- **Shared ranking upload:** the daily best is written to Firestore once per day per player (each UID writes only its own doc); attempts enforcement stays local.
- UI must give **feedback on remaining attempts** (e.g., "1 attempt left").
- **Exhausted state: lock + countdown.** When attempts are used up, Play is disabled; show last/best score + countdown to next daily game.
- **Reset attempts button**: debug-only, resets today's attempt counter only. Past scores and seasons are untouched. Lives on the main screen as a small `Reset attempts (debug)` control; easy to remove/disable for release.

## Duration & Format
- **Standard games last 30–90 seconds.**
- **Approved exception: Soda Shake (05) runs ~5s** (single shake-to-100% round). Documented in `games/05-soda-shake.md`.
- **Portrait orientation only.**
- **No pause.** Timer keeps running; leaving the game forfeits the attempt.
- Short rules, intuitive gameplay; each game shows a brief how-to-play.

## Controls
- Focus on **mobile sensors** where possible (camera, gyroscope, accelerometer), but touch-drag is allowed when the game design requires it (e.g. Escape Poruba).
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
