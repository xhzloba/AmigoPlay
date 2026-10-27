package com.example.movie.domain.usecase

import com.example.movie.domain.model.Shelf
import com.example.movie.domain.repository.MovieRepository

class GetHomeShelvesUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(): Result<List<Shelf>> = repository.getShelves()
}
