package com.rmgames.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rmgames.core.common.theme.RmTheme
import com.rmgames.core.common.ui.AudioScene
import com.rmgames.core.common.ui.LocalUiFeedback
import com.rmgames.core.common.ui.RmButton
import com.rmgames.core.common.ui.RmButtonVariant
import com.rmgames.core.common.ui.RmCard
import com.rmgames.core.common.ui.RmIcons
import com.rmgames.core.common.ui.RmListDivider
import com.rmgames.core.common.ui.RmListItem
import com.rmgames.core.common.ui.RmSlider
import com.rmgames.core.common.ui.RmSwitch
import com.rmgames.core.common.ui.RmTopBar
import com.rmgames.core.common.ui.SectionHeader
import com.rmgames.core.data.Settings
import com.rmgames.core.data.SettingsRepository

/**
 * Settings route (docs/03 §3.6, plan 09 Decision 3): reads and writes [SettingsRepository].
 *
 * @param onResetAttempts debug builds only (null hides the debug section).
 */
@Composable
fun SettingsRoute(
    repository: SettingsRepository,
    appVersion: String,
    nick: String? = null,
    onBack: () -> Unit,
    onResetAttempts: (() -> Unit)? = null,
) {
    val settings by repository.settings.collectAsStateWithLifecycle()
    val feedback = LocalUiFeedback.current
    LaunchedEffect(Unit) { feedback.scene(AudioScene.Menu) }
    SettingsScreen(
        settings = settings,
        appVersion = appVersion,
        nick = nick,
        onChange = { change -> repository.update(change) },
        onBack = onBack,
        onResetAttempts = onResetAttempts,
    )
}

/** Stateless Settings: Sound, Feedback, Player, Privacy, Credits, (Debug), version. */
@Composable
fun SettingsScreen(
    settings: Settings,
    appVersion: String,
    nick: String? = null,
    onChange: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
    onResetAttempts: (() -> Unit)? = null,
) {
    val c = RmTheme.colors
    val d = RmTheme.dimens
    Column(Modifier.fillMaxSize().background(c.background)) {
        RmTopBar("Settings", navigationIcon = RmIcons.Back, onNavigate = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = d.gutter).padding(bottom = d.gapMajor),
            verticalArrangement = Arrangement.spacedBy(d.gapRelated),
        ) {
            Group("Sound") {
                // Local slider value while dragging; saved once on release (one write, not dozens).
                var volume by remember(settings.musicVolume) { mutableFloatStateOf(settings.musicVolume) }
                RmListItem("Music", supporting = volumeLabel(volume), icon = RmIcons.Music)
                Row(Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp, bottom = 8.dp)) {
                    RmSlider(
                        value = volume,
                        onValueChange = { volume = it },
                        onValueChangeFinished = { onChange { s -> s.copy(musicVolume = volume) } },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = volumeDescription(volume) },
                    )
                }
                RmListDivider()
                ToggleRow("Sound effects", "Clicks, countdown and game sounds", RmIcons.Volume, settings.sfxEnabled) { on ->
                    onChange { it.copy(sfxEnabled = on) }
                }
            }
            Group("Feedback") {
                ToggleRow("Vibration", "Buttons, countdown and game feedback", RmIcons.Vibrate, settings.vibrationEnabled) { on ->
                    onChange { it.copy(vibrationEnabled = on) }
                }
            }
            Group("Player") {
                RmListItem(nick ?: "Nick", supporting = if (nick != null) "Your nick · set once, can't be changed" else "Not chosen yet", icon = RmIcons.User)
            }
            Group("Privacy") {
                RmListItem(
                    "Your data stays on this phone",
                    supporting = "Attempts, scores and settings are stored only on this device. No account, no personal data.",
                    icon = RmIcons.Lock,
                )
            }
            Group("Credits & licences") {
                Credits.fonts.forEachIndexed { i, f ->
                    if (i > 0) RmListDivider(inset = false)
                    RmListItem(f.name, supporting = "${f.use} · ${f.copyright} · ${f.licence}")
                }
            }
            if (onResetAttempts != null) {
                Group("Debug") {
                    RmListItem("Reset today's attempts", supporting = "Debug builds only. Scores and seasons are kept.",
                        icon = RmIcons.Bug, onClick = onResetAttempts, role = Role.Button)
                }
            }
            Text(
                "RM Games $appVersion",
                Modifier.fillMaxWidth().padding(top = d.gapSection),
                style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = RmTheme.dimens.gapRelated), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(title)
        RmCard(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { content() } }
    }
}

/** Whole row toggles (big touch target); the switch mirrors it. */
@Composable
private fun ToggleRow(title: String, supporting: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onToggle: (Boolean) -> Unit) {
    RmListItem(title, supporting = supporting, icon = icon, onClick = { onToggle(!checked) }, role = Role.Switch) {
        RmSwitch(checked = checked, onCheckedChange = null)
    }
}
