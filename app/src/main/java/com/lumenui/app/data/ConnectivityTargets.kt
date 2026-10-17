package com.lumenui.app.data

/**
 * Region-unlock checks, modelled on Bettbox's media-unlock checker.
 *
 * Most platforms sit behind Cloudflare, so a single request to `<domain>/cdn-cgi/trace`
 * yields the egress IP, region (`loc`), colo and WARP flag; comparing `loc` against the
 * platform's unsupported-region set decides unlocked vs blocked. Platforms with their own
 * signals (Netflix, Disney+, YouTube, Spotify, Gemini, GitHub, Reddit, TikTok, OpenAI) get a
 * dedicated routine, and a Cloudflare challenge is reported as its own status.
 */
object ConnectivityTargets {
    const val AI = "AI 服务"
    const val STREAMING = "流媒体解锁"
    const val COMMON = "常用 & 开发"
    const val SOCIAL = "社交"
    const val CHINA = "中国大陆"
    const val GAMING = "游戏"
    const val CRYPTO = "加密 / 支付"

    private val CLAUDE_UNSUPPORTED = setOf(
        "CN", "HK", "MO", "RU", "IR", "KP", "SY", "CU", "VE", "BY",
        "AF", "SS", "YE", "ZW", "MM", "SD", "SO", "CF",
        "VN", "TR"
    )

    private fun trace(
        name: String,
        category: String,
        domain: String,
        homepage: String,
        unsupported: Set<String> = emptySet()
    ) = ProbeTarget(
        name,
        category,
        ProbeKind.TRACE,
        domain = domain,
        homepage = homepage,
        unsupportedRegions = unsupported
    )

    private fun simple(name: String, category: String, url: String, homepage: String = url) =
        ProbeTarget(name, category, ProbeKind.SIMPLE, url = url, homepage = homepage)

    val all: List<ProbeTarget> = listOf(
        // AI
        ProbeTarget("OpenAI", AI, ProbeKind.OPENAI, homepage = "https://openai.com/"),
        trace("Claude", AI, "api.anthropic.com", "https://claude.ai/", CLAUDE_UNSUPPORTED),
        ProbeTarget("Gemini", AI, ProbeKind.GEMINI, homepage = "https://gemini.google.com/"),
        trace("Grok", AI, "api.x.ai", "https://grok.com/"),
        trace("Perplexity", AI, "www.perplexity.ai", "https://www.perplexity.ai/"),
        trace("OpenRouter", AI, "openrouter.ai", "https://openrouter.ai/"),
        trace("Poe", AI, "poe.com", "https://poe.com/"),
        trace("Suno", AI, "suno.com", "https://suno.com/"),

        // Streaming
        ProbeTarget("Netflix", STREAMING, ProbeKind.NETFLIX, homepage = "https://www.netflix.com/"),
        ProbeTarget("Disney+", STREAMING, ProbeKind.DISNEY, homepage = "https://www.disneyplus.com/"),
        ProbeTarget("YouTube", STREAMING, ProbeKind.YOUTUBE, homepage = "https://www.youtube.com/"),
        ProbeTarget("Spotify", STREAMING, ProbeKind.SPOTIFY, homepage = "https://www.spotify.com/"),
        ProbeTarget("TikTok", STREAMING, ProbeKind.TIKTOK, homepage = "https://www.tiktok.com/"),
        trace("Crunchyroll", STREAMING, "crunchyroll.com", "https://www.crunchyroll.com/"),
        ProbeTarget("HBO Max", STREAMING, ProbeKind.HBO, homepage = "https://www.max.com/"),
        ProbeTarget(
            "Twitch", STREAMING, ProbeKind.PING,
            url = "https://www.twitch.tv/", homepage = "https://www.twitch.tv/"
        ),
        ProbeTarget(
            "Apple TV+", STREAMING, ProbeKind.PING,
            url = "https://tv.apple.com/", homepage = "https://tv.apple.com/"
        ),

        // Common & developer
        simple("Google", COMMON, "https://www.google.com/generate_204", "https://www.google.com/"),
        ProbeTarget("GitHub", COMMON, ProbeKind.GITHUB, homepage = "https://github.com/"),
        trace("GitLab", COMMON, "gitlab.com", "https://gitlab.com/"),
        simple("Wikipedia", COMMON, "https://www.wikipedia.org/"),
        trace("npm", COMMON, "registry.npmjs.org", "https://www.npmjs.com/"),
        trace("cdnjs", COMMON, "cdnjs.cloudflare.com", "https://cdnjs.com/"),
        trace("unpkg", COMMON, "unpkg.com", "https://unpkg.com/"),
        trace("Node.js", COMMON, "nodejs.org", "https://nodejs.org/"),
        simple("Apple", COMMON, "https://www.apple.com/"),

        // Social
        trace("X (Twitter)", SOCIAL, "x.com", "https://x.com/"),
        trace("Discord", SOCIAL, "gateway.discord.gg", "https://discord.com/"),
        ProbeTarget("Reddit", SOCIAL, ProbeKind.REDDIT, homepage = "https://www.reddit.com/"),
        simple("Telegram", SOCIAL, "https://telegram.org/"),
        trace("Medium", SOCIAL, "medium.com", "https://medium.com/"),
        trace("Stack Overflow", SOCIAL, "stackoverflow.com", "https://stackoverflow.com/"),
        trace("Quora", SOCIAL, "www.quora.com", "https://www.quora.com/"),
        trace("V2EX", SOCIAL, "v2ex.com", "https://www.v2ex.com/"),

        // Mainland China
        simple("百度", CHINA, "https://www.baidu.com/"),
        simple("Bilibili", CHINA, "https://www.bilibili.com/"),
        simple("腾讯", CHINA, "https://www.qq.com/"),
        simple("网易", CHINA, "https://www.163.com/"),

        // Gaming
        simple("Steam", GAMING, "https://store.steampowered.com/"),
        trace("Epic Games", GAMING, "store.epicgames.com", "https://store.epicgames.com/"),
        trace("Ubisoft", GAMING, "store.ubisoft.com", "https://www.ubisoft.com/"),
        trace("Humble Bundle", GAMING, "humblebundle.com", "https://www.humblebundle.com/"),
        ProbeTarget(
            "PlayStation", GAMING, ProbeKind.PING,
            url = "https://www.playstation.com/", homepage = "https://www.playstation.com/"
        ),
        ProbeTarget(
            "Battle.net", GAMING, ProbeKind.PING,
            url = "https://www.battle.net/", homepage = "https://www.battle.net/"
        ),

        // Crypto / payments
        trace("Coinbase", CRYPTO, "www.coinbase.com", "https://www.coinbase.com/"),
        trace("OKX", CRYPTO, "www.okx.com", "https://www.okx.com/"),
        trace("Kraken", CRYPTO, "www.kraken.com", "https://www.kraken.com/"),
        simple("PayPal", CRYPTO, "https://www.paypal.com/")
    )

    /** Tested by default; everything else is opt-in from the settings sheet. */
    val defaultSelected: Set<String> = setOf(
        "OpenAI", "Claude", "Gemini", "Grok",
        "Netflix", "Disney+", "YouTube", "Spotify",
        "Google", "GitHub", "Telegram",
        "X (Twitter)", "Discord",
        "百度", "Bilibili", "腾讯", "网易"
    )
}
