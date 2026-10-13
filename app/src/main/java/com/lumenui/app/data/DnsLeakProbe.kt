package com.lumenui.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** One DNS resolver bash.ws saw answering, with whatever geo/ASN it could attach. */
data class DnsLeakNode(
    val ip: String,
    val countryCode: String?,
    val countryName: String?,
    val asn: String?
)

data class DnsLeakResult(
    /** The resolvers that actually answered the probes. */
    val resolvers: List<DnsLeakNode>,
    /** bash.ws' own verdict line, e.g. "DNS may be leaking.". */
    val conclusion: String?
) {
    /** true/false from bash.ws' conclusion, or null when it could not decide. */
    val leaking: Boolean?
        get() = conclusion?.lowercase()?.let {
            when {
                "not leak" in it || "no leak" in it -> false
                "leak" in it -> true
                else -> null
            }
        }
}

/**
 * DNS-leak detection over bash.ws's public test, mirroring the flow its own page runs:
 * start a test for a run id, fire a burst of requests at distinct random names under
 * `<run_id>.bash.ws` (that lookup is the leak path — the authoritative NS records which
 * resolver asked), then poll for the report and read back the resolvers plus bash.ws' verdict.
 *
 * The probes are real HTTP requests, not bare DNS lookups: under a proxy's fake-ip DNS a
 * local lookup only yields a synthetic address and never reaches the upstream resolver, so
 * bash.ws would see nothing. The request forces the proxy/tunnel to resolve the name for real.
 *
 * Only meaningful when the device's traffic is actually tunneled; if it is not, the "leak"
 * just reflects the phone's normal DNS.
 */
class DnsLeakProbe(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Short timeouts for the throwaway probe requests: only the lookup matters, not the reply. */
    private val probeClient = client.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun run(probes: Int = PROBE_COUNT): DnsLeakResult? = withContext(Dispatchers.IO) {
        runCatching {
            val start = post(BASE, mapOf("ajax" to "1", "start" to "1", "url" to "/dnsleak"))
            val runId = json.parseToJsonElement(start).jsonObject["run_id"]?.jsonPrimitive?.contentOrNull
                ?: return@withContext null

            // Distinct random names, so each is a fresh lookup the resolver can't cache away.
            coroutineScope {
                (1..probes).map {
                    async(Dispatchers.IO) {
                        val name = Random.nextLong(1_000_000_000L, 10_000_000_000L)
                        runCatching {
                            probeClient.newCall(
                                Request.Builder()
                                    .url("https://$name.$runId.bash.ws/")
                                    .header("User-Agent", PROBE_USER_AGENT)
                                    .build()
                            ).execute().close()
                        }
                    }
                }.awaitAll()
            }

            val testUrl = "/dnsleak/test/$runId"
            for (attempt in 0 until POLL_ATTEMPTS) {
                val poll = json.parseToJsonElement(post("$BASE$testUrl", checkBody(testUrl))).jsonObject
                if ((poll["done"]?.jsonPrimitive?.intOrNull ?: 0) == 1) {
                    return@withContext parse(poll["result"]?.jsonPrimitive?.contentOrNull.orEmpty())
                }
                delay(POLL_INTERVAL_MS)
            }
            null
        }.getOrNull()
    }

    /** Parses bash.ws' result table: one `<div class="tr">` per resolver, plus a verdict line. */
    internal fun parse(html: String): DnsLeakResult {
        val resolvers = html.split("<div class=\"tr\">").drop(1).mapNotNull { row ->
            val ip = Host.find(row)?.groupValues?.get(1) ?: return@mapNotNull null
            DnsLeakNode(
                ip = ip,
                countryCode = Ctr.find(row)?.groupValues?.get(1)?.uppercase(),
                countryName = CtrName.find(row)?.groupValues?.get(1),
                asn = Asn.find(row)?.groupValues?.get(1)
            )
        }
        val conclusion = Conclusion.find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        return DnsLeakResult(resolvers, conclusion)
    }

    private fun post(url: String, form: Map<String, String>): String {
        val body = FormBody.Builder().apply { form.forEach { (key, value) -> add(key, value) } }.build()
        return client.newCall(
            Request.Builder().url(url).header("User-Agent", PROBE_USER_AGENT).post(body).build()
        ).execute().use { it.body?.string().orEmpty() }
    }

    private fun checkBody(testUrl: String) =
        mapOf("ajax" to "1", "checkrun" to "1", "url" to testUrl)

    private companion object {
        const val BASE = "https://bash.ws"
        const val PROBE_COUNT = 30
        const val POLL_ATTEMPTS = 15
        const val POLL_INTERVAL_MS = 1500L

        val Host = Regex("data-host=\"([^\"]+)\"")
        val Ctr = Regex("data-ctr=\"([^\"]+)\"")
        val CtrName = Regex("data-ctrname=\"([^\"]+)\"")
        val Asn = Regex("<div class=\"td\">(AS[^<]*)</div>")
        val Conclusion = Regex("<div class=\"conclusion\">([^<]*)</div>")
    }
}
