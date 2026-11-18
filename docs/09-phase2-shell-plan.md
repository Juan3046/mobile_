# 09 — Phase 2 Plan: App Shell, Look & Feel, Audio

> Status: **proposal** (2026-11-18). Owner: P2 (Onion Roll is done). Covers the
> screens around the games (main, onboarding, result, ranking, settings), the
> daily game rotation, the visual identity and the app-wide audio. Games keep
> being built in parallel by P1/P3/P4 — this plan only touches shared modules.
> Items marked **DECISION** need the team's OK and a doc update before code.

## 0. Starting point (what `main` has today)

| Area | State |
|---|---|
| Games | 4 of 5 playable (Onion, Blink, Compass, Soda); Escape is still the Phase 0 skeleton. |
| `:app` | `MaterialTheme` default, `home` + `session/{gameId}` routes, permission gate. No `res/` (no app icon, no splash, no theme XML). |
| `:feature:home` | Phase 1b launcher: a plain list of all games with PLAY buttons. |
| `:feature:session` | Host for one run + placeholder `score: N` / `Run finished` texts. No timer, attempts or result screen. |
| `:core:data` | Pure stubs: `DayClock` (noon boundary), `DailyGameSelector` (`day % count`), in-memory attempts, score/season interfaces. No Room, DataStore or Firebase yet. |
| `:core:common` | Contracts only — no theme, no reusable UI. |
| Audio | Only Onion Roll's own synthesized SFX (in its module). |

## 1. Decisions to take first (docs before code — AGENTS.md §4)

1. **DECISION — Music in the MVP.** `06-out-of-scope-roadmap.md` excludes
   *per-minigame* soundtracks. Proposal: allow **app-level music** — one menu
   theme + one shared in-game loop + short stingers (new best, lock) — and keep
   dedicated per-game soundtracks post-MVP. Update `06` and `02` (Audio rules).
2. **DECISION — "Random" daily game.** `01-product-overview.md` fixes
   `index = daysSinceEpoch % gameCount` (a visible fixed cycle). Proposal:
   **seeded shuffle per season** — each 7-day season shuffles the game list with
   the season index as seed, so the daily game *looks* random, is still the same
   on every device and offline, and no game repeats inside a season while
   `gameCount ≥ 7`. Update `01` + `DailyGameSelector` docs.
3. **DECISION — Settings screen** (new `03-screens.md` §3.6): music volume,
   SFX on/off, vibration on/off, nick (read-only), credits & licences, privacy
   note; debug section (reset attempts, force daily game) in debug builds only.
4. **DECISION — Ownership.** P2 owns the Phase 2 shell: `:app` nav, `:feature:home`,
   `:feature:session` (timer, attempts, result), `:core:common` theme/UI kit,
   `:core:data`, and a new `:core:audio`. Update `08-implementation-plan.md`
   (ownership + "Phase 2 starts for the shell while 1b continues") and the
   "Current phase" line in `AGENTS.md`.
5. **DECISION — Game thumbnails.** `GameEntry` has no thumbnail. Proposal: add a
   backward-compatible `thumbnail: @Composable () -> Unit = {}` (same pattern as
   `drawsOwnHud`); each game owner fills theirs later; the shell shows a
   generated fallback (title + colour + icon) until then.
6. **DECISION — Asset sources & licences.** Fonts: Google Fonts under OFL,
   bundled in `res/font`. Music/SFX: CC0 or royalty-free with a written licence,
   or generated with a tool whose terms grant commercial use; every file listed
   in a `CREDITS.md` + the Settings credits page. No copyrighted material.

## 2. Workstreams

### A. Visual identity & design system (`:core:common`, `:app` resources)
- **Direction first, code second**: mood board → 2–3 visual directions as
  mockups (main + result screens) → team picks one.
  **DECIDED (2026-11-18): Direction A · Chunky Cabinet** — deep plum ground, hot-pink
  primary, sun-yellow reserved for “new best”; Bungee (display, scores) + Rubik (UI);
  medium radii with a 2px dark outline sitting on a 4px hard “ledge” that compresses on
  press; dark-first with a light variant. Spec, tokens and component catalogue live in
  [`design-system/`](../design-system/README.md) (`concept.html` = Main + Result screens).
  Directions B (Soft Tonal Pop) and C (Scoreboard Grid) were explored and discarded.
  PR 3 ports these tokens to `RmTheme`.
  **Status (2026-11-18): PR 3 done** — `RmTheme` (`:core:common` `theme/`: `RmColors` +
  `RmPalette`, `rmTypography`, `RmShapes`, `RmDimens`, `RmMotion`, WCAG `Contrast` checks in
  unit tests) wraps `MaterialTheme` in `:app`; fonts bundled and listed in `CREDITS.md`.
  **Status (2026-11-18): PR 4 done** — UI kit in `:core:common` `ui/`: `LedgeSurface` (the hard
  ledge), `RmButton` (filled/tonal/outlined/text, default/xl), `RmIconButton`, `RmCard`,
  `GameCard` + `GameThumbnail` fallback + `GameChip`, `AttemptsPips`, `CountdownChip` +
  `rememberRemaining`, `ScoreCounter`, `NewBestStamp`, `Confetti`, `StatTile`, `SeasonStrip`,
  `SectionHeader`, `RmTopBar`, `RmTransitions`, `RmIcons` (ported from the sprite). Click
  feedback goes through `LocalUiFeedback` (silent until `:core:audio` provides it). Logic is
  JVM-tested; `UiKitPreviews.kt` renders Main + Result in Android Studio. Arcade/playful, high
  contrast, readable at a glance, portrait, dark-first with a light variant.
- **Tokens**: colour scheme (brand + per-game accent colours), typography
  (one display font for titles/scores + one UI font), shapes, spacing, elevation,
  motion durations/easings. Implemented as `RmTheme` in `:core:common`.
- **UI kit** (`:core:common/ui`): `RmButton` (press scale + haptic + click SFX),
  `GameCard`, `AttemptsPips`, `CountdownChip`, `ScoreCounter` (count-up),
  `SectionHeader`, `RmTopBar`, `Confetti`, screen transition specs.
- **App chrome**: adaptive app icon, Android 12 SplashScreen API, status/nav bar
  colours, edge-to-edge, portrait lock (already).

### B. Audio system (new module `:core:audio`)
- `AudioDirector` facade: menu music, in-game loop, stingers, UI SFX.
  Music via `ExoPlayer`/`MediaPlayer` with fade/cross-fade; SFX via `SoundPool`.
- Rules: respects Settings (volume/mutes) and **audio focus**; pauses on
  background (lifecycle); ducks music while a game runs; **never blocks the
  main thread** (lesson from Onion Roll: AudioTrack calls froze the game loop).
- Games keep their own SFX; they read the user's SFX/vibration settings through
  a small `FeedbackSettings` flow exposed by `:core:audio` (opt-in, no change to
  the frozen `Game`/`GameScope` contracts).

### C. Data & daily loop (`:core:data`)
- `DailyGameSelector` → seeded season shuffle (Decision 2), TDD.
- `AttemptRepository` on DataStore (2/day, noon reset via `DayClock`).
- `ScoreRepository` on Room: attempts history, daily best, local best per game.
- `SettingsRepository` on DataStore (Decision 3).
- `SeasonRepository`: 7-day windows, points `(N−p+1)×25` (pure logic exists in
  `SeasonPoints`), auto-archive.
- Firebase (Anonymous Auth, nick reservation, daily-best upload, top-50
  ranking) as its own later stream (E), behind interfaces so screens work offline
  with fakes first.

### D. Screens (`:feature:home`, `:feature:session`, new `:feature:ranking`, `:feature:settings`, `:feature:onboarding`)
1. **Main** (`03` §3.1): today's game hero card (thumbnail, title, how-to),
   big Play, attempts pips, lock state with countdown to 12:00 + best of the day,
   Seasons History button, settings gear, season progress strip, debug reset.
2. **Pre-run**: how-to card + 3-2-1 countdown (sound + haptic), then the game.
3. **Result** (§3.3): score count-up, "new best" celebration, daily best,
   attempts left, provisional position/points, Play again or lock.
4. **Ranking / Seasons History** (§3.4): Season / History tabs.
5. **Settings** (§3.6, Decision 3).
6. **Nick onboarding** (§3.2) — UI first with a fake uniqueness check; real
   check lands with Firebase (stream E).

### E. Backend (Firebase, Spark plan)
Anonymous Auth, `nicks/{nick}` reservation, daily-best write (1/day/UID),
paginated top-50 reads, security rules. Separate PRs after D.1–D.3 work offline.

## 3. Order of work (small PRs, one module each, TDD)

| # | PR | Module(s) | Depends on |
|---|---|---|---|
| 1 | Docs: decisions 1–6 + ownership + this plan | `docs/`, `AGENTS.md` | team OK |
| 2 | Visual direction mockups (not code) — `design-system/` — **done** | — | 1 |
| 3 | `RmTheme` tokens + fonts — **done** | `:core:common`, `:app` | 2 |
| 4 | UI kit components (+ screenshot-free unit tests for logic) — **done** | `:core:common` | 3 |
| 5 | Seeded season shuffle selector | `:core:data` | 1 |
| 6 | DataStore attempts + settings repos | `:core:data` | 1 |
| 7 | `:core:audio` (director, settings flow, focus, no main-thread work) | `:core:audio` | 6 |
| 8 | Main screen (daily game, attempts, lock + countdown) | `:feature:home`, `:app` nav | 4, 5, 6 |
| 9 | Session: pre-run countdown, attempt consumption, result screen | `:feature:session` | 4, 6 |
| 10 | Room scores + daily best + local best | `:core:data` | 6 |
| 11 | Settings screen | `:feature:settings`, `:app` nav | 4, 6, 7 |
| 12 | Music & UI SFX wiring across screens | `:app`, features | 7, 8, 9 |
| 13 | Ranking / Seasons History (local data first) | `:feature:ranking` | 10 |
| 14 | App icon, splash, edge-to-edge | `:app` | 3 |
| 15 | Firebase: auth + nick onboarding | `:core:data`, `:feature:onboarding` | 8 |
| 16 | Firebase: daily-best upload + ranking reads | `:core:data`, `:feature:ranking` | 13, 15 |
| 17 | Thumbnails contract + fallback (Decision 5); owners add theirs | `:core:common`, games | 4 |

Exit of Phase 2 (`08`): full daily loop works offline with 2 attempts enforced,
plus the screens above styled with `RmTheme` and app-wide audio honouring Settings.

## 4. Tooling

- Claude plugins: **Design** (design-system, critique, UX copy, accessibility
  review), **Mobile Design System** (Material 3 token + component catalogue and
  screen concepts as HTML — fast visual direction before Compose), **UI Design
  Review** (senior-designer review of rendered screens/screenshots),
  **kotlin-lsp** (Kotlin diagnostics), **firebase** (stream E).
- Music/SFX generation (e.g. Creative Claw) only with a licence that allows
  commercial use; otherwise curated CC0 libraries. Record every asset in credits.
- Every screen is checked on a real device (screenshots via `adb`) before review.

## 5. Risks

- **Shared-module churn while games are in flight**: contract changes stay
  backward-compatible with defaults (as `drawsOwnHud`); announce in the PR.
- **Scope creep from "life" features** (music, particles, animations): every
  effect is opt-out in Settings and must keep 60 fps on the test device.
- **Audio licensing**: no asset merges without its licence recorded.
- **Escape Poruba** is still a skeleton — the daily rotation must tolerate a game
  that is not ready (skip unready games until it lands).
