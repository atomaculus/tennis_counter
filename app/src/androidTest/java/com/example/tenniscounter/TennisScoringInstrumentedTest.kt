package com.example.tenniscounter

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tenniscounter.ui.TennisViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TennisScoringInstrumentedTest {

    @Test
    fun deuceAndAdvantage_flowIsCorrect() {
        val vm = TennisViewModel(ApplicationProvider.getApplicationContext())

        repeat(3) {
            vm.addPointToPlayerA()
            vm.addPointToPlayerB()
        }
        assertEquals("40", vm.matchState.value.pointLabelForA())
        assertEquals("40", vm.matchState.value.pointLabelForB())

        vm.addPointToPlayerA()
        assertEquals("AD", vm.matchState.value.pointLabelForA())
        assertEquals("40", vm.matchState.value.pointLabelForB())

        vm.addPointToPlayerB()
        assertEquals("40", vm.matchState.value.pointLabelForA())
        assertEquals("40", vm.matchState.value.pointLabelForB())
    }

    @Test
    fun winsSetAtSixGamesByTwo() {
        val vm = TennisViewModel(ApplicationProvider.getApplicationContext())

        repeat(6) {
            repeat(4) { vm.addPointToPlayerA() }
        }

        val state = vm.matchState.value
        assertEquals(1, state.playerA.sets)
        assertEquals(0, state.playerA.games)
        assertEquals(0, state.playerB.games)
        assertEquals(1, state.completedSets.size)
        assertEquals(6, state.completedSets.first().a)
        assertEquals(0, state.completedSets.first().b)
    }

    @Test
    fun undoLastPointForPlayerA_restoresStateAfterGameBoundary() {
        val vm = TennisViewModel(ApplicationProvider.getApplicationContext())

        repeat(4) { vm.addPointToPlayerA() }
        vm.addPointToPlayerA()

        val didUndo = vm.undoLastPointForPlayerA()
        val state = vm.matchState.value

        assertTrue(didUndo)
        assertEquals(1, state.playerA.games)
        assertEquals("0", state.pointLabelForA())
        assertEquals("0", state.pointLabelForB())
    }

    @Test
    fun resetGame_keepsGamesButClearsCurrentPoints() {
        val vm = TennisViewModel(ApplicationProvider.getApplicationContext())

        repeat(4) { vm.addPointToPlayerA() }
        repeat(2) { vm.addPointToPlayerB() }

        vm.resetGame()

        val state = vm.matchState.value
        assertEquals(1, state.playerA.games)
        assertEquals(0, state.playerB.games)
        assertEquals("0", state.pointLabelForA())
        assertEquals("0", state.pointLabelForB())
    }

    @Test
    fun startNewMatch_clearsFinishedSummaryAndSavedState() {
        val vm = TennisViewModel(ApplicationProvider.getApplicationContext())

        repeat(4) { vm.addPointToPlayerA() }
        vm.finishMatch()
        assertTrue(vm.saveFinishedMatch())

        vm.startNewMatch()

        val state = vm.matchState.value
        assertNull(vm.finishedMatch.value)
        assertFalse(vm.isFinishedMatchSaved.value)
        assertEquals(0, state.playerA.games)
        assertEquals(0, state.playerB.games)
        assertEquals(0, state.playerA.sets)
        assertEquals(0, state.playerB.sets)
    }

    @Test
    fun currentServer_alternatesByGameAcrossSetBoundaries() {
        val vm = TennisViewModel(ApplicationProvider.getApplicationContext())

        assertTrue(vm.matchState.value.currentServerIsPlayerA())

        repeat(4) { vm.addPointToPlayerA() }
        assertFalse(vm.matchState.value.currentServerIsPlayerA())

        repeat(20) { vm.addPointToPlayerA() }
        val state = vm.matchState.value

        assertEquals(1, state.playerA.sets)
        assertFalse(state.currentServerIsPlayerA())
    }

    @Test
    fun serveSide_changesWithPointParityWithinGame() {
        val vm = TennisViewModel(ApplicationProvider.getApplicationContext())

        assertTrue(vm.matchState.value.serveStartsOnLeftSide())
        vm.addPointToPlayerA()
        assertFalse(vm.matchState.value.serveStartsOnLeftSide())
        vm.addPointToPlayerB()
        assertTrue(vm.matchState.value.serveStartsOnLeftSide())
    }
}
