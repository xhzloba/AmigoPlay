package com.example.movie.presentation.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.ContentKind
import com.example.movie.domain.model.FeedCard
import com.example.movie.domain.model.FeedKind
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.SeriesAlert
import com.example.movie.domain.model.kept
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.RecommendationRail
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PosterShape
import java.time.Instant
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun AlertsScreen(
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = LocalAppContainer.current.releaseWatch.store
    val alerts by store.alerts.collectAsStateWithLifecycle()
    val shelves by store.shelves.collectAsStateWithLifecycle()
    val shown = FeedKind.all.mapNotNull { kind ->
        shelves.firstOrNull { it.kind == kind && it.cards.isNotEmpty() && it.kept() }
    }
    var settings by remember { mutableStateOf(false) }
    val tick = rememberHapticTick()
    LaunchedEffect(shelves) { store.markShelvesRead() }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(top = top + 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
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
            Text(
                text = stringResource(R.string.nav_alerts),
                color = Color.White,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            settings = true
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ShellIcons.gear,
                    contentDescription = stringResource(R.string.alerts_settings),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (settings) FeedSheet(onDismiss = { settings = false })
        if (alerts.isEmpty() && shown.isEmpty()) {
            Text(
                text = stringResource(R.string.alerts_empty),
                modifier = Modifier.padding(horizontal = PosterGridInset, vertical = 18.dp),
                color = CinemaMuted,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
        } else {
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(shown, key = { it.kind }) { shelf ->
                    Column {
                        RecommendationRail(
                            movies = shelf.cards.map { it.toMovie() },
                            title = stringResource(
                                R.string.alerts_shelf_count,
                                stringResource(categoryTitle(shelf.kind)),
                                shelf.cards.size,
                            ),
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                }
                items(alerts, key = { it.id }) { alert ->
                    AlertRow(alert) {
                        store.markMovie(alert.movieId)
                        onMovieClick(alert.movieId)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertRow(alert: SeriesAlert, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember(alert.id) { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = PosterGridInset, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = alert.posterUrl,
            contentDescription = null,
            modifier = Modifier
                .width(46.dp)
                .height(68.dp)
                .clip(PosterShape)
                .background(Color(0xFF1C1C1E)),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
        ) {
            Text(
                text = alert.title,
                color = if (alert.read) CinemaMuted else Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(alert.textRes(), alert.season, alert.episode),
                modifier = Modifier.padding(top = 3.dp),
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 17.sp,
            )
        }
        if (!alert.read) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE23B5A)),
            )
        }
        Text(
            text = alert.at.shortDay(),
            modifier = Modifier.padding(start = 8.dp),
            color = CinemaMuted,
            fontSize = 12.sp,
        )
    }
}

private fun FeedCard.toMovie(): Movie = Movie(
    id = id,
    title = title,
    originalTitle = originalTitle,
    year = year,
    overview = "",
    genres = "",
    country = "",
    director = "",
    posterUrl = posterUrl,
    backdropUrl = null,
    logoUrl = null,
    ratingKp = ratingKp,
    ratingImdb = ratingImdb,
    age = age,
    kind = when (type) {
        "serial", "multserial" -> ContentKind.Series
        "anime" -> ContentKind.Anime
        else -> ContentKind.Movie
    },
    camrip = false,
    type = type,
)

private fun Long.shortDay(): String {
    val date = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "сегодня"
        today.minusDays(1) -> "вчера"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru")))
    }
}
