package com.example.tenniscounter.timer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.example.tenniscounter.ui.TimerStateStore
import kotlinx.coroutines.runBlocking

class MatchTimerService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MatchTimerService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "MatchTimerService onStartCommand startId=$startId")
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
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MatchTimerService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val TAG = "MatchTimerService"
    }
}
