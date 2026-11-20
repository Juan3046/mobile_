package com.rmgames.core.common.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmgames.core.common.theme.RmTheme

/**
 * The Direction A signature: content on a solid "hard ledge" with a dark outline.
 * When [pressed], the content sinks onto the ledge (see [ledgeGeometry]); the
 * footprint never changes, so pressing doesn't shift the layout.
 */
@Composable
fun LedgeSurface(
    color: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    outlined: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val d = RmTheme.dimens
    val ledgeColor = RmTheme.colors.ledge
    val geometry = ledgeGeometry(pressed, d)
    val offset by animateDpAsState(geometry.contentOffset, tween(RmTheme.motion.durationFast), label = "ledge")
    // propagateMinConstraints: a fillMaxWidth() caller stretches the face too.
    Box(modifier, propagateMinConstraints = true) {
        // The ledge: same shape, peeking out below the face.
        Box(Modifier.matchParentSize().padding(top = d.ledge).background(ledgeColor, shape))
        Box(
            Modifier
                .padding(bottom = d.ledge)
                .offset(y = offset)
                .clip(shape)
                .background(color)
                .then(if (outlined) Modifier.border(d.strokeWidth, ledgeColor, shape) else Modifier),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

enum class RmButtonVariant { Filled, Tonal, Outlined, Text }
enum class RmButtonSize { Default, Xl }

/**
 * App button (plan 09 §2A RmButton). Filled = the one main action per screen (Play),
 * Tonal = secondary, Outlined/Text = low emphasis. [RmButtonSize.Xl] is the hero size.
 * Every press gives haptic + click feedback through [LocalUiFeedback].
 */
@Composable
fun RmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RmButtonVariant = RmButtonVariant.Filled,
    size: RmButtonSize = RmButtonSize.Default,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = RmTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val xl = size == RmButtonSize.Xl
    val shape = if (xl) MaterialTheme.shapes.large else CircleShape
    val faceHeight = if (xl) 60.dp else 44.dp   // + 4dp ledge = 64 / 48 footprint
    val disabledFill = c.onSurface.copy(alpha = 0.12f)
    val disabledContent = c.onSurface.copy(alpha = 0.38f)
    val (fill, content) = when {
        !enabled && variant != RmButtonVariant.Text -> disabledFill to disabledContent
        !enabled -> Color.Transparent to disabledContent
        variant == RmButtonVariant.Filled -> c.primary to c.onPrimary
        variant == RmButtonVariant.Tonal -> c.secondaryContainer to c.onSecondaryContainer
        else -> Color.Transparent to c.primaryInk
    }
    val click = rememberRmClick(enabled, onClick)
    val clickable = modifier.clickable(
        interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = click,
    )
    val label: @Composable () -> Unit = {
        Row(
            Modifier.defaultMinSize(minHeight = faceHeight).padding(horizontal = if (xl) 32.dp else 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(if (xl) 24.dp else 18.dp))
            Text(
                text = if (xl) text.uppercase() else text,
                color = content, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = if (xl) MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, letterSpacing = 0.9.sp)
                else MaterialTheme.typography.labelLarge,
            )
        }
    }
    when {
        enabled && (variant == RmButtonVariant.Filled || variant == RmButtonVariant.Tonal) ->
            LedgeSurface(fill, shape, clickable, pressed = pressed) { label() }
        variant == RmButtonVariant.Outlined -> Box(
            clickable.padding(bottom = RmTheme.dimens.ledge).clip(shape)
                .border(RmTheme.dimens.strokeWidth, if (enabled) c.outline else disabledFill, shape)
                .background(if (pressed) c.primaryInk.copy(alpha = 0.10f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { label() }
        else -> Box(
            clickable.padding(bottom = RmTheme.dimens.ledge).clip(shape)
                .background(if (pressed && enabled) content.copy(alpha = 0.10f) else fill),
            contentAlignment = Alignment.Center,
        ) { label() }
    }
}

/** 48dp icon button (standard style): haptic + click feedback like [RmButton]. */
@Composable
fun RmIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = RmTheme.colors.onSurfaceVariant,
    enabled: Boolean = true,
) {
    val click = rememberRmClick(enabled, onClick)
    Box(
        modifier.size(RmTheme.dimens.targetMin).clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = click),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = if (enabled) tint else tint.copy(alpha = 0.38f), modifier = Modifier.size(24.dp))
    }
}

/** Wires [handleRmClick] to the view's haptics and the [LocalUiFeedback] sound. */
@Composable
internal fun rememberRmClick(enabled: Boolean, onClick: () -> Unit): () -> Unit {
    val view = LocalView.current
    val feedback = LocalUiFeedback.current
    return {
        handleRmClick(enabled, onClick, feedback) {
            if (feedback.hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}

/** Card on the ledge (Direction A default card). Clickable cards sink when pressed. */
@Composable
fun RmCard(
    modifier: Modifier = Modifier,
    color: Color = RmTheme.colors.surfaceContainer,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val click = onClick?.let { rememberRmClick(true, it) }
    LedgeSurface(
        color = color, shape = shape, pressed = pressed && onClick != null,
        modifier = if (click != null) modifier.clickable(interaction, indication = null, onClick = click) else modifier,
        content = content,
    )
}
