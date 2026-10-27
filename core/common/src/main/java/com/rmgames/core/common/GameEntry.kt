package com.rmgames.core.common

import androidx.compose.runtime.Composable

/**
 * A pluggable game as the session host consumes it: the [Game] logic plus the
 * Compose screen that renders it (frozen in Phase 1b). See
 * docs/08-implementation-plan.md.
 *
 * Pattern: Adapter — pairs a logic object with its UI behind one stable type, so the
 * host never inspects concrete games. Each `:feature:game:*` module exposes exactly
 * one entry; `:app` collects them into a [GameRegistry] once, which is what lets the
 * four developers work in parallel without touching shared navigation files.
 *
 * Deliberately minimal: the screen has **no parameters**. Screens only render the
 * game's state — the [Game] object is the single gateway to the host (it owns the
 * [GameScope] it received in [Game.start]), which keeps scoring/finishing rules out
 * of the UI and unit-testable on the JVM.
 *
 * @param game the game logic; the host calls [Game.start]/[Game.stop] around a run.
 * @param requiredPermissions runtime permissions the host must hold before the run
 * starts (e.g. `Manifest.permission.CAMERA` for Blink Sniper); empty for touch/sensor
 * games. Declared by the game module, checked and requested by the `:app` gate — a
 * denied permission blocks the run with an explanation, never a fake score
 * (docs/02-gameplay-rules.md).
 * @param screen renders the game's state; typically closes over the game instance.
 */
class GameEntry(
    val game: Game,
    val requiredPermissions: List<String> = emptyList(),
    val screen: @Composable () -> Unit,
)
