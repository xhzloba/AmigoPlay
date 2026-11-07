package com.example.movie.presentation.common

import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.movie.ui.theme.CinemaBlack
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val Scrim = Color(0xFF3A3A3E).copy(alpha = 0.86f)
private val SheetTween = tween<Float>(220, easing = FastOutSlowInEasing)
private val SheetSlide = tween<IntOffset>(260, easing = FastOutSlowInEasing)

@Composable
internal fun HandleSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dismissPx = with(LocalDensity.current) { 72.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }
    var present by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { present = true }
    LaunchedEffect(closing) {
        if (!closing) return@LaunchedEffect
        delay(270)
        onDismiss()
    }
    val visible = present && !closing
    Dialog(
        onDismissRequest = { if (!closing) closing = true },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setDimAmount(0f)
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                setWindowAnimations(0)
            }
        }
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(SheetTween),
                exit = fadeOut(SheetTween),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Scrim)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { if (!closing) closing = true },
                        ),
                )
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(SheetSlide) { it },
                    exit = slideOutVertically(SheetSlide) { it },
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, drag.roundToInt()) }
                            .clip(SheetShape)
                            .background(CinemaBlack)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            )
                            .windowInsetsPadding(WindowInsets.navigationBars),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .pointerInput(dismissPx) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, amount ->
                                            change.consume()
                                            drag = (drag + amount).coerceAtLeast(0f)
                                        },
                                        onDragEnd = {
                                            if (drag > dismissPx) closing = true else drag = 0f
                                        },
                                        onDragCancel = { if (!closing) drag = 0f },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .width(32.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.28f)),
                            )
                        }
                        content()
                    }
                }
            }
        }
    }
}
