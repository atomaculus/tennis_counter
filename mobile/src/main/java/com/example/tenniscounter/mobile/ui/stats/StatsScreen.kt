package com.example.tenniscounter.mobile.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.billing.PremiumUiState
import com.example.tenniscounter.mobile.ui.components.PlayceWordmark
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    premiumUiState: PremiumUiState,
    onUnlockPremium: () -> Unit,
    onRestorePurchases: () -> Unit,
    onOpenCounter: () -> Unit,
    onExport: (() -> Unit)? = null
) {
    if (!premiumUiState.isPremiumUnlocked) {
        PremiumLockedStatsState(
            premiumUiState = premiumUiState,
            onUnlockPremium = onUnlockPremium,
            onRestorePurchases = onRestorePurchases,
            onOpenCounter = onOpenCounter
        )
        return
    }

    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayceColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_title),
            style = MaterialTheme.typography.headlineMedium,
            color = PlayceColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        if (stats.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.stats_loading), color = PlayceColors.TextSecondary)
            }
        } else if (stats.totalMatches == 0) {
            EmptyStatsMessage()
        } else {
            StatsGrid(stats)

            ResultsDistributionCard(stats)

            if (onExport != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PlayceColors.Surface)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.stats_export_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = PlayceColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.stats_export_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = PlayceColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PlayceColors.Accent)
                                .clickable { onExport() }
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.stats_export_btn),
                                color = PlayceColors.Background,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(stats: MatchStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stats_matches_played),
                value = stats.totalMatches.toString(),
                accent = true
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stats_total_time),
                value = formatDurationLong(stats.totalPlayTimeSeconds)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stats_avg_duration),
                value = formatDuration(stats.avgDurationSeconds)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stats_longest),
                value = formatDuration(stats.longestMatchSeconds)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stats_shortest),
                value = formatDuration(stats.shortestMatchSeconds)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PlayceColors.Surface)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = if (accent) PlayceColors.Accent else PlayceColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = PlayceColors.TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyStatsMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PlayceColors.Surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.stats_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = PlayceColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.stats_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = PlayceColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResultsDistributionCard(stats: MatchStats) {
    val knownResults = stats.playerAWins + stats.playerBWins

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PlayceColors.Surface)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.stats_results_title),
                style = MaterialTheme.typography.titleSmall,
                color = PlayceColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.stats_results_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = PlayceColors.TextSecondary
            )

            if (knownResults == 0) {
                Text(
                    text = stringResource(R.string.stats_results_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlayceColors.TextSecondary
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    if (stats.playerAWins > 0) {
                        Box(
                            modifier = Modifier
                                .weight(stats.playerAWins.toFloat())
                                .fillMaxHeight()
                                .background(PlayceColors.Accent)
                        )
                    }
                    if (stats.playerBWins > 0) {
                        Box(
                            modifier = Modifier
                                .weight(stats.playerBWins.toFloat())
                                .fillMaxHeight()
                                .background(PlayceColors.TextPrimary)
                        )
                    }
                }
                ResultsDistributionRow(
                    label = stringResource(R.string.setup_player_a_hint),
                    wins = stats.playerAWins,
                    knownResults = knownResults,
                    dotColor = PlayceColors.Accent
                )
                ResultsDistributionRow(
                    label = stringResource(R.string.setup_player_b_hint),
                    wins = stats.playerBWins,
                    knownResults = knownResults,
                    dotColor = PlayceColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ResultsDistributionRow(
    label: String,
    wins: Int,
    knownResults: Int,
    dotColor: androidx.compose.ui.graphics.Color
) {
    val percent = (wins * 100f / knownResults).roundToInt()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = PlayceColors.TextSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.stats_results_value, wins, percent),
            style = MaterialTheme.typography.bodyMedium,
            color = PlayceColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PremiumLockedStatsState(
    premiumUiState: PremiumUiState,
    onUnlockPremium: () -> Unit,
    onRestorePurchases: () -> Unit,
    onOpenCounter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayceColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PlayceWordmark()
            Text(
                text = stringResource(R.string.stats_premium_upsell),
                color = PlayceColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.premium_title),
                    color = PlayceColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.premium_description),
                    color = PlayceColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                premiumUiState.productPriceLabel?.let { price ->
                    Text(
                        text = stringResource(R.string.premium_price, price),
                        color = PlayceColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                premiumUiState.message?.let { message ->
                    Text(
                        text = message,
                        color = PlayceColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                PrimaryButton(
                    text = if (premiumUiState.isPurchaseInProgress) {
                        stringResource(R.string.premium_opening)
                    } else {
                        stringResource(R.string.btn_unlock_premium)
                    },
                    onClick = onUnlockPremium,
                    enabled = !premiumUiState.isPurchaseInProgress
                )
                PrimaryButton(
                    text = stringResource(R.string.btn_use_free),
                    onClick = onOpenCounter,
                    style = PrimaryButtonStyle.Outline
                )
                PrimaryButton(
                    text = stringResource(R.string.btn_restore_purchase),
                    onClick = onRestorePurchases,
                    style = PrimaryButtonStyle.Outline,
                    enabled = premiumUiState.isBillingReady
                )
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private fun formatDurationLong(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
