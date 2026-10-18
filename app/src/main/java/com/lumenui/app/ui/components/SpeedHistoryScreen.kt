package com.lumenui.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.lumenui.app.data.SpeedHistoryEntry
import com.lumenui.app.data.SpeedProvider
import com.lumenui.app.data.formatJitter
import com.lumenui.app.data.formatMbps
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recent speed-test results. The list is loaded when the page opens and cleared in place; the
 * clear action needs a second, explicit tap because it cannot be undone.
 */
@Composable
fun SpeedHistoryScreen(
    load: () -> List<SpeedHistoryEntry>,
    remove: (Long) -> Unit,
    clear: () -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalGlassColors.current
    val noise = rememberFrostNoise()
    var entries by remember { mutableStateOf(load()) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }
    val armClear = hapticOnClick(HapticFeedbackType.ContextClick) {
        deleteTarget = DeleteTarget.All
    }
    val target = deleteTarget
    val confirmDelete = hapticOnClick(HapticFeedbackType.Confirm) {
        when (val current = target) {
            is DeleteTarget.One -> {
                remove(current.timestamp)
                entries = entries.filterNot { it.timestamp == current.timestamp }
            }
            DeleteTarget.All -> {
                clear()
                entries = emptyList()
            }
            null -> Unit
        }
        deleteTarget = null
    }

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
                        text = "测速历史",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (entries.isNotEmpty()) {
                        GlassIconButton(Lucide.Trash2, "清空历史", armClear)
                    }
                }
            }
            if (entries.isEmpty()) {
                EmptyHistory(Modifier.weight(1f))
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.timestamp }) { entry ->
                        val armDelete = hapticOnClick(HapticFeedbackType.ContextClick) {
                            deleteTarget = DeleteTarget.One(entry.timestamp)
                        }
                        HistoryCard(entry, armDelete)
                    }
                    item {
                        Text(
                            text = "长按单条记录可删除",
                            color = MutedColor,
                            fontSize = 10.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
        DeleteHistoryDialog(
            target = target,
            count = entries.size,
            onCancel = { deleteTarget = null },
            onConfirm = confirmDelete
        )
    }
}

@Composable
private fun DeleteHistoryDialog(
    target: DeleteTarget?,
    count: Int,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    GlassDialog(
        visible = target != null,
        onDismissRequest = onCancel,
        spring = FastDialogSpring
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = when (target) {
                    is DeleteTarget.One -> "删除这条记录？"
                    DeleteTarget.All -> "清空全部历史？"
                    null -> ""
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when (target) {
                    is DeleteTarget.One ->
                        "${formatTimestamp(target.timestamp)} 的测速记录，删除后无法恢复。"
                    DeleteTarget.All -> "共 $count 条记录，删除后无法恢复。"
                    null -> ""
                },
                color = MutedColor,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("取消", color = MutedColor, fontSize = 13.sp)
                }
                TextButton(onClick = onConfirm) {
                    Text(
                        text = if (target is DeleteTarget.All) "清空" else "删除",
                        color = BadColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private sealed interface DeleteTarget {
    data class One(val timestamp: Long) : DeleteTarget
    data object All : DeleteTarget
}

@Composable
private fun EmptyHistory(modifier: Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Lucide.History,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MutedColor.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "还没有测速记录",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "在测速页完成一次测试后会出现在这里",
                color = MutedColor,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun HistoryCard(entry: SpeedHistoryEntry, onDelete: () -> Unit) {
    val provider = runCatching { SpeedProvider.valueOf(entry.provider) }
        .getOrDefault(SpeedProvider.CLOUDFLARE)
    val providerText = providerLabel(provider) +
        (if (entry.uploadFallback) " · 上传经 LibreSpeed" else "") +
        (if (entry.uploadUnavailable) " · 上传未测" else "")
    val onSurface = MaterialTheme.colorScheme.onSurface

    GlassCard(
        Modifier
            .fillMaxWidth()
            .pointerInput(entry.timestamp) {
                detectTapGestures(onLongPress = { onDelete() })
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatTimestamp(entry.timestamp),
                color = onSurface,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(providerText, color = MutedColor, fontSize = 10.5.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryMetric(
                label = "下载",
                value = formatMbps(entry.downloadMbps),
                unit = "Mbps",
                color = DownloadColor,
                modifier = Modifier.weight(1f)
            )
            HistoryMetric(
                label = "上传",
                value = formatMbps(entry.uploadMbps),
                unit = "Mbps",
                color = UploadColor,
                modifier = Modifier.weight(1f)
            )
            HistoryMetric(
                label = "延迟",
                value = entry.latencyMs?.toString() ?: "—",
                unit = "ms",
                color = onSurface,
                modifier = Modifier.weight(1f)
            )
            HistoryMetric(
                label = "抖动",
                value = formatJitter(entry.jitterMs),
                unit = "ms",
                color = onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        entry.egressIp?.let { ip ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val flag = entry.egressRegion?.let { region ->
                    remember(region) { flagVector(region) }
                }
                if (flag != null) {
                    FlagBadge(flag)
                } else {
                    Icon(
                        Lucide.Globe,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MutedColor
                    )
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    text = listOfNotNull(entry.egressRegion, entry.egressColo).joinToString(" · "),
                    color = MutedColor,
                    fontSize = 10.5.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = ip,
                    color = MutedColor,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HistoryMetric(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier
) {
    Column(modifier) {
        Text(label, color = MutedColor, fontSize = 9.5.sp)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = unit,
                color = MutedColor,
                fontSize = 8.5.sp,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))

private fun providerLabel(provider: SpeedProvider): String = when (provider) {
    SpeedProvider.CLOUDFLARE -> "Cloudflare"
    SpeedProvider.OVH -> "OVH 备用节点"
    SpeedProvider.LIBRESPEED -> "LibreSpeed 备用节点"
}
