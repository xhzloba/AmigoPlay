package com.example.movie.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaSurface

private val CardShape = RoundedCornerShape(22.dp)
private val PosterShape = RoundedCornerShape(10.dp)
private val PosterWidth = 54.dp
private val PosterAspect = 1.46f

private data class FanSlot(val x: Int, val y: Int, val tilt: Float)

private val Fan = listOf(
    FanSlot(x = 0, y = 2, tilt = -8f),
    FanSlot(x = 36, y = -6, tilt = 0f),
    FanSlot(x = 70, y = 4, tilt = 8f),
)

@Composable
internal fun CatalogPlaque(
    movies: List<Movie>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val tick = rememberHapticTick()
    val covers = movies.take(3)
    Row(
        modifier = modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(CinemaSurface)
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), CardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onClick()
                },
            )
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.catalog_plaque_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.4).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.catalog_plaque_hint),
                color = Color(0xFF8A8A90),
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                letterSpacing = (-0.1).sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        PosterFan(covers)
    }
}

@Composable
private fun PosterFan(movies: List<Movie>) {
    val posterHeight = PosterWidth * PosterAspect
    Box(
        modifier = Modifier
            .width(124.dp)
            .height(posterHeight + 8.dp),
    ) {
        val drawOrder = listOf(0, 2, 1)
        drawOrder.forEach { index ->
            val slot = Fan[index]
            val movie = movies.getOrNull(index)
            val poster = Modifier
                .align(Alignment.BottomStart)
                .offset(x = slot.x.dp, y = slot.y.dp)
                .zIndex(if (index == 1) 1f else 0f)
                .graphicsLayer { rotationZ = slot.tilt }
                .width(PosterWidth)
                .height(posterHeight)
                .clip(PosterShape)
                .background(Color.White.copy(alpha = 0.08f))
            if (movie == null) {
                Box(poster)
            } else {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = stringResource(R.string.poster_content_description, movie.title),
                    contentScale = ContentScale.Crop,
                    modifier = poster,
                )
            }
        }
    }
}
