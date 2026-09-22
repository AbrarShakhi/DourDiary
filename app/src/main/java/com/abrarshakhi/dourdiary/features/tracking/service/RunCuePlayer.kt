package com.abrarshakhi.dourdiary.features.tracking.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.getSystemService
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.format.RunFormatter
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunCue
import java.util.concurrent.atomic.AtomicBoolean

class RunCuePlayer(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private val speechReady = AtomicBoolean(false)
    private val audioManager = context.getSystemService<AudioManager>()
    private var focusRequest: AudioFocusRequest? = null

    fun initialise() {
        if (textToSpeech != null) return
        textToSpeech = TextToSpeech(context) { status ->
            speechReady.set(status == TextToSpeech.SUCCESS)
        }.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = abandonAudioFocus()

                @Deprecated("Required by the abstract class", ReplaceWith(""))
                override fun onError(utteranceId: String?) = abandonAudioFocus()
            })
        }
    }

    fun play(cue: RunCue, unitSystem: UnitSystem) {
        vibrate()
        speak(cue, unitSystem)
    }

    fun release() {
        abandonAudioFocus()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        speechReady.set(false)
    }

    private fun speak(cue: RunCue, unitSystem: UnitSystem) {
        val engine = textToSpeech?.takeIf { speechReady.get() } ?: return

        val distanceSpoken = when {
            unitSystem == UnitSystem.METRIC && cue.milestone == 1 ->
                context.getString(R.string.cue_distance_metric_one)

            unitSystem == UnitSystem.METRIC ->
                context.getString(R.string.cue_distance_metric, cue.milestone)

            cue.milestone == 1 -> context.getString(R.string.cue_distance_imperial_one)
            else -> context.getString(R.string.cue_distance_imperial, cue.milestone)
        }

        val unitSpoken = context.getString(
            when (unitSystem) {
                UnitSystem.METRIC -> R.string.cue_unit_kilometre
                UnitSystem.IMPERIAL -> R.string.cue_unit_mile
            },
        )

        val announcement = context.getString(
            R.string.cue_announcement,
            distanceSpoken,
            RunFormatter.duration(cue.movingDurationMillis),
            RunFormatter.pace(cue.averagePace, unitSystem),
            unitSpoken,
        )

        requestAudioFocus()
        engine.speak(announcement, TextToSpeech.QUEUE_ADD, null, "cue-${cue.milestone}")
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        } ?: return

        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(220L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .build()
        focusRequest = request
        manager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        focusRequest?.let { manager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
