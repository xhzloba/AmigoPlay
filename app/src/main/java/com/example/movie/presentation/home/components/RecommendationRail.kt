@file:OptIn(ExperimentalFoundationApi::class)

package com.example.movie.presentation.home.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.common.PosterImage
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.PosterWave
import com.example.movie.presentation.home.posterWave
import com.example.movie.presentation.home.pressToOpen
import com.example.movie.presentation.home.rememberPressScale
import com.example.movie.presentation.haptic.HapticOnAppend
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.PosterShape

private val PosterWidth = 112.dp
private val PosterAspect = 1.46f
private val RailStart = PosterGridInset
private val PosterGap = 14.dp

@Composable
internal fun RailTitle(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val tick = rememberHapticTick()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val white = settings.whiteTitles
    val color = if (white) Color.White else Color(0xFFB4B4B8)
    val titleSize = if (white) 18.sp else CategoryTabFontSize
    val iconSize = with(LocalDensity.current) { titleSize.toDp() }
    Row(
        modifier = modifier
            .padding(horizontal = PosterGridInset)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onClick()
                        },
                    )
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            color = color,
            fontWeight = if (white) FontWeight.Bold else FontWeight.Normal,
            fontSize = titleSize,
            lineHeight = if (white) TextUnit.Unspecified else CategoryTabLineHeight,
            letterSpacing = if (white) TextUnit.Unspecified else CategoryTabLetterSpacing,
        )
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = ShellIcons.chevron,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
internal fun RecommendationRail(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    leadingIcon: ImageVector? = null,
    onOpen: (() -> Unit)? = null,
    onSettled: () -> Unit = {},
    stickToStart: Boolean = false,
    progressOf: (Movie) -> Float? = { null },
    captionOf: (Movie) -> String? = { null },
) {
    HapticOnAppend(movies.map { it.id })
    Column(modifier = modifier.fillMaxWidth().graphicsLayer()) {
        if (title != null) {
            RailTitle(title = title, onClick = onOpen, leadingIcon = leadingIcon)
            Spacer(Modifier.height(10.dp))
        }
        if (movies.isEmpty()) {
            RecommendationRailSkeleton()
        } else {
            RecommendationPager(
                movies = movies,
                onMovieClick = onMovieClick,
                onMoviePeek = onMoviePeek,
                onOpen = onOpen,
                onSettled = onSettled,
                stickToStart = stickToStart,
                progressOf = progressOf,
                captionOf = captionOf,
            )
        }
    }
}

@Composable
internal fun RecommendationRailSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val posterWidth = posterSize(PosterWidth)
            val posterHeight = posterWidth * PosterAspect
            val showTitles = posterTitlesOn()
            val rowHeight = posterHeight + WatchedBadgeHang + if (showTitles) PosterCaptionBlock else 0.dp
            val count = ((maxWidth - RailStart) / (posterWidth + PosterGap)).toInt().coerceIn(2, 4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(start = RailStart),
                horizontalArrangement = Arrangement.spacedBy(PosterGap),
                verticalAlignment = Alignment.Top,
            ) {
                repeat(count) {
                    Column(Modifier.width(posterWidth)) {
                        Box(
                            modifier = Modifier
                                .width(posterWidth)
                                .height(posterHeight)
                                .clip(PosterShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                        )
                        Spacer(Modifier.height(WatchedBadgeHang))
                        if (showTitles) PosterCaptionSkeleton()
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationPager(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onOpen: (() -> Unit)? = null,
    onSettled: () -> Unit,
    stickToStart: Boolean = false,
    progressOf: (Movie) -> Float? = { null },
    captionOf: (Movie) -> String? = { null },
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val head = movies.firstOrNull()?.id
    LaunchedEffect(head) {
        if (stickToStart && head != null) listState.scrollToItem(0)
    }
    val tick = rememberHapticTick()
    SettleEffect(listState, { listState.isScrollInProgress }, onSettled)
    HapticOnChange(listState) { listState.firstVisibleItemIndex }
    val decay = rememberSplineBasedDecay<Float>()
    val snapping = remember(listState) { SnapLayoutInfoProvider(listState, SnapPosition.Start) }
    val flingBehavior = remember(snapping, decay) {
        snapFlingBehavior(
            snapLayoutInfoProvider = snapping,
            decayAnimationSpec = decay,
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    val posterWidth = posterSize(PosterWidth)
    val posterHeight = posterWidth * PosterAspect
    val showTitles = posterTitlesOn()
    val watched = rememberWatchedIds()
    val favorites = rememberFavoriteIds()
    val rowHeight = posterHeight + WatchedBadgeHang + if (showTitles) PosterCaptionBlock else 0.dp
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Плашка «Все» занимает хвост после последнего постера, когда доскроллили до конца.
        val trail = onOpen != null
        val endPad = RailStart
        val trailWidth = if (trail) {
            (maxWidth - RailStart - posterWidth - PosterGap - endPad).coerceAtLeast(72.dp)
        } else {
            0.dp
        }
        val end = if (trail) {
            endPad
        } else {
            (maxWidth - posterWidth - RailStart).coerceAtLeast(RailStart)
        }
        CompositionLocalProvider(LocalOverscrollConfiguration provides OverscrollConfiguration()) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(start = RailStart, end = end),
            horizontalArrangement = Arrangement.spacedBy(PosterGap),
            flingBehavior = flingBehavior,
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
        ) {
            itemsIndexed(movies, key = { _, movie -> movie.id }) { _, movie ->
                var holdProgress by remember(movie.id) { mutableFloatStateOf(0f) }
                var pressed by remember(movie.id) { mutableStateOf(false) }
                val pressScale = rememberPressScale(pressed)
                val wave = remember(movie.id) { PosterWave() }
                Column(Modifier.width(posterWidth)) {
                    Box(
                        Modifier
                            .width(posterWidth)
                            .height(posterHeight)
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            },
                    ) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .clip(PosterShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                        ) {
                        PosterImage(
                            url = movie.posterUrl,
                            contentDescription = stringResource(R.string.poster_content_description, movie.title),
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
                        val fraction = progressOf(movie)
                        val caption = captionOf(movie)
                        if (fraction != null || !caption.isNullOrBlank()) {
                            WatchProgress(
                                fraction = fraction ?: 0f,
                                caption = caption,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                        }
                        if (movie.id in watched) {
                            WatchedBadge(Modifier.align(Alignment.BottomEnd))
                        } else if (movie.id in favorites) {
                            FavoriteBadge(Modifier.align(Alignment.BottomEnd))
                        }
                    }
                    Spacer(Modifier.height(WatchedBadgeHang))
                    if (showTitles) PosterCaption(movie.title, movie.year)
                }
            }
            if (onOpen != null) {
                item(key = "rail-see-all") {
                    RailSeeAllCard(
                        width = trailWidth,
                        height = posterHeight,
                        onClick = {
                            tick?.invoke()
                            onOpen()
                        },
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun RailSeeAllCard(
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val pressScale = rememberPressScale(pressed)
    val wave = remember { PosterWave() }
    Column(Modifier.width(width)) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(PosterShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .posterWave(wave)
                .pressToOpen(
                    onPressed = { pressed = it },
                    onClick = onClick,
                    wave = wave,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.rail_see_all),
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = ShellIcons.chevron,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
