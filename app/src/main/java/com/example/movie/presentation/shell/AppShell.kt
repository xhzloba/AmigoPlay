package com.example.movie.presentation.shell

import android.app.Activity
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.FranchiseSeason
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.ResumeItem
import com.example.movie.domain.model.episodeFinished
import com.example.movie.domain.model.episodeMarks
import com.example.movie.domain.model.fullyWatched
import com.example.movie.domain.model.toSaved
import com.example.movie.presentation.detail.WatchPlayer
import com.example.movie.presentation.detail.resumeWatchUrl
import com.example.movie.presentation.alerts.AlertsScreen
import com.example.movie.presentation.favorites.FavoritesScreen
import com.example.movie.presentation.haptic.rememberHapticTick
import kotlinx.coroutines.launch
import com.example.movie.presentation.home.CatalogGenresPage
import com.example.movie.presentation.home.CatalogListRoute
import com.example.movie.presentation.home.CatalogScreen
import com.example.movie.presentation.home.GenreAll
import com.example.movie.presentation.home.CatalogSort
import com.example.movie.presentation.home.HomeRoute
import com.example.movie.presentation.home.HomeSection
import com.example.movie.presentation.home.HomeViewModel
import com.example.movie.presentation.home.SearchPage
import com.example.movie.presentation.home.SectionRoute
import com.example.movie.presentation.common.LocalShelfNotices
import com.example.movie.presentation.home.MoviePeek
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.settings.SettingsRoute
import com.example.movie.presentation.update.LocalAppUpdate
import com.example.movie.presentation.update.UpdateBadge
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.common.appChrome
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class AppTab(@StringRes val label: Int, val color: Color) {
    Home(R.string.nav_home, Color(0xFF3B6EF5)),
    Catalog(R.string.nav_catalog, Color(0xFF14A36A)),
    Favorites(R.string.nav_favorites, Color(0xFFE23B5A)),
    Settings(R.string.nav_settings, Color(0xFFD4920A)),
    ;

    val icon: ImageVector
        get() = when (this) {
            Home -> ShellIcons.home
            Catalog -> ShellIcons.catalog
            Favorites -> ShellIcons.media
            Settings -> ShellIcons.settings
        }
}

@Composable
fun AppShell(
    tabName: String,
    onTabChange: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    handleBack: Boolean = true,
    session: Int = 0,
    alertsRequest: Int = 0,
) {
    val tab = AppTab.valueOf(tabName)
    val keyboard = LocalSoftwareKeyboardController.current
    val container = LocalAppContainer.current
    val homeViewModel: HomeViewModel = viewModel {
        HomeViewModel(
            getHomeShelves = container.getHomeShelves,
            getShelfMovies = container.getShelfMovies,
            getRecommendations = container.getRecommendations,
            searchMovies = container.searchMovies,
            searchHistory = container.searchHistory,
            viewHistory = container.viewHistory,
            resume = container.resume,
            favorites = container.favorites,
            watched = container.watched,
        )
    }
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val searchVeil = remember { Animatable(0f) }
    var searchShown by remember { mutableStateOf(false) }
    val catalogSearch = tab == AppTab.Catalog && homeState.searchOpen
    LaunchedEffect(catalogSearch) {
        if (catalogSearch) {
            searchShown = true
            withFrameNanos { }
            searchVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (searchShown) {
            searchVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            searchShown = false
        }
    }
    var peek by remember { mutableStateOf<Movie?>(null) }
    var resumePlayback by remember { mutableStateOf<ResumeItem?>(null) }
    val playResume: (String) -> Unit = { id ->
        val item = container.resume.items.value.firstOrNull { it.id == id }
        if (item != null) resumePlayback = item
    }
    var sectionName by rememberSaveable { mutableStateOf<String?>(null) }
    var searchCover by remember { mutableFloatStateOf(0f) }
    val section = sectionName?.let { runCatching { HomeSection.valueOf(it) }.getOrNull() }
        ?.takeIf { tab == AppTab.Home }
    val sectionShift = remember { Animatable(1f) }
    var sectionShown by remember { mutableStateOf<HomeSection?>(null) }
    var barHidden by remember { mutableStateOf(false) }
    var barHeight by remember { mutableIntStateOf(0) }
    var homeDockLeft by remember { mutableStateOf<Color?>(null) }
    var homeDockRight by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(section) {
        val move = tween<Float>(240, easing = FastOutSlowInEasing)
        if (section != null) {
            sectionShown = section
            sectionShift.snapTo(1f)
            withFrameNanos { }
            withFrameNanos { }
            sectionShift.animateTo(0f, move)
        } else if (sectionShown != null) {
            sectionShift.animateTo(1f, move)
            sectionShown = null
        }
    }
    var catalogListKey by rememberSaveable { mutableStateOf<String?>(null) }
    var genresType by rememberSaveable { mutableStateOf<String?>(null) }
    val notices = LocalShelfNotices.current
    LaunchedEffect(notices.openRequest) {
        if (notices.openRequest == 0) return@LaunchedEffect
        peek = null
        sectionName = null
        catalogListKey = null
        genresType = null
        if (homeState.searchOpen) homeViewModel.closeSearch()
    }
    val genresVeil = remember { Animatable(0f) }
    var genresShown by remember { mutableStateOf(false) }
    var genresHold by remember { mutableStateOf<String?>(null) }
    val genresOpen = genresType != null && tab == AppTab.Catalog
    LaunchedEffect(genresOpen) {
        if (genresOpen) {
            genresHold = genresType
            genresShown = true
            withFrameNanos { }
            genresVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (genresShown) {
            genresVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            genresShown = false
            genresHold = null
        }
    }
    val genresAlpha = if (genresShown) genresVeil.value else 0f
    val catalogList = catalogListKey?.takeIf { tab == AppTab.Catalog }
    val listShift = remember { Animatable(1f) }
    var listShown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(catalogList) {
        val move = tween<Float>(240, easing = FastOutSlowInEasing)
        if (catalogList != null) {
            listShown = catalogList
            listShift.snapTo(1f)
            withFrameNanos { }
            withFrameNanos { }
            listShift.animateTo(0f, move)
        } else if (listShown != null) {
            listShift.animateTo(1f, move)
            listShown = null
        }
    }
    var stageWidth by remember { mutableIntStateOf(0) }
    var base by remember { mutableStateOf(if (tab == AppTab.Catalog) AppTab.Home else tab) }
    var baseBehind by remember { mutableStateOf<AppTab?>(null) }
    val baseShift = remember { Animatable(0f) }
    var catalogShown by remember { mutableStateOf(tab == AppTab.Catalog) }
    var catalogMounted by remember { mutableStateOf(tab == AppTab.Catalog) }
    val catalogShift = remember { Animatable(if (tab == AppTab.Catalog) 0f else 1f) }
    var restParked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!catalogMounted) catalogMounted = true
        restParked = true
    }
    LaunchedEffect(tab) {
        val move = tween<Float>(240, easing = FastOutSlowInEasing)
        if (tab == AppTab.Catalog) {
            baseShift.snapTo(0f)
            baseBehind = null
            val dir = if (AppTab.Catalog.ordinal > base.ordinal) 1f else -1f
            catalogShown = true
            if (abs(catalogShift.value) > 0.85f) catalogShift.snapTo(dir)
            withFrameNanos { }
            catalogShift.animateTo(0f, move)
            return@LaunchedEffect
        }
        if (catalogShown) {
            if (tab != base) {
                base = tab
                baseShift.snapTo(0f)
                baseBehind = null
            }
            val dir = if (AppTab.Catalog.ordinal > tab.ordinal) 1f else -1f
            withFrameNanos { }
            withFrameNanos { }
            catalogShift.animateTo(dir, move)
            catalogShown = false
            return@LaunchedEffect
        }
        if (tab == base) return@LaunchedEffect
        val dir = if (tab.ordinal > base.ordinal) 1f else -1f
        baseBehind = base
        base = tab
        baseShift.snapTo(dir)
        withFrameNanos { }
        withFrameNanos { }
        baseShift.animateTo(0f, move)
        baseBehind = null
    }
    LaunchedEffect(peek) {
        if (peek != null) keyboard?.hide()
    }
    var alertsOpen by rememberSaveable { mutableStateOf(false) }
    val alertsVeil = remember { Animatable(0f) }
    var alertsShown by remember { mutableStateOf(false) }
    val openAlerts = { alertsOpen = true }
    LaunchedEffect(alertsRequest) {
        if (alertsRequest > 0) alertsOpen = true
    }
    LaunchedEffect(alertsOpen) {
        if (alertsOpen) {
            alertsShown = true
            withFrameNanos { }
            alertsVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (alertsShown) {
            alertsVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            alertsShown = false
        }
    }
    val context = LocalContext.current
    val exitHint = stringResource(R.string.exit_press_again)
    var exitArmedAt by remember { mutableLongStateOf(0L) }
    val atHomeRoot = handleBack &&
        resumePlayback == null &&
        !alertsShown &&
        peek == null &&
        sectionShown == null &&
        !genresShown &&
        listShown == null &&
        tab == AppTab.Home &&
        !homeState.searchOpen
    BackHandler(
        enabled = handleBack && resumePlayback == null && (alertsShown || peek != null || sectionShown != null || tab != AppTab.Home),
    ) {
        when {
            alertsShown -> alertsOpen = false
            peek != null -> peek = null
            tab == AppTab.Catalog && homeState.searchOpen -> homeViewModel.closeSearch()
            listShown != null -> catalogListKey = null
            genresShown -> genresType = null
            tab == AppTab.Home && sectionShown != null -> sectionName = null
            else -> onTabChange(AppTab.Home.name)
        }
    }
    // На корне главной: первый Back — toast, второй за 2с — выход.
    BackHandler(enabled = atHomeRoot) {
        val now = SystemClock.elapsedRealtime()
        if (now - exitArmedAt < 2_000L) {
            (context as? Activity)?.finish()
        } else {
            exitArmedAt = now
            Toast.makeText(context, exitHint, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(tab) {
        if (tab != AppTab.Home && tab != AppTab.Favorites) peek = null
    }
    @Composable
    fun TabBody(which: AppTab, modifier: Modifier) {
        Box(modifier) {
            when (which) {
                AppTab.Home -> HomeRoute(
                    onMovieClick = onMovieClick,
                    onMoviePeek = { peek = it },
                    onOpenSection = { sectionName = it.name },
                    onPlayResume = playResume,
                    onSearchCover = { searchCover = it },
                    onHeroDock = { left, right ->
                        homeDockLeft = left
                        homeDockRight = right
                    },
                    onOpenAlerts = openAlerts,
                    presentSearch = tab == AppTab.Home,
                    session = session,
                )
                AppTab.Catalog -> Unit
                AppTab.Favorites -> FavoritesScreen(
                    onMovieClick = onMovieClick,
                    onMoviePeek = { peek = it },
                    onPlayResume = playResume,
                )
                AppTab.Settings -> SettingsRoute()
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .appBackground(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { stageWidth = it.width }
                .then(
                    if (alertsShown) {
                        Modifier.graphicsLayer { alpha = 1f - alertsVeil.value }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Box(Modifier.fillMaxSize()) {
                for (which in listOf(AppTab.Home, AppTab.Favorites, AppTab.Settings)) {
                    val front = which == base
                    val under = which == baseBehind
                    if (!front && !under && !restParked) continue
                    key(which) {
                        val fraction: () -> Float = when {
                            front -> {
                                { baseShift.value }
                            }
                            under -> {
                                { 0f }
                            }
                            else -> {
                                { 2f }
                            }
                        }
                        val shown = front || under
                        TabBody(
                            which,
                            Modifier
                                .zIndex(if (front) 1f else 0f)
                                .slideBy(fraction, stageWidth)
                                .drawWithContent { if (shown) drawContent() }
                                .fillMaxSize()
                                .appBackground(),
                        )
                    }
                }
            }
            if (catalogMounted) {
                Box(
                    if (catalogShown) {
                        Modifier
                            .slideBy({ catalogShift.value }, stageWidth)
                            .fillMaxSize()
                            .appBackground()
                    } else {
                        Modifier.holdOffscreen()
                    },
                ) {
                    CatalogScreen(
                        onMovieClick = onMovieClick,
                        onMoviePeek = { peek = it },
                        onSearch = homeViewModel::openSearch,
                        onAlerts = openAlerts,
                        onGenres = { genresType = it },
                        onOpenShelf = { type, sort -> catalogListKey = "$type:$sort" },
                        active = tab == AppTab.Catalog,
                    )
                }
            }
        }
        val shown = sectionShown
        if (shown != null) {
            SectionRoute(
                section = shown,
                onBack = { sectionName = null },
                onMovieClick = if (shown == HomeSection.Continue) playResume else onMovieClick,
                onMoviePeek = { peek = it },
                onHideBar = { barHidden = it },
                modifier = Modifier
                    .slideBy({ sectionShift.value }, stageWidth)
                    .fillMaxSize()
                    .appBackground(),
            )
        }
        if (genresShown) {
            CatalogGenresPage(
                onBack = { genresType = null },
                onGenre = { name ->
                    val type = genresHold ?: return@CatalogGenresPage
                    catalogListKey = if (name == GenreAll) {
                        "$type:popular"
                    } else {
                        "$type:g:$name"
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = genresVeil.value },
            )
        }
        val opened = listShown
        if (opened != null) {
            val type = opened.substringBefore(':')
            val rest = opened.substringAfter(':')
            val genreName = rest.removePrefix("g:").takeIf { rest.startsWith("g:") }
            if (genreName != null) {
                CatalogListRoute(
                    type = type,
                    sort = "",
                    genre = genreName,
                    title = genreName,
                    onBack = { catalogListKey = null },
                    onMovieClick = onMovieClick,
                    onMoviePeek = { peek = it },
                    onHideBar = { barHidden = it },
                    modifier = Modifier
                        .slideBy({ listShift.value }, stageWidth)
                        .fillMaxSize()
                        .appBackground(),
                )
            } else {
                val titleRes = CatalogSort.entries.firstOrNull { it.sort == rest }?.title
                if (titleRes != null) {
                    CatalogListRoute(
                        type = type,
                        sort = rest,
                        title = stringResource(titleRes),
                        onBack = { catalogListKey = null },
                        onMovieClick = onMovieClick,
                        onMoviePeek = { peek = it },
                        onHideBar = { barHidden = it },
                        modifier = Modifier
                            .slideBy({ listShift.value }, stageWidth)
                            .fillMaxSize()
                            .appBackground(),
                    )
                }
            }
        }
        val searchDim = if (tab == AppTab.Catalog) searchVeil.value else searchCover
        val barAlpha = (1f - searchDim) * (1f - genresAlpha) * (1f - alertsVeil.value)
        val listOpen = sectionShown != null || listShown != null
        LaunchedEffect(sectionShown, listShown) { barHidden = false }
        val hide by animateFloatAsState(
            targetValue = if (barHidden && listOpen) 1f else 0f,
            animationSpec = tween(220),
            label = "barHide",
        )
        if (barAlpha > 0.02f) {
            val fromHome = sectionShown != null
            val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
            val washBar = settings.heroPosterColors &&
                settings.heroPosterPage &&
                tab == AppTab.Home &&
                sectionShown == null
            AppBottomBar(
                selected = if (fromHome) null else tab,
                onSelect = { next ->
                    if (fromHome && next == AppTab.Home) sectionName = null
                    else onTabChange(next.name)
                },
                washLeft = homeDockLeft.takeIf { washBar },
                washRight = homeDockRight.takeIf { washBar },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { barHeight = it.height }
                    .offset { IntOffset(0, (barHeight * hide).toInt()) }
                    .graphicsLayer { alpha = barAlpha },
            )
        }
        if (alertsShown) {
            AlertsScreen(
                onMovieClick = onMovieClick,
                onMoviePeek = { peek = it },
                onBack = { alertsOpen = false },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = alertsVeil.value }
                    .appBackground(),
            )
        }
        if (searchShown && tab == AppTab.Catalog) {
            SearchPage(
                query = homeState.searchQuery,
                results = homeState.searchResults,
                searching = homeState.searching,
                loadingMore = homeState.searchLoadingMore,
                end = homeState.searchEnd,
                error = homeState.searchError,
                recent = homeState.recentQueries,
                popular = homeState.searchPopular,
                popularReady = homeState.searchPopularReady,
                onQuery = homeViewModel::search,
                onClose = homeViewModel::closeSearch,
                onClearRecent = homeViewModel::clearRecent,
                onLoadMore = homeViewModel::loadMoreSearch,
                onRetry = homeViewModel::retrySearch,
                onMovieClick = onMovieClick,
                onMoviePeek = { peek = it },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = searchVeil.value },
            )
        }
        MoviePeek(
            movie = peek,
            onDismiss = { peek = null },
            onOpen = { id ->
                peek = null
                onMovieClick(id)
            },
            onContinue = { id ->
                peek = null
                playResume(id)
            },
        )
        val playing = resumePlayback
        if (playing != null) {
            val scope = rememberCoroutineScope()
            var aired by remember(playing.id) { mutableStateOf<List<FranchiseSeason>?>(null) }
            WatchPlayer(
                url = resumeWatchUrl(playing.franchiseId, playing.season, playing.episode),
                title = playing.title,
                logoUrl = playing.logoUrl,
                startPositionMs = playing.positionMs,
                onProgress = { position, duration, season, episode ->
                    val next = playing.copy(
                        positionMs = position,
                        durationMs = duration,
                        season = season ?: playing.season,
                        episode = episode ?: playing.episode,
                    )
                    val series = next.season != null && next.episode != null
                    if (series && aired == null && episodeFinished(position, duration)) {
                        aired = emptyList()
                        scope.launch {
                            val seasons = container.getMovieDetail(next.id).getOrNull()?.franchise?.seasons.orEmpty()
                            aired = seasons
                            val stored = container.resume.items.value.firstOrNull { it.id == next.id }
                            val marks = stored?.episodeMarks()
                                ?: container.resume.kept.value[next.id].orEmpty()
                            if (seasons.fullyWatched(marks)) {
                                container.watched.add((stored ?: next).toSaved())
                                container.favorites.remove(next.id)
                                container.resume.forget(next.id)
                            }
                        }
                    }
                    val finished = container.resume.record(next) { stored ->
                        aired.orEmpty().fullyWatched(stored.episodeMarks())
                    }
                    if (finished) {
                        container.watched.add(next.toSaved())
                        container.favorites.remove(next.id)
                    }
                },
                onDismiss = { resumePlayback = null },
            )
        }
    }
}

private fun Modifier.slideBy(fraction: () -> Float, widthPx: Int): Modifier =
    offset { IntOffset((fraction() * widthPx).roundToInt(), 0) }

private fun Modifier.holdOffscreen(): Modifier =
    layout { measurable, constraints ->
        if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
            val full = constraints.copy(
                minWidth = constraints.maxWidth,
                minHeight = constraints.maxHeight,
            )
            measurable.measure(full)
        }
        layout(0, 0) {}
    }.clearAndSetSemantics {}

private val IdleInk = Color(0xFFC7C7CC)
private val WashDockFallback = Color(0xFF121212)

/** Чуть глубже угол UltraBlur — бар читаемый, палитра та же. */
private fun Color.dockInk(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[1] = (hsv[1] * 0.95f).coerceAtMost(1f)
    hsv[2] = (hsv[2] * 0.82f).coerceIn(0.1f, 0.4f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
private fun AppBottomBar(
    selected: AppTab?,
    onSelect: (AppTab) -> Unit,
    washLeft: Color? = null,
    washRight: Color? = null,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    val press = ripple(color = Color(0xFF8E8E93))
    val updatePending = LocalAppUpdate.current.pending
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val belowGesture = (nav + 4.dp).coerceAtLeast(22.dp)
    val leftTarget = (washLeft ?: WashDockFallback).dockInk()
    val rightTarget = (washRight ?: washLeft ?: WashDockFallback).dockInk()
    val left by animateColorAsState(
        targetValue = leftTarget,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "barDockL",
    )
    val right by animateColorAsState(
        targetValue = rightTarget,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "barDockR",
    )
    val washOn = washLeft != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (washOn) Color.Transparent else appChrome())
            .drawBehind {
                if (washOn) {
                    // Горизонталь как нижние углы UltraBlur + плотный scrim для иконок.
                    drawRect(brush = Brush.horizontalGradient(listOf(left, right)))
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.22f),
                            0.4f to Color.Black.copy(alpha = 0.34f),
                            1f to Color.Black.copy(alpha = 0.46f),
                        ),
                    )
                }
                val stroke = 0.6.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = if (washOn) 0.16f else 0.28f),
                    start = Offset(0f, stroke / 2f),
                    end = Offset(size.width, stroke / 2f),
                    strokeWidth = stroke,
                )
            }
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = belowGesture),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTab.entries.forEach { tab ->
            val lit = tab == selected
            val badge = tab == AppTab.Settings && updatePending
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember(tab) { MutableInteractionSource() },
                            indication = press,
                            onClick = {
                                if (tab != selected) {
                                    tick?.invoke()
                                    onSelect(tab)
                                }
                            },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.label),
                        tint = if (lit) tab.color else IdleInk,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(tab.label),
                        modifier = Modifier.padding(top = 5.dp),
                        color = if (lit) tab.color else IdleInk,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (badge) {
                    UpdateBadge(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = 16.dp, y = (-2).dp),
                    )
                }
            }
        }
    }
}
