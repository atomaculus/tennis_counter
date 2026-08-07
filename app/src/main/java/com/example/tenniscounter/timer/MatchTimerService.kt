package com.example.tenniscounter.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.example.tenniscounter.MainActivity
import com.example.tenniscounter.R
import com.example.tenniscounter.ui.TimerSnapshot
import com.example.tenniscounter.ui.TimerStateStore
import com.example.tenniscounter.ui.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Keeps the match alive while the timer runs: promotes itself to a foreground
 * service with an Ongoing Activity chip on the watch face so the user can jump
 * back into the match with one tap. Observes [TimerStateStore] and stops itself
 * when the timer is no longer running (pause, reset, match ended).
 *
 * Swiping the app away from recents still consolidates and stops the timer
 * (see [onTaskRemoved]) — that is an explicit "I'm done" gesture.
 */
class MatchTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var stateWatchJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MatchTimerService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "MatchTimerService onStartCommand startId=$startId")
        val now = SystemClock.elapsedRealtime()
        val snapshot = runBlocking {
            TimerStateStore.ensureInitialized(applicationContext, now)
            TimerStateStore.read(applicationContext)
        }
        if (!snapshot.isRunning) {
            Log.i(TAG, "Timer not running on start — stopping service")
            stopSelf()
            return START_NOT_STICKY
        }
        promoteToForeground(snapshot)
        watchTimerState()
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val now = SystemClock.elapsedRealtime()
        Log.i(TAG, "MatchTimerService onTaskRemoved")
        runBlocking {
            TimerStateStore.ensureInitialized(applicationContext, now)
            val before = TimerStateStore.read(applicationContext)
            Log.i(
                TAG,
                "State before stop isRunning=${before.isRunning} accumulatedSeconds=${before.accumulatedSeconds}"
            )
            TimerStateStore.consolidateAndStop(applicationContext, now)
            val after = TimerStateStore.read(applicationContext)
            Log.i(
                TAG,
                "State after stop isRunning=${after.isRunning} accumulatedSeconds=${after.accumulatedSeconds}"
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
        Log.i(TAG, "MatchTimerService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun watchTimerState() {
        if (stateWatchJob?.isActive == true) return
        stateWatchJob = serviceScope.launch {
            applicationContext.dataStore.data
                .map { TimerStateStore.snapshotOf(it) }
                .distinctUntilChanged()
                .collect { snapshot ->
                    if (snapshot.isRunning) {
                        promoteToForeground(snapshot)
                    } else {
                        Log.i(TAG, "Timer stopped — leaving foreground")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
        }
    }

    private fun promoteToForeground(snapshot: TimerSnapshot) {
        val notificationBuilder = buildNotification(snapshot)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notificationBuilder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else {
                startForeground(NOTIFICATION_ID, notificationBuilder.build())
            }
        } catch (e: SecurityException) {
            // Health FGS needs a granted runtime permission (activity recognition /
            // body sensors). Without it we keep running as a plain started service,
            // which preserves the pre-N0 behavior.
            Log.w(TAG, "startForeground rejected — continuing as background service", e)
        }
    }

    private fun buildNotification(snapshot: TimerSnapshot): NotificationCompat.Builder {
        val touchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(getString(R.string.timer_notification_title))
            .setContentText(getString(R.string.timer_notification_text))
            .setContentIntent(touchIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Chip shows total match time counting up. StopwatchPart uses the
        // elapsedRealtime base, same as TimerStateStore.
        val stopwatchBase = snapshot.startElapsedRealtime - snapshot.accumulatedSeconds * 1000L
        val status = Status.Builder()
            .addTemplate("#time#")
            .addPart("time", Status.StopwatchPart(stopwatchBase))
            .build()

        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_notification_timer)
            .setTouchIntent(touchIntent)
            .setStatus(status)
            .build()
            .apply(applicationContext)

        return builder
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.timer_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    private companion object {
        const val TAG = "MatchTimerService"
        const val CHANNEL_ID = "match_timer"
        const val NOTIFICATION_ID = 1001
    }
}
