package com.example.movie.data.update

import com.example.movie.data.remote.dto.GithubAssetDto
import com.example.movie.data.remote.dto.GithubReleaseDto
import com.example.movie.data.remote.dto.toProbe
import com.example.movie.data.remote.dto.toRelease
import com.example.movie.domain.model.UpdateProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubReleaseParserTest {
    @Test
    fun versionCodeFollowsTheScheme() {
        assertEquals(10602, ReleaseVersions.codeOf("v1.6.2"))
        assertEquals(10700, ReleaseVersions.codeOf("1.7.0"))
        assertNull(ReleaseVersions.codeOf("latest"))
    }

    @Test
    fun newerReleaseWithApkIsOffered() {
        val release = GithubReleaseDto(
            tagName = "v1.8.0",
            body = "Новый каталог",
            assets = listOf(GithubAssetDto(name = "app.apk", browserDownloadUrl = "https://example.com/app.apk")),
        ).toRelease(currentCode = 10700)
        assertEquals("1.8.0", release?.versionName)
        assertEquals(10800, release?.versionCode)
        assertEquals("Новый каталог", release?.notes)
        assertEquals("https://example.com/app.apk", release?.apkUrl)
    }

    @Test
    fun sameVersionOrMissingApkIsIgnored() {
        val same = GithubReleaseDto(
            tagName = "1.7.0",
            assets = listOf(GithubAssetDto(name = "a.apk", browserDownloadUrl = "https://example.com/a.apk")),
        )
        assertNull(same.toRelease(10700))
        val noApk = GithubReleaseDto(tagName = "v9.0.0")
        assertNull(noApk.toRelease(1))
        val draft = GithubReleaseDto(
            tagName = "v9.0.0",
            draft = true,
            assets = listOf(GithubAssetDto(name = "a.apk", browserDownloadUrl = "https://example.com/a.apk")),
        )
        assertNull(draft.toRelease(1))
    }

    @Test
    fun highestPublishedReleaseWinsOverAnOlderLatestBadge() {
        val older = GithubReleaseDto(
            tagName = "v2.2.1",
            assets = listOf(GithubAssetDto(name = "a.apk", browserDownloadUrl = "https://example.com/221.apk")),
        )
        val newer = GithubReleaseDto(
            tagName = "v2.3.0",
            assets = listOf(GithubAssetDto(name = "a.apk", browserDownloadUrl = "https://example.com/230.apk")),
        )
        val probe = listOf(older, newer).toProbe(20202)
        assertTrue(probe is UpdateProbe.Available)
        assertEquals("2.3.0", (probe as UpdateProbe.Available).release.versionName)
        assertEquals(20300, probe.release.versionCode)
    }
}
