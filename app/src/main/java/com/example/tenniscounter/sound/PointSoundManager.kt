package com.example.tenniscounter.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays distinct tones when points are scored for Player A vs Player B.
 * Uses AudioTrack with generated sine waves — works reliably on all devices
 * including Wear OS watches where ToneGenerator is often silent.
 */
class PointSoundManager {

    /** High-pitched short beep for Player A (880 Hz). */
    fun playPlayerASound() {
        playTone(FREQ_PLAYER_A, DURATION_MS)
    }

    /** Lower-pitched beep for Player B (520 Hz). */
    fun playPlayerBSound() {
        playTone(FREQ_PLAYER_B, DURATION_MS)
    }

    fun release() {
        // No persistent resources to clean up — each tone creates a one-shot AudioTrack
    }

    private fun playTone(freqHz: Double, durationMs: Int) {
        try {
            val numSamples = (SAMPLE_RATE * durationMs / 1000.0).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val sample = sin(2.0 * PI * freqHz * i / SAMPLE_RATE)
                // Apply short fade-in/out to avoid click artifacts
                val envelope = when {
                    i < FADE_SAMPLES -> i.toDouble() / FADE_SAMPLES
                    i > numSamples - FADE_SAMPLES -> (numSamples - i).toDouble() / FADE_SAMPLES
                    else -> 1.0
                }
                buffer[i] = (sample * envelope * Short.MAX_VALUE).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.setNotificationMarkerPosition(numSamples)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) { t.release() }
                override fun onPeriodicNotification(t: AudioTrack) {}
            })
            track.play()
        } catch (_: Exception) {
            // Silently ignore — audio might be unavailable
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44100
        const val DURATION_MS = 150
        const val FADE_SAMPLES = 200
        const val FREQ_PLAYER_A = 880.0  // A5 — bright, high
        const val FREQ_PLAYER_B = 520.0  // ~C5 — noticeably lower
    }
}
