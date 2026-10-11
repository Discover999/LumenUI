package com.lumenui.app.web

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import com.lumenui.app.data.Backend
import java.io.IOException
import java.net.URLEncoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PanelAssets {
    // Virtual host only: requests to it are intercepted locally by WebViewAssetLoader and
    // served from the APK assets. ".internal" is ICANN-reserved for private use, so it is
    // never resolved by public DNS.
    const val DOMAIN = "panel.internal"
    const val PREFIX = "/zashboard/"

    // The panel is served over plain http on the asset-loader origin. This keeps the panel
    // origin consistent with the (usually http/ws) mihomo backend: no mixed-content blocking,
    // and zashboard's own "http backend over https panel" tip is not triggered.
    const val SCHEME = "http"
    const val INDEX_URL = "$SCHEME://$DOMAIN$PREFIX" + "index.html"
    const val ORIGIN = "$SCHEME://$DOMAIN$PREFIX"

    /**
     * Reads zashboard's own backend list out of its localStorage. zashboard keeps the
     * saved backends under `setup/api-list` (a JSON array) and the active one under
     * `setup/active-uuid`; these live in the WebView origin, separate from the app's
     * SharedPreferences list. Returns `{ok, list}`, where `list` is a normalised array
     * (port coerced to a number). `ok` is false if the read failed, so callers can tell
     * "no backends" apart from "could not read".
     */
    val SCAN_BACKENDS_JS = """
        (function () {
          try {
            var raw = localStorage.getItem('setup/api-list');
            var arr = raw ? JSON.parse(raw) : [];
            var out = [];
            for (var i = 0; i < arr.length; i++) {
              var b = arr[i] || {};
              var port = parseInt(b.port, 10);
              if (!b.host || !isFinite(port) || port <= 0) continue;
              out.push({
                protocol: String(b.protocol || 'http'),
                host: String(b.host),
                port: port,
                secret: String(b.password || b.secret || ''),
                path: String(b.secondaryPath || ''),
                label: String(b.label || '')
              });
            }
            return JSON.stringify({ ok: true, list: out });
          } catch (e) {
            return JSON.stringify({ ok: false, list: [] });
          }
        })()
    """.trimIndent()

    /**
     * Overwrites `setup/api-list` with [payloadBase64] (a base64 JSON array of
     * `{protocol,host,port,secret,path}`), preserving the extra fields and uuid of any
     * entry that already exists (matched by host:port). Entries only present in the panel
     * are dropped, so the panel mirrors the app. Returns the resulting count, or -1 on error.
     */
    fun pushBackendsJs(payloadBase64: String): String = """
        (function () {
          try {
            var keyOf = function (b) { return String(b.host || '').toLowerCase() + ':' + parseInt(b.port, 10); };
            var bin = atob('$payloadBase64');
            var bytes = new Uint8Array(bin.length);
            for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
            var incoming = JSON.parse(new TextDecoder('utf-8').decode(bytes));
            var raw = localStorage.getItem('setup/api-list');
            var existing = raw ? JSON.parse(raw) : [];
            var prev = {};
            for (var j = 0; j < existing.length; j++) prev[keyOf(existing[j])] = existing[j];
            var out = [];
            var seen = {};
            for (var k = 0; k < incoming.length; k++) {
              var b = incoming[k];
              var key = keyOf(b);
              if (!b.host || !key || seen[key]) continue;
              seen[key] = true;
              var p = prev[key];
              out.push({
                uuid: (p && p.uuid) ? p.uuid : (crypto.randomUUID ? crypto.randomUUID() : ('' + Date.now() + Math.random())),
                type: (p && p.type) ? p.type : 'clash',
                protocol: b.protocol || 'http',
                secondaryPath: b.secondaryPath || '',
                host: String(b.host),
                port: parseInt(b.port, 10),
                password: b.secret || '',
                label: b.label || (p && p.label) || '',
                disableUpgradeCore: p ? !!p.disableUpgradeCore : false,
                disableTunMode: p ? !!p.disableTunMode : false
              });
            }
            localStorage.setItem('setup/api-list', JSON.stringify(out));
            var active = localStorage.getItem('setup/active-uuid') || '';
            if (active && !out.some(function (e) { return e.uuid === active; })) {
              localStorage.setItem('setup/active-uuid', out.length ? out[0].uuid : '');
            }
            return out.length;
          } catch (e) {
            return -1;
          }
        })()
    """.trimIndent()
}

/** Serves the bundled zashboard build from assets over the asset-loader origin. */
class PanelAssetLoader(private val context: Context) {

    val loader: WebViewAssetLoader = WebViewAssetLoader.Builder()
        .setDomain(PanelAssets.DOMAIN)
        .setHttpAllowed(true)
        .addPathHandler(PanelAssets.PREFIX, AssetHandler(context))
        .build()

    private class AssetHandler(private val context: Context) : WebViewAssetLoader.PathHandler {
        override fun handle(path: String): WebResourceResponse? {
            var relative = path.removePrefix(PanelAssets.PREFIX)
            if (relative.isEmpty() || relative.endsWith("/")) relative += "index.html"
            return try {
                WebResourceResponse(
                    mimeOf(relative),
                    "utf-8",
                    200,
                    "OK",
                    mapOf("Cache-Control" to "no-cache"),
                    context.assets.open("zashboard/$relative")
                )
            } catch (e: IOException) {
                null
            }
        }

        private fun mimeOf(name: String): String {
            return when (name.substringAfterLast('.', "").lowercase()) {
                "html" -> "text/html"
                "js", "mjs" -> "text/javascript"
                "css" -> "text/css"
                "json", "webmanifest" -> "application/json"
                "svg" -> "image/svg+xml"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                "ico" -> "image/x-icon"
                "woff2" -> "font/woff2"
                "woff" -> "font/woff"
                "ttf" -> "font/ttf"
                "wasm" -> "application/wasm"
                "txt", "md" -> "text/plain"
                else -> "application/octet-stream"
            }
        }
    }
}

/** Builds the hash-route URL that hands the connection settings to zashboard. */
object PanelUrls {
    fun setupUrl(backend: Backend): String {
        val params = buildString {
            append("protocol=").append(encode(backend.protocol))
            append("&hostname=").append(encode(backend.host))
            append("&port=").append(backend.port)
            if (backend.secret.isNotBlank()) append("&secret=").append(encode(backend.secret))
            if (backend.secondaryPath.isNotBlank()) {
                append("&secondaryPath=").append(encode(backend.secondaryPath))
            }
        }
        return "${PanelAssets.INDEX_URL}#/setup?$params"
    }

    // The bundled panel is only reachable inside the app (virtual host), so "open in
    // browser" targets the backend's own web UI (mihomo `external-ui`), served at /ui/.
    fun uiUrl(backend: Backend): String = "${backend.baseUrl}/ui/"

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

/** Imperative handle over the live WebView so the Compose UI can drive it. */
class PanelController {
    internal var webView: WebView? = null

    /** Set by the UI; invoked with the backends found inside the panel. */
    var onPanelBackends: ((List<Backend>) -> Unit)? = null

    fun load(url: String) {
        webView?.loadUrl(url)
    }

    fun reload() {
        webView?.reload()
    }

    /** Asks the panel for its own backend list and reports it via [onPanelBackends]. */
    fun scanPanelBackends() {
        val view = webView ?: return
        if (!isPanelVisible(view)) return
        view.evaluateJavascript(PanelAssets.SCAN_BACKENDS_JS) { result ->
            val scan = PanelBackendsJson.parseScan(result)
            if (scan.ok) onPanelBackends?.invoke(scan.list)
        }
    }

    /** Writes [backends] into the panel's own list, then reloads so zashboard picks them up. */
    fun pushBackends(backends: List<Backend>) {
        val view = webView ?: return
        if (!isPanelVisible(view)) return
        val payload = Base64.encodeToString(
            PanelBackendsJson.encode(backends).toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        view.evaluateJavascript(PanelAssets.pushBackendsJs(payload)) { result ->
            val count = result?.trim()?.toIntOrNull()
            if (count != null && count >= 0) view.reload()
        }
    }

    private fun isPanelVisible(view: WebView): Boolean =
        view.url?.startsWith(PanelAssets.ORIGIN) == true

    fun goBack(): Boolean {
        val view = webView ?: return false
        return if (view.canGoBack()) {
            view.goBack()
            true
        } else {
            false
        }
    }

    fun openInBrowser(url: String) {
        webView?.context?.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
        )
    }
}

/** Parses the raw result of [PanelAssets.SCAN_BACKENDS_JS] into app [Backend]s. */
private object PanelBackendsJson {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    data class Dto(
        val protocol: String = "http",
        val host: String = "",
        val port: Int = 0,
        val secret: String = "",
        val path: String = "",
        val label: String = ""
    )

    @Serializable
    private data class Scan(val ok: Boolean = false, val list: List<Dto> = emptyList())

    data class ScanResult(val ok: Boolean, val list: List<Backend>)

    fun parseScan(evaluateResult: String?): ScanResult {
        val empty = ScanResult(false, emptyList())
        if (evaluateResult.isNullOrBlank() || evaluateResult == "null") return empty
        // evaluateJavascript hands the JS string back as a JSON-encoded string literal.
        val inner = runCatching { json.decodeFromString<String>(evaluateResult) }.getOrNull()
            ?: return empty
        val scan = runCatching { json.decodeFromString<Scan>(inner) }.getOrNull() ?: return empty
        return ScanResult(scan.ok, scan.list.mapNotNull { it.toBackend() })
    }

    fun encode(backends: List<Backend>): String = json.encodeToString(
        backends.map { Dto(it.protocol, it.host, it.port, it.secret, it.secondaryPath, it.name) }
    )

    private fun Dto.toBackend(): Backend? {
        if (host.isBlank() || port !in 1..65535) return null
        return Backend(
            name = label,
            protocol = protocol.ifBlank { "http" },
            host = host,
            port = port,
            secret = secret,
            secondaryPath = path
        )
    }
}
