package com.example.tenniscounter.spike

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.tenniscounter.R
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * N1 calibration spike: logs raw accelerometer + gyroscope at ~100 Hz to a CSV
 * on the watch while the user hits against a wall. Dev tooling only — launched
 * from [SensorSpikeLoggerActivity], never from the main app flow.
 *
 * CSV rows: `elapsed_ns,sensor,x,y,z` with sensor A (accel), G (gyro) or
 * M (marker tapped between series; x/y/z are 0). Files land in
 * `getExternalFilesDir(null)/sensor_spike/` for adb pull.
 */
class SensorSpikeLoggerService : Service(), SensorEventListener {

    data class SpikeUiState(
        val isLogging: Boolean = false,
        val sampleCount: Long = 0L,
        val markerCount: Int = 0,
        val fileName: String? = null
    )

    private var sensorManager: SensorManager? = null
    private var sensorThread: HandlerThread? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var writer: BufferedWriter? = null
    private var startElapsedNanos = 0L
    private var sampleCount = 0L
    private var markerCount = 0

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sensor spike logger",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startLogging()
            ACTION_MARK -> mark()
            ACTION_STOP -> stopLogging()
        }
        return START_NOT_STICKY
    }

    private fun startLogging() {
        if (uiState.value.isLogging) return

        val dir = File(getExternalFilesDir(null), "sensor_spike").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "spike_$stamp.csv")
        writer = file.bufferedWriter().apply {
            write("# PLAYCE sensor spike ${Build.MODEL} api=${Build.VERSION.SDK_INT} rate_hz=$TARGET_HZ")
            newLine()
            write("elapsed_ns,sensor,x,y,z")
            newLine()
        }
        startElapsedNanos = SystemClock.elapsedRealtimeNanos()
        sampleCount = 0
        markerCount = 0

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle("Sensor spike")
            .setContentText("Logging ${file.name}")
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, SensorSpikeLoggerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "startForeground rejected; logging without foreground", e)
        }

        wakeLock = getSystemService<PowerManager>()
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "playce:sensor_spike")
            ?.apply { acquire(MAX_SESSION_MS) }

        // Sensor callbacks (and the CSV writes they do) stay off the main thread.
        val thread = HandlerThread("sensor-spike").apply { start() }
        sensorThread = thread
        val handler = Handler(thread.looper)
        sensorManager = getSystemService<SensorManager>()?.also { sm ->
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sm.registerListener(this, it, SAMPLING_PERIOD_US, handler)
            }
            sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
                sm.registerListener(this, it, SAMPLING_PERIOD_US, handler)
            }
        }

        uiState.value = SpikeUiState(isLogging = true, fileName = file.name)
        Log.i(TAG, "Spike logging started: ${file.absolutePath}")
    }

    private fun mark() {
        val w = writer ?: return
        val t = SystemClock.elapsedRealtimeNanos() - startElapsedNanos
        try {
            synchronized(w) {
                w.write("$t,M,0,0,0")
                w.newLine()
                w.flush()
            }
        } catch (e: java.io.IOException) {
            return
        }
        markerCount++
        uiState.value = uiState.value.copy(markerCount = markerCount)
    }

    private fun stopLogging() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        sensorThread?.quitSafely()
        sensorThread = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        writer?.let { w ->
            synchronized(w) {
                w.flush()
                w.close()
            }
        }
        writer = null
        uiState.value = uiState.value.copy(isLogging = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "Spike logging stopped after $sampleCount samples, $markerCount markers")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val w = writer ?: return
        val tag = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> "A"
            Sensor.TYPE_GYROSCOPE -> "G"
            else -> return
        }
        val t = event.timestamp - startElapsedNanos
        try {
            synchronized(w) {
                w.write("$t,$tag,${event.values[0]},${event.values[1]},${event.values[2]}")
                w.newLine()
            }
            sampleCount++
            if (sampleCount % FLUSH_EVERY == 0L) {
                synchronized(w) { w.flush() }
                uiState.value = uiState.value.copy(sampleCount = sampleCount)
            }
        } catch (e: java.io.IOException) {
            // Writer closed by a concurrent stop — drop the sample.
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        if (uiState.value.isLogging) stopLogging()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        val uiState: MutableStateFlow<SpikeUiState> = MutableStateFlow(SpikeUiState())
        val state: StateFlow<SpikeUiState> get() = uiState

        const val ACTION_START = "com.example.tenniscounter.spike.START"
        const val ACTION_MARK = "com.example.tenniscounter.spike.MARK"
        const val ACTION_STOP = "com.example.tenniscounter.spike.STOP"

        private const val TAG = "SensorSpike"
        private const val CHANNEL_ID = "sensor_spike"
        private const val NOTIFICATION_ID = 1002
        private const val TARGET_HZ = 100
        private const val SAMPLING_PERIOD_US = 10_000
        private const val FLUSH_EVERY = 500L
        private const val MAX_SESSION_MS = 30 * 60 * 1000L

        fun send(context: Context, action: String) {
            context.startService(
                Intent(context, SensorSpikeLoggerService::class.java).setAction(action)
            )
        }
    }
}
