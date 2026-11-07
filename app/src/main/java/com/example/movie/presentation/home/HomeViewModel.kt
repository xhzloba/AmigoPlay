package com.example.movie.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.domain.usecase.GetHomeShelvesUseCase
import com.example.movie.domain.usecase.GetRecommendationsUseCase
import com.example.movie.domain.usecase.GetShelfMoviesUseCase
import com.example.movie.domain.usecase.GetTopMoviesUseCase
import com.example.movie.data.favorites.FavoriteStore
import com.example.movie.data.favorites.WatchedStore
import com.example.movie.data.history.ResumeStore
import com.example.movie.data.history.ViewHistoryStore
import com.example.movie.data.search.SearchHistoryStore
import com.example.movie.domain.model.toMovie
import com.example.movie.domain.usecase.SearchMoviesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getHomeShelves: GetHomeShelvesUseCase,
    private val getShelfMovies: GetShelfMoviesUseCase,
    private val getRecommendations: GetRecommendationsUseCase,
    private val getTopMovies: GetTopMoviesUseCase,
    private val searchMovies: SearchMoviesUseCase,
    private val searchHistory: SearchHistoryStore,
    private val viewHistory: ViewHistoryStore,
    private val resume: ResumeStore,
    private val favorites: FavoriteStore,
    private val watched: WatchedStore,
) : ViewModel() {
    private val cache = mutableMapOf<String, List<Movie>>()
    private var moviesJob: Job? = null
    private var searchJob: Job? = null
    private val _state = MutableStateFlow(HomeUiState(recentQueries = searchHistory.items()))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            viewHistory.items.collect { saved ->
                _state.value = _state.value.copy(history = saved.map { it.toMovie() })
            }
        }
        viewModelScope.launch {
            resume.items.collect { saved ->
                _state.value = _state.value.copy(resume = saved)
            }
        }
        viewModelScope.launch {
            favorites.items.collect { saved ->
                _state.value = _state.value.copy(favorites = saved.map { it.toMovie() })
            }
        }
        viewModelScope.launch {
            watched.items.collect { saved ->
                _state.value = _state.value.copy(watched = saved.map { it.toMovie() })
            }
        }
        loadShelves()
        loadRecommendations()
        loadWatching()
        loadTop()
        loadBest()
    }

    fun select(id: String) {
        if (_state.value.shelves.none { it.id == id }) return
        val phase = _state.value.phase
        val stuck = phase is HomePhase.Failed || phase is HomePhase.Empty
        if (id == _state.value.selectedId && !stuck) return
        if (stuck) cache.remove(id)
        showShelf(id)
    }

    fun openSearch() {
        searchJob?.cancel()
        _state.value = _state.value.copy(
            searchOpen = true,
            searchQuery = "",
            searchResults = emptyList(),
            searching = false,
            searchError = null,
            recentQueries = searchHistory.items(),
        )
    }

    fun clearRecent() {
        searchHistory.clear()
        _state.value = _state.value.copy(recentQueries = emptyList())
    }

    fun closeSearch() {
        searchJob?.cancel()
        _state.value = _state.value.copy(
            searchOpen = false,
            searchQuery = "",
            searchResults = emptyList(),
            searching = false,
            searchError = null,
        )
    }

    fun search(raw: String) {
        _state.value = _state.value.copy(searchQuery = raw, searchError = null)
        searchJob?.cancel()
        val query = raw.trim()
        if (query.length < 2) {
            _state.value = _state.value.copy(searchResults = emptyList(), searching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(280)
            _state.value = _state.value.copy(searching = true, searchError = null)
            searchMovies(query)
                .onSuccess { movies ->
                    if (_state.value.searchQuery.trim() != query) return@onSuccess
                    searchHistory.remember(query)
                    _state.value = _state.value.copy(
                        searchResults = movies,
                        searching = false,
                        recentQueries = searchHistory.items(),
                    )
                }
                .onFailure { error ->
                    if (_state.value.searchQuery.trim() != query) return@onFailure
                    _state.value = _state.value.copy(
                        searchResults = emptyList(),
                        searching = false,
                        searchError = error.asDataError(),
                    )
                }
        }
    }

    fun retry() {
        if (!_state.value.recommendationsReady || _state.value.recommendations.isEmpty()) {
            loadRecommendations()
        }
        if (!_state.value.watchingReady || _state.value.watching.isEmpty()) {
            loadWatching()
        }
        if (!_state.value.topReady || _state.value.top.isEmpty()) {
            loadTop()
        }
        if (!_state.value.bestReady || _state.value.best.isEmpty()) {
            loadBest()
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

    private fun loadWatching() {
        viewModelScope.launch {
            if (_state.value.watching.isEmpty()) {
                _state.value = _state.value.copy(watchingReady = false)
            }
            getShelfMovies(CatalogQuery(sort = "watching"), limit = 15)
                .onSuccess { movies ->
                    _state.value = _state.value.copy(
                        watching = movies,
                        watchingReady = true,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        watching = emptyList(),
                        watchingReady = true,
                    )
                }
        }
    }

    private fun loadTop() {
        viewModelScope.launch {
            if (_state.value.top.isEmpty()) {
                _state.value = _state.value.copy(topReady = false)
            }
            getTopMovies()
                .onSuccess { movies ->
                    _state.value = _state.value.copy(top = movies, topReady = true)
                }
                .onFailure {
                    _state.value = _state.value.copy(top = emptyList(), topReady = true)
                }
        }
    }

    private fun loadBest() {
        viewModelScope.launch {
            if (_state.value.best.isEmpty()) {
                _state.value = _state.value.copy(bestReady = false)
            }
            getShelfMovies(CatalogQuery(sort = "rating", type = "movie"))
                .onSuccess { movies ->
                    _state.value = _state.value.copy(best = movies.take(15), bestReady = true)
                }
                .onFailure {
                    _state.value = _state.value.copy(best = emptyList(), bestReady = true)
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
            var result = getShelfMovies(shelf.query)
            if (result.isFailure) {
                delay(600)
                if (!isActive || _state.value.selectedId != id) return@launch
                result = getShelfMovies(shelf.query)
            }
            result
                .onSuccess { movies ->
                    if (movies.isNotEmpty()) cache[id] = movies
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
