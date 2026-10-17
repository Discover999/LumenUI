package com.lumenui.app.data

import com.lumenui.app.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** How much a platform's anti-abuse checks are likely to distrust an egress address. */
enum class IpRisk { LOW, MEDIUM, HIGH }

/**
 * Egress-IP reputation from ip-api.com's free JSON endpoint (HTTP only, no key).
 * The free tier returns boolean flags and ASN/PTR details rather than a numeric score, so the
 * risk is derived from the flags the service itself reports — proxy and datacenter are the
 * carrier-grade tells platforms tend to distrust, a missing reverse record is a weaker one.
 * Nothing here is a third-party risk score, and the UI says so.
 */
@Serializable
data class IpReputation(
    val status: String = "fail",
    val proxy: Boolean = false,
    val hosting: Boolean = false,
    val mobile: Boolean = false,
    val country: String = "",
    val countryCode: String = "",
    val city: String = "",
    val isp: String = "",
    val org: String = "",
    @SerialName("as") val asn: String = "",
    val asname: String = "",
    val reverse: String = ""
) {
    val label: String
        get() = when {
            proxy -> "代理"
            hosting -> "机房"
            mobile -> "移动"
            else -> "住宅"
        }

    val risk: IpRisk
        get() = when {
            proxy || hosting -> IpRisk.HIGH
            reverse.isBlank() -> IpRisk.MEDIUM
            else -> IpRisk.LOW
        }

    /** The observations behind [risk], so the grade is explained rather than asserted. */
    val riskSignals: List<String>
        get() = buildList {
            if (proxy) add("代理标记")
            if (hosting) add("机房 IP")
            if (reverse.isBlank()) add("无反向解析")
            if (isEmpty()) add("无代理/机房标记")
        }

    /** ISP and network number, e.g. `Cloudflare, Inc. · AS13335`. */
    val operator: String
        get() = listOf(isp.ifBlank { org }.ifBlank { asname }, asNumber)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

    /** Just the AS number; the `as` field also repeats the ISP name already shown. */
    val asNumber: String
        get() = asn.substringBefore(' ').takeIf { it.startsWith("AS", ignoreCase = true) }.orEmpty()

    /** Where the address is registered, e.g. `South Brisbane · AU`. */
    val location: String
        get() = listOf(city, countryCode.ifBlank { country })
            .filter { it.isNotBlank() }
            .joinToString(" · ")
}

/** Looks up the reputation of an egress IP. */
interface IpReputationApi {
    suspend fun lookup(ip: String): IpReputation?
}

class HttpIpReputationApi(private val client: OkHttpClient) : IpReputationApi {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun lookup(ip: String): IpReputation? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(
                    "http://ip-api.com/json/$ip" +
                        "?fields=status,proxy,hosting,mobile,country,countryCode,city,isp,org,as,asname,reverse"
                )
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val parsed = json.decodeFromString<IpReputation>(response.body.string())
                parsed.takeIf { it.status == "success" }
            }
        }.getOrElse { error ->
            AppLog.w("IP reputation lookup failed for $ip", error)
            null
        }
    }
}
