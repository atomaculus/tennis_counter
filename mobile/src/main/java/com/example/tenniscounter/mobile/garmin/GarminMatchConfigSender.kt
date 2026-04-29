package com.example.tenniscounter.mobile.garmin

import android.content.Context
import android.util.Log
import com.example.tenniscounter.mobile.di.MobileServiceLocator

class GarminMatchConfigSender(private val appContext: Context) {

    fun sendConfig(
        playerAName: String,
        playerBName: String,
        setsToWin: Int,
        tiebreakAtSixAll: Boolean,
        superTiebreakInFinalSet: Boolean,
        noAdScoring: Boolean,
        tiebreakPoints: Int = if (superTiebreakInFinalSet) 10 else 7
    ) {
        val timestamp = System.currentTimeMillis()
        val payload = HashMap<String, Any?>().apply {
            put(GarminConstants.CONFIG_PLAYER_A_NAME, playerAName)
            put(GarminConstants.CONFIG_PLAYER_B_NAME, playerBName)
            put(GarminConstants.CONFIG_SETS_TO_WIN, setsToWin)
            put(GarminConstants.CONFIG_TIEBREAK_AT_SIX_ALL, tiebreakAtSixAll)
            put(GarminConstants.CONFIG_TIEBREAK_POINTS, tiebreakPoints)
            put(GarminConstants.CONFIG_SUPER_TIEBREAK_IN_FINAL_SET, superTiebreakInFinalSet)
            put(GarminConstants.CONFIG_NO_AD_SCORING, noAdScoring)
            put(GarminConstants.CONFIG_TIMESTAMP, timestamp)
        }
        val envelope = HashMap<String, Any?>().apply {
            put(GarminConstants.ENVELOPE_PATH, GarminConstants.PATH_MATCH_CONFIG)
            put(GarminConstants.ENVELOPE_KIND, GarminConstants.KIND_MATCH_CONFIG)
            put(GarminConstants.ENVELOPE_PAYLOAD, payload)
            put(GarminConstants.ENVELOPE_TIMESTAMP, timestamp)
        }

        MobileServiceLocator
            .garminConnectivityManager(appContext)
            .sendMessage(envelope) { device, _, status ->
                Log.d(TAG, "Garmin config send status=$status to ${device.friendlyName}")
            }
    }

    private companion object {
        const val TAG = "GarminMatchConfig"
    }
}
