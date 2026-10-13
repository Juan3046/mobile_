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

## 3.2 Nick Onboarding (first launch only)
- Shown once, before anything else, when no nick is bound to this install.
- Nick input + Continue button. Rules: 3–12 chars (letters/numbers/underscore), unique (rejected if taken), set once, never editable.
- Requires connection (one-time Firestore uniqueness reservation); results bind to the Anonymous Auth UID from then on.

## 3.3 Result Screen (after each run)
- Run score, daily best, remaining attempts.
- Provisional day position + provisional points it would award.
- Play again button (if attempts remain) else lock state (see 3.5).

## 3.4 Ranking / Seasons History
- Tabs: **Season** (current standings with nicks + points, plus my day-by-day breakdown) and **History** (past seasons + all-time total).
- Top-50 paginated reads (Firestore quota).

## 3.5 Locked / No-Attempts State
- Not a separate screen: Play disabled + countdown to next 12:00 noon + best of the day.

## Reserved / To Define
- [ ] Game screen per minigame (HUD per `games/` spec; timer; sensor feedback).

All screens: portrait only, English copy.
