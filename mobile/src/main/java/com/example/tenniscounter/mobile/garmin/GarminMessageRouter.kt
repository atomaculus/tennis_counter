package com.example.tenniscounter.mobile.garmin

import android.content.Context
import android.util.Log
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice

class GarminMessageRouter(private val appContext: Context) {

    private val liveScoreHandler = GarminLiveScoreHandler()
    private val finishedMatchHandler = GarminFinishedMatchHandler(appContext)

    fun route(device: IQDevice, app: IQApp, envelope: Map<String, Any?>) {
        Log.d(TAG, "Routing Garmin envelope from ${device.friendlyName}: $envelope")
        val path = GarminPayloadCodec.getString(envelope, GarminConstants.ENVELOPE_PATH)
        if (path.isBlank()) {
            Log.w(TAG, "Envelope without path from ${device.friendlyName}")
            return
        }
        val payload = GarminPayloadCodec.asPayloadMap(envelope[GarminConstants.ENVELOPE_PAYLOAD])
        if (payload == null) {
            Log.w(TAG, "Envelope payload missing or wrong type for path=$path")
            return
        }
        Log.d(
            TAG,
            "Decoded Garmin envelope path=$path payloadKeys=${payload.keys.joinToString()} app=$app"
        )

        when (path) {
            GarminConstants.PATH_LIVE_SCORE -> {
                liveScoreHandler.handle(payload)
            }
            GarminConstants.PATH_MATCH_FINISHED -> {
                finishedMatchHandler.handle(device, app, envelope, payload)
            }
            else -> {
                Log.d(TAG, "Ignoring unhandled path=$path from ${device.friendlyName}")
            }
        }
    }

    private companion object {
        const val TAG = "GarminMessageRouter"
    }
}
