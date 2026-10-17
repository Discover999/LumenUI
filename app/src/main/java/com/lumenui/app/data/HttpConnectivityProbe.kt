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

    /**
     * PING samples must read the same first hop as [checkPing]; following the chain would time
     * the site's OAuth callback (Battle.net) and mark every sample failed beside a 可用 verdict.
     */
    private val sampleNoFollowClient: OkHttpClient = sampleClient.newBuilder()
        .followRedirects(false)
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
        // PING is graded on the first hop, so its samples must be too; every other kind's verdict
        // reads the followed response, so their samples keep following.
        val client = if (target.kind == ProbeKind.PING) sampleNoFollowClient else sampleClient
        val gate = Semaphore(SAMPLE_CONCURRENCY)
        val deferred = (0 until count).map {
            async(Dispatchers.IO) {
                gate.withPermit {
                    val start = System.currentTimeMillis()
                    val ok = runCatching {
                        client.newCall(request).execute().use { res -> res.code in 200..399 }
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
        ProbeKind.OPENAI -> OPENAI_TRACE_URL
        ProbeKind.SIMPLE -> target.url.ifEmpty { "https://${target.domain}/" }
        ProbeKind.NETFLIX -> "https://www.netflix.com/title/$NETFLIX_LICENSED_TITLE"
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
        ProbeKind.OPENAI -> checkOpenAi(target, start)
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

    /**
     * Cloudflare trace probe: reads the egress region off the edge and compares it against the
     * target's unsupported-region set.
     *
     * Most targets carry no such set — there is no established region criterion for them — so for
     * those the verdict is reachability and nothing more, the same claim [checkPing] makes, with
     * the trace fetched for the region and address the card shows. Only the targets that do carry
     * a set ([OI] used to, Claude does) get a region verdict out of this routine.
     */
    private fun checkTrace(target: ProbeTarget, start: Long): ProbeResult {
        var anyResponse = false
        var flagged = false
        for (domain in listOf(target.domain) + target.fallbackDomains) {
            val response = get(followClient, "https://$domain/cdn-cgi/trace") ?: continue
            response.use { res ->
                anyResponse = true
                val body = res.body.string()
                val trace = parseTrace(res.code, body, res.header("cf-ray"))
                val region = trace.loc?.uppercase()?.let(RegionCodes::normalize)
                if (!trace.ip.isNullOrEmpty()) {
                    val blocked = region != null && target.unsupportedRegions.contains(region)
                    return target.done(
                        start,
                        if (blocked) ProbeStatus.BLOCKED else ProbeStatus.UNLOCKED,
                        region, trace.colo?.uppercase(), trace.ip, trace.warp
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

    /**
     * ChatGPT availability, following RegionRestrictionCheck: [OI] exposes two endpoints that
     * separate "this country is unsupported" from "this looks like a VPN", so the verdict comes
     * from the platform itself rather than from a region list that goes stale.
     *
     * Neither endpoint names the egress region, so a Cloudflare trace is fetched as well for the
     * region and address the card shows; the trace failing leaves the verdict untouched.
     */
    private fun checkOpenAi(target: ProbeTarget, start: Long): ProbeResult {
        var region: String? = null
        var colo: String? = null
        var ip: String? = null
        var warp = false
        get(followClient, OPENAI_TRACE_URL)?.use { res ->
            val trace = parseTrace(res.code, res.body.string(), res.header("cf-ray"))
            region = trace.loc?.uppercase()?.let(RegionCodes::normalize)
            colo = trace.colo?.uppercase()
            ip = trace.ip
            warp = trace.warp
        }
        val unsupported = get(followClient, OPENAI_COMPLIANCE_URL, OPENAI_COMPLIANCE_HEADERS)?.use { res ->
            readBody(res, OPENAI_MARKER_BYTES).contains("unsupported_country", ignoreCase = true)
        } ?: return target.done(start, ProbeStatus.FAILED)
        val vpn = get(followClient, OPENAI_IOS_URL, OPENAI_IOS_HEADERS)?.use { res ->
            readBody(res, OPENAI_MARKER_BYTES).contains("VPN", ignoreCase = true)
        } ?: return target.done(start, ProbeStatus.FAILED)
        val status = openAiStatus(unsupported, vpn)
        return target.done(start, status, region, colo, ip, warp)
    }

    /**
     * Netflix region-unlock check, following sjlleo/netflix-verify's model: a title every region
     * that carries Netflix can stream ([NETFLIX_AREA_TITLE]) says whether the service exists
     * where the egress sits, and a licensed (non-original) title ([NETFLIX_LICENSED_TITLE]) says
     * whether the catalogue reaches past Netflix's own productions.
     *
     * Redirects are not followed: an available title answers `301` with
     * `Location: https://www.netflix.com/<region>-<lang>/title/<id>`, and requiring that Location
     * to name the same title keeps a redirect to `/browse` or `/unsupportedbrowser` from reading
     * as availability. Region comes out of the same Location, already stripped of its language
     * suffix by [NETFLIX_REGION].
     */
    private fun checkNetflix(target: ProbeTarget, start: Long): ProbeResult {
        val area = netflixTitle(NETFLIX_AREA_TITLE)
        val licensed = netflixTitle(NETFLIX_LICENSED_TITLE)
        // The licensed title names the region too, but it may be the one that did not play, so
        // either response is allowed to supply it.
        val region = licensed?.region ?: area?.region
        return target.done(start, netflixStatus(area, licensed), region)
    }

    /** What one title page said: its HTTP status, whether it plays, and the region it named. */
    internal data class NetflixTitle(val code: Int, val playable: Boolean, val region: String?)

    /**
     * The verdict from the two title answers. Kept pure and covered by tests on purpose: this
     * grading has been inverted once, and a wrong verdict here still looks perfectly plausible on
     * the device, so nothing but a test catches it.
     */
    internal fun netflixStatus(area: NetflixTitle?, licensed: NetflixTitle?): ProbeStatus = when {
        // 403 is Netflix refusing the egress address outright, self-test title included.
        area?.code == 403 || licensed?.code == 403 -> ProbeStatus.BLOCKED
        // Licensed content plays, so the catalogue reaches past Netflix's own productions.
        licensed?.playable == true -> ProbeStatus.UNLOCKED
        // Only the self-test title plays: the region carries Netflix, but originals are all it
        // will serve this address.
        area?.playable == true -> ProbeStatus.LIMITED
        // Nothing answered at all. Reported as a failed probe, not as a region verdict.
        area == null && licensed == null -> ProbeStatus.FAILED
        // Both answered and neither title played.
        else -> ProbeStatus.BLOCKED
    }

    /**
     * Reads one title's answer. The page plays where the status is a 3xx whose `Location` still
     * names that same title; anywhere else — a `404`, or a redirect to `/browse` or
     * `/unsupportedbrowser` — means it is not available from this address. The region comes out of
     * the same Location, which is what [NETFLIX_REGION] strips the language suffix from.
     */
    internal fun netflixPage(id: String, code: Int, location: String?): NetflixTitle {
        val location = location.orEmpty()
        return NetflixTitle(
            code = code,
            playable = code in 300..399 && location.contains("/title/$id"),
            region = NETFLIX_REGION.find(location)?.groupValues?.get(1)?.uppercase()
        )
    }

    /** Requests one Netflix title without following redirects, or null when nothing came back. */
    private fun netflixTitle(id: String): NetflixTitle? {
        val response = get(noFollowClient, "https://www.netflix.com/title/$id") ?: return null
        return response.use { res -> netflixPage(id, res.code, res.header("Location")) }
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
            if (isGoogleAbuseLocation(res.request.url.toString())) {
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
        // An address Google distrusts is answered with a redirect to its abuse interstitial
        // (`www.google.com/sorry/index`), which sends the client straight back to Gemini, so a
        // redirect-following request never terminates. Inspect the first hop instead.
        get(noFollowClient, "https://gemini.google.com/")?.use { res ->
            if (isGoogleAbuseLocation(res.header("Location"))) {
                return target.done(start, ProbeStatus.BLOCKED)
            }
        }
        val response = get(followClient, "https://gemini.google.com/")
            ?: return target.done(start, ProbeStatus.FAILED)
        response.use { res ->
            val finalUrl = res.request.url.toString()
            if (isGoogleAbuseLocation(finalUrl)) {
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
            val region = redditRegion(res.header("server-timing"))
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
     *
     * Redirects are not followed: the site's own 3xx already proves the host answered, while the
     * chain can end somewhere that is not the site at all. Battle.net is the case that exposed
     * this — `www.battle.net` 301s to `shop.battle.net`, which bounces to an OAuth callback that
     * answers 401 to a cookie-less client, and following it made a reachable host read 不可用.
     */
    private fun checkPing(target: ProbeTarget, start: Long): ProbeResult {
        val url = sampleUrl(target) ?: return target.done(start, ProbeStatus.FAILED)
        val response = get(noFollowClient, url) ?: return target.done(start, ProbeStatus.FAILED)
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
            val region = hboRegion(res.headers("Set-Cookie"))
            if (region == null) {
                // Reachable but the edge did not name a region, so availability cannot be judged.
                // Report the failure without a region rather than inventing one.
                return target.done(start, ProbeStatus.BLOCKED)
            }
            val body = runCatching { res.body.string() }.getOrDefault("")
            val status = if (region in hboOfferedRegions(body)) ProbeStatus.UNLOCKED else ProbeStatus.BLOCKED
            // The region is reported either way, so a blocked card still shows where it was seen.
            return target.done(start, status, region)
        }
    }

    // --- Helpers ------------------------------------------------------------

    private fun get(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Response? = runCatching {
        val builder = request(url).get()
        headers.forEach { (name, value) -> builder.header(name, value) }
        client.newCall(builder.build()).execute()
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

    /** The fields a Cloudflare trace body carries that the cards show. */
    internal data class TraceInfo(val ip: String?, val loc: String?, val colo: String?, val warp: Boolean)

    // --- Platform parsers, kept pure so the shapes that were once mis-read stay covered --------

    /** Reddit names the POP it answered from in `server-timing`, e.g. `p=HKG`. */
    internal fun redditRegion(serverTiming: String?): String? =
        REDDIT_POP_REGEX.find(serverTiming.orEmpty())
            ?.groupValues?.get(1)
            ?.let(RegionCodes::normalize)

    /**
     * The region the HBO edge reports. It arrives as a `Set-Cookie: countryCode=XX` — there is no
     * plain `countryCode` header and it is not in the body — so every cookie has to be scanned.
     */
    internal fun hboRegion(setCookies: List<String>): String? =
        setCookies.firstNotNullOfOrNull { HBO_COUNTRY_COOKIE.find(it)?.groupValues?.get(1) }
            ?.uppercase()

    /**
     * The regions the HBO page itself offers, which the reported region is compared against. The
     * site always offers the US, so it counts even when the page does not link it.
     */
    internal fun hboOfferedRegions(body: String): Set<String> =
        HBO_COUNTRY_URL.findAll(body).map { it.groupValues[1].uppercase() }.toSet() + "US"

    /**
     * Parses a `/cdn-cgi/trace` body. A response that is not 2xx/3xx, or that carries an HTML
     * page instead of the key=value body, yields an all-null result — which callers read as
     * "this tracer does not apply to this domain" rather than as a verdict.
     */
    internal fun parseTrace(code: Int, body: String, cfRay: String?): TraceInfo {
        var ip: String? = null
        var loc: String? = null
        var colo: String? = null
        var warp = false
        if (code in 200..399 && body.isNotEmpty() && !body.contains("<!DOCTYPE")) {
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
        return TraceInfo(ip, loc, colo ?: coloFromRay(cfRay), warp)
    }

    /**
     * Google's anti-abuse interstitial, which it serves in place of the page that was asked for
     * once it decides the request's address looks like a proxy or a bot. It moved from
     * `sorry.google.com` to `www.google.com/sorry/index`, so both spellings have to be matched.
     */
    internal fun isGoogleAbuseLocation(location: String?): Boolean =
        location != null &&
            (location.contains("google.com/sorry") || location.contains("sorry.google.com"))

    /**
     * The two endpoints [OI] exposes disagree in a way that names a partial verdict: when only
     * one of them objects, the platform is reachable from one surface but not the other.
     */
    internal fun openAiStatus(unsupportedCountry: Boolean, vpnDetected: Boolean): ProbeStatus = when {
        unsupportedCountry && vpnDetected -> ProbeStatus.BLOCKED
        unsupportedCountry || vpnDetected -> ProbeStatus.LIMITED
        else -> ProbeStatus.UNLOCKED
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

        /**
         * Netflix's own self-test title ("Test Patterns"): it plays in every region that carries
         * the service, so it answers "does Netflix exist where this egress sits" on its own.
         */
        const val NETFLIX_AREA_TITLE = "80018499"

        /** A licensed title ("Breaking Bad"): it plays only where the catalogue is not originals-only. */
        const val NETFLIX_LICENSED_TITLE = "70143836"

        /** Captures the region out of `Location: .../<region>-<lang>/title/<id>`, dropping the language. */
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

        /** Trace used for [OI]'s region and egress address; its verdict comes from the endpoints. */
        const val OPENAI_TRACE_URL = "https://chatgpt.com/cdn-cgi/trace"

        /** Reports an unsupported country in the body, as `unsupported_country_region_territory`. */
        const val OPENAI_COMPLIANCE_URL = "https://api.openai.com/compliance/cookie_requirements"

        /** Reports a detected VPN in the body; the two endpoints disagree on purpose. */
        const val OPENAI_IOS_URL = "https://ios.chat.openai.com/"

        /** Enough of a marker response to hold its verdict keyword. */
        const val OPENAI_MARKER_BYTES = 8 * 1024

        /**
         * The compliance endpoint answers differently without these, so they are part of the
         * probe rather than incidental: it expects a platform.openai.com browser context.
         */
        val OPENAI_COMPLIANCE_HEADERS = mapOf(
            "accept" to "*/*",
            "authorization" to "Bearer null",
            "content-type" to "application/json",
            "origin" to "https://platform.openai.com",
            "referer" to "https://platform.openai.com/",
            "sec-fetch-dest" to "empty",
            "sec-fetch-mode" to "cors",
            "sec-fetch-site" to "same-site"
        )

        val OPENAI_IOS_HEADERS = mapOf(
            "accept" to "*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "sec-fetch-dest" to "document",
            "sec-fetch-mode" to "navigate",
            "sec-fetch-site" to "none",
            "sec-fetch-user" to "?1",
            "upgrade-insecure-requests" to "1"
        )

        val GEMINI_UNSUPPORTED = setOf(
            "CN", "CHN", "MO", "MAC", "RU", "RUS", "BY", "BLR", "IR", "IRN",
            "KP", "PRK", "SY", "SYR", "CU", "CUB", "VE", "VEN", "MM", "MMR",
            "SD", "SDN", "AF", "AFG", "SS", "SSD", "YE", "YEM", "ZW", "ZWE"
        )
    }
}
