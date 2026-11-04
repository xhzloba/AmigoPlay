package com.example.movie.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.toSaved

@Composable
fun DetailRoute(
    movieId: String,
    onOpenMovie: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: DetailViewModel = viewModel(key = movieId) {
        DetailViewModel(
            movieId = movieId,
            getMovieDetail = container.getMovieDetail,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail = state.detail
    LaunchedEffect(movieId, detail?.id) {
        if (detail != null && detail.id == movieId) container.viewHistory.remember(detail.toSaved())
    }
    DetailScreen(
        state = state,
        onRetry = viewModel::retry,
        onOpenMovie = onOpenMovie,
        onMoviePeek = onMoviePeek,
    )
}
