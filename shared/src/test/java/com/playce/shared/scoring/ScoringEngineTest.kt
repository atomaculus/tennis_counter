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
    fun `serve starts on right then alternates`() {
        var score = MatchScore()
        assertFalse(score.serveStartsOnLeftSide()) // 0 points = right

        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertTrue(score.serveStartsOnLeftSide()) // 1 point = left

        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertFalse(score.serveStartsOnLeftSide()) // 2 points = right
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

    // ---- Deciding tiebreak ----

    @Test
    fun `deciding tiebreak cannot be offered at match start or mid-set`() {
        var score = MatchScore()
        assertFalse(ScoringEngine.canOfferDecidingTiebreak(score))

        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertFalse(ScoringEngine.canOfferDecidingTiebreak(score))

        // One set won but not tied
        var oneSet = MatchScore()
        repeat(6) { oneSet = winGameForPlayer(oneSet, isPlayerA = true) }
        assertFalse(ScoringEngine.canOfferDecidingTiebreak(oneSet))
    }

    @Test
    fun `deciding tiebreak offered on any set tie regardless of best-of`() {
        // 1-1 in best of 3
        assertTrue(ScoringEngine.canOfferDecidingTiebreak(scoreToTiedSets(1)))

        // 1-1 in best of 5
        assertTrue(ScoringEngine.canOfferDecidingTiebreak(scoreToTiedSets(1, MatchFormat.GRAND_SLAM)))

        // 2-2 in best of 5
        assertTrue(ScoringEngine.canOfferDecidingTiebreak(scoreToTiedSets(2, MatchFormat.GRAND_SLAM)))
    }

    @Test
    fun `deciding tiebreak not offered once next set started`() {
        var score = scoreToTiedSets(1)
        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertFalse(ScoringEngine.canOfferDecidingTiebreak(score))
    }

    @Test
    fun `startDecidingTiebreak is a no-op when not offerable`() {
        val fresh = MatchScore()
        assertEquals(fresh, ScoringEngine.startDecidingTiebreak(fresh, 10))
    }

    @Test
    fun `deciding tiebreak to 7 wins the match in best of 3`() {
        var score = ScoringEngine.startDecidingTiebreak(scoreToTiedSets(1), 7)
        assertTrue(score.isTiebreak)
        assertEquals("0", score.pointLabelForA())

        repeat(7) { score = ScoringEngine.scorePoint(score, isPlayerA = true) }
        assertTrue(score.isMatchOver)
        assertEquals(2, score.playerA.sets)
        assertEquals(SetScore(7, 0), score.completedSets.last())
    }

    @Test
    fun `deciding tiebreak to 10 at 1-1 in best of 5 ends the whole match`() {
        var score = ScoringEngine.startDecidingTiebreak(scoreToTiedSets(1, MatchFormat.GRAND_SLAM), 10)

        repeat(9) { score = ScoringEngine.scorePoint(score, isPlayerA = false) }
        assertFalse(score.isMatchOver)

        score = ScoringEngine.scorePoint(score, isPlayerA = false)
        assertTrue(score.isMatchOver)
        assertEquals(2, score.playerB.sets) // fewer than setsToWin, still over
        assertEquals(SetScore(0, 10), score.completedSets.last())
    }

    @Test
    fun `deciding tiebreak requires 2-point lead`() {
        var score = ScoringEngine.startDecidingTiebreak(scoreToTiedSets(1), 10)
        repeat(9) {
            score = ScoringEngine.scorePoint(score, isPlayerA = true)
            score = ScoringEngine.scorePoint(score, isPlayerA = false)
        }
        // 10-9 not enough
        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertFalse(score.isMatchOver)
        // 11-9 wins
        score = ScoringEngine.scorePoint(score, isPlayerA = true)
        assertTrue(score.isMatchOver)
        assertEquals(SetScore(11, 9), score.completedSets.last())
    }

    // ---- Event replay ----

    @Test
    fun `replayEvents rebuilds deciding tiebreak state`() {
        val events = buildList {
            repeat(24) { add(ScoringEngine.MatchEvent.Point(isPlayerA = true)) }  // A wins set 6-0
            repeat(24) { add(ScoringEngine.MatchEvent.Point(isPlayerA = false)) } // B wins set 6-0
            add(ScoringEngine.MatchEvent.DecidingTiebreakStarted(10))
            repeat(3) { add(ScoringEngine.MatchEvent.Point(isPlayerA = true)) }
        }
        val score = ScoringEngine.replayEvents(events)
        assertTrue(score.isTiebreak)
        assertEquals(10, score.decidingTiebreakTarget)
        assertEquals(3, score.playerA.points)
        assertFalse(score.isMatchOver)
    }

    @Test
    fun `dropping last event undoes the deciding tiebreak choice`() {
        val events = buildList {
            repeat(24) { add(ScoringEngine.MatchEvent.Point(isPlayerA = true)) }
            repeat(24) { add(ScoringEngine.MatchEvent.Point(isPlayerA = false)) }
            add(ScoringEngine.MatchEvent.DecidingTiebreakStarted(10))
        }
        val undone = ScoringEngine.replayEvents(events.dropLast(1))
        assertFalse(undone.isTiebreak)
        assertEquals(null, undone.decidingTiebreakTarget)
        assertTrue(ScoringEngine.canOfferDecidingTiebreak(undone)) // question re-arms
    }

    @Test
    fun `replayEvents produces same score with and without timestamps`() {
        val baseTime = 1_723_050_000_000L
        val eventsWithoutTimestamps = buildList {
            repeat(24) { add(ScoringEngine.MatchEvent.Point(isPlayerA = true)) }
            repeat(24) { add(ScoringEngine.MatchEvent.Point(isPlayerA = false)) }
            add(ScoringEngine.MatchEvent.DecidingTiebreakStarted(10))
            repeat(3) { add(ScoringEngine.MatchEvent.Point(isPlayerA = true)) }
        }
        val eventsWithTimestamps = eventsWithoutTimestamps.mapIndexed { index, event ->
            when (event) {
                is ScoringEngine.MatchEvent.Point ->
                    event.copy(timestampMillis = baseTime + index * 1000L)
                is ScoringEngine.MatchEvent.DecidingTiebreakStarted ->
                    event.copy(timestampMillis = baseTime + index * 1000L)
            }
        }

        val scoreWithout = ScoringEngine.replayEvents(eventsWithoutTimestamps)
        val scoreWith = ScoringEngine.replayEvents(eventsWithTimestamps)

        assertEquals(scoreWithout, scoreWith)
    }

    // ---- MatchEventCodec ----

    @Test
    fun `toJson of empty list is empty array`() {
        assertEquals("[]", MatchEventCodec.toJson(emptyList()))
    }

    @Test
    fun `toJson of single point event`() {
        val events = listOf(ScoringEngine.MatchEvent.Point(isPlayerA = true, timestampMillis = 1723050000123))
        assertEquals(
            "[{\"type\":\"point\",\"isPlayerA\":true,\"t\":1723050000123}]",
            MatchEventCodec.toJson(events)
        )
    }

    @Test
    fun `toJson of mixed point and deciding tiebreak events`() {
        val events = listOf(
            ScoringEngine.MatchEvent.Point(isPlayerA = true, timestampMillis = 1723050000123),
            ScoringEngine.MatchEvent.DecidingTiebreakStarted(targetPoints = 10, timestampMillis = 1723050100456)
        )
        assertEquals(
            "[{\"type\":\"point\",\"isPlayerA\":true,\"t\":1723050000123},{\"type\":\"deciding_tb\",\"targetPoints\":10,\"t\":1723050100456}]",
            MatchEventCodec.toJson(events)
        )
    }

    @Test
    fun `fromJson of empty array is empty list`() {
        assertEquals(emptyList<ScoringEngine.MatchEvent>(), MatchEventCodec.fromJson("[]"))
    }

    @Test
    fun `fromJson tolerates surrounding and interior whitespace`() {
        val json = "  [ { \"type\" : \"point\" , \"isPlayerA\" : true , \"t\" : 42 } ] "
        val events = MatchEventCodec.fromJson(json)
        assertEquals(1, events.size)
        assertEquals(ScoringEngine.MatchEvent.Point(isPlayerA = true, timestampMillis = 42), events[0])
    }

    @Test
    fun `round-trip toJson then fromJson for empty list`() {
        val events = emptyList<ScoringEngine.MatchEvent>()
        assertEquals(events, MatchEventCodec.fromJson(MatchEventCodec.toJson(events)))
    }

    @Test
    fun `round-trip toJson then fromJson for single point event`() {
        val events = listOf(ScoringEngine.MatchEvent.Point(isPlayerA = false, timestampMillis = 1723050000123))
        assertEquals(events, MatchEventCodec.fromJson(MatchEventCodec.toJson(events)))
    }

    @Test
    fun `round-trip toJson then fromJson for mixed events`() {
        val events = listOf(
            ScoringEngine.MatchEvent.Point(isPlayerA = true, timestampMillis = 1),
            ScoringEngine.MatchEvent.Point(isPlayerA = false, timestampMillis = 2),
            ScoringEngine.MatchEvent.DecidingTiebreakStarted(targetPoints = 10, timestampMillis = 3),
            ScoringEngine.MatchEvent.Point(isPlayerA = true, timestampMillis = 4)
        )
        assertEquals(events, MatchEventCodec.fromJson(MatchEventCodec.toJson(events)))
    }

    @Test
    fun `fromJson of malformed json returns empty list instead of throwing`() {
        assertEquals(emptyList<ScoringEngine.MatchEvent>(), MatchEventCodec.fromJson("not json"))
        assertEquals(emptyList<ScoringEngine.MatchEvent>(), MatchEventCodec.fromJson(""))
        assertEquals(emptyList<ScoringEngine.MatchEvent>(), MatchEventCodec.fromJson("{\"type\":\"point\"}"))
        assertEquals(emptyList<ScoringEngine.MatchEvent>(), MatchEventCodec.fromJson("[{\"type\":\"point\"}]"))
        assertEquals(emptyList<ScoringEngine.MatchEvent>(), MatchEventCodec.fromJson("[{\"type\":\"unknown\",\"t\":1}]"))
        assertEquals(emptyList<ScoringEngine.MatchEvent>(), MatchEventCodec.fromJson("[{\"type\":\"point\",\"isPlayerA\":true"))
        assertEquals(emptyList<ScoringEngine.MatchEvent>(), MatchEventCodec.fromJson("[{\"type\":\"point\",\"isPlayerA\":true,\"t\":1}"))
    }

    // ---- Helpers ----

    private fun scoreToTiedSets(setsEach: Int, format: MatchFormat = MatchFormat.STANDARD): MatchScore {
        var score = MatchScore(format = format)
        repeat(setsEach) {
            repeat(6) { score = winGameForPlayer(score, isPlayerA = true) }
            repeat(6) { score = winGameForPlayer(score, isPlayerA = false) }
        }
        return score
    }


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
