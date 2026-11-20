package com.rmgames.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rmgames.core.common.theme.RmTheme
import com.rmgames.core.common.ui.AudioScene
import com.rmgames.core.common.ui.LocalUiFeedback
import com.rmgames.core.common.ui.RmButton
import com.rmgames.core.common.ui.RmButtonSize
import com.rmgames.core.common.ui.RmIcons
import com.rmgames.core.data.ClaimResult
import com.rmgames.core.data.NickClaim
import kotlinx.coroutines.launch

/** What to tell the player after a claim; null = no error. */
fun errorFor(result: ClaimResult): String? = when (result) {
    is ClaimResult.Invalid -> result.reason
    ClaimResult.Taken -> "That nick is taken — try another"
    ClaimResult.Offline -> "No connection. Choosing a nick needs internet once."
    is ClaimResult.Claimed, is ClaimResult.AlreadySet -> null
}

/** "9/12" — trimmed length out of the 12-character maximum. */
fun counterFor(input: String): String = "${input.trim().length}/12"

/**
 * First-launch nick onboarding (docs/03 §3.2): shown once, before anything else, when no
 * nick is bound to this install. The nick is set once and never editable.
 */
@Composable
fun NickOnboardingRoute(claim: NickClaim, onDone: () -> Unit) {
    val feedback = LocalUiFeedback.current
    LaunchedEffect(Unit) { feedback.scene(AudioScene.Menu) }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun submit() {
        if (busy) return
        busy = true
        scope.launch {
            val result = claim.claim(input)
            busy = false
            error = errorFor(result)
            if (result is ClaimResult.Claimed || result is ClaimResult.AlreadySet) onDone()
        }
    }
    NickScreen(input, { input = it.take(16); error = null }, error, busy, ::submit)
}

@Composable
fun NickScreen(input: String, onInput: (String) -> Unit, error: String?, busy: Boolean, onContinue: () -> Unit) {
    val c = RmTheme.colors
    val d = RmTheme.dimens
    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState())
            .padding(horizontal = d.gutter, vertical = d.gapMajor),
        verticalArrangement = Arrangement.spacedBy(d.gapSection),
    ) {
        Spacer(Modifier.height(d.gapMajor))
        Text("RM GAMES", style = MaterialTheme.typography.headlineSmall, color = c.primaryInk)
        Text("CHOOSE YOUR NICK", style = MaterialTheme.typography.headlineLarge, color = c.onSurface)
        Text(
            "It's how other players see you in the daily ranking. You choose it once and it can't be changed later.",
            style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                label = { Text("Nick") },
                singleLine = true,
                isError = error != null,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onContinue() }),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.primary, focusedLabelColor = c.primaryInk, cursorColor = c.primaryInk,
                    unfocusedBorderColor = c.outline,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(error ?: "3–12 letters, numbers or _", style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) c.error else c.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(counterFor(input), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
        RmButton(
            if (busy) "Checking…" else "Continue", onContinue, Modifier.fillMaxWidth(),
            size = RmButtonSize.Xl, icon = RmIcons.Check, enabled = !busy && input.isNotBlank(),
        )
    }
}
