package com.example.tenniscounter.mobile.garmin

object GarminPayloadCodec {

    fun asEnvelope(message: Any?): Map<String, Any?>? {
        if (message !is Map<*, *>) return null
        @Suppress("UNCHECKED_CAST")
        return message as Map<String, Any?>
    }

    fun asPayloadMap(value: Any?): Map<String, Any?>? {
        if (value !is Map<*, *>) return null
        @Suppress("UNCHECKED_CAST")
        return value as Map<String, Any?>
    }

    fun getString(map: Map<String, Any?>, key: String, default: String = ""): String {
        return when (val v = map[key]) {
            is String -> v
            null -> default
            else -> v.toString()
        }
    }

    fun getStringOrNull(map: Map<String, Any?>, key: String): String? {
        return when (val v = map[key]) {
            is String -> v.ifBlank { null }
            null -> null
            else -> v.toString().ifBlank { null }
        }
    }

    fun getInt(map: Map<String, Any?>, key: String, default: Int = 0): Int {
        return when (val v = map[key]) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: default
            else -> default
        }
    }

    fun getLong(map: Map<String, Any?>, key: String, default: Long = 0L): Long {
        return when (val v = map[key]) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: default
            else -> default
        }
    }

    fun getBoolean(map: Map<String, Any?>, key: String, default: Boolean = false): Boolean {
        return when (val v = map[key]) {
            is Boolean -> v
            is Number -> v.toInt() != 0
            is String -> v.equals("true", ignoreCase = true)
            else -> default
        }
    }
}
