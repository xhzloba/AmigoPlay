package com.example.movie.presentation.home

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Горизонтальный жест стартует только когда палец явно ушёл вбок.
 * Короткий сдвиг вниз остаётся у вертикального списка: полка его не
 * перехватывает и не откатывает пружиной.
 */
internal fun Modifier.sideScroll(
    state: ScrollableState,
    fling: FlingBehavior,
): Modifier = pointerInput(state, fling) {
    val slop = viewConfiguration.touchSlop
    coroutineScope {
        val scope = this
        var session: Job? = null
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            val deltas = Channel<Float>(Channel.UNLIMITED)
            var totalX = 0f
            var totalY = 0f
            var dragging = false
            val pointerId = down.id
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (!change.pressed) break
                    if (!dragging && change.isConsumed) break
                    val dx = change.position.x - change.previousPosition.x
                    val dy = change.position.y - change.previousPosition.y
                    totalX += dx
                    totalY += dy
                    tracker.addPosition(change.uptimeMillis, change.position)
                    if (!dragging) {
                        awaitPointerEvent(PointerEventPass.Final)
                        if (change.isConsumed) break
                        val sideways = abs(totalX) > slop && abs(totalX) > abs(totalY) * 1.4f
                        if (!sideways) {
                            if (abs(totalY) > slop && abs(totalY) >= abs(totalX)) break
                            continue
                        }
                        session?.cancel()
                        session = scope.launch {
                            state.scroll(MutatePriority.UserInput) {
                                for (delta in deltas) scrollBy(delta)
                            }
                        }
                        dragging = true
                        deltas.trySend(-(totalX - sign(totalX) * slop))
                    } else {
                        deltas.trySend(-dx)
                    }
                    change.consume()
                }
            } finally {
                deltas.close()
            }
            if (dragging) {
                val speed = -tracker.calculateVelocity().x
                val drag = session
                session = scope.launch {
                    drag?.join()
                    state.scroll {
                        with(fling) { performFling(speed) }
                    }
                }
            }
        }
    }
}
