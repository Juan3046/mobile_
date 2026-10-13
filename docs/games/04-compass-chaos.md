# 04 — Minigame 04: COMPASS CHAOS

## Concept
A fast-paced directional challenge. The player must physically spin their body and device to face a target cardinal direction (N, S, E, W) before the time runs out.

## Format & Camera
- Portrait only, 2D minimalist UI.
- Central compass needle and a highlighted target zone.

## Controls
- **Magnetometer only**.
- No touch input during gameplay.

## Rules
- The game calls out a direction (e.g., "POINT EAST!").
- The player has a short time window (e.g., 3 seconds) to align the phone's top edge with that magnetic direction.
- Failure to align in time = Game Over.

## Scoring
- **Score = successful turns survived** (higher is better). One run = one attempt (global 2-attempts/day, best of 2); the daily best ranks the day for position points (never summed).

## Fallback (missing sensor / permission denied)
- Per global rule: **disable Play with a clear message** ("Compass Chaos requires a magnetometer") and never record fake scores.

## Persistence (local-first MVP)
- Best turns survived, attempts used today; daily best uploads once for the shared ranking.

## Difficulty Curve
- 0-3 successful turns: 3.0 seconds per turn. 90-degree turns.
- 4-7 successful turns: 2.0 seconds per turn. 180-degree turns introduced.
- 8+ successful turns (Hell Mode): 1.2 seconds per turn. Rapid, unpredictable direction changes.

## Open Questions
- [x] Decided: score = successful turns survived; magnetometer block + message fallback; local-first persistence.
- [ ] Run duration: total run length undefined — needs a fixed duration (standard 30–90s) or approved exception.