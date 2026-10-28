package com.example.movie.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaSurface

private val CardShape = RoundedCornerShape(22.dp)
private val CardHeight = 132.dp
private val PosterWidth = 84.dp
private val PosterGap = 3.dp
private val FadeWidth = 56.dp

@Composable
internal fun CatalogPlaque(
    movies: List<Movie>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val tick = rememberHapticTick()
    val covers = List(3) { movies.getOrNull(it) }
    val stack = PosterWidth * covers.size + PosterGap * (covers.size - 1)
    Box(
        modifier = modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .height(CardHeight)
            .clip(CardShape)
            .background(CinemaSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onClick()
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(stack)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(PosterGap),
        ) {
            covers.forEach { movie ->
                if (movie == null) {
                    Box(
                        modifier = Modifier
                            .width(PosterWidth)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.06f)),
                    )
                } else {
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = stringResource(R.string.poster_content_description, movie.title),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(PosterWidth)
                            .fillMaxHeight(),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(FadeWidth)
                .fillMaxHeight()
                .offset(x = -(stack - FadeWidth))
                .background(
                    Brush.horizontalGradient(
                        listOf(CinemaSurface, Color.Transparent),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .width(150.dp),
        ) {
            Text(
                text = stringResource(R.string.catalog_plaque_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.catalog_plaque_hint),
                color = Color(0xFFB4B4B8),
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                letterSpacing = (-0.1).sp,
            )
        }
    }
}
