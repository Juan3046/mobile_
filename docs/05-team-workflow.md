# 05 — Team Workflow (4 People + AI Agents)

## Team
- 4 developers working with AI coding agents.
- Requirements phase first; **no implementation until requirements are frozen**.

## Version Control (GitHub)
- Repo: `https://github.com/Juan3046/mobile_.git` (connected, branch `main`).
- Flow: feature-per-module branches, small PRs, English names (e.g., `feature/daily-game-screen`).
- Tooling: GitHub CLI (`gh`) is installed and authenticated; use it to push branches and create/merge PRs (no direct pushes to `main`).
- No direct pushes to `main` (rule to enforce once repo exists).

## Parallel Development Plan
- **Status: Phase 1b (parallel game build)** — ownership: P1 Escape · P2 Onion · P3 Blink · P4 Compass + Soda. Phase 1a is done: `:core:sensors` + `:core:vision` are frozen and the hosting scaffolding (`GameEntry`/`GameRegistry`, session host, registry-driven nav) is merged — each developer now works only inside their own game module. See `08-implementation-plan.md`.

## Phase 1b — Working on Your Game Independently

1. **Sync + branch**: `git pull main`, then `git checkout -b feature/<module>-<gameplay>` (e.g. `feature/feature-game-onion-gameplay`). One module per PR.
2. **Your files** (nobody else touches them):
   - P1 `:feature:game:escape` · P2 `:feature:game:onion` · P3 `:feature:game:blink` · P4 `:feature:game:compass` + `:feature:game:soda`, plus your own `docs/games/0X-*.md`.
   - **Never edit**: `:app`, `:feature:session`, `:core:*`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `AGENTS.md`, or another team member's module. Contracts are frozen — if you believe one must change, first open a docs-only PR against `docs/08-implementation-plan.md` and agree it with the team.
3. **Implement** (three pieces, all inside your module):
   - Your `Game` impl: `start(scope)` collects your sensor/camera Flows in `scope.hostScope`, reports `scope.onScore(value)` (centiseconds/points) and ends with `scope.onFinished(win)` — **never suspend after `onFinished`** (the host cancels `hostScope` there). Collecting Flows in `hostScope` is enough: the host stops your game and cancels the scope, so listeners cannot leak.
   - Your `@Composable` screen: renders your game's state only — the `Game` object is the single gateway to the host (screens never call `onScore`/`onFinished` directly).
   - Your frozen factory `xxxEntry(...)`: the sensor/detector parameter is injected by `:app` — receive it, hand it to your `Game`, never change the signature.
4. **Test first** (`AGENTS.md` §5): JVM unit tests using the fakes (`FakeGyroscopeSensor`, `FakeAccelerometerSensor`, `FakeCompassSensor`, `FakeBlinkDetector`) — red → green. Then run the **full suite** before opening the PR: `./gradlew testDebugUnitTest` (JDK 17 — see "How to Build" in `README.md`). Red tests block the PR.
5. **Run on a device**: install the app → the home screen lists all five games → PLAY runs yours through the session host (score readout on top). Blink's camera permission is requested automatically by the `:app` gate.
6. **PR**: English, small (your module only), one reviewer, squash-merge. Update your `docs/games/0X-*.md` in the same PR whenever rules, scoring or controls change.

## Agent Rules
- Keep `docs/` and `AGENTS.md` in sync with every change (see `AGENTS.md` §4).
- Prefer editing existing files over creating new ones; keep changes small.
