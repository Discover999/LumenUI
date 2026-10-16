package com.lumenui.app.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.webkit.WebViewCompat
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Github
import com.composables.icons.lucide.Lucide
import com.lumenui.app.BuildConfig
import com.lumenui.app.R
import com.lumenui.app.ui.theme.LocalGlassColors

private const val GITHUB_URL = "https://github.com/Discover999/LumenUI"

/** Shown beside the repo link: the `owner/repo` tail, without the host. */
private const val GITHUB_LABEL = "Discover999/LumenUI"
private const val ISSUES_URL = "$GITHUB_URL/issues"
private const val PANEL_NAME = "zashboard"

@Composable
fun AboutDialog(visible: Boolean, onDismiss: () -> Unit) {
    val glass = LocalGlassColors.current
    val context = LocalContext.current
    // The engine the bundled panel actually renders in, which is the usual explanation for it
    // misbehaving on an older ROM.
    val webViewVersion = remember(context) {
        WebViewCompat.getCurrentWebViewPackage(context)?.versionName
    }
    val openUrl: (String) -> Unit = { url ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    GlassDialog(visible = visible, onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppMark(62.dp)
            Spacer(Modifier.height(13.dp))
            Text(
                text = "LumenUI",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(5.dp))
            // Two lines rather than one, so a drift between the app and the panel it ships shows
            // up here: the app version is *meant* to equal the panel's (versionCode 32901 tracks
            // the bundled zashboard), and the panel's own number is read out of its bundle at
            // build time rather than assumed — see PANEL_VERSION in app/build.gradle.kts.
            Text(
                text = "版本 ${BuildConfig.VERSION_NAME}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "$PANEL_NAME ${BuildConfig.PANEL_VERSION.ifEmpty { "未知" }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.5.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "WebView ${webViewVersion ?: "未知"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "连接外部 mihomo 后端的原生客户端",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            SectionCard("项目") {
                AboutRow(Lucide.Github, "GitHub 仓库", GITHUB_LABEL) { openUrl(GITHUB_URL) }
                RowHairline()
                AboutRow(Lucide.ExternalLink, "问题反馈", "提交 issue") { openUrl(ISSUES_URL) }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = glass.border)
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("好的")
            }
        }
    }
}

/** A titled group of rows on the card surface, so the page reads as sections rather than a list. */
@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val glass = LocalGlassColors.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        val shape = RoundedCornerShape(14.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(glass.cardBg)
                .border(1.dp, glass.cardBorder, shape),
            content = content
        )
    }
}

/** One tappable row: a leading mark, the label, and its value right-aligned. */
@Composable
private fun AboutRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowHairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(1.dp)
            .background(LocalGlassColors.current.cardBorder)
    )
}

/**
 * The launcher icon itself — [R.drawable.ic_launcher_background] with
 * [R.drawable.ic_launcher_foreground] over it — rather than a hand-drawn copy of the shape, so
 * this page cannot drift from the icon the system shows.
 *
 * Both layers live on an adaptive icon's 108-unit grid whose visible window is the central 72, so
 * they are drawn [ICON_LAYER_SCALE] larger and clipped by the tile: the same crop a launcher does.
 */
@Composable
private fun AppMark(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * ICON_TILE_RADIUS_RATIO)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.size(size * ICON_LAYER_SCALE)
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(size * ICON_LAYER_SCALE)
        )
    }
}

/** 108-unit adaptive-icon layer drawn so its central 72-unit window fills the tile. */
private const val ICON_LAYER_SCALE = 108f / 72f

private const val ICON_TILE_RADIUS_RATIO = 0.26f
