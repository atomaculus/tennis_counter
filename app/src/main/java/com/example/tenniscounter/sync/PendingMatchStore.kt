package com.example.tenniscounter.sync

import android.content.Context
import android.util.Base64

data class PendingMatchMessage(
    val idempotencyKey: String,
    val payload: ByteArray,
    val createdAtMillis: Long,
    val attemptCount: Int,
    val nextRetryAtMillis: Long,
    val targetNodeId: String?
)

object PendingMatchStore {
    private const val PREFS_NAME = "wear_pending_match_store"
    private const val KEY_IDEMPOTENCY = "idempotency_key"
    private const val KEY_PAYLOAD = "payload_base64"
    private const val KEY_CREATED_AT = "created_at_millis"
    private const val KEY_ATTEMPT_COUNT = "attempt_count"
    private const val KEY_NEXT_RETRY_AT = "next_retry_at_millis"
    private const val KEY_TARGET_NODE_ID = "target_node_id"
    private val lock = Any()

    fun savePending(context: Context, pending: PendingMatchMessage) {
        synchronized(lock) {
            prefs(context).edit()
                .putString(KEY_IDEMPOTENCY, pending.idempotencyKey)
                .putString(KEY_PAYLOAD, Base64.encodeToString(pending.payload, Base64.NO_WRAP))
                .putLong(KEY_CREATED_AT, pending.createdAtMillis)
                .putInt(KEY_ATTEMPT_COUNT, pending.attemptCount)
                .putLong(KEY_NEXT_RETRY_AT, pending.nextRetryAtMillis)
                .putString(KEY_TARGET_NODE_ID, pending.targetNodeId)
                .apply()
        }
    }

    fun readPending(context: Context): PendingMatchMessage? {
        synchronized(lock) {
            val idempotencyKey = prefs(context).getString(KEY_IDEMPOTENCY, null) ?: return null
            val payloadBase64 = prefs(context).getString(KEY_PAYLOAD, null) ?: return null
            val payload = runCatching { Base64.decode(payloadBase64, Base64.DEFAULT) }.getOrNull() ?: return null
            return PendingMatchMessage(
                idempotencyKey = idempotencyKey,
                payload = payload,
                createdAtMillis = prefs(context).getLong(KEY_CREATED_AT, 0L),
                attemptCount = prefs(context).getInt(KEY_ATTEMPT_COUNT, 0),
                nextRetryAtMillis = prefs(context).getLong(KEY_NEXT_RETRY_AT, 0L),
                targetNodeId = prefs(context).getString(KEY_TARGET_NODE_ID, null)
            )
        }
    }

    fun clearIfMatches(context: Context, idempotencyKey: String): Boolean {
        synchronized(lock) {
            val currentId = prefs(context).getString(KEY_IDEMPOTENCY, null) ?: return false
            if (currentId != idempotencyKey) return false
            prefs(context).edit().clear().apply()
            return true
        }
    }

    fun updateAfterAttempt(
        context: Context,
        idempotencyKey: String,
        attemptCount: Int,
        nextRetryAtMillis: Long
    ): Boolean {
        synchronized(lock) {
            val currentId = prefs(context).getString(KEY_IDEMPOTENCY, null) ?: return false
            if (currentId != idempotencyKey) return false
            prefs(context).edit()
                .putInt(KEY_ATTEMPT_COUNT, attemptCount)
                .putLong(KEY_NEXT_RETRY_AT, nextRetryAtMillis)
                .apply()
            return true
        }
    }

    fun updateTargetNodeId(context: Context, idempotencyKey: String, targetNodeId: String): Boolean {
        synchronized(lock) {
            val currentId = prefs(context).getString(KEY_IDEMPOTENCY, null) ?: return false
            if (currentId != idempotencyKey) return false
            prefs(context).edit().putString(KEY_TARGET_NODE_ID, targetNodeId).apply()
            return true
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
