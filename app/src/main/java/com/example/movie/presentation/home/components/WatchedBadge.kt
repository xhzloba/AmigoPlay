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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
internal fun rememberFavoriteIds(): Set<String> {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val ids by LocalAppContainer.current.favorites.ids.collectAsStateWithLifecycle()
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

@Composable
internal fun FavoriteBadge(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(22.dp)
            .offset(x = (-4).dp, y = 6.dp),
    ) {
        drawCircle(color = Color.White, radius = size.minDimension / 2f)
        drawPath(favoriteMark(), Color.Black)
    }
}

private fun DrawScope.favoriteMark(): Path {
    val inset = 5.4.dp.toPx()
    val left = inset
    val top = inset
    val width = size.width - inset * 2
    val height = size.height - inset * 2
    fun x(value: Float) = left + (value - 6f) / 35f * width
    fun y(value: Float) = top + (value - 3f) / 42f * height
    return Path().apply {
        moveTo(x(38f), y(3f))
        lineTo(x(9f), y(3f))
        cubicTo(x(8.20435f), y(3f), x(7.44129f), y(3.31607f), x(6.87868f), y(3.87868f))
        cubicTo(x(6.31607f), y(4.44129f), x(6f), y(5.20435f), x(6f), y(6f))
        lineTo(x(6f), y(45f))
        lineTo(x(23.5f), y(36.5f))
        lineTo(x(41f), y(45f))
        lineTo(x(41f), y(6f))
        cubicTo(x(41f), y(5.20435f), x(40.6839f), y(4.44129f), x(40.1213f), y(3.87868f))
        cubicTo(x(39.5587f), y(3.31607f), x(38.7957f), y(3f), x(38f), y(3f))
        close()
    }
}
