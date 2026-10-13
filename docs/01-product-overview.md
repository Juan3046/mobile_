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
- **Day boundary: 12:00 noon local.** Each day runs noon-to-noon: new daily game + 2 fresh attempts at 12:00. You can play at any moment within the day.
- **No position locks or closes.** Positions and season points are always live from the stored standings. Each day's score is writable only during its own day, so past days stay frozen once the boundary passes.
- **Seasons History screen** (opened from main-menu button) shows: past seasons list + current season progress + all-time total.
- **Player identity (MVP)**: Firebase Anonymous Auth UID + user-chosen nick at first launch. The nick is what appears on the shared ranking. No login, no personal data.
- **Nick rules:** unique across players (no repeats), set once before playing, never editable afterwards. Every result is stored under the UID bound to that nick. First launch requires connection (one-time uniqueness check via a `nicks/{nick}` reservation); afterwards the app works offline-first.
- **Daily game selection**: deterministic by date — `index = daysSinceEpoch % gameCount` (currently 5), same game on all devices, no server needed. Repetition within a 7-day season is accepted until 7 games exist.
- **Season rollover: auto-archive.** After day 7, the season closes, its score is archived to Seasons History, and a new empty season starts automatically.

## Open Questions
- [x] Decided: daily rotation (deterministic by date, same-for-all); scoring (daily best ranks the day, season = sum of position points `(N−p+1)×25`, ties share, noon-to-noon days, no locks); identity (Anonymous Auth UID + chosen nick); backend (Firebase Spark free + local-first cache).
- [ ] Minigame library: 5 drafted (01–05, see `games/`); 2 more needed for season 1; 04–05 still draft.
