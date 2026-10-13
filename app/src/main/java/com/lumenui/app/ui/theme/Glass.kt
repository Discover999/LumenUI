package com.lumenui.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A small tiled monochrome noise texture. Blended with a low alpha over the glass
 * gradient it gives the surface the fine grain that reads as "frosted".
 */
@Composable
fun rememberFrostNoise(size: Int = 140): ImageBitmap = remember(size) {
    val pixels = IntArray(size * size)
    val random = Random(1337)
    for (i in pixels.indices) {
        val v = random.nextInt(256)
        pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    val bitmap = createBitmap(size, size)
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    bitmap.asImageBitmap()
}

/**
 * Draws the frosted-glass material: an optional blurred backdrop (the window pixels
 * captured from behind the surface), a vertical translucent gradient, a grain overlay,
 * a bright top highlight and an optional hairline bottom edge.
 */
fun Modifier.glassSurface(
    glass: GlassColors,
    noise: ImageBitmap,
    shape: Shape = RectangleShape,
    topHighlight: Boolean = true,
    bottomHairline: Boolean = false,
    topColor: Color = glass.barTop,
    bottomColor: Color = glass.barBottom,
    borderColor: Color = glass.border,
    bottomScrim: Boolean = true,
    backdrop: ImageBitmap? = null
): Modifier = this
    .clip(shape)
    .drawWithCache {
        val fill = Brush.verticalGradient(listOf(topColor, bottomColor))
        val grain = ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated))
        val grainAlpha = if (glass.isDark) 0.07f else 0.05f
        val highlight = glass.highlight
        val stroke = 1.dp.toPx()
        onDrawBehind {
            backdrop?.let {
                drawImage(
                    image = it,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.Medium
                )
            }
            drawRect(brush = fill)
            drawRect(brush = grain, alpha = grainAlpha, blendMode = BlendMode.Overlay)
            if (bottomScrim) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.80f to Color.Transparent,
                        1f to Color.Black.copy(alpha = if (glass.isDark) 0.20f else 0.05f)
                    )
                )
            }
            if (topHighlight) {
                drawLine(
                    color = highlight,
                    start = Offset(0f, stroke / 2f),
                    end = Offset(size.width, stroke / 2f),
                    strokeWidth = stroke
                )
            }
            if (bottomHairline) {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, size.height - stroke / 2f),
                    end = Offset(size.width, size.height - stroke / 2f),
                    strokeWidth = stroke
                )
            }
        }
    }

/** Subtle top-to-bottom gradient used for the small chip / icon controls. */
fun controlBrush(glass: GlassColors): Brush = Brush.verticalGradient(
    listOf(glass.buttonBg, glass.buttonBg.copy(alpha = glass.buttonBg.alpha * 0.55f))
)
