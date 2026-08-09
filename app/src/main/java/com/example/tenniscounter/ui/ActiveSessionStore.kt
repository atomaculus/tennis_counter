package com.example.tenniscounter.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.activeSessionDataStore by preferencesDataStore(name = "active_session")

/** Everything needed to fully rebuild the scoreboard for a match in progress. */
data class ActiveSessionSnapshot(
    val eventsJson: String,
    val playerAName: String,
    val playerBName: String,
    val matchFormat: String,
    val initialServerIsPlayerA: Boolean?,
    val updatedAtMillis: Long
)

/**
 * Persists the match currently in progress (as [ActiveSessionSnapshot]) so it
 * survives process death / app restart, and remembers which mode (match,
 * paredón, 21) the user picked last so the mode selector can highlight it.
 *
 * Pattern mirrors [TimerStateStore]: a plain object over a Preferences
 * DataStore, all functions taking [Context] per call, no DI framework.
 */
object ActiveSessionStore {
    private val EVENTS_JSON_KEY = stringPreferencesKey("events_json")
    private val PLAYER_A_NAME_KEY = stringPreferencesKey("player_a_name")
    private val PLAYER_B_NAME_KEY = stringPreferencesKey("player_b_name")
    private val MATCH_FORMAT_KEY = stringPreferencesKey("match_format")
    private val HAS_INITIAL_SERVER_KEY = booleanPreferencesKey("has_initial_server")
    private val INITIAL_SERVER_IS_PLAYER_A_KEY = booleanPreferencesKey("initial_server_is_player_a")
    private val UPDATED_AT_MILLIS_KEY = longPreferencesKey("updated_at_millis")
    private val LAST_MODE_KEY = stringPreferencesKey("last_mode")

    /** A snapshot older than this is considered stale and is not restored. */
    private const val MAX_SNAPSHOT_AGE_MILLIS = 12 * 60 * 60 * 1000L

    const val MODE_MATCH = "match"
    const val MODE_PAREDON = "paredon"
    const val MODE_VEINTIUNO = "veintiuno"

    suspend fun saveMatchSnapshot(
        context: Context,
        eventsJson: String,
        playerAName: String,
        playerBName: String,
        matchFormat: String,
        initialServerIsPlayerA: Boolean?
    ) {
        context.activeSessionDataStore.edit { prefs ->
            prefs[EVENTS_JSON_KEY] = eventsJson
            prefs[PLAYER_A_NAME_KEY] = playerAName
            prefs[PLAYER_B_NAME_KEY] = playerBName
            prefs[MATCH_FORMAT_KEY] = matchFormat
            prefs[HAS_INITIAL_SERVER_KEY] = initialServerIsPlayerA != null
            if (initialServerIsPlayerA != null) {
                prefs[INITIAL_SERVER_IS_PLAYER_A_KEY] = initialServerIsPlayerA
            } else {
                prefs.remove(INITIAL_SERVER_IS_PLAYER_A_KEY)
            }
            prefs[UPDATED_AT_MILLIS_KEY] = System.currentTimeMillis()
        }
    }

    /** Null when nothing is saved, or the saved snapshot is older than 12h. */
    suspend fun readActive(context: Context): ActiveSessionSnapshot? {
        val prefs = context.activeSessionDataStore.data.first()
        val eventsJson = prefs[EVENTS_JSON_KEY] ?: return null
        val updatedAt = prefs[UPDATED_AT_MILLIS_KEY] ?: return null
        val age = System.currentTimeMillis() - updatedAt
        if (age !in 0..MAX_SNAPSHOT_AGE_MILLIS) return null

        val hasInitialServer = prefs[HAS_INITIAL_SERVER_KEY] ?: false
        return ActiveSessionSnapshot(
            eventsJson = eventsJson,
            playerAName = prefs[PLAYER_A_NAME_KEY] ?: "",
            playerBName = prefs[PLAYER_B_NAME_KEY] ?: "",
            matchFormat = prefs[MATCH_FORMAT_KEY] ?: "",
            initialServerIsPlayerA = if (hasInitialServer) prefs[INITIAL_SERVER_IS_PLAYER_A_KEY] else null,
            updatedAtMillis = updatedAt
        )
    }

    suspend fun clear(context: Context) {
        context.activeSessionDataStore.edit { prefs ->
            prefs.remove(EVENTS_JSON_KEY)
            prefs.remove(PLAYER_A_NAME_KEY)
            prefs.remove(PLAYER_B_NAME_KEY)
            prefs.remove(MATCH_FORMAT_KEY)
            prefs.remove(HAS_INITIAL_SERVER_KEY)
            prefs.remove(INITIAL_SERVER_IS_PLAYER_A_KEY)
            prefs.remove(UPDATED_AT_MILLIS_KEY)
            // last_mode is intentionally kept: it survives clears so the mode
            // selector keeps highlighting the last mode the user picked.
        }
    }

    suspend fun saveLastMode(context: Context, mode: String) {
        context.activeSessionDataStore.edit { prefs ->
            prefs[LAST_MODE_KEY] = mode
        }
    }

    suspend fun readLastMode(context: Context): String? {
        return context.activeSessionDataStore.data.first()[LAST_MODE_KEY]
    }
}
