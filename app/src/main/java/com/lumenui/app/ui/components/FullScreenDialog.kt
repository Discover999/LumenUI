package com.lumenui.app.ui.components

import android.app.Dialog
import android.view.Window
import androidx.compose.animation.core.Animatable
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

/**
 * How long a full-screen page takes to slide, in either direction. One value for both: the same
 * distance is travelled each way, so a difference between the two reads as the exit being a
 * different motion rather than the same motion seen from the other end.
 */
private const val PAGE_SLIDE_MS = 240

/** Where the page rests while parked off to the right (0) and once it has slid into place (1). */
private const val PAGE_PARKED = 0f
private const val PAGE_SETTLED = 1f

/**
 * How opaque the page is while it sits parked off-screen. A fade from zero would make the page
 * look like it is materialising, so the entrance rises from this floor instead of from nothing.
 *
 * The exit eases back down to the same floor, which is what keeps the two directions one motion
 * seen from either end: fading further than this on the way out ends the travel before the
 * animation does, so the exit reads as the shorter of the two. Ending on 0.35 is safe because
 * the page is then a full width off to the right — it is not on screen on the frame the window
 * is dismissed, so there is nothing left to cut.
 */
private const val PAGE_ALPHA_FLOOR = 0.35f

/**
 * Full-screen dialog backed by our own [Dialog] window instead of Compose's, so it can use a
 * non-floating theme with transparent bars and really reach under the status bar. Compose's
 * dialog window is inset above the status bar, which leaves a dimmed strip there.
 *
 * [visible] is the caller's "this page should be on screen" flag; [onClosed] fires once the
 * page has finished sliding out, so the caller can drop the instance it is holding. The window
 * is created before the flag turns false and disposed only after [onClosed], which is what
 * gives the exit animation somewhere to run — without it the window would vanish on the frame
 * the close was requested. With [slide] off there is no page motion to wait for, so a close is
 * reported on the spot and the caller's own animation decides when the content is done.
 *
 * The content slides in from the right and back out to the right, which reads as a page push.
 * A full-screen surface has no anchor to grow out of, so a slide beats the scale the popup
 * menus use. [slide] off is for a surface that runs an entrance of its own — the glass dialogs
 * scale where they stand — because a modal sliding in from the edge reads as a page arriving
 * rather than as something surfacing over what is already there; the layer then stays put and the
 * content owns both halves of its motion.
 *
 * The back gesture closes the page through the same path, and therefore plays the same slide —
 * but only as a plain close: the drag itself is deliberately not wired into the platform's
 * predictive back, because on a ROM that claims the edge swipe in its own window (ColorOS does,
 * via ColorSideGestureLeft/Right) the app never sees the drag, so a preview would appear on some
 * devices and silently do nothing on others.
 */
@Composable
fun FullScreenDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onClosed: () -> Unit,
    slide: Boolean = true,
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

    // The back gesture is answered by the dialog's own window, and [Dialog] answers a back by
    // cancelling itself — which dismisses the window on the spot, so the slide-out would be drawn
    // into a window that is already gone. That is the whole reason the gesture used to cut where
    // the toolbar arrow, which only raises the flag, slid. Cancelling is therefore turned into
    // the same request the two of them share, and the window lives on until the caller drops it
    // when [onClosed] reports the slide-out finished.
    val dialog = remember {
        object : Dialog(context, R.style.Theme_LumenUI_Dialog) {
            override fun cancel() {
                currentDismiss.value()
            }
        }.apply {
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
                            translationX = size.width * (1f - p)
                            // One law in both directions, so the exit is the entrance seen
                            // from the other end: the page stays legible while it travels
                            // instead of fading into or out of nothing.
                            alpha = PAGE_ALPHA_FLOOR + (1f - PAGE_ALPHA_FLOOR) * p
                        }
                ) {
                    currentContent.value()
                }
            }
        }
    }

    // One effect drives both directions. The entrance waits one frame so the window finishes its
    // first layout, otherwise the slide's first step is spent on a view that has no size yet;
    // the exit then hands the window back so the caller can drop it. Keeping both here, keyed on
    // `visible`, is also what lets a re-open mid-slide-out restart the entrance from wherever the
    // page is instead of leaving it stranded there.
    LaunchedEffect(dialog, visible, slide) {
        if (!slide) {
            // Nothing to draw: the content runs its own entrance, so the layer stays parked where
            // it stands and a close has no second phase left to wait for.
            progress.snapTo(PAGE_SETTLED)
            if (!visible) currentClosed.value()
            return@LaunchedEffect
        }
        if (visible) {
            withFrameNanos { }
            progress.animateTo(
                PAGE_SETTLED,
                tween(durationMillis = PAGE_SLIDE_MS, easing = LinearOutSlowInEasing)
            )
        } else {
            // Back out the way it came in: [PAGE_PARKED] is off to the right, so the page is
            // pushed away rather than slid into place a second time. The same curve and duration
            // as the entrance — the page is settling into its parked position, not being yanked
            // off, so an accelerating curve would read as the motion running away.
            progress.animateTo(
                PAGE_PARKED,
                tween(durationMillis = PAGE_SLIDE_MS, easing = LinearOutSlowInEasing)
            )
            currentClosed.value()
        }
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
