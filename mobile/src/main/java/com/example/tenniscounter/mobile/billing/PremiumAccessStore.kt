package com.example.tenniscounter.mobile.billing

import android.content.Context

object PremiumAccessStore {
    private const val PREFS_NAME = "playce_premium"
    private const val KEY_PREMIUM_UNLOCKED = "premium_unlocked"

    fun isPremiumUnlocked(context: Context): Boolean {
        return context
            .applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREMIUM_UNLOCKED, false)
    }

    fun setPremiumUnlocked(context: Context, value: Boolean) {
        context
            .applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PREMIUM_UNLOCKED, value)
            .apply()
    }
}
