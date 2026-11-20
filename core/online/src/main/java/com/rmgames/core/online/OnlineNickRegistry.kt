package com.rmgames.core.online

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rmgames.core.data.NickRegistry
import com.rmgames.core.data.Reservation
import com.rmgames.core.data.nickKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** The player's anonymous identity (Firebase Anonymous Auth UID). Throws when offline. */
interface AnonymousAuth {
    suspend fun uid(): String
}

sealed interface DirectoryOutcome {
    data object Reserved : DirectoryOutcome
    data object AlreadyMine : DirectoryOutcome
    data object TakenByOther : DirectoryOutcome
    /** This UID already owns a different nick (one nick per player, forever). */
    data class HasOtherNick(val nick: String) : DirectoryOutcome
}

/** The shared `nicks/{key}` directory. Must reserve atomically. Throws when offline. */
interface NickDirectory {
    suspend fun reserve(key: String, nick: String, uid: String): DirectoryOutcome
}

/**
 * Online nick uniqueness (docs/03 §3.2, plan 09 PR 15): sign in anonymously, then reserve
 * `nicks/{nickKey}` for this UID in one transaction. "Already mine" counts as reserved, so a
 * retry after a dropped response is safe. Any failure or a call slower than [timeoutMs] is
 * [Reservation.Offline] — the player stays on the screen and can try again.
 */
class OnlineNickRegistry(
    private val auth: AnonymousAuth,
    private val directory: NickDirectory,
    private val timeoutMs: Long = 10_000,
    private val onError: (Throwable) -> Unit = {},
) : NickRegistry {
    override suspend fun reserve(nick: String): Reservation = try {
        withTimeout(timeoutMs) {
            val uid = auth.uid()
            when (val outcome = directory.reserve(nickKey(nick), nick.trim(), uid)) {
                DirectoryOutcome.Reserved, DirectoryOutcome.AlreadyMine -> Reservation.Reserved
                DirectoryOutcome.TakenByOther -> Reservation.Taken
                is DirectoryOutcome.HasOtherNick -> Reservation.AlreadyHas(outcome.nick)
            }
        }
    } catch (e: TimeoutCancellationException) {
        onError(e)
        Reservation.Offline
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onError(e)
        Reservation.Offline
    }
}

/** Firebase Anonymous Auth: reuses the signed-in user, or signs in once. */
class FirebaseAnonymousAuth(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : AnonymousAuth {
    override suspend fun uid(): String =
        auth.currentUser?.uid ?: checkNotNull(auth.signInAnonymously().await().user).uid
}

/**
 * One Firestore transaction (no half-done states): `users/{uid}` decides first — a player who
 * already has a nick keeps it — then `nicks/{key}` = { uid, nick, createdAt } is created and
 * `users/{uid}` = { nick } written together. Nicks are never updated or deleted (`firestore.rules`).
 */
class FirestoreNickDirectory(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : NickDirectory {
    override suspend fun reserve(key: String, nick: String, uid: String): DirectoryOutcome {
        val userDoc = db.collection("users").document(uid)
        val nickDoc = db.collection("nicks").document(key)
        return db.runTransaction { tx ->
            val user = tx.get(userDoc)
            val owned = user.getString("nick")
            val snap = tx.get(nickDoc)
            when {
                owned != null && nickKey(owned) != key -> DirectoryOutcome.HasOtherNick(owned)
                snap.exists() && snap.getString("uid") != uid -> DirectoryOutcome.TakenByOther
                snap.exists() -> {
                    if (owned == null) tx.set(userDoc, mapOf("nick" to snap.getString("nick")))
                    DirectoryOutcome.AlreadyMine
                }
                else -> {
                    tx.set(nickDoc, mapOf("uid" to uid, "nick" to nick, "createdAt" to FieldValue.serverTimestamp()))
                    tx.set(userDoc, mapOf("nick" to nick))
                    DirectoryOutcome.Reserved
                }
            }
        }.await()
    }
}
