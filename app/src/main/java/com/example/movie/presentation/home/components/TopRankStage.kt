package com.example.movie.presentation.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.presentation.settings.LocalUserSettings
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.common.ImdbScore
import com.example.movie.presentation.common.KpScore
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(22.dp)
private const val CoverWidthFraction = 0.5f
private const val CoverAspect = 1.46f
private const val NeighborScale = 0.9f
private const val NeighborPull = 0.3f
private const val NeighborTurn = 46f
private val MetaSlot = 20.dp
private val ScoreSlot = 20.dp
private val OverviewSlot = 40.dp
private val Muted = Color(0xFF8E8E93)
private val TrackMuted = Color(0xFF5C5C62)

@Composable
internal fun TopRankStage(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color(0xFFB4B4B8),
            fontWeight = FontWeight.Normal,
            fontSize = CategoryTabFontSize,
            lineHeight = CategoryTabLineHeight,
            letterSpacing = CategoryTabLetterSpacing,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(14.dp))
        if (movies.isEmpty()) {
            TopRankSkeleton()
        } else {
            TopRankPager(
                movies = movies,
                onMovieClick = onMovieClick,
                onMoviePeek = onMoviePeek,
            )
        }
    }
}

@Composable
private fun TopRankPager(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    val pagerState = rememberPagerState { movies.size }
    val scope = rememberCoroutineScope()
    val tick = rememberHapticTick()
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPage by remember { mutableIntStateOf(-1) }
    HapticOnChange(pagerState) { if (scrubbing) Unit else pagerState.currentPage }
    LaunchedEffect(movies.size) {
        snapshotFlow { scrubPage }
            .collectLatest { page ->
                if (page >= 0) pagerState.scrollToPage(page.coerceIn(0, movies.lastIndex))
            }
    }
    LaunchedEffect(movies.size) {
        val last = movies.lastIndex
        if (last >= 0 && pagerState.currentPage > last) pagerState.scrollToPage(last)
    }
    val page = pagerState.currentPage.coerceIn(0, movies.lastIndex)
    val movie = movies[page]
    val drift = pagerState.currentPageOffsetFraction.absoluteValue
    val appear = smoothStep((1f - drift / 0.38f).coerceIn(0f, 1f))
    val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, movies.lastIndex.toFloat())

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = maxWidth * CoverWidthFraction
            val cardHeight = cardWidth * CoverAspect
            val side = ((maxWidth - cardWidth) / 2).coerceAtLeast(0.dp)
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = side),
                pageSize = PageSize.Fixed(cardWidth),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 2,
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight + 20.dp),
                key = { index -> movies[index].id },
            ) { index ->
                val item = movies[index]
                val pageOffset = (index - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                TopRankCard(
                    movie = item,
                    pageOffset = pageOffset,
                    onClick = {
                        tick?.invoke()
                        if (pageOffset.absoluteValue < 0.35f) {
                            onMovieClick(item.id)
                        } else {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    },
                    onPeek = { onMoviePeek(item) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = appear
                    translationY = (1f - appear) * 8.dp.toPx()
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onMovieClick(movie.id)
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = rankLabel(page),
                color = Color.White,
                fontSize = 48.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.72f)),
            )
            Spacer(Modifier.height(12.dp))
            val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
            var usingLogo by remember(movie.id, settings.showLogos, movie.logoUrl) {
                mutableStateOf(settings.showLogos && !movie.logoUrl.isNullOrBlank())
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (usingLogo) 52.dp else 26.dp)
                    .padding(horizontal = 32.dp),
                contentAlignment = if (usingLogo) Alignment.Center else Alignment.BottomCenter,
            ) {
                MovieLogoOrTitle(
                    movie = movie,
                    titleInsetExtra = 0.dp,
                    onUsingLogo = { usingLogo = it },
                )
            }
            Spacer(Modifier.height(if (usingLogo) 6.dp else 4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MetaSlot)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = movie.captionMeta(),
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ScoreSlot),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    movie.ratingKp?.let { score ->
                        KpScore(
                            score = score,
                            color = Color.White,
                            iconSize = 15.dp,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    movie.ratingImdb?.let { score ->
                        ImdbScore(
                            score = score,
                            color = Color.White,
                            iconHeight = 14.dp,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OverviewSlot)
                    .padding(horizontal = 32.dp)
                    .clipToBounds(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = movie.blurb(),
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        RankTrack(
            position = position,
            count = movies.size,
            onScrub = { fraction ->
                val last = movies.lastIndex
                if (last > 0) scrubPage = (fraction * last).roundToInt().coerceIn(0, last)
            },
            onScrubbing = { scrubbing = it },
        )
    }
}

@Composable
private fun TopRankCard(
    movie: Movie,
    pageOffset: Float,
    onClick: () -> Unit,
    onPeek: () -> Unit,
) {
    val distance = pageOffset.absoluteValue
    val clamped = distance.coerceIn(0f, 1f)
    val facing = 1f - clamped
    val active = distance < 0.12f
    var posterReady by remember(movie.id) { mutableStateOf(false) }
    var holdProgress by remember(movie.id) { mutableFloatStateOf(0f) }
    val shine = remember(movie.id) { Animatable(0f) }
    val travel = shine.value
    LaunchedEffect(active, posterReady) {
        if (!active || !posterReady) {
            shine.snapTo(0f)
            return@LaunchedEffect
        }
        delay(420)
        shine.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(2f - distance)
            .graphicsLayer {
                val scale = lerp(1f, NeighborScale, clamped) * (1f - 0.045f * holdProgress)
                scaleX = scale
                scaleY = scale
                val shift = pageOffset.coerceIn(-1f, 1f)
                rotationY = shift * NeighborTurn
                rotationZ = shift * -4f
                cameraDistance = 8.5f * density
                transformOrigin = TransformOrigin(0.5f, 0.62f)
                translationX = -shift * size.width * NeighborPull
                translationY = clamped * 18.dp.toPx()
                alpha = if (distance <= 1f) 1f else lerp(1f, 0f, (distance - 1f).coerceIn(0f, 1f))
            }
            .clip(CardShape)
            .holdToPeek(onPeek = onPeek, onProgress = { holdProgress = it })
            .holdCue(holdProgress)
            .clickable(
                interactionSource = remember(movie.id) { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .drawWithContent {
                drawContent()
                if (facing > 0.04f) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.28f * facing),
                        style = Stroke(width = 1.dp.toPx()),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                    )
                }
                if (!active || !posterReady || travel <= 0f || travel >= 1f) return@drawWithContent
                val envelope = sin(travel * PI.toFloat())
                val head = lerp(-0.35f, 1.2f, travel)
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.42f to Color.White.copy(alpha = 0.04f * envelope),
                            0.5f to Color.White.copy(alpha = 0.34f * envelope),
                            0.58f to Color.White.copy(alpha = 0.04f * envelope),
                            1f to Color.Transparent,
                        ),
                        start = Offset(size.width * head, 0f),
                        end = Offset(size.width * (head + 0.28f), size.height),
                    ),
                    blendMode = BlendMode.Screen,
                )
            },
    ) {
        AsyncImage(
            model = movie.posterUrl ?: movie.backdropUrl,
            contentDescription = stringResource(R.string.poster_content_description, movie.title),
            contentScale = ContentScale.Crop,
            onSuccess = { posterReady = true },
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1C1E)),
        )
        if (clamped > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = lerp(0f, 0.58f, clamped))),
            )
        }
    }
}

@Composable
private fun RankTrack(
    position: Float,
    count: Int,
    onScrub: (Float) -> Unit,
    onScrubbing: (Boolean) -> Unit,
) {
    if (count <= 1) return
    val fraction = (position / (count - 1)).coerceIn(0f, 1f)
    val thumb = 18.dp
    var dragging by remember { mutableStateOf(false) }
    val dragFraction = remember { mutableFloatStateOf(fraction) }
    val shown = if (dragging) dragFraction.floatValue else fraction
    val scrub = rememberUpdatedState(onScrub)
    val scrubbing = rememberUpdatedState(onScrubbing)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rankLabel(0),
            color = TrackMuted,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .height(40.dp)
                .pointerInput(count) {
                    val thumbPx = thumb.toPx()
                    val travel = (size.width - thumbPx).coerceAtLeast(1f)
                    fun fractionAt(x: Float) = ((x - thumbPx / 2f) / travel).coerceIn(0f, 1f)
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            scrubbing.value(true)
                            val next = fractionAt(offset.x)
                            dragFraction.floatValue = next
                            scrub.value(next)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val next = fractionAt(change.position.x)
                            dragFraction.floatValue = next
                            scrub.value(next)
                        },
                        onDragEnd = {
                            dragging = false
                            scrubbing.value(false)
                        },
                        onDragCancel = {
                            dragging = false
                            scrubbing.value(false)
                        },
                    )
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.14f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (maxWidth - thumb) * shown)
                    .size(thumb)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
        Text(
            text = rankLabel(count - 1),
            color = TrackMuted,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun TopRankSkeleton() {
    val transition = rememberInfiniteTransition(label = "top-rank-skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "top-rank-pulse",
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = maxWidth * CoverWidthFraction
            val cardHeight = cardWidth * CoverAspect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                contentAlignment = Alignment.Center,
            ) {
                SkeletonCard(cardWidth, cardHeight, shift = -(1f - NeighborPull), scale = NeighborScale, pulse = pulse * 0.65f, z = 0f)
                SkeletonCard(cardWidth, cardHeight, shift = 1f - NeighborPull, scale = NeighborScale, pulse = pulse * 0.65f, z = 0f)
                SkeletonCard(cardWidth, cardHeight, shift = 0f, scale = 1f, pulse = pulse, z = 1f)
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .width(84.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = pulse)),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = pulse)),
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = pulse)),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = pulse)),
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth()
                .height(OverviewSlot)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = pulse * 0.7f)),
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = pulse)),
        )
    }
}

@Composable
private fun SkeletonCard(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    shift: Float,
    scale: Float,
    pulse: Float,
    z: Float,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .zIndex(z)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = shift * size.width
            }
            .clip(CardShape)
            .background(Color.White.copy(alpha = pulse)),
    )
}

private fun Movie.captionMeta(): String = listOfNotNull(
    year?.toString(),
    genres
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString(", ")
        .takeIf { it.isNotEmpty() },
).joinToString(" · ")

private fun Movie.blurb(): String = overview.replace(Whitespace, " ").trim()

private val Whitespace = Regex("\\s+")

private fun rankLabel(index: Int): String = (index + 1).coerceAtLeast(1).toString()

private fun smoothStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
