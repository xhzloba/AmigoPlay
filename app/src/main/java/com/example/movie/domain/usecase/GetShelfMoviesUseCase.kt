package com.example.movie.domain.usecase

import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MoviePage
import com.example.movie.domain.repository.MovieRepository

class GetShelfMoviesUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(query: CatalogQuery, page: Int = 1, limit: Int? = null): Result<List<Movie>> =
        repository.getMovies(query, page, limit)

    suspend fun paged(query: CatalogQuery, page: Int = 1, limit: Int? = null): Result<MoviePage> =
        repository.getMoviePage(query, page, limit)
}
