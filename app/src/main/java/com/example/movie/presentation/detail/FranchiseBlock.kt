package com.example.movie.presentation.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.domain.model.Franchise
import com.example.movie.domain.model.MovieDetail
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaBlack
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Label = Color(0xFF8E8E93)
private val Value = Color(0xFFE8E8ED)
private val Card = Color.White.copy(alpha = 0.06f)
private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

@Composable
internal fun FranchiseBlock(detail: MovieDetail) {
    val franchise = detail.franchise ?: return
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            text = stringResource(if (franchise.series) R.string.franchise_about_series else R.string.franchise_about),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        val status = franchise.serialStatus
        if (franchise.series && !status.isNullOrBlank()) {
            val finished = status.equals("offline", ignoreCase = true)
            InfoLine(
                label = stringResource(R.string.franchise_status),
                value = stringResource(if (finished) R.string.franchise_finished else R.string.franchise_airing),
                valueColor = if (finished) Color(0xFFFF453A) else Color(0xFF32D74B),
            )
        }
        if (franchise.series && franchise.seasons.isNotEmpty()) {
            InfoLine(
                label = stringResource(R.string.franchise_seasons),
                value = stringResource(seasonCountRes(franchise.seasons.size), franchise.seasons.size),
            )
        }
        InfoLine(stringResource(R.string.franchise_director), franchise.directors.orDash(detail.director))
        InfoLine(stringResource(R.string.franchise_writers), franchise.screenwriters.orDash())
        InfoLine(stringResource(R.string.franchise_producers), franchise.producers.orDash())
        InfoLine(stringResource(R.string.franchise_operator), franchise.operators.orDash())
        InfoLine(stringResource(R.string.franchise_editor), franchise.editors.orDash())
        InfoLine(stringResource(R.string.franchise_design), franchise.designers.orDash())
        InfoLine(stringResource(R.string.franchise_genres), franchise.genres.orDash(detail.genres.joinToString(", ")))
        InfoLine(stringResource(R.string.franchise_country), franchise.countries.orDash(detail.country))
        InfoLine(stringResource(R.string.franchise_premiere), franchise.premiere.asDate())
        franchise.slogan?.let { InfoLine(stringResource(R.string.franchise_slogan), "«$it»") }
        franchise.premiereRu?.let { InfoLine(stringResource(R.string.franchise_premiere_ru), it.asDate()) }
        franchise.mpaa?.let { InfoLine(stringResource(R.string.franchise_mpaa), it) }
        franchise.budget?.let { InfoLine(stringResource(R.string.franchise_budget), it) }
        franchise.feesWorld?.let { InfoLine(stringResource(R.string.franchise_fees_world), it) }
        franchise.feesRus?.let { InfoLine(stringResource(R.string.franchise_fees_rus), it) }
        if (franchise.dubActors.isNotEmpty()) {
            InfoLine(stringResource(R.string.franchise_dub), franchise.dubActors.joinToString(", "))
        }
        if (franchise.voiceActing.isNotEmpty()) {
            InfoLine(stringResource(R.string.franchise_voice), franchise.voiceActing.joinToString(", "))
        }
        if (franchise.photos.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            PhotoStrip(franchise.photos)
        }
        if (franchise.facts.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Facts(franchise.facts)
        }
        if (franchise.seasons.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Seasons(franchise)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = Value) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = Label,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.width(132.dp),
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PhotoStrip(photos: List<String>) {
    LazyRow(
        contentPadding = PaddingValues(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(photos, key = { it }) { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 148.dp, height = 96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Card),
            )
        }
    }
}

@Composable
private fun Facts(facts: List<String>) {
    var selected by remember(facts) { mutableStateOf<String?>(null) }
    val tick = rememberHapticTick()
    val row = rememberLazyListState()
    HapticOnChange(row) { row.firstVisibleItemIndex }
    Text(
        text = stringResource(R.string.franchise_facts),
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(12.dp))
    LazyRow(
        state = row,
        modifier = Modifier
            .edgeToEdge(DetailInset)
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DetailInset),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(facts.size, key = { it }) { index ->
            val fact = facts[index]
            Text(
                text = fact,
                color = Color(0xFFC7C7CC),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(236.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Card)
                    .pointerInput(fact) {
                        detectTapGestures(onTap = {
                            tick?.invoke()
                            selected = fact
                        })
                    }
                    .padding(14.dp),
            )
        }
    }
    selected?.let { fact ->
        FactPopup(fact = fact, onDismiss = { selected = null })
    }
}

@Composable
private fun FactPopup(fact: String, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(CinemaBlack)
                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    text = stringResource(R.string.franchise_facts),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = fact,
                    color = Color(0xFFD2D2D8),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun Seasons(franchise: Franchise) {
    val tick = rememberHapticTick()
    var opened by remember(franchise.seasons) { mutableStateOf(setOf<Int>()) }
    Text(
        text = stringResource(R.string.franchise_seasons),
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(12.dp))
    franchise.seasons.forEach { season ->
        val expanded = season.number in opened
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Card),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            opened = if (expanded) opened - season.number else opened + season.number
                        },
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!season.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = season.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 36.dp, height = 52.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.franchise_season, season.number),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (season.episodes.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.franchise_episodes, season.episodes.size),
                            color = Label,
                            fontSize = 13.sp,
                        )
                    }
                }
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = tween(220),
                    label = "seasonArrow",
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { rotationZ = rotation },
                )
            }
            if (expanded) {
                season.episodes.forEach { episode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = episode.number.toString(),
                            color = Label,
                            fontSize = 14.sp,
                            modifier = Modifier.width(28.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = episode.name ?: stringResource(R.string.franchise_episode, episode.number),
                                color = Value,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            episode.release?.let { release ->
                                Text(
                                    text = release.asDate(),
                                    color = Label,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun seasonCountRes(count: Int): Int {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> R.string.franchise_season_many
        mod10 == 1 -> R.string.franchise_season_one
        mod10 in 2..4 -> R.string.franchise_season_few
        else -> R.string.franchise_season_many
    }
}

private fun List<String>.orDash(fallback: String = ""): String =
    joinToString(", ").ifBlank { fallback.trim() }.ifBlank { "—" }

private fun String?.asDate(): String {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return "—"
    val parsed = runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
    return parsed?.format(dateFormat) ?: raw
}
