package com.lumenui.app.data

import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One finished speed test, kept in newest-first order. */
@Serializable
data class SpeedHistoryEntry(
    val timestamp: Long,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val latencyMs: Long? = null,
    val jitterMs: Double? = null,
    val provider: String = SpeedProvider.CLOUDFLARE.name,
    val uploadUnavailable: Boolean = false,
    val egressIp: String? = null,
    val egressRegion: String? = null,
    val egressColo: String? = null
)

/**
 * Persists the last [MAX_ENTRIES] results. Kept small on purpose: this is a personal app's
 * recent-history list, not an analytics store.
 */
class SpeedHistoryStore(private val prefs: SharedPreferences) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<SpeedHistoryEntry> = runCatching {
        prefs.getString(KEY, null)?.let { json.decodeFromString<List<SpeedHistoryEntry>>(it) }
    }.getOrNull().orEmpty()

    fun add(entry: SpeedHistoryEntry) {
        val next = (listOf(entry) + load()).take(MAX_ENTRIES)
        runCatching { json.encodeToString(next) }
            .onSuccess { prefs.edit().putString(KEY, it).apply() }
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "speed_history_v1"
        const val MAX_ENTRIES = 50
    }
}
