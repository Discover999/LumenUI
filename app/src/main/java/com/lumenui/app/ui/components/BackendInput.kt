package com.lumenui.app.ui.components

import com.lumenui.app.data.Backend
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** What an address field can turn into: a bare host, `host:port`, or a full URL. */
internal data class ParsedAddress(
    val protocol: String? = null,
    val host: String,
    val port: Int? = null,
    val secondaryPath: String? = null
)

/**
 * Reads the address field the way people actually fill it: a bare host, `host:port`, or a
 * whole URL pasted from the backend's own dashboard. Full-width punctuation from Chinese
 * IMEs is folded to ASCII first, so `192.168.31.217：9090` becomes the intended address
 * instead of being silently stripped into `192.168.31.2179090`.
 */
internal object BackendInput {
    fun parse(raw: String): ParsedAddress? {
        val text = fold(raw).trim()
        if (text.isEmpty()) return null
        if (text.contains("://")) {
            val url = text.toHttpUrlOrNull() ?: return null
            return ParsedAddress(
                protocol = url.scheme,
                host = url.host,
                port = url.port,
                secondaryPath = url.encodedPath.takeUnless { it == "/" }
            )
        }
        val slash = text.indexOf('/')
        val authority = if (slash < 0) text else text.substring(0, slash)
        val path = if (slash < 0) null else text.substring(slash)
        if (authority.isEmpty()) return null
        val colon = authority.lastIndexOf(':')
        val portText = if (colon > 0 && authority.indexOf(':') == colon) {
            authority.substring(colon + 1)
        } else {
            ""
        }
        return if (portText.isNotEmpty() && portText.all(Char::isDigit)) {
            ParsedAddress(
                host = authority.substring(0, colon),
                port = portText.toIntOrNull(),
                secondaryPath = path
            )
        } else {
            ParsedAddress(host = authority, secondaryPath = path)
        }
    }

    /** True when the entry builds a URL the app can actually request (scheme, host and port). */
    fun isValid(backend: Backend): Boolean {
        if (backend.baseUrl.toHttpUrlOrNull() == null) return false
        // OkHttp accepts an all-digit-and-dot host as a DNS name, so the leftover of the old
        // "full-width colon stripped away" mangling (`192.168.31.2179090`) would pass as a
        // valid URL and only fail later at DNS resolution. Demand a real IPv4 in that case.
        return !backend.host.isNumericDotted() || backend.host.isIpv4()
    }

    private fun String.isNumericDotted(): Boolean =
        isNotEmpty() && all { it.isDigit() || it == '.' }

    private fun String.isIpv4(): Boolean {
        val parts = split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    /** Folds the full-width punctuation and digits a Chinese IME produces to ASCII. */
    fun fold(text: String): String = buildString(text.length) {
        for (c in text) append(fold(c))
    }

    private fun fold(c: Char): Char = when (c) {
        '：' -> ':'
        '．', '。' -> '.'
        '－' -> '-'
        '＿' -> '_'
        '／' -> '/'
        '\u3000' -> ' '
        in '０'..'９' -> '0' + (c - '０')
        in 'Ａ'..'Ｚ' -> 'A' + (c - 'Ａ')
        in 'ａ'..'ｚ' -> 'a' + (c - 'ａ')
        else -> c
    }
}
