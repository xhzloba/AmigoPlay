package com.example.movie.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movie.di.LocalAppContainer

@Composable
fun DetailRoute(
    movieId: String,
    onOpenMovie: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: DetailViewModel = viewModel(key = movieId) {
        DetailViewModel(
            movieId = movieId,
            getMovieDetail = container.getMovieDetail,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScreen(
        state = state,
        onRetry = viewModel::retry,
        onOpenMovie = onOpenMovie,
    )
}
