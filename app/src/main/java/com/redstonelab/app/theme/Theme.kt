package com.redstonelab.app.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = RedstoneAccent,
    onPrimary = TextWhite,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = TextWhite,
    secondary = ButtonDefault,
    onSecondary = TextWhite,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextWhite,
    tertiary = RedstoneAccentPressed,
    onTertiary = TextWhite,
    background = CanvasBg,
    onBackground = TextWhite,
    surface = SurfaceBar,
    onSurface = TextWhite,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = ButtonBorder,
    error = StatusError,
    onError = TextWhite
)

@Composable
fun RedstoneLabTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = CanvasBg.toArgb()
            window.navigationBarColor = CanvasBg.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
