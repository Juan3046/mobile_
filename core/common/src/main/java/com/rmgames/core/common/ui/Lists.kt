package com.rmgames.core.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rmgames.core.common.theme.RmTheme

/**
 * One/two-line list row (M3 list item): leading icon, headline, supporting text and a
 * trailing slot. Flat by design — rows sit inside an [RmCard] that carries the ledge.
 * With [onClick] the whole row is the touch target (e.g. a switch row toggles on tap).
 */
@Composable
fun RmListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    role: Role? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = RmTheme.colors
    val click = onClick?.let { rememberRmClick(true, it) }
    Row(
        modifier.fillMaxWidth().heightIn(min = if (supporting == null) 56.dp else 72.dp)
            .then(if (click != null) Modifier.clickable(role = role, onClick = click) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(headline, style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
            if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

/** Hairline between rows of the same group (inset past the leading icon). */
@Composable
fun RmListDivider(inset: Boolean = true) {
    HorizontalDivider(Modifier.padding(start = if (inset) 56.dp else 0.dp), thickness = 1.dp, color = RmTheme.colors.outlineVariant)
}

/** M3 switch in Direction A colours: primary track when on, the ledge colour as thumb outline. */
@Composable
fun RmSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = RmTheme.colors
    Switch(
        checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = c.primary, checkedThumbColor = c.onPrimary, checkedBorderColor = c.ledge,
            uncheckedTrackColor = c.surfaceContainerHighest, uncheckedThumbColor = c.outline, uncheckedBorderColor = c.outline,
        ),
    )
}

/** Continuous M3 slider in Direction A colours (music volume). */
@Composable
fun RmSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, onValueChangeFinished: (() -> Unit)? = null) {
    val c = RmTheme.colors
    Slider(
        value = value, onValueChange = onValueChange, modifier = modifier, onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.secondaryContainer,
        ),
    )
}
