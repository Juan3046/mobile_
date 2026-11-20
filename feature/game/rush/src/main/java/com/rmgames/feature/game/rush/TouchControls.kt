package com.rmgames.feature.game.rush

import kotlin.math.abs

/**
 * Touch controls (docs/games/06 "Controls", approved touch exception 2026-11-20): a finger
 * gesture from touch-down to lift, as a hop. Shorter than [threshold] in both axes = a tap =
 * forward; otherwise the larger axis decides (swipe left/right steps, down hops back, up hops
 * forward).
 */
internal fun swipeHop(dx: Float, dy: Float, threshold: Float): Hop = when {
    abs(dx) < threshold && abs(dy) < threshold -> Hop.FORWARD
    abs(dx) > abs(dy) -> if (dx > 0) Hop.RIGHT else Hop.LEFT
    dy > 0 -> Hop.BACK
    else -> Hop.FORWARD
}
