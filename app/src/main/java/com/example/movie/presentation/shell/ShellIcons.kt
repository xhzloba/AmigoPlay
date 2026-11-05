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
    val catalog: ImageVector = icon(filled = false) { grid() }
    val settings: ImageVector = sliders()
    val media: ImageVector = icon(filled = false) { media() }
    val search: ImageVector = icon(filled = false) { magnifier() }
    val genres: ImageVector = genreTag()
    val mic: ImageVector = icon(filled = false) { mic() }
    val close: ImageVector = icon(filled = false) { cross() }
    val history: ImageVector = icon(filled = false) { clock() }
    val chevron: ImageVector = chevron(forward = true)
    val back: ImageVector = chevron(forward = false)
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

private fun PathBuilder.media() {
    val left = 3.4f
    val top = 3.4f
    val right = 20.6f
    val bottom = 20.6f
    val radius = 3.2f
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
    moveTo(10.1f, 8.6f)
    lineTo(15.8f, 12f)
    lineTo(10.1f, 15.4f)
    close()
}

private fun chevron(forward: Boolean): ImageVector = ImageVector.Builder(
    name = if (forward) "chevron" else "back",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        if (forward) {
            moveTo(9f, 5.2f)
            lineTo(15.6f, 12f)
            lineTo(9f, 18.8f)
        } else {
            moveTo(15f, 5.2f)
            lineTo(8.4f, 12f)
            lineTo(15f, 18.8f)
        }
    }
}.build()

private fun PathBuilder.cross() {
    moveTo(6.4f, 6.4f)
    lineTo(17.6f, 17.6f)
    moveTo(17.6f, 6.4f)
    lineTo(6.4f, 17.6f)
}

private fun PathBuilder.clock() {
    moveTo(19.2f, 12f)
    arcTo(7.2f, 7.2f, 0f, true, true, 4.8f, 12f)
    arcTo(7.2f, 7.2f, 0f, true, true, 19.2f, 12f)
    close()
    moveTo(12f, 7.4f)
    lineTo(12f, 12.2f)
    lineTo(15.4f, 14.2f)
}

private fun PathBuilder.mic() {
    moveTo(9f, 6.2f)
    quadTo(9f, 3.2f, 12f, 3.2f)
    quadTo(15f, 3.2f, 15f, 6.2f)
    lineTo(15f, 11.2f)
    quadTo(15f, 14.2f, 12f, 14.2f)
    quadTo(9f, 14.2f, 9f, 11.2f)
    close()
    moveTo(6.4f, 11f)
    arcTo(5.6f, 5.6f, 0f, false, false, 17.6f, 11f)
    moveTo(12f, 16.6f)
    lineTo(12f, 20.6f)
}

private fun PathBuilder.magnifier() {
    moveTo(16.6f, 10.4f)
    arcTo(6.2f, 6.2f, 0f, true, true, 4.2f, 10.4f)
    arcTo(6.2f, 6.2f, 0f, true, true, 16.6f, 10.4f)
    close()
    moveTo(15.2f, 15.4f)
    lineTo(20.4f, 20.6f)
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

private fun genreTag(): ImageVector = ImageVector.Builder(
    name = "genres",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(4.2f, 12.2f)
        lineTo(12.2f, 4.2f)
        lineTo(20.2f, 4.2f)
        lineTo(20.2f, 12.2f)
        lineTo(12.2f, 20.2f)
        close()
    }
    path(fill = SolidColor(Color.Black)) {
        moveTo(16.7f, 8.2f)
        arcTo(1.15f, 1.15f, 0f, true, true, 14.4f, 8.2f)
        arcTo(1.15f, 1.15f, 0f, true, true, 16.7f, 8.2f)
        close()
    }
}.build()

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
