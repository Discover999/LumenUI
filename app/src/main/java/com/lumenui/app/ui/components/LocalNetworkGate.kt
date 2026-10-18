package com.lumenui.app.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise

/**
 * Android 17 blocks apps targeting SDK 37 from reaching LAN addresses until
 * ACCESS_LOCAL_NETWORK is granted. Both the status probe and the panel's WebView (which
 * inherits the app's grant) then fail while public internet traffic still works, so the
 * gate explains the permission before the system prompt and keeps a settings entry for a
 * denial the system will no longer prompt for. Older releases never ask.
 */
@Composable
fun LocalNetworkGate(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT < LOCAL_NETWORK_SDK) {
        content()
        return
    }
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasLocalNetworkAccess()) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        asked = true
        granted = result
    }
    // The user may grant the permission in Settings and come back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = context.hasLocalNetworkAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (granted) {
        content()
    } else {
        LocalNetworkPermissionScreen(
            denied = asked,
            onRequest = { launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK) },
            onOpenSettings = { context.openAppSettings() }
        )
    }
}

/** First API level where local network access is gated behind a runtime permission. */
private const val LOCAL_NETWORK_SDK = 37

private fun Context.hasLocalNetworkAccess(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

@Composable
private fun LocalNetworkPermissionScreen(
    denied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val glass = LocalGlassColors.current
    val noise = rememberFrostNoise()
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .glassSurface(glass = glass, noise = noise, shape = RoundedCornerShape(24.dp))
                .border(1.dp, glass.border, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Lucide.Globe,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "需要局域网访问权限",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Android 17 默认拦截对局域网地址的访问。授予后，LumenUI 才能连接同一网络下的 mihomo 后端。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRequest) { Text("授予权限") }
            if (denied) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onOpenSettings) {
                    Text("没有弹窗？去系统设置开启")
                }
            }
        }
    }
}
