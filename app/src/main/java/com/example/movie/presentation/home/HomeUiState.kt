package com.example.movie.presentation.home

import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.Shelf

data class HomeUiState(
    val shelves: List<Shelf> = emptyList(),
    val selectedId: String? = null,
    val movies: List<Movie> = emptyList(),
    val recommendations: List<Movie> = emptyList(),
    val recommendationsReady: Boolean = false,
    val best: List<Movie> = emptyList(),
    val bestReady: Boolean = false,
    val phase: HomePhase = HomePhase.Loading,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Movie> = emptyList(),
    val searching: Boolean = false,
    val searchError: DataError? = null,
)

sealed interface HomePhase {
    data object Loading : HomePhase
    data object Ready : HomePhase
    data object Empty : HomePhase
    data class Failed(val reason: DataError) : HomePhase
}
