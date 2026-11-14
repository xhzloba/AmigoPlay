package com.example.movie.presentation.home

import com.example.movie.domain.model.ContentKind
import com.example.movie.domain.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfPagesTest {
    @Test
    fun keepsNewIdsAndStopsOnEmptyPage() {
        val pages = ShelfPages()
        val first = pages.advance(listOf(movie("1"), movie("2")), hasMore = true, paged = true)
        assertEquals(listOf("1", "2"), first.fresh.map { it.id })
        assertFalse(first.end)
        assertFalse(first.again)
        assertEquals(1, pages.page)

        val empty = pages.advance(emptyList(), hasMore = true, paged = true)
        assertTrue(empty.fresh.isEmpty())
        assertTrue(empty.end)
        assertFalse(empty.again)
    }

    @Test
    fun stopsWhenApiHasNoNextEvenIfPageHasNewTitles() {
        val pages = ShelfPages()
        val last = pages.advance(listOf(movie("1"), movie("2")), hasMore = false, paged = true)
        assertEquals(listOf("1", "2"), last.fresh.map { it.id })
        assertTrue(last.end)
        assertFalse(last.again)
    }

    @Test
    fun skipsShiftedWindowAndStopsOnExactRepeat() {
        val pages = ShelfPages()
        pages.advance(listOf(movie("1"), movie("2"), movie("3")), hasMore = true, paged = true)

        val shifted = pages.advance(listOf(movie("1"), movie("2")), hasMore = true, paged = true)
        assertTrue(shifted.fresh.isEmpty())
        assertFalse(shifted.end)
        assertTrue(shifted.again)
        assertEquals(2, pages.page)

        val next = pages.advance(listOf(movie("2"), movie("4")), hasMore = true, paged = true)
        assertEquals(listOf("4"), next.fresh.map { it.id })
        assertFalse(next.end)

        val echo = pages.advance(listOf(movie("2"), movie("4")), hasMore = true, paged = true)
        assertTrue(echo.end)
        assertFalse(echo.again)
    }

    @Test
    fun seriesUpdatesFollowNextWhenSeriesComesBack() {
        val pages = ShelfPages()
        val page1 = pages.advance(
            listOf(movie("serial-1"), movie("serial-2"), movie("movie-1")),
            hasMore = true,
            paged = true,
        )
        assertEquals(listOf("serial-1", "serial-2", "movie-1"), page1.fresh.map { it.id })
        assertFalse(page1.end)

        val page2 = pages.advance(
            listOf(movie("serial-3"), movie("serial-4")),
            hasMore = true,
            paged = true,
        )
        assertEquals(listOf("serial-3", "serial-4"), page2.fresh.map { it.id })
        assertFalse(page2.end)

        // Новая серия подняла уже виденные id, API всё ещё отдаёт next.
        val shifted = pages.advance(
            listOf(movie("serial-1"), movie("serial-2"), movie("movie-1")),
            hasMore = true,
            paged = true,
        )
        assertTrue(shifted.fresh.isEmpty())
        assertTrue(shifted.again)
        assertFalse(shifted.end)

        val page4 = pages.advance(
            listOf(movie("serial-1"), movie("serial-5")),
            hasMore = true,
            paged = true,
        )
        assertEquals(listOf("serial-5"), page4.fresh.map { it.id })
        assertFalse(page4.end)
        assertFalse(page4.again)

        val done = pages.advance(listOf(movie("serial-6")), hasMore = false, paged = true)
        assertEquals(listOf("serial-6"), done.fresh.map { it.id })
        assertTrue(done.end)
    }

    @Test
    fun unpagedShelfEndsAfterFirstPage() {
        val pages = ShelfPages()
        val take = pages.advance(listOf(movie("1")), hasMore = true, paged = false)
        assertEquals(listOf("1"), take.fresh.map { it.id })
        assertTrue(take.end)
        assertFalse(take.again)
    }

    private fun movie(id: String) = Movie(
        id = id,
        title = id,
        originalTitle = "",
        year = null,
        overview = "",
        genres = "",
        country = "",
        director = "",
        posterUrl = null,
        backdropUrl = null,
        logoUrl = null,
        ratingKp = null,
        ratingImdb = null,
        age = null,
        kind = ContentKind.Series,
        camrip = false,
    )
}
