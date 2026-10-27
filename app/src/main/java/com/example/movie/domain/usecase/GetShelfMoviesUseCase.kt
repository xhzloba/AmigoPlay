package com.example.movie.domain.usecase

import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.Movie
import com.example.movie.domain.repository.MovieRepository

class GetShelfMoviesUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(query: CatalogQuery, page: Int = 1): Result<List<Movie>> =
        repository.getMovies(query, page)
}
