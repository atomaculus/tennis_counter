package com.example.tenniscounter.mobile.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.data.local.MatchEntity
import com.example.tenniscounter.mobile.ui.components.MatchCard
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onMatchClick: (Long) -> Unit,
    onNewMatch: ((Long) -> Unit) -> Unit
) {
    val matches by viewModel.matches.collectAsStateWithLifecycle()

    HistoryContent(
        matches = matches,
        onMatchClick = onMatchClick,
        onCreateMatch = {
            onNewMatch { createdId ->
                onMatchClick(createdId)
            }
        }
    )
}

@Composable
private fun HistoryContent(
    matches: List<MatchEntity>,
    onMatchClick: (Long) -> Unit,
    onCreateMatch: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayceColors.Background),
        containerColor = PlayceColors.Background
    ) { innerPadding ->
        if (matches.isEmpty()) {
            EmptyHistoryState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onCreateMatch = onCreateMatch
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.playce_wordmark_header),
                                contentDescription = "PLAYCE",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .width(152.dp)
                                    .height(30.dp)
                            )
                            Text(
                                text = "Your match highlights",
                                color = PlayceColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        PrimaryButton(
                            text = "New Match",
                            onClick = onCreateMatch,
                            style = PrimaryButtonStyle.Outline
                        )
                    }
                }

                items(matches, key = { it.id }) { match ->
                    MatchCard(
                        match = match,
                        dateText = formatDate(match.createdAt),
                        durationText = formatDuration(match.durationSeconds).takeIf { match.durationSeconds > 0 },
                        setScoresText = match.setScoresText?.let(::formatSetScoresForDisplay),
                        onClick = { onMatchClick(match.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(
    modifier: Modifier = Modifier,
    onCreateMatch: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "No Playces yet",
                color = PlayceColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Capture your next match highlight and build your history here.",
                color = PlayceColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        PrimaryButton(
            text = "Create your first Playce",
            onClick = onCreateMatch,
            style = PrimaryButtonStyle.Outline,
            modifier = Modifier
                .padding(top = 22.dp)
                .fillMaxWidth()
        )
    }
}

private fun formatSetScoresForDisplay(setScoresText: String): String {
    return setScoresText.trim().split(Regex("\\s+")).joinToString(" | ")
}

private fun formatDate(timestampMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    return Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private fun formatDuration(totalSeconds: Long): String {
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
private fun HistoryPreview() {
    PlayceTheme {
        HistoryContent(
            matches = listOf(
                MatchEntity(
                    id = 1,
                    createdAt = System.currentTimeMillis(),
                    durationSeconds = 4812,
                    finalScoreText = "6-4 / 3-6 / 6-3",
                    setScoresText = "6-4 3-6 6-3",
                    photoUri = null,
                    idempotencyKey = "preview-1"
                ),
                MatchEntity(
                    id = 2,
                    createdAt = System.currentTimeMillis() - 3_600_000,
                    durationSeconds = 2650,
                    finalScoreText = "7-6 / 6-4",
                    setScoresText = "7-6 6-4",
                    photoUri = null,
                    idempotencyKey = "preview-2"
                )
            ),
            onMatchClick = {},
            onCreateMatch = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HistoryEmptyPreview() {
    PlayceTheme {
        HistoryContent(
            matches = emptyList(),
            onMatchClick = {},
            onCreateMatch = {}
        )
    }
}
