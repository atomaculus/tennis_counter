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
 * Backward-compatible static accessor.
 * All existing code references PlayceColors.Background etc.
 * This delegates to the CompositionLocal so it works in both themes.
 *
 * IMPORTANT: This object can only be used inside @Composable functions
 * where LocalPlayceColors is provided. For non-composable contexts,
 * use the dark palette directly.
 */
object PlayceColors {
    val Background: Color get() = DarkPlayceColors.Background
    val Surface: Color get() = DarkPlayceColors.Surface
    val SurfaceElevated: Color get() = DarkPlayceColors.SurfaceElevated
    val Border: Color get() = DarkPlayceColors.Border
    val TextPrimary: Color get() = DarkPlayceColors.TextPrimary
    val TextSecondary: Color get() = DarkPlayceColors.TextSecondary
    val Accent: Color get() = DarkPlayceColors.Accent
    val AccentMuted: Color get() = DarkPlayceColors.AccentMuted
    val Danger: Color get() = DarkPlayceColors.Danger
}
