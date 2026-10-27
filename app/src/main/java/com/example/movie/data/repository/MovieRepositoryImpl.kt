package com.example.movie.data.repository

import com.example.movie.data.mapper.toDomain
import com.example.movie.data.mapper.toMovieDetail
import com.example.movie.data.mapper.toShelves
import com.example.movie.data.remote.VokinoApi
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.DataError
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
    private val token: String,
) : MovieRepository {
    override suspend fun getShelves(): Result<List<Shelf>> = runApi {
        api.main(token).toShelves()
    }

    override suspend fun getMovies(query: CatalogQuery, page: Int): Result<List<Movie>> = runApi {
        api.list(
            sort = query.sort,
            type = query.type,
            page = page,
            token = token,
            uhd = if (query.uhd) 1 else null,
            dolbyVision = if (query.dolbyVision) 1 else null,
        )
            .channels
            .map { it.toDomain() }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .let { movies ->
                if (query.sort == "watching" || query.uhd || query.dolbyVision) movies.take(15) else movies
            }
    }

    override suspend fun getRecommendations(): Result<List<Movie>> = runApi {
        api.recommendations(token)
            .channels
            .map { it.toDomain() }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
    }

    override suspend fun getMovie(id: String): Result<MovieDetail> = runApi {
        api.view(id, token).toMovieDetail()
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
