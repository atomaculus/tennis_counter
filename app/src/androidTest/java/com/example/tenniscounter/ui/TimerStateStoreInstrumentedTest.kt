package com.example.tenniscounter.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerStateStoreInstrumentedTest {

    @Test
    fun start_thenConsolidateAndStop_persistsElapsedSeconds() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TimerStateStore.resetStopped(context)

        TimerStateStore.start(context, nowElapsedRealtime = 1_000L)
        TimerStateStore.consolidateAndStop(context, nowElapsedRealtime = 7_500L)

        val snapshot = TimerStateStore.read(context)
        assertFalse(snapshot.isRunning)
        assertEquals(6L, snapshot.accumulatedSeconds)
        assertEquals(6, snapshot.elapsedSeconds(10_000L))
    }

    @Test
    fun resetStopped_clearsTimerState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        TimerStateStore.start(context, nowElapsedRealtime = 2_000L)
        TimerStateStore.consolidateAndStop(context, nowElapsedRealtime = 4_200L)
        TimerStateStore.resetStopped(context)

        val snapshot = TimerStateStore.read(context)
        assertFalse(snapshot.isRunning)
        assertEquals(0L, snapshot.startElapsedRealtime)
        assertEquals(0L, snapshot.accumulatedSeconds)
        assertEquals(0, snapshot.elapsedSeconds(10_000L))
    }

    @Test
    fun ensureInitialized_migratesLegacyKeys() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = context.dataStore

        prefs.edit { store ->
            store.clear()
            store[longPreferencesKey("start_time")] = 1_000L
            store[longPreferencesKey("paused_accumulated")] = 2_000L
            store[longPreferencesKey("last_pause_time")] = 8_000L
        }

        TimerStateStore.ensureInitialized(context, nowElapsedRealtime = 10_000L)

        val snapshot = TimerStateStore.read(context)
        assertTrue(snapshot.isRunning)
        assertEquals(10_000L, snapshot.startElapsedRealtime)
        assertEquals(7L, snapshot.accumulatedSeconds)
        assertEquals(7, snapshot.elapsedSeconds(10_000L))
    }
}
