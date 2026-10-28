package com.example.movie.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.movie.R
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.ContentKind
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.Shelf
import com.example.movie.presentation.home.components.CatalogPlaque
import com.example.movie.presentation.home.components.CategoryTabs
import com.example.movie.presentation.home.components.CategoryTabsSkeleton
import com.example.movie.presentation.home.components.CoverStackSkeleton
import com.example.movie.presentation.home.components.LogoMood
import com.example.movie.presentation.home.components.PosterCarousel
import com.example.movie.presentation.home.components.RecommendationRail
import com.example.movie.ui.theme.MovieTheme
import com.example.movie.ui.theme.PlayBlue

private val HomeBottomClearance = 82.dp
private val CatalogGap = 36.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onSelectShelf: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onRetry: () -> Unit,
    onOpenSearch: () -> Unit,
    onSearchQuery: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onOpenCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playAnim = remember { Animatable(PlayBlue) }
    var playRequest by remember { mutableStateOf(PlayBlue to false) }
    LaunchedEffect(playRequest) {
        val (color, dragging) = playRequest
        if (dragging) playAnim.snapTo(color)
        else playAnim.animateTo(color, tween(640, easing = FastOutSlowInEasing))
    }
    val play = playAnim.value
    val logo = remember { LogoMood() }
    BackHandler(enabled = state.searchOpen, onBack = onCloseSearch)
    if (state.searchOpen) {
        SearchPage(
            query = state.searchQuery,
            results = state.searchResults,
            searching = state.searching,
            error = state.searchError,
            onQuery = onSearchQuery,
            onClose = onCloseSearch,
            onMovieClick = onMovieClick,
            onMoviePeek = onMoviePeek,
        )
        return
    }
    val background = MaterialTheme.colorScheme.background
    if (state.shelves.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            when (val phase = state.phase) {
                HomePhase.Loading -> HomeSkeleton(
                    onSearch = onOpenSearch,
                    modifier = Modifier.fillMaxSize(),
                )
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

    val blocked = (state.phase is HomePhase.Failed || state.phase is HomePhase.Empty) && state.movies.isEmpty()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
    ) {
        if (blocked) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = 32.dp),
            ) {
                CategoryTabs(
                    shelves = state.shelves,
                    selectedId = state.selectedId,
                    playColor = play,
                    onSelect = onSelectShelf,
                    onSearch = onOpenSearch,
                    hat = logo.hat,
                    mustache = logo.mustache,
                    onMenuSettled = logo::onSettled,
                )
                StatusMessage(
                    message = if (state.phase is HomePhase.Failed) {
                        state.phase.reason.message()
                    } else {
                        stringResource(R.string.catalog_empty)
                    },
                    onRetry = if (state.phase is HomePhase.Failed) onRetry else null,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        } else {
            var scroll by remember { mutableFloatStateOf(0f) }
            var maxScroll by remember { mutableFloatStateOf(0f) }
            var viewport by remember { mutableIntStateOf(0) }
            var contentHeight by remember { mutableIntStateOf(0) }
            val pageScroll = rememberScrollableState { delta ->
                val next = (scroll - delta).coerceIn(0f, maxScroll)
                val used = next - scroll
                scroll = next
                -used
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .scrollable(
                        state = pageScroll,
                        orientation = Orientation.Vertical,
                        overscrollEffect = null,
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        bringIntoViewSpec = NoBringIntoView,
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .onSizeChanged {
                        viewport = it.height
                        maxScroll = (contentHeight - viewport).coerceAtLeast(0).toFloat()
                    },
            ) {
                Column(
                    modifier = Modifier
                        .scrollContent(scroll) { full ->
                            contentHeight = full
                            maxScroll = (contentHeight - viewport).coerceAtLeast(0).toFloat()
                        }
                        .padding(top = 32.dp),
                ) {
                    CategoryTabs(
                        shelves = state.shelves,
                        selectedId = state.selectedId,
                        playColor = play,
                        onSelect = onSelectShelf,
                        onSearch = onOpenSearch,
                        hat = logo.hat,
                        mustache = logo.mustache,
                        onMenuSettled = logo::onSettled,
                    )
                    Spacer(Modifier.height(36.dp))
                    if (state.phase is HomePhase.Loading && state.movies.isEmpty()) {
                        PosterSkeleton(Modifier.fillMaxWidth())
                    } else {
                        PosterCarousel(
                            movies = state.movies,
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            onPlayColor = { color, dragging ->
                                val next = color to dragging
                                if (next != playRequest) playRequest = next
                            },
                            onSettled = logo::onSettled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (!state.recommendationsReady || state.recommendations.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        RecommendationRail(
                            movies = state.recommendations,
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            title = stringResource(R.string.section_recommendations),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (!state.catalogPreviewReady || state.catalogPreview.isNotEmpty()) {
                        Spacer(Modifier.height(CatalogGap))
                        CatalogPlaque(
                            movies = state.catalogPreview,
                            onClick = onOpenCatalog,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (!state.topReady || state.top.isNotEmpty()) {
                        Spacer(Modifier.height(CatalogGap))
                        RecommendationRail(
                            movies = state.top,
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            title = stringResource(R.string.section_top),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (!state.bestReady || state.best.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        RecommendationRail(
                            movies = state.best,
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            title = stringResource(R.string.section_best),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(HomeBottomClearance))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private object NoBringIntoView : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

private fun Modifier.scrollContent(scroll: Float, onHeight: (Int) -> Unit): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity),
        )
        onHeight(placeable.height)
        val height = if (constraints.maxHeight == Constraints.Infinity) {
            placeable.height
        } else {
            constraints.maxHeight
        }
        layout(placeable.width, height) {
            placeable.place(0, -scroll.roundToInt())
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
private fun HomeSkeleton(onSearch: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 32.dp)) {
        CategoryTabsSkeleton(onSearch = onSearch)
        Spacer(Modifier.height(36.dp))
        CoverStackSkeleton(Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        RecommendationRail(
            movies = emptyList(),
            onMovieClick = {},
            onMoviePeek = {},
            title = stringResource(R.string.section_recommendations),
        )
        Spacer(Modifier.height(CatalogGap))
        CatalogPlaque(movies = emptyList())
        Spacer(Modifier.height(CatalogGap))
        RecommendationRail(
            movies = emptyList(),
            onMovieClick = {},
            onMoviePeek = {},
            title = stringResource(R.string.section_top),
        )
        Spacer(Modifier.height(24.dp))
        RecommendationRail(
            movies = emptyList(),
            onMovieClick = {},
            onMoviePeek = {},
            title = stringResource(R.string.section_best),
        )
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
                best = listOf(previewMovie("5", "Побег"), previewMovie("6", "Миля")),
                bestReady = true,
                phase = HomePhase.Ready,
            ),
            onSelectShelf = {},
            onMovieClick = {},
            onMoviePeek = {},
            onRetry = {},
            onOpenSearch = {},
            onSearchQuery = {},
            onCloseSearch = {},
            onOpenCatalog = {},
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
