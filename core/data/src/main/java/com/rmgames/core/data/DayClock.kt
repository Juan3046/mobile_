package com.rmgames.core.data

import java.time.Instant
import java.time.ZoneId

/**
 * Noon-to-noon days in [zoneId]. Day D runs 12:00 (D) → 12:00 (D+1) local time.
 * See docs/01-product-overview.md.
 */
class DayClock(private val zoneId: ZoneId = ZoneId.systemDefault()) {

    fun dayIndex(nowMillis: Long = System.currentTimeMillis()): Long {
        val dateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime()
        val epochDay = dateTime.toLocalDate().toEpochDay()
        return if (dateTime.hour < 12) epochDay - 1 else epochDay
    }

    fun nextResetMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val dateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime()
        val resetDate = if (dateTime.hour < 12) dateTime.toLocalDate() else dateTime.toLocalDate().plusDays(1)
        return resetDate.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
    }
}
