package com.example.tenniscounter.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PlayceColorScheme = darkColorScheme(
    primary = PlayceColors.Accent,
    onPrimary = PlayceColors.Background,
    secondary = PlayceColors.TextSecondary,
    background = PlayceColors.Background,
    onBackground = PlayceColors.TextPrimary,
    surface = PlayceColors.Surface,
    onSurface = PlayceColors.TextPrimary,
    surfaceVariant = PlayceColors.SurfaceElevated,
    outline = PlayceColors.Border,
    error = PlayceColors.Danger
)

@Composable
fun PlayceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PlayceColorScheme,
        typography = PlayceTypography,
        shapes = PlayceShapes,
        content = content
    )
}
