package com.lumenui.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.Apple
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Bot
import com.composables.icons.lucide.Boxes
import com.composables.icons.lucide.Castle
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleMinus
import com.composables.icons.lucide.CircleX
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.Compass
import com.composables.icons.lucide.CreditCard
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Gamepad
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.Gem
import com.composables.icons.lucide.Gift
import com.composables.icons.lucide.Github
import com.composables.icons.lucide.Gitlab
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Hexagon
import com.composables.icons.lucide.Joystick
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquare
import com.composables.icons.lucide.MessagesSquare
import com.composables.icons.lucide.MonitorPlay
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Music2
import com.composables.icons.lucide.Newspaper
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.PawPrint
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.Route
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Tv
import com.composables.icons.lucide.Twitter
import com.composables.icons.lucide.Video
import com.composables.icons.lucide.Wallet
import com.composables.icons.lucide.Waves
import com.composables.icons.lucide.Youtube
import com.composables.icons.lucide.Zap
import com.lumenui.app.R
import com.lumenui.app.data.ProbeStatus
import compose.icons.SimpleIcons
import compose.icons.simpleicons.Apple
import compose.icons.simpleicons.Baidu
import compose.icons.simpleicons.Bilibili
import compose.icons.simpleicons.Coinbase
import compose.icons.simpleicons.Crunchyroll
import compose.icons.simpleicons.Discord
import compose.icons.simpleicons.Epicgames
import compose.icons.simpleicons.Github
import compose.icons.simpleicons.Gitlab
import compose.icons.simpleicons.Google
import compose.icons.simpleicons.Humblebundle
import compose.icons.simpleicons.Medium
import compose.icons.simpleicons.Netflix
import compose.icons.simpleicons.NodeDotJs
import compose.icons.simpleicons.Npm
import compose.icons.simpleicons.Openai
import compose.icons.simpleicons.Paypal
import compose.icons.simpleicons.Quora
import compose.icons.simpleicons.Reddit
import compose.icons.simpleicons.Spotify
import compose.icons.simpleicons.Stackoverflow
import compose.icons.simpleicons.Steam
import compose.icons.simpleicons.Telegram
import compose.icons.simpleicons.Tencentqq
import compose.icons.simpleicons.Tiktok
import compose.icons.simpleicons.Ubisoft
import compose.icons.simpleicons.Wikipedia
import compose.icons.simpleicons.Youtube

/** A reachable target averaging at least this many ms is flagged amber instead of green. */
private const val SLOW_LATENCY_MS = 700L

internal fun statusVisual(status: ProbeStatus, latencyMs: Long?): Triple<ImageVector, Color, String> = when (status) {
    // Reachable but slow reads as a "dash" circle in amber, distinct from a fast green check.
    ProbeStatus.UNLOCKED -> if (latencyMs != null && latencyMs >= SLOW_LATENCY_MS) {
        Triple(Lucide.CircleMinus, WarnColor, "可用")
    } else {
        Triple(Lucide.CircleCheck, OkColor, "可用")
    }
    ProbeStatus.LIMITED, ProbeStatus.FLAGGED -> Triple(Lucide.CircleAlert, WarnColor, "受限")
    ProbeStatus.BLOCKED, ProbeStatus.FAILED -> Triple(Lucide.CircleX, BadColor, "不可用")
}

/** Longest IPv4 text (`xxx.xxx.xxx.xxx`), the widest address the IP line allows. */
private const val Ipv4MaxChars = 15

/**
 * Display form of an egress address: an IPv4 fits as-is; a longer address (IPv6) is pre-shortened
 * to its first groups instead of filling the line and relying on an ellipsis.
 */
internal fun displayIp(ip: String): String {
    if (ip.length <= Ipv4MaxChars) return ip
    val groups = ip.split(':')
    return if (groups.size >= 3) {
        groups.take(3).joinToString(":") + "…"
    } else {
        ip.take(Ipv4MaxChars - 1) + "…"
    }
}

private data class PlatformVisual(val icon: ImageVector, val color: Color)

/** Real brand mark (generated drawable or simple-icons vector) when available. */
@Composable
internal fun visualPainter(name: String): Pair<Painter, Color> {
    val fallback = fallbackVisual(name)
    val brand: Painter? = brandRes(name)?.let { painterResource(it) }
        ?: brandVector(name)?.let { rememberVectorPainter(it) }
    return (brand ?: rememberVectorPainter(fallback.icon)) to fallback.color
}

/** Brand marks generated from simple-icons' own SVGs (the Compose port lacks these). */
@DrawableRes
private fun brandRes(name: String): Int? = when (name) {
    "Claude" -> R.drawable.ic_brand_claude
    "Gemini" -> R.drawable.ic_brand_gemini
    "Grok" -> R.drawable.ic_brand_grok
    "Disney+" -> R.drawable.ic_brand_disney
    "Perplexity" -> R.drawable.ic_brand_perplexity
    "OpenRouter" -> R.drawable.ic_brand_openrouter
    "Poe" -> R.drawable.ic_brand_poe
    "Suno" -> R.drawable.ic_brand_suno
    "X (Twitter)" -> R.drawable.ic_brand_x
    "unpkg" -> R.drawable.ic_brand_unpkg
    "V2EX" -> R.drawable.ic_brand_v2ex
    "网易" -> R.drawable.ic_brand_netease
    "OKX" -> R.drawable.ic_brand_okx
    "Kraken" -> R.drawable.ic_brand_kraken
    else -> null
}

private fun brandVector(name: String): ImageVector? = when (name) {
    "OpenAI" -> SimpleIcons.Openai
    "Netflix" -> SimpleIcons.Netflix
    "YouTube" -> SimpleIcons.Youtube
    "Spotify" -> SimpleIcons.Spotify
    "TikTok" -> SimpleIcons.Tiktok
    "Crunchyroll" -> SimpleIcons.Crunchyroll
    "Google" -> SimpleIcons.Google
    "GitHub" -> SimpleIcons.Github
    "GitLab" -> SimpleIcons.Gitlab
    "Wikipedia" -> SimpleIcons.Wikipedia
    "npm" -> SimpleIcons.Npm
    "Node.js" -> SimpleIcons.NodeDotJs
    "Apple" -> SimpleIcons.Apple
    "Discord" -> SimpleIcons.Discord
    "Reddit" -> SimpleIcons.Reddit
    "Telegram" -> SimpleIcons.Telegram
    "Medium" -> SimpleIcons.Medium
    "Stack Overflow" -> SimpleIcons.Stackoverflow
    "Quora" -> SimpleIcons.Quora
    "百度" -> SimpleIcons.Baidu
    "Bilibili" -> SimpleIcons.Bilibili
    "腾讯" -> SimpleIcons.Tencentqq
    "Steam" -> SimpleIcons.Steam
    "Epic Games" -> SimpleIcons.Epicgames
    "Ubisoft" -> SimpleIcons.Ubisoft
    "Humble Bundle" -> SimpleIcons.Humblebundle
    "Coinbase" -> SimpleIcons.Coinbase
    "PayPal" -> SimpleIcons.Paypal
    else -> null
}

private fun fallbackVisual(name: String): PlatformVisual = when (name) {
    "OpenAI" -> PlatformVisual(Lucide.Bot, Color(0xFF10A37F))
    "Claude" -> PlatformVisual(Lucide.Sparkles, Color(0xFFD97757))
    "Gemini" -> PlatformVisual(Lucide.Gem, Color(0xFF4285F4))
    "Grok" -> PlatformVisual(Lucide.Zap, Color(0xFFB4B4B4))
    "Perplexity" -> PlatformVisual(Lucide.Compass, Color(0xFF20B8CD))
    "OpenRouter" -> PlatformVisual(Lucide.Route, Color(0xFF8E8E93))
    "Poe" -> PlatformVisual(Lucide.MessagesSquare, Color(0xFF8B5CF6))
    "Suno" -> PlatformVisual(Lucide.Music, Color(0xFFE11D48))
    "Netflix" -> PlatformVisual(Lucide.Film, Color(0xFFE50914))
    "Disney+" -> PlatformVisual(Lucide.Castle, Color(0xFF113CCF))
    "YouTube" -> PlatformVisual(Lucide.Youtube, Color(0xFFFF0000))
    "Spotify" -> PlatformVisual(Lucide.Headphones, Color(0xFF1DB954))
    "TikTok" -> PlatformVisual(Lucide.Video, Color(0xFF25F4EE))
    "Crunchyroll" -> PlatformVisual(Lucide.Tv, Color(0xFFF47521))
    "Google" -> PlatformVisual(Lucide.Search, Color(0xFF4285F4))
    "GitHub" -> PlatformVisual(Lucide.Github, Color(0xFFE6E6E6))
    "GitLab" -> PlatformVisual(Lucide.Gitlab, Color(0xFFFC6D26))
    "Wikipedia" -> PlatformVisual(Lucide.BookOpen, Color(0xFFB0B0B0))
    "npm" -> PlatformVisual(Lucide.Package, Color(0xFFCB3837))
    "cdnjs" -> PlatformVisual(Lucide.Cloud, Color(0xFFF38020))
    "unpkg" -> PlatformVisual(Lucide.Boxes, Color(0xFF4B8BF5))
    "Node.js" -> PlatformVisual(Lucide.Hexagon, Color(0xFF5FA04E))
    "Apple" -> PlatformVisual(Lucide.Apple, Color(0xFFD0D0D0))
    "X (Twitter)" -> PlatformVisual(Lucide.Twitter, Color(0xFFB0B0B0))
    "Discord" -> PlatformVisual(Lucide.MessagesSquare, Color(0xFF5865F2))
    "Reddit" -> PlatformVisual(Lucide.MessagesSquare, Color(0xFFFF4500))
    "Telegram" -> PlatformVisual(Lucide.Send, Color(0xFF26A5E4))
    "Medium" -> PlatformVisual(Lucide.PenLine, Color(0xFFB0B0B0))
    "Stack Overflow" -> PlatformVisual(Lucide.Layers, Color(0xFFF48024))
    "Quora" -> PlatformVisual(Lucide.MessageSquare, Color(0xFFB92B27))
    "V2EX" -> PlatformVisual(Lucide.Music2, Color(0xFF778087))
    "百度" -> PlatformVisual(Lucide.PawPrint, Color(0xFF2932E1))
    "Bilibili" -> PlatformVisual(Lucide.MonitorPlay, Color(0xFFFB7299))
    "腾讯" -> PlatformVisual(Lucide.MessagesSquare, Color(0xFF12B7F5))
    "网易" -> PlatformVisual(Lucide.Newspaper, Color(0xFFD93B3B))
    "Steam" -> PlatformVisual(Lucide.Gamepad2, Color(0xFF66C0F4))
    "Epic Games" -> PlatformVisual(Lucide.Gamepad, Color(0xFFB0B0B0))
    "Ubisoft" -> PlatformVisual(Lucide.Joystick, Color(0xFF0070FF))
    "Humble Bundle" -> PlatformVisual(Lucide.Gift, Color(0xFFCC2929))
    "Coinbase" -> PlatformVisual(Lucide.Wallet, Color(0xFF0052FF))
    "OKX" -> PlatformVisual(Lucide.Wallet, Color(0xFFB0B0B0))
    "Kraken" -> PlatformVisual(Lucide.Waves, Color(0xFF5741D9))
    "PayPal" -> PlatformVisual(Lucide.CreditCard, Color(0xFF003087))
    else -> PlatformVisual(Lucide.Globe, MutedColor)
}
