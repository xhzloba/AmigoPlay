@file:OptIn(ExperimentalFoundationApi::class)

package com.example.movie.presentation.home

import androidx.annotation.StringRes
import androidx.compose.animation.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.Shelf
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.PageRefresh
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.BrandWordmark
import com.example.movie.presentation.home.components.CategoryTabs
import com.example.movie.presentation.home.components.HomeContentInset
import com.example.movie.presentation.home.components.RecommendationRail
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.PlayBlue

private enum class CatalogTab(@StringRes val label: Int, val type: String) {
    Movies(R.string.catalog_movies, "movie"),
    Series(R.string.catalog_series, "serial"),
    Cartoons(R.string.catalog_cartoons, "multfilm"),
    CartoonSeries(R.string.catalog_cartoon_series, "multserial"),
    Anime(R.string.catalog_anime, "anime"),
}

@Composable
fun CatalogScreen(
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onSearch: () -> Unit,
    onGenres: (type: String) -> Unit,
    onOpenShelf: (type: String, sort: String) -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var tabName by rememberSaveable { mutableStateOf(CatalogTab.Movies.name) }
    val tab = CatalogTab.valueOf(tabName)
    val container = LocalAppContainer.current
    val viewModel: CatalogViewModel = viewModel(key = tab.type) {
        CatalogViewModel(type = tab.type, getShelfMovies = container.getShelfMovies)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(active, tab.type) {
        if (active) viewModel.ensureLoaded()
    }
    val play = remember { Animatable(PlayBlue) }
    val menu = CatalogTab.entries.map { item ->
        Shelf(
            id = item.name,
            title = stringResource(item.label),
            query = CatalogQuery(sort = item.type),
        )
    }
    val density = LocalDensity.current
    var brandHeight by remember { mutableIntStateOf(0) }
    val brandReserve = if (brandHeight == 0) 40.dp else with(density) { brandHeight.toDp() }
    val scrimDistance = with(density) { 28.dp.toPx() }
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val failed = state.shelves.all { it.ready && it.movies.isEmpty() }
    val rails = state.shelves.filterNot { it.ready && it.movies.isEmpty() }
    PageRefresh(
        modifier = modifier.fillMaxSize(),
    ) {
    Box(Modifier.fillMaxSize()) {
        if (failed) {
            SectionError(viewModel::retry)
        } else {
            key(tab.name) {
            val listState = rememberLazyListState()
            Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topInset + BrandTopInset + brandReserve + 12.dp,
                    bottom = 96.dp,
                ),
            ) {
                item(key = "tabs") {
                    CategoryTabs(
                        shelves = menu,
                        selectedId = tab.name,
                        play = play,
                        onSelect = { id ->
                            if (id == tab.name) return@CategoryTabs
                            tabName = id
                        },
                        onSearch = onSearch,
                        showBrand = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                }
                items(
                    count = rails.size,
                    key = { index -> rails[index].sort.name },
                ) { index ->
                    val rail = rails[index]
                    if (index > 0) Spacer(Modifier.height(CatalogGap))
                    RecommendationRail(
                        movies = rail.movies,
                        onMovieClick = onMovieClick,
                        onMoviePeek = onMoviePeek,
                        title = stringResource(rail.sort.title),
                        onOpen = { onOpenShelf(tab.type, rail.sort.sort) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            }
            CatalogScrim(
                progress = {
                    if (listState.firstVisibleItemIndex > 0) {
                        1f
                    } else {
                        (listState.firstVisibleItemScrollOffset / scrimDistance).coerceIn(0f, 1f)
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            }
            }
        }
        CatalogBrand(
            onSearch = onSearch,
            onGenres = { onGenres(tab.type) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = BrandTopInset)
                .onSizeChanged { brandHeight = it.height },
        )
    }
    }
}

@Composable
private fun CatalogBrand(
    onSearch: () -> Unit,
    onGenres: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = HomeContentInset - BrandLogoBleed, end = HomeContentInset - 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandWordmark(play = PlayBlue)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onGenres()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShellIcons.genres,
                contentDescription = stringResource(R.string.catalog_genres),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onSearch()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShellIcons.search,
                contentDescription = stringResource(R.string.search),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun CatalogScrim(progress: () -> Float, modifier: Modifier = Modifier) {
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    Box(
        modifier
            .fillMaxWidth()
            .height(top + BrandTopInset + 40.dp + 96.dp)
            .graphicsLayer { alpha = progress() }
            .graphicsLayer()
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
                val y = top.toPx() + BrandTopInset.toPx() + 20.dp.toPx()
                val radius = 140.dp.toPx()
                val logoWidth = 36.dp.toPx() * (754f / 186f)
                val logoX = (HomeContentInset - BrandLogoBleed).toPx() + logoWidth / 2f
                val searchX = size.width - (HomeContentInset + 11.dp).toPx()
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
                pool(Offset(logoX, y))
                pool(Offset(searchX - 40.dp.toPx(), y))
                pool(Offset(searchX, y))
            },
    )
}

@Composable
internal fun CatalogListRoute(
    type: String,
    sort: String,
    title: String,
    genre: String? = null,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onHideBar: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: CatalogListViewModel = viewModel(key = "$type:$sort:${genre.orEmpty()}") {
        CatalogListViewModel(
            query = CatalogQuery(sort = sort, type = type, genre = genre),
            getShelfMovies = container.getShelfMovies,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    SectionPage(
        title = title,
        state = state,
        onBack = onBack,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        onMovieClick = onMovieClick,
        onMoviePeek = onMoviePeek,
        onHideBar = onHideBar,
        modifier = modifier,
    )
}

