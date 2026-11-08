package com.example.movie.data.repository

import com.example.movie.data.mapper.kpId
import com.example.movie.data.mapper.toDomain
import com.example.movie.data.mapper.toFranchise
import com.example.movie.data.mapper.toMovieDetail
import com.example.movie.data.mapper.toShelves
import com.example.movie.data.remote.FranchiseApi
import com.example.movie.data.remote.VokinoApi
import com.example.movie.data.remote.dto.CatalogResponseDto
import com.example.movie.data.remote.dto.ChannelDto
import com.example.movie.data.remote.dto.FranchiseHitDto
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Franchise
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import com.example.movie.domain.model.Shelf
import com.example.movie.domain.model.WatchCheck
import com.example.movie.domain.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class MovieRepositoryImpl(
    private val api: VokinoApi,
    private val franchiseApi: FranchiseApi,
    private val token: String,
    private val franchiseToken: String,
    http: OkHttpClient,
) : MovieRepository {
    private val embed = http.newBuilder()
        .callTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
    override suspend fun getShelves(): Result<List<Shelf>> = runApi {
        api.main(token).toShelves()
    }

    override suspend fun getMovies(query: CatalogQuery, page: Int, limit: Int?): Result<List<Movie>> = runApi {
        api.list(
            sort = query.sort.takeIf { it.isNotBlank() },
            type = query.type,
            page = page,
            token = token,
            uhd = if (query.uhd) 1 else null,
            dolbyVision = if (query.dolbyVision) 1 else null,
            genre = query.genre?.takeIf { it.isNotBlank() },
        )
            .requireChannels()
            .map { it.toDomain() }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .let { movies ->
                val cap = limit ?: if (query.uhd || query.dolbyVision) 15 else null
                if (cap != null) movies.take(cap) else movies
            }
    }

    override suspend fun getRecommendations(): Result<List<Movie>> = runApi {
        api.recommendations(token)
            .requireChannels()
            .map { it.toDomain() }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
    }

    override suspend fun search(name: String): Result<List<Movie>> = runApi {
        api.search(name, token)
            .requireChannels()
            .map { it.toDomain() }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
            .take(40)
    }

    override suspend fun getMovie(id: String): Result<MovieDetail> = runApi {
        api.view(id, token).toMovieDetail()
    }

    override suspend fun franchise(id: String, title: String?, year: Int?): Franchise? =
        when (val check = watchCheck(id, title, year)) {
            is WatchCheck.Ready -> check.franchise
            else -> null
        }

    override suspend fun watchCheck(id: String, title: String?, year: Int?): WatchCheck {
        if (franchiseToken.isBlank()) return WatchCheck.Unknown
        val card = try {
            withTimeout(4_000) { lookupCard(id, title, year) }
        } catch (timeout: TimeoutCancellationException) {
            return WatchCheck.Unknown
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (http: HttpException) {
            return if (http.code() == 404 || http.code() == 410) WatchCheck.Missing else WatchCheck.Unknown
        } catch (_: IOException) {
            return WatchCheck.Unknown
        } catch (_: Exception) {
            return WatchCheck.Unknown
        }
        if (card == null) return WatchCheck.Missing
        val franchiseId = card.id ?: return if (card.playable) WatchCheck.Ready(card) else WatchCheck.Unknown
        val live = embedPlays(franchiseId)
        return when (live) {
            true -> WatchCheck.Ready(card.copy(playable = true))
            false -> WatchCheck.Ready(card.copy(playable = false))
            null -> if (card.playable) WatchCheck.Ready(card) else WatchCheck.Unknown
        }
    }

    private suspend fun lookupCard(id: String, title: String?, year: Int?): Franchise? {
        val byKp = cardByKp(id)
        if (byKp != null) return byKp
        return cardByTitle(title, year)
    }

    private suspend fun cardByKp(id: String): Franchise? {
        val kp = try {
            api.timeline(ident = id, token = token).kpId()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: return null
        return try {
            franchiseApi.details(franchiseToken, kp).toFranchise()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (http: HttpException) {
            if (http.code() == 404 || http.code() == 410) null else throw http
        }
    }

    private suspend fun cardByTitle(title: String?, year: Int?): Franchise? {
        val name = title?.trim().orEmpty()
        if (name.length < 3) return null
        val hit = try {
            franchiseApi.list(franchiseToken, name).results.pick(name, year)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (http: HttpException) {
            if (http.code() == 404 || http.code() == 410) return null else throw http
        } ?: return null
        val franchiseId = hit.id ?: return null
        return try {
            franchiseApi.detailsById(franchiseToken, franchiseId).toFranchise()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (http: HttpException) {
            if (http.code() == 404 || http.code() == 410) null else throw http
        }
    }

    private suspend fun embedPlays(id: Int): Boolean? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://embed.liftw.ws/embed/movie/$id")
            .header("User-Agent", "AmigoPlay")
            .build()
        try {
            embed.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> true
                    404, 410 -> false
                    else -> null
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun CatalogResponseDto.requireChannels(): List<ChannelDto> {
        if (error != null) throw IllegalStateException("catalog $error")
        return channels
    }

    private inline fun <T> runApi(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            Result.failure(DataError.Network())
        } catch (http: HttpException) {
            Result.failure(DataError.Http(http.code()))
        } catch (_: SerializationException) {
            Result.failure(DataError.Parsing())
        } catch (_: Exception) {
            Result.failure(DataError.Unknown())
        }
}

private fun List<FranchiseHitDto>.pick(title: String, year: Int?): FranchiseHitDto? {
    val wanted = title.folded()
    if (wanted.isEmpty()) return null
    val named = filter { it.name.folded() == wanted || it.originName.folded() == wanted }
    if (named.isEmpty()) return null
    if (year != null) {
        named.firstOrNull { it.year == year }?.let { return it }
        if (named.any { it.year != null && it.year != year }) return null
    }
    return named.singleOrNull()
}

private fun String?.folded(): String = orEmpty()
    .lowercase()
    .replace('ё', 'е')
    .filter { it.isLetterOrDigit() }
