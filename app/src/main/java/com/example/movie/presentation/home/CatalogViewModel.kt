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
)

internal class CatalogViewModel(
    private val type: String,
    private val getShelfMovies: GetShelfMoviesUseCase,
) : ViewModel() {
    private val sorts = CatalogSort.forType(type)
    private val _state = MutableStateFlow(CatalogUiState(sorts.map { CatalogRail(it) }))
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        _state.value = CatalogUiState(sorts.map { CatalogRail(it) })
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            sorts.map { sort ->
                async {
                    val result = getShelfMovies(CatalogQuery(sort = sort.sort, type = type))
                    _state.update { current ->
                        current.copy(
                            shelves = current.shelves.map { rail ->
                                if (rail.sort != sort) {
                                    rail
                                } else {
                                    rail.copy(
                                        movies = result.getOrDefault(emptyList()),
                                        ready = true,
                                    )
                                }
                            },
                        )
                    }
                }
            }.awaitAll()
        }
    }
}

internal class CatalogListViewModel(
    private val query: CatalogQuery,
    private val getShelfMovies: GetShelfMoviesUseCase,
) : ViewModel() {
    private val seen = mutableSetOf<String>()
    private var page = 0
    private var busy = false
    private val _state = MutableStateFlow(SectionUiState())
    val state: StateFlow<SectionUiState> = _state.asStateFlow()

    init {
        loadMore()
    }

    fun retry() {
        if (_state.value.movies.isEmpty()) {
            page = 0
            seen.clear()
            _state.value = SectionUiState()
        }
        loadMore(force = true)
    }

    fun loadMore(force: Boolean = false) {
        val current = _state.value
        if (busy || current.end) return
        if (!force && current.error != null) return
        busy = true
        val next = page + 1
        val first = current.movies.isEmpty()
        _state.value = current.copy(loading = first, loadingMore = !first, error = null)
        viewModelScope.launch {
            getShelfMovies(query, page = next)
                .onSuccess { batch ->
                    val fresh = batch.filter { seen.add(it.id) }
                    if (fresh.isNotEmpty()) page = next
                    _state.value = _state.value.copy(
                        movies = _state.value.movies + fresh,
                        loading = false,
                        loadingMore = false,
                        end = fresh.isEmpty(),
                        error = null,
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        loadingMore = false,
                        error = error as? DataError ?: DataError.Unknown(),
                    )
                }
            busy = false
        }
    }
}
