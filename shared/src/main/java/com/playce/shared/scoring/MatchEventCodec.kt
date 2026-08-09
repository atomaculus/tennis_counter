package com.playce.shared.scoring

/**
 * Manual JSON serialization for [ScoringEngine.MatchEvent] history.
 *
 * Output format matches the Garmin motor's event type strings ("point",
 * "deciding_tb") so the same JSON can be parsed on either side.
 */
object MatchEventCodec {

    /**
     * Serializes [events] to a JSON array string, e.g.:
     * `[{"type":"point","isPlayerA":true,"t":1723050000123},{"type":"deciding_tb","targetPoints":10,"t":1723050100456}]`
     *
     * Built manually with StringBuilder — fields are only bools/longs/ints,
     * so no string escaping is needed.
     */
    fun toJson(events: List<ScoringEngine.MatchEvent>): String {
        if (events.isEmpty()) return "[]"

        val sb = StringBuilder()
        sb.append('[')
        events.forEachIndexed { index, event ->
            if (index > 0) sb.append(',')
            when (event) {
                is ScoringEngine.MatchEvent.Point -> {
                    sb.append("{\"type\":\"point\",\"isPlayerA\":")
                        .append(event.isPlayerA)
                        .append(",\"t\":")
                        .append(event.timestampMillis)
                        .append('}')
                }
                is ScoringEngine.MatchEvent.DecidingTiebreakStarted -> {
                    sb.append("{\"type\":\"deciding_tb\",\"targetPoints\":")
                        .append(event.targetPoints)
                        .append(",\"t\":")
                        .append(event.timestampMillis)
                        .append('}')
                }
            }
        }
        sb.append(']')
        return sb.toString()
    }

    /**
     * Parses a JSON array string produced by [toJson] back into a list of
     * [ScoringEngine.MatchEvent]. Hand-rolled, no external dependencies.
     *
     * Tolerant of an empty array (`[]`) and surrounding/interior whitespace.
     * Never throws — any malformed input (missing brackets, unknown "type",
     * missing required field, garbage text) resolves to an empty list, since
     * this is used to restore a persisted session and a corrupt value must
     * not crash the app.
     */
    fun fromJson(json: String): List<ScoringEngine.MatchEvent> {
        return try {
            parseEvents(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val TYPE_REGEX = Regex("\"type\"\\s*:\\s*\"(\\w+)\"")
    private val IS_PLAYER_A_REGEX = Regex("\"isPlayerA\"\\s*:\\s*(true|false)")
    private val TARGET_POINTS_REGEX = Regex("\"targetPoints\"\\s*:\\s*(-?\\d+)")
    private val TIMESTAMP_REGEX = Regex("\"t\"\\s*:\\s*(-?\\d+)")

    private fun parseEvents(json: String): List<ScoringEngine.MatchEvent>? {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.first() != '[' || trimmed.last() != ']') return null
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isEmpty()) return emptyList()

        val events = mutableListOf<ScoringEngine.MatchEvent>()
        var i = 0
        val n = inner.length
        while (i < n) {
            while (i < n && (inner[i].isWhitespace() || inner[i] == ',')) i++
            if (i >= n) break
            if (inner[i] != '{') return null
            val end = inner.indexOf('}', i)
            if (end == -1) return null
            val objStr = inner.substring(i, end + 1)
            val event = parseEvent(objStr) ?: return null
            events.add(event)
            i = end + 1
        }
        return events
    }

    private fun parseEvent(obj: String): ScoringEngine.MatchEvent? {
        val type = TYPE_REGEX.find(obj)?.groupValues?.get(1) ?: return null
        val timestamp = TIMESTAMP_REGEX.find(obj)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        return when (type) {
            "point" -> {
                val isPlayerA = IS_PLAYER_A_REGEX.find(obj)?.groupValues?.get(1)?.toBooleanStrictOrNull()
                    ?: return null
                ScoringEngine.MatchEvent.Point(isPlayerA = isPlayerA, timestampMillis = timestamp)
            }
            "deciding_tb" -> {
                val targetPoints = TARGET_POINTS_REGEX.find(obj)?.groupValues?.get(1)?.toIntOrNull()
                    ?: return null
                ScoringEngine.MatchEvent.DecidingTiebreakStarted(
                    targetPoints = targetPoints,
                    timestampMillis = timestamp
                )
            }
            else -> null
        }
    }
}
