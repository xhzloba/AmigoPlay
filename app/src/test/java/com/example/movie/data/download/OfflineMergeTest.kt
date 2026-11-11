package com.example.movie.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMergeTest {
    @Test
    fun newEpisodeKeepsTheOnesAlreadyDownloaded() {
        val older = stream(season = 1, episode = 3, start = true)
        val fresh = stream(season = 1, episode = 1, start = true)
        val merged = mergeStreams(listOf(fresh), listOf(older), "movie")

        assertEquals(listOf(1, 3), merged.map { it.episode })
        assertTrue(merged.first { it.episode == 1 }.start)
        assertFalse(merged.first { it.episode == 3 }.start)
    }

    @Test
    fun sameEpisodeReplacesOnlyThatSlot() {
        val kept = stream(season = 1, episode = 3, start = false)
        val again = stream(season = 1, episode = 1, start = true, height = 1080)
        val previous = listOf(stream(season = 1, episode = 1, start = true, height = 530), kept)
        val merged = mergeStreams(listOf(again), previous, "movie")

        assertEquals(listOf(1, 3), merged.map { it.episode })
        assertEquals(1080, merged.first { it.episode == 1 }.height)
        assertEquals(530, merged.first { it.episode == 3 }.height)
    }

    @Test
    fun multiPickBuildsAllStreams() {
        val playback = com.example.movie.data.player.WatchPlayback(
            stream = com.example.movie.data.player.WatchStream(
                url = "https://example/1",
                kind = com.example.movie.data.player.WatchKind.Hls,
                subtitles = emptyList(),
            ),
            episodes = listOf(
                com.example.movie.data.player.WatchEpisode(
                    1, 1,
                    com.example.movie.data.player.WatchStream("https://example/1/1", com.example.movie.data.player.WatchKind.Hls, emptyList()),
                ),
                com.example.movie.data.player.WatchEpisode(
                    1, 2,
                    com.example.movie.data.player.WatchStream("https://example/1/2", com.example.movie.data.player.WatchKind.Hls, emptyList()),
                ),
            ),
        )
        val item = offlineItem(
            title = com.example.movie.domain.model.SavedTitle(
                id = "id",
                title = "Title",
                year = 2020,
                type = "serial",
            ),
            playback = playback,
            height = 720,
            picked = setOf(1 to 1, 1 to 2),
            audioIndex = -1,
        )
        assertEquals(2, item.streams.size)
        assertEquals(720, item.height)
    }

    private fun stream(season: Int, episode: Int, start: Boolean, height: Int = 530) = OfflineStream(
        season = season,
        episode = episode,
        url = "https://example/$season/$episode",
        hls = true,
        start = start,
        height = height,
    )
}
