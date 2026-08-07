package com.playce.shared.scoring

/**
 * Manual JSON serialization for [ScoringEngine.MatchEvent] history.
 *
 * Output format matches the Garmin motor's event type strings ("point",
 * "deciding_tb") so the same JSON can be parsed on either side. No parser is
 * provided here — reading arrives in N2.
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
}
