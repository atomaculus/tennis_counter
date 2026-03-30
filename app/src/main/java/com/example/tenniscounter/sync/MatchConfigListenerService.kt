package com.example.tenniscounter.sync

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.playce.shared.scoring.MatchFormat

/**
 * Receives match configuration sent by the phone via DataClient
 * on path /playce/match-config and pushes it into [MatchConfigRepository].
 */
class MatchConfigListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val path = event.dataItem.uri.path ?: continue
            if (path != CONFIG_PATH) continue

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

            val playerAName = dataMap.getString("playerAName", "Player A")
            val playerBName = dataMap.getString("playerBName", "Player B")
            val setsToWin = dataMap.getInt("setsToWin", 2)
            val tiebreakAtSixAll = dataMap.getBoolean("tiebreakAtSixAll", true)
            val superTiebreakInFinalSet = dataMap.getBoolean("superTiebreakInFinalSet", false)
            val noAdScoring = dataMap.getBoolean("noAdScoring", false)
            val timestamp = dataMap.getLong("timestamp")

            val format = MatchFormat(
                setsToWin = setsToWin,
                tiebreakAtSixAll = tiebreakAtSixAll,
                superTiebreakInFinalSet = superTiebreakInFinalSet,
                noAdScoring = noAdScoring
            )

            val config = MatchConfig(
                playerAName = playerAName,
                playerBName = playerBName,
                format = format,
                timestamp = timestamp
            )

            Log.d(TAG, "Match config received: $playerAName vs $playerBName, " +
                "sets=${setsToWin}, tb=$tiebreakAtSixAll, superTB=$superTiebreakInFinalSet, noAd=$noAdScoring")

            MatchConfigRepository.update(config)
        }
    }

    companion object {
        private const val TAG = "MatchConfigListener"
        private const val CONFIG_PATH = "/playce/match-config"
    }
}
