@file:OptIn(ExperimentalFoundationApi::class)

package com.example.movie.presentation.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
import com.example.movie.presentation.common.appChrome
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
import kotlinx.coroutines.flow.distinctUntilChanged

private val PosterWidth = 112.dp
private val PosterAspect = 1.46f
private val RailStart = PosterGridInset
private val PosterGap = 14.dp
private val SideRetouch = 56.dp

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
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val sideRetouch = settings.sliderShade
    val ink = appChrome()
    val scrolling = listState.isScrollInProgress
    // +1: контент уходит влево (листаем влево) → ретушь слева
    // -1: контент уходит вправо (листаем вправо) → ретушь справа
    var scrollDir by remember { mutableIntStateOf(0) }
    LaunchedEffect(listState, sideRetouch) {
        if (!sideRetouch) {
            scrollDir = 0
            return@LaunchedEffect
        }
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                val delta = when {
                    index > lastIndex -> 1
                    index < lastIndex -> -1
                    offset > lastOffset -> 1
                    offset < lastOffset -> -1
                    else -> 0
                }
                if (delta != 0) scrollDir = delta
                lastIndex = index
                lastOffset = offset
            }
    }
    val leftTarget = if (sideRetouch && scrolling && scrollDir > 0) 1f else 0f
    val rightTarget = if (sideRetouch && scrolling && scrollDir < 0) 1f else 0f
    val leftStrength by animateFloatAsState(
        targetValue = leftTarget,
        animationSpec = tween(
            durationMillis = if (leftTarget > 0f) 520 else 640,
            easing = FastOutSlowInEasing,
        ),
        label = "railRetouchL",
    )
    val rightStrength by animateFloatAsState(
        targetValue = rightTarget,
        animationSpec = tween(
            durationMillis = if (rightTarget > 0f) 520 else 640,
            easing = FastOutSlowInEasing,
        ),
        label = "railRetouchR",
    )
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
                .height(rowHeight)
                .then(
                    if (leftStrength > 0.01f || rightStrength > 0.01f) {
                        Modifier.railSideRetouch(
                            ink = ink,
                            width = SideRetouch,
                            left = leftStrength,
                            right = rightStrength,
                        )
                    } else {
                        Modifier
                    },
                ),
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

/** Ретушь на стороне, куда уходит контент: влево → слева, вправо → справа. */
private fun Modifier.railSideRetouch(
    ink: Color,
    width: Dp,
    left: Float,
    right: Float,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val leftAmount = left.coerceIn(0f, 1f)
            val rightAmount = right.coerceIn(0f, 1f)
            if (leftAmount <= 0.01f && rightAmount <= 0.01f) return@drawWithContent
            val w = width.toPx()
            if (w < 1f) return@drawWithContent
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 1f - leftAmount),
                        (w / size.width).coerceIn(0f, 0.45f) to Color.Black,
                        (1f - w / size.width).coerceIn(0.55f, 1f) to Color.Black,
                        1f to Color.Black.copy(alpha = 1f - rightAmount),
                    ),
                ),
                blendMode = BlendMode.DstIn,
            )
            if (leftAmount > 0.01f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to ink.copy(alpha = 0.78f * leftAmount),
                            0.55f to ink.copy(alpha = 0.3f * leftAmount),
                            1f to Color.Transparent,
                        ),
                        endX = w,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6A7A9A).copy(alpha = 0.28f * leftAmount),
                            Color.Transparent,
                        ),
                        center = Offset(0f, size.height * 0.45f),
                        radius = w * 1.4f,
                    ),
                    blendMode = BlendMode.Overlay,
                )
            }
            if (rightAmount > 0.01f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.45f to ink.copy(alpha = 0.3f * rightAmount),
                            1f to ink.copy(alpha = 0.78f * rightAmount),
                        ),
                        startX = size.width - w,
                        endX = size.width,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6A7A9A).copy(alpha = 0.28f * rightAmount),
                            Color.Transparent,
                        ),
                        center = Offset(size.width, size.height * 0.45f),
                        radius = w * 1.4f,
                    ),
                    blendMode = BlendMode.Overlay,
                )
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
