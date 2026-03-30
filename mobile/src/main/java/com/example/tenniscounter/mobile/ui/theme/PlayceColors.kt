package com.example.tenniscounter.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class PlayceColorPalette(
    val Background: Color,
    val Surface: Color,
    val SurfaceElevated: Color,
    val Border: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val Accent: Color,
    val AccentMuted: Color,
    val Danger: Color
)

val DarkPlayceColors = PlayceColorPalette(
    Background = Color(0xFF000000),
    Surface = Color(0xFF111111),
    SurfaceElevated = Color(0xFF171717),
    Border = Color(0xFF2A2A2A),
    TextPrimary = Color(0xFFF5F5F5),
    TextSecondary = Color(0xFFB6B6B6),
    Accent = Color(0xFFB8FF2C),
    AccentMuted = Color(0x33B8FF2C),
    Danger = Color(0xFFFF6B6B)
)

val LightPlayceColors = PlayceColorPalette(
    Background = Color(0xFFF5F5F5),
    Surface = Color(0xFFFFFFFF),
    SurfaceElevated = Color(0xFFEEEEEE),
    Border = Color(0xFFDDDDDD),
    TextPrimary = Color(0xFF111111),
    TextSecondary = Color(0xFF666666),
    Accent = Color(0xFF4CAF50),
    AccentMuted = Color(0x334CAF50),
    Danger = Color(0xFFE53935)
)

val LocalPlayceColors = compositionLocalOf { DarkPlayceColors }

/**
 * Backward-compatible accessor that delegates to the CompositionLocal,
 * so all existing PlayceColors.X references react to light/dark theme.
 *
 * Must be called inside @Composable functions.
 */
object PlayceColors {
    val Background: Color @Composable get() = LocalPlayceColors.current.Background
    val Surface: Color @Composable get() = LocalPlayceColors.current.Surface
    val SurfaceElevated: Color @Composable get() = LocalPlayceColors.current.SurfaceElevated
    val Border: Color @Composable get() = LocalPlayceColors.current.Border
    val TextPrimary: Color @Composable get() = LocalPlayceColors.current.TextPrimary
    val TextSecondary: Color @Composable get() = LocalPlayceColors.current.TextSecondary
    val Accent: Color @Composable get() = LocalPlayceColors.current.Accent
    val AccentMuted: Color @Composable get() = LocalPlayceColors.current.AccentMuted
    val Danger: Color @Composable get() = LocalPlayceColors.current.Danger
}
