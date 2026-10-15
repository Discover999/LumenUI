package com.lumenui.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lumenui.app.ui.MainScreen
import com.lumenui.app.ui.StartDestination
import com.lumenui.app.ui.theme.LumenTheme
import com.lumenui.app.ui.theme.SystemBarAppearance

class MainActivity : ComponentActivity() {

    /**
     * Page to open on top of the panel, set from a launcher shortcut. Held as state so a
     * second shortcut tapped while the activity is already running (singleTop) still lands:
     * [onNewIntent] updates it and the composition reacts, instead of the first page sticking.
     */
    private var startDestination by mutableStateOf<StartDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startDestination = intent.toStartDestination()
        enableEdgeToEdge()
        setContent {
            val darkTheme = isSystemInDarkTheme()
            LumenTheme(darkTheme) {
                SystemBarAppearance(darkTheme)
                MainScreen(
                    startDestination = startDestination,
                    onStartDestinationHandled = { startDestination = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toStartDestination()?.let { startDestination = it }
    }
}

/** Maps a launcher shortcut's action onto the page it should open, or null for a plain launch. */
private fun Intent?.toStartDestination(): StartDestination? = when (this?.action) {
    ACTION_CONNECTIVITY -> StartDestination.CONNECTIVITY
    ACTION_DNS -> StartDestination.DNS
    else -> null
}

private const val ACTION_CONNECTIVITY = "com.lumenui.app.shortcut.CONNECTIVITY"
private const val ACTION_DNS = "com.lumenui.app.shortcut.DNS"
