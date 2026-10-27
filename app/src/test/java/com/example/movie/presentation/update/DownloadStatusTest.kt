package com.example.movie.presentation.update

import com.example.movie.domain.model.DownloadProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStatusTest {
    @Test
    fun statusShowsPercentRatioAndSpeed() {
        val text = downloadStatus(
            DownloadProgress(
                read = 15L * 1024 * 1024,
                total = 30L * 1024 * 1024,
                bytesPerSecond = (2.4 * 1024 * 1024).toLong(),
            ),
        )
        assertEquals("50% — 15 МБ из 30 МБ — 2,4 МБ/с", text)
    }

    @Test
    fun notesGetADashPerLine() {
        assertEquals(
            "— Новый каталог\n— Скорость загрузки",
            dashedNotes("Новый каталог\nСкорость загрузки"),
        )
        assertEquals("— Уже с тире", dashedNotes("— Уже с тире"))
    }
}
