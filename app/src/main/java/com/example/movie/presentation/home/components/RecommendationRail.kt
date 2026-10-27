package com.example.movie.presentation.home.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick

private val PosterWidth = 112.dp
private val PosterAspect = 1.46f
private val RailStart = 20.dp
private val PosterGap = 14.dp
private val PosterShape = RoundedCornerShape(16.dp)

@Composable
internal fun RecommendationRail(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                color = Color(0xFFB4B4B8),
                fontWeight = FontWeight.Normal,
                fontSize = CategoryTabFontSize,
                lineHeight = CategoryTabLineHeight,
                letterSpacing = CategoryTabLetterSpacing,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(10.dp))
        }
        if (movies.isEmpty()) {
            RecommendationRailSkeleton()
        } else {
            RecommendationPager(
                movies = movies,
                onMovieClick = onMovieClick,
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
            val posterHeight = PosterWidth * PosterAspect
            val count = ((maxWidth - RailStart) / (PosterWidth + PosterGap)).toInt().coerceIn(2, 4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(posterHeight)
                    .padding(start = RailStart),
                horizontalArrangement = Arrangement.spacedBy(PosterGap),
                verticalAlignment = Alignment.Bottom,
            ) {
                repeat(count) {
                    Box(
                        modifier = Modifier
                            .width(PosterWidth)
                            .height(posterHeight)
                            .clip(PosterShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationPager(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val tick = rememberHapticTick()
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
    val posterHeight = PosterWidth * PosterAspect
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val end = (maxWidth - PosterWidth - RailStart).coerceAtLeast(RailStart)
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(start = RailStart, end = end),
            horizontalArrangement = Arrangement.spacedBy(PosterGap),
            flingBehavior = flingBehavior,
            modifier = Modifier
                .fillMaxWidth()
                .height(posterHeight),
        ) {
            itemsIndexed(movies, key = { _, movie -> movie.id }) { _, movie ->
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = stringResource(R.string.poster_content_description, movie.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(PosterWidth)
                        .height(posterHeight)
                        .clip(PosterShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(
                            interactionSource = remember(movie.id) { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                onMovieClick(movie.id)
                            },
                        ),
                )
            }
        }
    }
}
