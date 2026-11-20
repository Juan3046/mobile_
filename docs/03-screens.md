# 03 — Screens

> Portrait only, English copy. Flow: onboarding (first launch) → main → game → result; main → ranking/history.

Launch: the system splash (app icon on the theme background) stays until saved attempts, scores and settings are loaded. Screens draw edge-to-edge, content kept clear of the status and navigation bars.

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
- Season strip (season N, day d of 7, the player's season points). Points come from the online ranking (`RankingSync.seasonTotal` over this season's played daily-game bests, today's and yesterday's re-uploaded first) and appear once loaded; refreshed on every return to Main and at 12:00. Hidden on builds without Firebase or when no day could be read (no connection); "0" before the first scored day. Once today's game is scored, under "Best today" the Main screen also shows today's position with its season points ("#1 of 2 · 50 pts", provisional until 12:00), read in the same call.
- "Today's game" header with a countdown chip ("Ends in HH:MM:SS"; "Next game in" when locked).
- `GameCard`: thumbnail (generated fallback until each game ships art), sensor chip, duration, title, how-to (the game's `description`).
- Fixed bottom area, always reachable: attempts pips + text, best of today, the big **Play** button; **Seasons history** button once the Ranking screen exists (plan 09 PR 13).
- Locked (0 attempts): Play becomes a disabled "Back at 12:00" button + "Next game in" countdown + best of the day (§3.5).
- **Missing sensor** (docs/02): if this phone lacks the hardware today's game needs, Play becomes a disabled "Not playable on this phone" button with the reason above it ("Soda Shake needs an accelerometer, and this phone doesn't have one."). No attempt is spent. The game's intro (debug launcher too) shows the same reason with Start off and never asks for a permission. Required hardware per game: Onion Roll gyroscope, Blink Sniper front camera, Compass Chaos magnetometer, Soda Shake accelerometer; Poruba Casino none (touch buttons). Logic: `requiredSensors`/`unavailableReason` in `:app` (`SensorGate.kt`) over `AndroidSensorAvailability` (`:core:sensors`).
- Debug builds only: "Reset attempts (debug)" + a launcher for every registered game (also the ones out of the rotation).
- State refreshes on resume and at the 12:00 boundary.

## 3.2 Nick Onboarding (first launch only)
- Shown once, before anything else, when no nick is bound to this install.
- Nick input + Continue button. Rules: 3–12 chars (letters/numbers/underscore), unique (rejected if taken), set once, never editable.
- Requires connection (one-time Firestore uniqueness reservation); results bind to the Anonymous Auth UID from then on.

Implementation (`NickOnboardingRoute` in `:feature:onboarding`, rules in `NickClaim` in `:core:data`):
- Start destination is `onboarding` while `PlayerRepository.nick` is null; Continue → Main, and the screen never shows again.
- Live counter (n/12) and rule hint; errors in plain words: "3–12 characters", "Only letters, numbers and _", "That nick is taken — try another", "No connection. Choosing a nick needs internet once." Uniqueness ignores case (`nickKey`).
- Uniqueness goes through the `NickRegistry` seam. **Until Firebase is connected it is `LocalNickRegistry`** (accepts any valid nick on this device); the Firebase registry replaces it in `AppContainer` only.
- **One nick per player, forever**: if this account (Anonymous Auth UID) already owns a nick — after reinstalling or the debug "Forget nick" — whatever is typed, the app restores the owned nick.
- Settings → Player shows the nick (read-only); Settings → Debug has "Forget nick (debug)" to re-run onboarding.

## 3.3 Result Screen (after each run)
- Run score, daily best, remaining attempts.
- Provisional day position + provisional points it would award.
- Play again button (if attempts remain) else lock state (see 3.5).

Implementation (Phase 2 — `SessionRoute` in `:feature:session`, run logic in `SessionFlow`):
- Before each run: a "How to play" intro (GameCard + a **How to play** card with 3–4 short numbered steps and the goal + attempts + **Start**). The steps live in `gameHowTo()` (`:core:common`, written from each spec in `docs/games/`; added after the 2026-11-20 playtest). Start spends the attempt; with none left the intro is locked.
- 3-2-1 countdown overlay with haptics, then the game starts (frozen `SessionGameScope` contract unchanged); non-own-HUD games get a host score chip.
- Back during the countdown or the run asks to confirm, then forfeits the attempt and records nothing.
- On finish the game's end state holds ~1 s ("Run over"), then the Result: "New best" stamp + confetti when the run beat today's best, the score counting up (one line, shrinks to fit), daily best and attempts tiles, **Play again** (straight to the countdown) or the locked "Back at 12:00" + countdown, and Back to home.
- **Personal best**: when a run beats every earlier day for that game, the stamp reads "Personal best" (confetti + fanfare); otherwise the record is shown under the score. Not shown for the very first score.
- A run belongs to the day it started even if it ends after 12:00. Runs with no score record nothing.
- **Provisional ranking card** (plan 09 PR 16): after a scored real run the daily best is uploaded and the card shows the position with the season points next to it, "#4 of 212 · 5,225 pts" (`(N−p+1)×25`), noting it can change until 12:00. While loading: "Checking today's ranking…"; no connection: the best is kept and uploads next time; builds without Firebase say there is no online ranking; practice runs say nothing was saved.

## 3.4 Ranking / Seasons History
- Tabs: **Season** (current standings with nicks + points, plus my day-by-day breakdown) and **History** (past seasons + all-time total).
- Top-50 paginated reads (Firestore quota).

Implementation, local data first (`SeasonsHistoryRoute` in `:feature:ranking`, plan 09 PR 13; opened from "Seasons history" on Main):
- **Season** tab: season number, dates (noon-to-noon days), season strip, days played, and a day-by-day list (date · game · best, "Not played", "Today", "Upcoming").
- **History** tab: all-time days played and seasons played, **personal bests** per game (score + date), past seasons (dates, days played), newest first; empty state until Season 1 ends.
- Only the day's daily game counts for that day (debug-launched runs of other games are ignored).
- Built from the player's own daily bests (`ScoreRepository.history()`, full history on device).
- **Online** (plan 09 PR 16, Firebase builds): each played day of the current season shows its position and the season points it earns ("· #4 of 212 · 5,225 pts"), the **Season points** tile adds them up ("provisional"; "…" while checking, "—" with no connection), and a **Today · {game}** list shows today's top 50 (ties share a position, "(you)" marks the player). Today's and yesterday's bests are re-uploaded when the screen opens, so runs finished offline catch up.
- **Decision (2026-11-20)**: no all-player season leaderboard in the MVP — on the Spark plan it would need a per-player season total written by clients (easy to fake) or reading every day's list. The Season tab shows *my* season points; other players appear in the daily top 50.

## 3.5 Locked / No-Attempts State
- Not a separate screen: Play disabled + countdown to next 12:00 noon + best of the day.

## 3.6 Settings
Opened from the gear on the Main screen (`SettingsRoute` in `:feature:settings`, plan 09 Decision 3). Groups, each a card of list rows:
- **Sound**: Music volume (slider, saved on release; 0 % = off) and Sound effects (on/off). Applied by `:core:audio` (plan 09 PR 7).
- **Feedback**: Vibration (on/off) — already drives button and countdown haptics (`LocalUiFeedback`).
- **Player**: Nick, read-only. Until onboarding exists (PR 15) it explains the nick is chosen once and never editable.
- **Privacy**: no personal data. With online ranking, an anonymous account (no email/phone) stores the nick and each day's best score in Firebase; everything else stays on this phone.
- **Credits & licences**: every bundled asset with its licence — must match `CREDITS.md` (unit-tested).
- **Debug** (debug builds only): reset today's attempts.
- App version at the bottom. Settings persist through `SettingsRepository` (docs/04 "Data layer").

## Reserved / To Define
- [ ] Game screen per minigame (HUD per `games/` spec; timer; sensor feedback).

All screens: portrait only, English copy.
