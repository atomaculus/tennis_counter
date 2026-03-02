package com.example.tenniscounter.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class mirroring the live match state broadcast by the scorer watch.
 */
data class WearLiveMatchState(
    val playerAPoints: Int,
    val playerAGames: Int,
    val playerASets: Int,
    val playerBPoints: Int,
    val playerBGames: Int,
    val playerBSets: Int,
    val completedSets: String,
    val pointLabelA: String,
    val pointLabelB: String,
    val elapsedSeconds: Int,
    val isMatchActive: Boolean,
    val lastScoredPlayer: String,
    val scorerNodeId: String,
    val timestamp: Long
)

/**
 * Observes DataClient changes on /playce/live for the spectator watch.
 * Only surfaces state when the scorer is a different node (not self).
 */
class LiveScoreObserver(
    private val context: Context,
    private val localNodeId: String
) : DataClient.OnDataChangedListener {

    private val _state = MutableStateFlow<WearLiveMatchState?>(null)
    val state: StateFlow<WearLiveMatchState?> = _state.asStateFlow()

    fun startListening() {
        Wearable.getDataClient(context).addListener(this)
        Log.d(TAG, "Started listening for live score (localNode=$localNodeId)")
    }

    fun stopListening() {
        Wearable.getDataClient(context).removeListener(this)
        _state.value = null
        Log.d(TAG, "Stopped listening for live score")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            if (path != LiveScoreBroadcaster.LIVE_PATH) continue

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val isActive = dataMap.getBoolean("isMatchActive", false)

            if (!isActive) {
                Log.d(TAG, "Live score cleared")
                _state.value = null
                continue
            }

            val scorerNode = dataMap.getString("scorerNodeId", "") ?: ""
            if (scorerNode == localNodeId) {
                // This is my own broadcast — ignore for spectator mode
                continue
            }

            val liveState = WearLiveMatchState(
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
                scorerNodeId = scorerNode,
                timestamp = dataMap.getLong("timestamp")
            )

            Log.d(TAG, "Spectator update: ${liveState.pointLabelA}-${liveState.pointLabelB} " +
                    "G:${liveState.playerAGames}-${liveState.playerBGames} " +
                    "last=${liveState.lastScoredPlayer}")

            _state.value = liveState
        }
    }

    companion object {
        private const val TAG = "LiveScoreObserver"
    }
}
