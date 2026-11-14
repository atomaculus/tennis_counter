package com.playce.shared.training

/**
 * In-progress "21" training counter, shared by the phone and Wear screens.
 *
 * +1 advances the current streak up to [target]. A miss ends that attempt,
 * resets the streak to 0, and keeps [bestStreak]. Reaching the target locks
 * the counter: further +1 or miss calls do nothing, matching the UI, which
 * swaps the buttons for save-and-exit.
 */
data class VeintiunoSession(
    val count: Int = 0,
    val bestStreak: Int = 0,
    val misses: Int = 0,
    val hasActivity: Boolean = false,
    val target: Int = TARGET
) {
    val reachedTarget: Boolean get() = count >= target

    fun plusOne(): VeintiunoSession {
        if (reachedTarget) return this
        val next = count + 1
        return copy(
            count = next,
            bestStreak = maxOf(bestStreak, next),
            hasActivity = true
        )
    }

    fun miss(): VeintiunoSession {
        if (reachedTarget) return this
        return copy(
            count = 0,
            misses = misses + 1,
            hasActivity = true
        )
    }

    /**
     * Attempt count stored with the session: every miss, plus the streak that
     * is still open or that just reached the target. The screens only persist
     * a session when [hasActivity] is true.
     */
    fun savedAttempts(): Int = misses + 1

    companion object {
        const val TARGET = 21
    }
}
