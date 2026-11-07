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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        viewModelScope.launch { loadShelves() }
        viewModelScope.launch { loadRecommendations(keep = false) }
        viewModelScope.launch { loadWatching(keep = false) }
        viewModelScope.launch { loadTop(keep = false) }
        viewModelScope.launch { loadBest(keep = false) }
    }

    fun refresh() {
        if (_state.value.refreshing) return
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            moviesJob?.cancel()
            try {
                coroutineScope {
                    launch { loadRecommendations(keep = true) }
                    launch { loadWatching(keep = true) }
                    launch { loadTop(keep = true) }
                    launch { loadBest(keep = true) }
                    launch {
                        if (_state.value.shelves.isEmpty() || _state.value.selectedId == null) {
                            loadShelves()
                        } else {
                            reloadSelectedShelf()
                        }
                    }
                }
            } finally {
                _state.update { it.copy(refreshing = false) }
            }
        }
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
        viewModelScope.launch {
            if (!_state.value.recommendationsReady || _state.value.recommendations.isEmpty()) {
                loadRecommendations(keep = false)
            }
            if (!_state.value.watchingReady || _state.value.watching.isEmpty()) {
                loadWatching(keep = false)
            }
            if (!_state.value.topReady || _state.value.top.isEmpty()) {
                loadTop(keep = false)
            }
            if (!_state.value.bestReady || _state.value.best.isEmpty()) {
                loadBest(keep = false)
            }
            val selectedId = _state.value.selectedId
            if (_state.value.shelves.isEmpty() || selectedId == null) {
                loadShelves()
            } else {
                cache.remove(selectedId)
                showShelf(selectedId)
            }
        }
    }

    private suspend fun loadRecommendations(keep: Boolean) {
        if (!keep && _state.value.recommendations.isEmpty()) {
            _state.update { it.copy(recommendationsReady = false) }
        }
        getRecommendations()
            .onSuccess { movies ->
                _state.update { it.copy(recommendations = movies, recommendationsReady = true) }
            }
            .onFailure {
                if (keep && _state.value.recommendations.isNotEmpty()) return@onFailure
                _state.update { it.copy(recommendations = emptyList(), recommendationsReady = true) }
            }
    }

    private suspend fun loadWatching(keep: Boolean) {
        if (!keep && _state.value.watching.isEmpty()) {
            _state.update { it.copy(watchingReady = false) }
        }
        getShelfMovies(CatalogQuery(sort = "watching"), limit = 15)
            .onSuccess { movies ->
                _state.update { it.copy(watching = movies, watchingReady = true) }
            }
            .onFailure {
                if (keep && _state.value.watching.isNotEmpty()) return@onFailure
                _state.update { it.copy(watching = emptyList(), watchingReady = true) }
            }
    }

    private suspend fun loadTop(keep: Boolean) {
        if (!keep && _state.value.top.isEmpty()) {
            _state.update { it.copy(topReady = false) }
        }
        getTopMovies()
            .onSuccess { movies ->
                _state.update { it.copy(top = movies, topReady = true) }
            }
            .onFailure {
                if (keep && _state.value.top.isNotEmpty()) return@onFailure
                _state.update { it.copy(top = emptyList(), topReady = true) }
            }
    }

    private suspend fun loadBest(keep: Boolean) {
        if (!keep && _state.value.best.isEmpty()) {
            _state.update { it.copy(bestReady = false) }
        }
        getShelfMovies(CatalogQuery(sort = "rating", type = "movie"))
            .onSuccess { movies ->
                _state.update { it.copy(best = movies.take(15), bestReady = true) }
            }
            .onFailure {
                if (keep && _state.value.best.isNotEmpty()) return@onFailure
                _state.update { it.copy(best = emptyList(), bestReady = true) }
            }
    }

    private suspend fun reloadSelectedShelf() {
        val id = _state.value.selectedId ?: return
        val shelf = _state.value.shelves.firstOrNull { it.id == id } ?: return
        cache.remove(id)
        var result = getShelfMovies(shelf.query)
        if (result.isFailure) {
            delay(600)
            if (_state.value.selectedId != id) return
            result = getShelfMovies(shelf.query)
        }
        result.onSuccess { movies ->
            if (movies.isNotEmpty()) cache[id] = movies
            if (_state.value.selectedId != id) return@onSuccess
            _state.update {
                it.copy(
                    movies = movies,
                    phase = if (movies.isEmpty()) HomePhase.Empty else HomePhase.Ready,
                )
            }
        }
    }

    private suspend fun loadShelves() {
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
