package com.example.movie.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.data.download.OfflineItem
import com.example.movie.data.download.OfflinePhase
import com.example.movie.data.download.OfflineStatus
import com.example.movie.data.download.OfflineStream
import com.example.movie.presentation.common.DownloadProgressControls
import com.example.movie.presentation.common.canPause
import com.example.movie.presentation.common.inProgress
import com.example.movie.presentation.common.offlineProgressTitle
import com.example.movie.presentation.common.offlineSizeLabel
import com.example.movie.presentation.detail.StackPage
import com.example.movie.presentation.detail.qualityLabel
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface
import com.example.movie.ui.theme.PosterShape

private val RowShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(14.dp)
private val IconBtnShape = RoundedCornerShape(12.dp)

@Composable
internal fun DownloadsLibrary(
    items: List<OfflineItem>,
    topPadding: Dp,
    statusOf: (OfflineItem) -> OfflineStatus,
    onOpen: (OfflineItem) -> Unit,
    onPlay: (OfflineItem) -> Unit,
    onPause: (OfflineItem) -> Unit,
    onResume: (OfflineItem) -> Unit,
    onDelete: (OfflineItem) -> Unit,
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding, bottom = 96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.downloads_empty),
                color = CinemaMuted,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding, bottom = 96.dp),
    ) {
        items(items, key = { it.title.id }) { item ->
            DownloadTitleRow(
                item = item,
                statusOf = statusOf,
                onClick = {
                    if (item.series()) onOpen(item) else onPlay(item)
                },
                onPause = { onPause(item) },
                onResume = { onResume(item) },
                onDelete = { onDelete(item) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DownloadedEpisodes(
    item: OfflineItem,
    statusOf: (OfflineItem) -> OfflineStatus,
    onPlay: (season: Int, episode: Int) -> Unit,
    onRemove: (season: Int, episode: Int) -> Unit,
    onPause: (season: Int, episode: Int) -> Unit,
    onResume: (season: Int, episode: Int) -> Unit,
    onBack: () -> Unit,
) {
    val seasons = item.streams.filter { it.season > 0 || it.episode > 0 }.groupBy { it.season }.toSortedMap()
    var season by rememberSaveable(item.title.id) { mutableIntStateOf(seasons.keys.firstOrNull() ?: 0) }
    val shownSeason = season.takeIf { it in seasons.keys } ?: seasons.keys.firstOrNull() ?: 0
    val episodes = seasons[shownSeason].orEmpty().sortedBy { it.episode }
    StackPage(title = item.title.title, onBack = onBack) {
        Text(
            text = stringResource(episodeCountRes(item.streams.size), item.streams.size),
            color = CinemaMuted,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )
        if (seasons.size > 1) {
            Text(
                text = stringResource(R.string.download_season),
                color = CinemaMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                seasons.keys.forEach { number ->
                    val selected = number == shownSeason
                    Text(
                        text = stringResource(R.string.franchise_season, number),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .widthIn(min = 48.dp)
                            .heightIn(min = 40.dp)
                            .clip(ChipShape)
                            .background(Color.White.copy(alpha = if (selected) 0.14f else 0.06f))
                            .border(0.5.dp, Color.White.copy(alpha = if (selected) 0.28f else 0.08f), ChipShape)
                            .clickable(
                                interactionSource = remember(number) { MutableInteractionSource() },
                                indication = null,
                                onClick = { season = number },
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        } else if (shownSeason > 0) {
            Text(
                text = stringResource(R.string.franchise_season, shownSeason),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        episodes.forEach { stream ->
            EpisodeRow(
                stream = stream,
                status = statusOf(item.copy(streams = listOf(stream))),
                onPlay = { onPlay(stream.season, stream.episode) },
                onRemove = { onRemove(stream.season, stream.episode) },
                onPause = { onPause(stream.season, stream.episode) },
                onResume = { onResume(stream.season, stream.episode) },
            )
        }
    }
}

@Composable
private fun DownloadTitleRow(
    item: OfflineItem,
    statusOf: (OfflineItem) -> OfflineStatus,
    onClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
) {
    val tick = rememberHapticTick()
    val status = statusOf(item)
    val progress = status.phase.inProgress()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember(item.title.id) { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onClick()
                },
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.title.posterUrl,
            contentDescription = item.title.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 56.dp, height = 82.dp)
                .clip(PosterShape)
                .background(Color.White.copy(alpha = 0.06f)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title.title,
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (progress) {
                Spacer(Modifier.height(2.dp))
                DownloadProgressControls(
                    status = status,
                    caption = offlineSizeLabel(status) ?: offlineProgressTitle(status),
                    onPause = onPause,
                    onResume = onResume,
                    onDelete = onDelete,
                    showActions = false,
                )
            } else {
                Text(
                    text = item.libraryCaption(statusOf),
                    color = CinemaMuted,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (status.phase.canPause() || status.phase == OfflinePhase.Paused) {
                DownloadIconButton(
                    icon = if (status.phase == OfflinePhase.Paused) {
                        R.drawable.ic_player_play
                    } else {
                        R.drawable.ic_player_pause
                    },
                    description = stringResource(
                        if (status.phase == OfflinePhase.Paused) {
                            R.string.download_resume
                        } else {
                            R.string.download_pause
                        },
                    ),
                    onClick = {
                        if (status.phase == OfflinePhase.Paused) onResume() else onPause()
                    },
                )
                Spacer(Modifier.height(6.dp))
            }
            DownloadIconButton(
                icon = R.drawable.ic_action_delete,
                description = stringResource(R.string.download_delete),
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    stream: OfflineStream,
    status: OfflineStatus,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val tick = rememberHapticTick()
    val progress = status.phase.inProgress()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RowShape)
            .background(CinemaSurface)
            .clickable(
                interactionSource = remember(stream.season, stream.episode) { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onPlay()
                },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.downloads_episode_title, stream.episode),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            val voice = stream.audioNames.getOrNull(
                stream.audioOrder.indexOf(stream.audioIndex).takeIf { it >= 0 } ?: stream.audioIndex,
            )?.takeIf { it.isNotBlank() }
                ?: stream.audioNames.getOrNull(stream.audioIndex)?.takeIf { it.isNotBlank() }
            Text(
                text = listOfNotNull(qualityLabel(stream.height), voice).joinToString(" · "),
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (progress) {
                Spacer(Modifier.height(6.dp))
                DownloadProgressControls(
                    status = status,
                    caption = offlineSizeLabel(status) ?: offlineProgressTitle(status),
                    onPause = onPause,
                    onResume = onResume,
                    onDelete = onRemove,
                    showActions = false,
                )
            } else {
                offlineSizeLabel(status)?.let { size ->
                    Text(
                        text = "${offlineProgressTitle(status)} · $size",
                        color = if (status.phase == OfflinePhase.Ready) Color.White else CinemaMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } ?: Text(
                    text = offlineProgressTitle(status),
                    color = if (status.phase == OfflinePhase.Ready) Color.White else CinemaMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (status.phase.canPause() || status.phase == OfflinePhase.Paused) {
                DownloadIconButton(
                    icon = if (status.phase == OfflinePhase.Paused) {
                        R.drawable.ic_player_play
                    } else {
                        R.drawable.ic_player_pause
                    },
                    description = stringResource(
                        if (status.phase == OfflinePhase.Paused) {
                            R.string.download_resume
                        } else {
                            R.string.download_pause
                        },
                    ),
                    onClick = {
                        if (status.phase == OfflinePhase.Paused) onResume() else onPause()
                    },
                )
                Spacer(Modifier.height(6.dp))
            }
            DownloadIconButton(
                icon = R.drawable.ic_action_delete,
                description = stringResource(R.string.download_delete),
                onClick = onRemove,
            )
        }
    }
}

@Composable
private fun DownloadIconButton(
    icon: Int,
    description: String,
    onClick: () -> Unit,
) {
    val tick = rememberHapticTick()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(IconBtnShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun OfflineItem.libraryCaption(statusOf: (OfflineItem) -> OfflineStatus): String {
    if (!series()) {
        val stream = streams.firstOrNull()
        val status = statusOf(this)
        if (status.phase == OfflinePhase.Ready && stream != null) {
            return listOfNotNull(qualityLabel(stream.height), offlineSizeLabel(status)).joinToString(" · ")
        }
        return listOfNotNull(offlineProgressTitle(status), offlineSizeLabel(status)).joinToString(" · ")
    }
    val count = stringResource(episodeCountRes(streams.size), streams.size)
    val rows = streams.map { stream -> stream to statusOf(copy(streams = listOf(stream))) }
    val active = rows.firstOrNull { (_, status) -> status.phase.inProgress() }
    if (active != null) {
        val label = offlineProgressTitle(active.second)
        val size = offlineSizeLabel(active.second)
        return listOfNotNull(
            count,
            stringResource(R.string.downloads_episode_title, active.first.episode),
            label,
            size,
        ).joinToString(" · ")
    }
    if (rows.all { it.second.phase == OfflinePhase.Ready }) return count
    return "$count · ${stringResource(R.string.download_failed)}"
}

private fun OfflineItem.series(): Boolean = streams.any { it.season > 0 || it.episode > 0 }

private fun episodeCountRes(count: Int): Int {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> R.string.downloads_episode_many
        mod10 == 1 -> R.string.downloads_episode_one
        mod10 in 2..4 -> R.string.downloads_episode_few
        else -> R.string.downloads_episode_many
    }
}
