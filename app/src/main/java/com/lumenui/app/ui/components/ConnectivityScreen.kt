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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise

/** Status-row height, in sp so it scales with the font scale like the text it holds. */
private val StatusRowHeight = 22.sp

/** Egress-IP row height, in sp. Together with [StatusRowHeight] it fixes the status+IP region. */
private val IpRowHeight = 15.sp

/** Vertical gap between the rows inside a card. */
private val RowGap = 4.dp

/** Latency chart: tallest bar, the stub a useful sample still draws, and an empty slot. */
private val ChartMax = 22.dp
private val ChartMin = 5.dp
private val ChartEmpty = 4.dp

/** The latency at which a bar fills the chart, matching the amber band's own threshold. */
private const val ChartFullMs = 700f

/** Order the summary line reads in, worst last. */
private val SummaryOrder = listOf("可用", "受限", "不可用")

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
    onRetry: (ProbeTarget) -> Unit,
    onOpenDetail: (ProbeTarget) -> Unit
) {
    val glass = LocalGlassColors.current
    val noise = rememberFrostNoise()
    val total = targets.size
    // Starting or re-running the whole batch is a real action, so it is confirmed by touch.
    val runProbes = hapticOnClick(HapticFeedbackType.Confirm, onRerun)

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
                    Column(Modifier.weight(1f)) {
                        // Both lines set an explicit line height: text otherwise inherits the
                        // body style's, which is far taller than these sizes and left a gap
                        // between them big enough to read as two separate blocks.
                        Text(
                            text = "连通性测试",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // The run's own progress stays on this bar's right edge, so the verdict
                        // counts take the title's second line only once a run has finished.
                        if (!running && results.isNotEmpty()) {
                            Spacer(Modifier.height(1.dp))
                            VerdictCounts(results)
                        }
                    }
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
                        GlassIconButton(Lucide.RefreshCw, "重新测试", runProbes)
                    } else {
                        GlassIconButton(Lucide.Play, "开始测试", runProbes)
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
                        // A label and nothing else: the run's progress and its verdict already have
                        // their own places on this page, and a per-group counter only restated them.
                        Text(
                            text = category,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                        )
                    }
                    items(items, key = { it.name }) { target ->
                        val active = running || target.name in retrying
                        PlatformCard(
                            target = target,
                            result = results[target.name],
                            // While a probe is in flight the streamed list is the truth: the
                            // fallback below would otherwise replay the failed run's samples as
                            // if they were this run's.
                            samples = if (active) {
                                samples[target.name].orEmpty()
                            } else {
                                samples[target.name] ?: results[target.name]?.samples.orEmpty()
                            },
                            active = active,
                            reputation = reputation,
                            onRetry = { onRetry(target) },
                            // Only a settled result has evidence to explain; a bare card has none.
                            onOpenDetail = { if (results[target.name] != null) onOpenDetail(target) }
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
    onRetry: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val glass = LocalGlassColors.current
    val (brandIcon, accent) = visualPainter(target.name)
    val shape = RoundedCornerShape(14.dp)
    // A settled unfavourable row that is being re-probed is still "in progress", so the card
    // works that out once and both the status row and the latency chart follow it — the two
    // gates drifting apart is exactly what once made a retry tap look inert.
    val retryable = result != null &&
        (result.status == ProbeStatus.FAILED || result.status == ProbeStatus.BLOCKED)
    val pending = result == null || (active && retryable)
    // The verdict tints the outline, so a failing card can be picked out without reading it. A
    // row that is being (re)probed takes the neutral border, because its verdict is no longer
    // what the card is saying.
    val outline = result
        ?.takeIf { !pending }
        ?.let { statusVisual(it.status, averageLatency(samples)).second.copy(alpha = 0.35f) }
        ?: glass.cardBorder

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, outline, shape)
            // A settled result opens its evidence page; an untested card has nothing to explain,
            // so it stays inert rather than opening an empty sheet.
            .clickable(enabled = result != null, onClick = onOpenDetail)
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

        // A hairline separates the platform's identity from what the probe found, the way the
        // detail page's cards separate their sections.
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(glass.cardBorder))
        Spacer(Modifier.height(9.dp))

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
                StatusRow(result, active, pending, avgLatency, onRetry)
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

            // Fixed slots that fill in as each sample arrives, so the chart grows one bar at a
            // time. A probe in flight fills them — a first run and a re-probe alike — while an
            // untested or settled-unfavourable card shows none: the slots still hold the row, so
            // every card in the grid keeps its height.
            val status = result?.status
            val showSlots = if (pending) {
                active && samples.isNotEmpty()
            } else {
                // Settled: an unfavourable verdict shows no chart at all, a favourable one holds
                // the row even before any samples have landed.
                status != ProbeStatus.FAILED && status != ProbeStatus.BLOCKED
            }
            Spacer(Modifier.height(5.dp))
            SampleChart(samples, showSlots)
        }
    }
}

/** One card's latency samples as a small bar chart, one bar per slot. */
@Composable
private fun SampleChart(samples: List<Long>, showSlots: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(ChartMax),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(LATENCY_SAMPLE_COUNT) { index ->
            if (showSlots) {
                SampleBar(samples.getOrNull(index))
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * One sample bar: its height is that round trip against a fixed 700 ms ceiling, its colour the
 * same fast / middling / slow band the rest of the page uses. The ceiling is absolute rather than
 * per-card so two cards compare by height as well as by colour; a failed sample is a short red
 * stub and a slot that has not landed yet a faint one.
 */
@Composable
private fun RowScope.SampleBar(ms: Long?) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(ms) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
    }
    val height = when {
        ms == null || ms < 0 -> ChartEmpty
        else -> ChartMin + (ChartMax - ChartMin) * (ms / ChartFullMs).coerceIn(0f, 1f)
    }
    Box(
        Modifier
            .weight(1f)
            .height(height)
            .graphicsLayer {
                scaleY = grow.value
                // Bars grow out of the baseline rather than out of their own centre.
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (ms == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
                } else {
                    sampleColor(ms)
                }
            )
    )
}

/**
 * The verdict counts, each in its own colour, on the page title's second line once a run has
 * finished. They are the colours the cards use, so the line reads as a summary of them rather
 * than as a second verdict.
 */
@Composable
private fun VerdictCounts(results: Map<String, ProbeResult>) {
    val pieces = remember(results) {
        results.values
            .map { statusVisual(it.status, null) }
            .groupBy { it.third }
            .map { (label, group) -> Triple(label, group.size, group.first().second) }
            .sortedBy {
                SummaryOrder.indexOf(it.first).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
            }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        pieces.forEachIndexed { index, (label, count, color) ->
            if (index > 0) {
                Text(
                    text = "·",
                    color = MutedColor,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 5.dp)
                )
            }
            Text(
                text = "$count $label",
                color = color,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** Latency-sample dot colour: fast green, middling amber, slow or failed red. */
private fun sampleColor(ms: Long): Color = when {
    ms < 0 -> BadColor
    ms <= 300 -> OkColor
    ms <= 700 -> WarnColor
    else -> BadColor
}

/**
 * The card's latency figure: banded on the same thresholds as the chart below it, and set a step
 * above the verdict label in weight and colour rather than in size — at 14 sp it read as too big
 * beside that label, so the size is the label's own and the differentiation is left to the colour.
 */
@Composable
private fun LatencyFigure(ms: Long) {
    Text(
        text = "${ms}ms",
        color = delayColor(ms),
        fontSize = 13.sp,
        lineHeight = StatusRowHeight,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
}

/** Mean of the successful samples, or null when none have landed (or all failed). */
internal fun averageLatency(samples: List<Long>): Long? {
    val ok = samples.filter { it >= 0 }
    return if (ok.isEmpty()) null else ok.sum() / ok.size
}

@Composable
private fun StatusRow(
    result: ProbeResult?,
    active: Boolean,
    pending: Boolean,
    avgLatency: Long?,
    onRetry: () -> Unit
) {
    val minHeight = with(LocalDensity.current) { StatusRowHeight.toDp() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A re-probe of a settled unfavourable row shows the same pending form as a first-time
        // probe, so the stale verdict and its retry button are both replaced by the spinner
        // rather than sitting next to it. `pending` is decided by the card, so this row and the
        // latency chart cannot disagree about whether the probe is in flight.
        if (pending) {
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
                LatencyFigure(avgLatency)
            }
            return@Row
        }
        // Reaching here means the card did not call this row pending, which it only does for a
        // settled result; a parameter cannot carry that smart cast, so take it explicitly.
        val settled = result ?: return@Row
        val ms = avgLatency ?: settled.latencyMs
        val (icon, color, label) = statusVisual(settled.status, ms)
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
        if (settled.status == ProbeStatus.FAILED || settled.status == ProbeStatus.BLOCKED) {
            // An active re-probe is handled by the pending branch above, so reaching here means
            // the verdict is settled and the row can offer another attempt. BLOCKED counts as
            // retryable because a 403 can be a passing rate-limit rather than a region verdict.
            // No latency is shown: the request may well have answered quickly, and printing
            // that number next to 不可用 reads as if the platform were responding normally.
            RetryButton(onRetry)
        } else if (ms >= 0) {
            LatencyFigure(ms)
        }
    }
}

/** Round single-item re-probe button shown on a failed card. */
@Composable
private fun RetryButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    // A single re-probe spends a real request, so it is confirmed by touch like the batch run.
    val retry = hapticOnClick(HapticFeedbackType.Confirm, onClick)
    Box(
        Modifier
            .size(22.dp)
            .clip(shape)
            .border(1.dp, BadColor.copy(alpha = 0.35f), shape)
            .clickable(onClick = retry),
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

/**
 * Pill tagging the egress IP as residential / mobile / datacenter / proxy. The grid uses the
 * compact form; the detail page passes [large] because a 10 sp pill reads as a speck beside the
 * 17 sp address it qualifies.
 */
@Composable
internal fun ReputationPill(rep: IpReputation, large: Boolean = false) {
    val color = when {
        rep.proxy -> BadColor
        rep.hosting -> WarnColor
        rep.mobile -> MutedColor
        else -> OkColor
    }
    val shape = RoundedCornerShape(percent = 50)
    Box(
        Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.16f))
            .then(
                if (large) Modifier.border(1.dp, color.copy(alpha = 0.32f), shape) else Modifier
            )
            .padding(
                horizontal = if (large) 10.dp else 6.dp,
                vertical = if (large) 3.dp else 1.dp
            )
    ) {
        Text(
            text = rep.label,
            color = color,
            fontSize = if (large) 12.sp else 10.sp,
            lineHeight = if (large) 15.sp else 12.sp
        )
    }
}

