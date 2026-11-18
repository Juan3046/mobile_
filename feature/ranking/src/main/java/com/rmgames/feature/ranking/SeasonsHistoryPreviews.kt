package com.rmgames.feature.ranking

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rmgames.core.common.theme.RmTheme
import com.rmgames.core.data.DailyBest
import java.time.LocalDate

// Android Studio previews with illustrative data (Season 3, two past seasons). Not used at runtime.

private fun day(m: Int, d: Int) = LocalDate.of(2026, m, d).toEpochDay()

private val sample = SeasonHistory.build(
    todayIndex = day(12, 2),
    history = listOf(
        DailyBest(day(11, 17), "03-blink-sniper", 27),
        DailyBest(day(11, 18), "05-soda-shake", 1132),
        DailyBest(day(11, 25), "02-onion-roll", 62),
        DailyBest(day(11, 30), "04-compass-chaos", 9),
        DailyBest(day(12, 2), "05-soda-shake", 845),
    ),
    titles = mapOf("02-onion-roll" to "Onion Roll", "03-blink-sniper" to "Blink Sniper",
        "04-compass-chaos" to "Compass Chaos", "05-soda-shake" to "Soda Shake"),
)

@Preview(name = "Seasons history · dark", widthDp = 360, heightDp = 800)
@Composable
private fun HistoryDark() = RmTheme(darkTheme = true) { SeasonsHistoryScreen(sample, onBack = {}) }

@Preview(name = "Seasons history · light", widthDp = 360, heightDp = 800)
@Composable
private fun HistoryLight() = RmTheme(darkTheme = false) { SeasonsHistoryScreen(sample, onBack = {}) }
