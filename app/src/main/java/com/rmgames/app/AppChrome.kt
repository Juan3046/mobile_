package com.rmgames.app

/**
 * App chrome decisions (plan 09 PR 14): splash hand-over and system-bar styling.
 * Pure values so they are JVM-tested; MainActivity applies them.
 */
internal object AppChrome {
    /** Bar scrim on the light theme = RmPalette.Light.background (values/colors.xml). */
    const val LIGHT_BAR_SCRIM: Int = 0xFFFBF7FF.toInt()

    /** Bar scrim on the dark theme = RmPalette.Dark.background (values-night/colors.xml). */
    const val DARK_BAR_SCRIM: Int = 0xFF17121F.toInt()

    /** The splash stays up until saved attempts/scores/settings are in memory. */
    fun shouldKeepSplash(isLoaded: Boolean): Boolean = !isLoaded

    /** Light bar icons on the dark ground, dark icons on the light ground. */
    fun lightBarIcons(darkTheme: Boolean): Boolean = darkTheme
}
