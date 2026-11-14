package com.example.movie.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.presentation.shell.ShellIcons

@Composable
internal fun PosterImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    iconSize: Dp = 28.dp,
    showLabel: Boolean = true,
    onSuccess: (() -> Unit)? = null,
) {
    // LazyRow выбрасывает ячейку при скролле. remember сбрасывал failed,
    // AsyncImage стартовал с пустого кадра и плейсхолдер мигал. Saveable
    // держит уже решённый промах; новый url по-прежнему грузится с нуля.
    var failed by rememberSaveable(url) { mutableStateOf(url.isNullOrBlank()) }
    LaunchedEffect(failed) {
        if (failed) onSuccess?.invoke()
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!failed) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onError = { failed = true },
                onSuccess = { onSuccess?.invoke() },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 6.dp),
            ) {
                Icon(
                    imageVector = ShellIcons.trailers,
                    contentDescription = contentDescription,
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(iconSize),
                )
                if (showLabel) {
                    Text(
                        text = stringResource(R.string.no_poster),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
