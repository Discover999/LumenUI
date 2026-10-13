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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
                onSelect = { onSelect(backend) },
                onEdit = { onEdit(backend) },
                onDelete = { onDelete(backend) }
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
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val glass = LocalGlassColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) glass.chipBg else Color.Transparent)
            .clickable(onClick = onSelect)
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
            Text(
                text = backend.baseUrl.substringAfter("://"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        probeLabel(probe)?.let { label ->
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp, end = 2.dp)
            )
        }
        RowIconButton(Lucide.Pencil, "编辑", onEdit)
        RowIconButton(Lucide.Trash2, "删除", onDelete)
    }
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
