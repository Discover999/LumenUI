package com.lumenui.app.data

import com.lumenui.app.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

data class Probe(val ok: Boolean, val latencyMs: Long, val version: String? = null)

/** Minimal client for the mihomo (Clash.Meta) external controller API. */
class MihomoApi(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun probe(backend: Backend): Probe = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${backend.baseUrl}/version")
                .apply {
                    if (backend.secret.isNotBlank()) {
                        header("Authorization", "Bearer ${backend.secret}")
                    }
                }
                .build()
            val start = System.currentTimeMillis()
            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                if (!response.isSuccessful) return@withContext Probe(false, latency)
                val version = runCatching {
                    json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject["version"]
                        ?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                Probe(true, latency, version)
            }
        }.getOrElse { error ->
            AppLog.w("Backend probe failed for ${backend.baseUrl}", error)
            Probe(false, -1)
        }
    }
}
