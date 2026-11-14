package com.example.movie.presentation.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie.R
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.domain.usecase.GetShelfMoviesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class CatalogSort(val sort: String, @StringRes val title: Int) {
    Watching("watching", R.string.section_watching),
    Popular("popular", R.string.catalog_popular),
    Fresh("new", R.string.catalog_new),
    Updates("updatings", R.string.catalog_updates),
    Best("rating", R.string.section_best),
    ;

    companion object {
        fun forType(type: String): List<CatalogSort> =
            entries.filter { it != Watching || type == "movie" }
    }
}

internal data class CatalogRail(
    val sort: CatalogSort,
    val movies: List<Movie> = emptyList(),
    val ready: Boolean = false,
)

internal data class CatalogUiState(
    val shelves: List<CatalogRail> = emptyList(),
    val refreshing: Boolean = false,
)

internal class CatalogViewModel(
    private val type: String,
    private val getShelfMovies: GetShelfMoviesUseCase,
) : ViewModel() {
    private val sorts = CatalogSort.forType(type)
    private val _state = MutableStateFlow(CatalogUiState(sorts.map { CatalogRail(it) }))
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()
    private var loadJob: Job? = null
    private var started = false

    fun ensureLoaded() {
        if (started) return
        started = true
        load(keep = false)
    }

    fun retry() {
        started = true
        _state.value = CatalogUiState(sorts.map { CatalogRail(it) })
        load(keep = false)
    }

    fun refresh() {
        if (_state.value.refreshing) return
        load(keep = true)
    }

    private fun load(keep: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (keep) _state.update { it.copy(refreshing = true) }
            try {
                sorts.map { sort ->
                    async {
                        val result = getShelfMovies(CatalogQuery(sort = sort.sort, type = type))
                        _state.update { current ->
                            current.copy(
                                shelves = current.shelves.map { rail ->
                                    if (rail.sort != sort) {
                                        rail
                                    } else {
                                        val movies = if (result.isSuccess) {
                                            result.getOrDefault(emptyList())
                                        } else if (keep) {
                                            rail.movies
                                        } else {
                                            emptyList()
                                        }
                                        rail.copy(movies = movies, ready = true)
                                    }
                                },
                            )
                        }
                    }
                }.awaitAll()
            } finally {
                if (keep) _state.update { it.copy(refreshing = false) }
            }
        }
    }
}

internal class CatalogListViewModel(
    private val query: CatalogQuery,
    private val getShelfMovies: GetShelfMoviesUseCase,
) : ViewModel() {
    private val pages = ShelfPages()
    private var busy = false
    private val _state = MutableStateFlow(SectionUiState())
    val state: StateFlow<SectionUiState> = _state.asStateFlow()

    init {
        loadMore()
    }

    fun retry() {
        if (_state.value.movies.isEmpty()) {
            pages.reset()
            _state.value = SectionUiState()
        }
        loadMore(force = true)
    }

    fun loadMore(force: Boolean = false) {
        val current = _state.value
        if (busy || current.end) return
        if (!force && current.error != null) return
        busy = true
        val first = current.movies.isEmpty()
        _state.value = current.copy(loading = first, loadingMore = !first, error = null)
        viewModelScope.launch {
            try {
                var skips = 0
                while (true) {
                    var stop = true
                    getShelfMovies.paged(query, page = pages.page + 1)
                        .onSuccess { batch ->
                            val take = pages.advance(batch.movies, hasMore = batch.hasMore, paged = true)
                            when {
                                take.again && skips + 1 >= ShelfOverlapSkips -> {
                                    skips++
                                    _state.value = _state.value.copy(
                                        loading = false,
                                        loadingMore = false,
                                        end = true,
                                        error = null,
                                    )
                                }
                                take.again -> {
                                    skips++
                                    stop = false
                                    _state.value = _state.value.copy(
                                        loading = false,
                                        loadingMore = true,
                                        end = false,
                                        error = null,
                                    )
                                }
                                else -> _state.value = _state.value.copy(
                                    movies = _state.value.movies + take.fresh,
                                    loading = false,
                                    loadingMore = false,
                                    end = take.end,
                                    error = null,
                                )
                            }
                        }
                        .onFailure { error ->
                            _state.value = _state.value.copy(
                                loading = false,
                                loadingMore = false,
                                error = error as? DataError ?: DataError.Unknown(),
                            )
                        }
                    if (stop) break
                }
            } finally {
                busy = false
            }
        }
    }
}
