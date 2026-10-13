package com.lumenui.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.util.concurrent.TimeUnit

enum class ProbeStatus { UNLOCKED, LIMITED, FLAGGED, BLOCKED, FAILED }

/** Which routine probes a platform. */
enum class ProbeKind { TRACE, NETFLIX, DISNEY, YOUTUBE, SPOTIFY, GEMINI, GITHUB, REDDIT, TIKTOK, SIMPLE }

data class ProbeTarget(
    val name: String,
    val category: String,
    val kind: ProbeKind,
    val domain: String = "",
    val url: String = "",
    val fallbackDomains: List<String> = emptyList(),
    val unsupportedRegions: Set<String> = emptySet()
)

data class ProbeResult(
    val name: String,
    val category: String,
    val status: ProbeStatus,
    val latencyMs: Long,
    val region: String? = null,
    val colo: String? = null,
    val ip: String? = null,
    val isWarp: Boolean = false,
    /** Consecutive request timings in ms; a negative value marks a failed sample. */
    val samples: List<Long> = emptyList()
)

/** Latency samples shown per card (one dot each). */
const val LATENCY_SAMPLE_COUNT = 8

/**
 * Region-unlock checks, modelled on Bettbox's media-unlock checker.
 *
 * Most platforms sit behind Cloudflare, so a single request to `<domain>/cdn-cgi/trace`
 * yields the egress IP, region (`loc`), colo and WARP flag; comparing `loc` against the
 * platform's unsupported-region set decides unlocked vs blocked. Platforms with their own
 * edge signals (Netflix, Disney+, YouTube, Spotify, Gemini, GitHub, Reddit, TikTok) get a
 * dedicated routine, and a Cloudflare challenge is reported as its own status.
 */
object ConnectivityTargets {
    const val AI = "AI 服务"
    const val STREAMING = "流媒体解锁"
    const val COMMON = "常用 & 开发"
    const val SOCIAL = "社交"
    const val CHINA = "中国大陆"
    const val GAMING = "游戏"
    const val CRYPTO = "加密 / 支付"

    private val OPENAI_UNSUPPORTED = setOf(
        "CN", "HK", "MO", "RU", "IR", "KP", "SY", "CU", "VE", "BY",
        "AF", "SS", "YE", "ZW", "MM", "SD", "SO", "CF"
    )
    private val CLAUDE_UNSUPPORTED = OPENAI_UNSUPPORTED + setOf("VN", "TR")

    private fun trace(name: String, category: String, domain: String, unsupported: Set<String> = emptySet()) =
        ProbeTarget(name, category, ProbeKind.TRACE, domain = domain, unsupportedRegions = unsupported)

    private fun simple(name: String, category: String, url: String) =
        ProbeTarget(name, category, ProbeKind.SIMPLE, url = url)

    val all: List<ProbeTarget> = listOf(
        // AI
        trace("OpenAI", AI, "chatgpt.com", OPENAI_UNSUPPORTED),
        trace("Claude", AI, "api.anthropic.com", CLAUDE_UNSUPPORTED),
        ProbeTarget("Gemini", AI, ProbeKind.GEMINI),
        trace("Grok", AI, "api.x.ai"),
        trace("Perplexity", AI, "www.perplexity.ai"),
        trace("OpenRouter", AI, "openrouter.ai"),
        trace("Poe", AI, "poe.com"),
        trace("Suno", AI, "suno.com"),

        // Streaming
        ProbeTarget("Netflix", STREAMING, ProbeKind.NETFLIX),
        ProbeTarget("Disney+", STREAMING, ProbeKind.DISNEY),
        ProbeTarget("YouTube", STREAMING, ProbeKind.YOUTUBE),
        ProbeTarget("Spotify", STREAMING, ProbeKind.SPOTIFY),
        ProbeTarget("TikTok", STREAMING, ProbeKind.TIKTOK),
        trace("Crunchyroll", STREAMING, "crunchyroll.com"),

        // Common & developer
        simple("Google", COMMON, "https://www.google.com/generate_204"),
        ProbeTarget("GitHub", COMMON, ProbeKind.GITHUB),
        trace("GitLab", COMMON, "gitlab.com"),
        simple("Wikipedia", COMMON, "https://www.wikipedia.org/"),
        trace("npm", COMMON, "registry.npmjs.org"),
        trace("cdnjs", COMMON, "cdnjs.cloudflare.com"),
        trace("unpkg", COMMON, "unpkg.com"),
        trace("Node.js", COMMON, "nodejs.org"),
        simple("Apple", COMMON, "https://www.apple.com/"),

        // Social
        trace("X (Twitter)", SOCIAL, "x.com"),
        trace("Discord", SOCIAL, "gateway.discord.gg"),
        ProbeTarget("Reddit", SOCIAL, ProbeKind.REDDIT),
        simple("Telegram", SOCIAL, "https://telegram.org/"),
        trace("Medium", SOCIAL, "medium.com"),
        trace("Stack Overflow", SOCIAL, "stackoverflow.com"),
        trace("Quora", SOCIAL, "www.quora.com"),
        trace("V2EX", SOCIAL, "v2ex.com"),

        // Mainland China
        simple("百度", CHINA, "https://www.baidu.com/"),
        simple("Bilibili", CHINA, "https://www.bilibili.com/"),
        simple("腾讯", CHINA, "https://www.qq.com/"),
        simple("网易", CHINA, "https://www.163.com/"),

        // Gaming
        simple("Steam", GAMING, "https://store.steampowered.com/"),
        trace("Epic Games", GAMING, "store.epicgames.com"),
        trace("Ubisoft", GAMING, "store.ubisoft.com"),
        trace("Humble Bundle", GAMING, "humblebundle.com"),

        // Crypto / payments
        trace("Coinbase", CRYPTO, "www.coinbase.com"),
        trace("OKX", CRYPTO, "www.okx.com"),
        trace("Kraken", CRYPTO, "www.kraken.com"),
        simple("PayPal", CRYPTO, "https://www.paypal.com/")
    )

    /** Tested by default; everything else is opt-in from the settings sheet. */
    val defaultSelected: Set<String> = setOf(
        "OpenAI", "Claude", "Gemini", "Grok",
        "Netflix", "Disney+", "YouTube", "Spotify",
        "Google", "GitHub", "Telegram",
        "X (Twitter)", "Discord",
        "百度", "Bilibili", "腾讯", "网易"
    )
}

/** Probes connectivity targets; implementations report each latency sample as it lands. */
interface ConnectivityProbe {
    suspend fun probe(target: ProbeTarget, onSample: ((Long) -> Unit)? = null): ProbeResult
}

class HttpConnectivityProbe(base: OkHttpClient) : ConnectivityProbe {

    private val followClient: OkHttpClient = base.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val noFollowClient: OkHttpClient = followClient.newBuilder()
        .followRedirects(false)
        .build()

    /** Short-timeout client for the repeated latency samples (it only needs the headers). */
    private val sampleClient: OkHttpClient = base.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun probe(
        target: ProbeTarget,
        onSample: ((Long) -> Unit)?
    ): ProbeResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val result = runCatching { check(target, start) }.getOrElse {
            ProbeResult(target.name, target.category, ProbeStatus.FAILED, System.currentTimeMillis() - start)
        }
        // A failed probe can't be sampled without waiting out a timeout per sample, so skip it.
        if (result.status == ProbeStatus.FAILED) result
        else result.copy(samples = sampleLatency(target, LATENCY_SAMPLE_COUNT, onSample))
    }

    /**
     * Times [count] requests to the target's representative URL, so the card can show latency
     * as a row of samples rather than a single number. A request that does not return a 2xx/3xx
     * is recorded as -1 so it renders as a failed dot.
     *
     * A few run at a time rather than strictly one after another: a host that is slow or
     * unreachable would otherwise cost one timeout per sample (8 timeouts ≈ 48s), which made
     * the whole test take minutes. Concurrency is capped so the samples still measure roughly
     * one-at-a-time latency.
     *
     * Samples are reported one at a time with a [SAMPLE_INTERVAL_MS] gap between them, so the
     * dots (and the running average) fill in at a readable pace instead of arriving in a burst.
     */
    private suspend fun sampleLatency(
        target: ProbeTarget,
        count: Int,
        onSample: ((Long) -> Unit)? = null
    ): List<Long> = coroutineScope {
        val url = sampleUrl(target) ?: return@coroutineScope emptyList()
        val request = request(url).get().build()
        val gate = Semaphore(SAMPLE_CONCURRENCY)
        val deferred = (0 until count).map {
            async(Dispatchers.IO) {
                gate.withPermit {
                    val start = System.currentTimeMillis()
                    val ok = runCatching {
                        sampleClient.newCall(request).execute().use { res -> res.code in 200..399 }
                    }.getOrDefault(false)
                    if (ok) System.currentTimeMillis() - start else -1L
                }
            }
        }
        // Report in sample order (not completion order) so the card fills its dots left to right.
        val out = ArrayList<Long>(count)
        deferred.forEachIndexed { index, sample ->
            val ms = sample.await()
            out.add(ms)
            onSample?.let { cb ->
                withContext(Dispatchers.Main) { cb(ms) }
                // Pace the dots (and the average they feed) so each one lands visibly.
                if (index < count - 1) delay(SAMPLE_INTERVAL_MS)
            }
        }
        out
    }

    /** URL used for the repeated latency samples: the same endpoint the probe checks, or its home. */
    private fun sampleUrl(target: ProbeTarget): String? = when (target.kind) {
        ProbeKind.TRACE -> "https://${target.domain}/cdn-cgi/trace"
        ProbeKind.SIMPLE -> target.url.ifEmpty { "https://${target.domain}/" }
        ProbeKind.NETFLIX -> "https://www.netflix.com/title/70143836"
        ProbeKind.DISNEY -> "https://www.disneyplus.com/"
        ProbeKind.YOUTUBE -> "https://www.youtube.com/premium"
        ProbeKind.SPOTIFY -> "https://www.spotify.com/signup"
        ProbeKind.GEMINI -> "https://gemini.google.com/"
        ProbeKind.GITHUB -> "https://github.com/"
        ProbeKind.REDDIT -> "https://www.reddit.com/"
        ProbeKind.TIKTOK -> "https://perfops1.byteperf.com/500b-bench.jpg"
    }

    private fun check(target: ProbeTarget, start: Long): ProbeResult = when (target.kind) {
        ProbeKind.TRACE -> checkTrace(target, start)
        ProbeKind.NETFLIX -> checkNetflix(target, start)
        ProbeKind.DISNEY -> checkDisney(target, start)
        ProbeKind.YOUTUBE -> checkYouTube(target, start)
        ProbeKind.SPOTIFY -> checkSpotify(target, start)
        ProbeKind.GEMINI -> checkGemini(target, start)
        ProbeKind.GITHUB -> checkGitHub(target, start)
        ProbeKind.REDDIT -> checkReddit(target, start)
        ProbeKind.TIKTOK -> checkTikTok(target, start)
        ProbeKind.SIMPLE -> checkSimple(target, start)
    }

    private fun ProbeTarget.done(start: Long, status: ProbeStatus, region: String? = null, colo: String? = null, ip: String? = null, warp: Boolean = false) =
        ProbeResult(name, category, status, System.currentTimeMillis() - start, region, colo, ip, warp)

    // --- Cloudflare trace ---------------------------------------------------

    private fun checkTrace(target: ProbeTarget, start: Long): ProbeResult {
        var anyResponse = false
        var flagged = false
        for (domain in listOf(target.domain) + target.fallbackDomains) {
            val response = get(followClient, "https://$domain/cdn-cgi/trace") ?: continue
            response.use { res ->
                anyResponse = true
                val body = res.body?.string().orEmpty()
                var ip: String? = null
                var loc: String? = null
                var colo: String? = null
                var warp = false
                if (res.code in 200..399 && body.isNotEmpty() && !body.contains("<!DOCTYPE")) {
                    body.lineSequence().forEach { line ->
                        val i = line.indexOf('=')
                        if (i > 0) {
                            when (line.take(i).trim()) {
                                "ip" -> ip = line.substring(i + 1).trim()
                                "loc" -> loc = line.substring(i + 1).trim()
                                "colo" -> colo = line.substring(i + 1).trim()
                                "warp" -> warp = line.substring(i + 1).trim() != "off"
                            }
                        }
                    }
                }
                colo = colo ?: coloFromRay(res.header("cf-ray"))
                val region = loc?.uppercase()?.let(::normalizeRegion)
                if (!ip.isNullOrEmpty()) {
                    val blocked = region != null && target.unsupportedRegions.contains(region)
                    return target.done(
                        start,
                        if (blocked) ProbeStatus.BLOCKED else ProbeStatus.UNLOCKED,
                        region, colo?.uppercase(), ip, warp
                    )
                }
                if (isCloudflareChallenge(res, body)) flagged = true
            }
        }
        val status = when {
            flagged -> ProbeStatus.FLAGGED
            anyResponse -> ProbeStatus.BLOCKED
            else -> ProbeStatus.FAILED
        }
        return target.done(start, status)
    }

    // --- Platform-specific routines ----------------------------------------

    private fun checkNetflix(target: ProbeTarget, start: Long): ProbeResult {
        var region: String? = null
        get(noFollowClient, "https://www.netflix.com/title/70143836")?.use { res ->
            val location = res.header("Location").orEmpty()
            region = NETFLIX_REGION.find(location)?.groupValues?.get(1)?.uppercase() ?: region
            if ((res.code == 301 || res.code == 302) && location.contains("/title/70143836")) {
                return target.done(start, ProbeStatus.UNLOCKED, region)
            }
            if (res.code == 200) return target.done(start, ProbeStatus.UNLOCKED, region)
        }
        get(noFollowClient, "https://www.netflix.com/title/81280792")?.use { res ->
            val location = res.header("Location").orEmpty()
            region = NETFLIX_REGION.find(location)?.groupValues?.get(1)?.uppercase() ?: region
            val status = if ((res.code == 301 || res.code == 302) && location.contains("/title/81280792")) {
                ProbeStatus.LIMITED
            } else {
                ProbeStatus.BLOCKED
            }
            return target.done(start, status, region)
        }
        return target.done(start, ProbeStatus.FAILED, region)
    }

    private fun checkDisney(target: ProbeTarget, start: Long): ProbeResult {
        val response = head(followClient, "https://www.disneyplus.com/")
            ?: return target.done(start, ProbeStatus.FAILED)
        response.use { res ->
            val header = res.header("physical-location") ?: res.header("region")
            val region = header?.takeIf { it.isNotEmpty() }?.uppercase()
            val status = if (res.code == 200) {
                if (res.request.url.toString().contains("unavailable")) ProbeStatus.BLOCKED else ProbeStatus.UNLOCKED
            } else {
                ProbeStatus.BLOCKED
            }
            return target.done(start, status, region)
        }
    }

    private fun checkYouTube(target: ProbeTarget, start: Long): ProbeResult {
        val response = get(followClient, "https://www.youtube.com/premium")
            ?: return target.done(start, ProbeStatus.FAILED)
        response.use { res ->
            if (res.request.url.toString().contains("sorry.google.com")) {
                return target.done(start, ProbeStatus.BLOCKED)
            }
            val body = readBody(res, 150 * 1024)
            val region = GL_REGEX.find(body)?.groupValues?.get(1)
                ?: COUNTRY_REGEX.find(body)?.groupValues?.get(1)?.uppercase()
            val status = when {
                res.code != 200 -> ProbeStatus.BLOCKED
                body.contains("Premium is not available in your country") ||
                    body.contains("unavailable in your country") -> ProbeStatus.FLAGGED
                else -> ProbeStatus.UNLOCKED
            }
            return target.done(start, status, region)
        }
    }

    private fun checkSpotify(target: ProbeTarget, start: Long): ProbeResult {
        val response = get(followClient, "https://www.spotify.com/signup")
            ?: return target.done(start, ProbeStatus.FAILED)
        response.use { res ->
            val finalUrl = res.request.url.toString()
            var raw = SPOTIFY_REGION.find(finalUrl)?.groupValues?.get(1)?.uppercase()
                ?.takeIf { it != "INT" && it != "COM" }
            if (raw == null) {
                val body = readBody(res, 48 * 1024)
                raw = GEO_COUNTRY_REGEX.find(body)?.groupValues?.get(1)?.uppercase()
            }
            val region = raw?.let(::normalizeRegion)
            val status = when {
                finalUrl.contains("why-not-available") -> ProbeStatus.BLOCKED
                res.code == 403 || res.code == 429 -> ProbeStatus.BLOCKED
                res.code < 400 -> ProbeStatus.UNLOCKED
                else -> ProbeStatus.BLOCKED
            }
            return target.done(start, status, region)
        }
    }

    private fun checkGemini(target: ProbeTarget, start: Long): ProbeResult {
        val response = get(followClient, "https://gemini.google.com/")
            ?: return target.done(start, ProbeStatus.FAILED)
        response.use { res ->
            val finalUrl = res.request.url.toString()
            if (finalUrl.contains("sorry.google.com")) {
                return target.done(start, ProbeStatus.BLOCKED)
            }
            val body = readBody(res, 200 * 1024)
            val raw = GEMINI_REGION_REGEX.find(body)?.groupValues?.get(1)
                ?: GEMINI_REGION_ALT_REGEX.find(body)?.groupValues?.get(1)
            val region = raw?.let(::normalizeRegion)
            val unsupported = (raw != null && GEMINI_UNSUPPORTED.contains(raw)) ||
                (region != null && GEMINI_UNSUPPORTED.contains(region))
            val unavailable = finalUrl.contains("unavailable") ||
                body.contains("is not supported in your country") ||
                body.contains("isn't supported in your country") ||
                body.contains("unavailable in your country") ||
                body.contains("is not currently supported") ||
                body.contains("isn't currently supported")
            val status = when {
                res.code != 200 || unavailable -> ProbeStatus.BLOCKED
                unsupported -> ProbeStatus.BLOCKED
                else -> ProbeStatus.UNLOCKED
            }
            return target.done(start, status, region)
        }
    }

    private fun checkGitHub(target: ProbeTarget, start: Long): ProbeResult {
        val response = head(followClient, "https://github.com/")
            ?: return target.done(start, ProbeStatus.FAILED)
        response.use { res ->
            val region = res.header("x-github-edge-region")?.takeIf { it.isNotEmpty() }
                ?.uppercase()?.let(::normalizeRegion)
            val status = when (res.code) {
                200 -> ProbeStatus.UNLOCKED
                403 -> ProbeStatus.BLOCKED
                else -> ProbeStatus.FAILED
            }
            return target.done(start, status, region)
        }
    }

    private fun checkReddit(target: ProbeTarget, start: Long): ProbeResult {
        val response = head(followClient, "https://www.reddit.com/")
            ?: return target.done(start, ProbeStatus.FAILED)
        response.use { res ->
            val timing = res.header("server-timing").orEmpty()
            val region = REDDIT_POP_REGEX.find(timing)?.groupValues?.get(1)?.let(::normalizeRegion)
            val status = when {
                res.code == 200 -> ProbeStatus.UNLOCKED
                res.code == 403 -> ProbeStatus.BLOCKED
                res.code in 200..399 -> ProbeStatus.UNLOCKED
                else -> ProbeStatus.BLOCKED
            }
            return target.done(start, status, region)
        }
    }

    private fun checkTikTok(target: ProbeTarget, start: Long): ProbeResult {
        var response = head(followClient, "https://perfops1.byteperf.com/500b-bench.jpg")
        if (response == null || response.code != 200) {
            response?.close()
            response = head(followClient, "https://perfops2.byteperf.com/500b-bench.jpg")
        }
        val res = response ?: return target.done(start, ProbeStatus.FAILED)
        res.use {
            val via = res.header("via").orEmpty()
            val cinfo = res.header("x-response-cinfo") ?: res.header("x-request-ip").orEmpty()
            val raw = TIKTOK_VIA_REGEX.find(via)?.groupValues?.get(1)
                ?: Regex("\\b([A-Z]{2})\\b").find(cinfo.replace(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"), ""))?.groupValues?.get(1)
            val region = raw?.let(::normalizeRegion)
            val status = if (res.code in 200..399) ProbeStatus.UNLOCKED else ProbeStatus.BLOCKED
            return target.done(start, status, region)
        }
    }

    private fun checkSimple(target: ProbeTarget, start: Long): ProbeResult {
        val url = target.url.ifEmpty { "https://${target.domain}/" }
        val response = get(followClient, url) ?: return target.done(start, ProbeStatus.FAILED)
        response.use { res ->
            val status = if (res.code in 200..399) ProbeStatus.UNLOCKED else ProbeStatus.BLOCKED
            return target.done(start, status)
        }
    }

    // --- Helpers ------------------------------------------------------------

    private fun get(client: OkHttpClient, url: String): Response? = runCatching {
        client.newCall(request(url).get().build()).execute()
    }.getOrNull()

    private fun head(client: OkHttpClient, url: String): Response? = runCatching {
        client.newCall(request(url).head().build()).execute()
    }.getOrNull()

    private fun request(url: String) = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Accept-Language", "en-US,en;q=0.9")

    /** Reads at most [maxBytes] of the body, enough for the markers we look for. */
    private fun readBody(res: Response, maxBytes: Int): String {
        val source = res.body?.source() ?: return ""
        val buffer = Buffer()
        val chunk = Buffer()
        while (buffer.size < maxBytes) {
            val read = runCatching { source.read(chunk, (maxBytes - buffer.size).toLong()) }.getOrNull() ?: break
            if (read == -1L) break
            buffer.write(chunk, read)
        }
        return buffer.readUtf8()
    }

    private fun coloFromRay(ray: String?): String? {
        if (ray == null) return null
        val idx = ray.lastIndexOf('-')
        if (idx < 0 || idx >= ray.length - 1) return null
        val code = ray.substring(idx + 1).trim().uppercase()
        return if (code.length in 3..4) code else null
    }

    private fun isCloudflareChallenge(res: Response, body: String): Boolean {
        if (res.header("cf-mitigated")?.lowercase() == "challenge") return true
        val server = res.header("server")?.lowercase().orEmpty()
        val isCloudflare = server.contains("cloudflare") || res.header("cf-ray") != null
        if (!isCloudflare) return false
        if (res.code == 403 || res.code == 503) return true
        return body.contains("challenges.cloudflare.com") ||
            body.contains("cf-chl-") ||
            body.contains("/cdn-cgi/challenge-platform/") ||
            body.contains("cf-turnstile") ||
            body.contains("__CF\$cv\$params")
    }

    internal companion object {
        /** How many latency samples run at once. */
        const val SAMPLE_CONCURRENCY = 4

        /** Minimum gap between reporting two samples, so the dots don't pop in a burst. */
        const val SAMPLE_INTERVAL_MS = 300L

        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

        val NETFLIX_REGION = Regex("netflix\\.com/([a-z]{2})(?:-[a-z]{2})?/title/")
        val SPOTIFY_REGION = Regex("spotify\\.com/([a-z]{2})(?:-[a-z]{2})?/")
        val GEO_COUNTRY_REGEX = Regex("geoCountry\"?\\s*:\\s*\"([A-Z]{2})\"")
        val GL_REGEX = Regex("\"INNERTUBE_CONTEXT_GL\"\\s*:\\s*\"([A-Z]{2})\"")
        val COUNTRY_REGEX = Regex("\"countryCode\"\\s*:\\s*\"([A-Z]{2})\"")
        val GEMINI_REGION_REGEX = Regex(",2,1,200,\"([A-Z]{3})\"")
        val GEMINI_REGION_ALT_REGEX = Regex("\\[1,null,null,\\d+,\\d+,\"([A-Z]{3})\"")
        val REDDIT_POP_REGEX = Regex("p=([A-Z]{3})")
        val TIKTOK_VIA_REGEX = Regex("oversea-([A-Z]{2})-", RegexOption.IGNORE_CASE)

        val GEMINI_UNSUPPORTED = setOf(
            "CN", "CHN", "MO", "MAC", "RU", "RUS", "BY", "BLR", "IR", "IRN",
            "KP", "PRK", "SY", "SYR", "CU", "CUB", "VE", "VEN", "MM", "MMR",
            "SD", "SDN", "AF", "AFG", "SS", "SSD", "YE", "YEM", "ZW", "ZWE"
        )

        val REGION_ALIASES = mapOf(
            "TWN" to "TW", "NTW" to "TW", "TWD" to "TW",
            "JPN" to "JP", "JPY" to "JP",
            "KOR" to "KR", "KRW" to "KR",
            "SGP" to "SG", "SGD" to "SG",
            "GBR" to "GB", "GBP" to "GB",
            "USA" to "US", "USD" to "US",
            "HKG" to "HK", "HKD" to "HK",
            "CHN" to "CN", "RUS" to "RU", "IRN" to "IR", "PRK" to "KP",
            "SYR" to "SY", "CUB" to "CU", "BLR" to "BY", "VEN" to "VE",
            "MMR" to "MM", "SDN" to "SD", "AFG" to "AF", "SSD" to "SS",
            "YEM" to "YE", "ZWE" to "ZW", "MAC" to "MO",
            "DEU" to "DE", "FRA" to "FR", "NLD" to "NL", "CAN" to "CA",
            "AUS" to "AU", "IND" to "IN", "BRA" to "BR", "MEX" to "MX",
            "ITA" to "IT", "ESP" to "ES", "SWE" to "SE", "CHE" to "CH",
            "AUT" to "AT", "POL" to "PL", "TUR" to "TR", "VNM" to "VN",
            "THA" to "TH", "MYS" to "MY", "IDN" to "ID", "PHL" to "PH",
            "ARG" to "AR", "CHL" to "CL", "ZAF" to "ZA", "ARE" to "AE",
            "ISR" to "IL", "SAU" to "SA", "EGY" to "EG", "NGA" to "NG",
            "KEN" to "KE", "NZL" to "NZ", "IRL" to "IE", "PRT" to "PT",
            "GRC" to "GR", "CZE" to "CZ", "ROU" to "RO", "UKR" to "UA",
            "HUN" to "HU", "FIN" to "FI", "NOR" to "NO", "DNK" to "DK",
            "BEL" to "BE", "COL" to "CO", "PER" to "PE",
            // GitHub reports AWS-style edge regions.
            "SOUTHEASTASIA" to "SG", "EASTASIA" to "HK", "EASTUS" to "US",
            "EASTUS2" to "US", "WESTUS" to "US", "WESTUS2" to "US",
            "CENTRALUS" to "US", "SOUTHCENTRALUS" to "US", "NORTHCENTRALUS" to "US",
            "WESTEUROPE" to "NL", "NORTHEUROPE" to "IE", "JAPANEAST" to "JP",
            "JAPANWEST" to "JP", "AUSTRALIAEAST" to "AU", "CENTRALINDIA" to "IN",
            "SOUTHINDIA" to "IN", "BRAZILSOUTH" to "BR", "SOUTHAMERICA" to "BR",
            "UKSOUTH" to "GB", "FRANCECENTRAL" to "FR", "GERMANYWESTCENTRAL" to "DE",
            "CANADACENTRAL" to "CA", "KOREACENTRAL" to "KR", "SWITZERLANDNORTH" to "CH"
        )

        fun normalizeRegion(code: String): String? {
            val upper = code.uppercase()
            if (upper.length == 2) return upper
            return REGION_ALIASES[upper]
        }
    }
}
