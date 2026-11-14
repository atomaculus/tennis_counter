package com.example.tenniscounter.mobile.ui.counter

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.data.local.TrainingSessionEntity
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import com.playce.shared.training.TrainingSession
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val VEINTIUNO_TARGET = 21

/**
 * Phone version of the "21" training counter. Same rules as the watch
 * screen: +1 up to 21, MISS resets the current streak and counts an
 * attempt, reaching 21 unlocks save-and-exit. Persists to the Room
 * `training_sessions` table instead of the watch's local DataStore.
 */
@Composable
fun MobileVeintiunoScreen(onExit: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var count by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var hasActivity by remember { mutableStateOf(false) }
    val sessionStartedAtElapsedRealtime = remember { SystemClock.elapsedRealtime() }
    val reachedTarget = count >= VEINTIUNO_TARGET

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
        val dao = MobileServiceLocator.trainingSessionDao(context.applicationContext)
        scope.launch {
            withContext(Dispatchers.IO) {
                dao.insert(session.toEntity())
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.veintiuno_title),
                color = PlayceColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "$count",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = if (reachedTarget) PlayceColors.Accent else PlayceColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.veintiuno_best_streak, bestStreak),
                color = PlayceColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            if (reachedTarget) {
                PrimaryButton(
                    text = stringResource(R.string.veintiuno_save_and_exit),
                    onClick = {
                        saveSession()
                        onExit()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = PrimaryButtonStyle.Solid
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.veintiuno_plus_one),
                        onClick = {
                            count += 1
                            hasActivity = true
                            if (count > bestStreak) bestStreak = count
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        style = PrimaryButtonStyle.Solid
                    )
                    PrimaryButton(
                        text = stringResource(R.string.veintiuno_miss),
                        onClick = {
                            attempts += 1
                            count = 0
                            hasActivity = true
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        style = PrimaryButtonStyle.Outline
                    )
                }
            }

            PrimaryButton(
                text = stringResource(R.string.veintiuno_exit_small),
                onClick = {
                    if (hasActivity) saveSession()
                    onExit()
                },
                modifier = Modifier.fillMaxWidth(),
                style = PrimaryButtonStyle.Outline
            )
        }
    }
}

private fun TrainingSession.toEntity(): TrainingSessionEntity = TrainingSessionEntity(
    id = id,
    modality = modality,
    createdAt = createdAt,
    durationSeconds = durationSeconds,
    targetPoints = targetPoints,
    finalCount = finalCount,
    bestStreak = bestStreak,
    attempts = attempts
)
