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
import kotlin.math.PI
import kotlin.math.sin
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
    val center = Offset(size.width * 0.5f, size.height * 0.46f)
    drawVignette(center, amount)
    drawFocusRing(center, amount)
    drawFlare(amount)
    drawCore(center, amount)
}

private fun smoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun DrawScope.drawVignette(center: Offset, amount: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.42f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.5f * amount),
            ),
            center = center,
            radius = size.maxDimension * 0.78f,
        ),
    )
}

private fun DrawScope.drawFocusRing(center: Offset, amount: Float) {
    val envelope = sin(amount * PI.toFloat())
    if (envelope < 0.02f) return
    val band = lerp(size.minDimension * 0.22f, size.minDimension * 0.09f, amount)
    drawSoftRing(
        center = center,
        radius = lerp(size.maxDimension * 0.58f, size.minDimension * 0.16f, amount),
        band = band,
        color = Color.White.copy(alpha = 0.55f * envelope),
    )
    drawSoftRing(
        center = center,
        radius = lerp(size.maxDimension * 0.74f, size.minDimension * 0.30f, amount),
        band = band * 1.35f,
        color = Color(0xFFD6E4FF).copy(alpha = 0.22f * envelope),
    )
}

private fun DrawScope.drawFlare(amount: Float) {
    val peak = sin(amount * PI.toFloat()).let { it * it }
    if (peak < 0.02f) return
    val y = 0.47f
    val half = 0.045f
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                (y - half * 2.4f) to Color.Transparent,
                (y - half) to Color.White.copy(alpha = 0.10f * peak),
                y to Color.White.copy(alpha = 0.34f * peak),
                (y + half) to Color.White.copy(alpha = 0.10f * peak),
                (y + half * 2.4f) to Color.Transparent,
                1f to Color.Transparent,
            ),
        ),
        blendMode = BlendMode.Screen,
    )
}

private fun DrawScope.drawCore(center: Offset, amount: Float) {
    val gather = smoothstep(((amount - 0.28f) / 0.72f).coerceIn(0f, 1f))
    if (gather < 0.02f) return
    val radius = size.minDimension * 0.42f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = 0.40f * gather),
                0.4f to Color(0xFFD6E4FF).copy(alpha = 0.14f * gather),
                1f to Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
        blendMode = BlendMode.Screen,
    )
}

private fun DrawScope.drawSoftRing(center: Offset, radius: Float, band: Float, color: Color) {
    val outer = (radius + band).coerceAtLeast(1f)
    val mid = (radius / outer).coerceIn(0.08f, 0.92f)
    val inner = ((radius - band) / outer).coerceIn(0f, mid - 0.04f)
    val stops = if (inner > 0.02f) {
        arrayOf(
            0f to Color.Transparent,
            inner to Color.Transparent,
            mid to color,
            1f to Color.Transparent,
        )
    } else {
        arrayOf(
            0f to color.copy(alpha = color.alpha * 0.35f),
            mid to color,
            1f to Color.Transparent,
        )
    }
    drawCircle(
        brush = Brush.radialGradient(colorStops = stops, center = center, radius = outer),
        radius = outer,
        center = center,
        blendMode = BlendMode.Screen,
    )
}
