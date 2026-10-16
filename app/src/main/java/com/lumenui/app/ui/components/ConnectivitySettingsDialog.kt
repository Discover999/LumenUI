package com.lumenui.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.lumenui.app.data.ConnectivityTargets
import com.lumenui.app.ui.theme.LocalGlassColors

/** Lets the user pick which platforms the connectivity page tests. */
@Composable
fun ConnectivitySettingsDialog(
    visible: Boolean,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassColors.current
    GlassDialog(visible = visible, onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "选择测试项目",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "全选",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = hapticOnClick(HapticFeedbackType.ToggleOn, onSelectAll))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "默认",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = hapticOnClick(HapticFeedbackType.ContextClick, onSelectDefault))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            HorizontalDivider(color = glass.border, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                ConnectivityTargets.all.groupBy { it.category }.forEach { (category, targets) ->
                    Text(
                        text = "$category  (${targets.count { it.name in selected }}/${targets.size})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                    targets.forEach { target ->
                        val checked = target.name in selected
                        val (brandIcon, accent) = visualPainter(target.name)
                        // The tick follows the state the tap produces, so on and off differ.
                        val toggle = hapticOnClick(
                            type = if (checked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                            onClick = { onToggle(target.name) }
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (checked) glass.buttonBg else Color.Transparent)
                                .clickable(onClick = toggle)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Same brand mark and accent the platform shows on its result card,
                            // so the picker and the test read as the same list of targets.
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    brandIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = accent
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = target.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = if (checked) Lucide.CircleCheck else Lucide.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                // One accent for every row: per-target brand colours made the
                                // column of checks read as noise rather than as a single state.
                                tint = if (checked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    glass.border
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = glass.border, modifier = Modifier.padding(top = 6.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("完成")
            }
        }
    }
}
