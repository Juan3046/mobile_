package com.rmgames.feature.game.rush

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmgames.core.common.ui.LocalUiFeedback
import kotlin.math.ceil
import kotlin.math.floor

// Simple flat palette (procedural art, no assets).
private val Pavement = Color(0xFFD9D2C3)
private val PavementAlt = Color(0xFFCFC7B6)
private val Asphalt = Color(0xFF3B3F4A)
private val RoadLine = Color(0xFFE8E4DA)
private val Gravel = Color(0xFF8C8577)
private val Rail = Color(0xFF55504A)
private val Water = Color(0xFF6FB7E8)
private val Ice = Color(0xFFF1FAFF)
private val TramBody = Color(0xFF2F6FD6)
private val TramStripe = Color(0xFFF6F1FA)
private val Warning = Color(0xFFE53935)
private val Ink = Color(0xFF10131A)
private val Skin = Color(0xFFF2C9A0)
private val Hoodie = Color(0xFFFF5D8F)
private val Backpack = Color(0xFFFFD23F)
private val CarColors = listOf(Color(0xFFE63946), Color(0xFF2A9D8F), Color(0xFFF4A261), Color(0xFF8E7DBE))

/** Rows kept below the student on screen. */
private const val ROWS_BELOW = 2

/**
 * Class Rush screen (docs/games/06): a top-down street drawn with Canvas, the student, and a
 * small HUD (time left, rows). Renders [ClassRushGame.state] only. Debug builds also hop with
 * the arrow keys / WASD (emulator playtesting).
 */
@Composable
fun ClassRushScreen(game: ClassRushGame) {
    val s by game.state.collectAsState()
    val tilt by game.tilt.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    // Haptics (honours Settings → Vibration).
    val context = LocalContext.current
    val hapticsEnabled = LocalUiFeedback.current.hapticsEnabled
    val vibrator = remember(context) { RushVibrator(context) }
    val previous = remember { arrayOf(RushState()) }
    LaunchedEffect(s) {
        val buzz = rushBuzz(previous[0], s)
        previous[0] = s
        if (buzz != null && hapticsEnabled) vibrator.play(buzz)
    }

    // Smooth hops and camera.
    val px by animateFloatAsState(s.x, tween(if (s.phase == RushPhase.PLAYING) 90 else 0), label = "x")
    val py by animateFloatAsState(s.row.toFloat(), tween(110), label = "row")
    val camera by animateFloatAsState((s.row - ROWS_BELOW).coerceAtLeast(0).toFloat(), tween(220), label = "camera")

    Box(
        Modifier.fillMaxSize().background(Pavement)
            .focusRequester(focus).focusable()
            .onKeyEvent { debugHop(it)?.let { hop -> game.requestDebugHop(hop); true } ?: false },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / COLUMNS
            val t = s.elapsedMs / 1000f
            val first = floor(camera).toInt() - 1
            val last = first + ceil(size.height / cell).toInt() + 2
            for (row in first..last) {
                val top = size.height - (row - camera + 1) * cell
                drawLane(if (row < 0) Lane(LaneKind.PAVEMENT) else game.lane(row), row, top, cell, t)
            }
            if (s.phase != RushPhase.BLOCKED) {
                drawStudent(Offset(px * cell + cell / 2, size.height - (py - camera + 0.5f) * cell), cell, fell = s.end == RushEnd.FELL)
            }
        }

        // HUD: time left and rows.
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            HudChip("⏱ ${clockText(s.timeLeftMs)}")
            HudChip("ROWS ${s.score}")
        }

        // Tilt indicator: the dot is the phone's tilt, the ring is where a hop fires.
        if (s.phase == RushPhase.PLAYING) {
            Canvas(Modifier.align(Alignment.BottomEnd).padding(20.dp).size(96.dp)) { drawTiltIndicator(tilt) }
        }

        val message = when {
            s.phase == RushPhase.BLOCKED -> "Class Rush needs an accelerometer. This run is not recorded."
            s.phase == RushPhase.OVER -> endMessage(s)
            tilt == null && s.phase == RushPhase.PLAYING -> "Hold the phone still…"
            s.elapsedMs < 3_500 -> "Tilt forward to hop · back to the middle · again"
            else -> null
        }
        if (message != null) {
            Text(
                message,
                Modifier.align(if (s.phase == RushPhase.PLAYING) Alignment.BottomCenter else Alignment.Center)
                    .padding(24.dp).background(Ink.copy(alpha = 0.78f), RoundedCornerShape(16.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HudChip(text: String) {
    Text(
        text,
        Modifier.background(Ink.copy(alpha = 0.78f), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 8.dp),
        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black,
    )
}

private fun DrawScope.drawTiltIndicator(tilt: Tilt?) {
    val c = Offset(size.width / 2, size.height / 2)
    val r = size.minDimension / 2 * 0.8f
    drawCircle(Ink.copy(alpha = 0.6f), size.minDimension / 2, c)
    drawCircle(Color.White.copy(alpha = 0.35f), r * REARM_DEG / HOP_DEG, c) // calm zone (re-arm)
    drawCircle(Color.White, r, c, style = androidx.compose.ui.graphics.drawscope.Stroke(size.minDimension * 0.03f)) // hop ring
    if (tilt == null) return
    val scale = r / HOP_DEG
    val dot = Offset(
        c.x + (-tilt.roll * scale).coerceIn(-r * 1.2f, r * 1.2f),
        c.y + (-tilt.lean * scale).coerceIn(-r * 1.2f, r * 1.2f),
    )
    val past = maxOf(kotlin.math.abs(tilt.lean), kotlin.math.abs(tilt.roll)) >= HOP_DEG
    drawCircle(if (past) Hoodie else Backpack, size.minDimension * 0.11f, dot)
}

private fun DrawScope.drawLane(lane: Lane, row: Int, top: Float, cell: Float, t: Float) {
    val w = size.width
    when (lane.kind) {
        LaneKind.PAVEMENT -> {
            drawRect(if (row % 2 == 0) Pavement else PavementAlt, Offset(0f, top), Size(w, cell))
            // Paving joints.
            for (c in 1 until COLUMNS) drawRect(PavementAlt.copy(alpha = 0.6f), Offset(c * cell, top), Size(cell * 0.03f, cell))
        }
        LaneKind.ROAD -> {
            drawRect(Asphalt, Offset(0f, top), Size(w, cell))
            for (c in 0 until COLUMNS) drawRect(RoadLine, Offset(c * cell + cell * 0.3f, top - cell * 0.02f), Size(cell * 0.4f, cell * 0.04f))
            lane.movers.forEachIndexed { i, m -> drawCar(lane.moverX(m, t) * cell, top, m.length * cell, cell, CarColors[(row + i) % CarColors.size], lane.dir) }
        }
        LaneKind.TRAM -> {
            drawRect(Gravel, Offset(0f, top), Size(w, cell))
            listOf(0.3f, 0.7f).forEach { y -> drawRect(Rail, Offset(0f, top + cell * y - cell * 0.03f), Size(w, cell * 0.06f)) }
            lane.movers.forEach { m ->
                val x = lane.moverX(m, t) * cell
                drawRoundRect(TramBody, Offset(x, top + cell * 0.1f), Size(m.length * cell, cell * 0.8f), CornerRadius(cell * 0.2f))
                drawRect(TramStripe, Offset(x, top + cell * 0.42f), Size(m.length * cell, cell * 0.16f))
            }
            // Warning light: blinks while a tram is about to come in.
            if (lane.warning(t) && (t * 4).toInt() % 2 == 0) {
                val side = if (lane.dir > 0) cell * 0.35f else w - cell * 0.35f
                drawCircle(Warning, cell * 0.22f, Offset(side, top + cell / 2))
                drawCircle(Color.White, cell * 0.08f, Offset(side, top + cell / 2))
            }
        }
        LaneKind.POND -> {
            drawRect(Water, Offset(0f, top), Size(w, cell))
            lane.movers.forEach { m ->
                drawRoundRect(Ice, Offset(lane.moverX(m, t) * cell + cell * 0.05f, top + cell * 0.12f), Size(m.length * cell - cell * 0.1f, cell * 0.76f), CornerRadius(cell * 0.25f))
            }
        }
    }
}

private fun DrawScope.drawCar(x: Float, top: Float, length: Float, cell: Float, color: Color, dir: Int) {
    drawRoundRect(color, Offset(x + cell * 0.05f, top + cell * 0.15f), Size(length - cell * 0.1f, cell * 0.7f), CornerRadius(cell * 0.18f))
    // Windscreen on the driving side.
    val glassX = if (dir > 0) x + length - cell * 0.45f else x + cell * 0.15f
    drawRoundRect(Color(0xCCDDEEFF), Offset(glassX, top + cell * 0.25f), Size(cell * 0.3f, cell * 0.5f), CornerRadius(cell * 0.08f))
}

private fun DrawScope.drawStudent(c: Offset, cell: Float, fell: Boolean) {
    val r = cell * 0.36f
    if (fell) drawCircle(Color.White.copy(alpha = 0.5f), r * 1.4f, c) // splash
    drawRoundRect(Backpack, Offset(c.x - r * 0.75f, c.y - r * 0.1f), Size(r * 1.5f, r * 1.0f), CornerRadius(r * 0.3f))
    drawCircle(Hoodie, r * 0.85f, c)
    drawCircle(Skin, r * 0.5f, Offset(c.x, c.y - r * 0.35f))
    drawCircle(Ink, r * 0.85f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(cell * 0.03f))
}

private fun debugHop(event: KeyEvent): Hop? {
    if (!BuildConfig.DEBUG || event.type != KeyEventType.KeyDown) return null
    return when (event.key) {
        Key.DirectionUp, Key.W -> Hop.FORWARD
        Key.DirectionDown, Key.S -> Hop.BACK
        Key.DirectionLeft, Key.A -> Hop.LEFT
        Key.DirectionRight, Key.D -> Hop.RIGHT
        else -> null
    }
}
