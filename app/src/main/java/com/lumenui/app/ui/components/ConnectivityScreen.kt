package com.lumenui.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
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
import com.lumenui.app.data.IpReputation
import com.lumenui.app.data.LATENCY_SAMPLE_COUNT
import com.lumenui.app.data.ProbeResult
import com.lumenui.app.data.ProbeStatus
import com.lumenui.app.data.ProbeTarget
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.SystemBarAppearance
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise
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

/** A reachable target averaging at least this many ms is flagged amber instead of green. */
private const val SLOW_LATENCY_MS = 700L

/** Status-row height, in sp so it scales with the font scale like the text it holds. */
private val StatusRowHeight = 22.sp

/** Egress-IP row height, in sp. Together with [StatusRowHeight] it fixes the status+IP region. */
private val IpRowHeight = 15.sp

/** Vertical gap between the rows inside a card. */
private val RowGap = 4.dp

/** Diameter of one latency-sample dot. */
private val SampleDotSize = 6.dp

/**
 * Full-screen connectivity / region-unlock page, laid out as a two-column card grid.
 * Nothing runs until the user starts it; each result then lands on its own card.
 */
@Composable
fun ConnectivityScreen(
    targets: List<ProbeTarget>,
    running: Boolean,
    retrying: Set<String>,
    results: Map<String, ProbeResult>,
    samples: Map<String, List<Long>>,
    reputation: Map<String, IpReputation>,
    onBack: () -> Unit,
    onRerun: () -> Unit,
    onSettings: () -> Unit,
    onRetry: (ProbeTarget) -> Unit
) {
    // A dialog is its own window: drive its system bars from here so the status-bar icons
    // keep following the theme (the activity-level call does not reach this window).
    SystemBarAppearance()
    val glass = LocalGlassColors.current
    val noise = rememberFrostNoise()
    val total = targets.size

    Box(
        Modifier
            .fillMaxSize()
            .background(glass.pageBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .glassSurface(
                        glass = glass,
                        noise = noise,
                        bottomHairline = true,
                        topHighlight = false,
                        bottomScrim = false
                    )
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
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                targets.groupBy { it.category }.forEach { (category, items) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = category,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(items, key = { it.name }) { target ->
                        PlatformCard(
                            target = target,
                            result = results[target.name],
                            samples = samples[target.name] ?: results[target.name]?.samples.orEmpty(),
                            active = running || target.name in retrying,
                            reputation = reputation,
                            onRetry = { onRetry(target) }
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "解锁状态按各平台探测结果与出口地区判定；结果反映手机当前网络路径，仅供参考。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun PlatformCard(
    target: ProbeTarget,
    result: ProbeResult?,
    samples: List<Long>,
    active: Boolean,
    reputation: Map<String, IpReputation>,
    onRetry: () -> Unit
) {
    val glass = LocalGlassColors.current
    val (brandIcon, accent) = visualPainter(target.name)
    val shape = RoundedCornerShape(14.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, glass.cardBorder, shape)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(Modifier.width(8.dp))
            Text(
                text = target.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            result?.region?.let { region ->
                val flag = remember(region) { flagVector(region) }
                if (flag != null) {
                    FlagBadge(flag)
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = region,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(11.dp))

        val density = LocalDensity.current
        val statusRowHeight = with(density) { StatusRowHeight.toDp() }
        val ipRowHeight = with(density) { IpRowHeight.toDp() }
        val avgLatency = averageLatency(samples)

        Column(Modifier.fillMaxWidth()) {
            // Status + egress-IP region with a fixed height: a card that has an IP fills both
            // rows, while a card without one centres its single status row in the same space,
            // so every card in the grid stays the same size.
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = statusRowHeight + RowGap + ipRowHeight),
                verticalArrangement = Arrangement.Center
            ) {
                StatusRow(result, active, avgLatency, onRetry)
                result?.ip?.let { ip ->
                    Spacer(Modifier.height(RowGap))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = ipRowHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayIp(ip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = IpRowHeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        reputation[ip]?.let { rep ->
                            Spacer(Modifier.width(8.dp))
                            ReputationPill(rep)
                        }
                    }
                }
            }

            // Fixed slots that fill in as each sample arrives, so the dots "load" one by one.
            // A failed probe never samples, so its slots stay invisible but still hold the row.
            val showSlots = samples.isNotEmpty() ||
                (result != null && result.status != ProbeStatus.FAILED)
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(LATENCY_SAMPLE_COUNT) { index ->
                    if (showSlots) {
                        SampleDot(samples.getOrNull(index))
                    } else {
                        Spacer(Modifier.size(SampleDotSize))
                    }
                }
            }
        }
    }
}

/** One latency-sample dot. A null value is a not-yet-filled slot, shown as a faint placeholder. */
@Composable
private fun SampleDot(ms: Long?) {
    if (ms == null) {
        Box(
            Modifier
                .size(SampleDotSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f))
        )
        return
    }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier
            .size(SampleDotSize)
            .scale(0.4f + 0.6f * appear.value)
            .alpha(appear.value)
            .clip(CircleShape)
            .background(sampleColor(ms))
    )
}

/** Latency-sample dot colour: fast green, middling amber, slow or failed red. */
private fun sampleColor(ms: Long): Color = when {
    ms < 0 -> FailedColor
    ms <= 300 -> OkColor
    ms <= 700 -> RestrictedColor
    else -> FailedColor
}

/** Mean of the successful samples, or null when none have landed (or all failed). */
private fun averageLatency(samples: List<Long>): Long? {
    val ok = samples.filter { it >= 0 }
    return if (ok.isEmpty()) null else ok.sum() / ok.size
}

@Composable
private fun StatusRow(result: ProbeResult?, active: Boolean, avgLatency: Long?, onRetry: () -> Unit) {
    val minHeight = with(LocalDensity.current) { StatusRowHeight.toDp() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (result == null) {
            if (active) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    strokeWidth = 1.5.dp
                )
            } else {
                Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = StatusRowHeight)
            }
            // While the status is still pending, show the running sample average so it fills in
            // dot by dot alongside the latency row below.
            if (avgLatency != null) {
                Spacer(Modifier.weight(1f))
                Text("${avgLatency}ms", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            return@Row
        }
        val ms = avgLatency ?: result.latencyMs
        val (icon, color, label) = statusVisual(result.status, ms)
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = color)
        }
        Spacer(Modifier.width(7.dp))
        Text(label, color = color, fontSize = 13.sp, lineHeight = StatusRowHeight, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        if (result.status == ProbeStatus.FAILED) {
            RetryButton(onRetry)
        } else {
            if (ms >= 0) Text("${ms}ms", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

/** Round single-item re-probe button shown on a failed card. */
@Composable
private fun RetryButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier
            .size(22.dp)
            .clip(shape)
            .border(1.dp, FailedColor.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Lucide.RefreshCw,
            contentDescription = "重试",
            modifier = Modifier.size(13.dp),
            tint = FailedColor
        )
    }
}

/** Tiny pill tagging the egress IP as residential / mobile / datacenter / proxy. */
@Composable
private fun ReputationPill(rep: IpReputation) {
    val color = when {
        rep.proxy -> FailedColor
        rep.hosting -> RestrictedColor
        rep.mobile -> MutedColor
        else -> OkColor
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(text = rep.label, color = color, fontSize = 10.sp, lineHeight = 12.sp)
    }
}

private fun statusVisual(status: ProbeStatus, latencyMs: Long?): Triple<ImageVector, Color, String> = when (status) {
    // Reachable but slow reads as a "dash" circle in amber, distinct from a fast green check.
    ProbeStatus.UNLOCKED -> if (latencyMs != null && latencyMs >= SLOW_LATENCY_MS) {
        Triple(Lucide.CircleMinus, RestrictedColor, "可用")
    } else {
        Triple(Lucide.CircleCheck, OkColor, "可用")
    }
    ProbeStatus.LIMITED, ProbeStatus.FLAGGED -> Triple(Lucide.CircleAlert, RestrictedColor, "受限")
    ProbeStatus.BLOCKED, ProbeStatus.FAILED -> Triple(Lucide.CircleX, FailedColor, "不可用")
}

/** Longest IPv4 text (`xxx.xxx.xxx.xxx`), the widest address the IP line allows. */
private const val Ipv4MaxChars = 15

/**
 * Display form of an egress address: an IPv4 fits as-is; a longer address (IPv6) is pre-shortened
 * to its first groups instead of filling the line and relying on an ellipsis.
 */
private fun displayIp(ip: String): String {
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
