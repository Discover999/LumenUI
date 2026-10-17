package com.lumenui.app.ui

import com.lumenui.app.data.IpReputationApi
import com.lumenui.app.data.LATENCY_PING_INTERVAL_MS
import com.lumenui.app.data.SPEED_PHASE_MS
import com.lumenui.app.data.SpeedHistoryEntry
import com.lumenui.app.data.SpeedPhase
import com.lumenui.app.data.SpeedTestState
import com.lumenui.app.data.SpeedTester
import com.lumenui.app.data.TransferStats
import com.lumenui.app.data.jitterMs
import com.lumenui.app.util.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private const val MAX_SAMPLES = 96

/**
 * Runs one speed test at a time: latency, then download, then upload, then the egress lookup.
 * The finished state is kept, so re-opening the page shows the last result; only [cancel] — which
 * happens when the page is left mid-run — drops it.
 */
class SpeedTestRunner(
    private val tester: SpeedTester,
    private val reputation: IpReputationApi,
    private val scope: CoroutineScope,
    private val onFinished: (SpeedHistoryEntry) -> Unit = {}
) {
    private val _state = MutableStateFlow(SpeedTestState())
    val state: StateFlow<SpeedTestState> = _state.asStateFlow()

    private var job: Job? = null

    fun start() {
        if (_state.value.phase.running) return
        job?.cancel()
        job = scope.launch {
            _state.value = SpeedTestState(phase = SpeedPhase.LATENCY)
            try {
                val latencies = tester.latencySamples()
                if (latencies.isEmpty()) error("延迟测试没有响应")
                val pings = latencies.toMutableList()
                _state.update {
                    it.copy(
                        latencyMs = pings.average().roundToLong(),
                        jitterMs = jitterMs(pings),
                        provider = tester.provider
                    )
                }

                _state.update { it.copy(phase = SpeedPhase.DOWNLOAD) }
                val pinger = launch {
                    while (isActive) {
                        delay(LATENCY_PING_INTERVAL_MS)
                        val sample = try {
                            tester.ping()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        } ?: continue
                        pings += sample
                        _state.update {
                            it.copy(
                                latencyMs = sample,
                                jitterMs = jitterMs(pings)
                            )
                        }
                    }
                }

                val download: TransferStats
                val upload: TransferStats?
                try {
                    download = tester.download(SPEED_PHASE_MS) { sample ->
                        _state.update { state ->
                            state.copy(
                                liveMbps = sample.mbps,
                                downloadSamples = (state.downloadSamples + sample).takeLast(MAX_SAMPLES)
                            )
                        }
                    }
                    _state.update {
                        it.copy(
                            downloadMbps = download.mbps,
                            downloadBytes = download.bytes,
                            downloadMs = download.elapsedMs,
                            liveMbps = null,
                            phase = SpeedPhase.UPLOAD
                        )
                    }
                    upload = tester.upload(SPEED_PHASE_MS) { sample ->
                        _state.update { state ->
                            state.copy(
                                liveMbps = sample.mbps,
                                uploadSamples = (state.uploadSamples + sample).takeLast(MAX_SAMPLES)
                            )
                        }
                    }
                } finally {
                    pinger.cancel()
                    pinger.join()
                }

                val averageLatency = pings.average().roundToLong()
                val finalJitter = jitterMs(pings)
                val egress = tester.egress()
                val lookup = egress?.ip?.let { reputation.lookup(it) }
                val finished = _state.value.copy(
                    latencyMs = averageLatency,
                    jitterMs = finalJitter,
                    uploadMbps = upload?.mbps,
                    uploadBytes = upload?.bytes ?: 0,
                    uploadMs = upload?.elapsedMs ?: 0,
                    uploadUnavailable = upload == null,
                    liveMbps = null,
                    phase = SpeedPhase.DONE,
                    provider = tester.provider,
                    egress = egress,
                    reputation = lookup
                )
                _state.value = finished
                runCatching {
                    onFinished(
                        SpeedHistoryEntry(
                            timestamp = System.currentTimeMillis(),
                            downloadMbps = finished.downloadMbps,
                            uploadMbps = finished.uploadMbps,
                            latencyMs = finished.latencyMs,
                            jitterMs = finished.jitterMs,
                            provider = finished.provider.name,
                            uploadUnavailable = finished.uploadUnavailable,
                            egressIp = finished.egress?.ip,
                            egressRegion = finished.egress?.region,
                            egressColo = finished.egress?.colo
                        )
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AppLog.w("Speed test failed", error)
                _state.value = _state.value.copy(
                    phase = SpeedPhase.FAILED,
                    error = error.message ?: "测速失败"
                )
            }
        }
    }

    fun cancel() {
        reset()
    }

    /**
     * Leaves the page on its start screen. A finished run is already written to history, so
     * reopening the page must not replay the previous result; a run still in flight is cancelled.
     */
    fun reset() {
        job?.cancel()
        job = null
        _state.value = SpeedTestState()
    }

    fun cancelIfRunning() {
        if (_state.value.phase.running) reset()
    }
}
