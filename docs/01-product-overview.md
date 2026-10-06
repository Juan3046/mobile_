# 01 — Product Overview — RM Games

## Concept
Mobile app called **RM Games** that offers a short daily game.

## Goals
- Quick, intuitive fun in under 90 seconds.
- Sensor-driven gameplay (differentiator vs. touch-only games).
- Daily retention via attempts limit + historical ranking + seasons.

## Seasons & Ranking
- **Historical leaderboard**: accumulates scores across games/seasons (all-time total of season scores).
- **Seasons**: each season lasts **7 days**.
- **Season score = sum of daily bests** (max 7 entries, one best per day). Missed days count as 0.
- **Seasons History screen** (opened from main-menu button) shows: past seasons list + current season progress + all-time total.
- **Player identity (MVP)**: anonymous device ID (random UUID stored on device, no login).
- **Daily game selection**: same game for all devices each day. Random without repetition within a season; prioritize games not played for the longest time (least-recently-played first).
- **Season rollover: auto-archive.** After day 7, the season closes, its score is archived to Seasons History, and a new empty season starts automatically.

## Open Questions
- [x] Decided: daily rotation (same-for-all, no-repeat-in-season, least-recent first); scoring (sum of daily bests); identity (anonymous device ID); storage (local-only).
- [ ] Minigame library: #1 Escape Poruba + #2 Onion Roll + #3 Blink Sniper defined (see `games/`); 4 more needed for season 1 (see `games/README.md`).
