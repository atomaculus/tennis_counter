package com.example.tenniscounter.mobile.garmin

import android.content.Context
import android.util.Log
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice

class GarminAckSender(private val appContext: Context) {

    fun send(device: IQDevice, app: IQApp, idempotencyKey: String, status: String) {
        val envelope = HashMap<String, Any?>().apply {
            put(GarminConstants.ENVELOPE_PATH, GarminConstants.PATH_MATCH_FINISHED_ACK)
            put(GarminConstants.ENVELOPE_KIND, GarminConstants.KIND_FINISHED_MATCH_ACK)
            put(
                GarminConstants.ENVELOPE_PAYLOAD,
                HashMap<String, Any?>().apply {
                    put(GarminConstants.ACK_IDEMPOTENCY_KEY, idempotencyKey)
                    put(GarminConstants.ACK_STATUS, status)
                }
            )
            put(GarminConstants.ENVELOPE_IDEMPOTENCY_KEY, idempotencyKey)
            put(GarminConstants.ENVELOPE_TIMESTAMP, System.currentTimeMillis())
        }
        Log.i(
            TAG,
            "Sending Garmin ACK to ${device.friendlyName} appId=${GarminConstants.APP_ID} " +
                "idempotencyKey=$idempotencyKey status=$status envelope=$envelope"
        )

        val connectIQ = MobileServiceLocator
            .garminConnectivityManager(appContext)
            .connectIQ()

        runCatching {
            connectIQ.sendMessage(device, app, envelope) { d, _, sendStatus ->
                Log.d(
                    TAG,
                    "ACK sent device=${d.friendlyName} idempotencyKey=$idempotencyKey " +
                        "status=$status transport=$sendStatus"
                )
            }
        }.onFailure {
            Log.w(TAG, "ACK send threw for ${device.friendlyName} idempotencyKey=$idempotencyKey", it)
        }
    }

    private companion object {
        const val TAG = "GarminAckSender"
    }
}
