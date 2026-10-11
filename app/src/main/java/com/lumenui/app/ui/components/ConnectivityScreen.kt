package com.lumenui.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Apple
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Bot
import com.composables.icons.lucide.Boxes
import com.composables.icons.lucide.Castle
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
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Route
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Tv
import com.composables.icons.lucide.Twitter
import com.composables.icons.lucide.Video
import com.composables.icons.lucide.Wallet
import com.composables.icons.lucide.Waves
import com.composables.icons.lucide.Youtube
import com.composables.icons.lucide.Zap
import com.lumenui.app.R
import com.lumenui.app.data.ConnectivityTargets
import com.lumenui.app.data.ProbeResult
import com.lumenui.app.data.ProbeStatus
import com.lumenui.app.data.ProbeTarget
import com.lumenui.app.ui.theme.LocalGlassColors
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

private val OkColor = Color(0xFF6BD08A)
private val RestrictedColor = Color(0xFFE5B567)
private val FailedColor = Color(0xFFE0685F)
private val MutedColor = Color(0xFF9BA9BE)

/**
 * Full-screen connectivity / region-unlock page. Nothing runs until the user starts it;
 * each result then lands on its own row.
 */
@Composable
fun ConnectivityScreen(
    targets: List<ProbeTarget>,
    running: Boolean,
    results: Map<String, ProbeResult>,
    onBack: () -> Unit,
    onRerun: () -> Unit,
    onSettings: () -> Unit
) {
    val glass = LocalGlassColors.current
    val total = targets.size

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(glass.backgroundTop, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(glass.backgroundTop)
            ) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(Lucide.ArrowLeft, "返回", onBack)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "连通性测试",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (running) {
                        Text(
                            text = "${results.size}/$total",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    GlassIconButton(Lucide.SlidersHorizontal, "测试项设置", onSettings)
                    if (running || results.isNotEmpty()) {
                        GlassIconButton(Lucide.RefreshCw, "重新测试", onRerun)
                    } else {
                        GlassIconButton(Lucide.Play, "开始测试", onRerun)
                    }
                }
                HorizontalDivider(color = glass.border)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                targets.groupBy { it.category }.forEach { (category, items) ->
                    Text(
                        text = category,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                    items.forEach { target ->
                        ResultRow(target, results[target.name], running)
                    }
                }
            }
            Text(
                text = "解锁状态按各平台探测结果与出口地区判定；结果反映手机当前网络路径，仅供参考。",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun ResultRow(target: ProbeTarget, result: ProbeResult?, running: Boolean) {
    val glass = LocalGlassColors.current
    val (brandIcon, accent) = visualPainter(target.name)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(glass.buttonBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                brandIcon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = accent
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = target.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (result == null) {
            if (running) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    strokeWidth = 1.5.dp
                )
            } else {
                Text("—", color = MutedColor, fontSize = 13.sp)
            }
        } else {
            result.region?.let { region ->
                val flag = remember(region) { flagVector(region) }
                if (flag != null) {
                    FlagBadge(flag)
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    text = region,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(9.dp))
            }
            if (result.latencyMs >= 0) {
                Text(
                    text = "${result.latencyMs}ms",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(6.dp))
            }
            StatusDot(result.status)
        }
    }
}

/** Solid status dot: green unlocked, amber limited/flagged, red blocked/failed. */
@Composable
private fun StatusDot(status: ProbeStatus) {
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(statusColor(status))
    )
}

private fun statusColor(status: ProbeStatus): Color = when (status) {
    ProbeStatus.UNLOCKED -> OkColor
    ProbeStatus.LIMITED, ProbeStatus.FLAGGED -> RestrictedColor
    ProbeStatus.BLOCKED, ProbeStatus.FAILED -> FailedColor
}

private data class PlatformVisual(val icon: ImageVector, val color: Color)

/** Real brand mark (generated drawable or simple-icons vector) when available. */
@Composable
private fun visualPainter(name: String): Pair<Painter, Color> {
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
