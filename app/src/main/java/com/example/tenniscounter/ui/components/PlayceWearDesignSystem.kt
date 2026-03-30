package com.example.tenniscounter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

object PlayceWearColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF101010)
    val SurfaceElevated = Color(0xFF171717)
    val SurfacePressed = Color(0xFF202020)
    val Border = Color(0xFF2A2A2A)
    val TextPrimary = Color(0xFFF5F5F5)
    val TextSecondary = Color(0xFFB6B6B6)
    val Accent = Color(0xFFB8FF2C)
    val AccentPressed = Color(0xFFA6E828)
    val AccentSoft = Color(0x2218FF8C)
    val Scrim = Color(0xCC000000)
    val Danger = Color(0xFFFF6B6B)
}

object PlayceWearSpacing {
    val Xs: Dp = 4.dp
    val Sm: Dp = 8.dp
    val Md: Dp = 10.dp
    val Lg: Dp = 12.dp
    val Xl: Dp = 16.dp
}

object PlayceWearShapes {
    val Chip = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(16.dp)
    val Sheet = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    val Pill = RoundedCornerShape(999.dp)
}

enum class PlayceButtonVariant {
    Primary, Secondary, Danger
}

@Composable
fun TennisWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = PlayceWearColors.Accent,
            secondary = PlayceWearColors.SurfaceElevated,
            background = PlayceWearColors.Background,
            onBackground = PlayceWearColors.TextPrimary,
            onPrimary = PlayceWearColors.Background
        ),
        content = content
    )
}

@Composable
fun PlayceCard(
    modifier: Modifier = Modifier,
    accentBorder: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(PlayceWearShapes.Card)
            .background(PlayceWearColors.Surface)
            .border(
                width = 1.dp,
                color = if (accentBorder) PlayceWearColors.AccentSoft else PlayceWearColors.Border,
                shape = PlayceWearShapes.Card
            )
            .padding(PlayceWearSpacing.Lg)
    ) {
        content()
    }
}

@Composable
fun PlayceChip(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(PlayceWearShapes.Chip)
            .background(if (accent) PlayceWearColors.AccentSoft else PlayceWearColors.SurfaceElevated)
            .border(
                width = 1.dp,
                color = if (accent) PlayceWearColors.Accent.copy(alpha = 0.25f) else PlayceWearColors.Border,
                shape = PlayceWearShapes.Chip
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (accent) PlayceWearColors.Accent else PlayceWearColors.TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun PlayceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: PlayceButtonVariant = PlayceButtonVariant.Secondary
) {
    val background = when (variant) {
        PlayceButtonVariant.Primary -> PlayceWearColors.Accent
        PlayceButtonVariant.Secondary -> PlayceWearColors.SurfaceElevated
        PlayceButtonVariant.Danger -> PlayceWearColors.Danger.copy(alpha = 0.18f)
    }
    val content = when (variant) {
        PlayceButtonVariant.Primary -> PlayceWearColors.Background
        PlayceButtonVariant.Secondary -> PlayceWearColors.TextPrimary
        PlayceButtonVariant.Danger -> PlayceWearColors.Danger
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (variant) {
                    PlayceButtonVariant.Primary -> PlayceWearColors.Accent.copy(alpha = 0.35f)
                    PlayceButtonVariant.Secondary -> PlayceWearColors.Border
                    PlayceButtonVariant.Danger -> PlayceWearColors.Danger.copy(alpha = 0.28f)
                },
                shape = PlayceWearShapes.Pill
            ),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (enabled) background else PlayceWearColors.Surface,
            contentColor = if (enabled) content else PlayceWearColors.TextSecondary
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = if (enabled) content else PlayceWearColors.TextSecondary
        )
    }
}

data class SheetAction(
    val label: String,
    val onClick: () -> Unit
)
