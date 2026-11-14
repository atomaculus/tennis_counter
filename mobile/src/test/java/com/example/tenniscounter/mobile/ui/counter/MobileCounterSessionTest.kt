package com.example.tenniscounter.mobile.ui.counter

import com.playce.shared.scoring.MatchEventCodec
import com.playce.shared.scoring.ScoringEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MobileCounterSessionTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resetGame_dropsOpenPointsAndARelaunchDoesNotRestoreThem() {
        val playing = MobileCounterViewModel()
        repeat(4) { playing.addPointToPlayerA() }
        repeat(2) { playing.addPointToPlayerB() }

        playing.resetGame()

        assertEquals(1, playing.state.value.playerA.games)
        assertEquals(0, playing.state.value.playerA.points)
        assertEquals(0, playing.state.value.playerB.points)
        val kept = ScoringEngine.replayEvents(MatchEventCodec.fromJson(playing.pointEventsJson()))
        assertEquals(1, kept.playerA.games)
        assertEquals(0, kept.playerA.points)
        assertEquals(0, kept.playerB.points)

        val relaunch = MobileCounterViewModel()
        assertEquals(0, relaunch.state.value.playerA.points)
        assertEquals(0, relaunch.state.value.playerA.games)
        assertEquals(0, relaunch.state.value.playerB.points)
    }

    @Test
    fun startNewMatch_clearsTheScoreAndARelaunchStaysBlank() {
        val playing = MobileCounterViewModel()
        repeat(4) { playing.addPointToPlayerA() }
        repeat(2) { playing.addPointToPlayerB() }

        playing.startNewMatch()

        assertEquals(0, playing.state.value.playerA.games)
        assertEquals(0, playing.state.value.playerA.points)
        assertEquals(0, playing.state.value.playerB.points)
        assertTrue(MatchEventCodec.fromJson(playing.pointEventsJson()).isEmpty())

        val relaunch = MobileCounterViewModel()
        assertEquals(0, relaunch.state.value.playerA.games)
        assertEquals(0, relaunch.state.value.playerA.points)
        assertEquals(0, relaunch.state.value.playerB.games)
    }
}
