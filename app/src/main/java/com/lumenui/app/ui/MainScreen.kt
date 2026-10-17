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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lumenui.app.LumenApp
import com.lumenui.app.data.Backend
import com.lumenui.app.data.ConnectivityTargets
import com.lumenui.app.data.ProbeTarget
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Radar
import com.lumenui.app.ui.components.AboutDialog
import com.lumenui.app.ui.components.BackendEditDialog
import com.lumenui.app.ui.components.BackendsMenuContent
import com.lumenui.app.ui.components.ConnectivityDetailScreen
import com.lumenui.app.ui.components.ConnectivityScreen
import com.lumenui.app.ui.components.ConnectivitySettingsDialog
import com.lumenui.app.ui.components.DnsLeakScreen
import com.lumenui.app.ui.components.FrostedTopBar
import com.lumenui.app.ui.components.FullScreenDialog
import com.lumenui.app.ui.components.GlassMenuItem
import com.lumenui.app.ui.components.PanelWebView
import com.lumenui.app.ui.components.SetupScreen
import com.lumenui.app.ui.components.SpeedHistoryScreen
import com.lumenui.app.ui.components.SpeedTestScreen
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.web.PanelAssets
import com.lumenui.app.web.PanelController
import com.lumenui.app.web.PanelUrls
import androidx.compose.ui.platform.LocalContext

@Composable
fun MainScreen(
    /** A launcher shortcut asked for a specific page; consumed once via [onStartDestinationHandled]. */
    startDestination: StartDestination? = null,
    onStartDestinationHandled: () -> Unit = {}
) {
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
    val speedTest by viewModel.speedTest.collectAsStateWithLifecycle()

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
    var showSpeedTest by remember { mutableStateOf(false) }
    var showSpeedHistory by remember { mutableStateOf(false) }
    var showProbeSettings by remember { mutableStateOf(false) }
    var detailTarget by remember { mutableStateOf<ProbeTarget?>(null) }
    var editing by remember { mutableStateOf<Backend?>(null) }

    // The page being shown, kept until its slide-out finishes. The flags above drive the
    // transition; these hold what the page renders, so closing a page still has content to
    // animate and the detail page keeps the target it was opened for.
    var connectivityPage by remember { mutableStateOf(false) }
    var dnsPage by remember { mutableStateOf(false) }
    var speedPage by remember { mutableStateOf(false) }
    var speedHistoryPage by remember { mutableStateOf(false) }
    var detailPage by remember { mutableStateOf<ProbeTarget?>(null) }

    // Hand the selected backend to the panel whenever it changes. Applied at most once
    // per backend; the initial application happens via onPanelReady once the page loads.
    LaunchedEffect(current?.id) {
        current?.let(controller::connect)
    }

    // The top bar's status light only reports the last probe, so the current backend is
    // re-probed on a timer. Bounded to RESUMED: polling while the app is off screen would
    // spend the phone's proxied bandwidth to keep a dot nobody is looking at up to date.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.pollCurrentStatus()
        }
    }

    // A run's results expire after a TTL, which would empty the grid — and take the evidence page
    // opened on top of it with it — while they are being read. Held for as long as either page
    // window is still on screen, released once the last one has finished sliding out.
    val resultsOnScreen = connectivityPage || detailPage != null
    LaunchedEffect(resultsOnScreen) {
        viewModel.holdConnectivityResults(resultsOnScreen)
    }

    // A launcher shortcut asked for a page: open it, then report it handled so returning to
    // the app later does not re-open the same page. The panel pages need a backend; the speed
    // page measures the network directly, so it opens even before one is configured.
    LaunchedEffect(startDestination, current?.id) {
        val destination = startDestination ?: return@LaunchedEffect
        if (current == null && destination != StartDestination.SPEED) return@LaunchedEffect
        when (destination) {
            StartDestination.CONNECTIVITY -> {
                connectivityPage = true
                showConnectivity = true
            }
            StartDestination.DNS -> {
                dnsPage = true
                showDnsLeak = true
            }
            StartDestination.SPEED -> {
                speedPage = true
                showSpeedTest = true
            }
        }
        onStartDestinationHandled()
    }

    // A finished run's results expire after a TTL, which takes the evidence page with them.
    // Gating on "there is a result to show" keeps the back press from being consumed by a
    // page that is no longer on screen.
    val detailVisible = detailTarget?.let { connectivity.results[it.name] != null } == true

    // The page windows outlive the flags that opened them, so a close is only half done when
    // the flag flips: the window is still sliding out and still on screen. Keeping back
    // consumed until it reports closed stops a second back press from reaching the panel
    // through a page the user can still see.
    val connectivityClosing = connectivityPage && !showConnectivity
    val dnsClosing = dnsPage && !showDnsLeak
    val speedClosing = speedPage && !showSpeedTest
    val speedHistoryClosing = speedHistoryPage && !showSpeedHistory
    val detailClosing = detailPage?.let { detailTarget?.name != it.name } == true

    BackHandler(
        enabled = showConnectivity || showDnsLeak || showSpeedTest || showSpeedHistory ||
            detailVisible || connectivityClosing || dnsClosing || speedClosing ||
            speedHistoryClosing || detailClosing || canGoBack
    ) {
        when {
            // The detail page is on top of the connectivity page, so it closes first.
            detailVisible -> detailTarget = null
            detailClosing -> Unit
            showConnectivity -> showConnectivity = false
            connectivityClosing -> Unit
            showDnsLeak -> showDnsLeak = false
            dnsClosing -> Unit
            showSpeedHistory -> showSpeedHistory = false
            speedHistoryClosing -> Unit
            showSpeedTest -> showSpeedTest = false
            speedClosing -> Unit
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
                            connectivityPage = true
                            showConnectivity = true
                        }
                        GlassMenuItem(Lucide.Radar, "DNS 泄露检测") {
                            dismiss()
                            dnsPage = true
                            showDnsLeak = true
                        }
                        GlassMenuItem(Lucide.Gauge, "网络测速") {
                            dismiss()
                            speedPage = true
                            showSpeedTest = true
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

    // The dialogs are composed whether or not they are open, and are handed the flag the `if`
    // around them used to carry. Each one holds its own window up until its exit has finished, so
    // it has to still be in the composition at the moment the flag drops; a flag alone would be
    // gone before the card had anywhere to shrink into.
    BackendEditDialog(
        visible = showEditor,
        initial = editing,
        onSave = {
            viewModel.upsertBackend(it)
            controller.pushBackends(viewModel.backends.value)
            showEditor = false
        },
        onDismiss = { showEditor = false }
    )

    AboutDialog(visible = showAbout, onDismiss = { showAbout = false })

    if (connectivityPage) {
        // Full-screen page in its own window, so it covers the WebView and blocks touches.
        FullScreenDialog(
            visible = showConnectivity,
            onDismissRequest = { showConnectivity = false },
            onClosed = { connectivityPage = false }
        ) {
            ConnectivityScreen(
                targets = ConnectivityTargets.all.filter { it.name in selectedProbes },
                running = connectivity.running,
                retrying = connectivity.retrying,
                retryingGroups = connectivity.retryingGroups,
                results = connectivity.results,
                samples = connectivity.samples,
                reputation = connectivity.reputation,
                onBack = { showConnectivity = false },
                onRerun = { viewModel.runConnectivity() },
                onSettings = { showProbeSettings = true },
                onRetry = { target -> viewModel.retryConnectivity(target) },
                onRetryGroup = { targets -> viewModel.retryConnectivityGroup(targets) },
                onOpenDetail = { target ->
                    detailPage = target
                    detailTarget = target
                }
            )
        }

        ConnectivitySettingsDialog(
            visible = showProbeSettings,
            selected = selectedProbes,
            onToggle = { viewModel.toggleConnectivityTarget(it) },
            onSelectAll = { viewModel.selectAllConnectivityTargets() },
            onSelectDefault = { viewModel.selectDefaultConnectivityTargets() },
            onDismiss = { showProbeSettings = false }
        )
    }

    if (dnsPage) {
        // Full-screen page in its own window, so it covers the WebView and blocks touches.
        FullScreenDialog(
            visible = showDnsLeak,
            onDismissRequest = { showDnsLeak = false },
            onClosed = { dnsPage = false }
        ) {
            DnsLeakScreen(
                onBack = { showDnsLeak = false },
                load = { app.container.dnsLeak.run() }
            )
        }
    }

    if (speedPage) {
        FullScreenDialog(
            visible = showSpeedTest,
            onDismissRequest = { showSpeedTest = false },
            onClosed = {
                speedPage = false
                viewModel.leaveSpeedTest()
            }
        ) {
            SpeedTestScreen(
                state = speedTest,
                onBack = { showSpeedTest = false },
                onStart = { viewModel.startSpeedTest() },
                onCancel = { viewModel.cancelSpeedTest() },
                onHistory = {
                    speedHistoryPage = true
                    showSpeedHistory = true
                }
            )
        }
    }

    if (speedHistoryPage) {
        FullScreenDialog(
            visible = showSpeedHistory,
            onDismissRequest = { showSpeedHistory = false },
            onClosed = { speedHistoryPage = false }
        ) {
            SpeedHistoryScreen(
                load = { app.container.speedHistory.load() },
                remove = { app.container.speedHistory.remove(it) },
                clear = { app.container.speedHistory.clear() },
                onBack = { showSpeedHistory = false }
            )
        }
    }

    // The evidence page is its own window on top of the connectivity page, so it gets the
    // same edge-to-edge treatment and the BackHandler above closes it first. The result is
    // looked up by the retained target, so the page keeps its content while it slides out
    // even after the run's results age out.
    val detail = detailPage
    val detailResult = detail?.let { connectivity.results[it.name] }
    if (detail != null && detailResult != null) {
        FullScreenDialog(
            visible = detailTarget?.name == detail.name,
            onDismissRequest = { detailTarget = null },
            onClosed = { detailPage = null }
        ) {
            ConnectivityDetailScreen(
                target = detail,
                result = detailResult,
                reputation = detailResult.ip?.let { connectivity.reputation[it] },
                onBack = { detailTarget = null }
            )
        }
    }
}

private const val IP_CHECK_URL = "https://ip.skk.moe"
