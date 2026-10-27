package com.example.movie.presentation.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.movie.R
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.ContentKind
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.Shelf
import com.example.movie.presentation.home.components.CategoryTabs
import com.example.movie.presentation.home.components.CategoryTabsSkeleton
import com.example.movie.presentation.home.components.CoverStackSkeleton
import com.example.movie.presentation.home.components.PosterCarousel
import com.example.movie.presentation.home.components.RecommendationRail
import com.example.movie.presentation.home.components.RecommendationRailSkeleton
import com.example.movie.ui.theme.MovieTheme

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSelectShelf: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background
    if (state.shelves.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            when (val phase = state.phase) {
                HomePhase.Loading -> HomeSkeleton(Modifier.fillMaxSize())
                is HomePhase.Failed -> StatusMessage(
                    message = phase.reason.message(),
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> StatusMessage(
                    message = stringResource(R.string.catalog_empty),
                    onRetry = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 32.dp),
    ) {
        CategoryTabs(
            shelves = state.shelves,
            selectedId = state.selectedId,
            onSelect = onSelectShelf,
        )
        Spacer(Modifier.height(12.dp))
        when {
            state.phase is HomePhase.Loading && state.movies.isEmpty() -> {
                PosterSkeleton(Modifier.fillMaxWidth())
            }
            state.phase is HomePhase.Failed && state.movies.isEmpty() -> {
                StatusMessage(
                    message = state.phase.reason.message(),
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            state.movies.isEmpty() -> {
                StatusMessage(
                    message = stringResource(R.string.catalog_empty),
                    onRetry = null,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            else -> {
                PosterCarousel(
                    movies = state.movies,
                    onMovieClick = onMovieClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (!state.recommendationsReady || state.recommendations.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            RecommendationRail(
                movies = state.recommendations,
                onMovieClick = onMovieClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DataError.message(): String = when (this) {
    is DataError.Network -> stringResource(R.string.popular_network_error)
    is DataError.Http -> stringResource(R.string.popular_http_error, code)
    is DataError.Parsing -> stringResource(R.string.popular_parse_error)
    is DataError.Unknown -> stringResource(R.string.popular_load_error)
}

@Composable
private fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 32.dp)) {
        CategoryTabsSkeleton()
        Spacer(Modifier.height(12.dp))
        CoverStackSkeleton(Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        RecommendationRailSkeleton()
    }
}

@Composable
private fun PosterSkeleton(modifier: Modifier = Modifier) {
    CoverStackSkeleton(modifier)
}

@Composable
private fun StatusMessage(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
            ) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141414, widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    MovieTheme {
        HomeScreen(
            state = HomeUiState(
                shelves = listOf(
                    Shelf("popular:", "В тренде", CatalogQuery("popular")),
                    Shelf("updatings:", "Обновления", CatalogQuery("updatings")),
                    Shelf("popular:movie", "Фильмы", CatalogQuery("popular", "movie")),
                ),
                selectedId = "popular:",
                movies = listOf(previewMovie("1", "Джокер"), previewMovie("2", "Одиссея")),
                recommendations = listOf(previewMovie("3", "Фонари"), previewMovie("4", "Невский")),
                recommendationsReady = true,
                phase = HomePhase.Ready,
            ),
            onSelectShelf = {},
            onMovieClick = {},
            onRetry = {},
        )
    }
}

private fun previewMovie(id: String, title: String) = Movie(
    id = id,
    title = title,
    originalTitle = title,
    year = 2019,
    overview = "",
    genres = "триллер",
    country = "США",
    director = "",
    posterUrl = null,
    backdropUrl = null,
    logoUrl = null,
    ratingKp = null,
    ratingImdb = null,
    age = null,
    kind = ContentKind.Movie,
    camrip = false,
)
