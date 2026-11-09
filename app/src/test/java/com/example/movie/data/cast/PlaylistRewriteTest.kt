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
