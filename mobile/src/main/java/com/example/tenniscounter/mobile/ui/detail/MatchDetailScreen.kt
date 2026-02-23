package com.example.tenniscounter.mobile.ui.detail

import android.graphics.Rect
import android.net.Uri
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tenniscounter.mobile.data.local.MatchEntity
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.components.SectionHeader
import com.example.tenniscounter.mobile.ui.components.ShareCard
import com.example.tenniscounter.mobile.ui.share.MatchShareManager
import com.example.tenniscounter.mobile.ui.share.MatchShareManager.ShareRenderModel
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
    onBack: () -> Unit
) {
    val match by viewModel.match.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shareError = remember { mutableStateOf<String?>(null) }
    val shareRenderModel = remember { mutableStateOf<ShareRenderModel?>(null) }

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
                    Text("Back", color = PlayceColors.TextPrimary)
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
                Text("Match not found", color = PlayceColors.TextPrimary)
            }
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
}

@Composable
private fun DetailContent(
    match: MatchEntity,
    shareError: String?,
    onAddPhoto: () -> Unit,
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
                    "Duration ${formatDuration(match.durationSeconds)}"
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
                    text = "Photo: ${match.photoUri ?: "No photo selected"}",
                    color = PlayceColors.TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "ID: ${match.id}",
                    color = PlayceColors.TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }
        }

        PrimaryButton(
            text = "Share your Playce",
            onClick = onShare,
            modifier = Modifier.fillMaxWidth()
        )

        PrimaryButton(
            text = "Add photo",
            onClick = onAddPhoto,
            style = PrimaryButtonStyle.Outline,
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
private fun ShareCardCaptureDialog(
    renderModel: ShareRenderModel,
    onCaptured: (View, Rect) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val rootView = LocalView.current
        val captureBounds = remember { mutableStateOf<ComposeRect?>(null) }
        val sentCapture = remember(renderModel) { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.94f)
                    .aspectRatio(9f / 16f)
                    .onGloballyPositioned { coordinates ->
                        captureBounds.value = coordinates.boundsInRoot()
                    }
            ) {
                ShareCard(
                    data = renderModel.data,
                    photoBitmap = renderModel.photoBitmap,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        LaunchedEffect(captureBounds.value, sentCapture.value) {
            if (sentCapture.value) return@LaunchedEffect
            val bounds = captureBounds.value ?: return@LaunchedEffect

            withFrameNanos { }
            onCaptured(rootView, bounds.toAndroidRect())
            sentCapture.value = true
        }
    }
}

private fun ComposeRect.toAndroidRect(): Rect {
    return Rect(
        left.roundToInt(),
        top.roundToInt(),
        right.roundToInt(),
        bottom.roundToInt()
    )
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
    return setScoresText.trim().split(Regex("\\s+")).joinToString(" | ")
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
            onShare = {}
        )
    }
}
