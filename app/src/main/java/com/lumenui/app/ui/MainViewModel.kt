package com.lumenui.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumenui.app.data.Backend
import com.lumenui.app.data.Probe
import com.lumenui.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val _backends = MutableStateFlow(container.backends.load())
    val backends: StateFlow<List<Backend>> = _backends.asStateFlow()

    private val _current = MutableStateFlow(resolveCurrent())
    val current: StateFlow<Backend?> = _current.asStateFlow()

    private val _probe = MutableStateFlow<Probe?>(null)
    val probe: StateFlow<Probe?> = _probe.asStateFlow()

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
        if (index >= 0) list[index] = backend else list.add(backend)
        _backends.value = list
        container.backends.save(list)
        if (_current.value == null || _current.value?.id == backend.id) {
            selectBackend(backend)
        }
    }

    fun deleteBackend(backend: Backend) {
        val list = _backends.value.filterNot { it.id == backend.id }
        _backends.value = list
        container.backends.save(list)
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
     */
    fun syncFromPanel(panel: List<Backend>) {
        if (panel.isEmpty()) return
        val unique = panel.distinctBy { it.identity() }
        val existing = _backends.value
        val byIdentity = existing.associateBy { it.identity() }
        val merged = unique.map { p -> byIdentity[p.identity()]?.let { p.copy(id = it.id) } ?: p }
        if (merged == existing) return
        _backends.value = merged
        container.backends.save(merged)

        val current = _current.value
        val resolved = merged.firstOrNull { it.id == current?.id } ?: merged.first()
        if (current?.id != resolved.id) selectBackend(resolved) else _current.value = resolved
    }

    private fun Backend.identity() = "${protocol.lowercase()}://${host.lowercase()}:$port"

    fun refreshStatus(backend: Backend) {
        viewModelScope.launch {
            _probe.value = container.mihomoApi.probe(backend)
        }
    }

    /** Re-probes the current backend: used by the refresh button and opening the switcher. */
    fun refreshCurrent() {
        _current.value?.let { refreshStatus(it) }
    }
}
