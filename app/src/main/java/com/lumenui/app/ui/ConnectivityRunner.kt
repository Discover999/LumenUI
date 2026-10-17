package com.lumenui.app.ui

import com.lumenui.app.data.ConnectivityProbe
import com.lumenui.app.data.IpReputation
import com.lumenui.app.data.IpReputationApi
import com.lumenui.app.data.ProbeResult
import com.lumenui.app.data.ProbeTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** State of the connectivity page: the in-flight run plus everything it has produced so far. */
data class ConnectivityState(
    val running: Boolean = false,
    val results: Map<String, ProbeResult> = emptyMap(),
    val samples: Map<String, List<Long>> = emptyMap(),
    val reputation: Map<String, IpReputation> = emptyMap(),
    val retrying: Set<String> = emptySet(),
    /** Categories whose whole group is being re-probed, for the section header's own state. */
    val retryingGroups: Set<String> = emptySet()
)

/** How many connectivity probes run at once. */
private const val PROBE_CONCURRENCY = 8

/** How long a finished run's results stay before being discarded. */
internal const val CONNECTIVITY_RESULT_TTL_MS = 2 * 60 * 1000L

/**
 * Runs connectivity probes and accumulates their output into [state].
 *
 * Depends only on the [ConnectivityProbe] / [IpReputationApi] interfaces (no Android, no concrete
 * HTTP clients) so the orchestration can be driven with fakes in tests. A finished run's results
 * are discarded after [CONNECTIVITY_RESULT_TTL_MS], so a stale set never lingers — except while
 * [setResultsHeld] holds them, which a page showing them does for as long as it is open.
 */
class ConnectivityRunner(
    private val probe: ConnectivityProbe,
    private val reputation: IpReputationApi,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(ConnectivityState())
    val state: StateFlow<ConnectivityState> = _state.asStateFlow()

    private var runJob: Job? = null
    private var expiryJob: Job? = null
    private var resultsHeld = false

    /**
     * Holds the expiry while a page is showing the results, releasing it restarts the countdown.
     * The TTL exists to stop a stale set lingering, not to redraw a page the user is reading: it
     * landing mid-view empties the grid — and takes the evidence page opened on top of it with it
     * — under their eyes, which reads as the page breaking rather than as data ageing out.
     */
    fun setResultsHeld(held: Boolean) {
        if (resultsHeld == held) return
        resultsHeld = held
        if (held) {
            expiryJob?.cancel()
        } else if (_state.value.results.isNotEmpty()) {
            scheduleExpiry()
        }
    }

    /** Starts a fresh run over [targets], cancelling any run already in flight. */
    fun run(targets: List<ProbeTarget>) {
        runJob?.cancel()
        expiryJob?.cancel()
        runJob = scope.launch {
            _state.value = ConnectivityState(running = true)
            // Reputation is looked up once per distinct egress IP, kicked off the moment a probe
            // yields an IP (not after all probes) so it overlaps the remaining work; ip-api.com's
            // free tier allows only ~45 requests/min.
            val seen = mutableSetOf<String>()
            coroutineScope {
                val gate = Semaphore(PROBE_CONCURRENCY)
                targets.forEach { target ->
                    launch {
                        gate.withPermit {
                            val result = probeStreaming(target)
                            _state.value = _state.value.copy(
                                results = _state.value.results + (target.name to result)
                            )
                            result.ip?.let { ip -> if (seen.add(ip)) launch { lookupReputation(ip) } }
                        }
                    }
                }
            }
            _state.value = _state.value.copy(running = false)
            scheduleExpiry()
        }
    }

    /** Re-probes a single target and replaces its row once done. */
    fun retry(target: ProbeTarget) = retryAll(listOf(target))

    /**
     * Re-probes every target in [targets], leaving rows outside that set untouched. Every name is
     * marked retrying until the last probe returns, so a group's cards show their pending form
     * for the whole group run, and the shared gate keeps it inside the same concurrency budget as
     * a full run.
     */
    fun retryAll(targets: List<ProbeTarget>) {
        if (targets.isEmpty()) return
        expiryJob?.cancel()
        scope.launch {
            val names = targets.map { it.name }.toSet()
            // A group run marks its whole category so the header can show progress; a single
            // target does not, even though both share the per-card pending form.
            val groups = targets
                .map { it.category }
                .distinct()
                .singleOrNull()
                ?.takeIf { targets.size > 1 }
                ?.let { setOf(it) }
                .orEmpty()
            _state.value = _state.value.copy(
                retrying = _state.value.retrying + names,
                retryingGroups = _state.value.retryingGroups + groups,
                samples = _state.value.samples - names
            )
            val seen = mutableSetOf<String>()
            coroutineScope {
                val gate = Semaphore(PROBE_CONCURRENCY)
                targets.forEach { target ->
                    launch {
                        gate.withPermit {
                            val result = probeStreaming(target)
                            // Each card leaves the pending form the moment its own probe lands,
                            // rather than waiting for the group's slowest target.
                            _state.value = _state.value.copy(
                                results = _state.value.results + (target.name to result),
                                retrying = _state.value.retrying - target.name
                            )
                            result.ip?.let { ip ->
                                if (seen.add(ip) && ip !in _state.value.reputation) {
                                    lookupReputation(ip)
                                }
                            }
                        }
                    }
                }
            }
            _state.value = _state.value.copy(
                retrying = _state.value.retrying - names,
                retryingGroups = _state.value.retryingGroups - groups
            )
            scheduleExpiry()
        }
    }

    /** Discards the accumulated results once they have sat idle for the TTL. */
    private fun scheduleExpiry() {
        expiryJob?.cancel()
        if (resultsHeld) return
        expiryJob = scope.launch {
            delay(CONNECTIVITY_RESULT_TTL_MS)
            _state.value = ConnectivityState()
        }
    }

    /** Probes [target], streaming each latency sample into the state as it lands. */
    private suspend fun probeStreaming(target: ProbeTarget): ProbeResult =
        probe.probe(target) { ms ->
            _state.value = _state.value.copy(
                samples = _state.value.samples +
                    (target.name to (_state.value.samples[target.name].orEmpty() + ms))
            )
        }

    private suspend fun lookupReputation(ip: String) {
        val rep = reputation.lookup(ip)
        if (rep != null) {
            _state.value = _state.value.copy(
                reputation = _state.value.reputation + (ip to rep)
            )
        }
    }
}
