package com.example.movie.domain.usecase

import com.example.movie.domain.model.Movie
import com.example.movie.domain.repository.MovieRepository

class SearchMoviesUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(name: String): Result<List<Movie>> = repository.search(name)
}
