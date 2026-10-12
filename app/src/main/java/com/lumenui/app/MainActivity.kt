package com.lumenui.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.lumenui.app.ui.MainScreen
import com.lumenui.app.ui.theme.LumenTheme
import com.lumenui.app.ui.theme.SystemBarAppearance

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = isSystemInDarkTheme()
            LumenTheme(darkTheme) {
                SystemBarAppearance(darkTheme)
                MainScreen()
            }
        }
    }
}
