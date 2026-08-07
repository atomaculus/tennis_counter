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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.tenniscounter.mobile.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
    onUnlockPremium: () -> Unit,
    onSendConfigToWatch: ((String, String, FormatPreset, Boolean) -> Unit)? = null,
    onMatchCompleted: (() -> Unit)? = null,
    onSaveMatch: ((MobileFinishedSummary) -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Trigger review prompt when local match ends
    var wasMatchActive by remember { mutableStateOf(false) }
    LaunchedEffect(state.isMatchOver) {
        if (!state.isMatchOver) {
            wasMatchActive = true
        } else if (wasMatchActive) {
            wasMatchActive = false
            onMatchCompleted?.invoke()
        }
    }

    val finishedSummary = state.finishedSummary

    if (state.offerDecidingTiebreak) {
        DecidingTiebreakDialog(
            onDeclineNormalSet = viewModel::declineDecidingTiebreak,
            onTiebreakTo7 = { viewModel.startDecidingTiebreak(7) },
            onSuperTiebreakTo10 = { viewModel.startDecidingTiebreak(10) }
        )
    }

    Scaffold(
        containerColor = PlayceColors.Background
    ) { innerPadding ->
        if (finishedSummary != null) {
            MobileMatchFinishedContent(
                summary = finishedSummary,
                innerPadding = innerPadding,
                onSave = { onSaveMatch?.invoke(finishedSummary) },
                onNewMatch = {
                    viewModel.startNewMatch()
                    onMatchCompleted?.invoke()
                }
            )
        } else {
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
                MatchSetupCard(
                    onApplyConfig = { nameA, nameB, preset, initialServerIsPlayerA ->
                        viewModel.setPlayerNames(nameA, nameB)
                        viewModel.setMatchFormat(
                            com.playce.shared.scoring.MatchFormat(
                                setsToWin = preset.setsToWin,
                                tiebreakAtSixAll = preset.tiebreakAtSixAll,
                                superTiebreakInFinalSet = preset.superTiebreakInFinalSet,
                                noAdScoring = preset.noAdScoring
                            )
                        )
                        viewModel.setInitialServerIsPlayerA(initialServerIsPlayerA)
                    },
                    onSendToWatch = { nameA, nameB, preset, initialServerIsPlayerA ->
                        viewModel.setPlayerNames(nameA, nameB)
                        viewModel.setMatchFormat(
                            com.playce.shared.scoring.MatchFormat(
                                setsToWin = preset.setsToWin,
                                tiebreakAtSixAll = preset.tiebreakAtSixAll,
                                superTiebreakInFinalSet = preset.superTiebreakInFinalSet,
                                noAdScoring = preset.noAdScoring
                            )
                        )
                        viewModel.setInitialServerIsPlayerA(initialServerIsPlayerA)
                        onSendConfigToWatch?.invoke(nameA, nameB, preset, initialServerIsPlayerA)
                    }
                )
                TimerCard(
                    elapsedSeconds = state.elapsedSeconds,
                    isRunning = state.isTimerRunning,
                    hasStarted = state.hasTimerStarted,
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
                    onResetMatch = viewModel::resetMatch,
                    onEndMatch = viewModel::finishMatch,
                    hasStarted = state.hasTimerStarted
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DecidingTiebreakDialog(
    onDeclineNormalSet: () -> Unit,
    onTiebreakTo7: () -> Unit,
    onSuperTiebreakTo10: () -> Unit
) {
    Dialog(onDismissRequest = onDeclineNormalSet) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_sets_tied_title),
                    color = PlayceColors.TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.dialog_sets_tied_message),
                    color = PlayceColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                PrimaryButton(
                    text = stringResource(R.string.dialog_tiebreak_to_7),
                    onClick = onTiebreakTo7,
                    style = PrimaryButtonStyle.Solid,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = stringResource(R.string.dialog_super_tiebreak_to_10),
                    onClick = onSuperTiebreakTo10,
                    style = PrimaryButtonStyle.Solid,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = stringResource(R.string.dialog_continue_normal_set),
                    onClick = onDeclineNormalSet,
                    style = PrimaryButtonStyle.Outline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                            stringResource(R.string.premium_tools_unlocked)
                        } else {
                            stringResource(R.string.free_counter_label)
                        },
                        color = PlayceColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!premiumUiState.isPremiumUnlocked) {
                    PrimaryButton(
                        text = stringResource(R.string.premium_badge),
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
    hasStarted: Boolean,
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
                    text = stringResource(R.string.label_match_timer),
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
            val buttonText = when {
                isRunning -> stringResource(R.string.btn_pause)
                hasStarted -> stringResource(R.string.btn_resume)
                else -> stringResource(R.string.btn_start)
            }
            val buttonStyle = if (!hasStarted) PrimaryButtonStyle.Solid else PrimaryButtonStyle.Outline
            PrimaryButton(
                text = buttonText,
                onClick = onToggleTimer,
                style = buttonStyle
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
                text = stringResource(R.string.label_live_score),
                color = PlayceColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            ScoreHeaderRow()
            PlayerRow(
                name = state.playerAName,
                sets = state.playerA.sets,
                games = state.playerA.games,
                pointsLabel = state.pointLabelForA(),
                onAddPoint = onPointA,
                onUndo = { onUndoA() },
                accentColor = PlayceColors.Accent
            )
            PlayerRow(
                name = state.playerBName,
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
            MatchStatusRow(state = state)
        }
    }
}

// Same layout as the iOS ScoreboardCard's MatchStatusRow (server + serve side pills).
@Composable
private fun MatchStatusRow(state: MobileCounterState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatusPill(
            title = if (state.isTiebreak) stringResource(R.string.label_tiebreak) else stringResource(R.string.label_server),
            value = if (state.currentServerIsPlayerA()) state.playerAName else state.playerBName,
            highlighted = state.isTiebreak,
            modifier = Modifier.weight(1f)
        )
        StatusPill(
            title = stringResource(R.string.label_serve_side),
            value = if (state.serveStartsOnLeftSide()) {
                stringResource(R.string.serve_side_left)
            } else {
                stringResource(R.string.serve_side_right)
            },
            highlighted = false,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusPill(
    title: String,
    value: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) PlayceColors.AccentMuted else PlayceColors.SurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = PlayceColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            color = if (highlighted) PlayceColors.Accent else PlayceColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
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
        ScoreHeaderCell(stringResource(R.string.label_sets))
        ScoreHeaderCell(stringResource(R.string.label_games))
        ScoreHeaderCell(stringResource(R.string.label_pts))
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
                        text = stringResource(R.string.btn_add_point),
                        onClick = onAddPoint,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = stringResource(R.string.btn_undo),
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
    onResetMatch: () -> Unit,
    onEndMatch: () -> Unit,
    hasStarted: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (hasStarted) {
            PrimaryButton(
                text = stringResource(R.string.btn_end_match),
                onClick = onEndMatch,
                style = PrimaryButtonStyle.Solid,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryButton(
                text = stringResource(R.string.btn_reset_game),
                onClick = onResetGame,
                style = PrimaryButtonStyle.Outline,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = stringResource(R.string.btn_new_match),
                onClick = onResetMatch,
                style = PrimaryButtonStyle.Danger,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MobileMatchFinishedContent(
    summary: MobileFinishedSummary,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onSave: () -> Unit,
    onNewMatch: () -> Unit
) {
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayceColors.Background)
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Match finished badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PlayceColors.AccentMuted)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.btn_end_match).uppercase(),
                color = PlayceColors.Accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Player names row with score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = summary.playerAName,
                color = PlayceColors.Accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = summary.setsScore,
                color = PlayceColors.TextPrimary,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = summary.playerBName,
                color = PlayceColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Detail card
        Card(
            colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = summary.setsDetail,
                    color = PlayceColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.label_duration, formatElapsed(summary.durationSeconds)),
                    color = PlayceColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryButton(
                text = if (saved) "✓ Saved" else stringResource(R.string.btn_save_match),
                onClick = {
                    if (!saved) {
                        onSave()
                        saved = true
                    }
                },
                style = if (saved) PrimaryButtonStyle.Outline else PrimaryButtonStyle.Solid,
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(
                text = stringResource(R.string.btn_new_match),
                onClick = onNewMatch,
                style = PrimaryButtonStyle.Danger,
                modifier = Modifier.fillMaxWidth()
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
                    score = com.playce.shared.scoring.ScoringEngine.MatchScore(
                        playerA = CounterPlayerScore(points = 4, games = 2, sets = 1),
                        playerB = CounterPlayerScore(points = 3, games = 1, sets = 0),
                        completedSets = listOf(CounterSetScore(6, 4))
                    )
                ),
                onPointA = {},
                onPointB = {},
                onUndoA = { true },
                onUndoB = { true }
            )
        }
    }
}
