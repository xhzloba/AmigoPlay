package com.example.movie.data.update

import com.example.movie.data.remote.GithubApi
import com.example.movie.data.remote.dto.toProbe
import com.example.movie.domain.model.AppRelease
import com.example.movie.domain.model.UpdateProbe
import com.example.movie.domain.repository.AppUpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

internal class GithubUpdateRepository(
    private val api: GithubApi,
) : AppUpdateRepository {
    override suspend fun newerThan(versionCode: Int): AppRelease? =
        when (val probe = lookup(versionCode)) {
            is UpdateProbe.Available -> probe.release
            UpdateProbe.Current, UpdateProbe.Unavailable -> null
        }

    override suspend fun lookup(versionCode: Int): UpdateProbe {
        repeat(ATTEMPTS) { attempt ->
            try {
                return load().toProbe(versionCode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (attempt == ATTEMPTS - 1 || !error.transient()) return UpdateProbe.Unavailable
                delay(400L shl attempt)
            }
        }
        return UpdateProbe.Unavailable
    }

    private suspend fun load() = try {
        api.releases()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        if (!error.transient()) throw error
        listOf(api.latest())
    }

    private fun Throwable.transient(): Boolean {
        if (this is IOException) return true
        if (this is HttpException) {
            val code = code()
            return code == 408 || code == 429 || code in 500..599
        }
        return false
    }

    private companion object {
        const val ATTEMPTS = 4
    }
}
