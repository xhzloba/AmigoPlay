package com.example.movie.presentation.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie.R
import com.example.movie.data.history.ResumeStore
import com.example.movie.data.history.ViewHistoryStore
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.progressCaption
import com.example.movie.domain.model.toMovie
import com.example.movie.domain.usecase.GetRecommendationsUseCase
import com.example.movie.domain.usecase.GetShelfMoviesUseCase
import com.example.movie.domain.usecase.GetTopMoviesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeSection(@StringRes val title: Int) {
    Recommendations(R.string.section_recommendations),
    Watching(R.string.section_watching),
    Top(R.string.section_top),
    Best(R.string.section_best),
    History(R.string.section_history),
    Continue(R.string.section_continue),
    ;

    val paged: Boolean
        get() = this == Watching || this == Best
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
    private val getTopMovies: GetTopMoviesUseCase,
    private val viewHistory: ViewHistoryStore,
    private val resume: ResumeStore,
) : ViewModel() {
    private val seen = mutableSetOf<String>()
    private var page = 0
    private var busy = false
    private val _state = MutableStateFlow(
        if (section == HomeSection.History || section == HomeSection.Continue) {
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
        } else {
            loadMore()
        }
    }

    fun clearHistory() {
        viewHistory.clear()
    }

    fun retry() {
        if (_state.value.movies.isEmpty()) {
            page = 0
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
            fetch(next)
                .onSuccess { batch ->
                    val fresh = batch.filter { seen.add(it.id) }
                    if (fresh.isNotEmpty()) page = next
                    _state.value = _state.value.copy(
                        movies = _state.value.movies + fresh,
                        loading = false,
                        loadingMore = false,
                        end = fresh.isEmpty() || !section.paged,
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

    private suspend fun fetch(page: Int) = when (section) {
        HomeSection.Recommendations -> getRecommendations()
        HomeSection.Top -> getTopMovies()
        HomeSection.Watching -> getShelfMovies(CatalogQuery(sort = "watching"), page = page)
        HomeSection.Best -> getShelfMovies(CatalogQuery(sort = "rating", type = "movie"), page = page)
        HomeSection.History, HomeSection.Continue -> Result.success(emptyList())
    }
}
