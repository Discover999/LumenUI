package com.lumenui.app.data

import com.lumenui.app.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

data class Probe(val ok: Boolean, val latencyMs: Long, val version: String? = null)

/** Client for the mihomo (Clash.Meta) external controller API. */
class MihomoApi(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun probe(backend: Backend): Probe = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.currentTimeMillis()
            client.newCall(request(backend, "version")).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                if (!response.isSuccessful) return@withContext Probe(false, latency)
                val version = runCatching {
                    json.parseToJsonElement(response.body.string()).jsonObject["version"]
                        ?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                Probe(true, latency, version)
            }
        }.getOrElse { error ->
            AppLog.w("Backend probe failed for ${backend.baseUrl}", error)
            Probe(false, -1)
        }
    }

    /**
     * Builds a controller request. [segments] are appended to the backend's own base URL
     * (which may carry a secondary path) and URL-encoded.
     */
    private fun request(backend: Backend, vararg segments: String): Request {
        val base = backend.baseUrl.toHttpUrlOrNull()
            ?: error("Unusable backend URL: ${backend.baseUrl}")
        val url = newBaseUrl(base, segments)
        return Request.Builder()
            .url(url)
            .get()
            .apply { if (backend.secret.isNotBlank()) header("Authorization", "Bearer ${backend.secret}") }
            .build()
    }

    private fun newBaseUrl(base: okhttp3.HttpUrl, segments: Array<out String>) =
        base.newBuilder().apply { segments.forEach { addPathSegment(it) } }.build()
}
