package com.example.ui.keyboard

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants

enum class SoundType {
    SYSTEM_CLICK,
    MECHANICAL_CLICK,
    POP_BUBBLE,
    TYPEWRITER,
    OFF
}

enum class HapticStrength {
    OFF,
    LIGHT,
    MEDIUM,
    STRONG
}

class KeyboardSoundManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var toneGenerator: ToneGenerator? = null

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 60)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    fun playSound(soundType: SoundType, volume: Float = 1.0f) {
        if (soundType == SoundType.OFF) return

        try {
            when (soundType) {
                SoundType.SYSTEM_CLICK -> {
                    audioManager?.playSoundEffect(SoundEffectConstants.CLICK, volume)
                }
                SoundType.MECHANICAL_CLICK -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 15)
                }
                SoundType.POP_BUBBLE -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RADIO_ACK, 20)
                }
                SoundType.TYPEWRITER -> {
                    audioManager?.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT, volume * 1.2f)
                }
                SoundType.OFF -> {}
            }
        } catch (e: Exception) {
            // Ignore sound playback issues
        }
    }

    fun performHaptic(strength: HapticStrength) {
        if (strength == HapticStrength.OFF || vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = when (strength) {
                    HapticStrength.OFF -> 0
                    HapticStrength.LIGHT -> 40
                    HapticStrength.MEDIUM -> 120
                    HapticStrength.STRONG -> 255
                }
                val duration = when (strength) {
                    HapticStrength.OFF -> 0L
                    HapticStrength.LIGHT -> 15L
                    HapticStrength.MEDIUM -> 25L
                    HapticStrength.STRONG -> 40L
                }
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                val duration = when (strength) {
                    HapticStrength.OFF -> 0L
                    HapticStrength.LIGHT -> 10L
                    HapticStrength.MEDIUM -> 25L
                    HapticStrength.STRONG -> 50L
                }
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            // Ignore haptic errors
        }
    }
}
