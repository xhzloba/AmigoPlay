package com.example.movie.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SeriesReleaseTest {
    private val today = LocalDate.of(2026, 11, 8)

    @Test
    fun futureEpisodeIsNotAiredYet() {
        val snapshot = franchise(
            episode(1, 4, "2026-11-01"),
            episode(1, 5, "2026-11-20"),
        ).releaseSnapshot(today)
        assertEquals("1:4", snapshot.key)
        assertEquals(LocalDate.of(2026, 11, 20).toEpochDay(), snapshot.nextEpochDay)
    }

    @Test
    fun blankDateCountsAsAired() {
        val snapshot = franchise(
            episode(2, 1, null),
        ).releaseSnapshot(today)
        assertEquals("2:1", snapshot.key)
        assertNull(snapshot.nextEpochDay)
        assertEquals(true, snapshot.undated)
    }

    @Test
    fun anyRealDateIsNotUndated() {
        val snapshot = franchise(
            episode(1, 1, null),
            episode(1, 2, "2026-11-01"),
        ).releaseSnapshot(today)
        assertEquals(false, snapshot.undated)
    }

    @Test
    fun unseededSubscriptionDoesNotAlert() {
        val snapshot = franchise(episode(1, 3, "2026-11-01")).releaseSnapshot(today)
        assertNull(shouldAlert("", snapshot))
    }

    @Test
    fun newerEpisodeAlerts() {
        val snapshot = franchise(
            episode(1, 3, "2026-11-01"),
            episode(1, 4, "2026-11-08"),
        ).releaseSnapshot(today)
        val aired = shouldAlert("1:3", snapshot)
        assertEquals(4, aired?.episode)
    }

    @Test
    fun sameEpisodeDoesNotAlert() {
        val snapshot = franchise(episode(1, 3, "2026-11-01")).releaseSnapshot(today)
        assertNull(shouldAlert("1:3", snapshot))
    }

    @Test
    fun firstEpisodeAfterNoneAlerts() {
        val snapshot = franchise(episode(1, 1, "2026-11-08")).releaseSnapshot(today)
        assertEquals(1, shouldAlert("none", snapshot)?.episode)
    }

    private fun episode(season: Int, number: Int, release: String?) =
        Triple(season, number, release)

    private fun franchise(vararg episodes: Triple<Int, Int, String?>): Franchise {
        val seasons = episodes.groupBy { it.first }.map { (season, items) ->
            FranchiseSeason(
                number = season,
                posterUrl = null,
                episodes = items.map { FranchiseEpisode(it.second, null, it.third) },
            )
        }
        return Franchise(seasons = seasons, series = true)
    }
}
