package com.example.movie.data.history

import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeWriteTest {
    @Test
    fun keepsAStoppedMovie() {
        assertEquals(ResumeWrite.Save, resumeWrite(23 * 60_000L, 120 * 60_000L, sameEpisode = true))
    }

    @Test
    fun ignoresTheOpeningSeconds() {
        assertEquals(ResumeWrite.Ignore, resumeWrite(2_000L, 120 * 60_000L, sameEpisode = true))
    }

    @Test
    fun dropsAFinishedMovie() {
        assertEquals(ResumeWrite.Drop, resumeWrite(118 * 60_000L, 120 * 60_000L, sameEpisode = true))
    }

    @Test
    fun keepsTheNextEpisodeFromTheStart() {
        assertEquals(ResumeWrite.Save, resumeWrite(2_000L, 40 * 60_000L, sameEpisode = false))
    }
}
