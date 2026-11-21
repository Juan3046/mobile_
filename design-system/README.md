# RM Games — Design System (Direction A · Chunky Cabinet)

Visual identity for the Phase 2 shell (`docs/09-phase2-shell-plan.md` §2A, PR 2).
**Not app code** — a static HTML spec that `RmTheme` (PR 3) and the UI kit (PR 4)
implement in Compose. Chosen direction: **A · Chunky Cabinet** (decided 2026-11-18;
B “Soft Tonal Pop” and C “Scoreboard Grid” were explored and discarded).

Reference guideline: **Material 3** (Android) — its colour roles, 15-style type scale,
shape scale, elevation levels, state layers and component catalogue. Dark-first,
with a light variant.

## Open it

The pages are self-contained (no build). Open `concept.html` (the decision page) or
`index.html` (the catalogue) in a browser. If your browser blocks the local CSS under
`file://`, serve the folder:

```bash
python -m http.server 8765 --directory design-system
```

Then visit `http://localhost:8765/concept.html`. `?theme=light` preselects the light
theme on `index.html`.

| File | What it is |
|---|---|
| `concept.html` | **Start here.** The Main screen (03 §3.1) and Result screen (03 §3.3) in dark (default) and light. |
| `index.html` | Living style guide: foundations + every Material 3 component + the RM Games UI kit (GameCard, AttemptsPips, CountdownChip, ScoreCounter, StatTile, SeasonStrip, SectionHeader). Dev Mode inspector + **Copy as prompt** (Jetpack Compose first). |
| `icons.html` | Canonical inline SVG icon sprite (copied into the other pages). |
| `styles.css` | Token layer (dark + light) + component classes. Single source of styles. |
| `theme.js` | Shared dark/light switch for all pages (remembered per browser). |

## Direction A · Chunky Cabinet

> For a 60-second daily ritual, chunky outlined shapes standing on a hard ledge make
> every tap feel like pressing a real arcade button.

| | Dark (default) | Light |
|---|---|---|
| Ground | Deep plum `#17121F` | Lilac-white `#FBF7FF` |
| Primary (only interaction family) | Hot pink `#FF5D8F` / on `#2B0011` | Raspberry `#D61F5F` / on `#FFFFFF` |
| Celebration (tertiary, “new best” only) | Sun yellow `#FFD23F` | Sun yellow `#FFD23F` |
| Ledge / outline | `#07050A` | `#1C1524` |

- **Type:** Bungee (display, scores, Play) + Rubik (everything functional).
- **Shape:** radii 6 / 10 / 14 / 18 / 24 / full; 2px dark outline on buttons and cards.
- **Depth:** a solid 4px offset “ledge”, no blur; pressed drops to 1px and shifts 3px.
- **Restraint:** outline + ledge only on primary actions and cards — chips, list rows and
  the app bar stay flat so screens don't get busy.
- **Icons:** rounded caps/joins, 2.25 stroke.

All fonts are Google Fonts under the SIL Open Font License (Decision 6). Per-game
accent colours (`--game-*`) only fill the thumbnail fallback (Decision 5) and the game chip.

## Rules baked into the tokens

- **One interaction family** (primary). Tertiary is reserved for celebration (“new best”).
- **Every fill ships its `--on-*` partner**, WCAG-checked by math: body text ≥ 4.5:1,
  large text/UI ≥ 3:1, in both themes. One documented exception: the yellow “new best”
  stamp on the light ground is below 3:1 as a fill, so it always carries the dark outline.
- Numbers (scores, timers, ranks) always use `--font-num` with tabular figures.
- Touch targets ≥ 48dp; attempts are never shown by colour alone (pips + text).

## Extending

- **Add a token**: add the CSS variable in the right group (colour → both palette
  blocks, dark and light; shape/type/elevation → the direction block; shared → the
  `:root, [data-theme]` block). Reference it by name; never inline a value.
- **Add a component**: a class block in `styles.css` using tokens only, a
  `<section id="key">` in `index.html` with every variant/state, and a `SPECS.key`
  entry (Dev Mode + Copy as prompt pick it up automatically).
- **Port to Compose**: PR 3 turns these tokens into `RmTheme` in `:core:common` — the
  “⤓ Install design system” button in `index.html` copies a Compose prompt with the live
  token values.
- Var references must stay resolvable per scope: aliases live in a
  `:root, [data-theme]` block so a nested `[data-theme]` wrapper re-computes them.
