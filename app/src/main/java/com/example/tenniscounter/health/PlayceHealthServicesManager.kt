package com.example.tenniscounter.health

import android.content.Context
import android.util.Log
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class HealthMetricsSnapshot(
    val caloriesKcal: Double? = null,
    val avgHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null
) {
    fun hasAnyValue(): Boolean = caloriesKcal != null || avgHeartRateBpm != null || maxHeartRateBpm != null
}

class PlayceHealthServicesManager(context: Context) {
    private val appContext = context.applicationContext
    private val exerciseClient: ExerciseClient by lazy {
        HealthServices.getClient(appContext).exerciseClient
    }

    @Volatile
    private var isStarted = false

    @Volatile
    private var latestMetrics = HealthMetricsSnapshot()

    private var heartRateSampleSum = 0.0
    private var heartRateSampleCount = 0
    private var heartRateSampleMax: Double? = null

    private val callback = object : ExerciseUpdateCallback {
        override fun onRegistered() {
            Log.i(TAG, "Health Services callback registered")
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.w(TAG, "Health Services callback registration failed", throwable)
        }

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            latestMetrics = update.toMetricsSnapshot()
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onAvailabilityChanged(
            dataType: DataType<*, *>,
            availability: Availability
        ) = Unit
    }

    suspend fun startMatchWorkoutIfPossible() {
        if (isStarted) return

        withContext(Dispatchers.IO) {
            runCatching {
                val capabilities = exerciseClient.getCapabilitiesAsync().get()
                if (ExerciseType.TENNIS !in capabilities.supportedExerciseTypes) {
                    Log.i(TAG, "Health Services tennis exercise is not supported")
                    return@withContext
                }

                val tennisCapabilities = capabilities.getExerciseTypeCapabilities(ExerciseType.TENNIS)
                val requestedDataTypes = setOf(
                    DataType.CALORIES_TOTAL,
                    DataType.HEART_RATE_BPM_STATS,
                    DataType.HEART_RATE_BPM
                ).filter { dataType ->
                    dataType in tennisCapabilities.supportedDataTypes
                }.toSet()

                resetMetrics()
                exerciseClient.setUpdateCallback(callback)
                val config = ExerciseConfig.builder(ExerciseType.TENNIS)
                    .setDataTypes(requestedDataTypes)
                    .setIsGpsEnabled(false)
                    .setIsAutoPauseAndResumeEnabled(false)
                    .build()
                exerciseClient.startExerciseAsync(config).get()
                isStarted = true
                Log.i(TAG, "Health Services tennis session started dataTypes=$requestedDataTypes")
            }.onFailure {
                isStarted = false
                Log.w(TAG, "Health Services tennis session could not start", it)
            }
        }
    }

    suspend fun endWorkoutAndGetMetrics(): HealthMetricsSnapshot {
        if (!isStarted) return latestMetrics

        return withContext(Dispatchers.IO) {
            runCatching {
                exerciseClient.flushAsync().get()
                exerciseClient.endExerciseAsync().get()
                exerciseClient.clearUpdateCallbackAsync(callback).get()
            }.onFailure {
                Log.w(TAG, "Health Services session end failed", it)
            }
            isStarted = false
            latestMetrics
        }
    }

    private fun resetMetrics() {
        latestMetrics = HealthMetricsSnapshot()
        heartRateSampleSum = 0.0
        heartRateSampleCount = 0
        heartRateSampleMax = null
    }

    private fun ExerciseUpdate.toMetricsSnapshot(): HealthMetricsSnapshot {
        val previousMetrics = this@PlayceHealthServicesManager.latestMetrics
        val calories = getCaloriesKcal(this) ?: previousMetrics.caloriesKcal
        accumulateHeartRateSamples(this)
        val heartRateStats = getHeartRateStats(this) ?: getAccumulatedHeartRateStats()
        return HealthMetricsSnapshot(
            caloriesKcal = calories,
            avgHeartRateBpm = heartRateStats?.first ?: previousMetrics.avgHeartRateBpm,
            maxHeartRateBpm = heartRateStats?.second ?: previousMetrics.maxHeartRateBpm
        )
    }

    private fun getCaloriesKcal(update: ExerciseUpdate): Double? {
        return runCatching {
            update.latestMetrics.getData(DataType.CALORIES_TOTAL)?.total
        }.getOrNull()
    }

    private fun accumulateHeartRateSamples(update: ExerciseUpdate) {
        runCatching {
            update.latestMetrics.getData(DataType.HEART_RATE_BPM)
                .mapNotNull { sample -> sample.value.takeIf { it > 0.0 } }
                .forEach { value ->
                    heartRateSampleSum += value
                    heartRateSampleCount += 1
                    heartRateSampleMax = maxOf(heartRateSampleMax ?: value, value)
                }
        }.onFailure {
            Log.w(TAG, "Health Services heart rate samples could not be read", it)
        }
    }

    private fun getHeartRateStats(update: ExerciseUpdate): Pair<Int, Int>? {
        return runCatching {
            update.latestMetrics.getData(DataType.HEART_RATE_BPM_STATS)?.let { stats ->
                stats.average.toInt() to stats.max.toInt()
            }
        }.getOrNull()
    }

    private fun getAccumulatedHeartRateStats(): Pair<Int, Int>? {
        if (heartRateSampleCount <= 0) return null
        val avg = (heartRateSampleSum / heartRateSampleCount).roundToInt()
        val max = heartRateSampleMax?.roundToInt() ?: return null
        return avg to max
    }

    private companion object {
        const val TAG = "PlayceHealthServices"
    }
}
