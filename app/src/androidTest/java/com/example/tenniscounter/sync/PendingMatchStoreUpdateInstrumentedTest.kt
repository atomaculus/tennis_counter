package com.example.tenniscounter.sync

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingMatchStoreUpdateInstrumentedTest {

    @Test
    fun updateAfterAttempt_updatesStoredAttemptState() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pending = PendingMatchMessage(
            idempotencyKey = "attempt-key",
            payload = byteArrayOf(4, 5, 6),
            createdAtMillis = 123L,
            attemptCount = 0,
            nextRetryAtMillis = 1_000L,
            targetNodeId = null
        )
        PendingMatchStore.savePending(context, pending)

        assertTrue(
            PendingMatchStore.updateAfterAttempt(
                context = context,
                idempotencyKey = "attempt-key",
                attemptCount = 3,
                nextRetryAtMillis = 8_000L
            )
        )

        val stored = PendingMatchStore.readPending(context)
        requireNotNull(stored)
        assertEquals(3, stored.attemptCount)
        assertEquals(8_000L, stored.nextRetryAtMillis)
    }

    @Test
    fun updateTargetNodeId_requiresMatchingPendingMessage() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pending = PendingMatchMessage(
            idempotencyKey = "node-key",
            payload = byteArrayOf(1),
            createdAtMillis = 456L,
            attemptCount = 0,
            nextRetryAtMillis = 0L,
            targetNodeId = null
        )
        PendingMatchStore.savePending(context, pending)

        assertFalse(PendingMatchStore.updateTargetNodeId(context, "other-key", "phone-1"))
        assertTrue(PendingMatchStore.updateTargetNodeId(context, "node-key", "phone-1"))

        val stored = PendingMatchStore.readPending(context)
        requireNotNull(stored)
        assertEquals("phone-1", stored.targetNodeId)
    }
}
