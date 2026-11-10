package com.example.movie.presentation.common

import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

private val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val Scrim = Color(0xFF3A3A3E).copy(alpha = 0.86f)
private val SheetMotion = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)

@Composable
internal fun HandleSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dismissPx = with(LocalDensity.current) { 72.dp.toPx() }
    var sheetHeight by remember { mutableIntStateOf(0) }
    var drag by remember { mutableFloatStateOf(0f) }
    var closing by remember { mutableStateOf(false) }
    val travel = remember { Animatable(1f) }
    LaunchedEffect(sheetHeight) {
        if (closing || sheetHeight == 0) return@LaunchedEffect
        travel.animateTo(0f, SheetMotion)
    }
    LaunchedEffect(closing) {
        if (!closing) return@LaunchedEffect
        travel.animateTo(1f, SheetMotion)
        onDismiss()
    }
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
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (1f - travel.value).coerceIn(0f, 1f) }
                    .background(Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (!closing) closing = true },
                    ),
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { sheetHeight = it.height }
                    .graphicsLayer {
                        val distance = if (sheetHeight == 0) 4000f else sheetHeight.toFloat()
                        translationY = travel.value * distance + drag
                    }
                    .fillMaxWidth()
                    .clip(SheetShape)
                    .appBackground()
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
