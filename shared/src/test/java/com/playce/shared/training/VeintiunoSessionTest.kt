package com.playce.shared.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeintiunoSessionTest {

    @Test
    fun `fresh session is an empty streak`() {
        val session = VeintiunoSession()
        assertEquals(0, session.count)
        assertEquals(0, session.bestStreak)
        assertEquals(0, session.misses)
        assertEquals(21, session.target)
        assertFalse(session.hasActivity)
        assertFalse(session.reachedTarget)
    }

    @Test
    fun `plus one advances the streak and marks the session active`() {
        val session = VeintiunoSession().plusOne().plusOne()
        assertEquals(2, session.count)
        assertEquals(2, session.bestStreak)
        assertEquals(0, session.misses)
        assertTrue(session.hasActivity)
        assertFalse(session.reachedTarget)
    }

    @Test
    fun `miss resets the streak and keeps the best`() {
        val session = VeintiunoSession().plusOne().plusOne().plusOne().miss()
        assertEquals(0, session.count)
        assertEquals(3, session.bestStreak)
        assertEquals(1, session.misses)
        assertTrue(session.hasActivity)
        assertFalse(session.reachedTarget)
        assertEquals(2, session.savedAttempts())
    }

    @Test
    fun `best streak is the longest run, not the last one`() {
        val session = VeintiunoSession()
            .plusOne().plusOne().plusOne().plusOne().plusOne()
            .miss()
            .plusOne().plusOne()
        assertEquals(2, session.count)
        assertEquals(5, session.bestStreak)
        assertEquals(1, session.misses)
        assertEquals(2, session.savedAttempts())
    }

    @Test
    fun `a miss from zero still counts as an attempt`() {
        val session = VeintiunoSession().miss()
        assertEquals(0, session.count)
        assertEquals(0, session.bestStreak)
        assertEquals(1, session.misses)
        assertTrue(session.hasActivity)
        assertEquals(2, session.savedAttempts())
    }

    @Test
    fun `reaching 21 locks the counter and counts the finished run`() {
        var session = VeintiunoSession()
        repeat(21) { session = session.plusOne() }
        assertEquals(21, session.count)
        assertEquals(21, session.bestStreak)
        assertTrue(session.reachedTarget)
        assertEquals(1, session.savedAttempts())

        val locked = session.plusOne().miss()
        assertEquals(21, locked.count)
        assertEquals(21, locked.bestStreak)
        assertEquals(0, locked.misses)
        assertTrue(locked.reachedTarget)
    }

    @Test
    fun `misses before the target are kept when 21 is reached`() {
        var session = VeintiunoSession().plusOne().plusOne().miss().plusOne().miss()
        repeat(21) { session = session.plusOne() }
        assertEquals(21, session.count)
        assertEquals(21, session.bestStreak)
        assertEquals(2, session.misses)
        assertTrue(session.reachedTarget)
        // two misses plus the run that reached 21
        assertEquals(3, session.savedAttempts())
    }

    @Test
    fun `paredon stays hidden until the flag is turned on`() {
        assertFalse(TrainingFeatures.PAREDON_ENABLED)
    }
}
