package com.example.movie.presentation.shell

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object ShellIcons {
    val home: ImageVector = icon(filled = false) { house() }
    val homeFilled: ImageVector = icon(filled = true) { house() }
    val catalog: ImageVector = icon(filled = false) { grid() }
    val catalogFilled: ImageVector = icon(filled = true) { grid() }
    val favorite: ImageVector = icon(filled = false) { heart() }
    val favoriteFilled: ImageVector = icon(filled = true) { heart() }
    val settings: ImageVector = sliders()
}

private fun icon(filled: Boolean, body: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = "shell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = if (filled) null else SolidColor(Color.Black),
            strokeLineWidth = if (filled) 0f else 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.EvenOdd,
            pathBuilder = body,
        )
    }.build()

private fun PathBuilder.house() {
    moveTo(12f, 3.4f)
    lineTo(3.4f, 11f)
    lineTo(3.4f, 20.6f)
    lineTo(20.6f, 20.6f)
    lineTo(20.6f, 11f)
    close()
    moveTo(9.3f, 20.6f)
    lineTo(9.3f, 13.8f)
    lineTo(14.7f, 13.8f)
    lineTo(14.7f, 20.6f)
    close()
}

private fun PathBuilder.grid() {
    roundRect(3.2f, 3.2f, 7.4f, 1.6f)
    roundRect(13.4f, 3.2f, 7.4f, 1.6f)
    roundRect(3.2f, 13.4f, 7.4f, 1.6f)
    roundRect(13.4f, 13.4f, 7.4f, 1.6f)
}

private fun PathBuilder.heart() {
    moveTo(12f, 19.8f)
    curveTo(12f, 19.8f, 4.2f, 14.7f, 4.2f, 9.6f)
    curveTo(4.2f, 7f, 6.2f, 5.2f, 8.6f, 5.2f)
    curveTo(10.1f, 5.2f, 11.2f, 6f, 12f, 7.3f)
    curveTo(12.8f, 6f, 13.9f, 5.2f, 15.4f, 5.2f)
    curveTo(17.8f, 5.2f, 19.8f, 7f, 19.8f, 9.6f)
    curveTo(19.8f, 14.7f, 12f, 19.8f, 12f, 19.8f)
    close()
}

private fun PathBuilder.roundRect(left: Float, top: Float, size: Float, radius: Float) {
    val right = left + size
    val bottom = top + size
    moveTo(left + radius, top)
    lineTo(right - radius, top)
    quadTo(right, top, right, top + radius)
    lineTo(right, bottom - radius)
    quadTo(right, bottom, right - radius, bottom)
    lineTo(left + radius, bottom)
    quadTo(left, bottom, left, bottom - radius)
    lineTo(left, top + radius)
    quadTo(left, top, left + radius, top)
    close()
}

private fun sliders(): ImageVector = ImageVector.Builder(
    name = "settings",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    slider(6.2f, 8.2f)
    slider(12f, 15.4f)
    slider(17.8f, 10.6f)
}.build()

private fun ImageVector.Builder.slider(y: Float, knob: Float) {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(3.6f, y)
        lineTo(20.4f, y)
    }
    path(fill = SolidColor(Color.Black)) {
        moveTo(knob + 2.35f, y)
        arcTo(2.35f, 2.35f, 0f, true, true, knob - 2.35f, y)
        arcTo(2.35f, 2.35f, 0f, true, true, knob + 2.35f, y)
        close()
    }
}
