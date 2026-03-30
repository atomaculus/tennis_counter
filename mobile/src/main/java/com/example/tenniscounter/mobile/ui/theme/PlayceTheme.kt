package com.example.tenniscounter.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val PlayceDarkColorScheme = darkColorScheme(
    primary = DarkPlayceColors.Accent,
    onPrimary = DarkPlayceColors.Background,
    secondary = DarkPlayceColors.TextSecondary,
    background = DarkPlayceColors.Background,
    onBackground = DarkPlayceColors.TextPrimary,
    surface = DarkPlayceColors.Surface,
    onSurface = DarkPlayceColors.TextPrimary,
    surfaceVariant = DarkPlayceColors.SurfaceElevated,
    outline = DarkPlayceColors.Border,
    error = DarkPlayceColors.Danger
)

private val PlayceLightColorScheme = lightColorScheme(
    primary = LightPlayceColors.Accent,
    onPrimary = LightPlayceColors.Background,
    secondary = LightPlayceColors.TextSecondary,
    background = LightPlayceColors.Background,
    onBackground = LightPlayceColors.TextPrimary,
    surface = LightPlayceColors.Surface,
    onSurface = LightPlayceColors.TextPrimary,
    surfaceVariant = LightPlayceColors.SurfaceElevated,
    outline = LightPlayceColors.Border,
    error = LightPlayceColors.Danger
)

@Composable
fun PlayceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkPlayceColors else LightPlayceColors
    val colorScheme = if (darkTheme) PlayceDarkColorScheme else PlayceLightColorScheme

    CompositionLocalProvider(LocalPlayceColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PlayceTypography,
            shapes = PlayceShapes,
            content = content
        )
    }
}
