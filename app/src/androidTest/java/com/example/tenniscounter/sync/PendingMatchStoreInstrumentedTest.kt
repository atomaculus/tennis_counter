package com.example.tenniscounter.sync

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingMatchStoreInstrumentedTest {

    @Test
    fun clearIfMatches_isIdempotent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val msg = PendingMatchMessage(
            idempotencyKey = "k1",
            payload = byteArrayOf(1, 2, 3),
            createdAtMillis = System.currentTimeMillis(),
            attemptCount = 0,
            nextRetryAtMillis = 0L,
            targetNodeId = null
        )
        PendingMatchStore.savePending(context, msg)

        assertTrue(PendingMatchStore.clearIfMatches(context, "k1"))
        assertFalse(PendingMatchStore.clearIfMatches(context, "k1"))
    }
}
