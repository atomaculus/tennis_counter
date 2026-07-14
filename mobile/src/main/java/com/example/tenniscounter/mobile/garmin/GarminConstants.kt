package com.example.tenniscounter.mobile.garmin

object GarminConstants {
    // Must match playce_garmin/manifest.xml exactly or Connect IQ app events never bind.
    const val APP_ID = "c4f18a72b93e4d6fa1c8e5b2079d3a44"

    const val PATH_MATCH_CONFIG = "/playce/match-config"
    const val PATH_LIVE_SCORE = "/playce/live"
    const val PATH_MATCH_FINISHED = "/match_finished"
    const val PATH_MATCH_FINISHED_ACK = "/match_finished_ack"

    const val KIND_MATCH_CONFIG = "match_config"
    const val KIND_LIVE_SCORE = "live_score"
    const val KIND_FINISHED_MATCH = "finished_match"
    const val KIND_FINISHED_MATCH_ACK = "finished_match_ack"

    const val ENVELOPE_PATH = "path"
    const val ENVELOPE_KIND = "kind"
    const val ENVELOPE_PAYLOAD = "payload"
    const val ENVELOPE_IDEMPOTENCY_KEY = "idempotencyKey"
    const val ENVELOPE_TIMESTAMP = "timestamp"

    const val LIVE_PLAYER_A_POINTS = "playerA_points"
    const val LIVE_PLAYER_A_GAMES = "playerA_games"
    const val LIVE_PLAYER_A_SETS = "playerA_sets"
    const val LIVE_PLAYER_B_POINTS = "playerB_points"
    const val LIVE_PLAYER_B_GAMES = "playerB_games"
    const val LIVE_PLAYER_B_SETS = "playerB_sets"
    const val LIVE_COMPLETED_SETS = "completedSets"
    const val LIVE_POINT_LABEL_A = "pointLabelA"
    const val LIVE_POINT_LABEL_B = "pointLabelB"
    const val LIVE_ELAPSED_SECONDS = "elapsedSeconds"
    const val LIVE_IS_MATCH_ACTIVE = "isMatchActive"
    const val LIVE_LAST_SCORED_PLAYER = "lastScoredPlayer"
    const val LIVE_SCORER_NODE_ID = "scorerNodeId"
    const val LIVE_TIMESTAMP = "timestamp"

    const val FINISHED_CREATED_AT = "createdAt"
    const val FINISHED_DURATION_SECONDS = "durationSeconds"
    const val FINISHED_FINAL_SCORE_TEXT = "finalScoreText"
    const val FINISHED_SET_SCORES_TEXT = "setScoresText"
    const val FINISHED_IDEMPOTENCY_KEY = "idempotencyKey"
    const val FINISHED_PLAYER_A_NAME = "playerAName"
    const val FINISHED_PLAYER_B_NAME = "playerBName"

    const val CONFIG_PLAYER_A_NAME = "playerAName"
    const val CONFIG_PLAYER_B_NAME = "playerBName"
    const val CONFIG_SETS_TO_WIN = "setsToWin"
    const val CONFIG_TIEBREAK_AT_SIX_ALL = "tiebreakAtSixAll"
    const val CONFIG_TIEBREAK_POINTS = "tiebreakPoints"
    const val CONFIG_SUPER_TIEBREAK_IN_FINAL_SET = "superTiebreakInFinalSet"
    const val CONFIG_NO_AD_SCORING = "noAdScoring"
    const val CONFIG_INITIAL_SERVER_IS_PLAYER_A = "initialServerIsPlayerA"
    const val CONFIG_TIMESTAMP = "timestamp"

    const val ACK_STATUS = "status"
    const val ACK_IDEMPOTENCY_KEY = "idempotencyKey"

    const val ACK_STATUS_INSERTED = "inserted"
    const val ACK_STATUS_DUPLICATE = "duplicate"
    const val ACK_STATUS_PREMIUM_LOCKED = "premium_locked"
}
