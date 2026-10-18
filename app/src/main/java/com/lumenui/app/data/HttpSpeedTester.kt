package com.lumenui.app.data

import com.lumenui.app.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

private const val BASE_URL = "https://speed.cloudflare.com"
private const val OVH_DOWNLOAD_URL = "https://proof.ovh.net/files/10Mb.dat"

/** Last-resort backend: LibreSpeed's public Los Angeles server, the one that answered in tests. */
private const val LIBRESPEED_BASE_URL = "https://la.speedtest.clouvider.net/backend"
private const val LIBRESPEED_DOWNLOAD_MB = 25
private const val STREAMS = 4
private const val STREAM_BYTES = 25_000_000
private const val READ_CHUNK_BYTES = 64 * 1024
private const val UPLOAD_CHUNK_BYTES = 256 * 1024

/**
 * Real-bytes throughput against Cloudflare's speed endpoints. Downloads run four parallel
 * streams until the phase deadline or byte cap, uploads four parallel POSTs, and a ticker turns
 * the byte counter into the live samples the chart draws. Nothing is extrapolated: the final
 * figure is the bytes that actually moved over the time they took.
 */
class HttpSpeedTester(base: OkHttpClient) : SpeedTester {
    private val client = base.newBuilder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private var activeProvider = SpeedProvider.CLOUDFLARE
    override val provider: SpeedProvider get() = activeProvider
    override var uploadFallback = false
        private set

    /**
     * Cloudflare's speed host is the default, but some proxy listeners cannot complete TLS to it
     * (the phone's 7893 listener, as of 2026-10-17). When all five attempts fail, fall back to
     * OVH's public test file, which that listener does reach; its upload is unavailable. If OVH
     * also fails, try LibreSpeed's public backend, which carries both directions and the egress
     * lookup. One provider serves the whole run, so the result's label stays truthful.
     */
    override suspend fun latencySamples(count: Int): List<Long> = withContext(Dispatchers.IO) {
        val cloudflare = measureLatency("$BASE_URL/__down?bytes=0", count, asHead = false)
        if (cloudflare.isNotEmpty()) {
            activeProvider = SpeedProvider.CLOUDFLARE
            return@withContext cloudflare
        }
        val libreSpeed = measureLatency("$LIBRESPEED_BASE_URL/empty.php?r=$count", count, asHead = false)
        if (libreSpeed.isNotEmpty()) {
            activeProvider = SpeedProvider.LIBRESPEED
            return@withContext libreSpeed
        }
        activeProvider = SpeedProvider.OVH
        measureLatency(OVH_DOWNLOAD_URL, count, asHead = true)
    }

    private fun measureLatency(url: String, count: Int, asHead: Boolean): List<Long> =
        (0 until count).mapNotNull { pingUrl(url, asHead) }

    private fun pingUrl(url: String, asHead: Boolean): Long? = runCatching {
        val started = System.nanoTime()
        val ok = client.newCall(request(url, asHead)).execute().use { it.isSuccessful }
        if (!ok) return@runCatching null
        (System.nanoTime() - started) / 1_000_000
    }.getOrNull()

    override suspend fun ping(): Long? = withContext(Dispatchers.IO) {
        when (activeProvider) {
            SpeedProvider.CLOUDFLARE -> pingUrl("$BASE_URL/__down?bytes=0", asHead = false)
            SpeedProvider.OVH -> pingUrl(OVH_DOWNLOAD_URL, asHead = true)
            SpeedProvider.LIBRESPEED ->
                pingUrl("$LIBRESPEED_BASE_URL/empty.php?r=${System.nanoTime()}", asHead = false)
        }
    }

    override suspend fun download(
        durationMs: Long,
        onSample: (SpeedSample) -> Unit
    ): TransferStats = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        val total = AtomicLong()
        val stop = AtomicBoolean(false)
        val calls = ConcurrentLinkedQueue<Call>()
        val streamUrl = when (activeProvider) {
            SpeedProvider.OVH -> OVH_DOWNLOAD_URL
            SpeedProvider.LIBRESPEED ->
                "$LIBRESPEED_BASE_URL/garbage.php?ckSize=$LIBRESPEED_DOWNLOAD_MB"
            SpeedProvider.CLOUDFLARE -> "$BASE_URL/__down?bytes=$STREAM_BYTES"
        }
        coroutineScope {
            val ticker = ticker(started, durationMs, total, stop, calls, onSample)
            val streams = (0 until STREAMS).map {
                launch {
                    while (!stop.get() && System.currentTimeMillis() - started < durationMs) {
                        val call = client.newCall(request(streamUrl))
                        calls += call
                        try {
                            call.execute().use { response ->
                                if (!response.isSuccessful) return@use
                                val source = response.body.source()
                                val buffer = Buffer()
                                while (!stop.get()) {
                                    val read = source.read(buffer, READ_CHUNK_BYTES.toLong())
                                    if (read == -1L) break
                                    buffer.clear()
                                    total.addAndGet(read)
                                    if (System.currentTimeMillis() - started >= durationMs) {
                                        stop.set(true)
                                        break
                                    }
                                }
                            }
                        } catch (error: IOException) {
                            if (!stop.get()) AppLog.w("Speed download stream failed", error)
                        } finally {
                            calls.remove(call)
                        }
                    }
                }
            }
            streams.joinAll()
            stop.set(true)
            calls.forEach { it.cancel() }
            ticker.cancel()
        }
        if (total.get() == 0L) error("下载测试没有收到数据")
        TransferStats(total.get(), max(1L, System.currentTimeMillis() - started))
    }

    override suspend fun upload(
        durationMs: Long,
        onSample: (SpeedSample) -> Unit
    ): TransferStats? = withContext(Dispatchers.IO) {
        uploadFallback = false
        // The OVH fallback has no upload receiver; reporting it unavailable is honest, while a
        // fabricated or HTTPBin-limited number would not be.
        if (activeProvider == SpeedProvider.OVH) return@withContext null
        runUpload(activeProvider, durationMs, onSample)
    }

    /**
     * Retries the upload against LibreSpeed when Cloudflare's upload endpoint is blocked but its
     * download worked. The caller clears the failed attempt's samples before this runs.
     */
    override suspend fun uploadBackup(
        durationMs: Long,
        onSample: (SpeedSample) -> Unit
    ): TransferStats? = withContext(Dispatchers.IO) {
        if (activeProvider != SpeedProvider.CLOUDFLARE) return@withContext null
        val backup = runUpload(SpeedProvider.LIBRESPEED, durationMs, onSample)
        uploadFallback = backup != null
        backup
    }

    private suspend fun runUpload(
        provider: SpeedProvider,
        durationMs: Long,
        onSample: (SpeedSample) -> Unit
    ): TransferStats? {
        val uploadUrl = if (provider == SpeedProvider.LIBRESPEED) {
            "$LIBRESPEED_BASE_URL/empty.php"
        } else {
            "$BASE_URL/__up"
        }
        val started = System.currentTimeMillis()
        val total = AtomicLong()
        val stop = AtomicBoolean(false)
        val calls = ConcurrentLinkedQueue<Call>()
        val payload = ByteArray(UPLOAD_CHUNK_BYTES)
        val body = payload.toRequestBody("application/octet-stream".toMediaType())
        coroutineScope {
            val ticker = ticker(started, durationMs, total, stop, calls, onSample)
            val streams = (0 until STREAMS).map {
                launch {
                    while (!stop.get() && System.currentTimeMillis() - started < durationMs) {
                        val call = client.newCall(
                            Request.Builder()
                                .url(uploadUrl)
                                .post(body)
                                .header("User-Agent", PROBE_USER_AGENT)
                                .build()
                        )
                        calls += call
                        try {
                            call.execute().use { response ->
                                if (response.isSuccessful) total.addAndGet(payload.size.toLong())
                            }
                        } catch (error: IOException) {
                            if (!stop.get()) AppLog.w("Speed upload stream failed", error)
                        } finally {
                            calls.remove(call)
                        }
                    }
                }
            }
            streams.joinAll()
            stop.set(true)
            calls.forEach { it.cancel() }
            ticker.cancel()
        }
        return if (total.get() == 0L) {
            null
        } else {
            TransferStats(total.get(), max(1L, System.currentTimeMillis() - started))
        }
    }

    override suspend fun egress(): SpeedEgress? = withContext(Dispatchers.IO) {
        if (activeProvider == SpeedProvider.LIBRESPEED) {
            return@withContext fetchLibreSpeedEgress()
        }
        // The speed host's trace can be unreachable on the same listener that blocks its speed
        // endpoints; www.cloudflare.com reports the same egress and is reachable there.
        fetchEgress("$BASE_URL/cdn-cgi/trace")
            ?: fetchEgress("https://www.cloudflare.com/cdn-cgi/trace")
    }

    private fun fetchLibreSpeedEgress(): SpeedEgress? = runCatching {
        client.newCall(request("$LIBRESPEED_BASE_URL/getIP.php")).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val ip = Regex("\"processedString\"\\s*:\\s*\"([^\"]+)\"")
                .find(response.body.string())
                ?.groupValues
                ?.getOrNull(1)
                ?: return@use null
            SpeedEgress(ip)
        }
    }.getOrNull()

    private fun fetchEgress(url: String): SpeedEgress? = runCatching {
        client.newCall(request(url)).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val fields = response.body.string()
                .lineSequence()
                .mapNotNull { line ->
                    val index = line.indexOf('=')
                    if (index <= 0) null else line.substring(0, index) to line.substring(index + 1)
                }
                .toMap()
            val ip = fields["ip"] ?: return@use null
            SpeedEgress(ip, fields["loc"], fields["colo"])
        }
    }.getOrNull()

    /** Emits the interval throughput and ends the phase at its deadline by cancelling calls. */
    private fun CoroutineScope.ticker(
        startedAt: Long,
        durationMs: Long,
        total: AtomicLong,
        stop: AtomicBoolean,
        calls: ConcurrentLinkedQueue<Call>,
        onSample: (SpeedSample) -> Unit
    ) = launch {
        var lastBytes = 0L
        var lastAt = startedAt
        while (isActive && !stop.get()) {
            delay(SPEED_SAMPLE_INTERVAL_MS)
            val now = System.currentTimeMillis()
            val bytes = total.get()
            val interval = now - lastAt
            if (interval > 0) {
                onSample(
                    SpeedSample(
                        elapsedMs = now - startedAt,
                        mbps = (bytes - lastBytes) * 8.0 / (interval * 1000.0)
                    )
                )
            }
            lastBytes = bytes
            lastAt = now
            if (now - startedAt >= durationMs) {
                stop.set(true)
                calls.forEach { it.cancel() }
                break
            }
        }
    }

    private fun request(url: String, asHead: Boolean = false) = Request.Builder()
        .url(url)
        .apply { if (asHead) head() }
        .header("User-Agent", PROBE_USER_AGENT)
        .build()
}
