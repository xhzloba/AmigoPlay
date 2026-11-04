package com.example.movie.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.example.movie.presentation.haptic.HoldHaptic
import com.example.movie.presentation.haptic.rememberHoldHaptic
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val HoldMillis = 450L
private const val PressMillis = 110L
private const val PressInMillis = 70
private const val PressOutMillis = 160
private const val BuzzMillis = 280L

internal class PosterWave {
    var origin by mutableStateOf(Offset.Zero)
        private set
    var token by mutableStateOf(0)
        private set
    var released by mutableStateOf(false)
        private set
    var killed by mutableStateOf(false)
        private set
    private var settle = CompletableDeferred<Unit>().apply { complete(Unit) }

    fun press(at: Offset) {
        origin = at
        released = false
        killed = false
        settle = CompletableDeferred()
        token += 1
    }

    fun release() {
        released = true
    }

    fun kill() {
        killed = true
    }

    fun gate(): CompletableDeferred<Unit> = settle

    suspend fun awaitSettled() {
        settle.await()
    }
}

@Composable
internal fun Modifier.posterWave(wave: PosterWave): Modifier {
    val radius = remember { Animatable(0f) }
    val ink = remember { Animatable(0f) }
    LaunchedEffect(wave.token) {
        if (wave.token == 0) return@LaunchedEffect
        val gate = wave.gate()
        try {
            radius.snapTo(0.18f)
            ink.snapTo(1f)
            val grow = launch {
                radius.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
            }
            snapshotFlow { wave.killed to wave.released }.first { it.first || it.second }
            if (wave.killed) {
                grow.cancel()
                radius.snapTo(0f)
                ink.snapTo(0f)
            } else {
                grow.join()
                ink.animateTo(0f, tween(150, easing = FastOutSlowInEasing))
                radius.snapTo(0f)
            }
        } finally {
            if (!gate.isCompleted) gate.complete(Unit)
        }
    }
    return drawWithContent {
        drawContent()
        val spread = radius.value
        val alpha = ink.value
        if (spread <= 0.01f || alpha <= 0.01f) return@drawWithContent
        val origin = wave.origin
        val reach = maxOf(
            hypot(origin.x, origin.y),
            hypot(size.width - origin.x, origin.y),
            hypot(origin.x, size.height - origin.y),
            hypot(size.width - origin.x, size.height - origin.y),
        )
        val r = lerp(size.minDimension * 0.12f, reach, spread)
        drawRect(Color.Black.copy(alpha = 0.08f * alpha))
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Black.copy(alpha = 0.36f * alpha),
                    0.62f to Color.Black.copy(alpha = 0.16f * alpha),
                    1f to Color.Transparent,
                ),
                center = origin,
                radius = r,
            ),
            radius = r,
            center = origin,
        )
    }
}

@Composable
internal fun rememberPressScale(pressed: Boolean): Float {
    return animateFloatAsState(
        targetValue = if (pressed) 0.968f else 1f,
        animationSpec = tween(
            durationMillis = if (pressed) PressInMillis else PressOutMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "press-in",
    ).value
}

@Composable
internal fun Modifier.holdToPeek(
    onPeek: () -> Unit,
    onProgress: (Float) -> Unit,
    onPressed: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
    wave: PosterWave? = null,
): Modifier {
    val peek by rememberUpdatedState(onPeek)
    val press by rememberUpdatedState(onPressed)
    val click by rememberUpdatedState(onClick)
    val waveState by rememberUpdatedState(wave)
    val peeks = remember { arrayOf<() -> Unit>({ }) }
    val presser = remember { arrayOf<(Boolean) -> Unit>({ }) }
    val clicks = remember { arrayOf<() -> Unit>({ }) }
    val waves = remember { arrayOfNulls<PosterWave>(1) }
    val motor = remember { arrayOfNulls<HoldHaptic>(1) }
    val window = remember { floatArrayOf(Float.NaN, Float.NaN) }
    val haptic = rememberHoldHaptic()
    peeks[0] = peek
    presser[0] = press
    clicks[0] = click
    waves[0] = waveState
    motor[0] = haptic
    return onGloballyPositioned { coordinates ->
        val at = coordinates.positionInWindow()
        window[0] = at.x
        window[1] = at.y
    }.semantics {
        onClick {
            clicks[0].invoke()
            true
        }
    }.pointerInput(Unit) {
        val slop = viewConfiguration.touchSlop
        var pressEpoch = 0
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val origin = down.position
                val originFinger = fingerOnScreen(window, down)
                val started = System.nanoTime()
                val pressToken = ++pressEpoch
                waves[0]?.press(origin)
                presser[0].invoke(true)
                val buzz = launch {
                    delay(BuzzMillis)
                    motor[0]?.start()
                }
                var draggedAway = false
                var releaseDelay = 0L
                var openDetail = false
                try {
                    val tapped = withTimeoutOrNull(HoldMillis) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            val dragged = change != null &&
                                (fingerOnScreen(window, change) - originFinger).getDistance() > slop
                            if (dragged || change == null) return@withTimeoutOrNull false
                            if (!change.pressed) {
                                change.consume()
                                return@withTimeoutOrNull true
                            }
                        }
                        @Suppress("UNREACHABLE_CODE")
                        false
                    }
                    if (tapped == false) {
                        draggedAway = true
                    } else if (tapped == true) {
                        val elapsed = (System.nanoTime() - started) / 1_000_000
                        if (elapsed < PressMillis) releaseDelay = PressMillis - elapsed
                        openDetail = true
                    } else {
                        buzz.cancel()
                        motor[0]?.stop()
                        peeks[0].invoke()
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                } finally {
                    buzz.cancel()
                    motor[0]?.stop()
                    if (draggedAway) waves[0]?.kill() else waves[0]?.release()
                    val mine = pressToken
                    val hold = releaseDelay
                    val open = openDetail
                    launch {
                        if (hold > 0) delay(hold)
                        if (pressEpoch != mine) return@launch
                        presser[0].invoke(false)
                        if (!open) return@launch
                        coroutineScope {
                            launch { delay(PressOutMillis.toLong()) }
                            launch { waves[0]?.awaitSettled() }
                        }
                        if (pressEpoch == mine) clicks[0].invoke()
                    }
                }
            }
        }
    }
}

private fun fingerOnScreen(window: FloatArray, change: PointerInputChange): Offset {
    val x = window[0]
    val y = window[1]
    val local = change.position
    return if (x.isNaN() || y.isNaN()) local else Offset(x + local.x, y + local.y)
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
                0.5f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.36f * amount),
            ),
            center = center,
            radius = size.maxDimension * 0.72f,
        ),
    )
}

private fun DrawScope.drawFocusRing(center: Offset, amount: Float) {
    val envelope = sin(amount * PI.toFloat())
    if (envelope < 0.02f) return
    val band = lerp(size.minDimension * 0.26f, size.minDimension * 0.12f, amount)
    drawSoftRing(
        center = center,
        radius = lerp(size.minDimension * 0.36f, size.minDimension * 0.18f, amount),
        band = band,
        color = Color.White.copy(alpha = 0.38f * envelope),
    )
    drawSoftRing(
        center = center,
        radius = lerp(size.minDimension * 0.44f, size.minDimension * 0.28f, amount),
        band = band * 1.5f,
        color = Color(0xFFD6E4FF).copy(alpha = 0.15f * envelope),
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
                (y - half) to Color.White.copy(alpha = 0.07f * peak),
                y to Color.White.copy(alpha = 0.24f * peak),
                (y + half) to Color.White.copy(alpha = 0.07f * peak),
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
                0f to Color.White.copy(alpha = 0.30f * gather),
                0.4f to Color(0xFFD6E4FF).copy(alpha = 0.10f * gather),
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
