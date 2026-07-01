package com.example.tenniscounter.mobile.sync

import android.util.Log
import com.example.tenniscounter.mobile.BuildConfig
import com.example.tenniscounter.mobile.billing.PremiumAccessStore
import com.example.tenniscounter.mobile.data.local.MatchEntity
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.example.tenniscounter.mobile.health.HealthConnectMatchWriter
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WearMatchListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WearMatchListenerService created")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.i(
            TAG,
            "onMessageReceived path=${messageEvent.path} sourceNodeId=${messageEvent.sourceNodeId} dataSize=${messageEvent.data.size}"
        )
        if (messageEvent.path != MATCH_FINISHED_PATH) {
            Log.d(TAG, "Ignoring unexpected path=${messageEvent.path}")
            return
        }

        val dataMap = runCatching { DataMap.fromByteArray(messageEvent.data) }
            .getOrElse {
                Log.e(TAG, "Invalid payload for /match_finished", it)
                return
            }

        val createdAt = dataMap.getLong(KEY_CREATED_AT, -1L)
        val durationSeconds = dataMap.getLong(KEY_DURATION_SECONDS, -1L)
        val finalScoreText = dataMap.getString(KEY_FINAL_SCORE_TEXT).orEmpty()
        val setScoresText = dataMap.getString(KEY_SET_SCORES_TEXT)?.trim().orEmpty().ifBlank { null }
        val idempotencyKey = dataMap.getString(KEY_IDEMPOTENCY_KEY).orEmpty()
        val playerAName = dataMap.getString(KEY_PLAYER_A_NAME)?.trim().orEmpty().ifBlank { null }
        val playerBName = dataMap.getString(KEY_PLAYER_B_NAME)?.trim().orEmpty().ifBlank { null }
        val caloriesKcal = dataMap.getOptionalDouble(KEY_CALORIES_KCAL)
        val avgHeartRateBpm = dataMap.getOptionalInt(KEY_AVG_HEART_RATE_BPM)
        val maxHeartRateBpm = dataMap.getOptionalInt(KEY_MAX_HEART_RATE_BPM)
        Log.i(
            TAG,
            "Decoded payload createdAt=$createdAt durationSeconds=$durationSeconds finalScoreText=$finalScoreText " +
                "setScoresText=${setScoresText.orEmpty()} idempotencyKey=$idempotencyKey caloriesKcal=$caloriesKcal " +
                "avgHeartRateBpm=$avgHeartRateBpm maxHeartRateBpm=$maxHeartRateBpm"
        )

        if (createdAt <= 0L || durationSeconds < 0L || finalScoreText.isBlank()) {
            Log.w(TAG, "Ignoring /match_finished with missing required fields")
            return
        }
        if (idempotencyKey.isBlank()) {
            Log.e(TAG, "Ignoring /match_finished with missing idempotencyKey. sourceNodeId=${messageEvent.sourceNodeId}")
            return
        }

        serviceScope.launch {
            if (!BuildConfig.DEBUG && !PremiumAccessStore.isPremiumUnlocked(applicationContext)) {
                Log.i(TAG, "Premium locked: match not saved. idempotencyKey=$idempotencyKey")
                sendAck(messageEvent.sourceNodeId, idempotencyKey, status = "premium_locked")
                return@launch
            }

            val inserted = MobileServiceLocator.matchRepository(applicationContext).insertIfNotExists(
                createdAt = createdAt,
                durationSeconds = durationSeconds,
                finalScoreText = finalScoreText,
                idempotencyKey = idempotencyKey,
                setScoresText = setScoresText,
                photoUri = null,
                playerAName = playerAName,
                playerBName = playerBName,
                caloriesKcal = caloriesKcal,
                avgHeartRateBpm = avgHeartRateBpm,
                maxHeartRateBpm = maxHeartRateBpm
            )

            if (inserted) {
                Log.i(TAG, "Match inserted from wear. idempotencyKey=$idempotencyKey")
                HealthConnectMatchWriter(applicationContext).writeTennisSessionIfPermitted(
                    MatchEntity(
                        createdAt = createdAt,
                        durationSeconds = durationSeconds,
                        finalScoreText = finalScoreText,
                        setScoresText = setScoresText,
                        photoUri = null,
                        idempotencyKey = idempotencyKey,
                        playerAName = playerAName,
                        playerBName = playerBName,
                        caloriesKcal = caloriesKcal,
                        avgHeartRateBpm = avgHeartRateBpm,
                        maxHeartRateBpm = maxHeartRateBpm
                    )
                )
                sendAck(messageEvent.sourceNodeId, idempotencyKey, status = "inserted")
            } else {
                Log.i(TAG, "Duplicate match ignored. idempotencyKey=$idempotencyKey")
                sendAck(messageEvent.sourceNodeId, idempotencyKey, status = "duplicate")
            }
        }
    }

    private fun sendAck(sourceNodeId: String, idempotencyKey: String, status: String) {
        val ackPayload = DataMap().apply {
            putString(KEY_IDEMPOTENCY_KEY, idempotencyKey)
            putString(KEY_STATUS, status)
        }.toByteArray()

        runCatching {
            Tasks.await(
                Wearable.getMessageClient(applicationContext).sendMessage(
                    sourceNodeId,
                    MATCH_FINISHED_ACK_PATH,
                    ackPayload
                )
            )
        }.onSuccess {
            Log.i(TAG, "ACK sent sourceNodeId=$sourceNodeId idempotencyKey=$idempotencyKey status=$status")
        }.onFailure {
            Log.e(TAG, "ACK send failed sourceNodeId=$sourceNodeId idempotencyKey=$idempotencyKey status=$status", it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "WearMatchListenerService destroyed")
        serviceScope.cancel()
    }

    private companion object {
        const val TAG = "WearMatchListener"
        const val MATCH_FINISHED_PATH = "/match_finished"
        const val MATCH_FINISHED_ACK_PATH = "/match_finished_ack"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_DURATION_SECONDS = "durationSeconds"
        const val KEY_FINAL_SCORE_TEXT = "finalScoreText"
        const val KEY_SET_SCORES_TEXT = "setScoresText"
        const val KEY_IDEMPOTENCY_KEY = "idempotencyKey"
        const val KEY_PLAYER_A_NAME = "playerAName"
        const val KEY_PLAYER_B_NAME = "playerBName"
        const val KEY_STATUS = "status"
        const val KEY_CALORIES_KCAL = "caloriesKcal"
        const val KEY_AVG_HEART_RATE_BPM = "avgHeartRateBpm"
        const val KEY_MAX_HEART_RATE_BPM = "maxHeartRateBpm"
    }
}

private fun DataMap.getOptionalDouble(key: String): Double? {
    return if (containsKey(key)) getDouble(key) else null
}

private fun DataMap.getOptionalInt(key: String): Int? {
    return if (containsKey(key)) getInt(key) else null
}
