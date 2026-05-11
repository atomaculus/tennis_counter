package com.example.tenniscounter.mobile.garmin

import android.util.Log
import com.example.tenniscounter.mobile.sync.LiveMatchState
import com.example.tenniscounter.mobile.sync.LiveScoreRepository

class GarminLiveScoreHandler {

    fun handle(payload: Map<String, Any?>) {
        val isActive = GarminPayloadCodec.getBoolean(payload, GarminConstants.LIVE_IS_MATCH_ACTIVE, default = false)
        if (!isActive) {
            Log.d(TAG, "Live score cleared from Garmin (isMatchActive=false)")
            LiveScoreRepository.clear()
            return
        }

        val state = LiveMatchState(
            playerAPoints = GarminPayloadCodec.getInt(payload, GarminConstants.LIVE_PLAYER_A_POINTS),
            playerAGames = GarminPayloadCodec.getInt(payload, GarminConstants.LIVE_PLAYER_A_GAMES),
            playerASets = GarminPayloadCodec.getInt(payload, GarminConstants.LIVE_PLAYER_A_SETS),
            playerBPoints = GarminPayloadCodec.getInt(payload, GarminConstants.LIVE_PLAYER_B_POINTS),
            playerBGames = GarminPayloadCodec.getInt(payload, GarminConstants.LIVE_PLAYER_B_GAMES),
            playerBSets = GarminPayloadCodec.getInt(payload, GarminConstants.LIVE_PLAYER_B_SETS),
            completedSets = GarminPayloadCodec.getString(payload, GarminConstants.LIVE_COMPLETED_SETS, ""),
            pointLabelA = GarminPayloadCodec.getString(payload, GarminConstants.LIVE_POINT_LABEL_A, "0"),
            pointLabelB = GarminPayloadCodec.getString(payload, GarminConstants.LIVE_POINT_LABEL_B, "0"),
            elapsedSeconds = GarminPayloadCodec.getInt(payload, GarminConstants.LIVE_ELAPSED_SECONDS),
            isMatchActive = true,
            lastScoredPlayer = GarminPayloadCodec.getString(payload, GarminConstants.LIVE_LAST_SCORED_PLAYER, ""),
            scorerNodeId = GarminPayloadCodec.getString(payload, GarminConstants.LIVE_SCORER_NODE_ID, ""),
            timestamp = GarminPayloadCodec.getLong(payload, GarminConstants.LIVE_TIMESTAMP)
        )

        Log.d(
            TAG,
            "Live score from Garmin: ${state.pointLabelA}-${state.pointLabelB} " +
                "G:${state.playerAGames}-${state.playerBGames} " +
                "S:${state.playerASets}-${state.playerBSets} " +
                "last=${state.lastScoredPlayer}"
        )

        LiveScoreRepository.update(state)
    }

    private companion object {
        const val TAG = "GarminLiveScoreHandler"
    }
}
