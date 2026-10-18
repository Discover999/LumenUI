package com.lumenui.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowLeftRight
import com.composables.icons.lucide.CircleX
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.RefreshCw
import com.lumenui.app.data.SpeedGrade
import com.lumenui.app.data.SpeedPhase
import com.lumenui.app.data.SpeedProvider
import com.lumenui.app.data.SpeedSample
import com.lumenui.app.data.SpeedTestState
import com.lumenui.app.data.formatBytes
import com.lumenui.app.data.formatJitter
import com.lumenui.app.data.formatMBps
import com.lumenui.app.data.formatMbps
import com.lumenui.app.data.jitterGrade
import com.lumenui.app.data.latencyGrade
import com.lumenui.app.data.speedGrade
import com.lumenui.app.data.transferTime
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise
import java.util.Locale

private const val CHART_POINTS = 32

internal val DownloadColor = Color(0xFF48A1F1)
internal val UploadColor = Color(0xFFDE2580)
private val ExcellentColor = Color(0xFF34D399)
private val GoodColor = Color(0xFF22D3EE)
private val FairColor = Color(0xFFFBBF24)
private val PoorColor = Color(0xFFE0685F)
private val BrandColor = Color(0xFF3D74E6)

/**
 * Full-screen speed test: idle, a live three-phase run, and the result. Nothing runs until the
 * user taps 开始测速; a finished result stays visible while the page is open.
 */
@Composable
fun SpeedTestScreen(
    state: SpeedTestState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onHistory: () -> Unit
) {
    val glass = LocalGlassColors.current
    val start = hapticOnClick(HapticFeedbackType.Confirm, onStart)
    val cancel = hapticOnClick(HapticFeedbackType.Confirm, onCancel)

    Box(
        Modifier
            .fillMaxSize()
            .background(glass.pageBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            SpeedTopBar(onBack, onHistory)
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                when (state.phase) {
                    SpeedPhase.IDLE -> IdleContent(start)
                    SpeedPhase.LATENCY,
                    SpeedPhase.DOWNLOAD,
                    SpeedPhase.UPLOAD -> RunningContent(state, cancel)
                    SpeedPhase.DONE -> DoneContent(state, start)
                    SpeedPhase.FAILED -> FailedContent(state.error, start)
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun SpeedTopBar(onBack: () -> Unit, onHistory: () -> Unit) {
    val glass = LocalGlassColors.current
    val noise = rememberFrostNoise()
    val openHistory = hapticOnClick(HapticFeedbackType.ContextClick, onHistory)
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
                text = "网络测速",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            GlassIconButton(Lucide.History, "测速历史", openHistory)
        }
    }
}

@Composable
private fun IdleContent(onStart: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(DownloadColor.copy(alpha = 0.14f))
                    .border(1.dp, DownloadColor.copy(alpha = 0.38f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.Play,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = DownloadColor
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "测试当前出口速度",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "下载 / 上传 / 延迟，一次完成",
                color = MutedColor,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton("开始测速", onStart)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "由 Cloudflare 测速节点提供，不可用时自动切换备用节点 · 下载 / 上传各测 8 秒，流量随当前速度变化",
                color = MutedColor,
                fontSize = 10.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    GlassCard(Modifier.fillMaxWidth()) {
        Text("说明", color = MutedColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "测试依次进行：延迟 → 下载 → 上传。结果走当前代理出口，不等同于宽带标称速率。",
            color = MutedColor,
            fontSize = 11.5.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun RunningContent(state: SpeedTestState, onCancel: () -> Unit) {
    val uploading = state.phase == SpeedPhase.UPLOAD
    val color = if (uploading) UploadColor else DownloadColor
    val samples = if (uploading) state.uploadSamples else state.downloadSamples

    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(13.dp),
                color = color,
                strokeWidth = 1.5.dp
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = when (state.phase) {
                    SpeedPhase.LATENCY -> "正在测延迟"
                    SpeedPhase.UPLOAD -> "正在测上传"
                    else -> "正在测下载"
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        if (state.phase == SpeedPhase.LATENCY) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("正在测量五次样本", color = MutedColor, fontSize = 11.sp)
            }
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatMbps(state.liveMbps),
                    color = color,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Mbps",
                    color = MutedColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            SpeedChart(samples, color, Modifier.fillMaxWidth().height(96.dp))
            SpeedAxis(samples)
        }
        Spacer(Modifier.height(10.dp))
        SpeedSteps(state.phase)
    }
    Spacer(Modifier.height(12.dp))
    SummaryStrip(state)
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        SecondaryButton("停止测速", Lucide.CircleX, onCancel)
    }
}

@Composable
private fun DoneContent(state: SpeedTestState, onRetest: () -> Unit) {
    // One unit choice for both trend cards, so download and upload never disagree.
    var showMBps by remember { mutableStateOf(false) }
    MetricGrid(state)
    Spacer(Modifier.height(12.dp))
    TrendCard(
        title = "下载速度走势",
        mbps = state.downloadMbps,
        samples = state.downloadSamples,
        color = DownloadColor,
        showMBps = showMBps,
        onToggleUnit = { showMBps = !showMBps }
    )
    Spacer(Modifier.height(12.dp))
    TrendCard(
        title = "上传速度走势",
        mbps = state.uploadMbps,
        samples = state.uploadSamples,
        color = UploadColor,
        showMBps = showMBps,
        onToggleUnit = { showMBps = !showMBps },
        note = if (state.uploadUnavailable) "当前代理下上传节点不可达" else null
    )
    Spacer(Modifier.height(12.dp))
    EstimatesCard(
        downloadMbps = state.downloadMbps,
        uploadMbps = state.uploadMbps,
        provider = state.provider,
        uploadUnavailable = state.uploadUnavailable
    )
    Spacer(Modifier.height(12.dp))
    TrafficCard(
        downloadBytes = state.downloadBytes,
        uploadBytes = state.uploadBytes,
        uploadUnavailable = state.uploadUnavailable
    )
    state.egress?.let { egress ->
        Spacer(Modifier.height(12.dp))
        EgressCard(egress, state.reputation)
    }
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        SecondaryButton("重新测速", Lucide.RefreshCw, onRetest)
    }
}

@Composable
private fun FailedContent(error: String?, onRetry: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Lucide.CircleX,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = BadColor
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "测速失败",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = error ?: "请稍后重试",
            color = MutedColor,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton("重新测速", onRetry)
    }
}

@Composable
private fun MetricGrid(state: SpeedTestState) {
    val latency = latencyGrade(state.latencyMs)
    val jitter = jitterGrade(state.jitterMs)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                label = "下载",
                value = formatMbps(state.downloadMbps),
                unit = "Mbps",
                grade = speedGrade(state.downloadMbps),
                valueColor = DownloadColor,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "上传",
                value = formatMbps(state.uploadMbps),
                unit = "Mbps",
                grade = speedGrade(state.uploadMbps),
                valueColor = UploadColor,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                label = "延迟",
                value = state.latencyMs?.toString() ?: "—",
                unit = "ms",
                grade = latency,
                valueColor = latency?.ratingColor() ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "抖动",
                value = formatJitter(state.jitterMs),
                unit = "ms",
                grade = jitter,
                valueColor = jitter?.ratingColor() ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    grade: SpeedGrade?,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier, padding = 12.dp) {
        Text(label, color = MutedColor, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = unit,
                color = MutedColor,
                fontSize = 10.5.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        grade?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it.labelText(),
                color = it.ratingColor(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TrendCard(
    title: String,
    mbps: Double?,
    samples: List<SpeedSample>,
    color: Color,
    showMBps: Boolean,
    onToggleUnit: () -> Unit,
    note: String? = null
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(title, color = MutedColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            UnitToggle(showMBps, onToggleUnit)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (showMBps) formatMBps(mbps) else formatMbps(mbps),
                color = color,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = if (showMBps) "MB/s" else "Mbps",
                color = MutedColor,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        SpeedChart(samples, color, Modifier.fillMaxWidth().height(64.dp))
        SpeedAxis(samples)
        note?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MutedColor, fontSize = 10.5.sp, lineHeight = 15.sp)
        }
    }
}

/** Swaps both trend figures between Mbps and the MB/s conversion. */
@Composable
private fun UnitToggle(showMBps: Boolean, onToggle: () -> Unit) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(percent = 50)
    Box(
        Modifier
            .clip(shape)
            .background(glass.chipBg)
            .border(1.dp, glass.buttonBorder, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Lucide.ArrowLeftRight,
            contentDescription = if (showMBps) "切换到 Mbps" else "切换到 MB/s",
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EstimatesCard(
    downloadMbps: Double?,
    uploadMbps: Double?,
    provider: SpeedProvider,
    uploadUnavailable: Boolean
) {
    val availability = if (uploadUnavailable) " · 上传节点不可达，本次仅测下载与延迟" else ""
    GlassCard(Modifier.fillMaxWidth()) {
        Text("传输时间估算", color = MutedColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        EstimateRow("下载 1 GB 文件", transferTime(1000, downloadMbps))
        EstimateRow("上传 100 MB 文件", transferTime(100, uploadMbps))
        Spacer(Modifier.height(8.dp))
        Text(
            text = "根据本次测速计算的理论估算，使用十进制单位（1 GB = 1000 MB）。" +
                "不包含协议开销和网络波动。",
            color = MutedColor,
            fontSize = 10.5.sp,
            lineHeight = 15.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "测速节点：${provider.labelText()}$availability",
            color = MutedColor,
            fontSize = 10.5.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun EstimateRow(label: String, value: String?) {
    val glass = LocalGlassColors.current
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(glass.cardBorder))
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = MutedColor, fontSize = 12.sp)
            Text(
                text = value ?: "—",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TrafficCard(downloadBytes: Long, uploadBytes: Long, uploadUnavailable: Boolean) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("流量消耗", color = MutedColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrafficMetric(
                label = "下载",
                value = formatBytes(downloadBytes),
                color = DownloadColor,
                modifier = Modifier.weight(1f)
            )
            TrafficMetric(
                label = "上传",
                value = if (uploadUnavailable) "—" else formatBytes(uploadBytes),
                color = UploadColor,
                modifier = Modifier.weight(1f)
            )
            TrafficMetric(
                label = "合计",
                value = formatBytes(downloadBytes + uploadBytes),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "按实际传输字节统计，十进制单位（1 MB = 1000 KB）",
            color = MutedColor,
            fontSize = 10.5.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun TrafficMetric(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = MutedColor, fontSize = 9.5.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun EgressCard(egress: com.lumenui.app.data.SpeedEgress, reputation: com.lumenui.app.data.IpReputation?) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val flag = egress.region?.let { remember(it) { flagVector(it) } }
            if (flag != null) {
                FlagBadge(flag)
            } else {
                Icon(
                    Lucide.Globe,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MutedColor
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = egress.region.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            egress.colo?.takeIf { it.isNotBlank() }?.let { colo ->
                Spacer(Modifier.width(8.dp))
                Text("边缘节点 $colo", color = MutedColor, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = egress.ip,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            reputation?.let { ReputationPill(it, large = true) }
        }
    }
}

@Composable
private fun SummaryStrip(state: SpeedTestState) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(14.dp)
    val downloading = state.phase == SpeedPhase.DOWNLOAD
    val uploading = state.phase == SpeedPhase.UPLOAD
    val latency = latencyGrade(state.latencyMs)
    val jitter = jitterGrade(state.jitterMs)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, glass.cardBorder, shape)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SummaryCell(
            "延迟",
            state.latencyMs?.toString() ?: "—",
            "ms",
            Modifier.weight(1f),
            valueColor = latency?.ratingColor() ?: MaterialTheme.colorScheme.onSurface
        )
        StripDivider()
        SummaryCell(
            "抖动",
            formatJitter(state.jitterMs),
            "ms",
            Modifier.weight(1f),
            valueColor = jitter?.ratingColor() ?: MaterialTheme.colorScheme.onSurface
        )
        StripDivider()
        SummaryCell(
            "下载",
            state.downloadMbps?.let { formatMbps(it) }
                ?: if (downloading) formatMbps(state.liveMbps) else "—",
            "Mbps",
            Modifier.weight(1f),
            valueColor = DownloadColor
        )
        StripDivider()
        SummaryCell(
            "上传",
            state.uploadMbps?.let { formatMbps(it) }
                ?: if (uploading) formatMbps(state.liveMbps) else "—",
            "Mbps",
            Modifier.weight(1f),
            valueColor = UploadColor
        )
    }
}

@Composable
private fun SummaryCell(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier,
    valueColor: Color
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = unit,
                color = MutedColor,
                fontSize = 9.5.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = MutedColor, fontSize = 10.5.sp)
    }
}

@Composable
private fun StripDivider() {
    val glass = LocalGlassColors.current
    Box(Modifier.width(1.dp).height(28.dp).background(glass.cardBorder))
}

@Composable
private fun SpeedSteps(phase: SpeedPhase) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Step("延迟", phase, SpeedPhase.LATENCY)
        Step("下载", phase, SpeedPhase.DOWNLOAD)
        Step("上传", phase, SpeedPhase.UPLOAD)
    }
}

@Composable
private fun Step(label: String, phase: SpeedPhase, step: SpeedPhase) {
    val order = listOf(SpeedPhase.LATENCY, SpeedPhase.DOWNLOAD, SpeedPhase.UPLOAD)
    val current = order.indexOf(phase)
    val index = order.indexOf(step)
    val done = current > index
    val active = current == index
    val color = when {
        done -> OkColor
        active -> DownloadColor
        else -> MutedColor.copy(alpha = 0.5f)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = if (active) MaterialTheme.colorScheme.onSurface else MutedColor,
            fontSize = 10.5.sp
        )
    }
}

@Composable
private fun SpeedChart(samples: List<SpeedSample>, color: Color, modifier: Modifier = Modifier) {
    val glass = LocalGlassColors.current
    val grid = glass.cardBorder
    Canvas(modifier) {
        for (line in 1..3) {
            val y = size.height * line / 4f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val points = resample(smooth(samples))
        if (points.size < 2) return@Canvas
        val maxMbps = maxOf(10.0, points.maxOf { it.mbps })
        val maxElapsed = maxOf(1L, points.maxOf { it.elapsedMs })
        val path = Path()
        points.forEachIndexed { index, sample ->
            val x = sample.elapsedMs.toFloat() / maxElapsed * size.width
            val y = size.height -
                (sample.mbps / maxMbps).toFloat() * size.height * 0.9f -
                size.height * 0.05f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun SpeedAxis(samples: List<SpeedSample>) {
    val end = samples.lastOrNull()?.elapsedMs ?: 0L
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf(0L, end / 2, end).forEach { elapsed ->
            Text(formatAxis(elapsed), color = MutedColor, fontSize = 9.sp)
        }
    }
}

private fun formatAxis(ms: Long): String =
    if (ms % 1000L == 0L) {
        "${ms / 1000}s"
    } else {
        String.format(Locale.US, "%.1fs", ms / 1000.0)
    }

/**
 * Rolling 1 s mean, so the drawn line moves at the same pace as the live figure instead of
 * showing every 250 ms burst. The axis still uses each sample's own elapsed time.
 */
internal fun smooth(samples: List<SpeedSample>, windowMs: Long = 1_000L): List<SpeedSample> {
    if (samples.size < 2) return samples
    return samples.map { sample ->
        val from = sample.elapsedMs - windowMs
        var total = 0.0
        var count = 0
        for (other in samples) {
            if (other.elapsedMs > from && other.elapsedMs <= sample.elapsedMs) {
                total += other.mbps
                count++
            }
        }
        SpeedSample(sample.elapsedMs, if (count == 0) sample.mbps else total / count)
    }
}

/** Both charts draw the same number of points whatever the raw sample density was. */
private fun resample(samples: List<SpeedSample>, count: Int = CHART_POINTS): List<SpeedSample> {
    if (samples.size < 2) return samples
    val end = samples.last().elapsedMs.coerceAtLeast(1L)
    val out = ArrayList<SpeedSample>(count)
    var index = 0
    for (i in 0 until count) {
        val target = end * i / (count - 1)
        while (index < samples.size - 2 && samples[index + 1].elapsedMs < target) index++
        val a = samples[index]
        val b = samples[(index + 1).coerceAtMost(samples.lastIndex)]
        val span = (b.elapsedMs - a.elapsedMs).coerceAtLeast(1L)
        val fraction = ((target - a.elapsedMs).toDouble() / span).coerceIn(0.0, 1.0)
        out += SpeedSample(target, a.mbps + (b.mbps - a.mbps) * fraction)
    }
    return out
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BrandColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(percent = 50)
    Row(
        Modifier
            .height(38.dp)
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, glass.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MutedColor)
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, glass.cardBorder, shape)
            .padding(padding),
        content = content
    )
}

private fun SpeedGrade.labelText(): String = when (this) {
    SpeedGrade.EXCELLENT -> "优秀"
    SpeedGrade.GOOD -> "良好"
    SpeedGrade.FAIR -> "一般"
    SpeedGrade.POOR -> "较差"
}

private fun SpeedGrade.ratingColor(): Color = when (this) {
    SpeedGrade.EXCELLENT -> ExcellentColor
    SpeedGrade.GOOD -> GoodColor
    SpeedGrade.FAIR -> FairColor
    SpeedGrade.POOR -> PoorColor
}

private fun SpeedProvider.labelText(): String = when (this) {
    SpeedProvider.CLOUDFLARE -> "Cloudflare"
    SpeedProvider.OVH -> "OVH 备用节点"
}
