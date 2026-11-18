package com.rmgames.app

import com.rmgames.core.common.Game

/**
 * Games registered but not yet playable (still the Phase 0 skeleton). They stay in the
 * registry — the debug launcher can still open them — but out of the daily rotation
 * until their gameplay lands (docs/09-phase2-shell-plan.md §5 Risks). Remove an id here
 * in the same PR that makes its game playable.
 */
internal val NOT_READY_FOR_ROTATION = setOf("01-escape-poruba")

/** Games eligible to be "today's game". Falls back to all games if none is ready. */
internal fun dailyRotation(games: List<Game>): List<Game> =
    games.filterNot { it.id in NOT_READY_FOR_ROTATION }.ifEmpty { games }
