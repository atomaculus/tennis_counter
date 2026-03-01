package com.example.tenniscounter.mobile.ui.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tenniscounter.mobile.billing.PremiumUiState
import com.example.tenniscounter.mobile.ui.components.PlayceWordmark
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme

@Composable
fun MobileCounterScreen(
    viewModel: MobileCounterViewModel,
    premiumUiState: PremiumUiState,
    onUnlockPremium: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            HeaderRow(
                premiumUiState = premiumUiState,
                onUnlockPremium = onUnlockPremium
            )
            TimerCard(
                elapsedSeconds = state.elapsedSeconds,
                isRunning = state.isTimerRunning,
                onToggleTimer = viewModel::toggleTimer
            )
            ScoreboardCard(
                state = state,
                onPointA = viewModel::addPointToPlayerA,
                onPointB = viewModel::addPointToPlayerB,
                onUndoA = viewModel::undoLastPointForPlayerA,
                onUndoB = viewModel::undoLastPointForPlayerB
            )
            ActionRow(
                onResetGame = viewModel::resetGame,
                onResetMatch = viewModel::resetMatch
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeaderRow(
    premiumUiState: PremiumUiState,
    onUnlockPremium: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    PlayceWordmark()
                    Text(
                        text = if (premiumUiState.isPremiumUnlocked) {
                            "Counter + premium tools unlocked"
                        } else {
                            "Free live counter on mobile"
                        },
                        color = PlayceColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!premiumUiState.isPremiumUnlocked) {
                    PrimaryButton(
                        text = "Premium",
                        onClick = onUnlockPremium,
                        style = PrimaryButtonStyle.Solid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerCard(
    elapsedSeconds: Int,
    isRunning: Boolean,
    onToggleTimer: () -> Unit
) {
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
                    text = "MATCH TIMER",
                    color = PlayceColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatElapsed(elapsedSeconds),
                    color = if (isRunning) PlayceColors.Accent else PlayceColors.TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            PrimaryButton(
                text = if (isRunning) "Pause" else "Resume",
                onClick = onToggleTimer,
                style = PrimaryButtonStyle.Outline
            )
        }
    }
}

@Composable
private fun ScoreboardCard(
    state: MobileCounterState,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndoA: () -> Boolean,
    onUndoB: () -> Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "LIVE SCORE",
                color = PlayceColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            ScoreHeaderRow()
            PlayerRow(
                name = "Player A",
                sets = state.playerA.sets,
                games = state.playerA.games,
                pointsLabel = state.pointLabelForA(),
                onAddPoint = onPointA,
                onUndo = { onUndoA() },
                accentColor = PlayceColors.Accent
            )
            PlayerRow(
                name = "Player B",
                sets = state.playerB.sets,
                games = state.playerB.games,
                pointsLabel = state.pointLabelForB(),
                onAddPoint = onPointB,
                onUndo = { onUndoB() },
                accentColor = PlayceColors.TextPrimary
            )
            if (state.completedSets.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PlayceColors.SurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = state.completedSets.joinToString("  |  ") { "${it.a} – ${it.b}" },
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
private fun ScoreHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "",
            modifier = Modifier.weight(1.6f)
        )
        ScoreHeaderCell("Sets")
        ScoreHeaderCell("Games")
        ScoreHeaderCell("Pts")
    }
}

@Composable
private fun PlayerRow(
    name: String,
    sets: Int,
    games: Int,
    pointsLabel: String,
    onAddPoint: () -> Unit,
    onUndo: () -> Unit,
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        color = PlayceColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1.6f)
                    )
                    ScoreValueCell(sets.toString())
                    ScoreValueCell(games.toString())
                    ScoreValueCell(pointsLabel)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        text = "+ Point",
                        onClick = onAddPoint,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Undo",
                        onClick = onUndo,
                        style = PrimaryButtonStyle.Outline,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    onResetGame: () -> Unit,
    onResetMatch: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryButton(
                text = "Reset Game",
                onClick = onResetGame,
                style = PrimaryButtonStyle.Outline,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "New Match",
                onClick = onResetMatch,
                style = PrimaryButtonStyle.Danger,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RowScope.ScoreHeaderCell(text: String) {
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
private fun RowScope.ScoreValueCell(text: String) {
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

private fun formatElapsed(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MobileCounterPreview() {
    PlayceTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayceColors.Background)
        ) {
            ScoreboardCard(
                state = MobileCounterState(
                    playerA = CounterPlayerScore(points = 4, games = 2, sets = 1),
                    playerB = CounterPlayerScore(points = 3, games = 1, sets = 0),
                    completedSets = listOf(CounterSetScore(6, 4))
                ),
                onPointA = {},
                onPointB = {},
                onUndoA = { true },
                onUndoB = { true }
            )
        }
    }
}
