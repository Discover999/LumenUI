package com.lumenui.app.data

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

/** Probes connectivity targets; implementations report each latency sample as it lands. */
interface ConnectivityProbe {
    suspend fun probe(target: ProbeTarget, onSample: ((Long) -> Unit)? = null): ProbeResult
}
