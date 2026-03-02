package com.example.tenniscounter.mobile.sync

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives DataItem changes on /playce/live from the scorer watch
 * and pushes them into [LiveScoreRepository].
 */
class LiveScoreListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val path = event.dataItem.uri.path ?: continue
            if (path != LIVE_PATH) continue

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val isActive = dataMap.getBoolean("isMatchActive", false)

            if (!isActive) {
                Log.d(TAG, "Live score cleared (isMatchActive=false)")
                LiveScoreRepository.clear()
                continue
            }

            val liveState = LiveMatchState(
                playerAPoints = dataMap.getInt("playerA_points"),
                playerAGames = dataMap.getInt("playerA_games"),
                playerASets = dataMap.getInt("playerA_sets"),
                playerBPoints = dataMap.getInt("playerB_points"),
                playerBGames = dataMap.getInt("playerB_games"),
                playerBSets = dataMap.getInt("playerB_sets"),
                completedSets = dataMap.getString("completedSets", "") ?: "",
                pointLabelA = dataMap.getString("pointLabelA", "0") ?: "0",
                pointLabelB = dataMap.getString("pointLabelB", "0") ?: "0",
                elapsedSeconds = dataMap.getInt("elapsedSeconds"),
                isMatchActive = true,
                lastScoredPlayer = dataMap.getString("lastScoredPlayer", "") ?: "",
                scorerNodeId = dataMap.getString("scorerNodeId", "") ?: "",
                timestamp = dataMap.getLong("timestamp")
            )

            Log.d(TAG, "Live score update: ${liveState.pointLabelA}-${liveState.pointLabelB} " +
                    "G:${liveState.playerAGames}-${liveState.playerBGames} " +
                    "S:${liveState.playerASets}-${liveState.playerBSets} " +
                    "last=${liveState.lastScoredPlayer}")

            LiveScoreRepository.update(liveState)
        }
    }

    companion object {
        private const val TAG = "LiveScoreListener"
        private const val LIVE_PATH = "/playce/live"
    }
}
