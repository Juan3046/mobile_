package com.rmgames.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rmgames.core.common.GameEntry

/**
 * Pure decision state of the permission gate — *what to show*. The composable owns
 * the Android glue (system checks/requests); this part is JVM-unit-tested.
 * See docs/08-implementation-plan.md (Game Hosting Contract).
 */
class PermissionGateModel(
    val requiredPermissions: List<String>,
    initiallyGranted: Boolean,
) {
    /** True once the run may start: nothing required, or everything granted. */
    var isContentVisible: Boolean = initiallyGranted || requiredPermissions.isEmpty()
        private set

    /** True once the system dialog denied a permission — enables the settings hint. */
    var wasDenied: Boolean = false
        private set

    fun onPermissionsResult(results: Map<String, Boolean>) {
        isContentVisible = requiredPermissions.isNotEmpty() &&
            requiredPermissions.all { results[it] == true }
        if (!isContentVisible) wasDenied = true
    }

    fun onHostResumed(granted: Boolean) {
        isContentVisible = granted || requiredPermissions.isEmpty()
    }
}

/**
 * Session entry gate — the permission UX lives in `:app` by contract
 * (docs/08-implementation-plan.md). While [GameEntry.requiredPermissions] are not
 * held, shows the explanation/fallback (docs/02 + docs/games/03-blink-sniper.md)
 * instead of starting the run: a denied permission never records a fake score.
 *
 * Grant → content; denial → fallback with an "open settings" hint (covers "don't ask
 * again"); returning from Settings re-checks on ON_RESUME (and a mid-run revocation
 * closes the run). Hardware fallback copy (front camera missing) and home-level
 * Play disabling land in Phase 2.
 *
 * @param entry resolved from the registry; null = unknown game, content shows anyway.
 */
@Composable
fun PermissionGate(entry: GameEntry?, content: @Composable () -> Unit) {
    if (entry == null || entry.requiredPermissions.isEmpty()) {
        content()
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val model = remember(entry) {
        PermissionGateModel(
            requiredPermissions = entry.requiredPermissions,
            initiallyGranted = entry.requiredPermissions.all { it.isGranted(context) },
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results -> model.onPermissionsResult(results) }

    DisposableEffect(lifecycleOwner, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                model.onHostResumed(model.requiredPermissions.all { it.isGranted(context) })
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (model.isContentVisible) {
        content()
    } else {
        PermissionFallback(
            gameTitle = entry.game.title,
            permissions = model.requiredPermissions,
            showSettingsHint = model.wasDenied,
            onAllow = { launcher.launch(model.requiredPermissions.toTypedArray()) },
            onOpenSettings = { context.openAppSettings() },
        )
    }
}

@Composable
private fun PermissionFallback(
    gameTitle: String,
    permissions: List<String>,
    showSettingsHint: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val labels = permissions.joinToString(", ") { it.toPermissionLabel() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$gameTitle requires the $labels permission to play.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAllow) {
            Text("ALLOW ${labels.uppercase()}")
        }
        if (showSettingsHint) {
            TextButton(onClick = onOpenSettings) {
                Text("OPEN SETTINGS")
            }
            Text(
                text = "Permission denied. You can enable it in system settings.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun String.isGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, this) == PackageManager.PERMISSION_GRANTED

private fun String.toPermissionLabel(): String = when (this) {
    Manifest.permission.CAMERA -> "camera"
    else -> substringAfterLast('.')
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
    )
}
