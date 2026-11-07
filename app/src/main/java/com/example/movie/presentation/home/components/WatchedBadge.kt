package com.example.movie.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.PlayBlue

internal val WatchedBadgeHang = 8.dp

@Composable
internal fun rememberWatchedIds(): Set<String> {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val ids by LocalAppContainer.current.watched.ids.collectAsStateWithLifecycle()
    return if (settings.watchedBadge) ids else emptySet()
}

@Composable
internal fun WatchedBadge(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(22.dp)
            .offset(x = (-4).dp, y = 6.dp),
    ) {
        val ring = 2.dp.toPx()
        val outer = size.minDimension / 2f
        drawCircle(color = CinemaBlack, radius = outer)
        drawCircle(color = PlayBlue, radius = outer - ring)
        val mark = Path().apply {
            moveTo(size.width * 8.3f / 24f, size.height * 12.2f / 24f)
            lineTo(size.width * 10.9f / 24f, size.height * 14.8f / 24f)
            lineTo(size.width * 15.8f / 24f, size.height * 9.4f / 24f)
        }
        drawPath(
            mark,
            Color.White,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
