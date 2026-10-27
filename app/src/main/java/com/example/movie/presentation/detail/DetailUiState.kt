package com.example.movie.presentation.detail

import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.MovieDetail

sealed interface DetailPhase {
    data object Loading : DetailPhase
    data object Ready : DetailPhase
    data class Failed(val reason: DataError) : DetailPhase
}

data class DetailUiState(
    val detail: MovieDetail? = null,
    val phase: DetailPhase = DetailPhase.Loading,
)
