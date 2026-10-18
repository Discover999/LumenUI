package com.lumenui.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleX
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.RefreshCw
import com.lumenui.app.data.DnsLeakNode
import com.lumenui.app.data.DnsLeakResult
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise

/**
 * Full-screen DNS-leak page. Opening it does not send any traffic: the check starts only when
 * the user taps 开始检测, and its run then shows the verdict plus the resolvers that actually
 * answered, so a reader can tell a tunneled lookup from a leak.
 *
 * The layout leads with the verdict — a large state disc, the conclusion and the counts that
 * summarise the run — then lists the resolvers grouped by the country they answered from,
 * because which countries answer is what distinguishes a tunneled lookup from a leak.
 */
@Composable
fun DnsLeakScreen(
    onBack: () -> Unit,
    load: suspend () -> DnsLeakResult?
) {
    val glass = LocalGlassColors.current
    val noise = rememberFrostNoise()

    // 0 = nothing requested yet, so the page opens idle; each tap bumps it to (re)run.
    var runId by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<DnsLeakResult?>(null) }
    var failed by remember { mutableStateOf(false) }
    val started = runId > 0
    // Starting a run is a real action with a consequence, so it is confirmed by touch; the page's
    // own navigation (back) deliberately stays silent.
    val startRun = hapticOnClick(HapticFeedbackType.Confirm) { runId++ }

    LaunchedEffect(runId) {
        if (!started) return@LaunchedEffect
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
                    // The hero owns both the idle 开始检测 action and the run's own progress, so
                    // the header stays clear until a run has finished and only then offers the
                    // re-run — one start affordance and one spinner on the page, never two.
                    if (started && !running) {
                        GlassIconButton(Lucide.RefreshCw, "重新检测", startRun)
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
                VerdictHero(
                    verdict = verdictOf(
                        running = running,
                        started = started,
                        failed = failed,
                        result = result,
                        muted = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    stats = statsOf(result),
                    started = started,
                    running = running,
                    onStart = startRun
                )
                Spacer(Modifier.height(12.dp))
                ResolversCard(result?.resolvers.orEmpty(), running, started)
                result?.conclusion?.let { conclusion ->
                    Spacer(Modifier.height(12.dp))
                    ConclusionCard(conclusion)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "检测由 bash.ws 提供：解析随机子域名，再比对实际应答的 DNS 解析器与出口线路。" +
                        "仅在设备流量确实走隧道时才有意义。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

private data class Verdict(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val detail: String,
    /** Card tint; the idle states keep their neutral wash while their text stays readable. */
    val tint: Color = color
)

private fun verdictOf(
    running: Boolean,
    started: Boolean,
    failed: Boolean,
    result: DnsLeakResult?,
    muted: Color
): Verdict = when {
    !started -> Verdict(
        Lucide.Play, muted, "尚未开始检测", "点击下方开始检测，解析随机子域名并核对应答的 DNS 服务器",
        tint = MutedColor
    )
    running -> Verdict(
        Lucide.RefreshCw, muted, "正在检测…", "解析随机子域名，读取实际应答的 DNS 服务器",
        tint = MutedColor
    )
    failed || result == null -> Verdict(Lucide.CircleAlert, WarnColor, "检测失败", "无法连接检测服务，请稍后重试")
    result.leaking == true -> Verdict(Lucide.CircleX, BadColor, "可能存在 DNS 泄露", "部分查询未走隧道，被本地网络解析")
    result.leaking == false -> Verdict(Lucide.CircleCheck, OkColor, "未检测到泄露", "应答的解析器与出口线路一致")
    result.resolvers.isEmpty() -> Verdict(Lucide.CircleAlert, WarnColor, "未检测到 DNS 服务器", "可能被代理的 fake-ip 或缓存屏蔽，请稍后重试")
    else -> Verdict(Lucide.CircleAlert, WarnColor, "无法判定", result.conclusion ?: "未返回明确结论")
}

/** The counts that summarise a finished run, shown as a row of figures under the verdict. */
private fun statsOf(result: DnsLeakResult?): List<Pair<String, String>> {
    if (result == null || result.resolvers.isEmpty()) return emptyList()
    val resolvers = result.resolvers
    val countries = resolvers.mapNotNull { it.countryCode?.uppercase()?.takeIf { c -> c.isNotBlank() } }.distinct()
    val asns = resolvers.mapNotNull { it.asn?.takeIf { a -> a.isNotBlank() } }.distinct()
    return listOf(
        resolvers.size.toString() to "解析器",
        countries.size.toString() to "国家",
        asns.size.toString() to "ASN"
    )
}

@Composable
private fun VerdictHero(
    verdict: Verdict,
    stats: List<Pair<String, String>>,
    started: Boolean,
    running: Boolean,
    onStart: () -> Unit
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
                Brush.verticalGradient(
                    listOf(verdict.tint.copy(alpha = 0.13f), Color.Transparent)
                )
            )
            .border(1.dp, verdict.tint.copy(alpha = 0.30f), shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(verdict.tint.copy(alpha = 0.14f))
                    .border(1.dp, verdict.tint.copy(alpha = 0.36f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = verdict.color,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        verdict.icon,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = verdict.color
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = verdict.title,
                    color = verdict.color,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = verdict.detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            }
        }

        if (!started) {
            Spacer(Modifier.height(14.dp))
            StartAction(onStart)
        }

        if (stats.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(glass.cardBorder))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                stats.forEachIndexed { index, (value, label) ->
                    if (index > 0) {
                        Box(Modifier.width(1.dp).height(30.dp).background(glass.cardBorder))
                    }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = value,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/** The primary action, carried by the hero while nothing has been run yet. */
@Composable
private fun StartAction(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.40f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Lucide.Play, contentDescription = null, modifier = Modifier.size(16.dp), tint = accent)
        Spacer(Modifier.width(7.dp))
        Text("开始检测", color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** One country's resolvers, so the list reads as sections rather than a flat dump. */
private data class ResolverGroup(val code: String?, val label: String, val nodes: List<DnsLeakNode>)

private fun groupResolvers(resolvers: List<DnsLeakNode>): List<ResolverGroup> =
    resolvers
        .groupBy { it.countryCode?.uppercase()?.takeIf { code -> code.isNotBlank() } }
        .map { (code, nodes) ->
            val label = nodes.firstNotNullOfOrNull { it.countryName?.takeIf { n -> n.isNotBlank() } }
                ?: code
                ?: "未知地区"
            ResolverGroup(code, label, nodes)
        }
        // Named countries first, then the resolvers bash.ws could not place.
        .sortedWith(compareBy({ it.code == null }, { it.label }))

@Composable
private fun ResolversCard(resolvers: List<DnsLeakNode>, running: Boolean, started: Boolean) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(14.dp)
    val groups = remember(resolvers) { groupResolvers(resolvers) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, glass.cardBorder, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "DNS 服务器",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (resolvers.isNotEmpty()) Chip("${resolvers.size} 个")
        }
        Spacer(Modifier.height(12.dp))
        when {
            groups.isNotEmpty() -> groups.forEachIndexed { index, group ->
                if (index > 0) Spacer(Modifier.height(14.dp))
                GroupHeader(group)
                Spacer(Modifier.height(8.dp))
                group.nodes.forEachIndexed { rowIndex, node ->
                    if (rowIndex > 0) Spacer(Modifier.height(8.dp))
                    NodeLine(node)
                }
            }
            running -> Text("检测中…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            !started -> Text("尚未检测", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            else -> Text("未检测到解析器", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun GroupHeader(group: ResolverGroup) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val flag = group.code?.let { remember(it) { flagVector(it) } }
        if (flag != null) {
            FlagBadge(flag)
        } else {
            Icon(
                Lucide.Globe,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = group.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text("${group.nodes.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
private fun NodeLine(node: DnsLeakNode) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = displayIp(node.ip),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        node.asn?.takeIf { it.isNotBlank() }?.let { asn ->
            Spacer(Modifier.width(8.dp))
            Chip(asn)
        }
    }
}

@Composable
private fun Chip(text: String) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.5.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(glass.chipBg)
            .border(1.dp, glass.buttonBorder, shape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** bash.ws' own verdict sentence, kept verbatim so the page never paraphrases the third party. */
@Composable
private fun ConclusionCard(conclusion: String) {
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
        Text("bash.ws 原始结论", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = conclusion,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}
