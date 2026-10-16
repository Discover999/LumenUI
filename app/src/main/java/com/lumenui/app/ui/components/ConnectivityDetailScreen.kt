package com.lumenui.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.lumenui.app.data.IpReputation
import com.lumenui.app.data.ProbeResult
import com.lumenui.app.data.ProbeStatus
import com.lumenui.app.data.ProbeTarget
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise

/** Tallest bar in the latency chart; the shortest answered sample still shows a visible stub. */
private val BarMax = 34.dp
private val BarMin = 6.dp
private val BarFailed = 4.dp

/**
 * One platform's evidence page: which verdict the probe reached, what decided it, and the raw
 * observations behind it. Read-only — it describes a finished probe and never re-runs or
 * reinterprets one.
 *
 * The page leads with the verdict and its reasoning, then shows the two things a reader came for
 * as figures rather than sentences: the egress (flag, address, reputation) and the latency
 * samples as a small chart. Only the leftovers stay as labelled text rows.
 */
@Composable
fun ConnectivityDetailScreen(
    target: ProbeTarget,
    result: ProbeResult,
    reputation: IpReputation?,
    onBack: () -> Unit
) {
    val glass = LocalGlassColors.current
    val noise = rememberFrostNoise()
    val (icon, accent, label) = statusVisual(result.status, averageLatency(result.samples))
    val lines = remember(target, result) { evidenceFor(target, result) }
    val sections = remember(lines) { sectionsOf(lines) }
    val (brand, brandAccent) = visualPainter(target.name)

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
                    // The platform's mark on its own tinted tile, and tinted with the brand
                    // colour: the simple-icons vectors are flat paths, so drawn untinted they
                    // came through as near-black on the dark bar and were hard to make out.
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brandAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            brand,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = brandAccent
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = target.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                VerdictHero(
                    icon = icon,
                    accent = accent,
                    label = label,
                    probeKind = lines.value(LABEL_PROBE_KIND),
                    criterion = lines.value(LABEL_CRITERION)
                )
                if (result.ip != null || result.region != null || reputation != null) {
                    Spacer(Modifier.height(12.dp))
                    ExitCard(result, reputation)
                }
                if (result.samples.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LatencyCard(result)
                }
                sections.forEach { section ->
                    Spacer(Modifier.height(12.dp))
                    SectionCard(section)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "以上为本次探测的原始观察值；判定依据与探测方式见页首说明。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

private fun List<EvidenceLine>.value(label: String): String? =
    firstOrNull { it.label == label }?.value?.takeIf { it.isNotBlank() }

/** The page's focal card: the verdict, then how it was reached. */
@Composable
private fun VerdictHero(
    icon: ImageVector,
    accent: Color,
    label: String,
    probeKind: String?,
    criterion: String?
) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass.cardBg)
            // A tint of the verdict colour behind the card, so the state reads before any text.
            .background(
                Brush.verticalGradient(listOf(accent.copy(alpha = 0.13f), Color.Transparent))
            )
            .border(1.dp, accent.copy(alpha = 0.30f), shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.36f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp), tint = accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = accent,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (probeKind != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = probeKind,
                        color = MutedColor,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (criterion != null) {
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(glass.cardBorder))
            Spacer(Modifier.height(12.dp))
            Text(
                text = LABEL_CRITERION,
                color = MutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = criterion,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Where traffic left from: the region as a flag, the address itself, and what the address is
 * reputed to be. Labelled by the flag rather than by the words 出口 / 出口地址, which said the
 * same thing twice.
 */
@Composable
private fun ExitCard(result: ProbeResult, reputation: IpReputation?) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, glass.cardBorder, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val region = result.region
            val flag = region?.let { remember(it) { flagVector(it) } }
            if (flag != null) {
                FlagBadge(flag)
            } else {
                Icon(Lucide.Globe, contentDescription = null, modifier = Modifier.size(14.dp), tint = MutedColor)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = region.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            result.colo?.takeIf { it.isNotBlank() }?.let { colo ->
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "边缘节点 $colo",
                    color = MutedColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // The pill is pinned right, so it lines up whatever else the row carries.
            Spacer(Modifier.weight(1f))
            reputation?.let { rep ->
                Spacer(Modifier.width(8.dp))
                ReputationPill(rep)
            }
        }
        result.ip?.let { ip ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayIp(ip),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (result.isWarp) {
                    Spacer(Modifier.width(8.dp))
                    Tag("WARP")
                }
            }
        }
    }
}

/**
 * The latency samples as a chart: each bar is one probe's round trip, coloured on the same
 * thresholds the result cards use, and a failed sample is a short red stub rather than a number
 * sitting among the successful ones.
 */
@Composable
private fun LatencyCard(result: ProbeResult) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(14.dp)
    val samples = result.samples
    val answered = samples.filter { it >= 0 }
    val average = if (answered.isEmpty()) null else answered.sum() / answered.size
    // A failed verdict never carries a latency figure: that number would only be how long the
    // probe waited out its timeout.
    val suppressed = result.status == ProbeStatus.FAILED || result.status == ProbeStatus.BLOCKED
    val showAverage = average != null && !suppressed

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, glass.cardBorder, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (showAverage) delayLabel(average).orEmpty() else "—",
                color = if (showAverage) delayColor(average) else MutedColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    answered.isEmpty() -> "${samples.size} 次均失败"
                    else -> "${answered.size}/${samples.size} 次成功"
                },
                color = MutedColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 3.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "延迟",
                color = MutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        SampleBars(samples)
        if (answered.size < samples.size) {
            Spacer(Modifier.height(8.dp))
            Text("红色短柱为失败样本", color = MutedColor, fontSize = 10.5.sp)
        }
    }
}

@Composable
private fun SampleBars(samples: List<Long>) {
    val peak = samples.filter { it > 0 }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Row(
        Modifier
            .fillMaxWidth()
            .height(BarMax),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        samples.forEach { ms ->
            val height = if (ms < 0) {
                BarFailed
            } else {
                val fraction = (ms.toFloat() / peak.toFloat()).coerceIn(0f, 1f)
                BarMin + (BarMax - BarMin) * fraction
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (ms < 0) BadColor else delayColor(ms))
            )
        }
    }
}

/** Small tinted tag for a qualifier that belongs to a value, e.g. WARP on an egress address. */
@Composable
private fun Tag(text: String) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = text,
        color = MutedColor,
        fontSize = 10.5.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(glass.chipBg)
            .border(1.dp, glass.buttonBorder, shape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** One titled group of raw observations the page has no bespoke card for. */
@Composable
private fun SectionCard(section: EvidenceSection) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, glass.cardBorder, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(section.title, color = MutedColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        section.lines.forEachIndexed { index, line ->
            Spacer(Modifier.height(10.dp))
            if (index > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(glass.cardBorder))
                Spacer(Modifier.height(10.dp))
            }
            DetailLine(line)
        }
    }
}

@Composable
private fun DetailLine(line: EvidenceLine) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = line.label,
            color = MutedColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = line.value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
