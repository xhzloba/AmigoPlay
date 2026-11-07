package com.example.movie.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie

@Composable
fun HomeRoute(
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onOpenSection: (HomeSection) -> Unit,
    onPlayResume: (String) -> Unit = {},
    onSearchCover: (Float) -> Unit = {},
    presentSearch: Boolean = true,
    session: Int = 0,
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(
            getHomeShelves = container.getHomeShelves,
            getShelfMovies = container.getShelfMovies,
            getRecommendations = container.getRecommendations,
            getTopMovies = container.getTopMovies,
            searchMovies = container.searchMovies,
            searchHistory = container.searchHistory,
            viewHistory = container.viewHistory,
            resume = container.resume,
            favorites = container.favorites,
            watched = container.watched,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(session) {
        if (session > 0) viewModel.closeSearch()
    }
    HomeScreen(
        state = state,
        onSelectShelf = viewModel::select,
        onMovieClick = onMovieClick,
        onMoviePeek = onMoviePeek,
        onRetry = viewModel::retry,
        onOpenSearch = viewModel::openSearch,
        onSearchQuery = viewModel::search,
        onCloseSearch = viewModel::closeSearch,
        onClearRecent = viewModel::clearRecent,
        onOpenSection = onOpenSection,
        onPlayResume = onPlayResume,
        onRefresh = viewModel::refresh,
        onSearchCover = onSearchCover,
        presentSearch = presentSearch,
    )
}
