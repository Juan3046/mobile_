package com.rmgames.core.data

import com.rmgames.core.data.store.SnapshotStore
import com.rmgames.core.data.store.WriteThrough
import com.rmgames.core.data.store.readOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The player's identity on this device (docs/03 §3.2). Null nick = onboarding not done. */
data class PlayerProfile(val nick: String? = null)

/** Stores the player's nick on the device (same `SnapshotStore` seam as the other repositories). */
class PlayerRepository(
    store: SnapshotStore<PlayerProfile>,
    scope: CoroutineScope,
    private val onError: (Throwable) -> Unit = {},
) {
    private val source = store
    private val state = MutableStateFlow<String?>(null)
    private val writer = WriteThrough(store, scope, onError) { PlayerProfile(state.value) }

    /** The chosen nick, or null before onboarding. */
    val nick: StateFlow<String?> = state.asStateFlow()

    suspend fun load() {
        source.readOrNull(onError)?.let { state.value = it.nick }
    }

    /** Only [NickClaim] sets it, once (nicks are never editable). */
    internal fun setNick(nick: String) {
        state.value = nick
        writer.schedule()
    }
}

/** Outcome of asking the shared registry for a nick. */
enum class Reservation { Reserved, Taken, Offline }

/**
 * Reserves a unique nick for this player — **the seam to swap for Firebase** (plan 09 PR 15).
 * The Firebase version signs in anonymously and creates `nicks/{nickKey}` = { uid } in a
 * Firestore transaction (Taken if the document exists). Implementations must not throw;
 * report network problems as [Reservation.Offline].
 */
interface NickRegistry {
    suspend fun reserve(nick: String): Reservation
}

/**
 * Stand-in until Firebase is connected: accepts any valid nick on this device. Uniqueness
 * across players is only enforced once the Firebase registry replaces it (docs/09 Status).
 */
class LocalNickRegistry : NickRegistry {
    override suspend fun reserve(nick: String) = Reservation.Reserved
}

/** Uniqueness key: case and surrounding spaces don't make a different nick. */
fun nickKey(nick: String): String = nick.trim().lowercase()

/** Why [input] is not a valid nick (rules from docs/03 §3.2), or null if it is. */
fun nickProblem(input: String): String? {
    val nick = input.trim()
    return when {
        nick.length !in 3..12 -> "3–12 characters"
        !NickValidator.isValid(nick) -> "Only letters, numbers and _"
        else -> null
    }
}

sealed interface ClaimResult {
    data class Claimed(val nick: String) : ClaimResult
    data class Invalid(val reason: String) : ClaimResult
    data object Taken : ClaimResult
    data object Offline : ClaimResult
    data class AlreadySet(val nick: String) : ClaimResult
}

/** First-launch nick rules: validate, reserve once in the registry, then store it forever. */
class NickClaim(private val registry: NickRegistry, private val players: PlayerRepository) {
    suspend fun claim(input: String): ClaimResult {
        players.nick.value?.let { return ClaimResult.AlreadySet(it) }
        val nick = input.trim()
        nickProblem(nick)?.let { return ClaimResult.Invalid(it) }
        return when (runCatching { registry.reserve(nick) }.getOrDefault(Reservation.Offline)) {
            Reservation.Reserved -> { players.setNick(nick); ClaimResult.Claimed(nick) }
            Reservation.Taken -> ClaimResult.Taken
            Reservation.Offline -> ClaimResult.Offline
        }
    }
}
