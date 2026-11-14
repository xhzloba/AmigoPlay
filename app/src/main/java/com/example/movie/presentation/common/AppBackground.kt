package com.example.movie.presentation.common

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaBlack

internal const val THEME_CINEMA = "cinema"
internal const val THEME_GRAPHITE = "graphite"
internal const val THEME_BLACK = "black"

private val GraphiteTopLeft = Color(0xFF1A1E26)
private val GraphiteTopRight = Color(0xFF202228)
private val GraphiteBottomRight = Color(0xFF14161C)
private val GraphiteBottomLeft = Color(0xFF101318)

private val Bayer8 = intArrayOf(
    0, 32, 8, 40, 2, 34, 10, 42,
    48, 16, 56, 24, 50, 18, 58, 26,
    12, 44, 4, 36, 14, 46, 6, 38,
    60, 28, 52, 20, 62, 30, 54, 22,
    3, 35, 11, 43, 1, 33, 9, 41,
    51, 19, 59, 27, 49, 17, 57, 25,
    15, 47, 7, 39, 13, 45, 5, 37,
    63, 31, 55, 23, 61, 29, 53, 21,
)

private var graphiteImage: ImageBitmap? = null
private var graphiteW = 0
private var graphiteH = 0

internal fun DrawScope.drawGraphite() {
    val w = size.width.toInt().coerceAtLeast(1)
    val h = size.height.toInt().coerceAtLeast(1)
    val cached = graphiteImage
    val image = if (cached != null && w <= graphiteW && h <= graphiteH) {
        cached
    } else {
        val rw = maxOf(w, graphiteW)
        val rh = maxOf(h, graphiteH)
        renderGraphite(rw, rh).asImageBitmap().also {
            graphiteImage = it
            graphiteW = rw
            graphiteH = rh
        }
    }
    drawImage(
        image = image,
        dstSize = IntSize(w, h),
        filterQuality = FilterQuality.Low,
    )
}

private fun renderGraphite(w: Int, h: Int): Bitmap {
    val tl = GraphiteTopLeft.toArgb()
    val tr = GraphiteTopRight.toArgb()
    val br = GraphiteBottomRight.toArgb()
    val bl = GraphiteBottomLeft.toArgb()
    val tlR = ((tl shr 16) and 0xFF).toFloat()
    val tlG = ((tl shr 8) and 0xFF).toFloat()
    val tlB = (tl and 0xFF).toFloat()
    val trR = ((tr shr 16) and 0xFF).toFloat()
    val trG = ((tr shr 8) and 0xFF).toFloat()
    val trB = (tr and 0xFF).toFloat()
    val brR = ((br shr 16) and 0xFF).toFloat()
    val brG = ((br shr 8) and 0xFF).toFloat()
    val brB = (br and 0xFF).toFloat()
    val blR = ((bl shr 16) and 0xFF).toFloat()
    val blG = ((bl shr 8) and 0xFF).toFloat()
    val blB = (bl and 0xFF).toFloat()
    val pixels = IntArray(w * h)
    val wm = (w - 1).coerceAtLeast(1).toFloat()
    val hm = (h - 1).coerceAtLeast(1).toFloat()
    var i = 0
    for (y in 0 until h) {
        val v = y / hm
        val lr = tlR + (blR - tlR) * v
        val lg = tlG + (blG - tlG) * v
        val lb = tlB + (blB - tlB) * v
        val rr = trR + (brR - trR) * v
        val rg = trG + (brG - trG) * v
        val rb = trB + (brB - trB) * v
        val row = (y and 7) shl 3
        for (x in 0 until w) {
            val u = x / wm
            val d = (Bayer8[row + (x and 7)] - 32) * (2f / 64f)
            val r = (lr + (rr - lr) * u + d).toInt().coerceIn(0, 255)
            val g = (lg + (rg - lg) * u + d).toInt().coerceIn(0, 255)
            val b = (lb + (rb - lb) * u + d).toInt().coerceIn(0, 255)
            pixels[i++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}

@Composable
internal fun Modifier.appBackground(): Modifier {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    return when (settings.appTheme) {
        THEME_GRAPHITE -> drawBehind { drawGraphite() }
        THEME_BLACK -> background(Color.Black)
        else -> background(CinemaBlack)
    }
}

@Composable
internal fun appChrome(): Color {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    return when (settings.appTheme) {
        THEME_GRAPHITE -> Color(0xFF12141A)
        THEME_BLACK -> Color.Black
        else -> CinemaBlack
    }
}

/** Как у cinema-scrim: строка бренда + хвост. */
internal val TopScrimBrand = 40.dp
internal val TopScrimTail = 96.dp

/** Цвета затемнения шапки (AmigoPlay / back / меню) при скролле. */
internal data class TopScrimPalette(
    val top: Color,
    val mid: Color,
    val low: Color,
    val poolCore: Color,
    val poolMid: Color,
    val blur: Dp = 0.dp,
) {
    /** Та же кривая, что у «Как сейчас» — только base-цвет другой. */
    fun verticalBrush(): Brush = Brush.verticalGradient(
        0f to top,
        0.44f to mid,
        0.64f to low,
        0.86f to Color.Transparent,
        1f to Color.Transparent,
    )
}

/** Те же альфы cinema-scrim, любой base (UltraBlur / тема). */
internal fun topScrimPalette(base: Color): TopScrimPalette = TopScrimPalette(
    top = base.copy(alpha = 0.84f),
    mid = base.copy(alpha = 0.74f),
    low = base.copy(alpha = 0.42f),
    poolCore = base.copy(alpha = 0.58f),
    poolMid = base.copy(alpha = 0.26f),
)

@Composable
internal fun topScrimPalette(): TopScrimPalette {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    // Те же альфы, что cinema; graphite = тот же scrim, цвет GraphiteTopLeft.
    val base = if (settings.appTheme == THEME_GRAPHITE) GraphiteTopLeft else Color.Black
    return topScrimPalette(base)
}

@Composable
internal fun Modifier.topScrimFade(progress: Float): Modifier = topScrimFade { progress }

@Composable
internal fun Modifier.topScrimFade(progress: () -> Float): Modifier {
    val blurPx = with(LocalDensity.current) { topScrimPalette().blur.toPx() }
    return graphicsLayer {
        alpha = progress()
        renderEffect = if (blurPx > 0f) {
            BlurEffect(blurPx, blurPx, TileMode.Clamp)
        } else {
            null
        }
    }
}
