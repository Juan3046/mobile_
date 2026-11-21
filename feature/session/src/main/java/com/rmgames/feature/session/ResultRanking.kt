package com.rmgames.feature.session

import com.rmgames.core.data.Standing

/** What the Result's ranking card knows (docs/03 §3.3, plan 09 PR 16). */
sealed interface ResultRanking {
    data object Practice : ResultRanking
    /** Build without online ranking. */
    data object Offline : ResultRanking
    /** No daily best yet, so nothing to rank. */
    data object NoScore : ResultRanking
    data object Loading : ResultRanking
    /** Upload or read failed (no connection): retried next time. */
    data object Unknown : ResultRanking
    data class Known(val standing: Standing) : ResultRanking
}

/** [headline] = the position ("#4 of 212"); [points] = what it earns, shown right under it. */
data class RankingNote(val label: String, val headline: String?, val body: String, val points: String? = null)

internal fun initialResultRanking(practice: Boolean, online: Boolean, hasBest: Boolean): ResultRanking = when {
    practice -> ResultRanking.Practice
    !online -> ResultRanking.Offline
    !hasBest -> ResultRanking.NoScore
    else -> ResultRanking.Loading
}

internal fun resultRankingNote(r: ResultRanking): RankingNote = when (r) {
    ResultRanking.Practice -> RankingNote("PRACTICE RUN", null, "Debug practice: this score is not saved and no attempt was used.")
    ResultRanking.Offline -> RankingNote("PROVISIONAL RANKING", null, "Your best of the day decides your position. This build has no online ranking.")
    ResultRanking.NoScore -> RankingNote("PROVISIONAL RANKING", null, "Score a run to get a position in today's ranking.")
    ResultRanking.Loading -> RankingNote("PROVISIONAL RANKING", null, "Checking today's ranking…")
    ResultRanking.Unknown -> RankingNote("PROVISIONAL RANKING", null, "Couldn't reach the ranking. Your best is saved and uploads next time you're online.")
    is ResultRanking.Known -> RankingNote(
        "PROVISIONAL RANKING",
        "#${r.standing.position} of ${r.standing.players}",
        "Your position today and the season points it earns. It can change until 12:00 as others play.",
        points = "+${String.format(java.util.Locale.US, "%,d", r.standing.points)} season pts",
    )
}
