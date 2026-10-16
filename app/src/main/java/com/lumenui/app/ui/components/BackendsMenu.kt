package com.lumenui.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenui.app.data.Backend
import com.lumenui.app.data.Probe
import com.lumenui.app.ui.theme.LocalGlassColors

/** Content of the backend switcher popup, anchored to the top-bar title area. */
@Composable
fun BackendsMenuContent(
    backends: List<Backend>,
    currentId: String?,
    probes: Map<String, Probe>,
    onSelect: (Backend) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Backend) -> Unit,
    onDelete: (Backend) -> Unit,
    modifier: Modifier = Modifier
) {
    // Deleting a backend is irreversible and takes its stored secret with it, so the first tap
    // only arms one row and a second, deliberate tap on 删除 is what does it.
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "后端",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAdd) {
                Icon(
                    Lucide.Plus,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text("添加")
            }
        }

        if (backends.isEmpty()) {
            Text(
                "还没有后端，点击添加",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        backends.forEach { backend ->
            BackendRow(
                backend = backend,
                selected = backend.id == currentId,
                probe = probes[backend.id],
                confirming = backend.id == pendingDeleteId,
                onSelect = { onSelect(backend) },
                onEdit = { onEdit(backend) },
                onDelete = { pendingDeleteId = backend.id },
                onConfirmDelete = {
                    pendingDeleteId = null
                    onDelete(backend)
                },
                onCancelDelete = { pendingDeleteId = null }
            )
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun BackendRow(
    backend: Backend,
    selected: Boolean,
    probe: Probe?,
    confirming: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit
) {
    val glass = LocalGlassColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected || confirming) glass.chipBg else Color.Transparent)
            // Switching the active backend changes what the whole app talks to; while the row is
            // asking to confirm a delete, tapping it must not also switch backends.
            .clickable(
                enabled = !confirming,
                onClick = hapticOnClick(HapticFeedbackType.ContextClick, onSelect)
            )
            .padding(start = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(probeColor(probe))
        )
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = backend.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // The second line carries the warning while confirming, so the row keeps its height
            // and the consequence sits where the address used to be.
            Text(
                text = if (confirming) {
                    "删除后无法恢复，需重新添加"
                } else {
                    backend.baseUrl.substringAfter("://")
                },
                color = if (confirming) BadColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (confirming) {
            RowTextAction("取消", MaterialTheme.colorScheme.onSurfaceVariant, onCancelDelete)
            // Only the tap that really deletes is confirmed by touch; arming the row and
            // cancelling are both harmless, so they stay silent.
            RowTextAction("删除", BadColor, hapticOnClick(HapticFeedbackType.Confirm, onConfirmDelete))
        } else {
            probeLabel(probe)?.let { label ->
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp, end = 2.dp)
                )
            }
            // Opening the editor is only navigation and stays silent. Arming the delete gets a
            // lighter tick than the 删除 that follows, so the two steps feel different.
            RowIconButton(Lucide.Pencil, "编辑", onEdit)
            RowIconButton(Lucide.Trash2, "删除", hapticOnClick(HapticFeedbackType.ContextClick, onDelete))
        }
    }
}

/** Compact text action for the inline delete confirmation. */
@Composable
private fun RowTextAction(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp)
    )
}

/** Compact (34dp) row action so the backend list stays narrow. */
@Composable
private fun RowIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun probeColor(probe: Probe?): Color = when {
    probe == null -> Color(0xFF8A94A6)
    probe.ok -> Color(0xFF34D399)
    else -> Color(0xFFF87171)
}

private fun probeLabel(probe: Probe?): String? = when {
    probe == null -> null
    probe.ok -> "${probe.latencyMs}ms"
    else -> "离线"
}
