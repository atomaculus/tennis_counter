package com.example.tenniscounter.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.tenniscounter.R
import com.example.tenniscounter.training.TrainingSessionStore
import com.example.tenniscounter.ui.components.PlayceButton
import com.example.tenniscounter.ui.components.PlayceButtonVariant
import com.example.tenniscounter.ui.components.PlayceChip
import com.example.tenniscounter.ui.components.PlayceWearColors
import com.example.tenniscounter.ui.components.PlayceWearSpacing
import com.playce.shared.training.TrainingSession
import java.util.UUID
import kotlinx.coroutines.launch

private const val VEINTIUNO_TARGET = 21

/**
 * Minimal "21" training counter: tap +1 up to 21, MISS resets the current
 * streak and counts an attempt, and reaching 21 unlocks a save-and-exit
 * state. No scroll (project rule): everything fits a static viewport.
 */
@Composable
fun VeintiunoScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var count by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var hasActivity by remember { mutableStateOf(false) }
    val sessionStartedAtElapsedRealtime = remember { SystemClock.elapsedRealtime() }
    val reachedTarget by remember { derivedStateOf { count >= VEINTIUNO_TARGET } }

    LaunchedEffect(reachedTarget) {
        if (reachedTarget) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun saveSession() {
        val durationSeconds = (SystemClock.elapsedRealtime() - sessionStartedAtElapsedRealtime) / 1000L
        val session = TrainingSession(
            id = UUID.randomUUID().toString(),
            modality = TrainingSession.MODALITY_VEINTIUNO,
            createdAt = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            targetPoints = VEINTIUNO_TARGET,
            finalCount = count,
            bestStreak = bestStreak,
            attempts = attempts + 1 // the run in progress (or just finished) counts as an attempt
        )
        uiScope.launch { TrainingSessionStore.addSession(context, session) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayceWearColors.Background)
            .padding(horizontal = PlayceWearSpacing.Md, vertical = PlayceWearSpacing.Sm),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PlayceWearSpacing.Xs)
        ) {
            PlayceChip(text = stringResource(R.string.veintiuno_title), accent = reachedTarget)
            Text21Count(count = count, reachedTarget = reachedTarget)
            Text21BestStreak(bestStreak = bestStreak)

            if (reachedTarget) {
                PlayceButton(
                    text = stringResource(R.string.veintiuno_save_and_exit),
                    onClick = {
                        saveSession()
                        onExit()
                    },
                    variant = PlayceButtonVariant.Primary
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PlayceWearSpacing.Sm)
                ) {
                    PlayceButton(
                        text = stringResource(R.string.veintiuno_plus_one),
                        onClick = {
                            count += 1
                            hasActivity = true
                            if (count > bestStreak) bestStreak = count
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.weight(1f),
                        variant = PlayceButtonVariant.Primary
                    )
                    PlayceButton(
                        text = stringResource(R.string.veintiuno_miss),
                        onClick = {
                            attempts += 1
                            count = 0
                            hasActivity = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.weight(1f),
                        variant = PlayceButtonVariant.Secondary
                    )
                }
            }

            PlayceButton(
                text = stringResource(R.string.veintiuno_exit_small),
                onClick = {
                    if (hasActivity) saveSession()
                    onExit()
                },
                variant = PlayceButtonVariant.Secondary
            )
        }
    }
}

@Composable
private fun Text21Count(count: Int, reachedTarget: Boolean) {
    androidx.wear.compose.material.Text(
        text = "$count",
        fontSize = 52.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        color = if (reachedTarget) PlayceWearColors.Accent else PlayceWearColors.TextPrimary
    )
}

@Composable
private fun Text21BestStreak(bestStreak: Int) {
    androidx.wear.compose.material.Text(
        text = stringResource(R.string.veintiuno_best_streak, bestStreak),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = PlayceWearColors.TextSecondary
    )
}
