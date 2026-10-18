package com.lumenui.app.data

import java.util.Locale
import kotlin.math.abs

/** Where a speed test is in its run. */
enum class SpeedPhase(val running: Boolean) {
    IDLE(false),
    LATENCY(true),
    DOWNLOAD(true),
    UPLOAD(true),
    DONE(false),
    FAILED(false)
}

/** One instantaneous throughput reading, taken from the bytes moved since the previous sample. */
data class SpeedSample(val elapsedMs: Long, val mbps: Double)

/** Bytes actually moved in a phase and how long that took. */
data class TransferStats(val bytes: Long, val elapsedMs: Long) {
    val mbps: Double get() = speedMbps(bytes, elapsedMs)
}

/** Where the test left the device, read from Cloudflare's trace. */
data class SpeedEgress(val ip: String, val region: String? = null, val colo: String? = null)

/** The qualitative band shown beside a figure. */
enum class SpeedGrade { EXCELLENT, GOOD, FAIR, POOR }

/** Which node pair a run used: Cloudflare's speed endpoints, or the OVH fallback. */
enum class SpeedProvider { CLOUDFLARE, OVH }

/**
 * Everything the speed page renders. A finished run stays here until the next start, so leaving
 * and re-opening the page shows the last result instead of an empty idle screen.
 */
data class SpeedTestState(
    val phase: SpeedPhase = SpeedPhase.IDLE,
    val latencyMs: Long? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val downloadBytes: Long = 0,
    val downloadMs: Long = 0,
    val uploadBytes: Long = 0,
    val uploadMs: Long = 0,
    val downloadSamples: List<SpeedSample> = emptyList(),
    val uploadSamples: List<SpeedSample> = emptyList(),
    val liveMbps: Double? = null,
    val provider: SpeedProvider = SpeedProvider.CLOUDFLARE,
    val uploadUnavailable: Boolean = false,
    val egress: SpeedEgress? = null,
    val reputation: IpReputation? = null,
    val error: String? = null
)

/**
 * Measures through the current egress. Implementations report each throughput sample as it lands
 * and return what they actually moved; nothing here estimates a value that was not observed.
 */
interface SpeedTester {
    /** Set once the latency probe has picked a working node pair. */
    val provider: SpeedProvider

    suspend fun latencySamples(count: Int = LATENCY_SAMPLES): List<Long>

    /** One latency probe while the transfer phases run; null when the host did not answer. */
    suspend fun ping(): Long?

    suspend fun download(
        durationMs: Long,
        onSample: (SpeedSample) -> Unit
    ): TransferStats

    /** Null when the current egress has no usable upload node; the run is still a result. */
    suspend fun upload(
        durationMs: Long,
        onSample: (SpeedSample) -> Unit
    ): TransferStats?

    suspend fun egress(): SpeedEgress?
}

const val LATENCY_SAMPLES = 5
const val LATENCY_PING_INTERVAL_MS = 1_000L

/** Both directions run the same wall-clock window; there is no byte cap. */
const val SPEED_PHASE_MS = 8_000L
const val SPEED_SAMPLE_INTERVAL_MS = 250L

/** Real bytes over real elapsed time; Mbps, not MiB/s. */
fun speedMbps(bytes: Long, elapsedMs: Long): Double =
    if (elapsedMs <= 0L) 0.0 else bytes * 8.0 / (elapsedMs * 1000.0)

/** Mean absolute difference between consecutive latency samples, or null with fewer than two. */
fun jitterMs(samples: List<Long>): Double? {
    val valid = samples.filter { it >= 0L }
    if (valid.size < 2) return null
    var total = 0.0
    for (index in 1 until valid.size) total += abs(valid[index] - valid[index - 1])
    return total / (valid.size - 1)
}

/** Bands chosen to match the reference result page, which rates 0.9 Mbps as 一般. */
fun speedGrade(mbps: Double?): SpeedGrade? = when {
    mbps == null -> null
    mbps >= 50.0 -> SpeedGrade.EXCELLENT
    mbps >= 10.0 -> SpeedGrade.GOOD
    mbps >= 0.5 -> SpeedGrade.FAIR
    else -> SpeedGrade.POOR
}

fun latencyGrade(ms: Long?): SpeedGrade? = when {
    ms == null -> null
    ms <= 30L -> SpeedGrade.EXCELLENT
    ms <= 80L -> SpeedGrade.GOOD
    ms <= 150L -> SpeedGrade.FAIR
    else -> SpeedGrade.POOR
}

fun jitterGrade(ms: Double?): SpeedGrade? = when {
    ms == null -> null
    ms <= 10.0 -> SpeedGrade.EXCELLENT
    ms <= 30.0 -> SpeedGrade.GOOD
    ms <= 100.0 -> SpeedGrade.FAIR
    else -> SpeedGrade.POOR
}

fun formatMbps(mbps: Double?): String =
    mbps?.let { String.format(Locale.US, "%.1f", it) } ?: "—"

/** Decimal megabytes per second: 1 MB/s = 8 Mbps, the same decimal units the page uses. */
fun formatMBps(mbps: Double?): String =
    mbps?.let { String.format(Locale.US, "%.1f", it / 8.0) } ?: "—"

fun formatJitter(ms: Double?): String =
    ms?.let { String.format(Locale.US, "%.1f", it) } ?: "—"

/** Decimal units, matching the result page's estimates: 1 KB = 1000 B, 1 MB = 1000 KB. */
fun formatBytes(bytes: Long): String = when {
    bytes < 1_000L -> "$bytes B"
    bytes < 1_000_000L -> String.format(Locale.US, "%.1f KB", bytes / 1_000.0)
    bytes < 1_000_000_000L -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0)
    else -> String.format(Locale.US, "%.2f GB", bytes / 1_000_000_000.0)
}

/**
 * How long a transfer would take at [mbps]. Decimal units, as the result page's footnote says:
 * 1 GB = 1000 MB.
 */
fun transferTime(sizeMb: Long, mbps: Double?): String? {
    if (mbps == null || mbps <= 0.0) return null
    val seconds = sizeMb * 8.0 / mbps
    return when {
        seconds < 60.0 -> String.format(Locale.US, "%.1f 秒钟", seconds)
        seconds < 3600.0 -> String.format(Locale.US, "%.1f 分钟", seconds / 60.0)
        else -> String.format(Locale.US, "%.1f 小时", seconds / 3600.0)
    }
}
