package com.lumenui.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Egress-IP reputation from ip-api.com's free JSON endpoint (HTTP only, no key).
 * The free tier returns boolean flags rather than a numeric score, so the risk
 * reads as a category: proxy / datacenter are the carrier-grade tells platforms
 * tend to distrust, while residential is the clean case.
 */
@Serializable
data class IpReputation(
    val status: String = "fail",
    val proxy: Boolean = false,
    val hosting: Boolean = false,
    val mobile: Boolean = false
) {
    val label: String
        get() = when {
            proxy -> "代理"
            hosting -> "机房"
            mobile -> "移动"
            else -> "住宅"
        }
}

class IpReputationApi(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun lookup(ip: String): IpReputation? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("http://ip-api.com/json/$ip?fields=status,proxy,hosting,mobile")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val parsed = json.decodeFromString<IpReputation>(response.body?.string().orEmpty())
                parsed.takeIf { it.status == "success" }
            }
        }.getOrNull()
    }
}
