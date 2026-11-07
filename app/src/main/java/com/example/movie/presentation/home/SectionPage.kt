package com.example.movie.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.haptic.HapticOnAppend
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.PosterCaption
import com.example.movie.presentation.home.components.PosterCaptionSkeleton
import com.example.movie.presentation.home.components.WatchProgress
import com.example.movie.presentation.home.components.posterTitlesOn
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.PosterWave
import com.example.movie.presentation.home.posterWave
import com.example.movie.presentation.home.rememberPressScale
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.PosterShape
import kotlinx.coroutines.flow.distinctUntilChanged

private val PosterAspect = 1.46f

@Composable
fun SectionRoute(
    section: HomeSection,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: SectionViewModel = viewModel(key = section.name) {
        SectionViewModel(
            section = section,
            getShelfMovies = container.getShelfMovies,
            getRecommendations = container.getRecommendations,
            getTopMovies = container.getTopMovies,
            viewHistory = container.viewHistory,
            resume = container.resume,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    SectionPage(
        title = stringResource(section.title),
        state = state,
        onBack = onBack,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        onMovieClick = onMovieClick,
        onMoviePeek = onMoviePeek,
        onClear = if (section == HomeSection.History) {
            {
                viewModel.clearHistory()
                onBack()
            }
        } else {
            null
        },
        modifier = modifier,
    )
}

@Composable
internal fun SectionPage(
    title: String,
    state: SectionUiState,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val grid = rememberLazyGridState()
    val density = LocalDensity.current
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val pageBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
    val header = top + BrandTopInset + 36.dp + 18.dp
    val scrim = if (grid.firstVisibleItemIndex > 0) {
        1f
    } else {
        val threshold = with(density) { 28.dp.toPx() }
        (grid.firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
    }
    Box(
        modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        when {
            state.error != null && state.movies.isEmpty() -> SectionError(onRetry)
            else -> PosterGrid(
                movies = state.movies,
                state = grid,
                topPadding = header,
                bottomPadding = pageBottom,
                loadingMore = state.loadingMore,
                end = state.end,
                error = state.error,
                placeholder = state.loading && state.movies.isEmpty(),
                onLoadMore = onLoadMore,
                onRetry = onRetry,
                onMovieClick = onMovieClick,
                onMoviePeek = onMoviePeek,
                marks = state.marks,
            )
        }
        SectionScrim(progress = scrim, modifier = Modifier.align(Alignment.TopCenter))
        SectionBar(
            title = title,
            onBack = onBack,
            onClear = onClear,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun SectionBar(
    title: String,
    onBack: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = BrandTopInset)
            .height(36.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (onClear != null) 108.dp else 56.dp),
        )
        if (onClear != null) {
            Text(
                text = stringResource(R.string.search_clear),
                color = Color(0xFF4DA3FF),
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = PosterGridInset)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onClear()
                        },
                    ),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = PosterGridInset - 7.dp)
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onBack()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShellIcons.back,
                contentDescription = stringResource(R.string.section_back),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
internal fun PosterGrid(
    movies: List<Movie>,
    state: LazyGridState,
    topPadding: Dp,
    bottomPadding: Dp,
    loadingMore: Boolean,
    end: Boolean,
    error: DataError?,
    placeholder: Boolean,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    marks: Map<String, PosterMark> = emptyMap(),
) {
    HapticOnAppend(movies.map { it.id })
    LaunchedEffect(state, movies.size, end, error, placeholder) {
        if (placeholder || end || error != null) return@LaunchedEffect
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { last ->
                if (last >= movies.size - 6) onLoadMore()
            }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = state,
        contentPadding = PaddingValues(start = PosterGridInset, end = PosterGridInset, top = topPadding, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        val count = if (placeholder) 12 else movies.size
        items(count, key = { it }) { index ->
            if (placeholder) {
                SkeletonPoster()
            } else {
                PosterCell(movies[index], onMovieClick, onMoviePeek, marks[movies[index].id])
            }
        }
        if (!placeholder && (loadingMore || error != null)) {
            item(span = { GridItemSpan(3) }) {
                MoreRow(loading = loadingMore, error = error, onRetry = onRetry)
            }
        }
    }
}

@Composable
internal fun SectionScrim(progress: Float, modifier: Modifier = Modifier) {
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    Box(
        modifier
            .fillMaxWidth()
            .height(top + BrandTopInset + 40.dp + 96.dp)
            .graphicsLayer { alpha = progress }
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.84f),
                        0.44f to Color.Black.copy(alpha = 0.74f),
                        0.64f to Color.Black.copy(alpha = 0.42f),
                        0.86f to Color.Transparent,
                        1f to Color.Transparent,
                    ),
                )
                val y = top.toPx() + BrandTopInset.toPx() + 18.dp.toPx()
                val radius = 140.dp.toPx()
                val backX = PosterGridInset.toPx() + 4.dp.toPx()
                fun pool(center: Offset) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.Black.copy(alpha = 0.58f),
                                0.45f to Color.Black.copy(alpha = 0.26f),
                                1f to Color.Transparent,
                            ),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
                pool(Offset(backX, y))
                pool(Offset(backX + 160.dp.toPx(), y))
            },
    )
}

@Composable
private fun PosterCell(
    movie: Movie,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    mark: PosterMark? = null,
) {
    val tick = rememberHapticTick()
    var holdProgress by remember(movie.id) { mutableFloatStateOf(0f) }
    var pressed by remember(movie.id) { mutableStateOf(false) }
    val pressScale = rememberPressScale(pressed)
    val wave = remember(movie.id) { PosterWave() }
    val showTitles = posterTitlesOn()
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f / PosterAspect)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(PosterShape)
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = stringResource(R.string.poster_content_description, movie.title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .posterWave(wave)
                    .holdToPeek(
                        onPeek = { onMoviePeek(movie) },
                        onProgress = { holdProgress = it },
                        onPressed = { pressed = it },
                        onClick = {
                            tick?.invoke()
                            onMovieClick(movie.id)
                        },
                        wave = wave,
                    )
                    .holdCue(holdProgress),
            )
            if (mark != null) {
                WatchProgress(
                    fraction = mark.fraction,
                    caption = mark.caption,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        if (showTitles) PosterCaption(movie.title, movie.year)
    }
}

@Composable
private fun SkeletonPoster() {
    val showTitles = posterTitlesOn()
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f / PosterAspect)
                .clip(PosterShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        if (showTitles) PosterCaptionSkeleton()
    }
}

@Composable
internal fun SectionError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.popular_load_error),
            color = Color(0xFFB4B4B8),
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.retry),
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onRetry,
            ),
        )
    }
}

@Composable
private fun MoreRow(loading: Boolean, error: DataError?, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (error != null) {
            Text(
                text = stringResource(R.string.retry),
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRetry,
                ),
            )
        } else if (loading) {
            Box(
                Modifier
                    .size(width = 72.dp, height = 8.dp)
                    .clip(PosterShape)
                    .background(Color.White.copy(alpha = 0.12f)),
            )
        }
    }
}
