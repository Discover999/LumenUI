package com.lumenui.app.web

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.webkit.WebViewAssetLoader
import com.lumenui.app.data.Backend
import com.lumenui.app.util.AppLog
import java.io.IOException
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
     * Reads zashboard's own backend list and active selection out of its localStorage.
     * zashboard keeps the saved backends under `setup/api-list` (a JSON array) and the
     * active one under `setup/active-uuid`; these live in the WebView origin, separate
     * from the app's SharedPreferences list. Returns `{ok, list, active}`, where `list`
     * and `active` are normalised entries (port coerced to a number) and `active` is the
     * entry `setup/active-uuid` points at (or null). `ok` is false if the read failed, so
     * callers can tell "no backends" apart from "could not read".
     */
    val SCAN_BACKENDS_JS = """
        (function () {
          try {
            var norm = function (b) {
              var port = parseInt(b.port, 10);
              if (!b.host || !isFinite(port) || port <= 0) return null;
              return {
                protocol: String(b.protocol || 'http'),
                host: String(b.host),
                port: port,
                secret: String(b.password || b.secret || ''),
                path: String(b.secondaryPath || ''),
                label: String(b.label || '')
              };
            };
            var raw = localStorage.getItem('setup/api-list');
            var arr = raw ? JSON.parse(raw) : [];
            var out = [];
            for (var i = 0; i < arr.length; i++) {
              var n = norm(arr[i] || {});
              if (n) out.push(n);
            }
            var activeUuid = localStorage.getItem('setup/active-uuid') || '';
            var active = null;
            for (var k = 0; k < arr.length; k++) {
              if (String((arr[k] || {}).uuid || '') === activeUuid) { active = norm(arr[k]); break; }
            }
            return JSON.stringify({ ok: true, list: out, active: active });
          } catch (e) {
            return JSON.stringify({ ok: false, list: [], active: null });
          }
        })()
    """.trimIndent()

    /**
     * Console sentinel emitted by [WATCH_ACTIVE_BACKEND_JS] when the panel's active backend
     * changes; PanelWebView turns it back into a scan.
     */
    const val ACTIVE_WATCH_SENTINEL = "__lumen_active_backend_changed__"

    /**
     * Taps `localStorage` so the app learns when zashboard switches the active backend *in
     * place*. zashboard changes `setup/active-uuid` without navigating or reloading, so no
     * page/history event fires; the tap emits [ACTIVE_WATCH_SENTINEL] on the console instead.
     * Idempotent (guarded by a window flag), so re-injecting after a reload is harmless.
     */
    val WATCH_ACTIVE_BACKEND_JS = """
        (function () {
          try {
            if (window.__lumenActiveWatch) return;
            window.__lumenActiveWatch = true;
            var orig = Storage.prototype.setItem;
            Storage.prototype.setItem = function (key, value) {
              var changed = false;
              try { changed = key === 'setup/active-uuid' && this.getItem(key) !== String(value); } catch (e) {}
              var r = orig.apply(this, arguments);
              if (changed) { try { console.log('$ACTIVE_WATCH_SENTINEL'); } catch (e) {} }
              return r;
            };
          } catch (e) {}
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
                secondaryPath: b.path || '',
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

    /**
     * Upserts a single backend — [payloadBase64] is the base64 of a JSON object
     * `{protocol,host,port,secret,path,label}` — into the panel's `setup/api-list`, then
     * points `setup/active-uuid` at it. zashboard reads both keys on boot, so callers
     * reload afterwards. Keeping the secret here instead of in a `#/setup?secret=...`
     * hash is the point: it never lands in a WebView URL or history entry.
     */
    fun activateBackendJs(payloadBase64: String): String = """
        (function () {
          try {
            var bin = atob('$payloadBase64');
            var bytes = new Uint8Array(bin.length);
            for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
            var b = JSON.parse(new TextDecoder('utf-8').decode(bytes));
            var key = String(b.host || '').toLowerCase() + ':' + parseInt(b.port, 10);
            var raw = localStorage.getItem('setup/api-list');
            var list = raw ? JSON.parse(raw) : [];
            var found = null;
            for (var j = 0; j < list.length; j++) {
              if ((String(list[j].host || '').toLowerCase() + ':' + parseInt(list[j].port, 10)) === key) { found = list[j]; break; }
            }
            if (found) {
              found.type = 'clash';
              found.protocol = b.protocol || found.protocol || 'http';
              found.secondaryPath = b.path || '';
              found.password = b.secret || '';
              found.label = b.label || found.label || '';
            } else {
              found = {
                uuid: (crypto.randomUUID ? crypto.randomUUID() : ('' + Date.now() + Math.random())),
                type: 'clash',
                protocol: b.protocol || 'http',
                secondaryPath: b.path || '',
                host: String(b.host),
                port: parseInt(b.port, 10),
                password: b.secret || '',
                label: b.label || '',
                disableUpgradeCore: false,
                disableTunMode: false
              };
              list.push(found);
            }
            localStorage.setItem('setup/api-list', JSON.stringify(list));
            localStorage.setItem('setup/active-uuid', found.uuid);
            return 'ok';
          } catch (e) {
            return 'err';
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

/** Builds URLs the app hands to the WebView or an external browser. */
object PanelUrls {
    // The bundled panel is only reachable inside the app (virtual host), so "open in
    // browser" targets the backend's own web UI (mihomo `external-ui`), served at /ui/.
    fun uiUrl(backend: Backend): String = "${backend.baseUrl}/ui/"
}

/** Imperative handle over the live WebView so the Compose UI can drive it. */
class PanelController {
    internal var webView: WebView? = null

    /** Set by the UI; invoked with the panel's backend list and the one it has active. */
    var onPanelBackends: ((List<Backend>, Backend?) -> Unit)? = null

    /** Set by the UI; invoked once the panel page has finished loading. */
    var onPanelReady: (() -> Unit)? = null

    /** Identity of the backend already handed to the panel, so it is applied at most once. */
    private var connectedKey: String? = null

    fun load(url: String) {
        webView?.loadUrl(url)
    }

    fun reload() {
        webView?.reload()
    }

    /** Called by the WebView when the panel page finishes loading. */
    fun notifyPanelReady() {
        onPanelReady?.invoke()
    }

    /**
     * Detaches [view] when the WebView is released. Forgets the applied backend so that a
     * WebView created later (e.g. after the app fell back to its own setup screen) is handed
     * the active backend again instead of being skipped.
     */
    fun detach(view: WebView) {
        if (webView === view) {
            webView = null
            connectedKey = null
        }
    }

    /**
     * Hands [backend] to the panel by writing it into the panel's localStorage and making
     * it active, then reloading so zashboard connects to it. The secret travels through
     * injected JS only — never through a WebView URL. A no-op when the panel is not on its
     * own page yet (it will be applied from [onPanelReady]) or when already applied.
     */
    fun connect(backend: Backend) {
        val view = webView ?: return
        if (!isPanelVisible(view)) return
        val key = backend.connectKey()
        if (key == connectedKey) return
        connectedKey = key
        val payload = Base64.encodeToString(
            PanelBackendsJson.encodeOne(backend).toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        view.evaluateJavascript(PanelAssets.activateBackendJs(payload)) { result ->
            // Navigate to the panel home (no hash) rather than reload(): zashboard may have
            // bounced to its "#/setup" route while it had no backend, and reloading that hash
            // would leave it stuck on the setup page even though a backend is now stored.
            if (result?.contains("ok") == true) view.loadUrl(PanelAssets.INDEX_URL)
        }
    }

    /** Installs the localStorage tap that reports in-panel backend switches. */
    fun watchActiveBackend() {
        val view = webView ?: return
        if (!isPanelVisible(view)) return
        view.evaluateJavascript(PanelAssets.WATCH_ACTIVE_BACKEND_JS, null)
    }

    /** Asks the panel for its own backend list and active selection; reports via [onPanelBackends]. */
    fun scanPanelBackends() {
        val view = webView ?: return
        if (!isPanelVisible(view)) return
        view.evaluateJavascript(PanelAssets.SCAN_BACKENDS_JS) { result ->
            val scan = PanelBackendsJson.parseScan(result)
            if (scan.ok) {
                // The panel is already showing this backend; remember it so a follow-up
                // connect() for the same one is a no-op (no re-drive, no extra reload).
                scan.active?.let { connectedKey = it.connectKey() }
                onPanelBackends?.invoke(scan.list, scan.active)
            }
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
            android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri())
        )
    }
}

/** Parses the raw result of [PanelAssets.SCAN_BACKENDS_JS] into app [Backend]s. */
internal object PanelBackendsJson {
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
    private data class Scan(
        val ok: Boolean = false,
        val list: List<Dto> = emptyList(),
        val active: Dto? = null
    )

    data class ScanResult(val ok: Boolean, val list: List<Backend>, val active: Backend?)

    fun parseScan(evaluateResult: String?): ScanResult {
        val empty = ScanResult(false, emptyList(), null)
        if (evaluateResult.isNullOrBlank() || evaluateResult == "null") return empty
        // evaluateJavascript hands the JS string back as a JSON-encoded string literal.
        val inner = runCatching { json.decodeFromString<String>(evaluateResult) }.getOrNull()
        if (inner == null) {
            AppLog.w("Panel scan returned a non-string payload")
            return empty
        }
        val scan = runCatching { json.decodeFromString<Scan>(inner) }.getOrNull()
        if (scan == null) {
            AppLog.w("Panel scan payload could not be parsed")
            return empty
        }
        return ScanResult(scan.ok, scan.list.mapNotNull { it.toBackend() }, scan.active?.toBackend())
    }

    fun encode(backends: List<Backend>): String = json.encodeToString(
        backends.map { Dto(it.protocol, it.host, it.port, it.secret, it.secondaryPath, it.name) }
    )

    fun encodeOne(backend: Backend): String = json.encodeToString(
        Dto(backend.protocol, backend.host, backend.port, backend.secret, backend.secondaryPath, backend.name)
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

/** In-memory identity used to avoid re-applying the same backend to the panel twice. */
private fun Backend.connectKey(): String = "$protocol://$host:$port|$secondaryPath|$secret"
