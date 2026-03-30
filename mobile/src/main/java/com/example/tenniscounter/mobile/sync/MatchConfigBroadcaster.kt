package com.example.tenniscounter.mobile.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/**
 * Sends match configuration (player names + format) from the phone to the watch
 * via DataClient on path /playce/match-config.
 */
class MatchConfigBroadcaster(private val context: Context) {

    fun sendConfig(
        playerAName: String,
        playerBName: String,
        setsToWin: Int,
        tiebreakAtSixAll: Boolean,
        superTiebreakInFinalSet: Boolean,
        noAdScoring: Boolean
    ) {
        val request = PutDataMapRequest.create(CONFIG_PATH).apply {
            dataMap.putString("playerAName", playerAName)
            dataMap.putString("playerBName", playerBName)
            dataMap.putInt("setsToWin", setsToWin)
            dataMap.putBoolean("tiebreakAtSixAll", tiebreakAtSixAll)
            dataMap.putBoolean("superTiebreakInFinalSet", superTiebreakInFinalSet)
            dataMap.putBoolean("noAdScoring", noAdScoring)
            // timestamp forces propagation even when the rest of the payload is identical
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener {
                Log.d(TAG, "Match config sent: $playerAName vs $playerBName, sets=$setsToWin")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Match config send failed", e)
            }
    }

    companion object {
        const val CONFIG_PATH = "/playce/match-config"
        private const val TAG = "MatchConfigBroadcaster"
    }
}
