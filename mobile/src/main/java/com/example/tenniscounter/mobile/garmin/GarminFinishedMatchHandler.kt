package com.example.tenniscounter.mobile.garmin

import android.content.Context
import android.util.Log
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GarminFinishedMatchHandler(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ackSender = GarminAckSender(appContext)

    fun handle(
        device: IQDevice,
        app: IQApp,
        envelope: Map<String, Any?>,
        payload: Map<String, Any?>
    ) {
        val createdAt = GarminPayloadCodec.getLong(payload, GarminConstants.FINISHED_CREATED_AT, default = -1L)
        val durationSeconds = GarminPayloadCodec.getLong(payload, GarminConstants.FINISHED_DURATION_SECONDS, default = -1L)
        val finalScoreText = GarminPayloadCodec.getString(payload, GarminConstants.FINISHED_FINAL_SCORE_TEXT)
        val setScoresText = GarminPayloadCodec.getStringOrNull(payload, GarminConstants.FINISHED_SET_SCORES_TEXT)
        val idempotencyKey = GarminPayloadCodec.getString(payload, GarminConstants.FINISHED_IDEMPOTENCY_KEY)
            .ifBlank { GarminPayloadCodec.getString(envelope, GarminConstants.ENVELOPE_IDEMPOTENCY_KEY) }

        Log.i(
            TAG,
            "Decoded Garmin finished payload createdAt=$createdAt durationSeconds=$durationSeconds " +
                "finalScoreText=$finalScoreText idempotencyKey=$idempotencyKey payloadKeys=${payload.keys.joinToString()}"
        )

        if (createdAt <= 0L || durationSeconds < 0L || finalScoreText.isBlank()) {
            Log.w(TAG, "Ignoring Garmin /match_finished with missing required fields")
            return
        }
        if (idempotencyKey.isBlank()) {
            Log.e(TAG, "Ignoring Garmin /match_finished with missing idempotencyKey from ${device.friendlyName}")
            return
        }

        scope.launch {
            val inserted = MobileServiceLocator
                .matchRepository(appContext)
                .insertIfNotExists(
                    createdAt = createdAt,
                    durationSeconds = durationSeconds,
                    finalScoreText = finalScoreText,
                    idempotencyKey = idempotencyKey,
                    setScoresText = setScoresText,
                    photoUri = null
                )

            if (inserted) {
                Log.i(TAG, "Garmin match inserted. idempotencyKey=$idempotencyKey")
                ackSender.send(device, app, idempotencyKey, GarminConstants.ACK_STATUS_INSERTED)
            } else {
                Log.i(TAG, "Duplicate Garmin match ignored. idempotencyKey=$idempotencyKey")
                ackSender.send(device, app, idempotencyKey, GarminConstants.ACK_STATUS_DUPLICATE)
            }
        }
    }

    private companion object {
        const val TAG = "GarminFinishedMatch"
    }
}
