package com.lumenui.app.ui.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CircleDashed
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.SlidersHorizontal
import com.lumenui.app.data.IpReputation
import com.lumenui.app.data.LATENCY_SAMPLE_COUNT
import com.lumenui.app.data.ProbeResult
import com.lumenui.app.data.ProbeStatus
import com.lumenui.app.data.ProbeTarget
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.SystemBarAppearance
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise

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
    ms < 0 -> BadColor
    ms <= 300 -> OkColor
    ms <= 700 -> WarnColor
    else -> BadColor
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
                // Not tested yet: a faint dashed-circle badge plus a placeholder bar read as a
                // deliberate skeleton, aligned with the badge/label of a tested row.
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Lucide.CircleDashed,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
                Spacer(Modifier.width(7.dp))
                Box(
                    Modifier
                        .width(32.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                )
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
            .border(1.dp, BadColor.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Lucide.RefreshCw,
            contentDescription = "重试",
            modifier = Modifier.size(13.dp),
            tint = BadColor
        )
    }
}

/** Tiny pill tagging the egress IP as residential / mobile / datacenter / proxy. */
@Composable
private fun ReputationPill(rep: IpReputation) {
    val color = when {
        rep.proxy -> BadColor
        rep.hosting -> WarnColor
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

