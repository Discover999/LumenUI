package com.lumenui.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Wraps a click handler so the action is confirmed by touch.
 *
 * Two groups use this. Actions with a consequence — switching the active backend, starting or
 * retrying a test, flipping a test item, deleting a backend. And every control in the top bar,
 * which is app chrome rather than page content: the three icon buttons, plus the title whose tap
 * opens the backends switcher and the menu button, neither of which has a page transition to mark
 * it. Navigation inside a page stays silent by contrast — opening a page, going back, opening a
 * menu or a dialog there, tapping a card — because the page transition already marks those, and
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
