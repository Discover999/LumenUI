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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.lumenui.app.data.IpReputation
import com.lumenui.app.data.ProbeResult
import com.lumenui.app.data.ProbeTarget
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise

/**
 * One platform's evidence page: how the probe reaches it, which observation decided the
 * verdict, and the raw figures behind the card. Read-only — it describes a finished probe
 * and never re-runs or reinterprets one.
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
                        text = target.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = accent)
                    Spacer(Modifier.width(6.dp))
                    Text(label, color = accent, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    reputation?.let { rep ->
                        Spacer(Modifier.width(8.dp))
                        ReputationPill(rep)
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
                DetailCard(evidenceFor(target, result))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "以上为本次探测的原始观察值；判定依据说明了状态是如何由这些观察得出的。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun DetailCard(lines: List<EvidenceLine>) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass.cardBg)
            .border(1.dp, glass.cardBorder, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        lines.forEachIndexed { index, line ->
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
