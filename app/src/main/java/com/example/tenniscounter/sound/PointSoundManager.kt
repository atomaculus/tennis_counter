package com.example.tenniscounter.sound

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Plays distinct tones when points are scored for Player A vs Player B.
 * Uses Android's built-in ToneGenerator — no audio files needed.
 */
class PointSoundManager {

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME)
    } catch (_: RuntimeException) {
        // ToneGenerator can fail on some devices (no audio hardware, etc.)
        null
    }

    /** High-pitched short beep for Player A. */
    fun playPlayerASound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, DURATION_MS)
    }

    /** Lower-pitched beep for Player B. */
    fun playPlayerBSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, DURATION_MS)
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    private companion object {
        const val VOLUME = 80        // 0-100
        const val DURATION_MS = 150  // short beep
    }
}
