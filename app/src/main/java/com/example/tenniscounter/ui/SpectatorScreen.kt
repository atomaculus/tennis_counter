package com.example.tenniscounter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.AutoCenteringParams
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import com.example.tenniscounter.sound.PointSoundManager
import com.example.tenniscounter.sync.WearLiveMatchState
import java.util.Locale

// Re-declare color references to avoid depending on private MainActivity objects
private val BgColor = Color(0xFF000000)
private val SurfaceColor = Color(0xFF101010)
private val AccentColor = Color(0xFFB8FF2C)
private val TextPrimary = Color(0xFFF5F5F5)
private val TextSecondary = Color(0xFFB6B6B6)
private val BorderColor = Color(0xFF2A2A2A)

/**
 * Read-only spectator screen that mirrors the live score from the scorer watch.
 * No tap zones or action buttons — just displays the match state.
 */
@Composable
fun SpectatorScreen(
    liveState: WearLiveMatchState
) {
    val listState = rememberScalingLazyListState()
    val pointSound = remember { PointSoundManager() }
    DisposableEffect(Unit) { onDispose { pointSound.release() } }

    var prevTimestamp by remember { mutableLongStateOf(liveState.timestamp) }
    LaunchedEffect(liveState.timestamp) {
        if (liveState.timestamp != prevTimestamp) {
            when (liveState.lastScoredPlayer) {
                "A" -> pointSound.playPlayerASound()
                "B" -> pointSound.playPlayerBSound()
            }
            prevTimestamp = liveState.timestamp
        }
    }

    Scaffold(
        modifier = Modifier.background(BgColor),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 1),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // WATCHING LIVE badge
            item { WatchingLiveBadge() }

            // Sets & Games row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    SpectatorCompactScore("SETS", liveState.playerASets, liveState.playerBSets)
                    SpectatorCompactScore("GAMES", liveState.playerAGames, liveState.playerBGames)
                }
            }

            // Points board
            item {
                SpectatorPointsBoard(
                    pointA = liveState.pointLabelA,
                    pointB = liveState.pointLabelB
                )
            }

            // Completed sets
            if (liveState.completedSets.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceColor)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = liveState.completedSets.replace(",", "  |  "),
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Timer
            item { SpectatorTimer(liveState.elapsedSeconds) }
        }
    }
}

@Composable
private fun WatchingLiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "watch_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "watch_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AccentColor.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(AccentColor)
        )
        Text(
            text = "WATCHING LIVE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AccentColor
        )
    }
}

@Composable
private fun SpectatorCompactScore(label: String, a: Int, b: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextSecondary
            )
            Text(
                text = "$a - $b",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun SpectatorPointsBoard(pointA: String, pointB: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, AccentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val pointFontSize = when {
                maxWidth < 146.dp -> 36.sp
                maxWidth < 164.dp -> 42.sp
                maxWidth < 182.dp -> 48.sp
                else -> 56.sp
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpectatorBigPoint("A", pointA, pointFontSize, Modifier.weight(1f))
                Text(
                    text = "-",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                SpectatorBigPoint("B", pointB, pointFontSize, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpectatorBigPoint(
    label: String,
    points: String,
    pointFontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (label == "A") AccentColor else TextPrimary
        )
        AnimatedContent(
            targetState = points,
            transitionSpec = {
                (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.88f))
                    .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.88f))
            },
            label = "spectator_point_$label"
        ) { targetPoints ->
            Text(
                text = targetPoints,
                fontSize = pointFontSize,
                fontWeight = FontWeight.Black,
                color = if (label == "A") AccentColor else TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SpectatorTimer(elapsedSeconds: Int) {
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeText = if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    Text(
        text = timeText,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AccentColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
