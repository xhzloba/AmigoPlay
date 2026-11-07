package com.example.movie.data.download

import org.junit.Assert.assertEquals
import org.junit.Test

class ManifestQualitiesTest {
    @Test
    fun hlsHeightsComeFromResolution() {
        val manifest = """
            #EXT-X-STREAM-INF:BANDWIDTH=900000,RESOLUTION=640x360
            low.m3u8
            #EXT-X-STREAM-INF:RESOLUTION=1920x1080,BANDWIDTH=5000000
            high.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
            mid.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
            mid-again.m3u8
        """.trimIndent()

        assertEquals(listOf(1080, 720, 360), videoHeights(manifest))
    }

    @Test
    fun dashHeightsComeFromRepresentation() {
        val manifest = """
            <Representation width="1920" height="1080" bandwidth="5000000"/>
            <Representation height='720' width='1280'/>
            <Representation height="1080" width="1920"/>
        """.trimIndent()

        assertEquals(listOf(1080, 720), videoHeights(manifest))
    }

    @Test
    fun hlsKeysSkipFailoverAndPickAudio() {
        val manifest = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio0",NAME="rus0",URI="a0.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio0",NAME="rus1",URI="a1.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="failover-audio-0",NAME="rus0",URI="f0.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="failover-audio-0",NAME="rus1",URI="f1.m3u8"
            #EXT-X-STREAM-INF:RESOLUTION=1272x530,AUDIO="audio0"
            v530.m3u8
            #EXT-X-STREAM-INF:RESOLUTION=1272x530,AUDIO="failover-audio-0"
            v530f.m3u8
            #EXT-X-STREAM-INF:RESOLUTION=648x270,AUDIO="audio0"
            v270.m3u8
            #EXT-X-STREAM-INF:RESOLUTION=648x270,AUDIO="failover-audio-0"
            v270f.m3u8
        """.trimIndent()

        assertEquals(listOf(HlsStreamKey(0, 0), HlsStreamKey(1, 1)), hlsStreamKeys(manifest, 530, 1))
        assertEquals(listOf(HlsStreamKey(0, 2), HlsStreamKey(1, 0)), hlsStreamKeys(manifest, 270, 0))
        assertEquals(listOf(HlsStreamKey(0, 0), HlsStreamKey(1, 0)), hlsStreamKeys(manifest, 0, -1))
    }
}
