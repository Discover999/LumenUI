package com.lumenui.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lumenui.app.LumenApp
import com.lumenui.app.data.Backend
import com.lumenui.app.data.ConnectivityTargets
import com.lumenui.app.data.ProbeResult
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.lumenui.app.ui.components.AboutDialog
import com.lumenui.app.ui.components.BackendEditDialog
import com.lumenui.app.ui.components.BackendsMenuContent
import com.lumenui.app.ui.components.ConnectivityScreen
import com.lumenui.app.ui.components.ConnectivitySettingsDialog
import com.lumenui.app.ui.components.FrostedTopBar
import com.lumenui.app.ui.components.GlassMenuItem
import com.lumenui.app.ui.components.PanelWebView
import com.lumenui.app.ui.components.SetupScreen
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.web.PanelController
import com.lumenui.app.web.PanelUrls
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Composable
fun MainScreen() {
    val app = LocalContext.current.applicationContext as LumenApp
    val viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer { MainViewModel(app.container) }
        }
    )

    val backends by viewModel.backends.collectAsStateWithLifecycle()
    val current by viewModel.current.collectAsStateWithLifecycle()
    val probe by viewModel.probe.collectAsStateWithLifecycle()

    val controller = remember { PanelController() }
    DisposableEffect(controller) {
        controller.onPanelBackends = { viewModel.syncFromPanel(it) }
        onDispose { controller.onPanelBackends = null }
    }
    var loading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showConnectivity by remember { mutableStateOf(false) }
    var connectivityRunning by remember { mutableStateOf(false) }
    var connectivityResults by remember { mutableStateOf<Map<String, ProbeResult>>(emptyMap()) }
    var connectivityRunId by remember { mutableStateOf(0) }
    var selectedProbes by remember { mutableStateOf(app.container.settings.connectivityTargets) }
    var showProbeSettings by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Backend?>(null) }

    LaunchedEffect(showConnectivity, connectivityRunId) {
        // Nothing runs until the user taps 开始测试 / 重新测试 (runId 0 is the idle state).
        if (!showConnectivity || connectivityRunId == 0) return@LaunchedEffect
        connectivityRunning = true
        connectivityResults = emptyMap()
        val targets = ConnectivityTargets.all.filter { it.name in selectedProbes }
        coroutineScope {
            val gate = Semaphore(PROBE_CONCURRENCY)
            targets.forEach { target ->
                launch {
                    gate.withPermit {
                        val result = app.container.connectivity.probe(target)
                        connectivityResults = connectivityResults + (target.name to result)
                    }
                }
            }
        }
        connectivityRunning = false
    }

    BackHandler(enabled = showConnectivity || canGoBack) {
        if (showConnectivity) showConnectivity = false else controller.goBack()
    }

    val glass = LocalGlassColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(glass.backgroundTop, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        val backend = current
        if (backend == null) {
            SetupScreen(onAdd = {
                editing = null
                showEditor = true
            })
        } else {
            Column(Modifier.fillMaxSize()) {
                FrostedTopBar(
                    title = backend.displayName,
                    online = probe?.ok,
                    onHome = { controller.load(PanelUrls.setupUrl(backend)) },
                    onRefresh = {
                        controller.reload()
                        viewModel.refreshCurrent()
                    },
                    onIp = { controller.load(IP_CHECK_URL) },
                    titleMenu = { dismiss ->
                        // Re-scan the panel and re-probe the backend each time the switcher opens.
                        LaunchedEffect(Unit) {
                            controller.scanPanelBackends()
                            viewModel.refreshCurrent()
                        }
                        BackendsMenuContent(
                            backends = backends,
                            currentId = current?.id,
                            probe = probe,
                            onSelect = {
                                viewModel.selectBackend(it)
                                dismiss()
                            },
                            onAdd = {
                                dismiss()
                                editing = null
                                showEditor = true
                            },
                            onEdit = {
                                dismiss()
                                editing = it
                                showEditor = true
                            },
                            onDelete = {
                                viewModel.deleteBackend(it)
                                controller.pushBackends(viewModel.backends.value)
                            }
                        )
                    },
                    actionMenu = { dismiss ->
                        GlassMenuItem(Lucide.Globe, "连通性测试") {
                            dismiss()
                            showConnectivity = true
                        }
                        GlassMenuItem(Lucide.ExternalLink, "在浏览器打开") {
                            dismiss()
                            controller.openInBrowser(PanelUrls.uiUrl(backend))
                        }
                        GlassMenuItem(Lucide.Info, "关于") {
                            dismiss()
                            showAbout = true
                        }
                    }
                )
                Box(Modifier.fillMaxSize()) {
                    PanelWebView(
                        url = PanelUrls.setupUrl(backend),
                        controller = controller,
                        onLoadingChanged = { loading = it },
                        onCanGoBackChanged = { canGoBack = it },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (loading) {
                        LinearProgressIndicator(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        BackendEditDialog(
            initial = editing,
            onSave = {
                viewModel.upsertBackend(it)
                controller.pushBackends(viewModel.backends.value)
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    if (showConnectivity) {
        // Full-screen page in its own window, so it covers the WebView and blocks touches.
        Dialog(
            onDismissRequest = { showConnectivity = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            ConnectivityScreen(
                targets = ConnectivityTargets.all.filter { it.name in selectedProbes },
                running = connectivityRunning,
                results = connectivityResults,
                onBack = { showConnectivity = false },
                onRerun = { connectivityRunId++ },
                onSettings = { showProbeSettings = true }
            )
        }

        if (showProbeSettings) {
            ConnectivitySettingsDialog(
                selected = selectedProbes,
                onToggle = { name ->
                    val next = if (name in selectedProbes) selectedProbes - name else selectedProbes + name
                    selectedProbes = next
                    app.container.settings.connectivityTargets = next
                },
                onSelectAll = {
                    val all = ConnectivityTargets.all.map { it.name }.toSet()
                    selectedProbes = all
                    app.container.settings.connectivityTargets = all
                },
                onSelectDefault = {
                    selectedProbes = ConnectivityTargets.defaultSelected
                    app.container.settings.connectivityTargets = ConnectivityTargets.defaultSelected
                },
                onDismiss = { showProbeSettings = false }
            )
        }
    }
}

private const val IP_CHECK_URL = "https://ip.skk.moe"
private const val PROBE_CONCURRENCY = 8
