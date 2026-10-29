package com.example.movie.presentation.home.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.movie.R
import com.example.movie.domain.model.ContentKind
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.common.ImdbScore
import com.example.movie.presentation.common.KpScore
import com.example.movie.presentation.detail.drawPosterWash
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.rememberPressScale
import com.example.movie.presentation.palette.PosterWash
import com.example.movie.presentation.palette.posterWash
import com.example.movie.ui.theme.PosterShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CardShape = RoundedCornerShape(18.dp)
private val Ink = Color(0xFF121212)
private val BodyColor = Color(0xFFE4E0E2)
private val MetaColor = Color(0xFFC8C4C6)
private val BackdropHeight = 196.dp
private val ThumbWidth = 42.dp
private val ThumbHeight = 62.dp
private val CardHeight = 408.dp

@Composable
internal fun WatchingRail(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    title: String,
    onSettled: () -> Unit = {},
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
        Spacer(Modifier.height(12.dp))
        if (movies.isEmpty()) {
            WatchingCardSkeleton(Modifier.padding(start = 20.dp, end = 40.dp))
        } else {
            WatchingPager(
                movies = movies,
                onMovieClick = onMovieClick,
                onMoviePeek = onMoviePeek,
                onSettled = onSettled,
            )
        }
    }
}

@Composable
private fun WatchingPager(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onSettled: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { movies.size })
    val tick = rememberHapticTick()
    SettleEffect(pagerState, { pagerState.isScrollInProgress }, onSettled)
    HapticOnChange(pagerState) { pagerState.currentPage }
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(start = 20.dp, end = 40.dp),
        pageSpacing = 12.dp,
        beyondViewportPageCount = 1,
        key = { movies[it].id },
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight),
    ) { index ->
        val movie = movies[index]
        WatchingCard(
            movie = movie,
            onClick = {
                tick?.invoke()
                onMovieClick(movie.id)
            },
            onPeek = { onMoviePeek(movie) },
        )
    }
}

@Composable
private fun WatchingCard(
    movie: Movie,
    onClick: () -> Unit,
    onPeek: () -> Unit,
) {
    var holdProgress by remember(movie.id) { mutableFloatStateOf(0f) }
    var pressed by remember(movie.id) { mutableStateOf(false) }
    val pressScale = rememberPressScale(pressed)
    val backdrop = movie.backdropUrl ?: movie.posterUrl
    val wash = rememberWash(backdrop)
    val reveal = remember(movie.id) { Animatable(0f) }
    LaunchedEffect(wash) {
        if (wash == null) reveal.snapTo(0f) else reveal.animateTo(1f, tween(280))
    }
    val washAlpha = reveal.value
    val lead = movie.director.ifBlank { stringResource(movie.kind.label()) }
    val genres = movie.genresLine()
    val body = movie.overview.ifBlank { genres }
    val meta = movie.metaLine(if (movie.overview.isNotBlank()) genres else "")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CardShape)
            .drawBehind {
                drawRect(Ink)
                val colors = wash
                if (colors != null) {
                    drawPosterWash(colors.base, colors.right, colors.accent, washAlpha)
                }
            }
            .holdToPeek(
                onPeek = onPeek,
                onProgress = { holdProgress = it },
                onPressed = { pressed = it },
            )
            .holdCue(holdProgress)
            .clickable(
                interactionSource = remember(movie.id) { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        if (backdrop == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(BackdropHeight)
                    .background(Color.White.copy(alpha = 0.06f)),
            )
        } else {
            AsyncImage(
                model = backdrop,
                contentDescription = stringResource(R.string.poster_content_description, movie.title),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BackdropHeight)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black,
                                0.58f to Color.Black,
                                0.78f to Color.Black.copy(alpha = 0.45f),
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-16).dp),
                contentAlignment = Alignment.Center,
            ) {
                MovieLogoOrTitle(
                    movie = movie,
                    titleFontSize = 28.sp,
                    titleLineHeight = 34.sp,
                    titleInsetExtra = 0.dp,
                    titleAlign = TextAlign.Center,
                    logoAlignment = Alignment.Center,
                )
            }
            if (body.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = body,
                    color = BodyColor,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(ThumbWidth)
                        .height(ThumbHeight)
                        .clip(PosterShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = lead,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (meta.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = meta,
                            color = MetaColor,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (movie.ratingKp != null || movie.ratingImdb != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            movie.ratingKp?.let { score ->
                                KpScore(
                                    score = score,
                                    color = Color.White,
                                    iconSize = 14.dp,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            movie.ratingImdb?.let { score ->
                                ImdbScore(
                                    score = score,
                                    color = Color.White,
                                    iconHeight = 13.dp,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberWash(imageUrl: String?): PosterWash? {
    val context = LocalContext.current
    var wash by remember(imageUrl) { mutableStateOf<PosterWash?>(null) }
    LaunchedEffect(imageUrl) {
        wash = null
        val url = imageUrl ?: return@LaunchedEffect
        val result = context.imageLoader.execute(
            ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(160, 90)
                .build(),
        )
        val source = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        val bitmap = if (source.config == android.graphics.Bitmap.Config.HARDWARE) {
            source.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return@LaunchedEffect
        } else {
            source
        }
        val colors = withContext(Dispatchers.Default) { bitmap.posterWash() }
        if (bitmap !== source) bitmap.recycle()
        wash = colors
    }
    return wash
}

@Composable
private fun WatchingCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(CardShape)
            .background(Ink),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(BackdropHeight)
                .background(Color.White.copy(alpha = 0.06f)),
        )
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(0.62f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
            )
        }
    }
}

private fun ContentKind.label(): Int = when (this) {
    ContentKind.Movie -> R.string.kind_movie
    ContentKind.Series -> R.string.kind_series
    ContentKind.Anime -> R.string.kind_anime
    ContentKind.Other -> R.string.kind_movie
}

private fun Movie.genresLine(): String =
    genres.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString(", ")

private fun Movie.metaLine(genres: String): String = when {
    year != null && genres.isNotBlank() -> "$year · $genres"
    year != null -> year.toString()
    else -> genres
}
