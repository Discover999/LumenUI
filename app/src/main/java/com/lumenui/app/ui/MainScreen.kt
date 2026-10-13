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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.lumenui.app.data.IpReputation
import com.lumenui.app.data.ProbeResult
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
import com.lumenui.app.ui.components.GlassMenuItem
import com.lumenui.app.ui.components.PanelWebView
import com.lumenui.app.ui.components.SetupScreen
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.web.PanelAssets
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
    val probes by viewModel.probes.collectAsStateWithLifecycle()

    val controller = remember { PanelController() }
    val scope = rememberCoroutineScope()
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
    var connectivityRunning by remember { mutableStateOf(false) }
    var connectivityResults by remember { mutableStateOf<Map<String, ProbeResult>>(emptyMap()) }
    var reputationByIp by remember { mutableStateOf<Map<String, IpReputation>>(emptyMap()) }
    var samplesByTarget by remember { mutableStateOf<Map<String, List<Long>>>(emptyMap()) }
    var connectivityRunId by remember { mutableStateOf(0) }
    var selectedProbes by remember { mutableStateOf(app.container.settings.connectivityTargets) }
    var showProbeSettings by remember { mutableStateOf(false) }
    var retrying by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editing by remember { mutableStateOf<Backend?>(null) }

    LaunchedEffect(showConnectivity, connectivityRunId) {
        // Nothing runs until the user taps 开始测试 / 重新测试 (runId 0 is the idle state).
        if (!showConnectivity || connectivityRunId == 0) return@LaunchedEffect
        connectivityRunning = true
        connectivityResults = emptyMap()
        reputationByIp = emptyMap()
        samplesByTarget = emptyMap()
        // Reputation is looked up once per distinct egress IP, kicked off the moment a
        // probe yields an IP (not after all probes) so it overlaps the remaining work.
        // Everything here runs on the composition's Main dispatcher, so the plain set
        // needs no synchronization; ip-api.com's free tier allows only ~45 requests/min.
        val started = mutableSetOf<String>()
        val targets = ConnectivityTargets.all.filter { it.name in selectedProbes }
        coroutineScope {
            val gate = Semaphore(PROBE_CONCURRENCY)
            targets.forEach { target ->
                launch {
                    gate.withPermit {
                        val result = app.container.connectivity.probe(target) { ms ->
                            samplesByTarget = samplesByTarget +
                                (target.name to (samplesByTarget[target.name].orEmpty() + ms))
                        }
                        connectivityResults = connectivityResults + (target.name to result)
                        result.ip?.let { ip ->
                            if (started.add(ip)) {
                                launch {
                                    val rep = app.container.ipReputation.lookup(ip)
                                    if (rep != null) reputationByIp = reputationByIp + (ip to rep)
                                }
                            }
                        }
                    }
                }
            }
        }
        connectivityRunning = false
    }

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
                retrying = retrying,
                results = connectivityResults,
                samples = samplesByTarget,
                reputation = reputationByIp,
                onBack = { showConnectivity = false },
                onRerun = { connectivityRunId++ },
                onSettings = { showProbeSettings = true },
                onRetry = { target ->
                    retrying = retrying + target.name
                    samplesByTarget = samplesByTarget - target.name
                    scope.launch {
                        val result = app.container.connectivity.probe(target) { ms ->
                            samplesByTarget = samplesByTarget +
                                (target.name to (samplesByTarget[target.name].orEmpty() + ms))
                        }
                        connectivityResults = connectivityResults + (target.name to result)
                        result.ip?.let { ip ->
                            if (ip !in reputationByIp) {
                                val rep = app.container.ipReputation.lookup(ip)
                                if (rep != null) reputationByIp = reputationByIp + (ip to rep)
                            }
                        }
                        retrying = retrying - target.name
                    }
                }
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

    if (showDnsLeak) {
        // Full-screen page in its own window, so it covers the WebView and blocks touches.
        Dialog(
            onDismissRequest = { showDnsLeak = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            DnsLeakScreen(
                onBack = { showDnsLeak = false },
                load = { app.container.dnsLeak.run() }
            )
        }
    }
}

private const val IP_CHECK_URL = "https://ip.skk.moe"
private const val PROBE_CONCURRENCY = 8
