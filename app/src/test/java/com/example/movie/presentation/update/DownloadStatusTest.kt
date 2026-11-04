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
    fun plainNotesAreAdded() {
        val notes = releaseNotes("Новый каталог\n— Скорость загрузки")
        assertEquals(listOf("Новый каталог", "Скорость загрузки"), notes.added)
        assertEquals(emptyList<String>(), notes.fixed)
    }

    @Test
    fun dashedHeadersAreLabelsNotLines() {
        val notes = releaseNotes("— Добавлено\n— Логотип выше\n— Исправлено\n— Тап")
        assertEquals(listOf("Логотип выше"), notes.added)
        assertEquals(listOf("Тап"), notes.fixed)
    }

    @Test
    fun sectionHeadersSplitAddedAndFixed() {
        val notes = releaseNotes(
            """
            Добавлено
            — Логотип выше
            Исправлено
            — Тап после прокрутки
            """.trimIndent(),
        )
        assertEquals(listOf("Логотип выше"), notes.added)
        assertEquals(listOf("Тап после прокрутки"), notes.fixed)
    }
}
