package com.example.movie.data.update

import com.example.movie.data.remote.GithubApi
import com.example.movie.data.remote.dto.toRelease
import com.example.movie.domain.model.AppRelease
import com.example.movie.domain.model.UpdateProbe
import com.example.movie.domain.repository.AppUpdateRepository
import kotlinx.coroutines.CancellationException

internal class GithubUpdateRepository(
    private val api: GithubApi,
) : AppUpdateRepository {
    override suspend fun newerThan(versionCode: Int): AppRelease? =
        when (val probe = lookup(versionCode)) {
            is UpdateProbe.Available -> probe.release
            UpdateProbe.Current, UpdateProbe.Unavailable -> null
        }

    override suspend fun lookup(versionCode: Int): UpdateProbe = try {
        val dto = api.latest()
        if (dto.draft) {
            UpdateProbe.Unavailable
        } else {
            val release = dto.toRelease(versionCode)
            if (release != null) {
                UpdateProbe.Available(release)
            } else {
                val code = ReleaseVersions.codeOf(dto.tagName) ?: ReleaseVersions.codeOf(dto.name)
                if (code != null && code <= versionCode) UpdateProbe.Current else UpdateProbe.Unavailable
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        UpdateProbe.Unavailable
    }
}
