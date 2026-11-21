package com.rmgames.feature.game.rush

import kotlin.random.Random

/** The first rows are always pavement, so the run never starts in traffic. */
const val SAFE_START_ROWS = 3

/**
 * Builds the street for one run (docs/games/06 "Lanes & hazards"). Each row comes from its own
 * seeded random stream, so a lane does not depend on which rows were drawn before.
 * Speeds grow with the row (up to ×2.2 by row 100).
 */
class LaneGenerator(private val seed: Long) {
    private val cache = HashMap<Int, Lane>()

    fun lane(row: Int): Lane = cache.getOrPut(row) { build(row) }

    private fun build(row: Int): Lane {
        if (row < SAFE_START_ROWS) return Lane(LaneKind.PAVEMENT)
        val rng = Random(seed * 1_000_003L + row)
        val f = 1f + minOf(row, 100) * 0.012f
        val dir = if (rng.nextBoolean()) 1 else -1
        val roll = rng.nextFloat()
        return when {
            roll < 0.22f -> Lane(LaneKind.PAVEMENT)
            roll < 0.62f -> {
                val span = COLUMNS + 4f
                val count = 2 + rng.nextInt(2)
                val cars = (0 until count).map { i ->
                    Mover(start = -2f + i * span / count + rng.nextFloat() * 0.6f, length = if (rng.nextFloat() < 0.35f) 2f else 1f)
                }
                Lane(LaneKind.ROAD, dir, (1.2f + rng.nextFloat() * 1.3f) * f, cars, span)
            }
            roll < 0.78f -> {
                val speed = (8f + rng.nextFloat() * 3f) * minOf(f, 1.5f)
                val gapSeconds = 3f + rng.nextFloat() * 3f
                val span = COLUMNS + 5f + speed * gapSeconds
                Lane(LaneKind.TRAM, dir, speed, listOf(Mover(start = rng.nextFloat() * span - span / 2, length = 5f)), span)
            }
            else -> {
                val span = COLUMNS + 4f
                val floes = (0 until 3).map { i ->
                    Mover(start = -2f + i * span / 3 + rng.nextFloat() * 0.3f, length = 2f + rng.nextInt(2))
                }
                Lane(LaneKind.POND, dir, minOf((0.6f + rng.nextFloat() * 0.8f) * f, 2.2f), floes, span)
            }
        }
    }
}
