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
- **Season score = sum of daily position points.** Each game's own score only ranks the day; it is never added directly.
- **Daily points formula:** if `N` players submitted a score that day, position `p` (1-indexed) earns `(N − p + 1) × 25`. Examples: N=4 → 100/75/50/25; N=6 → 150/125/100/75/50/25. Last place always 25; day not played = 0.
- **Positions:** standard competition ranking — position = 1 + number of players strictly better. Tied players share the same position and points; the next position is skipped.
- **Day close: standings for day D lock at 12:00 noon local on D+1** (12h grace so offline devices can sync). Attempts still reset at 00:00 midnight. During the day and morning after, the position shown is provisional; points finalize from the locked standings (computed on next app open after the lock). Missed days score 0.
- **Seasons History screen** (opened from main-menu button) shows: past seasons list + current season progress + all-time total.
- **Player identity (MVP)**: Firebase Anonymous Auth UID + user-chosen nick at first launch. The nick is what appears on the shared ranking. No login, no personal data.
- **Daily game selection**: deterministic by date (rotating index over the game list) — same game on all devices, no server needed. Within-season no-repeat follows from rotation; least-recently-played kept as tiebreak rule for future seasons.
- **Season rollover: auto-archive.** After day 7, the season closes, its score is archived to Seasons History, and a new empty season starts automatically.

## Open Questions
- [x] Decided: daily rotation (deterministic by date, same-for-all); scoring (daily best ranks the day, season = sum of position points `(N−p+1)×25`, ties share, standings lock at 12:00 noon next day); identity (Anonymous Auth UID + chosen nick); backend (Firebase Spark free + local-first cache).
- [ ] Minigame library: 5 drafted (01–05, see `games/`); 2 more needed for season 1; 04–05 still draft.
