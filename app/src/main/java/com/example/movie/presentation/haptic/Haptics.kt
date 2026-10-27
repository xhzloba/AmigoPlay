package com.example.movie.presentation.haptic

import android.os.VibrationEffect
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

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

@Composable
internal fun rememberStrongHaptic(): (() -> Unit)? {
    val view = LocalView.current
    val hapticOn by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val vibrator = remember(view) {
        view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }
    val buzz = remember(vibrator) {
        vibrator?.let { motor ->
            {
                val effect = if (motor.hasAmplitudeControl()) {
                    VibrationEffect.createOneShot(700, 255)
                } else {
                    VibrationEffect.createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                motor.vibrate(effect)
            }
        }
    }
    return if (vibrator?.hasVibrator() == true && hapticOn.haptic) buzz else null
}

@Composable
internal fun HapticOnChange(key: Any, read: () -> Any?) {
    val tick = rememberHapticTick()
    LaunchedEffect(key, tick) {
        val perform = tick ?: return@LaunchedEffect
        snapshotFlow(read)
            .distinctUntilChanged()
            .drop(1)
            .collect { perform() }
    }
}
