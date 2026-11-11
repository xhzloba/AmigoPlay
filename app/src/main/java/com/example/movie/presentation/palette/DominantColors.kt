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

internal fun Color.forCapsule(): Color {
    val hsv = toHsv()
    if (hsv[1] in 0.08f..0.38f) hsv[1] = (hsv[1] + 0.1f).coerceAtMost(0.4f)
    hsv[2] = hsv[2].coerceIn(0.52f, 0.64f)
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
    val topLeft: Color,
    val topRight: Color,
    val bottomRight: Color,
    val bottomLeft: Color,
)

internal fun android.graphics.Bitmap.posterWash(): PosterWash? {
    if (width < 2 || height < 2) return null
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    val background = backgroundHue(pixels)
    val midX = width / 2
    val midY = height / 2
    val colors = refineWash(
        listOf(
            washCorner(pixels, 0, 0, midX, midY, background),
            washCorner(pixels, midX, 0, width, midY, background),
            washCorner(pixels, midX, midY, width, height, background),
            washCorner(pixels, 0, midY, midX, height, background),
        ),
    )
    return PosterWash(
        topLeft = colors[0],
        topRight = colors[1],
        bottomRight = colors[2],
        bottomLeft = colors[3],
    )
}

private fun refineWash(samples: List<WashSample>): List<Color> {
    val parent = IntArray(samples.size) { it }
    fun find(index: Int): Int {
        var cursor = index
        while (parent[cursor] != cursor) cursor = parent[cursor]
        return cursor
    }
    for (i in samples.indices) {
        for (j in i + 1 until samples.size) {
            val left = samples[i]
            val right = samples[j]
            if (left.saturation < 0.2f || right.saturation < 0.2f) continue
            if (hueDistance(left.hue / 360f, right.hue / 360f) > SimilarHue) continue
            parent[find(i)] = find(j)
        }
    }
    val hue = FloatArray(samples.size)
    samples.indices.groupBy { find(it) }.values.forEach { group ->
        val shared = if (group.size == 1) {
            samples[group[0]].hue
        } else {
            var sinSum = 0.0
            var cosSum = 0.0
            for (index in group) {
                val sample = samples[index]
                val weight = sample.count.coerceAtLeast(1).toDouble()
                val radians = Math.toRadians(sample.hue.toDouble())
                sinSum += kotlin.math.sin(radians) * weight
                cosSum += kotlin.math.cos(radians) * weight
            }
            ((Math.toDegrees(kotlin.math.atan2(sinSum, cosSum)) + 360.0) % 360.0).toFloat()
        }
        for (index in group) hue[index] = shared
    }
    return samples.mapIndexed { index, sample ->
        val hsv = floatArrayOf(
            hue[index],
            sample.saturation.coerceIn(0f, 1f),
            sample.value.coerceIn(0f, 1f),
        )
        Color(android.graphics.Color.HSVToColor(hsv)).toWashTone()
    }
}

private fun android.graphics.Bitmap.backgroundHue(pixels: IntArray): Int? {
    val margin = (minOf(width, height) * 0.05f).toInt().coerceAtLeast(2)
    val buckets = HashMap<Int, Int>()
    var total = 0
    for (y in 0 until height) {
        val edgeRow = y < margin || y >= height - margin
        val row = y * width
        for (x in 0 until width) {
            if (!edgeRow && x >= margin && x < width - margin) continue
            val pixel = pixels[row + x]
            val key = (android.graphics.Color.red(pixel) shr 4 shl 8) or
                (android.graphics.Color.green(pixel) shr 4 shl 4) or
                (android.graphics.Color.blue(pixel) shr 4)
            buckets[key] = (buckets[key] ?: 0) + 1
            total += 1
        }
    }
    if (total == 0) return null
    val top = buckets.maxBy { it.value }
    if (top.value < total * 0.6f) return null
    val red = ((top.key shr 8) and 15) * 16 + 8
    val green = ((top.key shr 4) and 15) * 16 + 8
    val blue = (top.key and 15) * 16 + 8
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    if (max == 0 || (max - min).toFloat() / max < 0.45f) return null
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(red, green, blue, hsv)
    return (hsv[0] / HueStep).toInt() % HueBins
}

private fun android.graphics.Bitmap.washCorner(
    pixels: IntArray,
    x0: Int,
    y0: Int,
    x1: Int,
    y1: Int,
    background: Int?,
): WashSample {
    val bins = Array(HueBins) { HueBin() }
    val hsv = FloatArray(3)
    for (y in y0 until y1) {
        val row = y * width
        for (x in x0 until x1) {
            val pixel = pixels[row + x]
            android.graphics.Color.colorToHSV(pixel, hsv)
            if (hsv[1] < 0.22f || hsv[2] < 0.08f) continue
            val bin = (hsv[0] / HueStep).toInt() % HueBins
            if (background != null && hueBinDistance(bin, background) <= 1) continue
            val red = android.graphics.Color.red(pixel)
            val green = android.graphics.Color.green(pixel)
            val blue = android.graphics.Color.blue(pixel)
            val chroma = (maxOf(red, green, blue) - minOf(red, green, blue)).toDouble()
            bins[bin].add(hsv[0], hsv[1], hsv[2], chroma)
        }
    }
    val chosen = bins.filter { it.count > 8 }.maxByOrNull { it.mass }
    if (chosen == null) {
        val fallback = meanColor(pixels, x0, y0, x1, y1).toHsv()
        return WashSample(fallback[0], fallback[1], fallback[2], 1)
    }
    return WashSample(chosen.hue(), chosen.medianSat(), chosen.medianValue(), chosen.count)
}

private fun hueBinDistance(a: Int, b: Int): Int {
    val delta = kotlin.math.abs(a - b)
    return minOf(delta, HueBins - delta)
}

private data class WashSample(
    val hue: Float,
    val saturation: Float,
    val value: Float,
    val count: Int,
)

private class HueBin {
    var mass = 0.0
    var count = 0
    var sinSum = 0.0
    var cosSum = 0.0
    val satHist = IntArray(ShadeBuckets)
    val valueHist = IntArray(ShadeBuckets)

    fun add(hue: Float, saturation: Float, value: Float, chroma: Double) {
        mass += chroma
        count += 1
        val radians = Math.toRadians(hue.toDouble())
        sinSum += kotlin.math.sin(radians)
        cosSum += kotlin.math.cos(radians)
        satHist[shadeBucket(saturation)] += 1
        valueHist[shadeBucket(value)] += 1
    }

    fun hue(): Float {
        val degrees = Math.toDegrees(kotlin.math.atan2(sinSum, cosSum))
        return ((degrees % 360.0 + 360.0) % 360.0).toFloat()
    }

    fun medianSat(): Float = median(satHist)

    fun medianValue(): Float = median(valueHist)

    private fun median(hist: IntArray): Float {
        val mid = count / 2
        var seen = 0
        for (index in hist.indices) {
            seen += hist[index]
            if (seen > mid) return (index + 0.5f) / hist.size
        }
        return 0f
    }
}

private fun shadeBucket(channel: Float): Int =
    (channel * ShadeBuckets).toInt().coerceIn(0, ShadeBuckets - 1)

private const val HueBins = 18
private const val HueStep = 20f
private const val ShadeBuckets = 24
private const val SimilarHue = 0.042f

private fun Color.toWashTone(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    if (hsv[1] < 0.08f) {
        hsv[2] = hsv[2].coerceIn(0.05f, 0.2f)
    } else {
        hsv[2] = hsv[2].coerceAtMost(0.78f)
    }
    return Color(android.graphics.Color.HSVToColor(hsv))
}

internal fun android.graphics.Bitmap.plexWash(): PosterWash? {
    if (width < 2 || height < 2) return null
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    val corners = PlexUltraBlur.colors(pixels, width, height) ?: return null
    return PosterWash(
        topLeft = Color(0xFF000000.toInt() or corners[0]),
        topRight = Color(0xFF000000.toInt() or corners[1]),
        bottomRight = Color(0xFF000000.toInt() or corners[2]),
        bottomLeft = Color(0xFF000000.toInt() or corners[3]),
    )
}

private fun android.graphics.Bitmap.meanColor(
    pixels: IntArray,
    x0: Int,
    y0: Int,
    x1: Int,
    y1: Int,
): Color {
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0
    for (y in y0 until y1) {
        val row = y * width
        for (x in x0 until x1) {
            val pixel = pixels[row + x]
            red += android.graphics.Color.red(pixel)
            green += android.graphics.Color.green(pixel)
            blue += android.graphics.Color.blue(pixel)
            count += 1
        }
    }
    if (count == 0) return Color(0xFF121212)
    return Color((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
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

internal fun DominantColors.backdropBrush(): Brush {
    return Brush.horizontalGradient(
        0f to primary,
        1f to secondary,
    )
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
