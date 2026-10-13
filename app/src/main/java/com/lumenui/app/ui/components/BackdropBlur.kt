package com.lumenui.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

/** Walks the context chain up to the hosting [Activity], if any. */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Captures [src] (a rect in [window]'s coordinate space) and returns a blurred copy.
 *
 * The popup menus live in their own window laid over the WebView, and Compose cannot
 * blur content it does not draw, so we snapshot the window pixels behind the menu and
 * blur them ourselves. Returns null when the copy fails; callers fall back to a plain
 * translucent material.
 */
suspend fun captureBlurredBackdrop(
    window: Window,
    src: Rect,
    radiusPx: Float
): ImageBitmap? {
    if (src.width() <= 0 || src.height() <= 0) return null
    val raw = createBitmap(src.width(), src.height())
    val ok = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            try {
                PixelCopy.request(
                    window,
                    src,
                    raw,
                    { result -> if (cont.isActive) cont.resume(result == PixelCopy.SUCCESS) },
                    Handler(Looper.getMainLooper())
                )
            } catch (_: Throwable) {
                if (cont.isActive) cont.resume(false)
            }
        }
    }
    if (!ok) {
        raw.recycle()
        return null
    }
    return withContext(Dispatchers.Default) {
        val out = blur(raw, radiusPx)
        raw.recycle()
        out.asImageBitmap()
    }
}

/**
 * Separable box blur, applied twice to approximate a gaussian. Work is done at a reduced
 * resolution first, which both speeds it up and smooths the result.
 */
private fun blur(src: Bitmap, radiusPx: Float): Bitmap {
    val scale = 0.4f
    val w = max(1, (src.width * scale).roundToInt())
    val h = max(1, (src.height * scale).roundToInt())
    val small = src.scale(w, h)
    val r = max(1, (radiusPx * scale).roundToInt())
    val px = IntArray(w * h)
    small.getPixels(px, 0, w, 0, 0, w, h)
    repeat(2) {
        boxBlurHorizontal(px, w, h, r)
        boxBlurVertical(px, w, h, r)
    }
    small.recycle()

    val blurred = createBitmap(w, h)
    blurred.setPixels(px, 0, w, 0, 0, w, h)
    if (w == src.width && h == src.height) return blurred
    val out = blurred.scale(src.width, src.height)
    blurred.recycle()
    return out
}

private fun boxBlurHorizontal(px: IntArray, w: Int, h: Int, r: Int) {
    val div = 2 * r + 1
    val row = IntArray(w)
    for (y in 0 until h) {
        val base = y * w
        for (x in 0 until w) row[x] = px[base + x]
        var a = 0; var rr = 0; var g = 0; var b = 0
        for (i in -r..r) {
            val c = row[i.coerceIn(0, w - 1)]
            a += (c ushr 24) and 0xFF
            rr += (c ushr 16) and 0xFF
            g += (c ushr 8) and 0xFF
            b += c and 0xFF
        }
        for (x in 0 until w) {
            px[base + x] = ((a / div) shl 24) or ((rr / div) shl 16) or ((g / div) shl 8) or (b / div)
            val add = row[(x + r + 1).coerceIn(0, w - 1)]
            val sub = row[(x - r).coerceIn(0, w - 1)]
            a += ((add ushr 24) and 0xFF) - ((sub ushr 24) and 0xFF)
            rr += ((add ushr 16) and 0xFF) - ((sub ushr 16) and 0xFF)
            g += ((add ushr 8) and 0xFF) - ((sub ushr 8) and 0xFF)
            b += (add and 0xFF) - (sub and 0xFF)
        }
    }
}

private fun boxBlurVertical(px: IntArray, w: Int, h: Int, r: Int) {
    val div = 2 * r + 1
    val col = IntArray(h)
    for (x in 0 until w) {
        for (y in 0 until h) col[y] = px[y * w + x]
        var a = 0; var rr = 0; var g = 0; var b = 0
        for (i in -r..r) {
            val c = col[i.coerceIn(0, h - 1)]
            a += (c ushr 24) and 0xFF
            rr += (c ushr 16) and 0xFF
            g += (c ushr 8) and 0xFF
            b += c and 0xFF
        }
        for (y in 0 until h) {
            px[y * w + x] = ((a / div) shl 24) or ((rr / div) shl 16) or ((g / div) shl 8) or (b / div)
            val add = col[(y + r + 1).coerceIn(0, h - 1)]
            val sub = col[(y - r).coerceIn(0, h - 1)]
            a += ((add ushr 24) and 0xFF) - ((sub ushr 24) and 0xFF)
            rr += ((add ushr 16) and 0xFF) - ((sub ushr 16) and 0xFF)
            g += ((add ushr 8) and 0xFF) - ((sub ushr 8) and 0xFF)
            b += (add and 0xFF) - (sub and 0xFF)
        }
    }
}
