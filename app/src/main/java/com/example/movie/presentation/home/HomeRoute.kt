package com.example.movie.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie

@Composable
fun HomeRoute(
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(
            getHomeShelves = container.getHomeShelves,
            getShelfMovies = container.getShelfMovies,
            getRecommendations = container.getRecommendations,
            searchMovies = container.searchMovies,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onSelectShelf = viewModel::select,
        onMovieClick = onMovieClick,
        onMoviePeek = onMoviePeek,
        onRetry = viewModel::retry,
        onOpenSearch = viewModel::openSearch,
        onSearchQuery = viewModel::search,
        onCloseSearch = viewModel::closeSearch,
    )
}
