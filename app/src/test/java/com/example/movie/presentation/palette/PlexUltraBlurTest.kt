package com.example.movie.presentation.palette

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Expected values come from the reference port that was checked bit for bit against the native code of
 * Plex Media Server 1.43.5. The images are generated, so the test needs no files.
 */
class PlexUltraBlurTest {

    @Test
    fun matchesTheServerOnGeneratedImages() {
        for (case in Expected.indices) {
            val image = image(case)
            val colors = PlexUltraBlur.colors(image.pixels, image.width, image.height)
            val actual = colors?.joinToString(" ") { "%06x".format(it) }
            assertEquals("case $case, ${image.width}x${image.height}", Expected[case], actual)
        }
    }

    @Test
    fun tooSmallImageHasNoColors() {
        assertEquals(null, PlexUltraBlur.colors(intArrayOf(0xFF336699.toInt()), 1, 1))
    }

    @Test
    fun transparentPixelsAreSkipped() {
        val pixels = IntArray(16 * 16) { 0x00336699 }
        assertEquals(null, PlexUltraBlur.colors(pixels, 16, 16))
    }

    private class Image(val width: Int, val height: Int, val pixels: IntArray)

    private class Sequence(private var state: Int) {
        fun below(limit: Int): Int {
            state = (state * 1103515245 + 12345) and 0x7fffffff
            return (state shr 8) % limit
        }
    }

    private fun image(case: Int): Image {
        val random = Sequence(1000 + case * 7919)
        val width = 6 + random.below(64)
        val height = 6 + random.below(64)
        val pixels = IntArray(width * height)
        fun color() = Triple(random.below(256), random.below(256), random.below(256))
        fun pack(red: Int, green: Int, blue: Int) =
            0xFF000000.toInt() or (red.coerceIn(0, 255) shl 16) or (green.coerceIn(0, 255) shl 8) or blue.coerceIn(0, 255)
        when (case % 6) {
            0 -> for (index in pixels.indices) {
                val (red, green, blue) = color()
                pixels[index] = pack(red, green, blue)
            }
            1 -> {
                val palette = List(2 + random.below(6)) { color() }
                for (index in pixels.indices) {
                    val (red, green, blue) = palette[random.below(palette.size)]
                    pixels[index] = pack(red, green, blue)
                }
            }
            2 -> {
                val from = color()
                val to = color()
                val span = 3 * (width - 1) + 2 * (height - 1)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val step = 3 * x + 2 * y
                        val red = from.first + Math.floorDiv((to.first - from.first) * step, span) + random.below(7) - 3
                        val green = from.second + Math.floorDiv((to.second - from.second) * step, span) + random.below(7) - 3
                        val blue = from.third + Math.floorDiv((to.third - from.third) * step, span) + random.below(7) - 3
                        pixels[y * width + x] = pack(red, green, blue)
                    }
                }
            }
            3 -> for (index in pixels.indices) {
                val value = random.below(256)
                pixels[index] = pack(value, value, value)
            }
            4 -> {
                val colors = List(6) { color() }
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val base = colors[x * 3 / width + 3 * (y * 2 / height)]
                        val red = base.first + random.below(9) - 4
                        val green = base.second + random.below(9) - 4
                        val blue = base.third + random.below(9) - 4
                        pixels[y * width + x] = pack(red, green, blue)
                    }
                }
            }
            else -> {
                val (red, green, blue) = color()
                val rare = 1 + random.below(12)
                for (index in pixels.indices) {
                    val tinted = random.below(rare) == 0 && case != 5
                    pixels[index] = if (tinted) pack(red, green, blue) else pack(255, 255, 255)
                }
            }
        }
        return Image(width, height, pixels)
    }

    private companion object {
        val Expected = listOf(
        "043907 2f6c44 040cfb 241ba4",
        "4b143d 135b14 0b14bc 135b14",
        "023440 136978 176977 1d6a6c",
        "1c1c1c 434343 303130 3b3b3b",
        "2c3200 b1060e 7414cb 933f45",
        null,
        "501818 4b6632 463aab 9b3927",
        "482301 2f6b4b 8c4a06 2f6b4b",
        "192c53 355aa6 2b56b7 3c5a9d",
        "242424 1f1e1e 202020 030403",
        "073533 186d58 366a4b 356d30",
        "4b1e19 90433a 90423a 90433a",
        "4d1636 713b96 4c147b 8b2096",
        "312257 852ca3 624aa4 852ca3",
        "551303 256b5e 1a6c61 306a58",
        "232423 141314 232222 141314",
        "570c1b a7233c 691eda 4a691e",
        "51004a 9d008f 9d0091 9d008f",
        "580721 321997 8e411b a0147a",
        "3f0a65 a02955 7c21bf a02955",
        "023723 216b5e 852da1 266a61",
        "141314 262525 232222 3a3939",
        "510149 166b64 551fec 830ac2",
        "083815 1c7030 197030 1c7030",
        "043333 723fa4 294ad2 1c7022",
        "013345 873197 943957 873197",
        "1d3504 864e10 ab200c 8d4911",
        "232423 303130 0b0b0b 2b2b2b",
        "461a3f 6127dc 861fb0 8c2190",
        "2d205e 5f47ae 5d48b0 5f47ae",
        "151c77 340ed1 86378e 3c6313",
        "3c1d51 a9095d 75419a 6b33c3",
        "461a42 933d4c 765729 8d3b6e",
        "262525 434343 232423 3b3b3b",
        "1c2b55 04536a 1a6d56 763aa9",
        "570727 a81850 a71952 a81850",
        "0c3626 245375 511e75 644aa1",
        "12028e 4424bf 4424bf 4b6827",
        "5a050d 932c33 a72333 b1031b",
        "262525 141314 4e4e4d 0b0b0b",
        "123049 6c30c5 6932c7 ae1227",
        "013537 046b6f 046b6f 046b6f",
        "471547 3854b5 8c1ca8 2553a3",
        "141382 2717a8 2423c3 2c0c8b",
        "103628 356c37 396c2a 2e6c42",
        "1e1d1d 242424 0b0b0b 030403",
        "090191 ae0e2e af0d2c 8a24a4",
        "1a351f 3b6a44 3b6a44 3b6a44",
        "441255 22657b 7621c9 521d3c",
        "211b73 242cd0 342ba4 035ab7",
        "392b10 5b5353 545456 725929",
        "2e2e2e 682c8b 030403 1c1c1c",
        "4e1336 206d4b 265ea0 2b5c44",
        "4d1e00 964001 954101 964001",
        "2e186b 5640a8 981094 423bdc",
        "541403 8223b4 a32f0d 9e3331",
        "4a1345 833e84 266d46 863d7f",
        "2e2e2e 2b2b2b 4c4c4b 2b2b2b",
        "4e0051 961596 6c1c59 8823a8",
        "191182 1f148b 1c138c 1f148b",
        )
    }
}
