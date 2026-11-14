package com.example.tenniscounter.mobile.ui.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.tenniscounter.mobile.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tenniscounter.mobile.billing.PremiumUiState
import com.example.tenniscounter.mobile.data.local.MatchEntity
import com.example.tenniscounter.mobile.health.HealthConnectMatchWriter
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.components.SectionHeader
import com.example.tenniscounter.mobile.ui.share.MatchShareManager
import com.example.tenniscounter.mobile.ui.share.MatchShareManager.ShareRenderModel
import com.example.tenniscounter.mobile.ui.share.ShareCardCaptureDialog
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun MatchDetailScreen(
    viewModel: MatchDetailViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    premiumUiState: PremiumUiState,
    onUnlockPremium: () -> Unit,
    onRestorePurchases: () -> Unit
) {
    val match by viewModel.match.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shareError = remember { mutableStateOf<String?>(null) }
    val shareRenderModel = remember { mutableStateOf<ShareRenderModel?>(null) }
    val showDeleteConfirm = remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri: Uri? ->
        viewModel.updatePhotoUri(selectedUri?.toString())
    }

    Scaffold(
        containerColor = PlayceColors.Background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.btn_back), color = PlayceColors.TextPrimary)
                }
            }
        }
    ) { innerPadding ->
        val currentMatch = match
        if (currentMatch == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.match_not_found), color = PlayceColors.TextPrimary)
            }
            return@Scaffold
        }

        LaunchedEffect(currentMatch.idempotencyKey) {
            HealthConnectMatchWriter(context.applicationContext)
                .writeTennisSessionIfPermitted(currentMatch)
        }

        if (!premiumUiState.isPremiumUnlocked) {
            LockedDetailContent(
                premiumUiState = premiumUiState,
                onUnlockPremium = onUnlockPremium,
                onRestorePurchases = onRestorePurchases,
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        DetailContent(
            match = currentMatch,
            shareError = shareError.value,
            onAddPhoto = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDeleteRequest = { showDeleteConfirm.value = true },
            onShare = {
                shareError.value = null
                scope.launch {
                    val prepared = MatchShareManager.prepareShareRenderModel(
                        context = context,
                        match = currentMatch
                    )
                    if (prepared.isSuccess) {
                        shareRenderModel.value = prepared.getOrNull()
                    } else {
                        shareError.value = prepared.exceptionOrNull()?.message
                            ?: "Failed to prepare share card"
                    }
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }

    shareRenderModel.value?.let { model ->
        ShareCardCaptureDialog(
            renderModel = model,
            onCaptured = { attachedView, captureRect ->
                scope.launch {
                    val bitmapResult = MatchShareManager.captureAttachedViewArea(
                        sourceView = attachedView,
                        captureRect = captureRect
                    )
                    if (bitmapResult.isFailure) {
                        shareError.value =
                            bitmapResult.exceptionOrNull()?.message ?: "Failed to capture share card"
                        shareRenderModel.value = null
                        return@launch
                    }

                    val shareResult = MatchShareManager.shareRenderedBitmap(
                        context = context,
                        bitmap = bitmapResult.getOrThrow()
                    )
                    if (shareResult.isFailure) {
                        shareError.value =
                            shareResult.exceptionOrNull()?.message ?: "Failed to share image"
                    }
                    shareRenderModel.value = null
                }
            },
            onDismissRequest = {
                shareRenderModel.value = null
            }
        )
    }

    if (showDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = false },
            title = {
                Text(stringResource(R.string.delete_match_dialog_title), color = PlayceColors.TextPrimary)
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm.value = false
                    viewModel.deleteMatch(onDeleted = onDeleted)
                }) {
                    Text(stringResource(R.string.btn_delete), color = PlayceColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm.value = false }) {
                    Text(stringResource(R.string.btn_cancel), color = PlayceColors.TextPrimary)
                }
            },
            containerColor = PlayceColors.Surface
        )
    }
}

@Composable
private fun LockedDetailContent(
    premiumUiState: PremiumUiState,
    onUnlockPremium: () -> Unit,
    onRestorePurchases: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PlayceColors.Background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.premium_required),
            color = PlayceColors.TextPrimary,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.premium_detail_upsell),
            color = PlayceColors.TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        premiumUiState.productPriceLabel?.let { price ->
            Text(
                text = stringResource(R.string.premium_price, price),
                color = PlayceColors.TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }
        premiumUiState.message?.let { message ->
            Text(
                text = message,
                color = PlayceColors.TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
        }
        PrimaryButton(
            text = if (premiumUiState.isPurchaseInProgress) stringResource(R.string.premium_opening) else stringResource(R.string.btn_unlock_premium),
            onClick = onUnlockPremium,
            enabled = !premiumUiState.isPurchaseInProgress,
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryButton(
            text = stringResource(R.string.btn_restore_purchase),
            onClick = onRestorePurchases,
            style = PrimaryButtonStyle.Outline,
            enabled = premiumUiState.isBillingReady,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetailContent(
    match: MatchEntity,
    shareError: String?,
    onAddPhoto: () -> Unit,
    onDeleteRequest: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(PlayceColors.Background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = match.finalScoreText,
                color = PlayceColors.TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
            )
            Text(
                text = listOf(
                    formatDate(match.createdAt),
                    stringResource(R.string.label_duration, formatDuration(match.durationSeconds))
                ).joinToString("  |  "),
                color = PlayceColors.TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
            border = BorderStroke(1.dp, PlayceColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionHeader("Score recap")
                Text(
                    text = match.finalScoreText,
                    color = PlayceColors.TextPrimary,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                )
                match.setScoresText?.let { setScores ->
                    Text(
                        text = formatSetScoresForDisplay(setScores),
                        color = PlayceColors.TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (match.hasHealthMetrics()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
                border = BorderStroke(1.dp, PlayceColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader("Activity")
                    match.caloriesKcal?.let { calories ->
                        HealthMetricRow("Calories", "${calories.roundToInt()} kcal")
                    }
                    match.avgHeartRateBpm?.let { avgHeartRate ->
                        HealthMetricRow("Avg HR", "$avgHeartRate bpm")
                    }
                    match.maxHeartRateBpm?.let { maxHeartRate ->
                        HealthMetricRow("Max HR", "$maxHeartRate bpm")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
            border = BorderStroke(1.dp, PlayceColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader("Metadata")
                Text(
                    text = stringResource(R.string.label_photo, match.photoUri ?: stringResource(R.string.label_no_photo)),
                    color = PlayceColors.TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.label_id, match.id),
                    color = PlayceColors.TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }
        }

        PrimaryButton(
            text = stringResource(R.string.btn_share_playce),
            onClick = onShare,
            modifier = Modifier.fillMaxWidth()
        )

        PrimaryButton(
            text = stringResource(R.string.btn_add_photo),
            onClick = onAddPhoto,
            style = PrimaryButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth()
        )

        PrimaryButton(
            text = stringResource(R.string.btn_delete_match),
            onClick = onDeleteRequest,
            style = PrimaryButtonStyle.Danger,
            modifier = Modifier.fillMaxWidth()
        )

        shareError?.let { error ->
            Text(
                text = error,
                color = PlayceColors.Danger,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun HealthMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = PlayceColors.TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = PlayceColors.TextPrimary,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
    }
}

private fun MatchEntity.hasHealthMetrics(): Boolean {
    return caloriesKcal != null || avgHeartRateBpm != null || maxHeartRateBpm != null
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

private fun formatSetScoresForDisplay(setScoresText: String): String {
    // Records saved by older builds may carry extra "| G 0-0" / "Games:" tokens; keep set pairs only.
    val tokens = setScoresText.trim().split(Regex("\\s+"))
    val sets = mutableListOf<String>()
    var skipNext = false
    for (token in tokens) {
        when {
            skipNext -> skipNext = false
            token == "|" -> Unit
            token == "G" || token == "Games:" -> skipNext = true
            else -> sets.add(token)
        }
    }
    return sets.joinToString(" | ")
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DetailPreview() {
    PlayceTheme {
        DetailContent(
            match = MatchEntity(
                id = 42,
                createdAt = System.currentTimeMillis(),
                durationSeconds = 4812,
                finalScoreText = "6-4 / 3-6 / 6-3",
                setScoresText = "6-4 3-6 6-3",
                photoUri = null,
                idempotencyKey = "preview"
            ),
            shareError = null,
            onAddPhoto = {},
            onDeleteRequest = {},
            onShare = {}
        )
    }
}
