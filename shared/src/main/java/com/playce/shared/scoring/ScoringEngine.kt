package com.playce.shared.scoring

/**
 * Pure, stateless tennis scoring engine.
 * All methods are side-effect free — they take state in and return new state out.
 */
object ScoringEngine {

    data class PlayerScore(
        val points: Int = 0,
        val games: Int = 0,
        val sets: Int = 0
    )

    data class SetScore(val a: Int, val b: Int)

    data class MatchScore(
        val playerA: PlayerScore = PlayerScore(),
        val playerB: PlayerScore = PlayerScore(),
        val completedSets: List<SetScore> = emptyList(),
        val isTiebreak: Boolean = false,
        val isMatchOver: Boolean = false,
        val format: MatchFormat = MatchFormat.STANDARD,
        val initialServerIsPlayerA: Boolean = true
    ) {
        fun pointLabelForA(): String = pointLabel(playerA.points, playerB.points)
        fun pointLabelForB(): String = pointLabel(playerB.points, playerA.points)

        fun currentServerIsPlayerA(): Boolean {
            val completedGames = completedSets.sumOf { it.a + it.b }
            val currentSetGames = playerA.games + playerB.games
            val totalGamesPlayed = completedGames + currentSetGames

            if (isTiebreak) {
                // In tiebreak, server changes after first point, then every 2 points
                val tiebreakPoints = playerA.points + playerB.points
                val serverChanges = if (tiebreakPoints == 0) 0 else ((tiebreakPoints - 1) / 2) + 1
                val baseServer = if (totalGamesPlayed % 2 == 0) initialServerIsPlayerA else !initialServerIsPlayerA
                return if (serverChanges % 2 == 0) baseServer else !baseServer
            }

            return if (totalGamesPlayed % 2 == 0) initialServerIsPlayerA else !initialServerIsPlayerA
        }

        fun serveStartsOnLeftSide(): Boolean {
            val totalPoints = playerA.points + playerB.points
            return totalPoints % 2 != 0
        }

        private fun pointLabel(playerPoints: Int, rivalPoints: Int): String {
            if (isTiebreak) return playerPoints.toString()

            if (playerPoints >= 3 && rivalPoints >= 3) {
                return when {
                    playerPoints == rivalPoints -> "40"
                    playerPoints == rivalPoints + 1 -> "AD"
                    else -> "40"
                }
            }
            return POINT_LABELS.getOrElse(playerPoints.coerceIn(0, 4)) { "0" }
        }
    }

    private val POINT_LABELS = listOf("0", "15", "30", "40", "AD")

    /**
     * Apply a point won by the given player. Returns the new match score.
     */
    fun scorePoint(score: MatchScore, isPlayerA: Boolean): MatchScore {
        if (score.isMatchOver) return score

        val format = score.format
        val winner: PlayerScore
        val loser: PlayerScore
        val isWinnerA = isPlayerA

        if (isPlayerA) {
            winner = score.playerA
            loser = score.playerB
        } else {
            winner = score.playerB
            loser = score.playerA
        }

        val result = if (score.isTiebreak || isSuperTiebreak(score)) {
            resolveTiebreakPoint(winner, loser, score, format)
        } else {
            resolveRegularPoint(winner, loser, score, format)
        }

        val newA = if (isWinnerA) result.newWinner else result.newLoser
        val newB = if (isWinnerA) result.newLoser else result.newWinner
        val sets = score.completedSets.toMutableList()
        result.completedSet?.let { (w, l) ->
            sets.add(if (isWinnerA) SetScore(w, l) else SetScore(l, w))
        }

        val matchOver = result.completedSet != null &&
            (if (isWinnerA) newA.sets else newB.sets) >= format.setsToWin

        val nowInTiebreak = when {
            matchOver -> false
            result.completedSet != null -> false  // tiebreak/set just ended
            result.enteredTiebreak -> true         // just entered tiebreak
            score.isTiebreak -> true               // still in tiebreak
            else -> false
        }

        return score.copy(
            playerA = newA,
            playerB = newB,
            completedSets = sets,
            isTiebreak = nowInTiebreak,
            isMatchOver = matchOver
        )
    }

    /**
     * Replay a list of points from a blank score to rebuild match state.
     * Each entry is true if Player A won the point.
     */
    fun replay(points: List<Boolean>, format: MatchFormat = MatchFormat.STANDARD, initialServerIsPlayerA: Boolean = true): MatchScore {
        var score = MatchScore(format = format, initialServerIsPlayerA = initialServerIsPlayerA)
        points.forEach { isA -> score = scorePoint(score, isA) }
        return score
    }

    /**
     * Check whether we're currently in a super-tiebreak scenario
     * (final set, super tiebreak configured, games are at 0-0 because
     * the set is played as a single tiebreak).
     */
    private fun isSuperTiebreak(score: MatchScore): Boolean {
        if (!score.format.superTiebreakInFinalSet) return false
        val maxSets = score.format.setsToWin
        // Final set = both players have won (maxSets - 1) sets
        return score.playerA.sets == maxSets - 1 &&
            score.playerB.sets == maxSets - 1 &&
            score.isTiebreak
    }

    // -- Regular game scoring --

    private data class PointResult(
        val newWinner: PlayerScore,
        val newLoser: PlayerScore,
        val completedSet: Pair<Int, Int>? = null,
        val enteredTiebreak: Boolean = false
    )

    private fun resolveRegularPoint(
        winner: PlayerScore,
        loser: PlayerScore,
        score: MatchScore,
        format: MatchFormat
    ): PointResult {
        val wp = winner.points + 1
        val lp = loser.points

        val takesGame = if (format.noAdScoring) {
            wp >= 4 && wp - lp >= 1
        } else {
            wp >= 4 && wp - lp >= 2
        }

        if (!takesGame) {
            return PointResult(
                newWinner = winner.copy(points = wp),
                newLoser = loser
            )
        }

        return resolveGameWon(winner, loser, score, format)
    }

    private fun resolveGameWon(
        winner: PlayerScore,
        loser: PlayerScore,
        score: MatchScore,
        format: MatchFormat
    ): PointResult {
        val wg = winner.games + 1
        val lg = loser.games

        // Check for tiebreak entry at 6-6
        if (format.tiebreakAtSixAll && wg == 6 && lg == 6) {
            // Check if this is the final set with super tiebreak
            val isFinalSet = winner.sets == format.setsToWin - 1 && loser.sets == format.setsToWin - 1
            if (isFinalSet && format.superTiebreakInFinalSet) {
                return PointResult(
                    newWinner = winner.copy(points = 0, games = wg),
                    newLoser = loser.copy(points = 0),
                    enteredTiebreak = true
                )
            }
            // Regular tiebreak
            return PointResult(
                newWinner = winner.copy(points = 0, games = wg),
                newLoser = loser.copy(points = 0),
                enteredTiebreak = true
            )
        }

        // Win set at 6+ games with 2+ lead, or 7-6 (after tiebreak, but this path is regular games)
        val takesSet = wg >= 6 && wg - lg >= 2

        return if (takesSet) {
            PointResult(
                newWinner = winner.copy(points = 0, games = 0, sets = winner.sets + 1),
                newLoser = loser.copy(points = 0, games = 0),
                completedSet = wg to lg
            )
        } else {
            PointResult(
                newWinner = winner.copy(points = 0, games = wg),
                newLoser = loser.copy(points = 0)
            )
        }
    }

    // -- Tiebreak scoring --

    private fun resolveTiebreakPoint(
        winner: PlayerScore,
        loser: PlayerScore,
        score: MatchScore,
        format: MatchFormat
    ): PointResult {
        val wp = winner.points + 1
        val lp = loser.points

        val targetPoints = if (isSuperTiebreak(score)) 10 else format.tiebreakPoints
        val takesTiebreak = wp >= targetPoints && wp - lp >= 2

        if (!takesTiebreak) {
            return PointResult(
                newWinner = winner.copy(points = wp),
                newLoser = loser
            )
        }

        // Tiebreak won → set is won (score recorded as 7-6 or equivalent)
        val setGamesWinner = winner.games + 1 // tiebreak counts as the winning game
        val setGamesLoser = loser.games

        return PointResult(
            newWinner = winner.copy(points = 0, games = 0, sets = winner.sets + 1),
            newLoser = loser.copy(points = 0, games = 0),
            completedSet = setGamesWinner to setGamesLoser
        )
    }
}
