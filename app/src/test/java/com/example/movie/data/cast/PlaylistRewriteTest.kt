package com.example.movie.data.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistRewriteTest {
    @Test
    fun rewritesHlsVariantsAndKey() {
        val text = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=1
            720/index.m3u8
            #EXT-X-KEY:METHOD=AES-128,URI="key.key"
        """.trimIndent()
        val out = rewriteHls(text, "https://cdn.example/a/master.m3u8") { "L:$it" }
        assertTrue(out.contains("L:https://cdn.example/a/720/index.m3u8"))
        assertTrue(out.contains("URI=\"L:https://cdn.example/a/key.key\""))
    }

    @Test
    fun keepsOnlyChosenHeightAndAudio() {
        val text = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="a",NAME="LostFilm",DEFAULT=YES,AUTOSELECT=YES,URI="lost.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="a",NAME="HDRezka",DEFAULT=NO,AUTOSELECT=NO,URI="rezka.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=2,RESOLUTION=1920x1080,AUDIO="a"
            1080/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=1,RESOLUTION=1280x720,AUDIO="a"
            720/index.m3u8
        """.trimIndent()
        val out = narrowHls(text, 720, "HDRezka", 0)
        assertTrue(out.contains("720/index.m3u8"))
        assertFalse(out.contains("1080/index.m3u8"))
        assertTrue(out.contains("NAME=\"HDRezka\",DEFAULT=YES,AUTOSELECT=YES"))
        assertTrue(out.contains("NAME=\"LostFilm\",DEFAULT=NO,AUTOSELECT=NO"))
    }

    @Test
    fun keepsVariantsWhenHeightIsMissing() {
        val text = """
            #EXTM3U
            #EXT-X-STREAM-INF:RESOLUTION=1920x1080
            1080/index.m3u8
            #EXTINF:6,
            seg.ts
        """.trimIndent()
        val master = narrowHls(text, 480, null, 0)
        assertTrue(master.contains("1080/index.m3u8"))
        val media = narrowHls("#EXTM3U\n#EXTINF:6,\nseg.ts", 720, null, 0)
        assertEquals("#EXTM3U\n#EXTINF:6,\nseg.ts", media)
    }

    @Test
    fun dropsOtherDashHeights() {
        val xml = """
            <MPD>
            <Representation height="1080" id="a"><BaseURL>a.m4s</BaseURL></Representation>
            <Representation height="720" id="b"><BaseURL>b.m4s</BaseURL></Representation>
            <Representation id="audio"><BaseURL>audio.m4s</BaseURL></Representation>
            </MPD>
        """.trimIndent()
        val out = narrowDash(xml, 720)
        assertFalse(out.contains("height=\"1080\""))
        assertTrue(out.contains("height=\"720\""))
        assertTrue(out.contains("audio.m4s"))
    }

    @Test
    fun rewritesDashTemplatesOntoLocalBase() {
        val xml = """
            <MPD>
            <BaseURL>https://cdn.example/v/</BaseURL>
            <SegmentTemplate media="seg_${'$'}Number${'$'}.m4s" initialization="init.mp4"/>
            </MPD>
        """.trimIndent()
        val (out, base) = rewriteDash(xml, "https://cdn.example/v/index.mpd", "http://phone/b/1/") { "L:$it" }
        assertEquals("https://cdn.example/v/", base)
        assertTrue(out.contains("<BaseURL>http://phone/b/1/</BaseURL>"))
        assertTrue(out.contains("media=\"http://phone/b/1/seg_\$Number\$.m4s\""))
        assertTrue(out.contains("initialization=\"L:https://cdn.example/v/init.mp4\""))
    }

    @Test
    fun parsesAvTransportControl() {
        val xml = """
            <root>
            <device><friendlyName>Living Room</friendlyName></device>
            <service>
            <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
            <controlURL>/av</controlURL>
            </service>
            </root>
        """.trimIndent()
        val device = parseDevice(xml, "http://192.168.1.20:1400/device.xml")
        assertEquals("Living Room", device?.name)
        assertEquals("http://192.168.1.20:1400/av", device?.controlUrl)
    }

    @Test
    fun rejectsParentSegments() {
        assertFalse(safeRelative("../secret"))
        assertFalse(safeRelative("/etc/passwd"))
        assertTrue(safeRelative("video/1.m4s"))
    }
}
