# 07 — Minigame 01: ESCAPE PORUBA

## Concept
2D arcade runner set in Poruba, Ostrava (Czech Republic). The player runs through
Poruba streets dodging obstacles to reach Ostrava Centrum. Run ends in win at ~90s;
completing it must feel extremely hard and rare.

- Win target: survive to 90.0s (`YOU ESCAPED PORUBA`, `ESCAPE TIME: 01:30.00`, optional `Never come back.`).
- Lose: any single collision = Game Over (`YOU'RE STILL IN PORUBA` + survived time + distance + best).
- Early game accessible; difficulty ramps aggressively; last 15–20s are hell.
- Fast loop: play → die → `ESCAPE AGAIN` restarts almost instantly (subject to global 2-attempts/day rule — see Open Questions).

## Format & Camera
- Portrait only, 2D top-down / slight pseudo-isometric.
- Road scrolls vertically downward; character stays near the lower third.
- Auto-forward; player controls horizontal position only.
- Max run length 90s (fits global 30–90s rule).

## Controls
- Mobile: drag finger left/right; character smoothly follows finger horizontally. Must feel instant, no input lag.
- Desktop/emulator debug only: `A` / Left arrow, `D` / Right arrow.
- No jump, attack, or extra buttons.
- Note: this game is touch-driven (exception to sensor-first guideline in `02-gameplay-rules.md` — to confirm).

## Rules
- One collision = Game Over, no lives.
- Hitboxes slightly smaller than sprites (hard but fair).
- No mathematically impossible patterns: before spawning a pattern, guarantee at least one feasible trajectory given max horizontal speed, current position, hitboxes, and available time. Brutal but fair — expert must be able to say "it was possible, I failed."

## Obstacles (Poruba/Ostrava themed)
Red trams, cars, buses, bicycles, scooters, containers, roadworks, fences, cones,
potholes, student groups, pedestrians, ice, narrow gaps between obstacles.

- Static: containers, roadworks, potholes, fences.
- Vertical movers: cars, buses, bicycles.
- Horizontal crossers: trams crossing screen, pedestrians, bicycles.
- Unpredictable: scooters, students slightly changing direction, cars changing lanes.

## Difficulty Curve (critical)
Continuous time-based difficulty, e.g. `difficulty = pow(elapsed / 90, 1.8)`, driving:
world speed, spawn rate, max simultaneous obstacles, mover speed, min gap size,
complex-pattern probability, tram frequency, time between patterns. Tune by playtest/sim.

- 0–15s Easy: low speed, wide gaps, isolated obstacles. Learn phase.
- 15–30s Normal: +25–35% speed, more obstacles, double patterns start, moving cars appear.
- 30–45s Hard: much higher spawn rate, horizontal trams appear, 2–3 threat combos, gaps shrink.
- 45–60s Very hard: high speed, simultaneous movers, less reaction time, forced fast crosses.
- 60–70s Brutal: very high spawn, multiple movers, chained patterns, small windows, fast L→R→L switches.
- 70–80s Extreme: trams + cars + statics, temporarily blocked lanes, narrow gaps, mandatory fast horizontal moves, almost no rest. Many good players die here.
- 80–90s HELL MODE: max speed, near-constant obstacles, 3–5 simultaneous threats, trams crossing while vertical vehicles spawn, minimal-but-possible gaps, fast direction changes, almost no rest, partially randomized combos. Most runs reaching here should end in Game Over.

## Pattern Library (unlock by difficulty, with variations)
- A: `X X . X` — single gap.
- B: `X . X .` — zigzag.
- C: vertical car + horizontal tram.
- D: two consecutive lines with gaps on opposite sides.
- E: nearly blocked road + mover crossing the only gap.
- Add variations so runs are not identical.

## Final Sequence (85–90s)
- Especially intense final pattern.
- Background sign appears: `OSTRAVA CENTRUM →`.
- On reaching 90s: stop gameplay, short escape animation, show `YOU ESCAPED PORUBA` + `ESCAPE TIME: 01:30.00` (+ optional `Never come back.`).

## Game Over Flow
- On hit: ~100ms freeze, simple impact effect, then `YOU'RE STILL IN PORUBA` + survived time + distance + best.
- Big `ESCAPE AGAIN` button; restart must be near-instant.
- Must respect global attempts: if no attempts left → lock + countdown screen instead (to confirm).

## Score & Persistence (local-only MVP)
- Save locally: best time, max distance, attempt count, whether ever escaped (`ESCAPES: X` on win).
- Score mapping to season (to confirm): score = survived centiseconds (win = 9000); daily best = max of up to 2 attempts; season = sum of daily bests.
- Distance: fictitious distance derived from time × speed; shown discreetly during run (e.g. `624 m`).

## HUD (minimal)
- Top small: `ESCAPE PORUBA`.
- One corner: timer (e.g. `62.4s`).
- Other corner: distance (e.g. `731 m`).
- Nothing else.

## Art Direction
- Simple 2D cartoon / pixel-inspired. Must run with provisional procedural in-project sprites; code structured for easy replacement later. No external assets required.
- Palette: grays, cold blue, muted green; red reserved for Ostrava trams.
- Character: young student, hoodie, backpack, baggy pants; extremely simple.
- Vibe: Poruba paneláks, socialist architecture, gray streets, grass, Czech signs, slightly absurd.

## Real Poruba Setting (must not feel generic)
- VŠB – Technical University of Ostrava must appear clearly: `VŠB-TUO`, `FEI`, `CAMPUS` signs, campus buildings, FEI zone, students with backpacks, bicycles, bike racks, campus greens. Ideally one run section is directly VŠB-inspired. Run can start at dorms: `PORUBA DORMS` → `ESCAPE.`
- Dorms zone: residence blocks with repetitive windows, students in/out, bikes, backpacks, grocery bags, benches, trees, pedestrian zones.
- Architecture: big residential blocks, pastel facades, socialist monumental arches, wide avenues, long straight streets, greens between buildings. Gag: it always feels like still Poruba.
- Trams: tracks, stops, wires, signs; bell sound + brief visual warning before a tram crosses fast. Early: predictable; ~80s: combined with cars and other threats.
- Stops/transport furniture: shelters, signs, poles, tracks, crosswalks; stop names as brief decoration.
- Nextbike-style shared bikes: parked, at stations, ridden by students, occasional unexpected crosser at high difficulty.
- Erasmus/university winks (restrained): student groups partially blocking path, grocery bags, students waiting for tram, discreet ESN flag, event posters, passing `Ahoj!`.
- ISIC easter egg: poster or student swiping card. No gameplay effect.
- Shops: generic supermarket, kebab, pub, café, small shop (no real brands) to make streets feel alive.
- Visual humor (rare): `OSTRAVA CENTRUM 8 km` repeating 30s later; `YOU ARE LEAVING PORUBA` followed seconds later by `WELCOME TO PORUBA`.

## Visual Zone Progression (90s)
- 0–15s `VŠB CAMPUS`: university buildings, students, bikes, grass, calm.
- 15–30s `PORUBA DORMS`: residence blocks, students, containers, bikes, cars.
- 30–45s `PORUBA STREETS`: residential blocks, shops, roads, parking.
- 45–60s `MAIN AVENUE`: bigger avenue, tram tracks, traffic, big buildings.
- 60–75s `TRAM ZONE`: many crossings, tracks, trams, traffic, pedestrians.
- 75–85s `EDGE OF PORUBA`: `CENTRUM →` / `OSTRAVA →` signs; player feels close.
- 85–90s `ESCAPE`: fewer buildings, road opens, `OSTRAVA CENTRUM →` backdrop + hardest sequence.

## Easter Eggs (sparse, integrated)
VŠB, FEI, ISIC, Nextbike bikes, Erasmus students, Czech signs, `Ahoj`, `Děkuji`,
Czech beer on a poster, trams, dorm blocks. Never all at once.

## Environment System (modular)
- Structure: `EnvironmentZone`, `DecorationSpawner`, `Landmark`, `BackgroundBuilding`, `PorubaZoneConfig`.
- Each zone defines: available buildings, decoration, trees, signs, student density, tram frequency, street furniture, landmarks.
- Decorations are background-only and never affect gameplay.
- Key landmarks (e.g. VŠB) spawn at fixed run times, not randomly.

## Audio (simple, ready for later files)
- Arcade music rising slightly in intensity; traffic ambience; tram bell; impact; special escape jingle.
- If no audio files yet, ship the system with hooks ready for drop-in files.

## Difficulty Feedback (noisy-but-readable)
- From 60s: occasional light vibration, faster music, more traffic/elements.
- ~75s: show `GET OUT.` for ~1s.
- ~85s: show `ALMOST OUT.`
- Never obscure obstacles with effects.

## Open Questions
- [x] Decided: touch allowed for Escape Poruba (sensor-first is a guideline); 1 run = 1 attempt (ESCAPE AGAIN only with attempts left, else lock + countdown); score = survived centiseconds (win = 9000), daily best = max, season = sum.
- [ ] Desktop A/D keys: debug-only and stripped from release? (assumed yes, to confirm at implementation).
