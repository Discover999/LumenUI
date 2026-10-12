package com.lumenui.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
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
import com.lumenui.app.data.IpReputation
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

/** Shown in place of the region/latency when a probe could not be reached at all. */
private const val FailedProbeLabel = "连接失败"

/**
 * Full-screen connectivity / region-unlock page. Nothing runs until the user starts it;
 * each result then lands on its own row.
 */
@Composable
fun ConnectivityScreen(
    targets: List<ProbeTarget>,
    running: Boolean,
    results: Map<String, ProbeResult>,
    reputation: Map<String, IpReputation>,
    onBack: () -> Unit,
    onRerun: () -> Unit,
    onSettings: () -> Unit
) {
    val glass = LocalGlassColors.current
    val total = targets.size
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseTextStyle = LocalTextStyle.current
    val nodeStyles = remember(baseTextStyle) {
        NodeTextStyles(
            region = baseTextStyle.copy(fontSize = 12.sp, lineHeight = 14.sp),
            ip = baseTextStyle.copy(fontSize = 11.sp, lineHeight = 12.sp),
            pill = baseTextStyle.copy(fontSize = 10.sp, lineHeight = 12.sp)
        )
    }
    val infoBlockWidth = remember(results, measurer, density, nodeStyles) {
        infoBlockWidthFor(results, measurer, density, nodeStyles)
    }

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
                        ResultRow(target, results[target.name], running, reputation, infoBlockWidth)
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
private fun ResultRow(
    target: ProbeTarget,
    result: ProbeResult?,
    running: Boolean,
    reputation: Map<String, IpReputation>,
    infoBlockWidth: Dp
) {
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
        Row(
            Modifier.weight(1f),
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
            Spacer(Modifier.width(8.dp))
        }
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
            // Measured-width info block: its left edge (region flag / IP start) and its right
            // edge (status dot / pinned pill) are the same for every row, so flags, IPs, pills
            // and dots all line up across rows.
            Column(Modifier.width(infoBlockWidth), horizontalAlignment = Alignment.Start) {
                if (result.status == ProbeStatus.FAILED) {
                    // A failed probe has no region/latency: centre the label in the block while
                    // the status dot stays pinned right so it lines up with the other rows.
                    Box(Modifier.fillMaxWidth()) {
                        Text(
                            text = FailedProbeLabel,
                            color = FailedColor,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        StatusDot(result.status, Modifier.align(Alignment.CenterEnd))
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        result.region?.let { region ->
                            val flag = remember(region) { flagVector(region) }
                            if (flag != null) {
                                FlagBadge(flag)
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = region,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        // A FAILED probe only knows how long it waited out the timeout, which is
                        // not a latency — show nothing for it.
                        if (result.latencyMs >= 0) {
                            Text(
                                text = "${result.latencyMs}ms",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 14.sp
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        StatusDot(result.status)
                    }
                }
                result.ip?.let { ip ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // IP at the block's left, the pill pinned to its right edge so it shares the
                        // status dot's column. A long address (IPv6) is pre-shortened for display, so
                        // every row's IP width stays in the same ballpark.
                        Text(
                            text = displayIp(ip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
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
        }
    }
}

/**
 * Status dot: a solid core inside an outward-expanding ring, tinted by status
 * (green unlocked, amber limited/flagged, red blocked/failed).
 *
 * The ripple value is read inside the draw lambda, so each frame only invalidates the draw phase
 * rather than recomposing every row.
 */
@Composable
private fun StatusDot(status: ProbeStatus, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    val transition = rememberInfiniteTransition(label = "statusDot")
    val ripple = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )
    Canvas(modifier.size(10.dp)) {
        val core = size.minDimension / 2f * 0.55f
        val ring = core + (size.minDimension / 2f * 1.75f - core) * ripple.value
        drawCircle(
            color = color.copy(alpha = (1f - ripple.value) * 0.55f),
            radius = ring,
            style = Stroke(width = 1.25.dp.toPx())
        )
        drawCircle(color = color, radius = core)
    }
}

/** Floor for the info block width, used before any result has been measured. */
private val InfoBlockMinWidth = 96.dp

/** Longest IPv4 text (`xxx.xxx.xxx.xxx`), which is also the widest address the block allows. */
private const val Ipv4MaxChars = 15

/**
 * Display form of an egress address. An IPv4 fits as-is; a longer address (IPv6) is pre-shortened
 * to its first groups so it lands inside the IPv4-sized slot — keeping every row's IP a similar
 * width — instead of filling the slot and relying on an ellipsis.
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

/**
 * Text styles resolved from the theme, so measuring with them yields the same widths the rows
 * actually render. A bare `TextStyle` misses the theme's font/letter-spacing (Material's default
 * `bodyLarge` carries `letterSpacing = 0.5sp`), which made the measured block too narrow and
 * ellipsised the IP.
 */
private data class NodeTextStyles(
    val region: TextStyle,
    val ip: TextStyle,
    val pill: TextStyle
)

/**
 * Width of each row's info block = the widest line across the current results, so the region flag,
 * the IP and the status dot (and, via the pinned pill, the reputation tag) share a column while
 * the block still hugs its content. Re-measured as results stream in.
 */
private fun infoBlockWidthFor(
    results: Map<String, ProbeResult>,
    measurer: TextMeasurer,
    density: Density,
    styles: NodeTextStyles
): Dp {
    fun px(value: Dp) = with(density) { value.roundToPx() }
    fun textWidth(text: String, style: TextStyle) = measurer.measure(text, style).size.width

    val pillWidth = textWidth("住宅", styles.pill) + px(12.dp)
    // Size the block for at most a full IPv4 (`xxx.xxx.xxx.xxx`); a longer address (IPv6) is left
    // to ellipsise rather than stretching this block — and every other row — far wider.
    val ipWidthCap = textWidth("255.255.255.255", styles.ip)
    var widest = px(InfoBlockMinWidth)
    results.values.forEach { result ->
        val line1 = if (result.status == ProbeStatus.FAILED) {
            textWidth(FailedProbeLabel, styles.region) + px(10.dp)
        } else {
            val region = result.region
            (if (region != null) px(21.dp) + px(5.dp) + textWidth(region, styles.region) else 0) +
                (if (result.latencyMs >= 0) textWidth("${result.latencyMs}ms", styles.region) + px(6.dp) else 0) +
                px(10.dp)
        }
        val line2 = result.ip?.let {
            minOf(textWidth(displayIp(it), styles.ip), ipWidthCap) + px(8.dp) + pillWidth
        } ?: 0
        widest = maxOf(widest, line1, line2)
    }
    return with(density) { (widest + px(2.dp)).toDp() }
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
