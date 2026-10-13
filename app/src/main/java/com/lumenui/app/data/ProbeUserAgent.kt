package com.lumenui.app.data

/**
 * Browser-like User-Agent for the probe requests. Some edges (Cloudflare in particular)
 * answer a non-browser agent differently, so the probes present themselves as Chrome.
 */
internal const val PROBE_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
