package com.example.tenniscounter.sync

import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearAckListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != MATCH_FINISHED_ACK_PATH) {
            return
        }
        val dataMap = runCatching { DataMap.fromByteArray(messageEvent.data) }
            .getOrElse {
                Log.e(TAG, "Invalid ACK payload sourceNodeId=${messageEvent.sourceNodeId}", it)
                return
            }

        val idempotencyKey = dataMap.getString(KEY_IDEMPOTENCY_KEY).orEmpty()
        val status = dataMap.getString(KEY_STATUS).orEmpty()
        if (idempotencyKey.isBlank()) {
            Log.e(TAG, "ACK missing idempotencyKey sourceNodeId=${messageEvent.sourceNodeId}")
            return
        }

        val cleared = PendingMatchStore.clearIfMatches(applicationContext, idempotencyKey)
        Log.i(
            TAG,
            "ACK received idempotencyKey=$idempotencyKey status=${status.ifBlank { "n/a" }} sourceNodeId=${messageEvent.sourceNodeId} clearedPending=$cleared"
        )
    }

    private companion object {
        const val TAG = "WearAckListener"
        const val MATCH_FINISHED_ACK_PATH = "/match_finished_ack"
        const val KEY_IDEMPOTENCY_KEY = "idempotencyKey"
        const val KEY_STATUS = "status"
    }
}
