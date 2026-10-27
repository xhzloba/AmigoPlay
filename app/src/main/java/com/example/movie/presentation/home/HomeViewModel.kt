package com.example.movie.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.domain.usecase.GetHomeShelvesUseCase
import com.example.movie.domain.usecase.GetRecommendationsUseCase
import com.example.movie.domain.usecase.GetShelfMoviesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getHomeShelves: GetHomeShelvesUseCase,
    private val getShelfMovies: GetShelfMoviesUseCase,
    private val getRecommendations: GetRecommendationsUseCase,
) : ViewModel() {
    private val cache = mutableMapOf<String, List<Movie>>()
    private var moviesJob: Job? = null
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadShelves()
        loadRecommendations()
    }

    fun select(id: String) {
        if (_state.value.shelves.none { it.id == id }) return
        if (id == _state.value.selectedId && _state.value.phase !is HomePhase.Failed) return
        showShelf(id)
    }

    fun retry() {
        if (!_state.value.recommendationsReady || _state.value.recommendations.isEmpty()) {
            loadRecommendations()
        }
        val selectedId = _state.value.selectedId
        if (_state.value.shelves.isEmpty() || selectedId == null) {
            loadShelves()
        } else {
            cache.remove(selectedId)
            showShelf(selectedId)
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            if (_state.value.recommendations.isEmpty()) {
                _state.value = _state.value.copy(recommendationsReady = false)
            }
            getRecommendations()
                .onSuccess { movies ->
                    _state.value = _state.value.copy(
                        recommendations = movies,
                        recommendationsReady = true,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        recommendations = emptyList(),
                        recommendationsReady = true,
                    )
                }
        }
    }

    private fun loadShelves() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                shelves = emptyList(),
                selectedId = null,
                movies = emptyList(),
                phase = HomePhase.Loading,
            )
            getHomeShelves()
                .onSuccess { shelves ->
                    _state.value = _state.value.copy(shelves = shelves)
                    val first = shelves.firstOrNull()
                    if (first == null) {
                        _state.value = _state.value.copy(phase = HomePhase.Empty)
                    } else {
                        showShelf(first.id)
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(phase = HomePhase.Failed(error.asDataError()))
                }
        }
    }

    private fun showShelf(id: String) {
        val shelf = _state.value.shelves.firstOrNull { it.id == id } ?: return
        cache[id]?.let { cached ->
            moviesJob?.cancel()
            _state.value = _state.value.copy(
                selectedId = id,
                movies = cached,
                phase = if (cached.isEmpty()) HomePhase.Empty else HomePhase.Ready,
            )
            return
        }
        moviesJob?.cancel()
        _state.value = _state.value.copy(
            selectedId = id,
            movies = emptyList(),
            phase = HomePhase.Loading,
        )
        moviesJob = viewModelScope.launch {
            getShelfMovies(shelf.query)
                .onSuccess { movies ->
                    cache[id] = movies
                    if (_state.value.selectedId != id) return@onSuccess
                    _state.value = _state.value.copy(
                        movies = movies,
                        phase = if (movies.isEmpty()) HomePhase.Empty else HomePhase.Ready,
                    )
                }
                .onFailure { error ->
                    if (_state.value.selectedId != id) return@onFailure
                    _state.value = _state.value.copy(
                        movies = emptyList(),
                        phase = HomePhase.Failed(error.asDataError()),
                    )
                }
        }
    }
}

private fun Throwable.asDataError(): DataError = this as? DataError ?: DataError.Unknown()
