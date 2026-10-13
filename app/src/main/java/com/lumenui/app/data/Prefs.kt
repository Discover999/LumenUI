package com.lumenui.app.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.lumenui.app.util.AppLog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackendRepository(private val prefs: SharedPreferences) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): List<Backend> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val stored = runCatching { json.decodeFromString<List<Backend>>(raw) }
            .getOrElse { error ->
                AppLog.e("Stored backend list is unreadable; starting empty", error)
                return emptyList()
            }
        return stored.map { it.copy(secret = Crypto.decrypt(it.secret)) }
    }

    fun save(list: List<Backend>) {
        val stored = list.map { it.copy(secret = Crypto.encrypt(it.secret)) }
        prefs.edit { putString(KEY, json.encodeToString(stored)) }
    }

    companion object {
        private const val KEY = "backends_v1"
    }
}

class SettingsRepository(private val prefs: SharedPreferences) {
    var currentBackendId: String?
        get() = prefs.getString(KEY_CURRENT, null)
        set(value) = prefs.edit { putString(KEY_CURRENT, value) }

    /** Platforms the connectivity page tests; defaults to the mainstream set. */
    var connectivityTargets: Set<String>
        get() = prefs.getStringSet(KEY_TARGETS, null) ?: ConnectivityTargets.defaultSelected
        set(value) = prefs.edit { putStringSet(KEY_TARGETS, value) }

    companion object {
        private const val KEY_CURRENT = "current_backend_id"
        private const val KEY_TARGETS = "connectivity_targets_v2"
    }
}
