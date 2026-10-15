package com.lumenui.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumenui.app.data.Backend
import com.lumenui.app.data.ConnectivityTargets
import com.lumenui.app.data.Probe
import com.lumenui.app.data.ProbeTarget
import com.lumenui.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val _backends = MutableStateFlow(container.backends.load())
    val backends: StateFlow<List<Backend>> = _backends.asStateFlow()

    private val _current = MutableStateFlow(resolveCurrent())
    val current: StateFlow<Backend?> = _current.asStateFlow()

    private val _probe = MutableStateFlow<Probe?>(null)
    val probe: StateFlow<Probe?> = _probe.asStateFlow()

    /** Latest probe per backend id, so the switcher can show every backend's latency at once. */
    private val _probes = MutableStateFlow<Map<String, Probe>>(emptyMap())
    val probes: StateFlow<Map<String, Probe>> = _probes.asStateFlow()

    private val connectivityRunner =
        ConnectivityRunner(container.connectivity, container.ipReputation, viewModelScope)

    /** State of the connectivity page, driven by [connectivityRunner]. */
    val connectivity: StateFlow<ConnectivityState> = connectivityRunner.state

    private val _connectivityTargets = MutableStateFlow(container.settings.connectivityTargets)
    val connectivityTargets: StateFlow<Set<String>> = _connectivityTargets.asStateFlow()

    init {
        _current.value?.let { refreshStatus(it) }
    }

    private fun resolveCurrent(): Backend? {
        val id = container.settings.currentBackendId
        return _backends.value.firstOrNull { it.id == id } ?: _backends.value.firstOrNull()
    }

    fun selectBackend(backend: Backend) {
        container.settings.currentBackendId = backend.id
        _current.value = backend
        _probe.value = null
        refreshStatus(backend)
    }

    fun upsertBackend(backend: Backend) {
        val list = _backends.value.toMutableList()
        val index = list.indexOfFirst { it.id == backend.id }
        val isNew = index < 0
        if (index >= 0) list[index] = backend else list.add(backend)
        _backends.value = list
        container.backends.save(list)
        // A freshly added backend becomes the active one; editing keeps the current
        // selection, except when the edited backend *is* the current one (re-selected
        // so the change is applied).
        if (isNew || _current.value?.id == backend.id) {
            selectBackend(backend)
        }
    }

    fun deleteBackend(backend: Backend) {
        val list = _backends.value.filterNot { it.id == backend.id }
        _backends.value = list
        container.backends.save(list)
        _probes.value = _probes.value - backend.id
        if (_current.value?.id == backend.id) {
            val next = list.firstOrNull()
            container.settings.currentBackendId = next?.id
            _current.value = next
            _probe.value = null
            next?.let { refreshStatus(it) }
        }
    }

    /**
     * Mirrors the zashboard panel's backend list: the app's list becomes exactly what the
     * panel has, so additions *and* deletions made in the panel propagate. Entries that
     * already exist keep their app-side id, so the current selection survives a sync. An
     * empty panel list is ignored, to avoid wiping the app if the panel is unreadable/empty.
     *
     * [active] is the backend the panel currently has selected; when it maps to a known
     * backend the app follows it, so switching backends inside the panel updates the top bar.
     */
    fun syncFromPanel(panel: List<Backend>, active: Backend?) {
        if (panel.isEmpty()) return
        val unique = panel.distinctBy { it.identity() }
        val existing = _backends.value
        val byIdentity = existing.associateBy { it.identity() }
        val merged = unique.map { p -> byIdentity[p.identity()]?.let { p.copy(id = it.id) } ?: p }
        if (merged != existing) {
            _backends.value = merged
            container.backends.save(merged)
        }

        // Follow the panel's active backend; without one, keep the current selection if it
        // still exists, otherwise fall back to the first backend.
        val desired = active?.let { a -> merged.firstOrNull { it.identity() == a.identity() } }
            ?: merged.firstOrNull { it.id == _current.value?.id }
            ?: merged.first()
        val current = _current.value
        if (current?.id != desired.id) {
            selectBackend(desired)
        } else if (current != desired) {
            _current.value = desired
        }
    }

    private fun Backend.identity() = "${protocol.lowercase()}://${host.lowercase()}:$port"

    fun refreshStatus(backend: Backend) {
        viewModelScope.launch { probeInto(backend) }
    }

    /** Probes every stored backend concurrently, so the switcher can show all their latencies. */
    fun refreshAll() {
        viewModelScope.launch {
            _backends.value.forEach { backend -> launch { probeInto(backend) } }
        }
    }

    /** Re-probes the current backend: used by the refresh button. */
    fun refreshCurrent() {
        _current.value?.let { refreshStatus(it) }
    }

    private suspend fun probeInto(backend: Backend) {
        val result = container.mihomoApi.probe(backend)
        _probes.value = _probes.value + (backend.id to result)
        if (_current.value?.id == backend.id) _probe.value = result
    }

    // --- Connectivity page ---------------------------------------------------

    /** Starts a fresh run over the currently selected targets. */
    fun runConnectivity() {
        connectivityRunner.run(ConnectivityTargets.all.filter { it.name in _connectivityTargets.value })
    }

    /** Re-probes a single target and replaces its row once done. */
    fun retryConnectivity(target: ProbeTarget) {
        connectivityRunner.retry(target)
    }

    fun toggleConnectivityTarget(name: String) {
        setConnectivityTargets(
            if (name in _connectivityTargets.value) _connectivityTargets.value - name
            else _connectivityTargets.value + name
        )
    }

    fun selectAllConnectivityTargets() {
        setConnectivityTargets(ConnectivityTargets.all.map { it.name }.toSet())
    }

    fun selectDefaultConnectivityTargets() {
        setConnectivityTargets(ConnectivityTargets.defaultSelected)
    }

    private fun setConnectivityTargets(next: Set<String>) {
        _connectivityTargets.value = next
        container.settings.connectivityTargets = next
    }
}
