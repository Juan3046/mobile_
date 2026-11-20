package com.rmgames.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.rmgames.core.data.LocalNickRegistry
import com.rmgames.core.data.NickRegistry
import com.rmgames.core.data.OfflineRanking
import com.rmgames.core.data.Ranking

/**
 * Online features need the per-developer `app/google-services.json` (git-ignored). Without it
 * the build skips the google-services plugin, no FirebaseApp exists, and the app keeps the
 * local stand-ins — it always builds and runs offline.
 */
internal fun chooseNickRegistry(firebaseConfigured: Boolean, online: () -> NickRegistry): NickRegistry =
    if (firebaseConfigured) online() else LocalNickRegistry()

internal fun chooseRanking(firebaseConfigured: Boolean, online: () -> Ranking): Ranking =
    if (firebaseConfigured) online() else OfflineRanking()

internal fun isFirebaseConfigured(context: Context): Boolean =
    runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)
