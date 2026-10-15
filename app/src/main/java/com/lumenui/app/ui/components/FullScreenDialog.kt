package com.lumenui.app.ui.components

import android.app.Dialog
import android.view.Window
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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

/** How long a full-screen page takes to slide in or out. */
private const val PAGE_SLIDE_IN_MS = 240
private const val PAGE_SLIDE_OUT_MS = 190

/** Where the page sits before it has slid in (0) and once it has settled (1). */
private const val PAGE_PARKED = 0f
private const val PAGE_SETTLED = 1f

/**
 * Full-screen dialog backed by our own [Dialog] window instead of Compose's, so it can use a
 * non-floating theme with transparent bars and really reach under the status bar. Compose's
 * dialog window is inset above the status bar, which leaves a dimmed strip there.
 *
 * [visible] is the caller's "this page should be on screen" flag; [onClosed] fires once the
 * page has finished sliding out, so the caller can drop the instance it is holding. The window
 * is created before the flag turns false and disposed only after [onClosed], which is what
 * gives the exit animation somewhere to run — without it the window would vanish on the frame
 * the close was requested.
 *
 * The content slides in from the right and back out to the right, which reads as a page push.
 * A full-screen surface has no anchor to grow out of, so a slide beats the scale the popup
 * menus use.
 *
 * The gesture is deliberately not wired into the platform's predictive back: on a ROM that
 * claims the edge swipe in its own window (ColorOS does, via ColorSideGestureLeft/Right) the
 * app never sees the drag, so the handler would silently do nothing. A plain slide that always
 * plays is preferable to a preview that only appears on some devices.
 */
@Composable
fun FullScreenDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onClosed: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val glass = LocalGlassColors.current
    val darkTheme = glass.isDark
    val currentContent = rememberUpdatedState(content)
    val currentDismiss = rememberUpdatedState(onDismissRequest)
    val currentClosed = rememberUpdatedState(onClosed)
    val currentGlass = rememberUpdatedState(glass)
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
        }
    }

    // 0 = fully off to the right, 1 = settled. The exit animates this a second time, so the
    // content has to read it from inside its own composition rather than from the outer one.
    val progress = remember { Animatable(PAGE_PARKED) }

    DisposableEffect(dialog, dialogView) {
        dialog.setContentView(dialogView)
        dialog.setOnCancelListener { currentDismiss.value() }
        dialog.show()
        dialog.window?.applyEdgeToEdge()
        onDispose { dialog.dismiss() }
    }

    // The content is hosted in the dialog's own view tree, so it is composed here rather
    // than on the outer composition: writing it once from a `remember` inside this composable
    // would leave it attached to *this* composition, where the recompositions that drive the
    // slide never reach it.
    dialogView.setContent {
        CompositionLocalProvider(LocalGlassColors provides currentGlass.value) {
            MaterialTheme(
                colorScheme = currentColorScheme.value,
                typography = currentTypography.value,
                shapes = currentShapes.value
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = progress.value
                            // Only partially transparent, so the page stays legible as it
                            // travels: it should read as moving, not fading into nothing.
                            alpha = 0.35f + 0.65f * p
                            translationX = size.width * (1f - p)
                        }
                ) {
                    currentContent.value()
                }
            }
        }
    }

    // Reveal once, when the page first appears. Waiting one frame lets the window finish its
    // first layout, otherwise the slide's first step is spent on a view that has no size yet.
    LaunchedEffect(dialog) {
        withFrameNanos { }
        progress.animateTo(
            PAGE_SETTLED,
            tween(durationMillis = PAGE_SLIDE_IN_MS, easing = LinearOutSlowInEasing)
        )
    }

    // Retract on close, then hand the window back so the caller can drop it. Snapping to 0
    // first keeps an immediate re-open from resuming a half-finished slide.
    LaunchedEffect(dialog, visible) {
        if (visible) return@LaunchedEffect
        progress.snapTo(PAGE_PARKED)
        progress.animateTo(
            PAGE_SETTLED,
            tween(durationMillis = PAGE_SLIDE_OUT_MS, easing = FastOutLinearInEasing)
        )
        currentClosed.value()
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
