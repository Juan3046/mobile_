package com.rmgames.core.common.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * App icons, ported from the design-system sprite (design-system/icons.html): original
 * geometry in the style of Lucide / Material Symbols outlined, 24-unit grid, rounded
 * 2.25 stroke. Drawn in black; `Icon(tint = …)` recolours them.
 */
object RmIcons {
    val Play = filled("play", "M7 4.5v15a1 1 0 0 0 1.5.9l12-7.5a1 1 0 0 0 0-1.8l-12-7.5A1 1 0 0 0 7 4.5z")
    val Replay = stroked("replay", "M3 12a9 9 0 1 0 3-6.7L3 8", "M3 3v5h5")
    val Settings = stroked("settings", circle(12f, 12f, 3f),
        "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z")
    val Close = stroked("close", "M18 6L6 18M6 6l12 12")
    val Back = stroked("back", "M19 12H5", "M12 19l-7-7 7-7")
    val Check = stroked("check", "M20 6L9 17l-5-5")
    val ChevronRight = stroked("chevron-right", "M9 18l6-6-6-6")
    val Timer = stroked("timer", circle(12f, 13f, 8f), "M12 9v4l2 2M9 2h6M12 2v3")
    val Lock = stroked("lock", rect(4f, 11f, 16f, 10f, 2f), "M8 11V7a4 4 0 0 1 8 0v4")
    val Star = stroked("star", "M12 2.5l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5L2.5 9.4l6.6-.9z")
    val Trophy = stroked("trophy", "M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0z", "M17 5h3v2a3 3 0 0 1-3 3M7 5H4v2a3 3 0 0 0 3 3")
    val Podium = stroked("podium", "M9 21V9h6v12M3 21v-7h6M15 21v-5h6v5M2 21h20",
        "M12 3l.9 1.8 2 .3-1.4 1.4.3 2-1.8-1-1.8 1 .3-2-1.4-1.4 2-.3z")
    val Calendar = stroked("calendar", rect(3f, 4f, 18f, 17f, 2f), "M16 2v4M8 2v4M3 10h18")
    val History = stroked("history", "M3 12a9 9 0 1 0 2.6-6.4L3 8", "M3 3v5h5M12 7v5l3 2")
    val Info = stroked("info", circle(12f, 12f, 9f), "M12 11v6M12 7.5v.5")
    val Bug = stroked("bug", rect(7f, 7f, 10f, 13f, 5f),
        "M12 11v9M7 13H3M21 13h-4M7 17l-3 2M17 17l3 2M7 9L4 7M17 9l3-2M9 5l1.5 2M15 5l-1.5 2")
    val Music = stroked("music", "M9 18V5l12-2v13", circle(6f, 18f, 3f), circle(18f, 16f, 3f))
    val Volume = stroked("volume", "M11 5L6 9H2v6h4l5 4z", "M15.5 8.5a5 5 0 0 1 0 7M19 5a10 10 0 0 1 0 14")
    val Vibrate = stroked("vibrate", rect(8f, 4f, 8f, 16f, 1.5f), "M4 9l-2 3 2 3M20 9l2 3-2 3")
    val User = stroked("user", circle(12f, 8f, 4f), "M4 21a8 8 0 0 1 16 0")

    // Sensors / games
    val Camera = stroked("camera", "M22 18a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h3l2-3h6l2 3h3a2 2 0 0 1 2 2z", circle(12f, 13f, 4f))
    val Eye = stroked("eye", "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z", circle(12f, 12f, 3f))
    val Compass = stroked("compass", circle(12f, 12f, 9f), "M15.5 8.5l-2 5-5 2 2-5z")
    val Can = stroked("can", "M7 5h10l1 3v11a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8z", "M6 8h12M9 3h6")
    val Onion = stroked("onion", "M12 3c0 3-6 6-6 11a6 6 0 0 0 12 0c0-5-6-8-6-11z",
        "M12 3c-1 4-2 7-2 11s1 6 2 6M12 3c1 4 2 7 2 11s-1 6-2 6")
    val Run = stroked("run", circle(14f, 4f, 2f), "M6 21l3-6 3 2v5M8 10l3-3 4 2 3 3M11 7l-1 6")
    val Shake = stroked("shake", rect(8f, 3f, 8f, 18f, 2f), "M4 8v8M20 8v8M2 10v4M22 10v4")
    val Cards = stroked("cards", rect(9f, 3f, 12f, 16f, 2f), "M6 6.5l-2 .6a2 2 0 0 0-1.3 2.4l3 10.4a2 2 0 0 0 2.4 1.4l4.4-1.2", "M15 7.5l-2.2 3.5 2.2 3.5 2.2-3.5z")
    val Tilt = stroked("tilt", rect(7f, 3f, 10f, 18f, 2f), "M3 8a10 10 0 0 0 0 8M21 8a10 10 0 0 1 0 8")

    /** Every icon by sprite name (used by tests and previews). */
    val all: Map<String, ImageVector> by lazy {
        listOf(Play, Replay, Settings, Close, Back, Check, ChevronRight, Timer, Lock, Star, Trophy, Podium,
            History, Calendar, Info, Bug, Music, Volume, Vibrate, User, Camera, Eye, Compass, Can, Onion, Run, Shake, Tilt, Cards)
            .associateBy { it.name }
    }

    private fun stroked(name: String, vararg paths: String) = build(name, paths, filled = false)
    private fun filled(name: String, vararg paths: String) = build(name, paths, filled = true)

    private fun build(name: String, paths: Array<out String>, filled: Boolean): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = if (filled) SolidColor(Color.Black) else null,
                    stroke = if (filled) null else SolidColor(Color.Black),
                    strokeLineWidth = 2.25f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) =
        "M${x + rx} ${y}h${w - 2 * rx}a$rx $rx 0 0 1 $rx ${rx}v${h - 2 * rx}a$rx $rx 0 0 1 ${-rx} ${rx}" +
            "h${-(w - 2 * rx)}a$rx $rx 0 0 1 ${-rx} ${-rx}v${-(h - 2 * rx)}a$rx $rx 0 0 1 $rx ${-rx}z"
}
