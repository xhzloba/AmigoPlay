package com.example.movie.presentation.detail

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.movie.R
import com.example.movie.data.download.OfflineItem
import com.example.movie.data.download.OfflinePhase
import com.example.movie.data.download.OfflineStatus
import com.example.movie.data.download.toPlayback
import com.example.movie.data.player.WatchPlayback
import com.example.movie.data.player.WatchStream
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface

private val SheetShape = RoundedCornerShape(22.dp)
private val ChipShape = RoundedCornerShape(14.dp)

internal fun qualityLabel(height: Int): String = if (height <= 0) "Лучшее" else "${height}p"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DownloadSheet(
    playback: WatchPlayback,
    heights: List<Int>,
    initialSeason: Int?,
    initialEpisode: Int?,
    status: (season: Int?, episode: Int?) -> OfflineStatus?,
    onDownload: (season: Int?, episode: Int?, height: Int, audioIndex: Int) -> Unit,
    onDelete: ((season: Int?, episode: Int?) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val episodes = playback.episodes
    val seasons = episodes.map { it.season }.distinct().sorted()
    val series = seasons.isNotEmpty()
    val opening = episodes.firstOrNull { it.stream.url == playback.stream.url }
    val startSeason = initialSeason?.takeIf { it in seasons } ?: opening?.season ?: seasons.firstOrNull() ?: 0
    var season by remember(playback, initialSeason) { mutableIntStateOf(startSeason) }
    val inSeason = episodes.filter { it.season == season }
    val startEpisode = initialEpisode?.takeIf { episode -> inSeason.any { it.episode == episode } }
        ?: inSeason.firstOrNull()?.episode
        ?: opening?.episode
        ?: 0
    var episode by remember(playback, season, initialEpisode) { mutableIntStateOf(startEpisode) }
    val current = inSeason.firstOrNull { it.episode == episode }?.stream ?: playback.stream
    val qualities = heights.ifEmpty { listOf(0) }
    var height by remember(playback) { mutableIntStateOf(qualities.first()) }
    val voices = current.audioRows()
    var audio by remember(playback, season, episode) {
        mutableIntStateOf(voices.firstOrNull()?.first ?: -1)
    }
    val currentStatus = status(if (series) season else null, if (series) episode else null)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(SheetShape)
                .background(CinemaSurface)
                .border(0.5.dp, Color.White.copy(alpha = 0.10f), SheetShape),
        ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.download_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.4).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.download_hint),
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            if (series) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.download_selected, season, episode),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (currentStatus != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = statusText(currentStatus), color = Color.White, fontSize = 14.sp)
            }
            if (series) {
                PickerLabel(stringResource(R.string.download_season))
                ChoiceGrid(
                    items = seasons.map { it.toString() to (it == season) },
                    onPick = { season = seasons[it] },
                )
                PickerLabel(stringResource(R.string.download_episode))
                ChoiceGrid(
                    items = inSeason.map { it.episode.toString() to (it.episode == episode) },
                    onPick = { episode = inSeason[it].episode },
                )
            }
            PickerLabel(stringResource(R.string.player_quality))
            ChoiceGrid(
                items = qualities.map { qualityLabel(it) to (it == height) },
                onPick = { height = qualities[it] },
            )
            if (voices.isNotEmpty()) {
                PickerLabel(stringResource(R.string.player_audio))
                voices.forEach { (index, name) ->
                    val selected = index == audio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(ChipShape)
                            .background(Color.White.copy(alpha = if (selected) 0.14f else 0.06f))
                            .border(
                                0.5.dp,
                                Color.White.copy(alpha = if (selected) 0.28f else 0.08f),
                                ChipShape,
                            )
                            .clickable(
                                interactionSource = remember(index) { MutableInteractionSource() },
                                indication = null,
                                onClick = { audio = index },
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (selected) Color.White else Color.Transparent)
                                .border(1.5.dp, Color.White.copy(alpha = if (selected) 1f else 0.35f), CircleShape),
                        )
                        Text(
                            text = name,
                            color = if (selected) Color.White else Color(0xFFD2D2D8),
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
            }
        }
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ChipShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onDownload(
                                if (series) season else null,
                                if (series) episode else null,
                                height,
                                audio,
                            )
                        },
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.download_title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (onDelete != null) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ChipShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onDelete(if (series) season else null, if (series) episode else null) },
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.download_delete),
                        color = CinemaMuted,
                        fontSize = 15.sp,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun PickerLabel(text: String) {
    Text(
        text = text,
        color = CinemaMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceGrid(items: List<Pair<String, Boolean>>, onPick: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, (label, selected) ->
            Box(
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .heightIn(min = 40.dp)
                    .clip(ChipShape)
                    .background(Color.White.copy(alpha = if (selected) 0.14f else 0.06f))
                    .border(0.5.dp, Color.White.copy(alpha = if (selected) 0.28f else 0.08f), ChipShape)
                    .clickable(
                        interactionSource = remember(label, index) { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPick(index) },
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

private fun WatchStream.audioRows(): List<Pair<Int, String>> {
    if (audioNames.isEmpty()) return emptyList()
    val order = audioOrder.ifEmpty { audioNames.indices.toList() }
    return order.mapNotNull { index -> audioNames.getOrNull(index)?.let { index to it } }
}

@Composable
private fun statusText(status: OfflineStatus): String = when (status.phase) {
    OfflinePhase.Ready -> stringResource(R.string.download_ready)
    OfflinePhase.Failed -> stringResource(R.string.download_failed)
    OfflinePhase.Preparing -> stringResource(R.string.download_preparing)
    OfflinePhase.Downloading -> stringResource(R.string.download_progress, status.percent)
}

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
                onDismiss = onDismiss,
            )
        }
    }
}
