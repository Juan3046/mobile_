# Games Index — RM Games

One file per minigame. This folder is the source of truth for game specs.
Global rules (attempts, duration, seasons, scoring) live in `docs/02-gameplay-rules.md`
and always take precedence unless a game doc explicitly documents an approved exception.

| # | File | Game | Status |
|---|------|------|--------|
| 01 | `01-escape-poruba.md` | Escape Poruba (2D arcade runner) | Defined |
| 02 | `02-onion-roll.md` | Onion Roll (gyroscope, checkout belt) | Defined |

Season 1 needs 7 defined games: **2 / 7 done**.

## Conventions

- File name: game number + lowercase kebab-case (e.g. `02-onion-roll.md`).
- Games are referred to by their number (Game 01, Game 02, …) matching the index table.
- All content in English.
- Portrait orientation only; 30–90 seconds per run.
- Sensor-first controls; touch allowed only as a documented exception.
- Keep the global 2-attempts/day rule (document any exception as open question).

## Required Sections (per `docs/02-gameplay-rules.md`)

Every new game doc must define:

1. **Concept** — title, thumbnail/preview, short description, how to play.
2. **Controls** — sensor(s) used + permissions required (or touch, with justification).
3. **Format** — duration (30–90s), orientation, camera/scroll style.
4. **Rules & Scoring** — win/lose conditions, score mapping to daily best / season.
5. **Fallback** — behavior if a sensor is missing or permission is denied
   (Play must be blocked with a clear message; never record fake scores).
6. **Persistence** — what is saved locally (MVP is local-only, offline).
7. **Open Questions** — unresolved items, marked `[x]` when decided.

See `01-escape-poruba.md` for a full example.
