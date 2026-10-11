package com.lumenui.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF5B9CFF),
    onPrimary = Color(0xFF04121F),
    primaryContainer = Color(0xFF16385F),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF9BB8FF),
    onSecondary = Color(0xFF0B1B34),
    secondaryContainer = Color(0xFF22344F),
    onSecondaryContainer = Color(0xFFD8E3FF),
    tertiary = Color(0xFF7FD3E8),
    onTertiary = Color(0xFF00333F),
    tertiaryContainer = Color(0xFF184B58),
    onTertiaryContainer = Color(0xFFB6EBFB),
    background = Color(0xFF0B0E15),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF0B0E15),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF1B2130),
    onSurfaceVariant = Color(0xFFAEB8C8),
    surfaceContainerLowest = Color(0xFF080A10),
    surfaceContainerLow = Color(0xFF10141C),
    surfaceContainer = Color(0xFF141922),
    surfaceContainerHigh = Color(0xFF191F2A),
    surfaceContainerHighest = Color(0xFF1E2531),
    outline = Color(0xFF3A4459),
    outlineVariant = Color(0xFF262E3E),
    inverseSurface = Color(0xFFE6E9EF),
    inverseOnSurface = Color(0xFF1A1F2B),
    inversePrimary = Color(0xFF2F6BE0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceTint = Color(0xFF5B9CFF),
    scrim = Color(0xFF000000)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2F6BE0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E6FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF4A5F7F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE6F6),
    onSecondaryContainer = Color(0xFF0A1B33),
    tertiary = Color(0xFF186A7A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB6EBFB),
    onTertiaryContainer = Color(0xFF00333F),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF151A22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF151A22),
    surfaceVariant = Color(0xFFE3E8F0),
    onSurfaceVariant = Color(0xFF48505F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainer = Color(0xFFF1F4F9),
    surfaceContainerHigh = Color(0xFFEBEFF5),
    surfaceContainerHighest = Color(0xFFE5EAF1),
    outline = Color(0xFFB9C1CE),
    outlineVariant = Color(0xFFD5DBE4),
    inverseSurface = Color(0xFF2A3038),
    inverseOnSurface = Color(0xFFEFF1F5),
    inversePrimary = Color(0xFF5B9CFF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceTint = Color(0xFF2F6BE0),
    scrim = Color(0xFF000000)
)

/** Palette for the translucent "frosted glass" top bar and floating controls. */
data class GlassColors(
    val isDark: Boolean,
    val barTop: Color,
    val barBottom: Color,
    val highlight: Color,
    val border: Color,
    val chipBg: Color,
    val buttonBg: Color,
    val buttonBorder: Color,
    val fieldBg: Color,
    val sheetBg: Color,
    val backgroundTop: Color
)

private val DarkGlass = GlassColors(
    isDark = true,
    barTop = Color(0xFF1D1D1F),
    barBottom = Color(0xFF1D1D1F),
    highlight = Color.White.copy(alpha = 0.10f),
    border = Color.White.copy(alpha = 0.10f),
    chipBg = Color.White.copy(alpha = 0.14f),
    buttonBg = Color.White.copy(alpha = 0.12f),
    buttonBorder = Color.White.copy(alpha = 0.14f),
    fieldBg = Color.White.copy(alpha = 0.08f),
    sheetBg = Color(0xFF141922),
    backgroundTop = Color(0xFF10151F)
)

private val LightGlass = GlassColors(
    isDark = false,
    barTop = Color.White.copy(alpha = 0.86f),
    barBottom = Color.White.copy(alpha = 0.60f),
    highlight = Color.White.copy(alpha = 0.95f),
    border = Color(0xFF0B0E15).copy(alpha = 0.06f),
    chipBg = Color.White.copy(alpha = 0.80f),
    buttonBg = Color.White.copy(alpha = 0.78f),
    buttonBorder = Color(0xFF0B0E15).copy(alpha = 0.07f),
    fieldBg = Color.White.copy(alpha = 0.72f),
    sheetBg = Color(0xFFF7F9FC),
    backgroundTop = Color(0xFFFFFFFF)
)

val LocalGlassColors = staticCompositionLocalOf { DarkGlass }

@Composable
fun LumenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalGlassColors provides if (darkTheme) DarkGlass else LightGlass) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            content = content
        )
    }
}
