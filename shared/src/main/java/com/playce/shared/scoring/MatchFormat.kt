package com.playce.shared.scoring

/**
 * Configurable match format for different tennis scoring rules.
 */
data class MatchFormat(
    /** Best of 3 or best of 5 sets */
    val setsToWin: Int = 2,
    /** Play a tiebreak at 6-6 in regular sets */
    val tiebreakAtSixAll: Boolean = true,
    /** Points needed to win a tiebreak (typically 7) */
    val tiebreakPoints: Int = 7,
    /** Use super tiebreak (10 points) in final set instead of a regular set */
    val superTiebreakInFinalSet: Boolean = false,
    /** No-advantage scoring: at deuce, next point wins the game */
    val noAdScoring: Boolean = false
) {
    companion object {
        /** Standard ATP/WTA format: best of 3, tiebreak at 6-6 */
        val STANDARD = MatchFormat()

        /** Grand Slam format: best of 5 sets */
        val GRAND_SLAM = MatchFormat(setsToWin = 3)

        /** Fast format: best of 3, super tiebreak in final set, no-ad */
        val FAST4 = MatchFormat(
            setsToWin = 2,
            noAdScoring = true,
            superTiebreakInFinalSet = true
        )
    }
}
