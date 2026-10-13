package com.lumenui.app.ui.components

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Github
import com.composables.icons.lucide.Lucide
import com.lumenui.app.BuildConfig
import com.lumenui.app.ui.theme.LocalGlassColors

private const val GITHUB_URL = "https://github.com/Discover999/LumenUI"

/** Shown on the GitHub button: the `owner/repo` tail, without the host. */
private const val GITHUB_LABEL = "Discover999/LumenUI"
private const val PANEL_NAME = "zashboard"

private val MarkBar = Color(0xFF3D74E6)
private val MarkTile = Color(0xFF9BA9BE)

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val glass = LocalGlassColors.current
    val context = LocalContext.current

    GlassDialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppMark(56.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Lumen UI",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "版本 ${BuildConfig.VERSION_NAME}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "跟随内置面板 $PANEL_NAME 版本",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = glass.border)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "连接外部 mihomo 后端的纯客户端，内置 $PANEL_NAME 面板；\n提供原生顶栏与多后端管理。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(glass.buttonBg)
                    .clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri()))
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Lucide.Github,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = GITHUB_LABEL,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("好的")
            }
        }
    }
}

/** Miniature of the launcher mark: a blue bar over two neutral tiles on a white tile. */
@Composable
private fun AppMark(size: Dp) {
    val unit = size * 0.72f / 46f
    val gap = unit * 6
    val radius = unit * 6
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(unit * 46),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(unit * 20)
                    .clip(RoundedCornerShape(radius))
                    .background(MarkBar)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(radius))
                        .background(MarkTile)
                )
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(radius))
                        .background(MarkTile)
                )
            }
        }
    }
}
