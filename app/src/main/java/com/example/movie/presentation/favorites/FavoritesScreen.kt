package com.example.movie.presentation.favorites

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.toMovie
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.BrandWordmark
import com.example.movie.presentation.home.components.CategoryTabFontSize
import com.example.movie.presentation.home.components.CategoryTabLetterSpacing
import com.example.movie.presentation.home.components.CategoryTabLineHeight
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface

private val PosterShape = RoundedCornerShape(16.dp)
private val FavoritePlay = Color(0xFFE23B5A)
private val CapsuleInk = Color.White.copy(alpha = 0.72f)
private val MenuShape = RoundedCornerShape(16.dp)

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
) {
    val items by LocalAppContainer.current.favorites.items.collectAsStateWithLifecycle()
    var tabName by rememberSaveable { mutableStateOf(FavoriteTab.All.name) }
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    val tab = FavoriteTab.valueOf(tabName)
    val inTab = tab.type?.let { type -> items.filter { it.type == type } } ?: items
    val genres = if (tab.hasGenres) inTab.genreLabels() else emptyList()
    val shown = genre?.let { name -> inTab.filter { name in it.genreKeys() } } ?: inTab
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = 32.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp - BrandLogoBleed, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandWordmark(play = FavoritePlay)
            Spacer(Modifier.weight(1f))
            LibraryMenu()
        }
        FavoriteTabs(
            selected = tab,
            genres = genres,
            genre = genre,
            onSelect = {
                genre = null
                tabName = it.name
            },
            onGenre = { genre = it },
            modifier = Modifier.padding(top = 12.dp),
        )
        if (shown.isEmpty()) {
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
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shown, key = { it.id }) { item ->
                    FavoritePoster(
                        item = item,
                        onMovieClick = onMovieClick,
                        onMoviePeek = onMoviePeek,
                    )
                }
            }
        }
    }
}

private enum class LibrarySection(@StringRes val label: Int) {
    Favorites(R.string.nav_favorites),
    Watched(R.string.favorites_watched),
    Actors(R.string.favorites_actors),
}

@Composable
private fun LibraryMenu() {
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
                text = stringResource(R.string.nav_favorites),
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
                val lit = section == LibrarySection.Favorites
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
                            onClick = { open = false },
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

@Composable
private fun FavoriteTabs(
    selected: FavoriteTab,
    genres: List<String>,
    genre: String?,
    onSelect: (FavoriteTab) -> Unit,
    onGenre: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var menuOpen by remember { mutableStateOf(false) }
    HapticOnChange(listState) { listState.passedTab() }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(FavoriteTab.entries, key = { it.name }) { tab ->
            val lit = tab == selected
            val title = stringResource(tab.label)
            val picked = genres.firstOrNull { it.lowercase() == genre }
            val canOpen = lit && tab.hasGenres && genres.isNotEmpty()
            Box {
                Row(
                    modifier = Modifier.clickable(
                        interactionSource = remember(tab.name) { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (canOpen) {
                                menuOpen = !menuOpen
                            } else {
                                menuOpen = false
                                onSelect(tab)
                            }
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
                    if (canOpen) {
                        MenuArrow(
                            open = menuOpen,
                            tint = if (picked == null) Color.White else CapsuleInk,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                DropdownMenu(
                    expanded = canOpen && menuOpen,
                    onDismissRequest = { menuOpen = false },
                    offset = DpOffset(0.dp, 10.dp),
                    containerColor = CinemaSurface,
                    shape = MenuShape,
                ) {
                    genres.forEach { label ->
                        val chosen = label.lowercase() == genre
                        Text(
                            text = label,
                            color = if (chosen) CapsuleInk else CinemaMuted,
                            fontWeight = if (chosen) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember(label) { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        onGenre(if (chosen) null else label.lowercase())
                                        menuOpen = false
                                    },
                                )
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
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
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    val tick = rememberHapticTick()
    var holdProgress by remember(item.id) { mutableFloatStateOf(0f) }
    AsyncImage(
        model = item.posterUrl,
        contentDescription = stringResource(R.string.poster_content_description, item.title),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f / 1.46f)
            .graphicsLayer {
                val scale = 1f - 0.045f * holdProgress
                scaleX = scale
                scaleY = scale
            }
            .clip(PosterShape)
            .background(Color.White.copy(alpha = 0.06f))
            .holdToPeek(
                onPeek = { onMoviePeek(item.toMovie()) },
                onProgress = { holdProgress = it },
            )
            .holdCue(holdProgress)
            .clickable(
                interactionSource = remember(item.id) { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onMovieClick(item.id)
                },
            ),
    )
}
