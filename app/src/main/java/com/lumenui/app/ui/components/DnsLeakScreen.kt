package com.lumenui.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleX
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.lumenui.app.data.DnsLeakNode
import com.lumenui.app.data.DnsLeakResult
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.SystemBarAppearance
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise

/**
 * Full-screen DNS-leak page. Runs the check the moment it opens and shows the verdict plus
 * the resolvers that actually answered, so a reader can tell a tunneled lookup from a leak.
 */
@Composable
fun DnsLeakScreen(
    onBack: () -> Unit,
    load: suspend () -> DnsLeakResult?
) {
    // A dialog is its own window: drive its system bars from here so the icons follow the theme.
    SystemBarAppearance()
    val glass = LocalGlassColors.current
    val noise = rememberFrostNoise()

    var runId by remember { mutableStateOf(1) }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<DnsLeakResult?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(runId) {
        running = true
        failed = false
        result = null
        val loaded = load()
        result = loaded
        failed = loaded == null
        running = false
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
                        text = "DNS 泄露检测",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (running) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                    } else {
                        GlassIconButton(Lucide.RefreshCw, "重新检测") { runId++ }
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                VerdictCard(verdictOf(running, failed, result))
                Spacer(Modifier.height(12.dp))
                ResolversCard(result?.resolvers.orEmpty(), running)
                result?.conclusion?.let { conclusion ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = conclusion,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "检测由 bash.ws 提供：解析随机子域名，再比对实际应答的 DNS 解析器与出口线路。" +
                        "仅在设备流量确实走隧道时才有意义。",
                    color = MutedColor,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

private data class Verdict(val icon: ImageVector, val color: Color, val title: String, val detail: String)

private fun verdictOf(running: Boolean, failed: Boolean, result: DnsLeakResult?): Verdict = when {
    running -> Verdict(Lucide.RefreshCw, MutedColor, "正在检测…", "解析随机子域名，读取实际应答的 DNS 服务器")
    failed || result == null -> Verdict(Lucide.CircleAlert, WarnColor, "检测失败", "无法连接检测服务，请稍后重试")
    result.leaking == true -> Verdict(Lucide.CircleX, BadColor, "可能存在 DNS 泄露", "部分查询未走隧道，被本地网络解析")
    result.leaking == false -> Verdict(Lucide.CircleCheck, OkColor, "未检测到泄露", "应答的解析器与出口线路一致")
    result.resolvers.isEmpty() -> Verdict(Lucide.CircleAlert, WarnColor, "未检测到 DNS 服务器", "可能被代理的 fake-ip 或缓存屏蔽，请稍后重试")
    else -> Verdict(Lucide.CircleAlert, WarnColor, "无法判定", result.conclusion ?: "未返回明确结论")
}

@Composable
private fun VerdictCard(verdict: Verdict) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, glass.cardBorder, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(verdict.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(verdict.icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = verdict.color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(verdict.title, color = verdict.color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(verdict.detail, color = MutedColor, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun ResolversCard(resolvers: List<DnsLeakNode>, running: Boolean) {
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
        Text(
            text = "DNS 服务器" + if (resolvers.isNotEmpty()) " · ${resolvers.size}" else "",
            color = MutedColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        when {
            resolvers.isNotEmpty() -> resolvers.forEachIndexed { index, node ->
                if (index > 0) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(glass.cardBorder)
                    )
                    Spacer(Modifier.height(10.dp))
                }
                NodeLine(node)
            }
            running -> Text("检测中…", color = MutedColor, fontSize = 13.sp)
            else -> Text("未检测到解析器", color = MutedColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun NodeLine(node: DnsLeakNode) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val flag = node.countryCode?.let { remember(it) { flagVector(it) } }
        if (flag != null) {
            FlagBadge(flag)
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = node.ip,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOfNotNull(
                node.countryName?.takeIf { it.isNotBlank() },
                node.asn?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = meta,
                    color = MutedColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
