package com.lumenui.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Wraps a click handler so the action is confirmed by touch.
 *
 * Only actions with a consequence use this — switching the active backend, starting or retrying a
 * test, flipping a test item, deleting a backend. Plain navigation (opening a page, going back,
 * opening a menu or a dialog) deliberately stays silent: the page transition already marks it, and
 * buzzing on every tap reads as noise rather than as feedback.
 *
 * These are the view's haptic constants, so no VIBRATE permission is needed and the system's own
 * touch-feedback setting is honoured — with that switched off, nothing happens, which is correct.
 */
@Composable
internal fun hapticOnClick(type: HapticFeedbackType, onClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return {
        haptics.performHapticFeedback(type)
        onClick()
    }
}
