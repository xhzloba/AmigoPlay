package com.example.movie.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.presentation.settings.LocalUserSettings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.example.movie.R
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.ContentKind
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.Shelf
import com.example.movie.domain.model.progressCaption
import com.example.movie.domain.model.toMovie
import com.example.movie.presentation.detail.drawPlexWash
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.BrandRow
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.HomeContentInset
import com.example.movie.presentation.home.components.CategoryTabs
import com.example.movie.presentation.home.components.CategoryTabsSkeleton
import com.example.movie.presentation.home.components.CoverStackSkeleton
import com.example.movie.presentation.home.components.LogoMood
import com.example.movie.presentation.home.components.PageRefresh
import com.example.movie.presentation.home.components.PosterCarousel
import com.example.movie.presentation.home.components.RecommendationRail
import com.example.movie.presentation.common.BookmarkFilled
import com.example.movie.presentation.common.HideBarOnScroll
import com.example.movie.presentation.common.PlexPlay
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.common.TopScrimBrand
import com.example.movie.presentation.common.TopScrimTail
import com.example.movie.presentation.common.topScrimFade
import com.example.movie.presentation.common.topScrimPalette
import com.example.movie.presentation.palette.PosterWash
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.MovieTheme
import com.example.movie.ui.theme.PlayBlue

private val HomeBottomClearance = 96.dp
/** Между полками и вокруг главного слайдера (до карусели и после точек). */
private val HomeRailGap = 24.dp
private const val HistoryRailSize = 15

private data class ShelfTint(
    val play: Color,
    val chip: Color,
    val dragging: Boolean,
)

internal val CatalogGap = 36.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSelectShelf: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onRetry: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAlerts: () -> Unit = {},
    onSearchQuery: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onClearRecent: () -> Unit,
    onLoadMoreSearch: () -> Unit = {},
    onRetrySearch: () -> Unit = {},
    onOpenSection: (HomeSection) -> Unit = {},
    onPlayResume: (String) -> Unit = {},
    onSearchCover: (Float) -> Unit = {},
    onHeroDock: (Color?, Color?) -> Unit = { _, _ -> },
    onHideBar: (Boolean) -> Unit = {},
    presentSearch: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val playAnim = remember { Animatable(PlayBlue) }
    val capsuleAnim = remember { Animatable(PlayBlue) }
    var playRequest by remember { mutableStateOf(ShelfTint(PlayBlue, PlayBlue, false)) }
    LaunchedEffect(Unit) {
        val glide = tween<Color>(720, easing = FastOutSlowInEasing)
        snapshotFlow { playRequest.play to playRequest.chip }
            .distinctUntilChanged()
            .collect { (nextPlay, nextChip) ->
                coroutineScope {
                    launch { playAnim.animateTo(nextPlay, glide) }
                    launch { capsuleAnim.animateTo(nextChip, glide) }
                }
            }
    }
    var heroWash by remember { mutableStateOf<PosterWash?>(null) }
    // Кэш живёт на HomeScreen: LazyColumn уничтожает карусель при скролле вниз.
    val heroWashes = remember { mutableStateMapOf<String, PosterWash>() }
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    var railsLookOpen by remember { mutableStateOf(false) }
    val pageWashOn = settings.heroPosterColors && settings.heroPosterPage
    val playTintOn = settings.heroPosterColors
    LaunchedEffect(settings.heroPosterColors) {
        if (!settings.heroPosterColors) {
            heroWash = null
            heroWashes.clear()
            playRequest = ShelfTint(PlayBlue, PlayBlue, false)
        }
    }
    val paintedWash = heroWash.takeIf { pageWashOn }
    SideEffect {
        // Dock бара только в режиме «на всю главную».
        val dock = heroWash.takeIf { pageWashOn }
        onHeroDock(dock?.bottomLeft, dock?.bottomRight)
    }
    val homeBackground = if (paintedWash != null) {
        Modifier.drawBehind {
            drawRect(Color(0xFF121212))
            drawPlexWash(paintedWash, 1f)
        }
    } else {
        Modifier.appBackground()
    }
    val play = playAnim.value
    val logo = remember { LogoMood() }
    BackHandler(enabled = presentSearch && state.searchOpen, onBack = onCloseSearch)
    val veil = remember { androidx.compose.animation.core.Animatable(0f) }
    var searchCover by remember { mutableStateOf(false) }
    LaunchedEffect(state.searchOpen, presentSearch) {
        if (!presentSearch) return@LaunchedEffect
        if (state.searchOpen) {
            searchCover = true
            veil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (searchCover) {
            veil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            searchCover = false
        }
    }
    val cover = if (searchCover) veil.value else 0f
    SideEffect { onSearchCover(cover) }
    Box(modifier = modifier.fillMaxSize()) {
        PageRefresh(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (searchCover) {
                        Modifier.graphicsLayer { alpha = 1f - veil.value }
                    } else {
                        Modifier
                    },
                ),
        ) {
    if (state.shelves.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(homeBackground)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            when (val phase = state.phase) {
                HomePhase.Loading ->                 HomeSkeleton(
                    onSearch = onOpenSearch,
                    onAlerts = onOpenAlerts,
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
    } else {
    val blocked = (state.phase is HomePhase.Failed || state.phase is HomePhase.Empty) && state.movies.isEmpty()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(homeBackground)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
            ),
    ) {
        if (blocked) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = BrandTopInset),
            ) {
                CategoryTabs(
                    shelves = state.shelves,
                    selectedId = state.selectedId,
                    play = playAnim,
                    border = capsuleAnim.value,
                    onSelect = onSelectShelf,
                    onSearch = onOpenSearch,
                    onAlerts = onOpenAlerts,
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
            val density = LocalDensity.current
            val scrimDistance = with(density) { 28.dp.toPx() }
            val parkDistance = with(density) { 12.dp.toPx() }
            var brandHeight by remember { mutableIntStateOf(0) }
            val playBlend = remember { androidx.compose.animation.core.Animatable(0f) }
            val listState = rememberLazyListState()
            HideBarOnScroll(
                enabled = settings.hideBottomBarOnScroll,
                listState = listState,
                onHideBar = onHideBar,
            )
            val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
            LaunchedEffect(parkDistance, playTintOn) {
                // UltraBlur: Play остаётся цветом обложки, без ухода в синий при скролле.
                if (playTintOn) {
                    playBlend.snapTo(0f)
                    return@LaunchedEffect
                }
                snapshotFlow {
                    listState.firstVisibleItemIndex > 0 ||
                        listState.firstVisibleItemScrollOffset >= parkDistance
                }
                    .distinctUntilChanged()
                    .collect { away ->
                        playBlend.animateTo(
                            if (away) 1f else 0f,
                            tween(520, easing = FastOutSlowInEasing),
                        )
                    }
            }
            val brandReserve = if (brandHeight == 0) 40.dp else with(density) { brandHeight.toDp() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topInset + BrandTopInset + brandReserve + 12.dp,
                    bottom = HomeBottomClearance,
                ),
            ) {
                item(key = "tabs") {
                    CategoryTabs(
                        shelves = state.shelves,
                        selectedId = state.selectedId,
                        play = playAnim,
                        border = capsuleAnim.value,
                        onSelect = onSelectShelf,
                        onSearch = onOpenSearch,
                        hat = logo.hat,
                        mustache = logo.mustache,
                        onMenuSettled = logo::onSettled,
                        showBrand = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(HomeRailGap))
                }
                item(key = "hero") {
                    if (state.phase is HomePhase.Loading && state.movies.isEmpty()) {
                        PosterSkeleton(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(HomeRailGap))
                    } else {
                        PosterCarousel(
                            movies = state.movies,
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            heroWashes = heroWashes,
                            onHeroWash = { wash ->
                                // null при recreate карусели не сбрасываем — иначе мелькает фон темы
                                if (wash != null) heroWash = wash
                            },
                            onPlayColor = { playColor, chip, _ ->
                                if (!playTintOn) return@PosterCarousel
                                playRequest = ShelfTint(playColor, chip, false)
                            },
                            onSettled = logo::onSettled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                val showRecommendations = settings.homeRailVisible(HomeRails.Recommendations) &&
                    (!state.recommendationsReady || state.recommendations.isNotEmpty())
                val showHistory = settings.homeRailVisible(HomeRails.History) && state.history.isNotEmpty()
                val showContinue = settings.homeRailVisible(HomeRails.Continue) && state.resume.isNotEmpty()
                val showFavorites = settings.homeRailVisible(HomeRails.Favorites) && state.favorites.isNotEmpty()
                val showWatched = settings.homeRailVisible(HomeRails.Watched) && state.watched.isNotEmpty()
                val showWatching = settings.homeRailVisible(HomeRails.Watching) &&
                    (!state.watchingReady || state.watching.isNotEmpty())
                val showBest = settings.homeRailVisible(HomeRails.Best) &&
                    (!state.bestReady || state.best.isNotEmpty())
                // После героя зазор уже в карусели; между полками — HomeRailGap.
                val railBeforePlaque = showRecommendations || showHistory || showContinue ||
                    showFavorites || showWatched || showWatching || showBest
                val firstRail = when {
                    showRecommendations -> "recommendations"
                    showHistory -> "history"
                    showContinue -> "continue"
                    showFavorites -> "favorites"
                    showWatched -> "watched"
                    showWatching -> "watching"
                    showBest -> "best"
                    else -> null
                }
                if (showRecommendations) {
                    item(key = "recommendations") {
                        RecommendationRail(
                            movies = state.recommendations,
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            title = stringResource(R.string.section_recommendations),
                            onOpen = { onOpenSection(HomeSection.Recommendations) },
                            onSettled = logo::onSettled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (showHistory) {
                    item(key = "history") {
                        if (firstRail != "history") Spacer(Modifier.height(HomeRailGap))
                        RecommendationRail(
                            movies = state.history.take(HistoryRailSize),
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            title = stringResource(R.string.section_history),
                            leadingIcon = ShellIcons.history,
                            onOpen = { onOpenSection(HomeSection.History) },
                            onSettled = logo::onSettled,
                            stickToStart = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (showContinue) {
                    item(key = "continue") {
                        if (firstRail != "continue") Spacer(Modifier.height(HomeRailGap))
                        val rail = state.resume.take(HistoryRailSize)
                        RecommendationRail(
                            movies = rail.map { it.toMovie() },
                            onMovieClick = onPlayResume,
                            onMoviePeek = onMoviePeek,
                            title = stringResource(R.string.section_continue),
                            leadingIcon = PlexPlay,
                            onOpen = { onOpenSection(HomeSection.Continue) },
                            onSettled = logo::onSettled,
                            stickToStart = true,
                            progressOf = { movie -> rail.firstOrNull { it.id == movie.id }?.fraction },
                            captionOf = { movie -> rail.firstOrNull { it.id == movie.id }?.progressCaption() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (showFavorites) {
                    item(key = "favorites") {
                        if (firstRail != "favorites") Spacer(Modifier.height(HomeRailGap))
                        RecommendationRail(
                            movies = state.favorites.take(HistoryRailSize),
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            title = stringResource(R.string.section_favorites),
                            leadingIcon = BookmarkFilled,
                            onOpen = if (state.favorites.size >= HistoryRailSize) {
                                { onOpenSection(HomeSection.Favorites) }
                            } else {
                                null
                            },
                            onSettled = logo::onSettled,
                            stickToStart = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (showWatched) {
                    item(key = "watched") {
                        if (firstRail != "watched") Spacer(Modifier.height(HomeRailGap))
                        RecommendationRail(
                            movies = state.watched.take(HistoryRailSize),
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            title = stringResource(R.string.section_watched),
                            onOpen = if (state.watched.size >= HistoryRailSize) {
                                { onOpenSection(HomeSection.Watched) }
                            } else {
                                null
                            },
                            onSettled = logo::onSettled,
                            stickToStart = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (showWatching) {
                    item(key = "watching") {
                        if (firstRail != "watching") Spacer(Modifier.height(HomeRailGap))
                        RecommendationRail(
                            movies = state.watching,
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            title = stringResource(R.string.section_watching),
                            onOpen = { onOpenSection(HomeSection.Watching) },
                            onSettled = logo::onSettled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (showBest) {
                    item(key = "best") {
                        if (firstRail != "best") Spacer(Modifier.height(HomeRailGap))
                        RecommendationRail(
                            movies = state.best,
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            title = stringResource(R.string.section_best),
                            onOpen = { onOpenSection(HomeSection.Best) },
                            onSettled = logo::onSettled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item(key = "rails_look") {
                    if (railBeforePlaque) Spacer(Modifier.height(HomeRailGap))
                    HomeRailsLookPlaque(
                        onOpen = { railsLookOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            BrandScrim(
                progress = {
                    if (listState.firstVisibleItemIndex > 0) {
                        1f
                    } else {
                        (listState.firstVisibleItemScrollOffset / scrimDistance).coerceIn(0f, 1f)
                    }
                },
                wash = heroWash.takeIf { playTintOn },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            BrandRow(
                play = play,
                blend = playBlend,
                onSearch = onOpenSearch,
                onAlerts = onOpenAlerts,
                hat = logo.hat,
                mustache = logo.mustache,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = BrandTopInset)
                    .onSizeChanged { brandHeight = it.height },
            )
            }
        }
    }
    }
        }
        if (railsLookOpen) {
            HomeRailsLookSheet(onDismiss = { railsLookOpen = false })
        }
        if (presentSearch && searchCover) {
            SearchPage(
                query = state.searchQuery,
                results = state.searchResults,
                searching = state.searching,
                loadingMore = state.searchLoadingMore,
                end = state.searchEnd,
                error = state.searchError,
                recent = state.recentQueries,
                popular = state.searchPopular,
                popularReady = state.searchPopularReady,
                onQuery = onSearchQuery,
                onClose = onCloseSearch,
                onClearRecent = onClearRecent,
                onLoadMore = onLoadMoreSearch,
                onRetry = onRetrySearch,
                onMovieClick = onMovieClick,
                onMoviePeek = onMoviePeek,
                modifier = Modifier.graphicsLayer { alpha = veil.value },
            )
        }
    }
}

@Composable
private fun BrandScrim(
    progress: () -> Float,
    wash: PosterWash? = null,
    modifier: Modifier = Modifier,
) {
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val theme = topScrimPalette()
    val washBase = wash?.let { lerp(it.topLeft, it.topRight, 0.5f) }
    val animatedWash by animateColorAsState(
        targetValue = washBase ?: Color.Black,
        animationSpec = tween(720, easing = FastOutSlowInEasing),
        label = "brandScrimWash",
    )
    val palette = if (washBase != null) topScrimPalette(animatedWash) else theme
    Box(
        modifier
            .fillMaxWidth()
            .height(top + BrandTopInset + TopScrimBrand + TopScrimTail)
            .topScrimFade(progress)
            .drawBehind {
                drawRect(brush = palette.verticalBrush())
                val y = top.toPx() + BrandTopInset.toPx() + 20.dp.toPx()
                val radius = 140.dp.toPx()
                val logoWidth = 36.dp.toPx() * (754f / 186f)
                val logoX = (HomeContentInset - BrandLogoBleed).toPx() + logoWidth / 2f
                val searchX = size.width - (HomeContentInset + 11.dp).toPx()
                fun pool(center: Offset) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to palette.poolCore,
                                0.45f to palette.poolMid,
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
private fun DataError.message(): String = when (this) {
    is DataError.Network -> stringResource(R.string.popular_network_error)
    is DataError.Http -> stringResource(R.string.popular_http_error, code)
    is DataError.Parsing -> stringResource(R.string.popular_parse_error)
    is DataError.Unknown -> stringResource(R.string.popular_load_error)
}

@Composable
private fun HomeSkeleton(onSearch: () -> Unit, onAlerts: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = BrandTopInset)) {
        CategoryTabsSkeleton(onSearch = onSearch, onAlerts = onAlerts)
        Spacer(Modifier.height(16.dp))
        CoverStackSkeleton(Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        RecommendationRail(
            movies = emptyList(),
            onMovieClick = {},
            onMoviePeek = {},
            title = stringResource(R.string.section_recommendations),
        )
        Spacer(Modifier.height(CatalogGap))
        RecommendationRail(
            movies = emptyList(),
            onMovieClick = {},
            onMoviePeek = {},
            title = stringResource(R.string.section_watching),
        )
        Spacer(Modifier.height(CatalogGap))
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
                    Shelf("popular:", "Популярное", CatalogQuery("popular")),
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
            onClearRecent = {},
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
