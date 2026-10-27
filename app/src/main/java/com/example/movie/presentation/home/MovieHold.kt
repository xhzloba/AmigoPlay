package com.example.movie.presentation.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.example.movie.presentation.haptic.HoldHaptic
import com.example.movie.presentation.haptic.rememberHoldHaptic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal const val HoldMillis = 800L
private const val BuzzAfterNanos = 120_000_000L

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
                val started = System.nanoTime()
                val token = ++epoch
                var buzzing = false
                val frames = launch {
                    val t0 = System.nanoTime()
                    while (isActive && epoch == token) {
                        val progress = ((System.nanoTime() - t0) / 1_000_000f / HoldMillis).coerceIn(0f, 1f)
                        if (epoch != token) return@launch
                        reporter[0].invoke(progress)
                        if (progress >= 1f) break
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
                            if (!buzzing && System.nanoTime() - started >= BuzzAfterNanos) {
                                buzzing = true
                                motor[0]?.start(HoldMillis)
                            }
                        }
                        @Suppress("UNREACHABLE_CODE")
                        true
                    }
                    if (endedEarly != null) return@awaitEachGesture
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

internal fun Modifier.holdCue(progress: Float, corner: Dp, tint: Color): Modifier = drawWithContent {
    drawContent()
    if (progress > 0.01f) drawHoldCue(progress, corner.toPx(), tint)
}

private fun DrawScope.drawHoldCue(progress: Float, cornerPx: Float, tint: Color) {
    val amount = progress.coerceIn(0f, 1f)
    val inset = size.minDimension * 0.08f
    val radius = (cornerPx - inset).coerceAtLeast(0f)
    val outline = Path().apply {
        addRoundRect(
            RoundRect(
                left = inset,
                top = inset,
                right = size.width - inset,
                bottom = size.height - inset,
                cornerRadius = CornerRadius(radius, radius),
            ),
        )
    }
    val measure = PathMeasure().apply { setPath(outline, false) }
    val traveled = (measure.length * amount).coerceAtLeast(1f).coerceAtMost(measure.length)
    val ink = Color(
        red = (tint.red * 0.45f + 0.55f).coerceIn(0f, 1f),
        green = (tint.green * 0.45f + 0.55f).coerceIn(0f, 1f),
        blue = (tint.blue * 0.45f + 0.55f).coerceIn(0f, 1f),
    )
    drawRect(Color.Black.copy(alpha = 0.08f * amount))
    val head = measure.getPosition(traveled)
    val glow = size.minDimension * 0.62f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = 0.20f),
                0.22f to ink.copy(alpha = 0.22f),
                0.55f to ink.copy(alpha = 0.07f),
                1f to Color.Transparent,
            ),
            center = head,
            radius = glow,
        ),
        radius = glow,
        center = head,
        blendMode = BlendMode.Screen,
    )
    val span = measure.length * 0.12f
    repeat(2) { index ->
        val near = (index + 1f) / 2f
        val distance = (traveled - span * (1f - near)).coerceIn(0f, measure.length)
        val center = measure.getPosition(distance)
        val radiusPx = glow * (0.72f + 0.2f * near)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to ink.copy(alpha = 0.10f * near),
                    1f to Color.Transparent,
                ),
                center = center,
                radius = radiusPx,
            ),
            radius = radiusPx,
            center = center,
            blendMode = BlendMode.Screen,
        )
    }
}
