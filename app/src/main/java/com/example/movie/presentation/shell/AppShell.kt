package com.example.movie.presentation.shell

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.favorites.FavoritesScreen
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.CatalogListRoute
import com.example.movie.presentation.home.CatalogScreen
import com.example.movie.presentation.home.CatalogSort
import com.example.movie.presentation.home.HomeRoute
import com.example.movie.presentation.home.HomeSection
import com.example.movie.presentation.home.HomeViewModel
import com.example.movie.presentation.home.SearchPage
import com.example.movie.presentation.home.SectionRoute
import com.example.movie.presentation.home.MoviePeek
import com.example.movie.presentation.settings.SettingsRoute
import com.example.movie.presentation.update.LocalAppUpdate
import com.example.movie.presentation.update.UpdateBadge
import com.example.movie.ui.theme.CinemaBlack

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
) {
    val tab = AppTab.valueOf(tabName)
    val keyboard = LocalSoftwareKeyboardController.current
    val container = LocalAppContainer.current
    val homeViewModel: HomeViewModel = viewModel {
        HomeViewModel(
            getHomeShelves = container.getHomeShelves,
            getShelfMovies = container.getShelfMovies,
            getRecommendations = container.getRecommendations,
            getTopMovies = container.getTopMovies,
            searchMovies = container.searchMovies,
            searchHistory = container.searchHistory,
            viewHistory = container.viewHistory,
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
    var sectionName by rememberSaveable { mutableStateOf<String?>(null) }
    var searchCover by remember { mutableFloatStateOf(0f) }
    val section = sectionName?.let { runCatching { HomeSection.valueOf(it) }.getOrNull() }
        ?.takeIf { tab == AppTab.Home }
    val sectionVeil = remember { Animatable(0f) }
    var sectionShown by remember { mutableStateOf<HomeSection?>(null) }
    LaunchedEffect(section) {
        if (section != null) {
            sectionShown = section
            sectionVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (sectionShown != null) {
            sectionVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            sectionShown = null
        }
    }
    val sectionAlpha = if (sectionShown != null) sectionVeil.value else 0f
    var catalogListKey by rememberSaveable { mutableStateOf<String?>(null) }
    val catalogList = catalogListKey?.takeIf { tab == AppTab.Catalog }
    val listVeil = remember { Animatable(0f) }
    var listShown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(catalogList) {
        if (catalogList != null) {
            listShown = catalogList
            listVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (listShown != null) {
            listVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            listShown = null
        }
    }
    val listAlpha = if (listShown != null) listVeil.value else 0f
    val catalogVeil = remember { Animatable(if (tab == AppTab.Catalog) 1f else 0f) }
    var catalogShown by remember { mutableStateOf(tab == AppTab.Catalog) }
    var catalogMounted by remember { mutableStateOf(tab == AppTab.Catalog) }
    var catalogUnder by remember { mutableStateOf(if (tab == AppTab.Catalog) AppTab.Home else tab) }
    val underTab = if (tab == AppTab.Catalog) catalogUnder else tab
    SideEffect {
        if (tab != AppTab.Catalog) catalogUnder = tab
    }
    LaunchedEffect(Unit) {
        if (!catalogMounted) {
            withFrameNanos { }
            catalogMounted = true
        }
    }
    LaunchedEffect(tab) {
        if (tab == AppTab.Catalog) {
            catalogShown = true
            withFrameNanos { }
            catalogVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (catalogShown) {
            catalogVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            catalogShown = false
        }
    }
    val catalogAlpha = if (catalogShown) catalogVeil.value else 0f
    LaunchedEffect(peek) {
        if (peek != null) keyboard?.hide()
    }
    BackHandler(
        enabled = handleBack && (peek != null || sectionShown != null || tab != AppTab.Home),
    ) {
        when {
            peek != null -> peek = null
            tab == AppTab.Catalog && homeState.searchOpen -> homeViewModel.closeSearch()
            listShown != null -> catalogListKey = null
            tab == AppTab.Home && sectionShown != null -> sectionName = null
            else -> onTabChange(AppTab.Home.name)
        }
    }
    LaunchedEffect(tab) {
        if (tab != AppTab.Home && tab != AppTab.Favorites) peek = null
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaBlack),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (1f - sectionAlpha) * (1f - catalogAlpha) },
        ) {
            when (underTab) {
                AppTab.Home -> HomeRoute(
                    onMovieClick = onMovieClick,
                    onMoviePeek = { peek = it },
                    onOpenSection = { sectionName = it.name },
                    onSearchCover = { searchCover = it },
                    presentSearch = tab == AppTab.Home,
                    session = session,
                )
                AppTab.Catalog -> Unit
                AppTab.Favorites -> FavoritesScreen(
                    onMovieClick = onMovieClick,
                    onMoviePeek = { peek = it },
                )
                AppTab.Settings -> SettingsRoute()
            }
        }
        if (catalogMounted) {
            Box(
                Modifier.then(
                    if (catalogShown) {
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = catalogAlpha }
                            .background(CinemaBlack)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            )
                    } else {
                        Modifier.holdOffscreen()
                    },
                ),
            ) {
                CatalogScreen(
                    onMovieClick = onMovieClick,
                    onMoviePeek = { peek = it },
                    onSearch = homeViewModel::openSearch,
                    onOpenShelf = { type, sort -> catalogListKey = "$type:$sort" },
                    active = tab == AppTab.Catalog,
                )
            }
        }
        val shown = sectionShown
        if (shown != null) {
            SectionRoute(
                section = shown,
                onBack = { sectionName = null },
                onMovieClick = onMovieClick,
                onMoviePeek = { peek = it },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = sectionAlpha },
            )
        }
        val opened = listShown
        if (opened != null) {
            val sort = opened.substringAfter(':')
            val titleRes = CatalogSort.entries.firstOrNull { it.sort == sort }?.title
            if (titleRes != null) {
                CatalogListRoute(
                    type = opened.substringBefore(':'),
                    sort = sort,
                    title = stringResource(titleRes),
                    onBack = { catalogListKey = null },
                    onMovieClick = onMovieClick,
                    onMoviePeek = { peek = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = listVeil.value },
                )
            }
        }
        val searchDim = if (tab == AppTab.Catalog) searchVeil.value else searchCover
        val barAlpha = (1f - searchDim) * (1f - sectionAlpha) * (1f - listAlpha)
        if (barAlpha > 0.02f) {
            AppBottomBar(
                selected = tab,
                onSelect = { next -> onTabChange(next.name) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = barAlpha },
            )
        }
        if (searchShown && tab == AppTab.Catalog) {
            SearchPage(
                query = homeState.searchQuery,
                results = homeState.searchResults,
                searching = homeState.searching,
                error = homeState.searchError,
                recent = homeState.recentQueries,
                onQuery = homeViewModel::search,
                onClose = homeViewModel::closeSearch,
                onClearRecent = homeViewModel::clearRecent,
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
        )
    }
}

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

@Composable
private fun AppBottomBar(
    selected: AppTab?,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    val press = ripple(color = Color(0xFF8E8E93))
    val updatePending = LocalAppUpdate.current.pending
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val belowGesture = (nav + 4.dp).coerceAtLeast(22.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CinemaBlack)
            .drawBehind {
                val stroke = 0.6.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
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
