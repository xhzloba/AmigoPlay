package com.example.movie.presentation.shell

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object ShellIcons {
    val home: ImageVector = filled(
        "home",
        "M23.9864 4.00009C24.3242 4.00009 24.6522 4.11294 24.9185 4.32071L45 20V39.636C44.9985 40.4312 44.5623 41.4377 44 42C43.4377 42.5623 42.4311 42.9985 41.6359 43H27V28H21V43H6.5C5.70485 42.9984 4.56226 42.682 4 42.1197C3.43774 41.5575 3.00163 40.7952 3 40V21L23.0544 4.32071C23.3207 4.11294 23.6487 4.00009 23.9864 4.00009ZM30 28V40H42V21.4314L24 7.40726L6 22V40L18 40V28C18.0008 27.2046 18.3171 26.442 18.8796 25.8796C19.442 25.3171 20.2046 25.0008 21 25H27C27.7954 25.0009 28.5579 25.3173 29.1203 25.8797C29.6827 26.4421 29.9991 27.2046 30 28Z",
    )
    val catalog: ImageVector = filled(
        "catalog",
        "M31.0012 13.7598C31.0546 13.3494 30.8569 12.9479 30.4999 12.7417C30.1428 12.5355 29.6963 12.5652 29.3675 12.8166L19.0718 20.6938C18.9639 20.7763 18.8699 20.8853 18.802 21.0031C18.734 21.1207 18.6901 21.2507 18.6725 21.3854L16.9985 34.2402C16.9452 34.6508 17.1428 35.0522 17.4999 35.2584C17.8569 35.4645 18.3035 35.435 18.6323 35.1835L28.928 27.3064C29.0358 27.2238 29.1298 27.1148 29.1977 26.9971C29.2656 26.8794 29.3097 26.7494 29.3273 26.6148L31.0012 13.7598ZM26.1649 25.25C25.4746 26.4458 23.9456 26.8554 22.7499 26.1651C21.5541 25.4747 21.1444 23.9458 21.8348 22.75C22.5252 21.5543 24.0541 21.1446 25.2499 21.835C26.4456 22.5253 26.8553 24.0543 26.1649 25.25Z",
        "M45 24C45 35.598 35.598 45 24 45C12.402 45 3 35.598 3 24C3 12.402 12.402 3 24 3C35.598 3 45 12.402 45 24ZM42 24C42 33.9411 33.9411 42 24 42C14.0589 42 6 33.9411 6 24C6 14.0589 14.0589 6 24 6C33.9411 6 42 14.0589 42 24Z",
    )
    val settings: ImageVector = sliders()
    val media: ImageVector = icon(filled = false) { media() }
    val search: ImageVector = icon(filled = false) { magnifier() }
    val bell: ImageVector = icon(filled = false) { bell() }
    val genres: ImageVector = genreTag()
    val mic: ImageVector = icon(filled = false) { mic() }
    val close: ImageVector = icon(filled = false) { cross() }
    /** Material History — часы со стрелкой назад. */
    val history: ImageVector = ImageVector.Builder(
        name = "history",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M13,3c-4.97,0 -9,4.03 -9,9H1l3.89,3.89 0.07,0.14L9,12H6c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08V8H12z",
            ).toNodes(),
            fill = SolidColor(Color.Black),
        )
    }.build()
    val brush: ImageVector = filled24(
        "brush",
        "M7,14c-1.66,0 -3,1.34 -3,3 0,1.31 -1.16,2 -2,2 0.92,1.22 2.49,2 4,2 2.21,0 4,-1.79 4,-4 0,-1.66 -1.34,-3 -3,-3zM20.71,4.63l-1.34,-1.34c-0.39,-0.39 -1.02,-0.39 -1.41,0L9,12.25 11.75,15l8.96,-8.96c0.39,-0.39 0.39,-1.02 0,-1.41z",
    )
    /** Три обложки на полке. */
    val shelf: ImageVector = filled24(
        "shelf",
        "M3,3h5v11H3zM9.5,5h5v9h-5zM16,3h5v11h-5zM2,16h20v2H2z",
    )
    val chevron: ImageVector = chevron(forward = true)
    val back: ImageVector = chevron(forward = false)
    val person: ImageVector = person()
    val gear: ImageVector = gearIcon()
    val trailers: ImageVector = filled(
        "trailers",
        "M42 24C42 31.2328 38.3435 37.6115 32.7782 41.3886C33.1935 41.2738 33.602 41.1447 34 41C45.1693 36.9384 47 32 47 32L48 35C48 35 44.3832 40.459 34.5 43.5C28 45.5 21 45 21 45C9.40202 45 0 35.598 0 24C0 12.402 9.40202 3 21 3C32.598 3 42 12.402 42 24ZM21 19C24.3137 19 27 16.3137 27 13C27 9.68629 24.3137 7 21 7C17.6863 7 15 9.68629 15 13C15 16.3137 17.6863 19 21 19ZM10 30C13.3137 30 16 27.3137 16 24C16 20.6863 13.3137 18 10 18C6.68629 18 4 20.6863 4 24C4 27.3137 6.68629 30 10 30ZM38 24C38 27.3137 35.3137 30 32 30C28.6863 30 26 27.3137 26 24C26 20.6863 28.6863 18 32 18C35.3137 18 38 20.6863 38 24ZM21 26C22.1046 26 23 25.1046 23 24C23 22.8954 22.1046 22 21 22C19.8954 22 19 22.8954 19 24C19 25.1046 19.8954 26 21 26ZM27 35C27 38.3137 24.3137 41 21 41C17.6863 41 15 38.3137 15 35C15 31.6863 17.6863 29 21 29C24.3137 29 27 31.6863 27 35Z",
    )
}

private fun filled24(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { data ->
            addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }
    }.build()

private fun filled(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 48f,
        viewportHeight = 48f,
    ).apply {
        paths.forEach { data ->
            addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            )
        }
    }.build()

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

private fun gearIcon(): ImageVector = ImageVector.Builder(
    name = "gear",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.EvenOdd,
    ) {
        moveTo(11.01f, 5.78f)
        lineTo(10.51f, 2.62f)
        lineTo(13.49f, 2.62f)
        lineTo(12.99f, 5.78f)
        lineTo(15.7f, 6.9f)
        lineTo(17.58f, 4.31f)
        lineTo(19.69f, 6.42f)
        lineTo(17.1f, 8.3f)
        lineTo(18.22f, 11.01f)
        lineTo(21.38f, 10.51f)
        lineTo(21.38f, 13.49f)
        lineTo(18.22f, 12.99f)
        lineTo(17.1f, 15.7f)
        lineTo(19.69f, 17.58f)
        lineTo(17.58f, 19.69f)
        lineTo(15.7f, 17.1f)
        lineTo(12.99f, 18.22f)
        lineTo(13.49f, 21.38f)
        lineTo(10.51f, 21.38f)
        lineTo(11.01f, 18.22f)
        lineTo(8.3f, 17.1f)
        lineTo(6.42f, 19.69f)
        lineTo(4.31f, 17.58f)
        lineTo(6.9f, 15.7f)
        lineTo(5.78f, 12.99f)
        lineTo(2.62f, 13.49f)
        lineTo(2.62f, 10.51f)
        lineTo(5.78f, 11.01f)
        lineTo(6.9f, 8.3f)
        lineTo(4.31f, 6.42f)
        lineTo(6.42f, 4.31f)
        lineTo(8.3f, 6.9f)
        close()
        moveTo(15.05f, 12f)
        arcTo(3.05f, 3.05f, 0f, true, true, 8.95f, 12f)
        arcTo(3.05f, 3.05f, 0f, true, true, 15.05f, 12f)
        close()
    }
}.build()

private fun person(): ImageVector = ImageVector.Builder(
    name = "person",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.6f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(15.2f, 8.2f)
        arcTo(3.2f, 3.2f, 0f, true, true, 8.8f, 8.2f)
        arcTo(3.2f, 3.2f, 0f, true, true, 15.2f, 8.2f)
        close()
        moveTo(5.7f, 19.6f)
        curveTo(6.5f, 15.1f, 8.8f, 13.4f, 12f, 13.4f)
        curveTo(15.2f, 13.4f, 17.5f, 15.1f, 18.3f, 19.6f)
    }
}.build()

private fun PathBuilder.bell() {
    moveTo(4.2f, 16.4f)
    curveTo(6.4f, 16.4f, 6.9f, 11.5f, 7.5f, 9.4f)
    curveTo(8.3f, 6.1f, 9.9f, 4.2f, 12f, 4.2f)
    curveTo(14.1f, 4.2f, 15.7f, 6.1f, 16.5f, 9.4f)
    curveTo(17.1f, 11.5f, 17.6f, 16.4f, 20.4f, 16.4f)
    moveTo(10.2f, 16.4f)
    curveTo(10.55f, 22.05f, 13.45f, 22.05f, 13.8f, 16.4f)
}

private fun PathBuilder.magnifier() {
    moveTo(16.6f, 10.4f)
    arcTo(6.2f, 6.2f, 0f, true, true, 4.2f, 10.4f)
    arcTo(6.2f, 6.2f, 0f, true, true, 16.6f, 10.4f)
    close()
    moveTo(15.2f, 15.4f)
    lineTo(20.4f, 20.6f)
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
