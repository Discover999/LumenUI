package com.lumenui.app.ui.components

import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.lumenui.app.ui.theme.LocalGlassColors
import com.lumenui.app.ui.theme.glassSurface
import com.lumenui.app.ui.theme.rememberFrostNoise
import kotlinx.coroutines.launch

/** Backdrop blur radius for the popup menus, in dp. */
private val MENU_BLUR = 18.dp

/** How long an anchored menu takes to grow and fade in when it opens. */
private const val MENU_OPEN_MS = 140

/** Backdrop blur radius for the full-screen dialog background, in dp. */
private val DIALOG_BLUR = 24.dp

/**
 * How long the blurred backdrop takes to fade in once it is captured. The surface renders
 * opaque immediately and only turns translucent as the backdrop arrives, so opening never
 * waits on the capture; a short fade keeps the hand-off from reading as a flicker.
 */
private const val BACKDROP_FADE_MS = 20

/**
 * Rounded frosted-glass container used for the popup menus and dialog cards.
 *
 * When [translucent] is true the fill is a light tint that lets a blurred backdrop show
 * through; otherwise it is a near-opaque surface that must carry itself. [backdropAlpha]
 * drives the crossfade between the two so a surface can appear opaque immediately and
 * turn translucent as the blurred backdrop fades in.
 */
@Composable
fun FrostedMenu(
    modifier: Modifier = Modifier,
    backdrop: ImageBitmap? = null,
    translucent: Boolean = backdrop != null,
    backdropAlpha: Float = if (translucent) 1f else 0f,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalGlassColors.current
    val noise = rememberFrostNoise()
    val shape = RoundedCornerShape(16.dp)

    val opaqueTop = if (glass.isDark) 0.96f else 0.97f
    val opaqueBottom = if (glass.isDark) 0.90f else 0.93f
    val clearTop = if (glass.isDark) 0.46f else 0.58f
    val clearBottom = if (glass.isDark) 0.34f else 0.46f
    val topAlpha = opaqueTop + (clearTop - opaqueTop) * backdropAlpha
    val bottomAlpha = opaqueBottom + (clearBottom - opaqueBottom) * backdropAlpha
    val ring = if (glass.isDark) Color.White.copy(alpha = 0.20f) else Color(0xFF0B0E15).copy(alpha = 0.08f)
    // Lift the base tone toward white so the card reads as a light frosted haze.
    val cardTint = if (glass.isDark) lerp(glass.sheetBg, Color.White, 0.22f) else glass.sheetBg

    Column(
        modifier
            .glassSurface(
                glass = glass,
                noise = noise,
                shape = shape,
                topColor = cardTint.copy(alpha = topAlpha),
                bottomColor = cardTint.copy(alpha = bottomAlpha),
                borderColor = ring,
                backdrop = backdrop,
                backdropAlpha = backdropAlpha,
                // zashboard's glass has a single uniform inset ring and no top-only
                // highlight or bottom scrim, so the edge reads evenly all the way round.
                topHighlight = false,
                bottomScrim = false
            )
            .border(1.dp, ring, shape)
            .padding(vertical = 6.dp),
        content = content
    )
}

/**
 * Renders [anchor] and, when [expanded], pops [menuContent] up right below it,
 * clamped to the screen. Tapping outside dismisses.
 *
 * The menu is a separate window over the WebView, so on open we snapshot the window
 * pixels behind it and blur them. The menu itself renders immediately with an opaque
 * material so opening never waits on the capture; the blurred backdrop fades in on top
 * once it is ready.
 */
@Composable
fun AnchoredGlassMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    menuModifier: Modifier = Modifier,
    alignToAnchorEnd: Boolean = true,
    menuContent: @Composable ColumnScope.() -> Unit,
    anchor: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var backdrop by remember { mutableStateOf<ImageBitmap?>(null) }
    var requested by remember { mutableStateOf(false) }

    val open = remember { Animatable(0f) }
    LaunchedEffect(expanded) {
        if (!expanded) {
            backdrop = null
            requested = false
            open.snapTo(0f)
        } else {
            // Replays on every open, since the menu is only composed while expanded.
            open.animateTo(1f, tween(durationMillis = MENU_OPEN_MS))
        }
    }

    val backdropAlpha by animateFloatAsState(
        targetValue = if (backdrop != null) 1f else 0f,
        animationSpec = tween(durationMillis = BACKDROP_FADE_MS),
        label = "menuBackdropAlpha"
    )

    Box(modifier) {
        anchor()
        if (expanded) {
            val density = LocalDensity.current
            val gap = with(density) { 6.dp.roundToPx() }
            val margin = with(density) { 8.dp.roundToPx() }
            val provider = remember(gap, margin, alignToAnchorEnd) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val maxX = (windowSize.width - popupContentSize.width - margin)
                            .coerceAtLeast(margin)
                        val preferredX = if (alignToAnchorEnd) {
                            anchorBounds.right - popupContentSize.width
                        } else {
                            anchorBounds.left
                        }
                        val x = preferredX.coerceIn(margin, maxX)
                        val below = anchorBounds.bottom + gap
                        val y = if (below + popupContentSize.height <= windowSize.height - margin) {
                            below
                        } else {
                            (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(margin)
                        }
                        return IntOffset(x, y)
                    }
                }
            }
            Popup(
                popupPositionProvider = provider,
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = true)
            ) {
                val popupView = LocalView.current
                val context = LocalContext.current
                val blurPx = with(density) { MENU_BLUR.toPx() }
                FrostedMenu(
                    modifier = menuModifier
                        .onGloballyPositioned { coords ->
                            val size = coords.size
                            if (!requested && backdrop == null && size.width > 0 && size.height > 0) {
                                requested = true
                                val activity = popupView.context.findActivity() ?: context.findActivity()
                                if (activity == null) return@onGloballyPositioned
                                val viewLoc = IntArray(2).also { popupView.getLocationOnScreen(it) }
                                val winLoc = IntArray(2)
                                    .also { activity.window.decorView.getLocationOnScreen(it) }
                                val x = viewLoc[0] - winLoc[0]
                                val y = viewLoc[1] - winLoc[1]
                                val rect = Rect(x, y, x + size.width, y + size.height)
                                val intersects = rect.intersect(
                                    0,
                                    0,
                                    activity.window.decorView.width,
                                    activity.window.decorView.height
                                )
                                scope.launch {
                                    // An empty rect cannot be captured, so skip the copy entirely.
                                    backdrop = if (intersects) {
                                        runCatching {
                                            captureBlurredBackdrop(activity.window, rect, blurPx)
                                        }.getOrNull()
                                    } else {
                                        null
                                    }
                                }
                            }
                        }
                        .graphicsLayer {
                            val progress = open.value
                            alpha = progress
                            // Grows out of the anchor side rather than from the centre, so the
                            // menu reads as unfolding from the button that opened it.
                            val origin = if (alignToAnchorEnd) 1f else 0f
                            transformOrigin = TransformOrigin(origin, 0f)
                            scaleX = 0.94f + 0.06f * progress
                            scaleY = 0.94f + 0.06f * progress
                        },
                    backdrop = backdrop,
                    translucent = true,
                    backdropAlpha = backdropAlpha,
                    content = menuContent
                )
            }
        }
    }
}

/** A single tappable row inside a [FrostedMenu]. */
@Composable
fun GlassMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
    }
}

/** How long the dialog card takes to grow and fade in when it opens. */
private const val DIALOG_OPEN_MS = 160

/**
 * Frosted modal dialog: captures the window behind it (a snapshot, since the dialog is its
 * own window over the WebView) and floats a translucent [FrostedMenu] card on top, so
 * dialogs match the popup menus instead of using the plain Material surface.
 *
 * The platform's own background blur (`Window.setBackgroundBlurRadius`) is not used: it only
 * engages for translucent windows and is unimplemented on some vendor ROMs, where it left the
 * dialog with no frost at all.
 *
 * The card scales up and fades in over [DIALOG_OPEN_MS]. The dialog window itself appears at
 * once, so a plain cut reads as a hard pop; the short entrance gives the eye something to
 * follow and covers the frame where the new window is first composited.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }
    val density = LocalDensity.current
    val glass = LocalGlassColors.current
    val blurRadiusPx = remember(density) { with(density) { DIALOG_BLUR.roundToPx() } }
    var backdrop by remember { mutableStateOf<ImageBitmap?>(null) }
    val backdropAlpha by animateFloatAsState(
        targetValue = if (backdrop != null) 1f else 0f,
        animationSpec = tween(durationMillis = BACKDROP_FADE_MS),
        label = "dialogBackdropAlpha"
    )
    val open = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        open.animateTo(1f, tween(durationMillis = DIALOG_OPEN_MS))
    }

    FullScreenDialog(onDismissRequest = onDismissRequest) {
        // The dialog window is created and laid out on this same frame, so wait for it to
        // settle before the (main-thread) full-screen capture, otherwise the copy competes
        // with the window's first composition for the frame and the open visibly stutters.
        LaunchedEffect(activity) {
            val window = activity?.window ?: return@LaunchedEffect
            withFrameNanos { }
            val decor = window.decorView
            backdrop = runCatching {
                captureBlurredBackdrop(
                    window,
                    Rect(0, 0, decor.width, decor.height),
                    blurRadiusPx.toFloat()
                )
            }.getOrNull()
        }
        Box(Modifier.fillMaxSize()) {
            backdrop?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(backdropAlpha),
                    contentScale = ContentScale.FillBounds
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (glass.isDark) 0.30f else 0.16f))
            )
            FrostedMenu(
                modifier = modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val progress = open.value
                        alpha = progress
                        scaleX = 0.92f + 0.08f * progress
                        scaleY = 0.92f + 0.08f * progress
                    },
                translucent = true,
                content = content
            )
        }
    }
}
