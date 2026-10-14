package com.lumenui.app.ui.components

import android.app.Dialog
import android.view.Window
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.lumenui.app.R
import com.lumenui.app.ui.theme.LocalGlassColors

/**
 * Full-screen dialog backed by our own [Dialog] window instead of Compose's, so it can use a
 * non-floating theme with transparent bars and really reach under the status bar. Compose's
 * dialog window is inset above the status bar, which leaves a dimmed strip there.
 *
 * The content is hosted in a [ComposeView]. A dialog has its own view tree, so the activity's
 * lifecycle owners are provided explicitly and the app-wide theme locals are re-provided
 * around the content.
 */
@Composable
fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val darkTheme = LocalGlassColors.current.isDark
    val currentContent = rememberUpdatedState(content)
    val currentDismiss = rememberUpdatedState(onDismissRequest)
    val currentGlass = rememberUpdatedState(LocalGlassColors.current)
    val currentColorScheme = rememberUpdatedState(MaterialTheme.colorScheme)
    val currentTypography = rememberUpdatedState(MaterialTheme.typography)
    val currentShapes = rememberUpdatedState(MaterialTheme.shapes)

    val dialog = remember {
        Dialog(context, R.style.Theme_LumenUI_Dialog).apply {
            setCanceledOnTouchOutside(true)
        }
    }
    val dialogView = remember {
        ComposeView(context).apply {
            (activity as? LifecycleOwner)?.let { setViewTreeLifecycleOwner(it) }
            (activity as? ViewModelStoreOwner)?.let { setViewTreeViewModelStoreOwner(it) }
            (activity as? SavedStateRegistryOwner)?.let { setViewTreeSavedStateRegistryOwner(it) }
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                CompositionLocalProvider(LocalGlassColors provides currentGlass.value) {
                    MaterialTheme(
                        colorScheme = currentColorScheme.value,
                        typography = currentTypography.value,
                        shapes = currentShapes.value
                    ) {
                        currentContent.value()
                    }
                }
            }
        }
    }

    DisposableEffect(dialog, dialogView) {
        dialog.setContentView(dialogView)
        dialog.setOnCancelListener { currentDismiss.value() }
        dialog.show()
        dialog.window?.let { it.applyEdgeToEdge() }
        onDispose { dialog.dismiss() }
    }

    // The window is live, so re-apply the bar icon tint whenever the theme changes.
    SideEffect {
        dialog.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}

private fun Window.applyEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(this, false)
}
