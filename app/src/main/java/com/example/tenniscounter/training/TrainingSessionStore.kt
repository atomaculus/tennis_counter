package com.example.tenniscounter.training

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.playce.shared.training.TrainingSession
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.trainingSessionsDataStore by preferencesDataStore(name = "training_sessions")

/**
 * Local (watch-only) history of completed training sessions (paredón, 21).
 * Stored as a hand-serialized JSON array in a Preferences DataStore, capped
 * to the most recent [MAX_SESSIONS] entries — this is a lightweight local
 * log, not meant to be a full match history like the phone's Room database.
 */
object TrainingSessionStore {
    private val SESSIONS_JSON_KEY = stringPreferencesKey("sessions_json")
    private const val MAX_SESSIONS = 50

    /** Newest session first. */
    suspend fun addSession(context: Context, session: TrainingSession) {
        context.trainingSessionsDataStore.edit { prefs ->
            val current = decode(prefs[SESSIONS_JSON_KEY])
            val updated = (listOf(session) + current).take(MAX_SESSIONS)
            prefs[SESSIONS_JSON_KEY] = encode(updated)
        }
    }

    suspend fun readAll(context: Context): List<TrainingSession> {
        val json = context.trainingSessionsDataStore.data.first()[SESSIONS_JSON_KEY]
        return decode(json)
    }

    private fun encode(sessions: List<TrainingSession>): String {
        val array = JSONArray()
        sessions.forEach { session ->
            val obj = JSONObject()
            obj.put("id", session.id)
            obj.put("modality", session.modality)
            obj.put("createdAt", session.createdAt)
            obj.put("durationSeconds", session.durationSeconds)
            obj.put("targetPoints", session.targetPoints)
            obj.put("finalCount", session.finalCount)
            obj.put("bestStreak", session.bestStreak)
            obj.put("attempts", session.attempts)
            array.put(obj)
        }
        return array.toString()
    }

    /** Never throws — a corrupt value resolves to an empty list. */
    private fun decode(json: String?): List<TrainingSession> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        TrainingSession(
                            id = obj.optString("id"),
                            modality = obj.optString("modality"),
                            createdAt = obj.optLong("createdAt"),
                            durationSeconds = obj.optLong("durationSeconds"),
                            targetPoints = obj.optInt("targetPoints"),
                            finalCount = obj.optInt("finalCount"),
                            bestStreak = obj.optInt("bestStreak"),
                            attempts = obj.optInt("attempts")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
