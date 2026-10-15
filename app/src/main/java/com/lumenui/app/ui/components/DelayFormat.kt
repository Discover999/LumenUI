package com.lumenui.app.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Pure formatting for a node's latency. Kept free of Android screen types so the
 * thresholds can be unit-tested directly.
 */

/**
 * Node-delay colour: green under 300 ms, amber to 700 ms, red beyond. A null figure (never
 * tested) or a non-positive one (tested, no answer) is muted rather than red, so "unknown"
 * and "failed" do not read as the same thing.
 */
internal fun delayColor(ms: Long?): Color = when {
    ms == null || ms <= 0 -> MutedColor
    ms <= 300 -> OkColor
    ms <= 700 -> WarnColor
    else -> BadColor
}

/** How a node's delay reads: `未测试` / `超时` / `123ms`. */
internal fun delayLabel(ms: Long?): String? = when {
    ms == null -> null
    ms <= 0 -> "超时"
    else -> "${ms}ms"
}
