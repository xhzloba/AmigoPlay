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
                cc: [{"url":"https://cdn.example/ru.vtt?t=1","name":"Рус. полные"}],
                audio: {"names":["Рус. Дублированный","LostFilm"],"order":[1,0]}
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
        assertEquals(listOf("Рус. Дублированный", "LostFilm"), playback?.stream?.audioNames)
        assertEquals(listOf(1, 0), playback?.stream?.audioOrder)
        assertEquals("https://cdn.example/video.mpd?t=1", playback?.stream?.alternateUrl)
        assertEquals(WatchKind.Dash, playback?.stream?.alternateKind)
        assertEquals(
            listOf(
                WatchManifest("https://cdn.example/master.m3u8?t=1", WatchKind.Hls),
                WatchManifest("https://cdn.example/video.mpd?t=1", WatchKind.Dash),
            ),
            playback?.stream?.manifests,
        )
    }

    @Test
    fun parsesDashAv1AndVp9Candidates() {
        val html = """
            source: {
                hls: "https://cdn.example/master.m3u8",
                dash: "https://cdn.example/h264.mpd",
                dash_vp9: "https://cdn.example/vp9.mpd",
                dash_av1: "https://cdn.example/av1.mpd"
            }
        """.trimIndent()

        val stream = parseLiftEmbed(html)?.stream

        assertEquals("https://cdn.example/master.m3u8", stream?.url)
        assertEquals(
            listOf(
                WatchManifest("https://cdn.example/master.m3u8", WatchKind.Hls),
                WatchManifest("https://cdn.example/av1.mpd", WatchKind.Dash),
                WatchManifest("https://cdn.example/vp9.mpd", WatchKind.Dash),
                WatchManifest("https://cdn.example/h264.mpd", WatchKind.Dash),
            ),
            stream?.manifests,
        )
    }

    @Test
    fun playlistApiNestedStreamsPreferAv1Vp9Hls() {
        val raw = """
            {
              "title":"Мстители",
              "streams":{
                "hls":"https://cdn.example/master.m3u8",
                "dash_vp9":"https://cdn.example/vp9.mpd",
                "dash_av1":"https://cdn.example/av1.mpd"
              },
              "audio":{"names":["Рус"],"order":[0]},
              "cc":[{"url":"https://cdn.example/ru.vtt","name":"Рус"}]
            }
        """.trimIndent()

        val stream = parseLiftPlaylist(raw)?.stream

        assertEquals("https://cdn.example/master.m3u8", stream?.url)
        assertEquals(WatchKind.Hls, stream?.kind)
        assertEquals(
            listOf(
                WatchManifest("https://cdn.example/master.m3u8", WatchKind.Hls),
                WatchManifest("https://cdn.example/av1.mpd", WatchKind.Dash),
                WatchManifest("https://cdn.example/vp9.mpd", WatchKind.Dash),
            ),
            stream?.manifests,
        )
        assertEquals(listOf("Рус"), stream?.audioNames)
        assertEquals("https://cdn.example/ru.vtt", stream?.subtitles?.single()?.url)
    }

    @Test
    fun playlistApiSeriesEpisodeStreams() {
        val raw = """
            {
              "title":"Игра престолов",
              "seasons":[{
                "season":5,
                "episodes":[{
                  "episode":"1",
                  "streams":{
                    "hls":"https://cdn.example/s5e1.m3u8",
                    "dash_vp9":"https://cdn.example/s5e1-vp9.mpd",
                    "dash_av1":"https://cdn.example/s5e1-av1.mpd"
                  },
                  "audio":{"names":["LostFilm"],"order":[0]}
                }]
              }]
            }
        """.trimIndent()

        val playback = parseLiftPlaylist(raw, season = 5, episode = 1)

        assertEquals("https://cdn.example/s5e1.m3u8", playback?.stream?.url)
        assertEquals(
            listOf(
                WatchManifest("https://cdn.example/s5e1.m3u8", WatchKind.Hls),
                WatchManifest("https://cdn.example/s5e1-av1.mpd", WatchKind.Dash),
                WatchManifest("https://cdn.example/s5e1-vp9.mpd", WatchKind.Dash),
            ),
            playback?.stream?.manifests,
        )
        assertEquals(listOf(5 to 1), playback?.episodes?.map { it.season to it.episode })
    }

    @Test
    fun dashManifestKeepsHeightsHlsDrops() {
        val hls = "#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=1272x530\n"
        val dash = """<Representation height="2160"/><Representation height="1080"/><Representation height="720"/>"""
        assertEquals(setOf(530), manifestHeights(hls))
        assertEquals(setOf(2160, 1080, 720), manifestHeights(dash))
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
                {"episode":"1","dash":"https://cdn.example/e1.mpd","hls":"https://cdn.example/e1.m3u8","audio":{"names":["Кубик в кубе","Рус. Дублированный"],"order":[0,1]},"cc":[{"url":"https://cdn.example/e1.vtt","name":"Рус"}]},
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
        assertEquals(listOf("Кубик в кубе", "Рус. Дублированный"), playback?.episodes?.first()?.stream?.audioNames)
        assertEquals(listOf(0, 1), playback?.episodes?.first()?.stream?.audioOrder)
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
