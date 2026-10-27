package com.example.movie.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.common.ImdbScore
import com.example.movie.presentation.common.KpScore
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface

private val CardShape = RoundedCornerShape(28.dp)
private val Notes = Color(0xFFD2D2D8)

@Composable
internal fun MoviePeek(
    movie: Movie?,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = movie != null, onBack = onDismiss)
    AnimatedVisibility(
        visible = movie != null,
        enter = fadeIn(tween(180)) + scaleIn(
            animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
            initialScale = 0.96f,
        ),
        exit = fadeOut(tween(140)) + scaleOut(animationSpec = tween(140), targetScale = 0.98f),
    ) {
        val shown = movie ?: return@AnimatedVisibility
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
            PeekCard(
                movie = shown,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 22.dp),
            )
        }
    }
}

@Composable
private fun PeekCard(movie: Movie, modifier: Modifier = Modifier) {
    val meta = listOfNotNull(
        movie.year?.toString(),
        movie.genres.takeIf { it.isNotBlank() },
        movie.country.takeIf { it.isNotBlank() },
        movie.age?.let { stringResource(R.string.age_badge, it) },
    ).joinToString("  ·  ")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CinemaSurface)
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), CardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = movie.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.4).sp,
            textAlign = TextAlign.Center,
        )
        if (movie.originalTitle.isNotBlank() && movie.originalTitle != movie.title) {
            Text(
                text = movie.originalTitle,
                modifier = Modifier.padding(top = 4.dp),
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                modifier = Modifier.padding(top = 12.dp),
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (movie.director.isNotBlank()) {
            Text(
                text = movie.director,
                modifier = Modifier.padding(top = 6.dp),
                color = Notes,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (movie.ratingKp != null || movie.ratingImdb != null) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                movie.ratingKp?.let { score ->
                    KpScore(
                        score = score,
                        color = Color.White,
                        iconSize = 18.dp,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                movie.ratingImdb?.let { score ->
                    ImdbScore(
                        score = score,
                        color = Color.White,
                        iconHeight = 18.dp,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        if (movie.overview.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = movie.overview,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                color = Notes,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Start,
            )
        }
    }
}
