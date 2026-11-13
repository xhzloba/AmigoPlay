package com.example.movie.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.presentation.common.THEME_GRAPHITE
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.RailTitle
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.ShellIcons
import java.util.Locale

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

/** В «Фильмы» аниме не показываем — у него своя вкладка. */
internal fun catalogGenresFor(type: String): List<String> =
    if (type == "movie") CatalogGenres.filter { it != "аниме" } else CatalogGenres

private val GenreChipShape = RoundedCornerShape(14.dp)
private val GenreChipCinema = Color.White.copy(alpha = 0.08f)
private val GenreChipGraphite = Color(0xFF2A2E38)
private val GenreChipBorderCinema = Color.White.copy(alpha = 0.06f)
private val GenreChipBorderGraphite = Color.White.copy(alpha = 0.10f)

@Composable
internal fun CatalogGenreChips(
    type: String,
    onOpenAll: () -> Unit,
    onGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val graphite = settings.appTheme == THEME_GRAPHITE
    val fill = if (graphite) GenreChipGraphite else GenreChipCinema
    val stroke = if (graphite) GenreChipBorderGraphite else GenreChipBorderCinema
    val genres = remember(type) { catalogGenresFor(type).filter { it != GenreAll } }
    val row = rememberLazyListState()
    HapticOnChange(row) { row.firstVisibleItemIndex }
    Column(modifier.fillMaxWidth()) {
        RailTitle(
            title = stringResource(R.string.catalog_genres),
            onClick = onOpenAll,
            leadingIcon = ShellIcons.genres,
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            state = row,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = PosterGridInset),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(genres, key = { it }) { name ->
                GenreChip(
                    name = name,
                    fill = fill,
                    stroke = stroke,
                    onClick = { onGenre(name) },
                )
            }
        }
    }
}

@Composable
private fun GenreChip(
    name: String,
    fill: Color,
    stroke: Color,
    onClick: () -> Unit,
) {
    val tick = rememberHapticTick()
    Text(
        text = name.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale("ru")) else ch.toString()
        },
        color = Color.White.copy(alpha = 0.92f),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(GenreChipShape)
            .background(fill)
            .border(0.5.dp, stroke, GenreChipShape)
            .clickable(
                interactionSource = remember(name) { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onClick()
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
internal fun CatalogGenresPage(
    type: String,
    onBack: () -> Unit,
    onGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val header = top + BrandTopInset + 36.dp + 18.dp
    val genres = remember(type) { catalogGenresFor(type) }
    val grid = rememberLazyGridState()
    val density = LocalDensity.current
    val scrim = if (grid.firstVisibleItemIndex > 0) {
        1f
    } else {
        val threshold = with(density) { 28.dp.toPx() }
        (grid.firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
    }
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
            state = grid,
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
            items(genres, key = { it }) { name ->
                GenreTile(name = name, onClick = { onGenre(name) })
            }
        }
        // Та же растушёвка шапки, что у полного списка (cinema / graphite).
        SectionScrim(progress = scrim, modifier = Modifier.align(Alignment.TopCenter))
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
