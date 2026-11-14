package com.example.tenniscounter.ui

/**
 * Orders active-session disk writes. Each save or clear takes a token;
 * a token from an earlier call is stale once a reset (or a newer snapshot)
 * has opened another one, so the pre-reset points cannot land after the clear.
 */
internal class ActiveSessionWriteGate {
    private var generation = 0

    fun open(): Int = ++generation

    fun isCurrent(token: Int): Boolean = token == generation
}
