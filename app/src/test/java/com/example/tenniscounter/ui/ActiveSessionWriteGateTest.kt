package com.example.tenniscounter.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveSessionWriteGateTest {

    @Test
    fun resetSupersedesAnInFlightSaveOfThePreviousPoints() {
        val gate = ActiveSessionWriteGate()

        val saveOfOpenPoints = gate.open()
        val reset = gate.open()

        assertFalse(gate.isCurrent(saveOfOpenPoints))
        assertTrue(gate.isCurrent(reset))
    }

    @Test
    fun newerSnapshotSupersedesThePreviousOne() {
        val gate = ActiveSessionWriteGate()

        val firstPoint = gate.open()
        val secondPoint = gate.open()

        assertFalse(gate.isCurrent(firstPoint))
        assertTrue(gate.isCurrent(secondPoint))
    }
}
