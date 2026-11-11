package com.example.movie.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val NoticeShelfFavorites = "Favorites"
internal const val NoticeShelfWatched = "Watched"
internal const val NoticeShelfDownloads = "Downloads"

private const val NoticeDelayMillis = 1_500L
private const val NoticeHoldMillis = 2_000L

internal class ShelfNotices(private val scope: CoroutineScope?) {
    var label by mutableStateOf("")
    var visible by mutableStateOf(false)
    var openRequest by mutableIntStateOf(0)
    var section by mutableStateOf(NoticeShelfFavorites)
    var jump: () -> Unit = {}

    private var job: Job? = null
    private var token = 0

    fun show(message: String, shelf: String) {
        val scope = scope ?: return
        section = shelf
        val ticket = ++token
        job?.cancel()
        visible = false
        job = scope.launch {
            delay(NoticeDelayMillis)
            if (ticket != token) return@launch
            label = message
            visible = true
            delay(NoticeHoldMillis)
            if (ticket == token) visible = false
        }
    }

    fun open() {
        token++
        job?.cancel()
        visible = false
        openRequest++
        jump()
    }
}

internal val LocalShelfNotices = staticCompositionLocalOf { ShelfNotices(null) }

@Composable
internal fun ShelfNoticeHost() {
    val notices = LocalShelfNotices.current
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val aboveBar = (nav + 4.dp).coerceAtLeast(22.dp) + 58.dp
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(100f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = notices.visible,
            enter = fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 2 },
            exit = fadeOut(tween(140)),
        ) {
            Text(
                text = notices.label,
                color = Color.Black,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(bottom = aboveBar)
                    .fillMaxWidth()
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            )
        }
    }
}
