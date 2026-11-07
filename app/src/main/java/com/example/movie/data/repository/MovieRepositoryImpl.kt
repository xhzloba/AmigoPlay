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
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Franchise
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import com.example.movie.domain.model.Shelf
import com.example.movie.domain.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

internal class MovieRepositoryImpl(
    private val api: VokinoApi,
    private val franchiseApi: FranchiseApi,
    private val token: String,
    private val franchiseToken: String,
) : MovieRepository {
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

    override suspend fun franchise(id: String): Franchise? = loadFranchise(id)

    private suspend fun loadFranchise(id: String): Franchise? {
        if (franchiseToken.isBlank()) return null
        val kp = runCatching { api.timeline(ident = id, token = token).kpId() }.getOrNull() ?: return null
        return runCatching { franchiseApi.details(franchiseToken, kp).toFranchise() }.getOrNull()
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
