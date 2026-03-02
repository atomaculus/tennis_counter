package com.example.tenniscounter.sync

import android.content.Context
import android.util.Log
import com.example.tenniscounter.ui.MatchState
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/**
 * Broadcasts the live match state to all connected nodes (phone + spectator watches)
 * via DataClient. Uses [PutDataMapRequest.setUrgent] for low-latency delivery.
 *
 * Path: /playce/live
 */
class LiveScoreBroadcaster(private val context: Context) {

    /**
     * Writes the current match state into a DataItem so that all connected
     * nodes (phone, spectator watches) receive it automatically.
     */
    fun broadcastState(
        state: MatchState,
        lastScoredPlayer: String,
        scorerNodeId: String
    ) {
        val request = PutDataMapRequest.create(LIVE_PATH).apply {
            dataMap.putInt("playerA_points", state.playerA.points)
            dataMap.putInt("playerA_games", state.playerA.games)
            dataMap.putInt("playerA_sets", state.playerA.sets)
            dataMap.putInt("playerB_points", state.playerB.points)
            dataMap.putInt("playerB_games", state.playerB.games)
            dataMap.putInt("playerB_sets", state.playerB.sets)
            dataMap.putString(
                "completedSets",
                state.completedSets.joinToString(",") { "${it.a}-${it.b}" }
            )
            dataMap.putString("pointLabelA", state.pointLabelForA())
            dataMap.putString("pointLabelB", state.pointLabelForB())
            dataMap.putInt("elapsedSeconds", state.elapsedSeconds)
            dataMap.putBoolean("isMatchActive", true)
            dataMap.putString("lastScoredPlayer", lastScoredPlayer)
            dataMap.putString("scorerNodeId", scorerNodeId)
            // timestamp forces propagation even when the rest of the payload is identical
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener {
                Log.d(TAG, "Live score broadcast OK (last=$lastScoredPlayer)")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Live score broadcast failed", e)
            }
    }

    /** Marks the match as inactive so observers clear their UI. */
    fun clearLiveScore() {
        val request = PutDataMapRequest.create(LIVE_PATH).apply {
            dataMap.putBoolean("isMatchActive", false)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener {
                Log.d(TAG, "Live score cleared")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Live score clear failed", e)
            }
    }

    companion object {
        const val LIVE_PATH = "/playce/live"
        private const val TAG = "LiveScoreBroadcast"
    }
}
