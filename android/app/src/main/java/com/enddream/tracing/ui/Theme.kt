package com.enddream.tracing.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalDark = compositionLocalOf { false }

private val FallbackLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF0F6B5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB6E8DE),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6E0),
    onSecondaryContainer = Color(0xFF06201C),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCE5FF),
    onTertiaryContainer = Color(0xFF001E31),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F8F7),
    onBackground = Color(0xFF1A1C1B),
    surface = Color(0xFFF7F8F7),
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFE2E6E4),
    onSurfaceVariant = Color(0xFF434947),
    outline = Color(0xFF737876),
    outlineVariant = Color(0xFFC3C9C6)
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF83D5C8),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF9FF2E4),
    secondary = Color(0xFFB1CCC5),
    onSecondary = Color(0xFF1C3531),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCCE8E1),
    tertiary = Color(0xFFACCAE5),
    onTertiary = Color(0xFF143349),
    tertiaryContainer = Color(0xFF2C4A61),
    onTertiaryContainer = Color(0xFFCCE5FF),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF15171A),
    onBackground = Color(0xFFE4E6E8),
    surface = Color(0xFF15171A),
    onSurface = Color(0xFFE4E6E8),
    surfaceVariant = Color(0xFF262A2E),
    onSurfaceVariant = Color(0xFFC2C7CC),
    outline = Color(0xFF8D9399),
    outlineVariant = Color(0xFF3A3F44)
)

@Composable
fun TracingTheme(mode: String = "auto", content: @Composable () -> Unit) {
    val dark = when (mode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val light = remember(context, dark) {
        if (!dark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { dynamicLightColorScheme(context) }.getOrDefault(FallbackLight)
        } else {
            FallbackLight
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }
    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else light) {
            BackdropHost { content() }
        }
    }
}
