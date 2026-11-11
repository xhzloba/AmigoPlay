package com.example.movie.presentation.palette

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * UltraBlur colours exactly as Plex Media Server 1.43.5 computes them (`/services/ultrablur/colors`):
 * four quadrants, MMCQ palette of each, Okhsl scoring against the neighbouring corner, lightness cap.
 * Float maths and the odd corners (unstable sort, stale lookahead, byte wrap) repeat the server on purpose,
 * so do not tidy them up: the output is checked bit for bit against the native code.
 */
internal object PlexUltraBlur {

    /** [pixels] are ARGB, row by row. Returns top left, top right, bottom right, bottom left as 0xRRGGBB. */
    fun colors(pixels: IntArray, width: Int, height: Int): IntArray? {
        val halfWidth = width / 2
        val halfHeight = height / 2
        if (halfWidth < 1 || halfHeight < 1 || pixels.size < width * height) return null
        val topLeft = corner(pixels, width, 0, 0, halfWidth, halfHeight, null, TopLeftLightness)
            ?: return null
        val topRight = corner(pixels, width, halfWidth, 0, halfWidth, halfHeight, topLeft, CornerLightness)
            ?: return null
        val bottomLeft = corner(pixels, width, 0, halfHeight, halfWidth, halfHeight, topLeft, CornerLightness)
            ?: return null
        val bottomRight = corner(pixels, width, halfWidth, halfHeight, halfWidth, halfHeight, bottomLeft, CornerLightness)
            ?: return null
        return intArrayOf(topLeft.rgb(), topRight.rgb(), bottomRight.rgb(), bottomLeft.rgb())
    }

    private fun corner(
        pixels: IntArray,
        stride: Int,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        previous: Tone?,
        maxLightness: Double,
    ): Tone? {
        val palette = palette(pixels, stride, left, top, width, height) ?: return null
        val tones = Array(palette.size) { toneOf(palette[it]) }
        val scores = FloatArray(tones.size) { score(tones[it], previous, it, tones.size) }
        val order = MutableList(tones.size) { it }
        serverSort(order) { first, second -> scores[first] > scores[second] }
        val best = tones[order[0]]
        val lightness = if (best.lightness.toDouble() > maxLightness) maxLightness.toFloat() else best.lightness
        val saturation = if (best.saturation > 0.2f && best.saturation < 0.6f) 0.6f else best.saturation
        return Tone(best.hue, saturation, lightness, best.labL, best.labA, best.labB)
    }

    private fun palette(pixels: IntArray, stride: Int, left: Int, top: Int, width: Int, height: Int): IntArray? {
        val histogram = IntArray(1 shl (3 * ColorBits))
        var redMin = Int.MAX_VALUE
        var redMax = Int.MIN_VALUE
        var greenMin = Int.MAX_VALUE
        var greenMax = Int.MIN_VALUE
        var blueMin = Int.MAX_VALUE
        var blueMax = Int.MIN_VALUE
        val total = width * height
        var index = 0
        while (index < total) {
            val pixel = pixels[(top + index / width) * stride + left + index % width]
            index += SampleStep
            if (pixel ushr 24 < MinAlpha) continue
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            if (red > WhiteLevel && green > WhiteLevel && blue > WhiteLevel) continue
            val r = red shr ColorShift
            val g = green shr ColorShift
            val b = blue shr ColorShift
            histogram[(r shl (2 * ColorBits)) or (g shl ColorBits) or b] += 1
            if (r < redMin) redMin = r
            if (r > redMax) redMax = r
            if (g < greenMin) greenMin = g
            if (g > greenMax) greenMax = g
            if (b < blueMin) blueMin = b
            if (b > blueMax) blueMax = b
        }
        if (redMin > redMax) return null

        val byCount = BoxQueue { first, second -> first.count() < second.count() }
        byCount.push(ColorBox(redMin, redMax, greenMin, greenMax, blueMin, blueMax, histogram))
        if (!split(byCount, PopulationTarget)) return null
        val byWeight = BoxQueue { first, second ->
            first.count().toLong() * first.volume() < second.count().toLong() * second.volume()
        }
        while (byCount.size > 0) byWeight.push(byCount.pop())
        if (!split(byWeight, MaxColors - byWeight.size)) return null
        val colors = IntArray(byWeight.size)
        for (slot in colors.indices) colors[slot] = byWeight.pop().average()
        return colors
    }

    private fun split(queue: BoxQueue, target: Int): Boolean {
        var colors = 1
        var rounds = 0
        while (rounds < MaxRounds) {
            val box = queue.pop()
            if (box.count() == 0) {
                queue.push(box)
                rounds++
                continue
            }
            val halves = box.cut() ?: return false
            queue.push(halves.first)
            val second = halves.second
            if (second != null) {
                queue.push(second)
                colors++
            }
            if (colors >= target) return true
            rounds++
        }
        return true
    }

    private fun score(tone: Tone, previous: Tone?, index: Int, count: Int): Float {
        val saturation = if (tone.saturation.isNaN()) 0f else tone.saturation
        val population = when {
            index == 0 -> 100f
            count.toFloat() - 1f == 0f -> 0f
            else -> (100f - index.toFloat() / (count.toFloat() - 1f) * 100f).coerceAtLeast(0f)
        }
        val dark = plateau(tone.lightness, 0f, 0f, 0.4f, 0.7f) * 1.5f
        val vivid = plateau(saturation, 0.2f, 0.6f, 1f, 1f) * 3f
        val contrast = if (previous == null) {
            300f
        } else {
            val lightness = tone.labL - previous.labL
            val a = tone.labA - previous.labA
            val b = tone.labB - previous.labB
            plateau(sqrt(a * a + lightness * lightness + b * b), 0f, 0.05f, 0.1f, 1f) * 3f
        }
        val hue = tone.hue * 360f
        val green = plateau(hue, 70f, 80f, 120f, 130f)
        val skinHue = plateau(hue, 30f, 40f, 70f, 80f)
        val skinSaturation = plateau(saturation, 0.1f, 0.2f, 0.6f, 0.7f)
        val paleSaturation = plateau(saturation, 0f, 0f, 0.2f, 0.4f)
        val paleLightness = plateau(tone.lightness, 0.3f, 0.5f, 1f, 1f)
        val skin = if (skinHue != 0f && skinSaturation != 0f) -(skinHue + skinSaturation) else 0f
        val pale = if (paleSaturation != 0f && paleLightness != 0f) -(paleSaturation + paleLightness) else 0f
        return population + dark + vivid + contrast - (green + green) + skin + pale
    }

    /** 100 between [from] and [to], linear ramps down to 0 at [start] and [end], 0 outside. */
    private fun plateau(value: Float, start: Float, from: Float, to: Float, end: Float): Float {
        if (value >= from && value <= to) return 100f
        val ramp = when {
            value < from -> if (start == from) return 0f else 100f + (from - value) / (start - from) * 100f
            value > to -> if (to == end) return 0f else 100f + (value - to) / (to - end) * 100f
            else -> return 0f
        }
        return if (ramp < 0f) 0f else ramp
    }

    private fun toneOf(rgb: Int): Tone {
        val red = ((rgb shr 16) and 0xFF).toFloat() / 255f
        val green = ((rgb shr 8) and 0xFF).toFloat() / 255f
        val blue = (rgb and 0xFF).toFloat() / 255f
        val lab = linearToOklab(gammaToLinear(red), gammaToLinear(green), gammaToLinear(blue))
        val chroma = sqrt(lab[1] * lab[1] + lab[2] * lab[2])
        val a = lab[1] / chroma
        val b = lab[2] / chroma
        val hue = 0.5f + 0.5f * atan2(-lab[2], -lab[1]) / Pi
        val limits = chromaLimits(lab[0], a, b)
        val base = limits[0]
        val middle = limits[1]
        val peak = limits[2]
        val saturation = if (chroma < middle) {
            val k1 = Mid * base
            val k2 = 1f - k1 / middle
            chroma / (k1 + k2 * chroma) * Mid
        } else {
            val k1 = (1f - Mid) * middle * middle * MidInverse * MidInverse / base
            val k2 = 1f - k1 / (peak - middle)
            Mid + (1f - Mid) * ((chroma - middle) / (k1 + k2 * (chroma - middle)))
        }
        // The server keeps Lab of the gamma-encoded colour for the distance term, not of the linear one.
        val stored = linearToOklab(red, green, blue)
        return Tone(hue, saturation, toe(lab[0]), stored[0], stored[1], stored[2])
    }

    private class Tone(
        val hue: Float,
        val saturation: Float,
        val lightness: Float,
        val labL: Float,
        val labA: Float,
        val labB: Float,
    ) {
        fun rgb(): Int {
            val channels = okhslToSrgb(hue, if (saturation.isNaN()) 0f else saturation, lightness)
            return (byteOf(channels[0]) shl 16) or (byteOf(channels[1]) shl 8) or byteOf(channels[2])
        }

        private fun byteOf(channel: Float): Int {
            val clamped = if (channel > 1f) 1f else if (channel < 0f) 0f else channel
            return (clamped * 255f).toInt()
        }
    }

    private class ColorBox(
        var redMin: Int,
        var redMax: Int,
        var greenMin: Int,
        var greenMax: Int,
        var blueMin: Int,
        var blueMax: Int,
        val histogram: IntArray,
    ) {
        private var counted = -1

        fun volume(): Int = (redMax - redMin + 1) * (greenMax - greenMin + 1) * (blueMax - blueMin + 1)

        fun count(): Int {
            if (counted < 0) {
                var sum = 0
                for (red in redMin..redMax) {
                    for (green in greenMin..greenMax) {
                        val row = (red shl (2 * ColorBits)) or (green shl ColorBits)
                        for (blue in blueMin..blueMax) sum += histogram[row or blue]
                    }
                }
                counted = sum
            }
            return counted
        }

        fun average(): Int {
            var total = 0L
            var redSum = 0L
            var greenSum = 0L
            var blueSum = 0L
            for (red in redMin..redMax) {
                for (green in greenMin..greenMax) {
                    val row = (red shl (2 * ColorBits)) or (green shl ColorBits)
                    for (blue in blueMin..blueMax) {
                        val hits = histogram[row or blue].toLong()
                        total += hits
                        redSum += hits * (2 * red + 1)
                        greenSum += hits * (2 * green + 1)
                        blueSum += hits * (2 * blue + 1)
                    }
                }
            }
            val half = 1 shl (ColorShift - 1)
            // An empty box averages its bounds; the server stores that in a byte, so 256 wraps to 0.
            val red = if (total > 0) (redSum * half / total).toInt() else (half * (redMin + redMax + 1)) and 0xFF
            val green = if (total > 0) (greenSum * half / total).toInt() else (half * (greenMin + greenMax + 1)) and 0xFF
            val blue = if (total > 0) (blueSum * half / total).toInt() else (half * (blueMin + blueMax + 1)) and 0xFF
            return (red shl 16) or (green shl 8) or blue
        }

        fun cut(): Pair<ColorBox, ColorBox?>? {
            if (count() == 1) return copy() to null
            val redSpan = redMax - redMin + 1
            val greenSpan = greenMax - greenMin + 1
            val blueSpan = blueMax - blueMin + 1
            val widest = maxOf(redSpan, greenSpan, blueSpan)
            val axis = if (widest == redSpan) 0 else if (widest == greenSpan) 1 else 2
            val low = when (axis) { 0 -> redMin; 1 -> greenMin; else -> blueMin }
            val high = when (axis) { 0 -> redMax; 1 -> greenMax; else -> blueMax }
            val running = IntArray(Levels)
            var total = 0
            for (level in low..high) {
                total += slice(axis, level)
                running[level] = total
            }
            fun runningAt(level: Int): Int = if (level in low..high) running[level] else 0
            for (level in low..high) {
                if (running[level] <= total / 2) continue
                val below = level - low
                val above = high - level
                var edge = if (below <= above) {
                    minOf(high - 1, (level + above / 2.0).toInt())
                } else {
                    maxOf(low, (level - 1 - below / 2.0).toInt())
                }
                while (edge < high && runningAt(edge) <= 0) edge++
                // The server never refreshes the lookahead here, so the edge slides to the first filled slice.
                if (total - runningAt(edge) == 0) {
                    while (runningAt(edge - 1) > 0) edge--
                }
                val first = copy()
                val second = copy()
                when (axis) {
                    0 -> { first.redMax = edge; second.redMin = edge + 1 }
                    1 -> { first.greenMax = edge; second.greenMin = edge + 1 }
                    else -> { first.blueMax = edge; second.blueMin = edge + 1 }
                }
                return first to second
            }
            return null
        }

        private fun slice(axis: Int, level: Int): Int {
            var sum = 0
            when (axis) {
                0 -> for (green in greenMin..greenMax) {
                    val row = (level shl (2 * ColorBits)) or (green shl ColorBits)
                    for (blue in blueMin..blueMax) sum += histogram[row or blue]
                }
                1 -> for (red in redMin..redMax) {
                    val row = (red shl (2 * ColorBits)) or (level shl ColorBits)
                    for (blue in blueMin..blueMax) sum += histogram[row or blue]
                }
                else -> for (red in redMin..redMax) {
                    for (green in greenMin..greenMax) {
                        sum += histogram[(red shl (2 * ColorBits)) or (green shl ColorBits) or level]
                    }
                }
            }
            return sum
        }

        private fun copy() = ColorBox(redMin, redMax, greenMin, greenMax, blueMin, blueMax, histogram)
    }

    private class BoxQueue(private val less: (ColorBox, ColorBox) -> Boolean) {
        private val boxes = ArrayList<ColorBox>()
        private var sorted = false

        val size: Int get() = boxes.size

        fun push(box: ColorBox) {
            boxes.add(box)
            sorted = false
        }

        fun pop(): ColorBox {
            if (!sorted) {
                serverSort(boxes, less)
                sorted = true
            }
            return boxes.removeAt(boxes.lastIndex)
        }
    }

    private const val ColorBits = 5
    private const val ColorShift = 8 - ColorBits
    private const val Levels = 1 shl ColorBits
    private const val SampleStep = 5
    private const val MinAlpha = 125
    private const val WhiteLevel = 250
    private const val MaxColors = 10
    private const val PopulationTarget = 8
    private const val MaxRounds = 1000
    private const val TopLeftLightness = 0.2
    private const val CornerLightness = 0.4
}

/**
 * `std::sort` of the libc++ the server is built with. Above six elements it is a quicksort, so equal keys
 * land in an order of its own; a stable sort here would pick other colours.
 */
private fun <T> serverSort(items: MutableList<T>, less: (T, T) -> Boolean) = serverSort(items, 0, items.size, less)

private fun <T> serverSort(items: MutableList<T>, from: Int, to: Int, less: (T, T) -> Boolean) {
    var first = from
    var last = to
    while (true) {
        val length = last - first
        when (length) {
            0, 1 -> return
            2 -> {
                if (less(items[last - 1], items[first])) items.swap(first, last - 1)
                return
            }
            3 -> {
                sortThree(items, first, first + 1, last - 1, less)
                return
            }
            4 -> {
                sortFour(items, first, first + 1, first + 2, last - 1, less)
                return
            }
            5 -> {
                sortFive(items, first, first + 1, first + 2, first + 3, last - 1, less)
                return
            }
        }
        if (length <= InsertionLimit) {
            insertionSort(items, first, last, less)
            return
        }
        var middle = first + length / 2
        val tail = last - 1
        var swaps = if (length >= 1000) {
            val quarter = length / 4
            sortFive(items, first, first + quarter, middle, middle + quarter, tail, less)
        } else {
            sortThree(items, first, middle, tail, less)
        }
        var up = first
        var down = tail
        var restart = false
        if (!less(items[up], items[middle])) {
            while (true) {
                down--
                if (up == down) {
                    up++
                    down = last - 1
                    if (!less(items[first], items[down])) {
                        while (true) {
                            if (up == down) return
                            if (less(items[first], items[up])) {
                                items.swap(up, down)
                                swaps++
                                up++
                                break
                            }
                            up++
                        }
                    }
                    if (up == down) return
                    while (true) {
                        while (!less(items[first], items[up])) up++
                        down--
                        while (less(items[first], items[down])) down--
                        if (up >= down) break
                        items.swap(up, down)
                        swaps++
                        up++
                    }
                    first = up
                    restart = true
                    break
                }
                if (less(items[down], items[middle])) {
                    items.swap(up, down)
                    swaps++
                    break
                }
            }
        }
        if (restart) continue
        up++
        if (up < down) {
            while (true) {
                while (less(items[up], items[middle])) up++
                down--
                while (!less(items[down], items[middle])) down--
                if (up > down) break
                items.swap(up, down)
                swaps++
                if (middle == up) middle = down
                up++
            }
        }
        if (up != middle && less(items[middle], items[up])) {
            items.swap(up, middle)
            swaps++
        }
        if (swaps == 0) {
            val headSorted = insertionSortBounded(items, first, up, less)
            if (insertionSortBounded(items, up + 1, last, less)) {
                if (headSorted) return
                last = up
                continue
            } else if (headSorted) {
                first = up + 1
                continue
            }
        }
        if (up - first < last - up) {
            serverSort(items, first, up, less)
            first = up + 1
        } else {
            serverSort(items, up + 1, last, less)
            last = up
        }
    }
}

private fun <T> sortThree(items: MutableList<T>, x: Int, y: Int, z: Int, less: (T, T) -> Boolean): Int {
    if (!less(items[y], items[x])) {
        if (!less(items[z], items[y])) return 0
        items.swap(y, z)
        if (less(items[y], items[x])) {
            items.swap(x, y)
            return 2
        }
        return 1
    }
    if (less(items[z], items[y])) {
        items.swap(x, z)
        return 1
    }
    items.swap(x, y)
    if (less(items[z], items[y])) {
        items.swap(y, z)
        return 2
    }
    return 1
}

private fun <T> sortFour(items: MutableList<T>, a: Int, b: Int, c: Int, d: Int, less: (T, T) -> Boolean): Int {
    var swaps = sortThree(items, a, b, c, less)
    if (less(items[d], items[c])) {
        items.swap(c, d)
        swaps++
        if (less(items[c], items[b])) {
            items.swap(b, c)
            swaps++
            if (less(items[b], items[a])) {
                items.swap(a, b)
                swaps++
            }
        }
    }
    return swaps
}

private fun <T> sortFive(items: MutableList<T>, a: Int, b: Int, c: Int, d: Int, e: Int, less: (T, T) -> Boolean): Int {
    var swaps = sortFour(items, a, b, c, d, less)
    if (less(items[e], items[d])) {
        items.swap(d, e)
        swaps++
        if (less(items[d], items[c])) {
            items.swap(c, d)
            swaps++
            if (less(items[c], items[b])) {
                items.swap(b, c)
                swaps++
                if (less(items[b], items[a])) {
                    items.swap(a, b)
                    swaps++
                }
            }
        }
    }
    return swaps
}

private fun <T> insertionSort(items: MutableList<T>, first: Int, last: Int, less: (T, T) -> Boolean) {
    var sortedEnd = first + 2
    sortThree(items, first, first + 1, sortedEnd, less)
    var next = sortedEnd + 1
    while (next != last) {
        if (less(items[next], items[sortedEnd])) sink(items, first, sortedEnd, next, less)
        sortedEnd = next
        next++
    }
}

private fun <T> insertionSortBounded(items: MutableList<T>, first: Int, last: Int, less: (T, T) -> Boolean): Boolean {
    when (last - first) {
        0, 1 -> return true
        2 -> {
            if (less(items[last - 1], items[first])) items.swap(first, last - 1)
            return true
        }
        3 -> {
            sortThree(items, first, first + 1, last - 1, less)
            return true
        }
        4 -> {
            sortFour(items, first, first + 1, first + 2, last - 1, less)
            return true
        }
        5 -> {
            sortFive(items, first, first + 1, first + 2, first + 3, last - 1, less)
            return true
        }
    }
    var sortedEnd = first + 2
    sortThree(items, first, first + 1, sortedEnd, less)
    var moves = 0
    var next = sortedEnd + 1
    while (next != last) {
        if (less(items[next], items[sortedEnd])) {
            sink(items, first, sortedEnd, next, less)
            moves++
            if (moves == 8) return next + 1 == last
        }
        sortedEnd = next
        next++
    }
    return true
}

private fun <T> sink(items: MutableList<T>, first: Int, sortedEnd: Int, from: Int, less: (T, T) -> Boolean) {
    val moved = items[from]
    var source = sortedEnd
    var hole = from
    while (true) {
        items[hole] = items[source]
        hole = source
        if (hole == first) break
        source--
        if (!less(moved, items[source])) break
    }
    items[hole] = moved
}

private fun <T> MutableList<T>.swap(first: Int, second: Int) {
    val held = this[first]
    this[first] = this[second]
    this[second] = held
}

private const val InsertionLimit = 6

// Björn Ottosson's ok_color.h in single precision, expression for expression.

private fun power(base: Float, exponent: Float): Float = Math.pow(base.toDouble(), exponent.toDouble()).toFloat()

private fun cubeRoot(value: Float): Float = Math.cbrt(value.toDouble()).toFloat()

private fun larger(first: Float, second: Float): Float = if (second > first) second else first

private fun smaller(first: Float, second: Float): Float = if (second < first) second else first

private fun gammaToLinear(channel: Float): Float =
    if (0.04045f < channel) power((channel + 0.055f) / 1.055f, 2.4f) else channel / 12.92f

private fun linearToGamma(channel: Float): Float =
    if (0.0031308f >= channel) 12.92f * channel else 1.055f * power(channel, 0.4166666666666667f) - 0.055f

private fun linearToOklab(red: Float, green: Float, blue: Float): FloatArray {
    val l = cubeRoot(0.4122214708f * red + 0.5363325363f * green + 0.0514459929f * blue)
    val m = cubeRoot(0.2119034982f * red + 0.6806995451f * green + 0.1073969566f * blue)
    val s = cubeRoot(0.0883024619f * red + 0.2817188376f * green + 0.6299787005f * blue)
    return floatArrayOf(
        0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
        1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
        0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
    )
}

private fun oklabToLinear(lightness: Float, a: Float, b: Float): FloatArray {
    val lRoot = lightness + 0.3963377774f * a + 0.2158037573f * b
    val mRoot = lightness - 0.1055613458f * a - 0.0638541728f * b
    val sRoot = lightness - 0.0894841775f * a - 1.2914855480f * b
    val l = lRoot * lRoot * lRoot
    val m = mRoot * mRoot * mRoot
    val s = sRoot * sRoot * sRoot
    return floatArrayOf(
        4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
        -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
        -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s,
    )
}

private fun maxSaturation(a: Float, b: Float): Float {
    val k0: Float
    val k1: Float
    val k2: Float
    val k3: Float
    val k4: Float
    val wl: Float
    val wm: Float
    val ws: Float
    if (-1.88170328f * a - 0.80936493f * b > 1f) {
        k0 = 1.19086277f; k1 = 1.76576728f; k2 = 0.59662641f; k3 = 0.75515197f; k4 = 0.56771245f
        wl = 4.0767416621f; wm = -3.3077115913f; ws = 0.2309699292f
    } else if (1.81444104f * a - 1.19445276f * b > 1f) {
        k0 = 0.73956515f; k1 = -0.45954404f; k2 = 0.08285427f; k3 = 0.12541070f; k4 = 0.14503204f
        wl = -1.2684380046f; wm = 2.6097574011f; ws = -0.3413193965f
    } else {
        k0 = 1.35733652f; k1 = -0.00915799f; k2 = -1.15130210f; k3 = -0.50559606f; k4 = 0.00692167f
        wl = -0.0041960863f; wm = -0.7034186147f; ws = 1.7076147010f
    }
    val saturation = k0 + k1 * a + k2 * b + k3 * a * a + k4 * a * b
    val kl = 0.3963377774f * a + 0.2158037573f * b
    val km = -0.1055613458f * a - 0.0638541728f * b
    val ks = -0.0894841775f * a - 1.2914855480f * b
    val lRoot = 1f + saturation * kl
    val mRoot = 1f + saturation * km
    val sRoot = 1f + saturation * ks
    val l = lRoot * lRoot * lRoot
    val m = mRoot * mRoot * mRoot
    val s = sRoot * sRoot * sRoot
    val lSlope = 3f * kl * lRoot * lRoot
    val mSlope = 3f * km * mRoot * mRoot
    val sSlope = 3f * ks * sRoot * sRoot
    val lCurve = 6f * kl * kl * lRoot
    val mCurve = 6f * km * km * mRoot
    val sCurve = 6f * ks * ks * sRoot
    val value = wl * l + wm * m + ws * s
    val slope = wl * lSlope + wm * mSlope + ws * sSlope
    val curve = wl * lCurve + wm * mCurve + ws * sCurve
    return saturation - value * slope / (slope * slope - 0.5f * value * curve)
}

private fun gamutEdge(a: Float, b: Float, lightness: Float, cuspLightness: Float, cuspChroma: Float): Float {
    val endLightness = lightness
    val endChroma = 1f
    val startLightness = lightness
    if ((endLightness - startLightness) * cuspChroma - (cuspLightness - startLightness) * endChroma <= 0f) {
        return cuspChroma * startLightness /
            (endChroma * cuspLightness + cuspChroma * (startLightness - endLightness))
    }
    var reach = cuspChroma * (startLightness - 1f) /
        (endChroma * (cuspLightness - 1f) + cuspChroma * (startLightness - endLightness))
    val lightnessStep = endLightness - startLightness
    val chromaStep = endChroma
    val kl = 0.3963377774f * a + 0.2158037573f * b
    val km = -0.1055613458f * a - 0.0638541728f * b
    val ks = -0.0894841775f * a - 1.2914855480f * b
    val lStep = lightnessStep + chromaStep * kl
    val mStep = lightnessStep + chromaStep * km
    val sStep = lightnessStep + chromaStep * ks
    val atLightness = startLightness * (1f - reach) + reach * endLightness
    val atChroma = reach * endChroma
    val lRoot = atLightness + atChroma * kl
    val mRoot = atLightness + atChroma * km
    val sRoot = atLightness + atChroma * ks
    val l = lRoot * lRoot * lRoot
    val m = mRoot * mRoot * mRoot
    val s = sRoot * sRoot * sRoot
    val lSlope = 3f * lStep * lRoot * lRoot
    val mSlope = 3f * mStep * mRoot * mRoot
    val sSlope = 3f * sStep * sRoot * sRoot
    val lCurve = 6f * lStep * lStep * lRoot
    val mCurve = 6f * mStep * mStep * mRoot
    val sCurve = 6f * sStep * sStep * sRoot

    val red = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s - 1f
    val redSlope = 4.0767416621f * lSlope - 3.3077115913f * mSlope + 0.2309699292f * sSlope
    val redCurve = 4.0767416621f * lCurve - 3.3077115913f * mCurve + 0.2309699292f * sCurve
    val redRate = redSlope / (redSlope * redSlope - 0.5f * red * redCurve)
    val redReach = if (redRate >= 0f) -red * redRate else Float.MAX_VALUE

    val green = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s - 1f
    val greenSlope = -1.2684380046f * lSlope + 2.6097574011f * mSlope - 0.3413193965f * sSlope
    val greenCurve = -1.2684380046f * lCurve + 2.6097574011f * mCurve - 0.3413193965f * sCurve
    val greenRate = greenSlope / (greenSlope * greenSlope - 0.5f * green * greenCurve)
    val greenReach = if (greenRate >= 0f) -green * greenRate else Float.MAX_VALUE

    val blue = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s - 1f
    val blueSlope = -0.0041960863f * lSlope - 0.7034186147f * mSlope + 1.7076147010f * sSlope
    val blueCurve = -0.0041960863f * lCurve - 0.7034186147f * mCurve + 1.7076147010f * sCurve
    val blueRate = blueSlope / (blueSlope * blueSlope - 0.5f * blue * blueCurve)
    val blueReach = if (blueRate >= 0f) -blue * blueRate else Float.MAX_VALUE

    reach += smaller(redReach, smaller(greenReach, blueReach))
    return reach
}

/** Chroma at saturation 0, 0.8 and 1 for this lightness and hue direction. */
private fun chromaLimits(lightness: Float, a: Float, b: Float): FloatArray {
    val cuspSaturation = maxSaturation(a, b)
    val atCusp = oklabToLinear(1f, cuspSaturation * a, cuspSaturation * b)
    val cuspLightness = cubeRoot(1f / larger(larger(atCusp[0], atCusp[1]), atCusp[2]))
    val cuspChroma = cuspLightness * cuspSaturation
    val peak = gamutEdge(a, b, lightness, cuspLightness, cuspChroma)
    val riseMax = cuspChroma / cuspLightness
    val fallMax = cuspChroma / (1f - cuspLightness)
    val scale = peak / smaller(lightness * riseMax, (1f - lightness) * fallMax)

    val riseMid = 0.11516993f + 1f / (
        7.44778970f + 4.15901240f * b +
            a * (
                -2.19557347f + 1.75198401f * b +
                    a * (-2.13704948f - 10.02301043f * b + a * (-4.24894561f + 5.38770819f * b + 4.69891013f * a))
                )
        )
    val fallMid = 0.11239642f + 1f / (
        1.61320320f - 0.68124379f * b +
            a * (
                0.40370612f + 0.90148123f * b +
                    a * (-0.27087943f + 0.61223990f * b + a * (0.00299215f - 0.45399568f * b - 0.14661872f * a))
                )
        )
    var rise = lightness * riseMid
    var fall = (1f - lightness) * fallMid
    val middle = 0.9f * scale *
        sqrt(sqrt(1f / (1f / (rise * rise * rise * rise) + 1f / (fall * fall * fall * fall))))
    rise = lightness * 0.4f
    fall = (1f - lightness) * 0.8f
    val base = sqrt(1f / (1f / (rise * rise) + 1f / (fall * fall)))
    return floatArrayOf(base, middle, peak)
}

private fun toe(lightness: Float): Float =
    0.5f * (ToeK3 * lightness - ToeK1 + sqrt((ToeK3 * lightness - ToeK1) * (ToeK3 * lightness - ToeK1) + 4f * ToeK2 * ToeK3 * lightness))

private fun toeInverse(lightness: Float): Float =
    (lightness * lightness + ToeK1 * lightness) / (ToeK3 * (lightness + ToeK2))

private fun okhslToSrgb(hue: Float, saturation: Float, lightness: Float): FloatArray {
    if (lightness == 1f) return floatArrayOf(1f, 1f, 1f)
    if (lightness == 0f) return floatArrayOf(0f, 0f, 0f)
    val a = cos(2f * Pi * hue)
    val b = sin(2f * Pi * hue)
    val labLightness = toeInverse(lightness)
    val limits = chromaLimits(labLightness, a, b)
    val base = limits[0]
    val middle = limits[1]
    val peak = limits[2]
    val chroma = if (saturation < Mid) {
        val share = MidInverse * saturation
        val k1 = Mid * base
        val k2 = 1f - k1 / middle
        share * k1 / (1f - k2 * share)
    } else {
        val share = (saturation - Mid) / (1f - Mid)
        val k1 = (1f - Mid) * middle * middle * MidInverse * MidInverse / base
        val k2 = 1f - k1 / (peak - middle)
        middle + share * k1 / (1f - k2 * share)
    }
    val linear = oklabToLinear(labLightness, chroma * a, chroma * b)
    return floatArrayOf(linearToGamma(linear[0]), linearToGamma(linear[1]), linearToGamma(linear[2]))
}

private const val Pi = 3.14159265358979323846f
private const val Mid = 0.8f
private const val MidInverse = 1.25f
private const val ToeK1 = 0.206f
private const val ToeK2 = 0.03f
private const val ToeK3 = (1f + ToeK1) / (1f + ToeK2)
