package com.lumenui.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.RefreshCw
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.controlBrush
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise

/**
 * Frosted-glass top bar. Both the title area and the menu button open a frosted
 * popup anchored right below themselves.
 */
@Composable
fun FrostedTopBar(
    title: String,
    online: Boolean?,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
    onIp: () -> Unit,
    titleMenu: @Composable (dismiss: () -> Unit) -> Unit,
    actionMenu: @Composable (dismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassColors.current
    val noise = rememberFrostNoise()
    var titleMenuExpanded by remember { mutableStateOf(false) }
    var actionMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .glassSurface(
                glass = glass,
                noise = noise,
                bottomHairline = true,
                // Flat zashboard-style surface: no top highlight, no bottom scrim.
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
            AnchoredGlassMenu(
                expanded = titleMenuExpanded,
                onDismissRequest = { titleMenuExpanded = false },
                modifier = Modifier.weight(1f),
                menuModifier = Modifier.width(280.dp),
                alignToAnchorEnd = false,
                menuContent = { titleMenu { titleMenuExpanded = false } },
                anchor = {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { titleMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusLed(online)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )

            GlassIconButton(Lucide.House, "返回面板", onHome)
            GlassIconButton(Lucide.Globe, "IP 检测", onIp)
            GlassIconButton(Lucide.RefreshCw, "刷新", onRefresh)

            AnchoredGlassMenu(
                expanded = actionMenuExpanded,
                onDismissRequest = { actionMenuExpanded = false },
                menuModifier = Modifier.width(184.dp),
                alignToAnchorEnd = true,
                menuContent = { actionMenu { actionMenuExpanded = false } },
                anchor = {
                    GlassIconButton(Lucide.Menu, "菜单") { actionMenuExpanded = true }
                }
            )
        }
    }
}

@Composable
private fun StatusLed(online: Boolean?) {
    val color = when (online) {
        true -> Color(0xFF34D399)
        false -> Color(0xFFF87171)
        null -> Color(0xFF8A94A6)
    }
    Box(
        Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun GlassIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val glass = LocalGlassColors.current
    Box(
        Modifier
            .padding(start = 6.dp)
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(controlBrush(glass))
            .border(1.dp, glass.buttonBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}
