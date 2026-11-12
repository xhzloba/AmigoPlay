package com.example.movie.domain.usecase

import com.example.movie.domain.model.MoviePage
import com.example.movie.domain.repository.MovieRepository

class SearchMoviesUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(name: String, page: Int = 1): Result<MoviePage> =
        repository.search(name, page)
}
