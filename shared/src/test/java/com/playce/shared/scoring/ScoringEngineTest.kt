package com.playce.shared.scoring

import com.playce.shared.scoring.ScoringEngine.MatchScore
import com.playce.shared.scoring.ScoringEngine.PlayerScore
import com.playce.shared.scoring.ScoringEngine.SetScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringEngineTest {

    // ---- Point progression ----

    @Test
    fun `point progression follows 0-15-30-40`() {
        var score = MatchScore()
        val labels = mutableListOf<String>()

        repeat(3) {
            score = ScoringEngine.scorePoint(score, isPlayerA = true)
            labels.add(score.pointLabelForA())
        }
        assertEquals(listOf("15", "30", "40"), labels)
    }

    @Test
    fun `winning 4 points wins a game`() {
        var score = MatchScore()
        repeat(4) { score = ScoringEngine.scorePoint(score, isPlayerA = true) }
        assertEquals(0, score.playerA.points)
        assertEquals(1, score.playerA.games)
    }

    // ---- Deuce & advantage ----

    @Test
    fun `deuce shows 40-40`() {
        val score = scoreToDeuce()
        assertEquals("40", score.pointLabelForA())
        assertEquals("40", score.pointLabelForB())
    }

    @Test
    fun `advantage shows AD for leading player`() {
        var score = scoreToDeuce()
        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertEquals("AD", score.pointLabelForA())
        assertEquals("40", score.pointLabelForB())
    }

    @Test
    fun `advantage lost returns to deuce`() {
        var score = scoreToDeuce()
        score = ScoringEngine.scorePoint(score, isPlayerA = true) // AD-40
        score = ScoringEngine.scorePoint(score, isPlayerA = false) // back to deuce
        assertEquals("40", score.pointLabelForA())
        assertEquals("40", score.pointLabelForB())
    }

    @Test
    fun `game won from advantage`() {
        var score = scoreToDeuce()
        score = ScoringEngine.scorePoint(score, isPlayerA = true) // AD
        score = ScoringEngine.scorePoint(score, isPlayerA = true) // game
        assertEquals(1, score.playerA.games)
        assertEquals(0, score.playerA.points)
    }

    // ---- No-ad scoring ----

    @Test
    fun `no-ad deuce point wins game immediately`() {
        val format = MatchFormat(noAdScoring = true)
        var score = MatchScore(format = format)
        // Get to 40-40 (3-3 points)
        repeat(3) { score = ScoringEngine.scorePoint(score, true) }
        repeat(3) { score = ScoringEngine.scorePoint(score, false) }
        // Next point wins the game
        score = ScoringEngine.scorePoint(score, true)
        assertEquals(1, score.playerA.games)
    }

    // ---- Set progression ----

    @Test
    fun `winning 6 games with 2-game lead wins set`() {
        var score = MatchScore()
        // Player A wins 6 games, B wins 0
        repeat(6) { score = winGameForPlayer(score, isPlayerA = true) }
        assertEquals(1, score.playerA.sets)
        assertEquals(1, score.completedSets.size)
        assertEquals(SetScore(6, 0), score.completedSets[0])
    }

    @Test
    fun `5-5 requires two more games to win set`() {
        var score = MatchScore()
        // Alternate to 5-5
        repeat(5) {
            score = winGameForPlayer(score, isPlayerA = true)
            score = winGameForPlayer(score, isPlayerA = false)
        }
        assertEquals(5, score.playerA.games)
        assertEquals(5, score.playerB.games)

        // 6-5
        score = winGameForPlayer(score, isPlayerA = true)
        assertEquals(0, score.playerA.sets) // no set won yet

        // 7-5
        score = winGameForPlayer(score, isPlayerA = true)
        assertEquals(1, score.playerA.sets)
        assertEquals(SetScore(7, 5), score.completedSets.last())
    }

    // ---- Tiebreak ----

    @Test
    fun `tiebreak starts at 6-6`() {
        var score = scoreTo6All()
        assertTrue(score.isTiebreak)
    }

    @Test
    fun `tiebreak point labels are numeric`() {
        var score = scoreTo6All()
        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertEquals("1", score.pointLabelForA())
        assertEquals("0", score.pointLabelForB())
    }

    @Test
    fun `tiebreak won at 7 points with 2-point lead`() {
        var score = scoreTo6All()
        // A wins 7-0
        repeat(7) { score = ScoringEngine.scorePoint(score, isPlayerA = true) }
        assertEquals(1, score.playerA.sets)
        assertEquals(SetScore(7, 6), score.completedSets.last())
        assertFalse(score.isTiebreak)
    }

    @Test
    fun `tiebreak requires 2-point lead`() {
        var score = scoreTo6All()
        // Get to 6-6 in tiebreak
        repeat(6) {
            score = ScoringEngine.scorePoint(score, isPlayerA = true)
            score = ScoringEngine.scorePoint(score, isPlayerA = false)
        }
        assertEquals(6, score.playerA.points)
        assertEquals(6, score.playerB.points)
        assertTrue(score.isTiebreak)

        // 7-6 not enough
        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertTrue(score.isTiebreak)
        assertEquals(0, score.playerA.sets)

        // 8-6 wins
        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertEquals(1, score.playerA.sets)
    }

    @Test
    fun `tiebreak disabled means no tiebreak at 6-6`() {
        val format = MatchFormat(tiebreakAtSixAll = false)
        var score = MatchScore(format = format)
        // Get to 6-6
        repeat(6) {
            score = winGameForPlayer(score, isPlayerA = true)
            score = winGameForPlayer(score, isPlayerA = false)
        }
        assertFalse(score.isTiebreak)
        assertEquals(6, score.playerA.games)
        assertEquals(6, score.playerB.games)
    }

    // ---- Match completion ----

    @Test
    fun `match ends when setsToWin reached - best of 3`() {
        var score = MatchScore(format = MatchFormat(setsToWin = 2))
        // Win 2 sets
        repeat(2) {
            repeat(6) { score = winGameForPlayer(score, isPlayerA = true) }
        }
        assertTrue(score.isMatchOver)
        assertEquals(2, score.playerA.sets)
    }

    @Test
    fun `match ends when setsToWin reached - best of 5`() {
        var score = MatchScore(format = MatchFormat.GRAND_SLAM)
        repeat(3) {
            repeat(6) { score = winGameForPlayer(score, isPlayerA = true) }
        }
        assertTrue(score.isMatchOver)
    }

    @Test
    fun `no points after match is over`() {
        var score = MatchScore()
        repeat(2) {
            repeat(6) { score = winGameForPlayer(score, isPlayerA = true) }
        }
        assertTrue(score.isMatchOver)
        val before = score.copy()
        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertEquals(before, score)
    }

    // ---- Super tiebreak ----

    @Test
    fun `super tiebreak in final set at 1-1 sets`() {
        val format = MatchFormat(superTiebreakInFinalSet = true)
        var score = MatchScore(format = format)

        // Each player wins 1 set
        repeat(6) { score = winGameForPlayer(score, isPlayerA = true) }
        repeat(6) { score = winGameForPlayer(score, isPlayerA = false) }
        assertEquals(1, score.playerA.sets)
        assertEquals(1, score.playerB.sets)

        // Get to 6-6 in final set to trigger tiebreak
        repeat(6) {
            score = winGameForPlayer(score, isPlayerA = true)
            score = winGameForPlayer(score, isPlayerA = false)
        }
        assertTrue(score.isTiebreak)

        // Super tiebreak: first to 10 with 2-point lead
        repeat(10) { score = ScoringEngine.scorePoint(score, isPlayerA = true) }
        assertTrue(score.isMatchOver)
        assertEquals(2, score.playerA.sets)
    }

    // ---- Server rotation ----

    @Test
    fun `server alternates each game`() {
        var score = MatchScore(initialServerIsPlayerA = true)
        assertTrue(score.currentServerIsPlayerA())

        score = winGameForPlayer(score, isPlayerA = true) // 1-0
        assertFalse(score.currentServerIsPlayerA())

        score = winGameForPlayer(score, isPlayerA = true) // 2-0
        assertTrue(score.currentServerIsPlayerA())
    }

    // ---- Serve side ----

    @Test
    fun `serve starts on left then alternates`() {
        var score = MatchScore()
        assertTrue(score.serveStartsOnLeftSide()) // 0 points = left

        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertFalse(score.serveStartsOnLeftSide()) // 1 point = right

        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertTrue(score.serveStartsOnLeftSide()) // 2 points = left
    }

    // ---- Replay ----

    @Test
    fun `replay recreates exact score`() {
        val points = listOf(true, true, true, true, false, false, false, false) // A wins game, B wins game
        val score = ScoringEngine.replay(points)
        assertEquals(1, score.playerA.games)
        assertEquals(1, score.playerB.games)
    }

    @Test
    fun `replay with empty list gives fresh score`() {
        val score = ScoringEngine.replay(emptyList())
        assertEquals(PlayerScore(), score.playerA)
        assertEquals(PlayerScore(), score.playerB)
    }

    // ---- Helpers ----

    private fun scoreToDeuce(): MatchScore {
        var score = MatchScore()
        repeat(3) { score = ScoringEngine.scorePoint(score, isPlayerA = true) }
        repeat(3) { score = ScoringEngine.scorePoint(score, isPlayerA = false) }
        return score
    }

    private fun scoreTo6All(): MatchScore {
        var score = MatchScore()
        repeat(6) {
            score = winGameForPlayer(score, isPlayerA = true)
            score = winGameForPlayer(score, isPlayerA = false)
        }
        return score
    }

    private fun winGameForPlayer(score: MatchScore, isPlayerA: Boolean): MatchScore {
        var s = score
        repeat(4) { s = ScoringEngine.scorePoint(s, isPlayerA) }
        return s
    }
}
