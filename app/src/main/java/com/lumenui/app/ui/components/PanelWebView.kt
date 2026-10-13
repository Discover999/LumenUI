package com.lumenui.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewClientCompat
import com.lumenui.app.web.PanelAssetLoader
import com.lumenui.app.web.PanelAssets
import com.lumenui.app.web.PanelController

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PanelWebView(
    url: String,
    controller: PanelController,
    onLoadingChanged: (Boolean) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assetLoader = remember { PanelAssetLoader(context.applicationContext) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(AndroidColor.TRANSPARENT)
                overScrollMode = View.OVER_SCROLL_NEVER

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mediaPlaybackRequiresUserGesture = false
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    allowFileAccess = false
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                webViewClient = object : WebViewClientCompat() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        assetLoader.loader.shouldInterceptRequest(request.url)?.let { return it }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView,
                        url: String?,
                        isReload: Boolean
                    ) {
                        onCanGoBackChanged(view.canGoBack())
                        // Switching backends inside the panel is an in-app navigation, so
                        // re-read its list/active here to keep the app's top bar in sync.
                        if (url != null && url.startsWith(PanelAssets.ORIGIN)) {
                            controller.scanPanelBackends()
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        // Pull zashboard's own backend list whenever the panel (re)loads, so
                        // backends added inside the panel show up in the app switcher, and let
                        // the app apply its active backend from here on out.
                        if (url != null && url.startsWith(PanelAssets.ORIGIN)) {
                            controller.watchActiveBackend()
                            controller.scanPanelBackends()
                            controller.notifyPanelReady()
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onLoadingChanged(newProgress < 100)
                    }

                    override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                        // The injected localStorage tap noticed an in-panel backend switch.
                        if (msg.message() == PanelAssets.ACTIVE_WATCH_SENTINEL) {
                            controller.scanPanelBackends()
                            return true
                        }
                        return false
                    }
                }

                controller.webView = this
            }
        },
        onRelease = { view ->
            controller.detach(view)
            view.destroy()
        }
    )

    LaunchedEffect(url) {
        controller.load(url)
    }
}
