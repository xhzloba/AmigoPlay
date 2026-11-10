package com.example.movie.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.EpisodeProgress
import com.example.movie.domain.model.FranchiseEpisode
import com.example.movie.domain.model.FranchiseSeason
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.combineMarks
import com.example.movie.domain.model.episodeMarks
import com.example.movie.domain.model.isUnreleased
import com.example.movie.domain.model.toSaved
import com.example.movie.presentation.common.HandleSheet
import com.example.movie.presentation.common.LocalShelfNotices
import com.example.movie.presentation.common.NoticeShelfWatched
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PlayBlue
import kotlin.math.roundToInt

@Composable
internal fun SeasonWatchSheet(
    movie: Movie,
    knownSeasons: List<FranchiseSeason> = emptyList(),
    onDismiss: () -> Unit,
) {
    val container = LocalAppContainer.current
    val resumeStore = container.resume
    val watched = container.watched
    val favorites = container.favorites
    val resume by resumeStore.items.collectAsStateWithLifecycle()
    val kept by resumeStore.kept.collectAsStateWithLifecycle()
    val tick = rememberHapticTick()
    val notices = LocalShelfNotices.current
    val addedWatched = stringResource(R.string.notice_watched_series)
    val removedWatched = stringResource(R.string.notice_watched_series_off)
    var attempt by remember { mutableIntStateOf(0) }
    var seasons by remember { mutableStateOf(knownSeasons.takeIf { it.isNotEmpty() }) }
    var failed by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var marks by remember { mutableStateOf<List<EpisodeProgress>>(emptyList()) }
    var seeded by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(movie.id, attempt) {
        if (attempt == 0 && !seasons.isNullOrEmpty()) return@LaunchedEffect
        failed = false
        val loaded = container.loadFranchise(movie.id, movie.title, movie.year)?.seasons.orEmpty()
        if (loaded.isEmpty()) {
            seasons = null
            failed = true
        } else {
            seasons = loaded
            failed = false
        }
    }
    val loaded = seasons
    LaunchedEffect(loaded) {
        if (loaded == null || seeded) return@LaunchedEffect
        val combined = combineMarks(
            resume.firstOrNull { it.id == movie.id }?.episodeMarks().orEmpty(),
            kept[movie.id].orEmpty(),
        )
        val open = loaded.airedKeys()
        marks = combined
        picked = combined
            .filter { it.completed }
            .map { it.season to it.episode }
            .filter { it in open }
            .toSet()
        if (loaded.size == 1) opened = loaded.first().number
        seeded = true
    }
    val open = loaded?.airedKeys().orEmpty()
    val all = open.isNotEmpty() && open.all { it in picked }
    val some = !all && open.any { it in picked }
    HandleSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.watch_seasons_title),
                modifier = Modifier.padding(horizontal = 22.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            when {
                loaded == null && !failed -> CircularProgressIndicator(
                    color = PlayBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(vertical = 28.dp).size(28.dp),
                )
                failed -> {
                    Text(
                        text = stringResource(R.string.watch_seasons_failed),
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        color = CinemaMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.watch_seasons_retry),
                        color = PlayBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    tick?.invoke()
                                    attempt += 1
                                },
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                loaded != null -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                    ) {
                        SeasonPickRow(
                            title = stringResource(R.string.watch_seasons_all),
                            mark = when {
                                all -> Mark.Full
                                some -> Mark.Partial
                                else -> Mark.Empty
                            },
                            onClick = {
                                tick?.invoke()
                                picked = if (all) emptySet() else open
                            },
                        )
                        loaded.forEach { season ->
                            val keys = season.airedKeys()
                            val on = keys.count { it in picked }
                            val mark = when {
                                keys.isEmpty() || on == 0 -> Mark.Empty
                                on == keys.size -> Mark.Full
                                else -> Mark.Partial
                            }
                            SeasonBlock(
                                title = stringResource(R.string.franchise_season, season.number),
                                progress = if (keys.isEmpty()) {
                                    null
                                } else {
                                    stringResource(R.string.watch_season_progress, on, keys.size)
                                },
                                mark = mark,
                                enabled = keys.isNotEmpty(),
                                expanded = opened == season.number,
                                onToggle = {
                                    tick?.invoke()
                                    picked = if (mark == Mark.Full) picked - keys else picked + keys
                                },
                                onExpand = {
                                    tick?.invoke()
                                    opened = if (opened == season.number) null else season.number
                                },
                            ) {
                                season.episodes.forEach { episode ->
                                    val airedEpisode = !episode.release.isUnreleased()
                                    val key = season.number to episode.number
                                    val checked = key in picked
                                    val progress = marks.firstOrNull {
                                        it.season == season.number && it.episode == episode.number
                                    }
                                    EpisodePickRow(
                                        episode = episode,
                                        checked = checked,
                                        enabled = airedEpisode,
                                        percent = if (
                                            !checked &&
                                            progress != null &&
                                            !progress.completed &&
                                            progress.fraction > 0f
                                        ) {
                                            (progress.fraction * 100f).roundToInt().coerceIn(1, 99)
                                        } else {
                                            null
                                        },
                                        onClick = {
                                            tick?.invoke()
                                            picked = if (checked) picked - key else picked + key
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (applying || picked.isEmpty()) return@Button
                            tick?.invoke()
                            applying = true
                            val finished = resumeStore.watchSeasons(movie.id, loaded, picked)
                            if (finished) {
                                watched.add(movie.toSaved())
                                favorites.remove(movie.id)
                                notices.show(addedWatched, NoticeShelfWatched)
                            } else if (movie.id in watched.ids.value) {
                                watched.remove(movie.id)
                                notices.show(removedWatched, NoticeShelfWatched)
                            }
                            onDismiss()
                        },
                        enabled = picked.isNotEmpty() && !applying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.White.copy(alpha = 0.18f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f),
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .padding(horizontal = 22.dp)
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.watch_seasons_done),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = stringResource(R.string.watch_seasons_reset),
                        color = CinemaMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !applying,
                                onClick = {
                                    if (applying) return@clickable
                                    tick?.invoke()
                                    applying = true
                                    val wasWatched = movie.id in watched.ids.value
                                    resumeStore.resetProgress(movie.id)
                                    watched.remove(movie.id)
                                    if (wasWatched) notices.show(removedWatched, NoticeShelfWatched)
                                    onDismiss()
                                },
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

private enum class Mark { Empty, Partial, Full }

@Composable
private fun SeasonPickRow(
    title: String,
    mark: Mark,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickMark(mark)
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = if (mark == Mark.Empty) FontWeight.Normal else FontWeight.Medium,
        )
    }
}

@Composable
private fun SeasonBlock(
    title: String,
    progress: String?,
    mark: Mark,
    enabled: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    episodes: @Composable () -> Unit,
) {
    val turn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "season",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        onClick = onToggle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                PickMark(mark, enabled)
            }
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExpand,
                    )
                    .padding(end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (mark == Mark.Empty) FontWeight.Normal else FontWeight.Medium,
                )
                if (progress != null) {
                    Text(
                        text = progress,
                        color = CinemaMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(turn),
                    tint = Color.White.copy(alpha = 0.55f),
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                expandVertically(tween(280, easing = FastOutSlowInEasing), expandFrom = Alignment.Top),
            exit = fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                shrinkVertically(tween(240, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top),
        ) {
            Column(Modifier.padding(bottom = 4.dp)) {
                episodes()
            }
        }
    }
}

@Composable
private fun EpisodePickRow(
    episode: FranchiseEpisode,
    checked: Boolean,
    enabled: Boolean,
    percent: Int?,
    onClick: () -> Unit,
) {
    val ink = if (enabled) Color.White else Color.White.copy(alpha = 0.34f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 28.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickMark(if (checked) Mark.Full else Mark.Empty, enabled)
        Spacer(Modifier.width(12.dp))
        val label = stringResource(R.string.cast_episode_value, episode.number)
        val title = episode.name?.takeIf { name ->
            name.isNotBlank() && !name.equals(label, ignoreCase = true) && name != episode.number.toString()
        }
        Text(
            text = if (title == null) label else "$label · $title",
            modifier = Modifier.weight(1f),
            color = ink,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (percent != null) {
            Text(
                text = "$percent%",
                modifier = Modifier.padding(start = 8.dp),
                color = CinemaMuted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun PickMark(state: Mark, enabled: Boolean = true) {
    Canvas(Modifier.size(22.dp)) {
        val radius = CornerRadius(6.dp.toPx())
        val inset = 0.8.dp.toPx()
        val box = Size(size.width - inset * 2, size.height - inset * 2)
        val origin = Offset(inset, inset)
        if (!enabled || state == Mark.Empty) {
            drawRoundRect(
                color = Color.White.copy(alpha = if (enabled) 0.45f else 0.2f),
                topLeft = origin,
                size = box,
                cornerRadius = radius,
                style = Stroke(width = 1.6.dp.toPx()),
            )
            return@Canvas
        }
        drawRoundRect(
            color = PlayBlue,
            topLeft = origin,
            size = box,
            cornerRadius = radius,
        )
        val stroke = 2.dp.toPx()
        if (state == Mark.Partial) {
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.28f, size.height * 0.5f),
                end = Offset(size.width * 0.72f, size.height * 0.5f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            return@Canvas
        }
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.28f, size.height * 0.52f),
            end = Offset(size.width * 0.44f, size.height * 0.68f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.44f, size.height * 0.68f),
            end = Offset(size.width * 0.74f, size.height * 0.34f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun List<FranchiseSeason>.airedKeys(): Set<Pair<Int, Int>> = flatMap { it.airedKeys() }.toSet()

private fun FranchiseSeason.airedKeys(): Set<Pair<Int, Int>> =
    episodes.filterNot { it.release.isUnreleased() }.map { number to it.number }.toSet()
