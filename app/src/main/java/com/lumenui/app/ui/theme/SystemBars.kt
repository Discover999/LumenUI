package com.lumenui.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Keeps the status/navigation bar icon colours in sync with the current theme.
 *
 * The activity declares `uiMode` in configChanges (so the WebView is not recreated on a
 * theme switch), which means the XML theme is not re-applied — the appearance has to be
 * driven from Compose instead.
 *
 * A dialog lives in its own window, so the activity-level call does not cover it: content
 * shown in a dialog (e.g. the connectivity page) has to call this again from inside the
 * dialog. Otherwise its bars keep the dialog window's default and the icons can end up
 * the same colour as the surface behind them (invisible).
 *
 * The appearance is re-applied every time the window regains focus: a theme switch that
 * happens while the app is in the background recomposes the UI while the window is not
 * visible, where the system ignores the request — without the focus re-apply the bars
 * would stay wrong after returning to the app.
 */
@Composable
fun SystemBarAppearance(darkTheme: Boolean = LocalGlassColors.current.isDark) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect { view.applySystemBarAppearance(darkTheme) }

    val current by rememberUpdatedState(darkTheme)
    DisposableEffect(view) {
        val observer = view.viewTreeObserver
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) view.applySystemBarAppearance(current)
        }
        observer.addOnWindowFocusChangeListener(listener)
        onDispose { if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener) }
    }
}

private fun View.applySystemBarAppearance(darkTheme: Boolean) {
    val window = (parent as? DialogWindowProvider)?.window
        ?: context.findActivity()?.window
        ?: return
    WindowCompat.getInsetsController(window, this).apply {
        isAppearanceLightStatusBars = !darkTheme
        isAppearanceLightNavigationBars = !darkTheme
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
