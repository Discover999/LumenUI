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
import androidx.compose.runtime.rememberUpdatedState
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
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Radar
import com.lumenui.app.ui.components.AboutDialog
import com.lumenui.app.ui.components.BackendEditDialog
import com.lumenui.app.ui.components.BackendsMenuContent
import com.lumenui.app.ui.components.ConnectivityScreen
import com.lumenui.app.ui.components.ConnectivitySettingsDialog
import com.lumenui.app.ui.components.DnsLeakScreen
import com.lumenui.app.ui.components.FrostedTopBar
import com.lumenui.app.ui.components.FullScreenDialog
import com.lumenui.app.ui.components.GlassMenuItem
import com.lumenui.app.ui.components.PanelWebView
import com.lumenui.app.ui.components.SetupScreen
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.web.PanelAssets
import com.lumenui.app.web.PanelController
import com.lumenui.app.web.PanelUrls
import androidx.compose.ui.platform.LocalContext

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
    val probes by viewModel.probes.collectAsStateWithLifecycle()
    val connectivity by viewModel.connectivity.collectAsStateWithLifecycle()
    val selectedProbes by viewModel.connectivityTargets.collectAsStateWithLifecycle()

    val controller = remember { PanelController() }
    val latestCurrent by rememberUpdatedState(current)
    DisposableEffect(controller) {
        controller.onPanelBackends = { list, active -> viewModel.syncFromPanel(list, active) }
        // The panel page finished loading: hand it the active backend (no-op after the
        // first time) so zashboard connects without the secret ever touching a URL.
        controller.onPanelReady = { latestCurrent?.let(controller::connect) }
        onDispose {
            controller.onPanelBackends = null
            controller.onPanelReady = null
        }
    }
    var loading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showConnectivity by remember { mutableStateOf(false) }
    var showDnsLeak by remember { mutableStateOf(false) }
    var showProbeSettings by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Backend?>(null) }

    // Hand the selected backend to the panel whenever it changes. Applied at most once
    // per backend; the initial application happens via onPanelReady once the page loads.
    LaunchedEffect(current?.id) {
        current?.let(controller::connect)
    }

    BackHandler(enabled = showConnectivity || showDnsLeak || canGoBack) {
        when {
            showConnectivity -> showConnectivity = false
            showDnsLeak -> showDnsLeak = false
            else -> controller.goBack()
        }
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
                    onHome = { controller.load(PanelAssets.INDEX_URL) },
                    onRefresh = {
                        controller.reload()
                        viewModel.refreshCurrent()
                    },
                    onIp = { controller.load(IP_CHECK_URL) },
                    titleMenu = { dismiss ->
                        // Re-scan the panel and probe *every* backend each time the switcher opens.
                        LaunchedEffect(Unit) {
                            controller.scanPanelBackends()
                            viewModel.refreshAll()
                        }
                        BackendsMenuContent(
                            backends = backends,
                            currentId = current?.id,
                            probes = probes,
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
                        GlassMenuItem(Lucide.Radar, "DNS 泄露检测") {
                            dismiss()
                            showDnsLeak = true
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
                        url = PanelAssets.INDEX_URL,
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
        FullScreenDialog(onDismissRequest = { showConnectivity = false }) {
            ConnectivityScreen(
                targets = ConnectivityTargets.all.filter { it.name in selectedProbes },
                running = connectivity.running,
                retrying = connectivity.retrying,
                results = connectivity.results,
                samples = connectivity.samples,
                reputation = connectivity.reputation,
                onBack = { showConnectivity = false },
                onRerun = { viewModel.runConnectivity() },
                onSettings = { showProbeSettings = true },
                onRetry = { target -> viewModel.retryConnectivity(target) }
            )
        }

        if (showProbeSettings) {
            ConnectivitySettingsDialog(
                selected = selectedProbes,
                onToggle = { viewModel.toggleConnectivityTarget(it) },
                onSelectAll = { viewModel.selectAllConnectivityTargets() },
                onSelectDefault = { viewModel.selectDefaultConnectivityTargets() },
                onDismiss = { showProbeSettings = false }
            )
        }
    }

    if (showDnsLeak) {
        // Full-screen page in its own window, so it covers the WebView and blocks touches.
        FullScreenDialog(onDismissRequest = { showDnsLeak = false }) {
            DnsLeakScreen(
                onBack = { showDnsLeak = false },
                load = { app.container.dnsLeak.run() }
            )
        }
    }
}

private const val IP_CHECK_URL = "https://ip.skk.moe"
