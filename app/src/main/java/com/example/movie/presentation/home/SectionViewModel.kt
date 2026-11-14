package com.example.movie.presentation.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie.R
import com.example.movie.data.favorites.FavoriteStore
import com.example.movie.data.favorites.WatchedStore
import com.example.movie.data.history.ResumeStore
import com.example.movie.data.history.ViewHistoryStore
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MoviePage
import com.example.movie.domain.model.progressCaption
import com.example.movie.domain.model.toMovie
import com.example.movie.domain.usecase.GetRecommendationsUseCase
import com.example.movie.domain.usecase.GetShelfMoviesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeSection(@StringRes val title: Int) {
    Recommendations(R.string.section_recommendations),
    Watching(R.string.section_watching),
    Best(R.string.section_best),
    History(R.string.section_history),
    Continue(R.string.section_continue),
    Favorites(R.string.section_favorites),
    Watched(R.string.section_watched),
    ;

    val paged: Boolean
        get() = this == Watching || this == Best

    val stored: Boolean
        get() = this == History || this == Continue || this == Favorites || this == Watched
}

data class PosterMark(val fraction: Float, val caption: String)

data class SectionUiState(
    val movies: List<Movie> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val end: Boolean = false,
    val error: DataError? = null,
    val marks: Map<String, PosterMark> = emptyMap(),
)

class SectionViewModel(
    private val section: HomeSection,
    private val getShelfMovies: GetShelfMoviesUseCase,
    private val getRecommendations: GetRecommendationsUseCase,
    private val viewHistory: ViewHistoryStore,
    private val resume: ResumeStore,
    private val favorites: FavoriteStore,
    private val watched: WatchedStore,
) : ViewModel() {
    private val pages = ShelfPages()
    private var busy = false
    private val _state = MutableStateFlow(
        if (section.stored) {
            SectionUiState(loading = false, end = true)
        } else {
            SectionUiState()
        },
    )
    val state: StateFlow<SectionUiState> = _state.asStateFlow()

    init {
        if (section == HomeSection.History) {
            viewModelScope.launch {
                viewHistory.items.collect { saved ->
                    _state.value = SectionUiState(
                        movies = saved.map { it.toMovie() },
                        loading = false,
                        end = true,
                    )
                }
            }
        } else if (section == HomeSection.Continue) {
            viewModelScope.launch {
                resume.items.collect { saved ->
                    _state.value = SectionUiState(
                        movies = saved.map { it.toMovie() },
                        loading = false,
                        end = true,
                        marks = saved.associate { item ->
                            item.id to PosterMark(item.fraction, item.progressCaption())
                        },
                    )
                }
            }
        } else if (section == HomeSection.Favorites) {
            viewModelScope.launch {
                favorites.items.collect { saved ->
                    _state.value = SectionUiState(
                        movies = saved.map { it.toMovie() },
                        loading = false,
                        end = true,
                    )
                }
            }
        } else if (section == HomeSection.Watched) {
            viewModelScope.launch {
                watched.items.collect { saved ->
                    _state.value = SectionUiState(
                        movies = saved.map { it.toMovie() },
                        loading = false,
                        end = true,
                    )
                }
            }
        } else {
            loadMore()
        }
    }

    fun clearHistory() {
        viewHistory.clear()
    }

    fun retry() {
        if (_state.value.movies.isEmpty()) {
            pages.rewind()
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
                    fetch(pages.page + 1)
                        .onSuccess { batch ->
                            val take = pages.advance(batch.movies, hasMore = batch.hasMore, paged = section.paged)
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

    private suspend fun fetch(page: Int): Result<MoviePage> = when (section) {
        HomeSection.Recommendations -> getRecommendations().map { MoviePage(it, hasMore = false) }
        HomeSection.Watching -> getShelfMovies.paged(CatalogQuery(sort = "watching"), page = page)
        HomeSection.Best -> getShelfMovies.paged(CatalogQuery(sort = "rating", type = "movie"), page = page)
        HomeSection.History, HomeSection.Continue, HomeSection.Favorites, HomeSection.Watched ->
            Result.success(MoviePage(emptyList(), hasMore = false))
    }
}
