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
    val ranked = rankedFrom(buckets, pixels.size) ?: return null
    val primary = ranked.first()
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

private fun android.graphics.Bitmap.rankedClusters(): List<IntArray>? {
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
    return rankedFrom(buckets, pixels.size)
}

private fun rankedFrom(buckets: Map<Int, IntArray>, pixelCount: Int): List<IntArray>? {
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
    if (clusters.isEmpty()) return null
    clusters.sortByDescending { it[0] }
    val minCount = pixelCount * 0.012f
    val ranked = clusters.filter { it[0] >= minCount }
    return ranked.ifEmpty { listOf(clusters.first()) }
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

internal fun DominantColors.richer(): DominantColors = DominantColors(primary.richer(), secondary.richer())

internal data class PosterWash(
    val base: Color,
    val right: Color,
    val accent: Color,
)

internal fun android.graphics.Bitmap.posterWash(): PosterWash? {
    val ranked = washClusters() ?: return null
    val chromatic = ranked.filter { it.saturation() >= GraySat }
    val dominant = ranked.first()
    val pool = when {
        chromatic.isEmpty() -> ranked
        dominant.saturation() < GraySat -> listOf(dominant) + chromatic
        else -> chromatic
    }
    val picks = ArrayList<HueCluster>(3)
    for (cluster in pool) {
        if (picks.size == 3) break
        if (picks.isNotEmpty() && cluster.count < picks[0].count * 0.05f) continue
        val gray = cluster.saturation() < GraySat
        val distinct = picks.all { other ->
            val otherGray = other.saturation() < GraySat
            when {
                gray && otherGray -> false
                gray || otherGray -> true
                else -> hueDistance(cluster.hue(), other.hue()) >= 0.07f
            }
        }
        if (!distinct) continue
        picks.add(cluster)
    }
    if (picks.isEmpty()) return null
    val base = picks[0].toColor().posterInk()
    val baseValue = base.toHsv()[2]
    val right = if (picks.size >= 2) {
        picks[1].toColor().posterInk().lift(0.16f)
    } else {
        base.withValue(baseValue + 0.14f)
    }
    val accent = if (picks.size >= 3) {
        picks[2].toColor().posterInk()
    } else {
        base.withValue(baseValue - 0.05f)
    }
    return PosterWash(base, right, accent)
}

internal fun Color.differsInHue(other: Color): Boolean {
    val a = toHsv()
    val b = other.toHsv()
    val aGray = a[1] < 0.07f
    val bGray = b[1] < 0.07f
    if (aGray && bGray) return false
    if (aGray || bGray) return true
    return hueDistance(a[0] / 360f, b[0] / 360f) >= 0.07f
}

private class HueCluster(
    var count: Int,
    var red: Int,
    var green: Int,
    var blue: Int,
    val seedHue: Float,
    val seedSat: Float,
)

private fun android.graphics.Bitmap.washClusters(): List<HueCluster>? {
    if (width == 0 || height == 0) return null
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    val buckets = HashMap<Int, IntArray>(256)
    val hsv = FloatArray(3)
    for (pixel in pixels) {
        android.graphics.Color.colorToHSV(pixel, hsv)
        val sat = hsv[1]
        val value = hsv[2]
        if (value > 0.97f) continue
        if (value < 0.035f) continue
        if (value < 0.06f && sat < 0.2f) continue
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
    val clusters = ArrayList<HueCluster>(16)
    for (bucket in buckets.values.sortedByDescending { it[0] }) {
        val bucketHsv = bucket.toHsv()
        val match = clusters.firstOrNull { cluster ->
            val bucketChromatic = bucketHsv[1] >= 0.12f
            val seedChromatic = cluster.seedSat >= 0.12f
            if (bucketChromatic && seedChromatic) {
                hueDistance(bucketHsv[0] / 360f, cluster.seedHue) < 0.08f
            } else if (!bucketChromatic && !seedChromatic) {
                rgbDistance(bucket, cluster.sums()) < 55 * 55
            } else {
                false
            }
        }
        if (match == null) {
            clusters.add(
                HueCluster(
                    count = bucket[0],
                    red = bucket[1],
                    green = bucket[2],
                    blue = bucket[3],
                    seedHue = bucketHsv[0] / 360f,
                    seedSat = bucketHsv[1],
                ),
            )
        } else {
            match.count += bucket[0]
            match.red += bucket[1]
            match.green += bucket[2]
            match.blue += bucket[3]
        }
    }
    if (clusters.isEmpty()) return null
    clusters.sortByDescending { it.count }
    val minCount = pixels.size * 0.012f
    val ranked = clusters.filter { it.count >= minCount }
    return ranked.ifEmpty { listOf(clusters.first()) }
}

private fun HueCluster.sums(): IntArray = intArrayOf(count, red, green, blue)

private fun HueCluster.toColor(): Color = Color(red / count, green / count, blue / count)

private fun HueCluster.hue(): Float = sums().toHsv()[0] / 360f

private fun HueCluster.saturation(): Float = sums().toHsv()[1]

internal fun DominantColors.backdropBrush(): Brush {
    return Brush.horizontalGradient(
        0f to primary,
        1f to secondary,
    )
}

private const val GraySat = 0.28f

private fun Color.posterInk(): Color {
    val hsv = toHsv()
    if (hsv[1] < GraySat) {
        hsv[1] = 0f
        hsv[2] = 0.16f
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
    hsv[1] = (hsv[1] * 1.5f + 0.2f).coerceIn(0.68f, 0.94f)
    val bright = if (hsv[2] < 0.22f) hsv[2] + 0.06f else hsv[2] * 0.95f
    hsv[2] = bright.coerceIn(0.18f, 0.58f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun Color.lift(extra: Float): Color {
    val hsv = toHsv()
    hsv[2] = (hsv[2] + extra).coerceIn(0.22f, 0.68f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun Color.withValue(value: Float): Color {
    val hsv = toHsv()
    hsv[2] = value.coerceIn(0.24f, 0.7f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun Color.richer(): Color {
    val hsv = toHsv()
    if (hsv[1] < 0.05f) return this
    hsv[1] = (hsv[1] * 1.65f).coerceAtMost(1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
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
