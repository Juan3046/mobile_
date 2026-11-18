# 01 — Minigame 01: PORUBA CASINO

## Concept
A short portrait blackjack game set in an absurd, student-life-inspired Poruba casino.
The player plays against an automated dealer and controls the hand with deliberate
head gestures detected by the front-facing camera.

- **Nod = HIT** (request one more card).
- **Shake head left/right = STAND** (finish the hand and let the dealer play).
- The run has **no time limit** (approved duration exception) and can include as many
  hands as the player's banana balance allows.
- The in-game currency is fictional **bananas**. It has no real-world value and cannot
  be purchased, redeemed, or transferred.

## Format
- Portrait orientation; unlimited run duration, ending only when the player has less than
  10 bananas after a hand settles.
- One player hand against one dealer hand at a time.
- A fresh, shuffled standard 52-card deck is used for each hand.
- The run starts with **100 bananas**. Each hand has a fixed **10-banana wager**.
- No player-selected bet size, double-down, split, or insurance.

## Blackjack Rules
- Deal two cards to the player and two to the dealer; one dealer card is face-up.
- Number cards are face value; J/Q/K = 10; Ace = 1 or 11, whichever is best without busting.
- Player may HIT or STAND using the camera gestures.
- Player total over 21 is a bust and loses the wager.
- The dealer draws until reaching 17 and stands on all 17s, including soft 17.
- Highest total at or below 21 wins; same total is a push.
- A natural blackjack is 21 with the player's first two cards. If both player and dealer
  have a natural blackjack, the hand pushes.

## Banana Balance & Score
- Starting balance: **100 bananas**.
- Fixed wager: **10 bananas per hand**.
- Ordinary win: **+10 net bananas** (1:1 profit); loss: **−10**; push: **0**.
- Natural blackjack: **+15 net bananas** (3:2 profit).
- The game's raw score is the **final banana balance**; higher is better. It ranks the
  day's run and is not itself added to the season score (global position-points rule).
- Wagers and all balances are fictional game state only.

## Run Flow
- If the balance is below 10 bananas after a hand is settled, the run ends immediately
  as a loss; report the remaining balance as the score.
- There is no countdown or automatic stand. The player may take as long as needed to
  choose HIT or STAND.
- Present play as visible, paced steps: pause about **650 ms** between initial cards,
  show the selected gesture/action for **700 ms** before applying it, wait **1 second**
  before the dealer's first action, pause **1.1 seconds** between dealer draws, and keep
  each resolved hand on screen for **2.5 seconds** before the next deal. These are brief
  presentation pauses only; they never expire a hand or run.

## Camera Gestures & Permission
- Front-facing camera through the shared `:core:vision` pipeline when device hardware
  supports it; devices without a front camera enter touch-control mode without asking
  for camera permission.
- When front-camera hardware is present, camera permission is requested before the
  camera-controlled run.
- Shared `HeadGestureDetector` emits `NOD` and `SHAKE` observations plus face-tracking
  status; game code does not consume CameraX or ML Kit types directly.
- Gesture recognition uses face head-pose changes with deliberate thresholds,
  neutral re-arming (8°), an 18° gesture threshold, and a 450 ms cooldown to avoid
  repeated/accidental actions; these defaults need physical-device tuning.
- Show a clear cue for each action (`NOD TO HIT`, `SHAKE TO STAND`) and camera/face
  tracking readiness without storing video. Do not display a game timer.
- If no front camera/face is available or tracking cannot start, switch to visible
  **HIT** and **STAND** touch buttons and continue the same blackjack run. Clearly
  explain that the buttons are the active controls; never strand the player on a
  camera-wait screen or report a fabricated score.
- Camera frames and face data stay on-device and are not saved or uploaded.

## HUD & Art Direction
- Dealer cards, player cards, current total, banana balance, and current gesture hints.
- Explain each transition with a short message: card dealt, current total, player action,
  dealer reveal/draw/total, hand result, and reason the run ended.
- Play short local sound effects for card deals, player decisions, dealer draws, and
  hand outcomes. Generate the tones on-device; do not download, save, or upload audio.
- When camera fallback is active, show accessible **HIT** and **STAND** buttons below
  the gesture area; only enable them during the player's turn.
- Make bust, win, natural blackjack, push, and loss immediately distinguishable.
- Procedural Compose/Canvas card and casino graphics; no external assets required.
- Poruba flavor through restrained local references/signage and playful casino styling;
  maintain readable cards and gesture feedback as the priority.
- All UI copy in English.

## Persistence
- Banana balance, cards, and camera observations exist only for the active run.
- Each run starts at 100 bananas; no balance, camera frames, or face data are saved or uploaded.

## TDD Plan
1. Pure JVM tests first for card values/Aces, hand totals, bust/natural detection,
   dealer soft-17 behavior, win/loss/push payouts, wager/balance changes, and no timeout.
2. Test gesture classification in `:core:vision` using synthetic head-pose samples:
   nod/shake thresholds, neutral re-arm, cooldown, and no event for noise.
3. Test `PorubaCasinoGame` with a fake `HeadGestureDetector`, including hit, stand,
   unexpired player turns, balance-based run completion, score reporting, and cleanup.
4. Verify the module and app build, run full JVM tests + lint, then test gestures on a
   physical device and tune thresholds.

## Coordinated Replacement Work
- Replaces Game 01 Escape Poruba before its feature branch is merged; do not merge the
  unmerged Escape Poruba branch.
- Implemented as `:feature:game:casino` with game ID `01-poruba-casino`.
- The coordinated change includes `HeadGestureDetector` in `:core:vision` and the
  required module/permission/registry wiring in `:app`; these shared files require
  owner review. Further game-only changes stay inside `:feature:game:casino`.

## Open Questions
- [ ] Tune nod/shake angles, minimum gesture duration, and cooldown
  through tests and physical-device playtesting.
