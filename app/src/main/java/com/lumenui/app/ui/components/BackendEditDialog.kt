package com.lumenui.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenui.app.data.Backend
import com.lumenui.app.ui.theme.LocalGlassColors
import java.util.UUID

@Composable
fun BackendEditDialog(
    visible: Boolean,
    initial: Backend?,
    onSave: (Backend) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("9090") }
    var secret by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("http") }
    var secondaryPath by remember { mutableStateOf("") }
    var addressError by remember { mutableStateOf(false) }

    // The dialog stays composed while it is closed, so the fields are reset from `initial`
    // every time it opens instead of keeping the backend that was edited last.
    LaunchedEffect(visible, initial) {
        if (!visible) return@LaunchedEffect
        name = initial?.name.orEmpty()
        host = initial?.host.orEmpty()
        port = (initial?.port ?: 9090).toString()
        secret = initial?.secret.orEmpty()
        protocol = initial?.protocol ?: "http"
        secondaryPath = initial?.secondaryPath.orEmpty()
        addressError = false
    }

    // A pasted `host:port` or URL carries its own port/path, which win over the fields.
    val parsed = BackendInput.parse(host)
    val effectivePort = parsed?.port ?: port.toIntOrNull()
    val portValid = effectivePort != null && effectivePort in 1..65535
    val canSave = host.isNotBlank() && portValid

    GlassDialog(visible = visible, onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (initial == null) "添加后端" else "编辑后端",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            GlassField(name, { name = it.take(NAME_MAX) }, "名称（可选）")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("协议", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                ProtocolChip("http", protocol == "http") { protocol = "http" }
                Spacer(Modifier.width(8.dp))
                ProtocolChip("https", protocol == "https") { protocol = "https" }
            }
            GlassField(
                value = host,
                onValueChange = { input ->
                    host = BackendInput.fold(input)
                        .filter { c -> !c.isWhitespace() }
                        .take(ADDRESS_MAX)
                    addressError = false
                },
                label = "地址 / IP"
            )
            GlassField(
                value = port,
                onValueChange = { input -> port = input.filter { it.isDigit() }.take(PORT_MAX) },
                label = "端口",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (!portValid && (port.isNotEmpty() || parsed?.port != null)) {
                Text(
                    "端口需在 1–65535 之间",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
            if (addressError) {
                Text(
                    "无法识别这个地址：请填 host、host:port 或 http://host:port",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
            GlassField(secret, { secret = it.take(SECRET_MAX) }, "密钥 Secret（可选）")
            GlassField(
                secondaryPath,
                { secondaryPath = it.filter { c -> !c.isWhitespace() }.take(PATH_MAX) },
                "二级路径（可选）"
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    enabled = canSave,
                    onClick = {
                        val backend = Backend(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            protocol = parsed?.protocol ?: protocol,
                            host = parsed?.host ?: host.trim(),
                            port = parsed?.port ?: port.toIntOrNull() ?: 9090,
                            secret = secret.trim(),
                            secondaryPath = parsed?.secondaryPath ?: secondaryPath.trim()
                        )
                        if (BackendInput.isValid(backend)) {
                            onSave(backend)
                        } else {
                            addressError = true
                        }
                    }
                ) { Text("保存") }
            }
        }
    }
}

/**
 * Frosted text field. Uses a filled (not outlined) field so the floating label moves to the
 * top *inside* the box instead of straddling the border: the border can then be one
 * continuous self-drawn ring, with no notch/square carved out for the label.
 */
@Composable
private fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val glass = LocalGlassColors.current
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = shape,
        keyboardOptions = keyboardOptions,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (focused) scheme.primary.copy(alpha = 0.80f) else glass.buttonBorder,
                shape
            ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = scheme.onSurface,
            unfocusedTextColor = scheme.onSurface,
            focusedContainerColor = glass.fieldBg,
            unfocusedContainerColor = glass.fieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedLabelColor = scheme.primary,
            unfocusedLabelColor = scheme.onSurfaceVariant,
            cursorColor = scheme.primary
        )
    )
}

/** Frosted pill for the protocol selector (http / https). */
@Composable
private fun ProtocolChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val glass = LocalGlassColors.current
    val scheme = MaterialTheme.colorScheme
    val background = if (selected) scheme.primary.copy(alpha = 0.22f) else glass.buttonBg
    val border = if (selected) scheme.primary.copy(alpha = 0.60f) else glass.buttonBorder
    val foreground = if (selected) scheme.primary else scheme.onSurfaceVariant
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

private const val NAME_MAX = 16
private const val ADDRESS_MAX = 512
private const val PORT_MAX = 5
private const val SECRET_MAX = 256
private const val PATH_MAX = 128
