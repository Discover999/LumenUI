package com.lumenui.app.data

import com.lumenui.app.util.AppLog
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
        val result = runCatching { check(target, start) }.getOrElse { error ->
            AppLog.w("Probe routine failed for ${target.name}", error)
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
        ProbeKind.HBO -> target.url.ifEmpty { "https://www.hbomax.com/" }
        ProbeKind.PING -> target.url.ifEmpty { "https://${target.domain}/" }
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
        ProbeKind.HBO -> checkHbo(target, start)
        ProbeKind.PING -> checkPing(target, start)
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
                val body = res.body.string()
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
                val region = loc?.uppercase()?.let(RegionCodes::normalize)
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
            val region = raw?.let(RegionCodes::normalize)
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
            val region = raw?.let(RegionCodes::normalize)
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
                ?.uppercase()?.let(RegionCodes::normalize)
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
            val region = REDDIT_POP_REGEX.find(timing)?.groupValues?.get(1)?.let(RegionCodes::normalize)
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
            val region = raw?.let(RegionCodes::normalize)
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

    /**
     * Reachability-only probe. Reports UNLOCKED when the site answers and BLOCKED when it does
     * not, and deliberately makes no claim about region availability — there is no established
     * criterion for these platforms, so calling a reachable site "available" would be a guess.
     */
    private fun checkPing(target: ProbeTarget, start: Long): ProbeResult {
        val url = sampleUrl(target) ?: return target.done(start, ProbeStatus.FAILED)
        val response = get(followClient, url) ?: return target.done(start, ProbeStatus.FAILED)
        response.use { res ->
            val status = if (res.code in 200..399) ProbeStatus.UNLOCKED else ProbeStatus.BLOCKED
            return target.done(start, status)
        }
    }

    /**
     * HBO Max region-unlock check, following RegionRestrictionCheck: request the site root,
     * then compare the region the edge reports against the list of regions the page offers.
     *
     * The edge names the viewer's region in a `Set-Cookie: countryCode=XX` (not a plain
     * `countryCode` header, and not in the body), so the cookies must be scanned rather than
     * a single header read — reading `header("countryCode")` always misses it.
     */
    private fun checkHbo(target: ProbeTarget, start: Long): ProbeResult {
        val url = target.url.ifEmpty { "https://www.hbomax.com/" }
        val response = get(followClient, url) ?: return target.done(start, ProbeStatus.FAILED)
        response.use { res ->
            val region = res.headers("Set-Cookie")
                .firstNotNullOfOrNull { HBO_COUNTRY_COOKIE.find(it)?.groupValues?.get(1) }
                ?.uppercase()
            if (region == null) {
                // Reachable but the edge did not name a region, so availability cannot be judged.
                // Report the failure without a region rather than inventing one.
                return target.done(start, ProbeStatus.BLOCKED)
            }
            val body = runCatching { res.body.string() }.getOrDefault("")
            val offered = HBO_COUNTRY_URL.findAll(body)
                .map { it.groupValues[1].uppercase() }
                .toSet() + "US"
            val status = if (region in offered) ProbeStatus.UNLOCKED else ProbeStatus.BLOCKED
            // The region is reported either way, so a blocked card still shows where it was seen.
            return target.done(start, status, region)
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
        .header("User-Agent", PROBE_USER_AGENT)
        .header("Accept-Language", "en-US,en;q=0.9")

    /** Reads at most [maxBytes] of the body, enough for the markers we look for. */
    private fun readBody(res: Response, maxBytes: Int): String {
        val source = res.body.source()
        val buffer = Buffer()
        val chunk = Buffer()
        while (buffer.size < maxBytes) {
            val read = runCatching { source.read(chunk, maxBytes - buffer.size) }.getOrNull() ?: break
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

        val NETFLIX_REGION = Regex("netflix\\.com/([a-z]{2})(?:-[a-z]{2})?/title/")
        val SPOTIFY_REGION = Regex("spotify\\.com/([a-z]{2})(?:-[a-z]{2})?/")
        val GEO_COUNTRY_REGEX = Regex("geoCountry\"?\\s*:\\s*\"([A-Z]{2})\"")
        val GL_REGEX = Regex("\"INNERTUBE_CONTEXT_GL\"\\s*:\\s*\"([A-Z]{2})\"")
        val COUNTRY_REGEX = Regex("\"countryCode\"\\s*:\\s*\"([A-Z]{2})\"")
        val GEMINI_REGION_REGEX = Regex(",2,1,200,\"([A-Z]{3})\"")
        val GEMINI_REGION_ALT_REGEX = Regex("\\[1,null,null,\\d+,\\d+,\"([A-Z]{3})\"")
        val REDDIT_POP_REGEX = Regex("p=([A-Z]{3})")
        val TIKTOK_VIA_REGEX = Regex("oversea-([A-Z]{2})-", RegexOption.IGNORE_CASE)

        /**
         * The viewer's region, carried as `countryCode=XX` inside a `Set-Cookie` header. The
         * key is anchored so the match cannot land on the leading letters of `countryCode`.
         */
        val HBO_COUNTRY_COOKIE = Regex("countryCode=([A-Za-z]{2})")

        /** Regions the page offers, listed as `"url":"/xx/xx"` paths in the body. */
        val HBO_COUNTRY_URL = Regex("\"url\":\"/([a-z]{2})/[a-z]{2}\"")

        val GEMINI_UNSUPPORTED = setOf(
            "CN", "CHN", "MO", "MAC", "RU", "RUS", "BY", "BLR", "IR", "IRN",
            "KP", "PRK", "SY", "SYR", "CU", "CUB", "VE", "VEN", "MM", "MMR",
            "SD", "SDN", "AF", "AFG", "SS", "SSD", "YE", "YEM", "ZW", "ZWE"
        )
    }
}
