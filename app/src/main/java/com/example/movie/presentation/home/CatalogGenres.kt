package com.example.movie.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.presentation.common.appBackground

internal const val GenreAll = "Все"

internal val CatalogGenres = listOf(
    GenreAll,
    "боевик",
    "фэнтези",
    "фантастика",
    "триллер",
    "военный",
    "детектив",
    "комедия",
    "драма",
    "ужасы",
    "криминал",
    "мелодрама",
    "вестерн",
    "биография",
    "аниме",
    "детский",
    "мультфильм",
    "фильм-нуар",
    "для взрослых",
    "документальный",
    "игра",
    "история",
    "концерт",
    "короткометражка",
    "музыка",
    "мюзикл",
    "приключения",
    "семейный",
    "спорт",
    "ток-шоу",
)

@Composable
internal fun CatalogGenresPage(
    onBack: () -> Unit,
    onGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val header = top + BrandTopInset + 36.dp + 18.dp
    Box(
        modifier
            .fillMaxSize()
            .appBackground()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = PosterGridInset,
                end = PosterGridInset,
                top = header,
                bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(CatalogGenres, key = { it }) { name ->
                GenreTile(name = name, onClick = { onGenre(name) })
            }
        }
        GenresBar(onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun GenreTile(name: String, onClick: () -> Unit) {
    val tick = rememberHapticTick()
    var pressed by remember(name) { mutableStateOf(false) }
    val scale = rememberPressScale(pressed)
    val wave = remember(name) { PosterWave() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .posterWave(wave, light = true)
            .pressToOpen(
                onPressed = { pressed = it },
                onClick = {
                    tick?.invoke()
                    onClick()
                },
                wave = wave,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun GenresBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val tick = rememberHapticTick()
    Box(
        modifier
            .fillMaxWidth()
            .appBackground()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = BrandTopInset)
            .height(36.dp),
    ) {
        Text(
            text = stringResource(R.string.catalog_genres),
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = PosterGridInset - 7.dp)
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onBack()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShellIcons.back,
                contentDescription = stringResource(R.string.section_back),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
