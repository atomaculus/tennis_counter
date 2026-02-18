package com.example.tenniscounter.mobile.sync

import android.util.Log
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
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
        Log.i(
            TAG,
            "Decoded payload createdAt=$createdAt durationSeconds=$durationSeconds finalScoreText=$finalScoreText setScoresText=${setScoresText.orEmpty()} idempotencyKey=$idempotencyKey"
        )

        if (createdAt <= 0L || durationSeconds < 0L || finalScoreText.isBlank()) {
            Log.w(TAG, "Ignoring /match_finished with missing required fields")
            return
        }

        serviceScope.launch {
            val inserted = MobileServiceLocator.matchRepository(applicationContext).insertIfNotExists(
                createdAt = createdAt,
                durationSeconds = durationSeconds,
                finalScoreText = finalScoreText,
                setScoresText = setScoresText,
                photoUri = null
            )

            if (inserted) {
                Log.i(TAG, "Match inserted from wear. idempotencyKey=$idempotencyKey")
            } else {
                Log.i(TAG, "Duplicate match ignored. idempotencyKey=$idempotencyKey")
            }
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
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_DURATION_SECONDS = "durationSeconds"
        const val KEY_FINAL_SCORE_TEXT = "finalScoreText"
        const val KEY_SET_SCORES_TEXT = "setScoresText"
        const val KEY_IDEMPOTENCY_KEY = "idempotencyKey"
    }
}
