package com.rmgames.core.common.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmgames.core.common.theme.RmMotion
import com.rmgames.core.common.theme.RmTheme

/**
 * Top app bar. [brand] = the Main screen's "RM GAMES" wordmark (display face, left);
 * otherwise a centred functional title. Flat by design: the ledge is for actions/cards.
 */
@Composable
fun RmTopBar(
    title: String,
    modifier: Modifier = Modifier,
    brand: Boolean = false,
    navigationIcon: ImageVector? = null,
    navigationLabel: String = "Back",
    onNavigate: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = RmTheme.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigationIcon != null) RmIconButton(navigationIcon, navigationLabel, onNavigate, tint = c.onSurface)
        else if (!brand) Spacer(Modifier.width(48.dp))
        Text(
            text = if (brand) title.uppercase() else title,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp).semantics { heading() },
            style = if (brand) MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp)
            else MaterialTheme.typography.titleMedium,
            textAlign = if (brand) TextAlign.Start else TextAlign.Center,
            color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        // Reserve the same 48dp as the navigation slot so a centred title stays centred.
        Row(Modifier.widthIn(min = if (brand) 0.dp else 48.dp), horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Screen transition specs for the NavHost (plan 09 §2A): short rise + fade. */
object RmTransitions {
    fun enter(m: RmMotion = RmMotion()): EnterTransition =
        fadeIn(tween(m.durationBase, easing = m.standard)) +
            slideInVertically(tween(m.durationBase, easing = m.emphasized)) { it / 16 }

    fun exit(m: RmMotion = RmMotion()): ExitTransition =
        fadeOut(tween(m.durationFast, easing = m.standard))

    fun popExit(m: RmMotion = RmMotion()): ExitTransition =
        fadeOut(tween(m.durationFast, easing = m.standard)) + scaleOut(tween(m.durationBase), targetScale = 0.98f)
}
