package com.example.movie.presentation.palette

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.absoluteValue

internal data class DominantColors(
    val primary: Color,
    val secondary: Color,
)

internal fun android.graphics.Bitmap.dominantColors(
    transform: (Color) -> Color,
): DominantColors? {
    if (width == 0 || height == 0) return null
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    val buckets = HashMap<Int, IntArray>(256)
    val hsv = FloatArray(3)
    for (pixel in pixels) {
        android.graphics.Color.colorToHSV(pixel, hsv)
        if (hsv[2] < 0.06f || hsv[2] > 0.97f) continue
        val key = ((android.graphics.Color.red(pixel) shr 3) shl 10) or
            ((android.graphics.Color.green(pixel) shr 3) shl 5) or
            (android.graphics.Color.blue(pixel) shr 3)
        val bucket = buckets.getOrPut(key) { IntArray(4) }
        bucket[0] += 1
        bucket[1] += android.graphics.Color.red(pixel)
        bucket[2] += android.graphics.Color.green(pixel)
        bucket[3] += android.graphics.Color.blue(pixel)
    }
    if (buckets.isEmpty()) return null
    val clusters = ArrayList<IntArray>(16)
    for (bucket in buckets.values.sortedByDescending { it[0] }) {
        val match = clusters.firstOrNull { sameColorFamily(it, bucket) }
        if (match == null) {
            clusters.add(bucket.copyOf())
        } else {
            match[0] += bucket[0]
            match[1] += bucket[1]
            match[2] += bucket[2]
            match[3] += bucket[3]
        }
    }
    clusters.sortByDescending { it[0] }
    val minCount = pixels.size * 0.012f
    val ranked = clusters.filter { it[0] >= minCount }
    val primary = ranked.firstOrNull() ?: clusters.first()
    val primaryHsv = primary.toHsv()
    val secondary = ranked.firstOrNull { cluster ->
        if (cluster[0] < primary[0] * 0.10f) return@firstOrNull false
        val clusterHsv = cluster.toHsv()
        if (clusterHsv[1] < 0.12f) return@firstOrNull false
        if (primaryHsv[1] >= 0.18f) {
            hueDistance(clusterHsv[0] / 360f, primaryHsv[0] / 360f) >= 0.07f
        } else {
            true
        }
    } ?: primary
    return DominantColors(
        primary = transform(primary.toColor()),
        secondary = transform(secondary.toColor()),
    )
}

private fun sameColorFamily(a: IntArray, b: IntArray): Boolean {
    val hsvA = a.toHsv()
    val hsvB = b.toHsv()
    val chromaticA = hsvA[1] >= 0.12f
    val chromaticB = hsvB[1] >= 0.12f
    if (chromaticA && chromaticB) {
        return hueDistance(hsvA[0] / 360f, hsvB[0] / 360f) < 0.07f
    }
    if (!chromaticA && !chromaticB) {
        return rgbDistance(a, b) < 55 * 55
    }
    return false
}

private fun IntArray.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(r(), g(), b(), hsv)
    return hsv
}

private fun IntArray.toColor(): Color = Color(r(), g(), b())

private fun IntArray.r(): Int = (this[1] / this[0]).coerceIn(0, 255)

private fun IntArray.g(): Int = (this[2] / this[0]).coerceIn(0, 255)

private fun IntArray.b(): Int = (this[3] / this[0]).coerceIn(0, 255)

private fun rgbDistance(a: IntArray, b: IntArray): Int {
    val dr = a.r() - b.r()
    val dg = a.g() - b.g()
    val db = a.b() - b.b()
    return dr * dr + dg * dg + db * db
}

private fun hueDistance(a: Float, b: Float): Float {
    val delta = (a - b).absoluteValue
    return minOf(delta, 1f - delta)
}

internal fun Color.forGlow(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[2] = hsv[2].coerceIn(0.42f, 0.78f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

internal fun Color.forWordmark(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    if (hsv[1] < 0.14f) {
        hsv[1] = 0f
        hsv[2] = 0.86f
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
    hsv[1] = hsv[1].coerceAtMost(0.8f)
    hsv[2] = (hsv[2] + 0.16f).coerceIn(0.7f, 0.9f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

internal fun DominantColors.seat(): DominantColors = DominantColors(primary.seated(), secondary.seated())

internal fun DominantColors.backdropBrush(): Brush {
    return Brush.horizontalGradient(
        0f to primary,
        1f to secondary,
    )
}

private fun Color.seated(): Color {
    val hsv = toHsv()
    if (hsv[2] < 0.08f) return this
    val sat = hsv[1]
    val value = hsv[2]
    if (sat >= 0.04f && sat < 0.18f && value > 0.68f) {
        hsv[1] = (sat + 0.1f).coerceAtMost(0.28f)
    }
    hsv[2] = (value * 0.66f).coerceIn(0.36f, 0.58f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv
}
