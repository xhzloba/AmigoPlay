package com.example.movie.presentation.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.example.movie.presentation.haptic.HoldHaptic
import com.example.movie.presentation.haptic.rememberHoldHaptic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal const val HoldMillis = 800L
private const val CueAfterNanos = 260_000_000L

@Composable
internal fun Modifier.holdToPeek(
    onPeek: () -> Unit,
    onProgress: (Float) -> Unit,
): Modifier {
    val peek by rememberUpdatedState(onPeek)
    val report by rememberUpdatedState(onProgress)
    val haptic = rememberHoldHaptic()
    val motor = remember { arrayOfNulls<HoldHaptic>(1) }
    val reporter = remember { arrayOf<(Float) -> Unit>({ }) }
    motor[0] = haptic
    reporter[0] = report
    return pointerInput(Unit) {
        val slop = viewConfiguration.touchSlop
        var epoch = 0
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val origin = down.position
                val token = ++epoch
                val frames = launch {
                    val t0 = System.nanoTime()
                    var buzzing = false
                    while (isActive && epoch == token) {
                        val elapsed = System.nanoTime() - t0
                        if (epoch != token) return@launch
                        if (elapsed >= CueAfterNanos) {
                            if (!buzzing) {
                                buzzing = true
                                motor[0]?.start()
                            }
                            val span = (HoldMillis * 1_000_000L - CueAfterNanos).toFloat()
                            val progress = ((elapsed - CueAfterNanos).toFloat() / span).coerceIn(0f, 1f)
                            reporter[0].invoke(progress)
                        }
                        if (elapsed >= HoldMillis * 1_000_000L) break
                        delay(16)
                    }
                }
                try {
                    val endedEarly = withTimeoutOrNull(HoldMillis) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            val left = change == null || !change.pressed ||
                                (change.position - origin).getDistance() > slop
                            if (left) return@withTimeoutOrNull true
                        }
                        @Suppress("UNREACHABLE_CODE")
                        true
                    }
                    if (endedEarly != null) return@awaitEachGesture
                    motor[0]?.stop()
                    peek()
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                        if (event.changes.none { it.pressed }) break
                    }
                } finally {
                    epoch++
                    frames.cancel()
                    motor[0]?.stop()
                    reporter[0].invoke(0f)
                }
            }
        }
    }
}

internal fun Modifier.holdCue(progress: Float): Modifier = drawWithContent {
    drawContent()
    if (progress > 0.01f) drawHoldCue(progress)
}

private fun DrawScope.drawHoldCue(progress: Float) {
    val amount = smoothstep(progress)
    drawRect(Color.Black.copy(alpha = 0.08f * amount))
    drawRainbowRipple(amount)
}

private fun smoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private val Rainbow = listOf(
    Color(0xFF64D2FF),
    Color(0xFF5E5CE6),
    Color(0xFFFF375F),
    Color(0xFFFF9F0A),
    Color(0xFFFFD60A),
    Color(0xFF30D158),
)

private fun DrawScope.drawRainbowRipple(amount: Float) {
    val reach = size.width * 0.42f
    repeat(Rainbow.size) { index ->
        val base = index / Rainbow.size.toFloat()
        val y = 0.08f + base * 0.76f + amount * 0.08f
        val color = Rainbow[index]
        val opposite = Rainbow[(index + Rainbow.size / 2) % Rainbow.size]
        drawSideLight(Offset(0f, size.height * y), color, reach, amount)
        drawSideLight(Offset(size.width, size.height * (1.08f - y)), opposite, reach, amount)
    }
}

private fun DrawScope.drawSideLight(spot: Offset, color: Color, reach: Float, strength: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = 0.58f * strength),
                0.5f to color.copy(alpha = 0.20f * strength),
                1f to Color.Transparent,
            ),
            center = spot,
            radius = reach,
        ),
        radius = reach,
        center = spot,
        blendMode = BlendMode.Screen,
    )
}
