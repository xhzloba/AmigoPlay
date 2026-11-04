package com.example.movie.presentation.haptic

import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.presentation.settings.LocalUserSettings
import kotlin.math.abs

@Composable
internal fun rememberHapticTick(): (() -> Unit)? {
    val view = LocalView.current
    val hapticOn by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val hardware = remember(view) {
        view.context.getSystemService(VibratorManager::class.java)
            ?.defaultVibrator
            ?.hasVibrator() == true
    }
    val tick = remember(view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            Unit
        }
    }
    return if (hardware && hapticOn.haptic) tick else null
}

internal class HoldHaptic(private val vibrator: Vibrator) {
    fun start() {
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(longArrayOf(0, 600), intArrayOf(0, 255), 0)
        } else {
            VibrationEffect.createWaveform(longArrayOf(0, 600), 0)
        }
        val usage = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
        vibrator.vibrate(effect, usage)
    }

    fun stop() {
        vibrator.cancel()
    }
}

@Composable
internal fun rememberHoldHaptic(): HoldHaptic? {
    val view = LocalView.current
    val hapticOn by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val vibrator = remember(view) {
        view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }
    val hold = remember(vibrator) { vibrator?.let(::HoldHaptic) }
    return if (vibrator?.hasVibrator() == true && hapticOn.haptic) hold else null
}

@Composable
internal fun HapticOnChange(key: Any, read: () -> Any?) {
    val ticks = rememberScrollTicks()
    LaunchedEffect(key, ticks) {
        val perform = ticks ?: return@LaunchedEffect
        var previous: Any? = null
        var primed = false
        snapshotFlow(read).collect { value ->
            if (!primed) {
                previous = value
                primed = true
                return@collect
            }
            if (value == previous) return@collect
            val count = if (value is Int && previous is Int) {
                abs(value - previous as Int).coerceIn(1, 8)
            } else {
                1
            }
            previous = value
            perform(count)
        }
    }
}

@Composable
private fun rememberScrollTicks(): ((Int) -> Unit)? {
    val view = LocalView.current
    val hapticOn by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val vibrator = remember(view) {
        view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }
    val play = remember(vibrator) {
        vibrator?.let { motor ->
            { count: Int ->
                val usage = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
                val pulses = count.coerceIn(1, 8)
                if (pulses == 1) {
                    motor.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK), usage)
                } else {
                    val timings = LongArray(pulses * 2)
                    val amplitudes = IntArray(pulses * 2)
                    for (index in 0 until pulses) {
                        timings[index * 2] = if (index == 0) 0L else 16L
                        timings[index * 2 + 1] = 12L
                        amplitudes[index * 2] = 0
                        amplitudes[index * 2 + 1] = 180
                    }
                    val effect = if (motor.hasAmplitudeControl()) {
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    } else {
                        VibrationEffect.createWaveform(timings, -1)
                    }
                    motor.vibrate(effect, usage)
                }
            }
        }
    }
    return if (vibrator?.hasVibrator() == true && hapticOn.haptic) play else null
}

@Composable
internal fun HapticOnAppend(ids: () -> List<String>) {
    val ticks = rememberScrollTicks()
    LaunchedEffect(ticks) {
        val perform = ticks ?: return@LaunchedEffect
        var previous = emptyList<String>()
        snapshotFlow(ids).collect { value ->
            val grew = previous.isNotEmpty() &&
                value.size > previous.size &&
                value.subList(0, previous.size) == previous
            previous = value
            if (grew) perform(1)
        }
    }
}
