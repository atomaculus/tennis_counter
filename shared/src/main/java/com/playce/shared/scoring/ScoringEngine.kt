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
        val initialServerIsPlayerA: Boolean = true,
        /**
         * When non-null, the match is being decided by a standalone tiebreak
         * (played instead of a full set after tied sets); value is the points
         * target (7 or 10). Winning it wins the match regardless of sets won.
         */
        val decidingTiebreakTarget: Int? = null
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
            (score.decidingTiebreakTarget != null ||
                (if (isWinnerA) newA.sets else newB.sets) >= format.setsToWin)

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
     * A scorable match event. Match history must be kept as events (not bare
     * points) so that undo can also rewind a deciding-tiebreak choice.
     */
    sealed interface MatchEvent {
        data class Point(val isPlayerA: Boolean) : MatchEvent
        data class DecidingTiebreakStarted(val targetPoints: Int) : MatchEvent
    }

    /**
     * Replay a list of match events from a blank score to rebuild match state.
     */
    fun replayEvents(
        events: List<MatchEvent>,
        format: MatchFormat = MatchFormat.STANDARD,
        initialServerIsPlayerA: Boolean = true
    ): MatchScore {
        var score = MatchScore(format = format, initialServerIsPlayerA = initialServerIsPlayerA)
        events.forEach { event ->
            score = when (event) {
                is MatchEvent.Point -> scorePoint(score, event.isPlayerA)
                is MatchEvent.DecidingTiebreakStarted -> startDecidingTiebreak(score, event.targetPoints)
            }
        }
        return score
    }

    /**
     * A deciding tiebreak can be offered right after a completed set left the
     * players tied on sets (any tie: 1-1 in best-of-3 or best-of-5, 2-2 in
     * best-of-5), before any point of the next set is played.
     */
    fun canOfferDecidingTiebreak(score: MatchScore): Boolean =
        !score.isMatchOver &&
            score.decidingTiebreakTarget == null &&
            !score.isTiebreak &&
            score.playerA.sets == score.playerB.sets &&
            score.playerA.sets > 0 &&
            score.playerA.games == 0 && score.playerB.games == 0 &&
            score.playerA.points == 0 && score.playerB.points == 0

    /**
     * Replace the upcoming set with a standalone tiebreak to the given points
     * target (7 = regular, 10 = super). Whoever wins it wins the match.
     * No-op if the current state does not allow it.
     */
    fun startDecidingTiebreak(score: MatchScore, targetPoints: Int): MatchScore {
        if (!canOfferDecidingTiebreak(score) || targetPoints < 1) return score
        return score.copy(isTiebreak = true, decidingTiebreakTarget = targetPoints)
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

        val targetPoints = score.decidingTiebreakTarget
            ?: if (isSuperTiebreak(score)) 10 else format.tiebreakPoints
        val takesTiebreak = wp >= targetPoints && wp - lp >= 2

        if (!takesTiebreak) {
            return PointResult(
                newWinner = winner.copy(points = wp),
                newLoser = loser
            )
        }

        // Tiebreak won → set is won. A regular tiebreak counts as the winning
        // game (7-6); a deciding tiebreak records its own points (e.g. 10-7)
        // so history reads "6-4 | 3-6 | 10-7".
        val isDeciding = score.decidingTiebreakTarget != null
        val setGamesWinner = if (isDeciding) wp else winner.games + 1
        val setGamesLoser = if (isDeciding) lp else loser.games

        return PointResult(
            newWinner = winner.copy(points = 0, games = 0, sets = winner.sets + 1),
            newLoser = loser.copy(points = 0, games = 0),
            completedSet = setGamesWinner to setGamesLoser
        )
    }
}
