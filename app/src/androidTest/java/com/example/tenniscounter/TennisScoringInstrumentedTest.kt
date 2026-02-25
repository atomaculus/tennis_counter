package com.example.tenniscounter

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tenniscounter.ui.TennisViewModel
import org.junit.Assert.assertEquals
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
}
