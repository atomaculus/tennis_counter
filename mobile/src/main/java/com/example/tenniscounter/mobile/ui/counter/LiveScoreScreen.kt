package com.example.tenniscounter.mobile.ui.counter

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.sound.PointSoundManager
import com.example.tenniscounter.mobile.sync.LiveMatchState
import com.example.tenniscounter.mobile.ui.components.PlayceWordmark
import com.example.tenniscounter.mobile.ui.theme.PlayceColors

/**
 * Read-only screen that displays the live score being broadcast from the scorer watch.
 * Plays a sound each time the score changes (different tone for Player A vs B).
 */
@Composable
fun LiveScoreScreen(liveState: LiveMatchState) {
    val pointSound = remember { PointSoundManager() }
    DisposableEffect(Unit) { onDispose { pointSound.release() } }

    // Track previous lastScoredPlayer + timestamp to detect new points
    var prevTimestamp by remember { mutableStateOf(liveState.timestamp) }
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
        containerColor = PlayceColors.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayceColors.Background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LiveHeader()
            LiveTimerCard(elapsedSeconds = liveState.elapsedSeconds)
            LiveScoreboardCard(liveState = liveState)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LiveHeader() {
    Card(
        colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    PlayceWordmark()
                    Text(
                        text = stringResource(R.string.watching_live),
                        color = PlayceColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                LiveBadge()
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(PlayceColors.Accent.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(PlayceColors.Accent)
        )
        Text(
            text = stringResource(R.string.label_live),
            color = PlayceColors.Accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LiveTimerCard(elapsedSeconds: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.label_match_timer),
                    color = PlayceColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatLiveElapsed(elapsedSeconds),
                    color = PlayceColors.Accent,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LiveScoreboardCard(liveState: LiveMatchState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.label_live_score),
                color = PlayceColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            LiveScoreHeaderRow()
            LivePlayerRow(
                name = stringResource(R.string.setup_player_a_hint),
                sets = liveState.playerASets,
                games = liveState.playerAGames,
                pointsLabel = liveState.pointLabelA,
                accentColor = PlayceColors.Accent
            )
            LivePlayerRow(
                name = stringResource(R.string.setup_player_b_hint),
                sets = liveState.playerBSets,
                games = liveState.playerBGames,
                pointsLabel = liveState.pointLabelB,
                accentColor = PlayceColors.TextPrimary
            )
            val completedSets = liveState.completedSets
            if (completedSets.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PlayceColors.SurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = completedSets.replace(",", "  |  "),
                        color = PlayceColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveScoreHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "",
            modifier = Modifier.weight(1.6f)
        )
        LiveScoreHeaderCell(stringResource(R.string.label_sets))
        LiveScoreHeaderCell(stringResource(R.string.label_games))
        LiveScoreHeaderCell(stringResource(R.string.label_pts))
    }
}

@Composable
private fun LivePlayerRow(
    name: String,
    sets: Int,
    games: Int,
    pointsLabel: String,
    accentColor: androidx.compose.ui.graphics.Color = PlayceColors.Accent
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PlayceColors.Border, RoundedCornerShape(14.dp))
            .background(PlayceColors.Background)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    color = PlayceColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1.6f)
                )
                LiveScoreValueCell(sets.toString())
                LiveScoreValueCell(games.toString())
                LiveScoreValueCell(pointsLabel)
            }
        }
    }
}

@Composable
private fun RowScope.LiveScoreHeaderCell(text: String) {
    Text(
        text = text,
        color = PlayceColors.TextSecondary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .weight(0.75f)
            .padding(horizontal = 2.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun RowScope.LiveScoreValueCell(text: String) {
    Text(
        text = text,
        color = PlayceColors.TextPrimary,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .weight(0.75f)
            .padding(horizontal = 2.dp),
        textAlign = TextAlign.Center
    )
}

private fun formatLiveElapsed(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
