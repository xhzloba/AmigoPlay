package com.example.movie.domain.usecase

import com.example.movie.domain.model.Movie
import com.example.movie.domain.repository.MovieRepository

class GetRecommendationsUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(): Result<List<Movie>> = repository.getRecommendations()
}
