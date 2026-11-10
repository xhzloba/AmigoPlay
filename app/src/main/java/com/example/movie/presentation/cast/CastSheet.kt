package com.example.movie.presentation.cast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.data.cast.DlnaDevice
import com.example.movie.data.cast.TvCastHub
import com.example.movie.domain.model.EpisodeProgress
import com.example.movie.domain.model.FranchiseEpisode
import com.example.movie.domain.model.FranchiseSeason
import com.example.movie.domain.model.ResumeItem
import com.example.movie.domain.model.isUnreleased
import com.example.movie.presentation.common.HandleSheet
import com.example.movie.presentation.detail.resumeWatchUrl
import com.example.movie.ui.theme.PlayBlue

@Composable
internal fun CastSheet(
    title: String,
    pageUrl: String?,
    resolving: Boolean,
    seasons: List<FranchiseSeason> = emptyList(),
    resume: ResumeItem? = null,
    marks: List<EpisodeProgress> = emptyList(),
    watchId: Int? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val state by TvCastHub.state.collectAsStateWithLifecycle()
    var opened by remember { mutableStateOf<String?>(null) }
    val series = watchId != null && seasons.isNotEmpty()
    val startSeason = seasons.firstOrNull { it.number == resume?.season } ?: seasons.firstOrNull()
    val startEpisode = startSeason?.aired(resume?.episode)
    var seasonNumber by remember(seasons, resume?.season) { mutableIntStateOf(startSeason?.number ?: 0) }
    var episodeNumber by remember(seasons, resume?.episode) { mutableIntStateOf(startEpisode?.number ?: 0) }
    fun shownTitle(season: Int, episode: Int): String {
        if (!series) return context.getString(R.string.cast_now_movie, title)
        if (season <= 0 || episode <= 0) return context.getString(R.string.cast_now_series, title)
        return context.getString(R.string.cast_now_series_episode, title, season, episode)
    }
    fun pageFor(season: Int, episode: Int): String? {
        val id = watchId
        if (!series || id == null || season <= 0 || episode <= 0) return pageUrl
        return resumeWatchUrl(id, season, episode)
    }
    fun launch(target: CastTarget, device: DlnaDevice) {
        val season = seasonNumber
        val episode = episodeNumber
        launchOn(
            context = context,
            target = target,
            device = device,
            page = pageFor(season, episode),
            shown = shownTitle(season, episode),
        )
    }
    HandleSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
        ) {
            Text(
                text = stringResource(R.string.cast_title),
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = shownTitle(seasonNumber, episodeNumber),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            val readyText = stringResource(R.string.cast_ready)
            val preparingText = stringResource(R.string.cast_preparing)
            val searchingText = stringResource(R.string.cast_searching)
            val raw = state.message
            val loading = resolving || state.busy || state.searching
            val silent = raw == readyText || raw == preparingText || raw == searchingText
            val message = when {
                loading || silent -> null
                pageUrl.isNullOrBlank() -> stringResource(R.string.cast_no_stream)
                raw.isNotBlank() -> raw
                state.running -> null
                else -> stringResource(R.string.cast_idle)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = PlayBlue,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else if (message != null) {
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!pageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                if (!state.running) {
                    CastRow(
                        text = stringResource(R.string.cast_start),
                        enabled = !state.busy && !resolving,
                        filled = true,
                        onClick = {
                            val page = pageFor(seasonNumber, episodeNumber) ?: pageUrl
                            TvCastService.prepare(context, page, shownTitle(seasonNumber, episodeNumber))
                        },
                    )
                }
                if (state.running) {
                    state.devices.forEach { device ->
                        Spacer(Modifier.height(10.dp))
                        TvDrop(
                            device = device,
                            expanded = opened == device.location,
                            playing = device.name == state.playingOn,
                            busy = state.busy,
                            hls = state.hls,
                            qualities = state.qualities,
                            quality = state.quality,
                            audios = state.audios,
                            audio = state.audio,
                            seasons = if (series) seasons else emptyList(),
                            marks = marks,
                            seasonNumber = seasonNumber,
                            episodeNumber = episodeNumber,
                            onToggle = {
                                opened = if (opened == device.location) null else device.location
                            },
                            onQuality = { height -> TvCastService.quality(context, height) },
                            onAudio = { index -> TvCastService.audio(context, index) },
                            onSeason = { number ->
                                val id = watchId ?: return@TvDrop
                                val next = seasons.firstOrNull { it.number == number } ?: return@TvDrop
                                val episode = next.aired(null)?.number ?: return@TvDrop
                                seasonNumber = number
                                episodeNumber = episode
                                TvCastService.retarget(context, resumeWatchUrl(id, number, episode), shownTitle(number, episode))
                            },
                            onEpisode = { number ->
                                episodeNumber = number
                                val id = watchId ?: return@TvDrop
                                TvCastService.retarget(
                                    context,
                                    resumeWatchUrl(id, seasonNumber, number),
                                    shownTitle(seasonNumber, number),
                                )
                            },
                            onBrowser = { launch(CastTarget.Browser, device) },
                            onPlayer = { launch(CastTarget.Player, device) },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    CastRow(
                        text = stringResource(R.string.cast_find),
                        enabled = !state.busy,
                        icon = R.drawable.ic_cast,
                        onClick = { TvCastService.search(context) },
                    )
                }
                if (state.running) {
                    Spacer(Modifier.height(14.dp))
                    CastRow(
                        text = stringResource(R.string.cast_stop),
                        enabled = !state.busy,
                        onClick = { TvCastService.stop(context) },
                    )
                }
            }
        }
    }
}

private enum class CastTarget { Browser, Player }

private fun launchOn(
    context: android.content.Context,
    target: CastTarget,
    device: DlnaDevice,
    page: String?,
    shown: String,
) {
    when (target) {
        CastTarget.Browser -> TvCastService.openBrowser(context, device.name, device.location, page, shown)
        CastTarget.Player -> TvCastService.play(context, device.name, device.controlUrl, page, shown)
    }
}

@Composable
private fun TvDrop(
    device: DlnaDevice,
    expanded: Boolean,
    playing: Boolean,
    busy: Boolean,
    hls: Boolean,
    qualities: List<Int>,
    quality: Int?,
    audios: List<String>,
    audio: Int,
    seasons: List<FranchiseSeason>,
    marks: List<EpisodeProgress>,
    seasonNumber: Int,
    episodeNumber: Int,
    onToggle: () -> Unit,
    onQuality: (Int?) -> Unit,
    onAudio: (Int) -> Unit,
    onSeason: (Int) -> Unit,
    onEpisode: (Int) -> Unit,
    onBrowser: () -> Unit,
    onPlayer: () -> Unit,
) {
    val turn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "tv",
    )
    val season = seasons.firstOrNull { it.number == seasonNumber }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle,
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = device.name,
                    modifier = Modifier.padding(horizontal = 28.dp),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (playing) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(PlayBlue),
                        )
                        Text(
                            text = stringResource(R.string.cast_live),
                            modifier = Modifier.padding(start = 6.dp),
                            color = PlayBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .rotate(turn),
                tint = Color.White.copy(alpha = 0.7f),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                expandVertically(tween(280, easing = FastOutSlowInEasing), expandFrom = Alignment.Top),
            exit = fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                shrinkVertically(tween(240, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top),
        ) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                if (qualities.isNotEmpty()) {
                    val auto = stringResource(R.string.player_quality_auto)
                    val ladder = listOf<Int?>(null) + qualities
                    val spot = ladder.indexOf(quality).let { if (it < 0) 0 else it }
                    ValueRow(stringResource(R.string.cast_quality_value, if (quality == null) auto else "${quality}p")) {
                        onQuality(ladder[(spot + 1) % ladder.size])
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (audios.isNotEmpty()) {
                    ValueRow(
                        text = stringResource(R.string.cast_audio_value, audios.getOrNull(audio) ?: "—"),
                        enabled = audios.size > 1,
                    ) {
                        onAudio((audio + 1) % audios.size)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (seasons.isNotEmpty()) {
                    val openSeasons = seasons.filter { it.episodes.any { episode -> !episode.release.isUnreleased() } }
                    val seasonSpot = openSeasons.indexOfFirst { it.number == seasonNumber }
                    StepRow(
                        label = stringResource(R.string.franchise_season, seasonNumber),
                        minusEnabled = seasonSpot > 0,
                        plusEnabled = seasonSpot in 0 until openSeasons.lastIndex,
                        onMinus = { openSeasons.getOrNull(seasonSpot - 1)?.let { onSeason(it.number) } },
                        onPlus = { openSeasons.getOrNull(seasonSpot + 1)?.let { onSeason(it.number) } },
                    )
                    Spacer(Modifier.height(8.dp))
                    if (season != null) {
                        val aired = season.episodes.filterNot { it.release.isUnreleased() }
                        val episodeSpot = aired.indexOfFirst { it.number == episodeNumber }
                        val seen = marks.any { it.completed && it.season == season.number && it.episode == episodeNumber }
                        StepRow(
                            label = stringResource(R.string.cast_episode_value, episodeNumber),
                            labelColor = if (seen) PlayBlue else Color.White,
                            minusEnabled = episodeSpot > 0,
                            plusEnabled = episodeSpot in 0 until aired.lastIndex,
                            onMinus = { aired.getOrNull(episodeSpot - 1)?.let { onEpisode(it.number) } },
                            onPlus = { aired.getOrNull(episodeSpot + 1)?.let { onEpisode(it.number) } },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                CastRow(
                    text = stringResource(R.string.cast_tv_browser),
                    enabled = !busy,
                    white = true,
                    onClick = onBrowser,
                )
                Spacer(Modifier.height(8.dp))
                CastRow(
                    text = stringResource(R.string.cast_tv_player),
                    enabled = !busy && hls,
                    white = true,
                    onClick = onPlayer,
                )
            }
        }
    }
}

@Composable
private fun ValueRow(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.72f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StepRow(
    label: String,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    labelColor: Color = Color.White,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 52.dp),
            color = labelColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        StepKey("−", minusEnabled, Modifier.align(Alignment.CenterStart).padding(start = 6.dp), onMinus)
        StepKey("+", plusEnabled, Modifier.align(Alignment.CenterEnd).padding(end = 6.dp), onPlus)
    }
}

@Composable
private fun StepKey(
    glyph: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.14f else 0.05f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.28f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun FranchiseSeason.aired(prefer: Int?): FranchiseEpisode? {
    val open = episodes.filterNot { it.release.isUnreleased() }
    return open.firstOrNull { it.number == prefer } ?: open.firstOrNull()
}

@Composable
private fun CastRow(
    text: String,
    enabled: Boolean,
    filled: Boolean = false,
    white: Boolean = false,
    icon: Int? = null,
    onClick: () -> Unit,
) {
    val background = when {
        white && enabled -> Color.White
        white -> Color.White.copy(alpha = 0.4f)
        filled -> PlayBlue
        else -> Color.White.copy(alpha = 0.08f)
    }
    val ink = when {
        white && enabled -> Color.Black
        white -> Color.Black.copy(alpha = 0.35f)
        enabled -> Color.White
        else -> Color.White.copy(alpha = 0.4f)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = if (icon != null) 28.dp else 0.dp),
            color = ink,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(20.dp),
                tint = ink,
            )
        }
    }
}
