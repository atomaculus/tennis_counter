package com.example.tenniscounter.mobile.ui.share

import android.graphics.Rect
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import kotlin.math.roundToInt

@Composable
fun ShareCardCaptureDialog(
    renderModel: MatchShareManager.ShareRenderModel,
    onCaptured: (View, Rect) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val rootView = LocalView.current
        val captureBounds = remember { mutableStateOf<ComposeRect?>(null) }
        val captureRequested = remember(renderModel) { mutableStateOf(false) }
        val sentCapture = remember(renderModel) { mutableStateOf(false) }
        val layout = remember(renderModel) { mutableStateOf(ShareCardLayout()) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.btn_cancel), color = PlayceColors.TextPrimary)
                    }
                    Text(
                        text = stringResource(R.string.share_customize_title),
                        color = PlayceColors.TextPrimary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(56.dp))
                }

                // Card preview (this is the area cropped on capture).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.4f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(9f / 16f)
                            .onGloballyPositioned { coordinates ->
                                captureBounds.value = coordinates.boundsInRoot()
                            }
                    ) {
                        ShareCard(
                            data = renderModel.data,
                            layout = layout.value,
                            photoBitmap = renderModel.photoBitmap,
                            modifier = Modifier.fillMaxSize(),
                            onOffsetChange = { x, y ->
                                layout.value = layout.value.copy(offsetX = x, offsetY = y)
                            }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.share_drag_hint),
                    color = PlayceColors.TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Position presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PresetButton(
                            label = stringResource(R.string.share_pos_top),
                            selected = layout.value.anchor == ShareCardAnchor.TOP && layout.value.offsetX == 0f && layout.value.offsetY == 0f,
                            modifier = Modifier.weight(1f)
                        ) { layout.value = layout.value.copy(anchor = ShareCardAnchor.TOP, offsetX = 0f, offsetY = 0f) }
                        PresetButton(
                            label = stringResource(R.string.share_pos_center),
                            selected = layout.value.anchor == ShareCardAnchor.CENTER && layout.value.offsetX == 0f && layout.value.offsetY == 0f,
                            modifier = Modifier.weight(1f)
                        ) { layout.value = layout.value.copy(anchor = ShareCardAnchor.CENTER, offsetX = 0f, offsetY = 0f) }
                        PresetButton(
                            label = stringResource(R.string.share_pos_bottom),
                            selected = layout.value.anchor == ShareCardAnchor.BOTTOM && layout.value.offsetX == 0f && layout.value.offsetY == 0f,
                            modifier = Modifier.weight(1f)
                        ) { layout.value = layout.value.copy(anchor = ShareCardAnchor.BOTTOM, offsetX = 0f, offsetY = 0f) }
                    }

                    EditorToggle(stringResource(R.string.share_data_set_scores), layout.value.showSetScores) {
                        layout.value = layout.value.copy(showSetScores = it)
                    }
                    EditorToggle(stringResource(R.string.share_data_duration), layout.value.showDuration) {
                        layout.value = layout.value.copy(showDuration = it)
                    }
                    EditorToggle(stringResource(R.string.share_data_date), layout.value.showDate) {
                        layout.value = layout.value.copy(showDate = it)
                    }

                    OutlinedTextField(
                        value = layout.value.playerA,
                        onValueChange = { layout.value = layout.value.copy(playerA = it) },
                        label = { Text(stringResource(R.string.share_player_1)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = layout.value.playerB,
                        onValueChange = { layout.value = layout.value.copy(playerB = it) },
                        label = { Text(stringResource(R.string.share_player_2)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Pinned action, always visible above the keyboard / nav bar.
                PrimaryButton(
                    text = stringResource(R.string.share_action),
                    onClick = { captureRequested.value = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        }

        LaunchedEffect(captureRequested.value, captureBounds.value, sentCapture.value) {
            if (sentCapture.value || !captureRequested.value) return@LaunchedEffect
            val bounds = captureBounds.value ?: return@LaunchedEffect

            // Let the latest layout render before snapshotting the view.
            withFrameNanos { }
            withFrameNanos { }
            onCaptured(rootView, bounds.toAndroidRect())
            sentCapture.value = true
        }
    }
}

@Composable
private fun PresetButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    PrimaryButton(
        text = label,
        onClick = onClick,
        style = if (selected) PrimaryButtonStyle.Solid else PrimaryButtonStyle.Outline,
        modifier = modifier
    )
}

@Composable
private fun EditorToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PlayceColors.TextPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
