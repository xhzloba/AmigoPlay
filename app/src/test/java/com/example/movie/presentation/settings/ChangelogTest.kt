package com.example.movie.presentation.settings

import com.example.movie.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ChangelogTest {
    @Test
    fun latestReleaseMatchesAppVersion() {
        assertEquals(BuildConfig.VERSION_NAME, Changelog.releases.first().version)
    }
}
