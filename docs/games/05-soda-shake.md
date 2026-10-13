# 05 — Minigame 05: SODA SHAKE

## Concept
A physical endurance and speed test. The player must physically shake the phone as fast as possible to build pressure inside a virtual soda can until it pops.

> **Approved duration exception:** this game runs a single ~5s round, outside the standard 30–90s rule (see `docs/02-gameplay-rules.md`).

## Format & Camera
- Portrait only, 2D cartoon UI.
- Features a soda can and a highly visible pressure gauge.

## Controls
- **Accelerometer only**.
- Device shaking (detecting high-force peaks on X, Y, and Z axes).

## Rules
- The player must reach 100% pressure within a strict 5-second time limit.
- If the timer hits zero before the pressure reaches 100% = Game Over.

## Difficulty Curve
- Round 1: Requires low shake intensity to fill the gauge.
- Round 2-3: The pressure gauge drains if the player stops shaking for even 0.5 seconds.
- Round 4+: The total physical force required to reach 100% increases significantly, demanding aggressive and continuous shaking.

## Open Questions
- [x] Decided: ~5s single-round duration approved as exception to the 30–90s rule.
- [ ] Scoring mapping: binary win/lose, pressure %, or time-to-pop? How does it feed daily best / season sum?
- [ ] Fallback: block + message if no accelerometer (per global rule)?
- [ ] Persistence: what is saved locally?