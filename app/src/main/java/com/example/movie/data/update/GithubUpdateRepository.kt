package com.example.movie.data.update

import com.example.movie.data.remote.GithubApi
import com.example.movie.data.remote.dto.toRelease
import com.example.movie.domain.model.AppRelease
import com.example.movie.domain.repository.AppUpdateRepository
import kotlinx.coroutines.CancellationException

internal class GithubUpdateRepository(
    private val api: GithubApi,
) : AppUpdateRepository {
    override suspend fun newerThan(versionCode: Int): AppRelease? = try {
        api.latest().toRelease(versionCode)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}
