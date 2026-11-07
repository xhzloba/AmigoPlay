package com.example.movie.data.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackLabelsTest {
    @Test
    fun videoLabelUsesHeightAndBitrate() {
        assertEquals("1080p", videoTrackLabel(1080, 4_500_000))
        assertEquals("4K", videoTrackLabel(2160, 12_000_000))
        assertEquals("720p", videoTrackLabel(720, 0))
        assertEquals("Видео", videoTrackLabel(0, 0))
    }

    @Test
    fun namedLabelPrefersManifestName() {
        assertEquals("Рус. Дублированный", namedTrackLabel("Рус. Дублированный", "ru", 0))
        assertEquals("Русский", namedTrackLabel(null, "ru", 1))
        assertEquals("Дорожка 3", namedTrackLabel(" ", "und", 2))
    }

    @Test
    fun audioCodeMapsToStudioName() {
        val names = listOf("Кубик в кубе", "Рус. Дублированный", "LostFilm")
        assertEquals("Кубик в кубе", audioTrackLabel("rus0", "ru", 4, names))
        assertEquals("LostFilm", audioTrackLabel("rus2", "ru", 0, names))
        assertEquals("Русский", audioTrackLabel(null, "ru", 0, names))
    }
}
