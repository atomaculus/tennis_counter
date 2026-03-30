package com.example.tenniscounter.mobile.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.tenniscounter.mobile.MainActivity
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlayceWidgetProvider : AppWidgetProvider() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        scope.launch {
            val lastMatch = try {
                val dao = MobileServiceLocator.matchDao(context.applicationContext)
                dao.getLatestMatch()
            } catch (_: Exception) {
                null
            }

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_playce)

                if (lastMatch != null) {
                    views.setTextViewText(R.id.widget_last_score, lastMatch.finalScoreText)
                    val detail = lastMatch.setScoresText ?: "Match completed"
                    views.setTextViewText(R.id.widget_last_detail, detail)
                } else {
                    views.setTextViewText(R.id.widget_last_score, "No matches yet")
                    views.setTextViewText(R.id.widget_last_detail, "Tap to start a match")
                }

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
