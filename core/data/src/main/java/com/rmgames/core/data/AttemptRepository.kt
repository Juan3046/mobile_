package com.rmgames.core.data

const val MAX_ATTEMPTS_PER_DAY = 2

/** Attempts enforcement stays local. Phase 0: in-memory; Room/DataStore wiring in Phase 1. */
interface AttemptRepository {
    fun remaining(dayIndex: Long): Int
    /** Returns false when no attempts remain. */
    fun consume(dayIndex: Long): Boolean
}

class InMemoryAttemptRepository : AttemptRepository {
    private val used = mutableMapOf<Long, Int>()

    override fun remaining(dayIndex: Long): Int =
        MAX_ATTEMPTS_PER_DAY - (used[dayIndex] ?: 0)

    override fun consume(dayIndex: Long): Boolean {
        if (remaining(dayIndex) <= 0) return false
        used[dayIndex] = (used[dayIndex] ?: 0) + 1
        return true
    }
}
