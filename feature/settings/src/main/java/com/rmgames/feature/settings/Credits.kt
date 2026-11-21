package com.rmgames.feature.settings

/** One bundled third-party asset and its licence (plan 09 Decision 6). */
data class CreditEntry(val name: String, val use: String, val copyright: String, val licence: String)

/**
 * What the Settings credits section lists. Must match CREDITS.md (a unit test checks it):
 * add the row in both places in the same change that bundles a new asset.
 */
object Credits {
    private const val OFL = "SIL Open Font License 1.1"

    val fonts = listOf(
        CreditEntry("Bungee", "Titles, scores and the Play button", "© 2023 The Bungee Project Authors", OFL),
        CreditEntry("Rubik", "Interface text", "© 2015 The Rubik Project Authors", OFL),
        CreditEntry("JetBrains Mono", "Timers and readouts", "© 2020 The JetBrains Mono Project Authors", OFL),
    )
}

/** Volume as a whole percentage, e.g. "70%". */
/**
 * Privacy row (title to text), docs/03 §3.6. With the online ranking the nick and daily bests
 * leave the phone under an anonymous account, so the offline promise must not show then.
 */
fun privacyNote(online: Boolean): Pair<String, String> = if (online) {
    "Anonymous, no personal data" to
        "Your nick and each day's best score are shared for the ranking (no email or phone number). Everything else stays on this phone."
} else {
    "Your data stays on this phone" to "Attempts, scores and settings are stored only on this device. No account, no personal data."
}

fun volumeLabel(volume: Float): String = "${(volume.coerceIn(0f, 1f) * 100).toInt()}%"

/** Screen-reader text for the music slider. */
fun volumeDescription(volume: Float): String =
    if (volume <= 0f) "Music off" else "Music volume ${volumeLabel(volume)}"
