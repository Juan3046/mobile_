package com.rmgames.core.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rmgames.core.common.theme.RmTheme

/**
 * Primary tabs (M3): equal-width labels with a 3dp primary-ink indicator under the
 * selected one, a hairline below the row. Flat — the ledge stays on actions and cards.
 */
@Composable
fun RmTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = RmTheme.colors
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { i, label ->
                val isSelected = i == selected
                val click = rememberRmClick(true) { onSelect(i) }
                Column(
                    Modifier.weight(1f).heightIn(min = 48.dp)
                        .semantics { this.selected = isSelected }
                        .clickable(role = Role.Tab, onClick = click),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.weight(1f, fill = false).padding(vertical = 12.dp)) {
                        Text(label, style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) c.primaryInk else c.onSurfaceVariant)
                    }
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp)
                            .background(if (isSelected) c.primaryInk else c.background, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                    )
                }
            }
        }
        RmListDivider(inset = false)
    }
}
