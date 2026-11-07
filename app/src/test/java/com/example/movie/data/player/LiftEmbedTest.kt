package com.example.movie.data.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiftEmbedTest {
    @Test
    fun prefersHlsManifestAndCaptions() {
        val html = """
            source: {
                dash: "https://cdn.example/video.mpd?t=1",
                hls: "https://cdn.example/master.m3u8?t=1",
                cc: [{"url":"https://cdn.example/ru.vtt?t=1","name":"Рус. полные"}]
            }
        """.trimIndent()

        val playback = parseLiftEmbed(html)

        assertEquals("https://cdn.example/master.m3u8?t=1", playback?.stream?.url)
        assertEquals(WatchKind.Hls, playback?.stream?.kind)
        assertEquals(
            listOf(WatchSubtitle("https://cdn.example/ru.vtt?t=1", "Рус. полные")),
            playback?.stream?.subtitles,
        )
        assertTrue(playback?.episodes.orEmpty().isEmpty())
    }

    @Test
    fun fallsBackToDash() {
        val playback = parseLiftEmbed("""dash: "https://cdn.example/a.mpd"""")
        assertEquals(WatchKind.Dash, playback?.stream?.kind)
        assertEquals("https://cdn.example/a.mpd", playback?.stream?.url)
    }

    @Test
    fun seriesPlaylistOpensCurrentEpisode() {
        val html = """
            current: { season: 1, episode: "2" },
            seasons:[{
              "season":1,
              "episodes":[
                {"episode":"1","dash":"https://cdn.example/e1.mpd","hls":"https://cdn.example/e1.m3u8","cc":[{"url":"https://cdn.example/e1.vtt","name":"Рус"}]},
                {"episode":"2","hls":"https://cdn.example/e2.m3u8"}
              ]
            },{
              "season":2,
              "episodes":[{"episode":1,"hls":"https://cdn.example/s2e1.m3u8"}]
            }]
        """.trimIndent()

        val playback = parseLiftEmbed(html)

        assertEquals("https://cdn.example/e2.m3u8", playback?.stream?.url)
        assertEquals(listOf(1 to 1, 1 to 2, 2 to 1), playback?.episodes?.map { it.season to it.episode })
        assertEquals("https://cdn.example/e1.vtt", playback?.episodes?.first()?.stream?.subtitles?.single()?.url)
    }

    @Test
    fun queryPicksRequestedEpisode() {
        val html = """
            seasons:[{"season":1,"episodes":[
              {"episode":"1","hls":"https://cdn.example/e1.m3u8"},
              {"episode":"3","hls":"https://cdn.example/e3.m3u8"}
            ]}]
        """.trimIndent()

        val playback = parseLiftEmbed(html, season = 1, episode = 3)

        assertEquals("https://cdn.example/e3.m3u8", playback?.stream?.url)
    }

    @Test
    fun ignoresPageWithoutManifest() {
        assertNull(parseLiftEmbed("<html>no player</html>"))
    }
}
