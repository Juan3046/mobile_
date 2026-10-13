# 04 — Minigame 04: COMPASS CHAOS

## Concept
A fast-paced directional challenge. The player must physically spin their body and device to face a target cardinal direction (N, S, E, W) before the time runs out.

> **Approved duration exception:** fixed 20s round, outside the standard 30–90s rule (see `docs/02-gameplay-rules.md`).

## Format & Camera
- Portrait only, 2D minimalist UI.
- Central compass needle and a highlighted target zone.
- **Fixed 20s round.** Random directions are requested back-to-back until time expires.

## Controls
- **Magnetometer only**.
- No touch input during gameplay.

## Rules
- The game calls out a random direction (e.g., "POINT EAST!").
- Align the phone's top edge with that magnetic direction within the window to score; a missed window simply scores nothing and the next direction is called.
- No game over — the run always lasts the full 20s. Most correct directions wins.

## Scoring
- **Score = correct directions in 20s** (higher is better). One run = one attempt (global 2-attempts/day, best of 2); the daily best ranks the day for position points (never summed).

## Fallback (missing sensor / permission denied)
- Per global rule: **disable Play with a clear message** ("Compass Chaos requires a magnetometer") and never record fake scores.

## Persistence (local-first MVP)
- Best turns survived, attempts used today; daily best uploads once for the shared ranking.

## Difficulty Curve (time-based, fits the 20s round)
- 0–7s: 3.0s per direction, 90-degree turns.
- 7–14s: 2.0s per direction, 180-degree turns introduced.
- 14–20s (Hell Mode): 1.2s per direction, rapid unpredictable changes.

## Open Questions
- [x] Decided: fixed 20s round (duration exception); score = correct directions, miss = no point, no game over; magnetometer block + message fallback; local-first persistence.