# 03 — Screens

> Portrait only, English copy. Flow: onboarding (first launch) → main → game → result; main → ranking/history.

## 3.1 Main Screen (defined)
Required elements:
1. Game title.
2. Thumbnail / preview image.
3. Short description of how to play.
4. **Play** button (starts the daily game / consumes an attempt per rules in `02-gameplay-rules.md`).
5. **Seasons History** button (opens ranking / seasons history: past 7-day seasons + historical leaderboard).

Also visible (per global rules):
- Remaining attempts indicator.
- Debug reset-attempts button (test phase only).

Implementation (Phase 2, Direction A — `HomeRoute`/`HomeScreen` in `:feature:home`):
- Top bar with the "RM GAMES" wordmark and the settings gear (→ §3.6).
- Season strip (season N, day d of 7, the player's season points).
- "Today's game" header with a countdown chip ("Ends in HH:MM:SS"; "Next game in" when locked).
- `GameCard`: thumbnail (generated fallback until each game ships art), sensor chip, duration, title, how-to (the game's `description`).
- Fixed bottom area, always reachable: attempts pips + text, best of today, the big **Play** button; **Seasons history** button once the Ranking screen exists (plan 09 PR 13).
- Locked (0 attempts): Play becomes a disabled "Back at 12:00" button + "Next game in" countdown + best of the day (§3.5).
- Debug builds only: "Reset attempts (debug)" + a launcher for every registered game (also the ones out of the rotation).
- State refreshes on resume and at the 12:00 boundary.

## 3.2 Nick Onboarding (first launch only)
- Shown once, before anything else, when no nick is bound to this install.
- Nick input + Continue button. Rules: 3–12 chars (letters/numbers/underscore), unique (rejected if taken), set once, never editable.
- Requires connection (one-time Firestore uniqueness reservation); results bind to the Anonymous Auth UID from then on.

## 3.3 Result Screen (after each run)
- Run score, daily best, remaining attempts.
- Provisional day position + provisional points it would award.
- Play again button (if attempts remain) else lock state (see 3.5).

Implementation (Phase 2 — `SessionRoute` in `:feature:session`, run logic in `SessionFlow`):
- Before each run: a "How to play" intro (GameCard + attempts + **Start**). Start spends the attempt; with none left the intro is locked.
- 3-2-1 countdown overlay with haptics, then the game starts (frozen `SessionGameScope` contract unchanged); non-own-HUD games get a host score chip.
- Back during the countdown or the run asks to confirm, then forfeits the attempt and records nothing.
- On finish the game's end state holds ~1 s ("Run over"), then the Result: "New best" stamp + confetti when the run beat today's best, the score counting up (one line, shrinks to fit), daily best and attempts tiles, **Play again** (straight to the countdown) or the locked "Back at 12:00" + countdown, and Back to home.
- A run belongs to the day it started even if it ends after 12:00. Runs with no score record nothing.
- Provisional position/points: shown as a note until online ranking lands (plan 09 PR 16) — no local data to rank against yet.

## 3.4 Ranking / Seasons History
- Tabs: **Season** (current standings with nicks + points, plus my day-by-day breakdown) and **History** (past seasons + all-time total).
- Top-50 paginated reads (Firestore quota).

## 3.5 Locked / No-Attempts State
- Not a separate screen: Play disabled + countdown to next 12:00 noon + best of the day.

## 3.6 Settings
Opened from the gear on the Main screen (`SettingsRoute` in `:feature:settings`, plan 09 Decision 3). Groups, each a card of list rows:
- **Sound**: Music volume (slider, saved on release; 0 % = off) and Sound effects (on/off). Applied by `:core:audio` (plan 09 PR 7).
- **Feedback**: Vibration (on/off) — already drives button and countdown haptics (`LocalUiFeedback`).
- **Player**: Nick, read-only. Until onboarding exists (PR 15) it explains the nick is chosen once and never editable.
- **Privacy**: everything is stored only on this phone; no account, no personal data (update when online ranking lands).
- **Credits & licences**: every bundled asset with its licence — must match `CREDITS.md` (unit-tested).
- **Debug** (debug builds only): reset today's attempts.
- App version at the bottom. Settings persist through `SettingsRepository` (docs/04 "Data layer").

## Reserved / To Define
- [ ] Game screen per minigame (HUD per `games/` spec; timer; sensor feedback).

All screens: portrait only, English copy.
