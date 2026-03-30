package com.example.tenniscounter.mobile.review

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Manages in-app review prompts. Shows the review dialog after the user
 * completes a configurable number of matches.
 *
 * Google's API handles rate-limiting internally — calling launchReview
 * more often than allowed is a no-op, not an error.
 */
object InAppReviewManager {
    private const val PREFS_NAME = "playce_review"
    private const val KEY_MATCH_COUNT = "completed_match_count"
    private const val KEY_REVIEW_REQUESTED = "review_requested"
    private const val MATCHES_BEFORE_REVIEW = 3
    private const val TAG = "InAppReview"

    /**
     * Call this after each completed match to increment the counter.
     * If the threshold is met, triggers the in-app review flow.
     */
    fun onMatchCompleted(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_MATCH_COUNT, 0) + 1
        prefs.edit().putInt(KEY_MATCH_COUNT, count).apply()

        if (count >= MATCHES_BEFORE_REVIEW && !prefs.getBoolean(KEY_REVIEW_REQUESTED, false)) {
            requestReview(activity)
            prefs.edit().putBoolean(KEY_REVIEW_REQUESTED, true).apply()
        }
    }

    private fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener {
                        Log.d(TAG, "Review flow completed")
                    }
            } else {
                Log.w(TAG, "Review request failed", task.exception)
            }
        }
    }
}
