package com.example.movie.presentation.favorites

import android.graphics.drawable.ColorDrawable
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.movie.presentation.home.PeekEnter
import com.example.movie.presentation.home.PeekExit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.progressCaption
import com.example.movie.domain.model.toMovie
import com.example.movie.domain.model.toSaved
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.PosterCaption
import com.example.movie.presentation.home.components.WatchProgress
import com.example.movie.presentation.home.components.posterTitlesOn
import com.example.movie.presentation.home.components.BrandWordmark
import com.example.movie.presentation.home.components.MenuSombreroEffect
import com.example.movie.presentation.home.components.LogoMood
import com.example.movie.presentation.home.components.CategoryTabFontSize
import com.example.movie.presentation.home.components.CategoryTabLetterSpacing
import com.example.movie.presentation.home.components.CategoryTabLineHeight
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.PosterWave
import com.example.movie.presentation.home.posterWave
import com.example.movie.presentation.home.rememberPressScale
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface
import com.example.movie.ui.theme.PosterShape

private val FavoritePlay = Color(0xFFE23B5A)
private val CapsuleInk = Color.White.copy(alpha = 0.72f)
private val MenuShape = RoundedCornerShape(16.dp)
private val FilterShape = RoundedCornerShape(22.dp)
private val FilterNotes = Color(0xFFD2D2D8)

private enum class LibrarySort(@StringRes val label: Int) {
    Genre(R.string.favorites_sort_genre),
    Year(R.string.favorites_sort_year),
    Title(R.string.favorites_sort_title),
}

private fun decodeFilters(raw: String): Map<String, String> {
    if (raw.isEmpty()) return emptyMap()
    return raw.split('\u001e').mapNotNull { row ->
        val split = row.indexOf('\u001f')
        if (split <= 0) null else row.substring(0, split) to row.substring(split + 1)
    }.toMap()
}

private fun encodeFilters(filters: Map<String, String>): String =
    filters.entries.joinToString("\u001e") { "${it.key}\u001f${it.value}" }

private fun Map<String, String>.with(key: String, value: String?): Map<String, String> {
    val next = toMutableMap()
    if (value == null) next.remove(key) else next[key] = value
    return next
}

private enum class FavoriteTab(@StringRes val label: Int, val type: String?, val hasGenres: Boolean) {
    All(R.string.favorites_all, null, true),
    Movies(R.string.favorites_movies, "movie", true),
    Series(R.string.favorites_series, "serial", true),
    Cartoons(R.string.favorites_cartoons, "multfilm", false),
    Anime(R.string.favorites_anime, "anime", false),
}

@Composable
internal fun FavoritesScreen(
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onPlayResume: (String) -> Unit = {},
) {
    val container = LocalAppContainer.current
    val favorites by container.favorites.items.collectAsStateWithLifecycle()
    val watched by container.watched.items.collectAsStateWithLifecycle()
    val resume by container.resume.items.collectAsStateWithLifecycle()
    var libraryName by rememberSaveable { mutableStateOf(LibrarySection.Favorites.name) }
    val library = LibrarySection.entries.firstOrNull { it.name == libraryName } ?: LibrarySection.Favorites
    val items = when (library) {
        LibrarySection.Favorites -> favorites
        LibrarySection.Continue -> resume.map { it.toSaved() }
        LibrarySection.Watched -> watched
        LibrarySection.Actors -> emptyList()
    }
    var tabName by rememberSaveable { mutableStateOf(FavoriteTab.All.name) }
    var genreRaw by rememberSaveable { mutableStateOf("") }
    var sortRaw by rememberSaveable { mutableStateOf("") }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val logo = remember { LogoMood() }
    val tab = FavoriteTab.valueOf(tabName)
    val genre = decodeFilters(genreRaw)[tab.name]
    val sort = decodeFilters(sortRaw)[tab.name]?.let { runCatching { LibrarySort.valueOf(it) }.getOrNull() }
    val inTab = tab.type?.let { type -> items.filter { it.type == type } } ?: items
    val genres = if (tab.hasGenres) inTab.genreLabels() else emptyList()
    val filtered = genre?.let { name -> inTab.filter { name in it.genreKeys() } } ?: inTab
    val shown = filtered.sortedFor(sort)
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val scrim = if (gridState.firstVisibleItemIndex > 0) {
        1f
    } else {
        val threshold = with(density) { 28.dp.toPx() }
        (gridState.firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
    }
    var brandHeight by remember { mutableIntStateOf(0) }
    val brandReserve = if (brandHeight == 0) 40.dp else with(density) { brandHeight.toDp() }
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val onSelect = { next: FavoriteTab ->
        filtersOpen = false
        tabName = next.name
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (shown.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = BrandTopInset),
            ) {
                LibraryBrand(
                    hat = logo.hat,
                    mustache = logo.mustache,
                    library = library,
                    onLibrary = { libraryName = it.name },
                )
                FavoriteTabs(
                    selected = tab,
                    genres = genres,
                    genre = genre,
                    filtersOpen = filtersOpen,
                    onSelect = onSelect,
                    onOpenFilters = { filtersOpen = true },
                    onMenuSettled = logo::onSettled,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.catalog_empty),
                        color = CinemaMuted,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = PosterGridInset,
                    end = PosterGridInset,
                    top = topInset + BrandTopInset + brandReserve + 12.dp,
                    bottom = 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "tabs", span = { GridItemSpan(maxLineSpan) }) {
                    FavoriteTabs(
                        selected = tab,
                        genres = genres,
                        genre = genre,
                        filtersOpen = filtersOpen,
                        onSelect = onSelect,
                        onOpenFilters = { filtersOpen = true },
                        onMenuSettled = logo::onSettled,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .layout { measurable, constraints ->
                                val bleed = PosterGridInset.roundToPx()
                                val placeable = measurable.measure(
                                    constraints.copy(maxWidth = constraints.maxWidth + bleed * 2),
                                )
                                layout(constraints.maxWidth, placeable.height) {
                                    placeable.place(-bleed, 0)
                                }
                            },
                    )
                }
                items(shown, key = { it.id }) { item ->
                    val mark = if (library == LibrarySection.Continue) resume.firstOrNull { it.id == item.id } else null
                    FavoritePoster(
                        item = item,
                        progress = mark?.fraction,
                        caption = mark?.progressCaption(),
                        onMovieClick = if (library == LibrarySection.Continue) onPlayResume else onMovieClick,
                        onMoviePeek = onMoviePeek,
                    )
                }
            }
            LibraryScrim(
                progress = scrim,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            LibraryBrand(
                hat = logo.hat,
                mustache = logo.mustache,
                library = library,
                onLibrary = { libraryName = it.name },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = BrandTopInset)
                    .onSizeChanged { brandHeight = it.height },
            )
        }
        LibraryFilterDialog(
                open = filtersOpen && tab.hasGenres,
                title = stringResource(tab.label),
                genres = genres,
                genre = genre,
                sort = sort,
                onGenre = { next ->
                    genreRaw = encodeFilters(decodeFilters(genreRaw).with(tab.name, next))
                },
                onSort = { next ->
                    val value = if (sort == next) null else next.name
                    sortRaw = encodeFilters(decodeFilters(sortRaw).with(tab.name, value))
                },
                onDismiss = { filtersOpen = false },
            )
    }
}

@Composable
private fun LibraryBrand(
    hat: Boolean,
    mustache: Boolean,
    library: LibrarySection,
    onLibrary: (LibrarySection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PosterGridInset - BrandLogoBleed, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandWordmark(play = FavoritePlay, hat = hat, mustache = mustache)
        Spacer(Modifier.weight(1f))
        LibraryMenu(selected = library, onSelect = onLibrary)
    }
}

@Composable
private fun LibraryScrim(progress: Float, modifier: Modifier = Modifier) {
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
                val y = top.toPx() + BrandTopInset.toPx() + 20.dp.toPx()
                val radius = 140.dp.toPx()
                val logoWidth = 36.dp.toPx() * (754f / 186f)
                val logoX = (PosterGridInset - BrandLogoBleed).toPx() + logoWidth / 2f
                val menuX = size.width - 80.dp.toPx()
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
                pool(Offset(menuX, y))
            },
    )
}

private enum class LibrarySection(@StringRes val label: Int) {
    Favorites(R.string.nav_favorites),
    Continue(R.string.section_continue),
    Watched(R.string.favorites_watched),
    Actors(R.string.favorites_actors),
}

@Composable
private fun LibraryMenu(
    selected: LibrarySection,
    onSelect: (LibrarySection) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.height(40.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(CircleShape)
                .background(CinemaSurface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { open = true },
                )
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(selected.label),
                color = CapsuleInk,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = (-0.2).sp,
            )
            Spacer(Modifier.width(2.dp))
            MenuArrow(open = open, tint = CapsuleInk, size = 16.dp)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = CinemaSurface,
            shape = MenuShape,
        ) {
            LibrarySection.entries.forEach { section ->
                val lit = section == selected
                Text(
                    text = stringResource(section.label),
                    color = if (lit) CapsuleInk else CinemaMuted,
                    fontWeight = if (lit) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember(section.name) { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                open = false
                                onSelect(section)
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun List<SavedTitle>.genreLabels(): List<String> {
    val labels = LinkedHashMap<String, String>()
    for (item in this) {
        item.genres.split(',').forEach { raw ->
            val label = raw.trim()
            if (label.isEmpty()) return@forEach
            labels.putIfAbsent(label.lowercase(), label)
        }
    }
    return labels.values.toList()
}

private fun SavedTitle.genreKeys(): Set<String> =
    genres.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

private fun List<SavedTitle>.sortedFor(sort: LibrarySort?): List<SavedTitle> = when (sort) {
    null -> this
    LibrarySort.Title -> sortedBy { it.title.lowercase() }
    LibrarySort.Year -> sortedWith(
        compareByDescending<SavedTitle> { it.year != null }
            .thenByDescending { it.year ?: 0 }
            .thenBy { it.title.lowercase() },
    )
    LibrarySort.Genre -> sortedWith(
        compareBy<SavedTitle> { it.genres.substringBefore(',').trim().lowercase() }
            .thenBy { it.title.lowercase() },
    )
}

@Composable
private fun FavoriteTabs(
    selected: FavoriteTab,
    genres: List<String>,
    genre: String?,
    filtersOpen: Boolean,
    onSelect: (FavoriteTab) -> Unit,
    onOpenFilters: () -> Unit,
    onMenuSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    MenuSombreroEffect(listState, onMenuSettled)
    HapticOnChange(listState) { listState.passedTab() }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = PosterGridInset),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(FavoriteTab.entries, key = { it.name }) { tab ->
            val lit = tab == selected
            val title = stringResource(tab.label)
            val picked = genres.firstOrNull { it.lowercase() == genre }
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember(tab.name) { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (tab != selected) onSelect(tab)
                        else if (tab.hasGenres) onOpenFilters()
                    },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(title)
                        if (lit && picked != null) {
                            withStyle(SpanStyle(color = CapsuleInk, fontWeight = FontWeight.Normal)) {
                                append(" — $picked")
                            }
                        }
                    },
                    color = if (lit) Color.White else Color(0xFF8A8A90),
                    fontWeight = if (lit) FontWeight.Bold else FontWeight.Normal,
                    fontSize = CategoryTabFontSize,
                    lineHeight = CategoryTabLineHeight,
                    letterSpacing = CategoryTabLetterSpacing,
                )
                if (tab.hasGenres && lit) {
                    MenuArrow(
                        open = filtersOpen && lit,
                        tint = if (lit && picked == null) Color.White else CapsuleInk,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryFilterDialog(
    open: Boolean,
    title: String,
    genres: List<String>,
    genre: String?,
    sort: LibrarySort?,
    onGenre: (String?) -> Unit,
    onSort: (LibrarySort) -> Unit,
    onDismiss: () -> Unit,
) {
    val visible = remember { MutableTransitionState(false) }
    LaunchedEffect(open) { visible.targetState = open }
    if (!visible.currentState && !visible.targetState) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setWindowAnimations(0)
                setDimAmount(0f)
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
        }
        AnimatedVisibility(
            visibleState = visible,
            enter = PeekEnter,
            exit = PeekExit,
            modifier = Modifier.fillMaxSize(),
        ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 22.dp)
                        .fillMaxWidth()
                        .clip(FilterShape)
                        .background(CinemaSurface)
                        .border(0.5.dp, Color.White.copy(alpha = 0.10f), FilterShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.favorites_sort),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        color = CinemaMuted,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LibrarySort.entries.forEach { option ->
                            FilterChip(
                                label = stringResource(option.label),
                                chosen = option == sort,
                                onClick = { onSort(option) },
                            )
                        }
                    }
                    if (genres.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                        )
                        Text(
                            text = stringResource(R.string.favorites_genres),
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            color = CinemaMuted,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                genres.forEach { label ->
                                    FilterChip(
                                        label = label,
                                        chosen = label.lowercase() == genre,
                                        onClick = {
                                            onGenre(if (label.lowercase() == genre) null else label.lowercase())
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

@Composable
private fun FilterChip(
    label: String,
    chosen: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (chosen) Color.White else FilterNotes,
        fontWeight = if (chosen) FontWeight.Medium else FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (chosen) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember(label) { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun MenuArrow(
    open: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(220),
        label = "menuArrow",
    )
    Icon(
        imageVector = Icons.Filled.KeyboardArrowDown,
        contentDescription = stringResource(R.string.favorites_genres),
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
    )
}

private fun LazyListState.passedTab(): Int {
    val head = layoutInfo.visibleItemsInfo.firstOrNull() ?: return firstVisibleItemIndex
    return if (head.offset + head.size / 2 < 0) head.index + 1 else head.index
}

@Composable
private fun FavoritePoster(
    item: SavedTitle,
    badge: String? = null,
    progress: Float? = null,
    caption: String? = null,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    val tick = rememberHapticTick()
    var holdProgress by remember(item.id) { mutableFloatStateOf(0f) }
    var pressed by remember(item.id) { mutableStateOf(false) }
    val pressScale = rememberPressScale(pressed)
    val wave = remember(item.id) { PosterWave() }
    val showTitles = posterTitlesOn()
    Column {
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / 1.46f)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .clip(PosterShape)
                    .background(Color.White.copy(alpha = 0.06f)),
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = stringResource(R.string.poster_content_description, item.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .posterWave(wave)
                        .holdToPeek(
                            onPeek = { onMoviePeek(item.toMovie()) },
                            onProgress = { holdProgress = it },
                            onPressed = { pressed = it },
                            onClick = {
                                tick?.invoke()
                                onMovieClick(item.id)
                            },
                            wave = wave,
                        )
                        .holdCue(holdProgress),
                )
                if (progress != null || !caption.isNullOrBlank()) {
                    WatchProgress(
                        fraction = progress ?: 0f,
                        caption = caption,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            if (badge != null) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        if (showTitles) PosterCaption(item.title, item.year)
    }
}
