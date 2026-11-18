package com.rmgames.core.data.store

import com.rmgames.core.data.Settings

/**
 * Turns a snapshot into text and back. Payloads start with a version (`v1|`) so a future
 * format — or the same format stored remotely — can be read or migrated. [decode]
 * returns null for corrupt or unknown data instead of throwing: the app then starts from
 * defaults rather than crashing.
 */
interface SnapshotCodec<T> {
    fun encode(value: T): String
    fun decode(text: String): T?
}

private const val V1 = "v1|"

private inline fun <T> decodeV1(text: String, body: (String) -> T): T? =
    if (!text.startsWith(V1)) null else runCatching { body(text.removePrefix(V1)) }.getOrNull()

private fun entries(body: String) = if (body.isEmpty()) emptyList() else body.split(';')

/** Attempts used per day: `v1|20410:1;20411:2`. */
object AttemptsCodec : SnapshotCodec<Map<Long, Int>> {
    override fun encode(value: Map<Long, Int>) =
        V1 + value.entries.sortedBy { it.key }.joinToString(";") { "${it.key}:${it.value}" }

    override fun decode(text: String) = decodeV1(text) { body ->
        entries(body).associate { e ->
            val (day, used) = e.split(':').also { require(it.size == 2) }
            day.toLong() to used.toInt()
        }
    }
}

/** Daily best per (day, game): `v1|20410,05-soda-shake,427;…`. */
object ScoresCodec : SnapshotCodec<Map<Pair<Long, String>, Long>> {
    override fun encode(value: Map<Pair<Long, String>, Long>): String {
        value.keys.forEach { (_, id) -> require(id.none { it == ',' || it == ';' || it == '|' }) { "bad game id: $id" } }
        return V1 + value.entries.sortedWith(compareBy({ it.key.first }, { it.key.second }))
            .joinToString(";") { "${it.key.first},${it.key.second},${it.value}" }
    }

    override fun decode(text: String) = decodeV1(text) { body ->
        entries(body).associate { e ->
            val (day, id, best) = e.split(',').also { require(it.size == 3) }
            (day.toLong() to id) to best.toLong()
        }
    }
}

/** User settings: `v1|music=0.7;sfx=1;vib=1`. Unknown keys are ignored (forward compatible). */
object SettingsCodec : SnapshotCodec<Settings> {
    override fun encode(value: Settings) =
        V1 + "music=${value.musicVolume};sfx=${value.sfxEnabled.bit()};vib=${value.vibrationEnabled.bit()}"

    override fun decode(text: String) = decodeV1(text) { body ->
        val kv = entries(body).associate { e -> e.split('=').let { it[0] to it[1] } }
        val d = Settings()
        Settings(
            musicVolume = kv["music"]?.toFloat() ?: d.musicVolume,
            sfxEnabled = kv["sfx"]?.let { it == "1" } ?: d.sfxEnabled,
            vibrationEnabled = kv["vib"]?.let { it == "1" } ?: d.vibrationEnabled,
        )
    }

    private fun Boolean.bit() = if (this) "1" else "0"
}
