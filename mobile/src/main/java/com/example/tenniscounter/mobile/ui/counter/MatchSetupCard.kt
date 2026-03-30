package com.example.tenniscounter.mobile.ui.counter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.theme.PlayceColors

/**
 * Match configuration card: player names, format selection, and "Send to Watch" button.
 */
@Composable
fun MatchSetupCard(
    onApplyConfig: (playerAName: String, playerBName: String, formatPreset: FormatPreset) -> Unit,
    onSendToWatch: (playerAName: String, playerBName: String, formatPreset: FormatPreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var playerAName by remember { mutableStateOf("") }
    var playerBName by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableIntStateOf(0) }
    var sentToWatch by remember { mutableStateOf(false) }

    val presets = FormatPreset.entries

    Card(
        colors = CardDefaults.cardColors(containerColor = PlayceColors.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header - tap to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.setup_title),
                        color = PlayceColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = if (expanded) stringResource(R.string.setup_subtitle_expanded) else stringResource(R.string.setup_subtitle_collapsed),
                        color = PlayceColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = PlayceColors.TextSecondary,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Player names
                    Text(
                        text = stringResource(R.string.setup_player_names),
                        color = PlayceColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NameField(
                            value = playerAName,
                            onValueChange = {
                                playerAName = it.take(12)
                                sentToWatch = false
                            },
                            placeholder = stringResource(R.string.setup_player_a_hint),
                            modifier = Modifier.weight(1f)
                        )
                        NameField(
                            value = playerBName,
                            onValueChange = {
                                playerBName = it.take(12)
                                sentToWatch = false
                            },
                            placeholder = stringResource(R.string.setup_player_b_hint),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Format presets
                    Text(
                        text = stringResource(R.string.setup_match_format),
                        color = PlayceColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEachIndexed { index, preset ->
                            FormatChip(
                                label = preset.label,
                                subtitle = preset.subtitle,
                                selected = selectedPreset == index,
                                onClick = {
                                    selectedPreset = index
                                    sentToWatch = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryButton(
                            text = stringResource(R.string.setup_btn_apply),
                            onClick = {
                                onApplyConfig(playerAName, playerBName, presets[selectedPreset])
                            },
                            style = PrimaryButtonStyle.Outline,
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryButton(
                            text = if (sentToWatch) stringResource(R.string.setup_btn_sent) else stringResource(R.string.setup_btn_send_watch),
                            onClick = {
                                onSendToWatch(playerAName, playerBName, presets[selectedPreset])
                                sentToWatch = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (sentToWatch) {
                        Text(
                            text = stringResource(R.string.setup_sent_message),
                            color = PlayceColors.Accent,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = PlayceColors.TextSecondary)
        },
        singleLine = true,
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PlayceColors.TextPrimary,
            unfocusedTextColor = PlayceColors.TextPrimary,
            cursorColor = PlayceColors.Accent,
            focusedBorderColor = PlayceColors.Accent,
            unfocusedBorderColor = PlayceColors.Border
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun FormatChip(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) PlayceColors.AccentMuted else PlayceColors.Background
    val borderColor = if (selected) PlayceColors.Accent else PlayceColors.Border
    val textColor = if (selected) PlayceColors.Accent else PlayceColors.TextPrimary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            color = PlayceColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

enum class FormatPreset(
    val label: String,
    val subtitle: String,
    val setsToWin: Int,
    val tiebreakAtSixAll: Boolean,
    val superTiebreakInFinalSet: Boolean,
    val noAdScoring: Boolean
) {
    STANDARD("Standard", "Best of 3", 2, true, false, false),
    GRAND_SLAM("Grand Slam", "Best of 5", 3, true, false, false),
    FAST4("Fast4", "No-Ad + STB", 2, true, true, true);
}
