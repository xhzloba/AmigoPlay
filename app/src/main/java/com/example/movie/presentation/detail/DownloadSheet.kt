package com.example.movie.presentation.detail

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.data.download.OfflineItem
import com.example.movie.data.download.OfflinePhase
import com.example.movie.data.download.OfflineStatus
import com.example.movie.data.download.offlineItem
import com.example.movie.data.download.toPlayback
import com.example.movie.data.player.WatchPlayback
import com.example.movie.data.player.fetchLiftPlayback
import com.example.movie.data.player.forDownload
import com.example.movie.data.player.preferRicherManifest
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.FranchiseEpisode
import com.example.movie.domain.model.FranchiseSeason
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.isUnreleased
import com.example.movie.presentation.common.DownloadProgressControls
import com.example.movie.presentation.common.HandleSheet
import com.example.movie.presentation.common.LocalShelfNotices
import com.example.movie.presentation.common.NoticeShelfDownloads
import com.example.movie.presentation.common.inProgress
import com.example.movie.presentation.common.offlineProgressTitle
import com.example.movie.presentation.common.offlineSizeLabel
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PlayBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Qualities = listOf(0, 2160, 1080, 720, 480)

internal fun qualityLabel(height: Int): String = when {
    height <= 0 -> "Лучшее"
    height >= 2000 -> "4K"
    else -> "${height}p"
}

@Composable
internal fun DownloadSheet(
    title: SavedTitle,
    series: Boolean,
    watchId: Int?,
    knownSeasons: List<FranchiseSeason> = emptyList(),
    existing: OfflineItem? = null,
    onDismiss: () -> Unit,
) {
    val container = LocalAppContainer.current
    val offline = container.offline
    val scope = rememberCoroutineScope()
    val tick = rememberHapticTick()
    val notices = LocalShelfNotices.current
    val queued = stringResource(R.string.notice_download_queued)
    val failed = stringResource(R.string.download_failed)
    var attempt by remember { mutableIntStateOf(0) }
    var seasons by remember { mutableStateOf(knownSeasons.takeIf { it.isNotEmpty() }) }
    var failedLoad by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var seeded by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf<Int?>(null) }
    var height by remember { mutableIntStateOf(0) }
    var ladder by remember { mutableStateOf(Qualities) }
    var audios by remember { mutableStateOf<List<String>>(emptyList()) }
    var audioOrder by remember { mutableStateOf<List<Int>>(emptyList()) }
    var audio by remember { mutableIntStateOf(0) }
    var cachedPlayback by remember { mutableStateOf<WatchPlayback?>(null) }
    var streamReady by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val tasks by offline.tasks.collectAsStateWithLifecycle()
    val pending by offline.pending.collectAsStateWithLifecycle()

    LaunchedEffect(watchId) {
        streamReady = false
        cachedPlayback = null
        audios = emptyList()
        audioOrder = emptyList()
        audio = 0
        ladder = Qualities
        height = 0
        val franchise = watchId ?: return@LaunchedEffect
        val playback = withContext(Dispatchers.IO) {
            runCatching { fetchLiftPlayback(franchise) }.getOrNull()
        } ?: return@LaunchedEffect
        val rich = withContext(Dispatchers.IO) { playback.stream.preferRicherManifest() }
        cachedPlayback = playback.copy(stream = rich)
        audios = rich.audioNames
        audioOrder = rich.audioOrder
        val probed = offline.heights(rich)
            .filter { it > 0 }
            .distinct()
            .sortedDescending()
        if (probed.isNotEmpty()) {
            ladder = listOf(0) + probed
            if (height !in ladder) height = 0
        }
        streamReady = true
    }

    LaunchedEffect(title.id, attempt, series) {
        if (!series) {
            seasons = emptyList()
            failedLoad = false
            return@LaunchedEffect
        }
        if (attempt == 0 && !seasons.isNullOrEmpty()) return@LaunchedEffect
        failedLoad = false
        val loaded = container.loadFranchise(title.id, title.title, title.year)?.seasons.orEmpty()
        if (loaded.isEmpty()) {
            seasons = null
            failedLoad = true
        } else {
            seasons = loaded
            failedLoad = false
        }
    }
    val loaded = seasons
    LaunchedEffect(loaded, existing) {
        if (!series || loaded == null || seeded) return@LaunchedEffect
        val open = loaded.airedKeys()
        val kept = existing?.streams
            ?.map { it.season to it.episode }
            ?.filter { it in open }
            ?.toSet()
            .orEmpty()
        picked = kept
        if (loaded.size == 1) opened = loaded.first().number
        seeded = true
    }

    HandleSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.download_title),
                modifier = Modifier.padding(horizontal = 22.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.download_hint),
                modifier = Modifier.padding(horizontal = 22.dp),
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
            ) {
                val spot = ladder.indexOf(height).let { if (it < 0) 0 else it }
                DownloadValueRow(
                    text = stringResource(R.string.cast_quality_value, qualityLabel(height)),
                ) {
                    tick?.invoke()
                    height = ladder[(spot + 1) % ladder.size]
                }
                Spacer(Modifier.height(8.dp))
                when {
                    audios.isNotEmpty() -> DownloadValueRow(
                        text = stringResource(
                            R.string.cast_audio_value,
                            audios.getOrNull(audio) ?: "—",
                        ),
                        enabled = audios.size > 1,
                    ) {
                        tick?.invoke()
                        audio = (audio + 1) % audios.size
                    }
                    watchId != null && !streamReady -> DownloadValueRow(
                        text = stringResource(R.string.download_audio_loading),
                        enabled = false,
                    ) {}
                }
            }

            when {
                series && loaded == null && !failedLoad -> CircularProgressIndicator(
                    color = PlayBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(vertical = 28.dp).size(28.dp),
                )
                series && failedLoad -> {
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
                series && loaded != null -> {
                    val open = loaded.airedKeys()
                    val all = open.isNotEmpty() && open.all { it in picked }
                    val some = !all && open.any { it in picked }
                    Spacer(Modifier.height(10.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                    ) {
                        DownloadPickRow(
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
                            DownloadSeasonBlock(
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
                                    val aired = !episode.release.isUnreleased()
                                    val key = season.number to episode.number
                                    val checked = key in picked
                                    val status = existing?.streams
                                        ?.firstOrNull { it.season == season.number && it.episode == episode.number }
                                        ?.let { stream ->
                                            offline.status(
                                                existing.copy(streams = listOf(stream)),
                                                tasks,
                                                pending,
                                            )
                                        }
                                    DownloadEpisodeRow(
                                        episode = episode,
                                        checked = checked,
                                        enabled = aired,
                                        status = status,
                                        onClick = {
                                            tick?.invoke()
                                            picked = if (checked) picked - key else picked + key
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                !series -> {
                    existing?.let { item ->
                        val status = offline.status(item, tasks, pending)
                        if (status.phase.inProgress() || status.phase == OfflinePhase.Ready) {
                            Spacer(Modifier.height(12.dp))
                            DownloadProgressControls(
                                status = status,
                                caption = offlineSizeLabel(status)
                                    ?: offlineProgressTitle(status),
                                onPause = { offline.pause(title.id) },
                                onResume = { offline.resume(title.id) },
                                onDelete = {
                                    offline.remove(title.id)
                                    onDismiss()
                                },
                                modifier = Modifier.padding(horizontal = 22.dp),
                                showDelete = false,
                            )
                        }
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error.orEmpty(),
                    color = Color(0xFFFF8A80),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 22.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            val canStart = watchId != null && !applying && (!series || picked.isNotEmpty())
            Button(
                onClick = {
                    if (!canStart) return@Button
                    val franchise = watchId ?: return@Button
                    tick?.invoke()
                    applying = true
                    error = null
                    scope.launch {
                        val raw = cachedPlayback ?: withContext(Dispatchers.IO) {
                            runCatching { fetchLiftPlayback(franchise) }.getOrNull()
                        }
                        if (raw == null) {
                            error = failed
                            applying = false
                            return@launch
                        }
                        val playback = withContext(Dispatchers.IO) { raw.forDownload() }
                        cachedPlayback = playback
                        if (audios.isEmpty()) {
                            audios = playback.stream.audioNames
                            audioOrder = playback.stream.audioOrder
                        }
                        val code = when {
                            audios.isEmpty() -> -1
                            audioOrder.isNotEmpty() -> audioOrder.getOrNull(audio) ?: audio
                            else -> audio
                        }
                        val keys = if (series) picked else emptySet()
                        offline.enqueue(offlineItem(title, playback, height, keys, audioIndex = code))
                        notices.show(queued, NoticeShelfDownloads)
                        onDismiss()
                    }
                },
                enabled = canStart,
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
                if (applying) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.download_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (existing != null) {
                Text(
                    text = stringResource(R.string.download_delete),
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
                                tick?.invoke()
                                if (series && picked.isNotEmpty()) {
                                    picked.forEach { (season, episode) ->
                                        offline.removeStream(title.id, season, episode)
                                    }
                                } else {
                                    offline.remove(title.id)
                                }
                                onDismiss()
                            },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

private enum class Mark { Empty, Partial, Full }

@Composable
private fun DownloadValueRow(
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
private fun DownloadPickRow(title: String, mark: Mark, onClick: () -> Unit) {
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
private fun DownloadSeasonBlock(
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
        label = "download-season",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                    Text(text = progress, color = CinemaMuted, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).rotate(turn),
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
            Column(Modifier.padding(bottom = 4.dp)) { episodes() }
        }
    }
}

@Composable
private fun DownloadEpisodeRow(
    episode: FranchiseEpisode,
    checked: Boolean,
    enabled: Boolean,
    status: OfflineStatus?,
    onClick: () -> Unit,
) {
    val ink = if (enabled) Color.White else Color.White.copy(alpha = 0.34f)
    val active = status?.phase?.inProgress() == true
    Column(
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
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PickMark(if (checked) Mark.Full else Mark.Empty, enabled)
            Spacer(Modifier.width(12.dp))
            val label = stringResource(R.string.cast_episode_value, episode.number)
            val name = episode.name?.takeIf {
                it.isNotBlank() && !it.equals(label, ignoreCase = true) && it != episode.number.toString()
            }
            Text(
                text = if (name == null) label else "$label · $name",
                modifier = Modifier.weight(1f),
                color = ink,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (status != null && !active) {
                Text(
                    text = offlineProgressTitle(status),
                    modifier = Modifier.padding(start = 8.dp),
                    color = CinemaMuted,
                    fontSize = 13.sp,
                )
            }
        }
        if (status != null && active) {
            Spacer(Modifier.height(6.dp))
            DownloadProgressControls(
                status = status,
                caption = offlineSizeLabel(status) ?: offlineProgressTitle(status),
                onPause = {},
                onResume = {},
                onDelete = {},
                modifier = Modifier.padding(start = 34.dp),
                showDelete = false,
                showActions = false,
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
        drawRoundRect(color = PlayBlue, topLeft = origin, size = box, cornerRadius = radius)
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

@Composable
internal fun OfflinePlayer(
    item: OfflineItem,
    season: Int? = null,
    episode: Int? = null,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            LiftExoPlayer(
                playback = item.toPlayback(season, episode),
                title = item.title.title,
                logoUrl = item.title.logoUrl,
                onDismiss = onDismiss,
            )
        }
    }
}
