package com.example.movie.presentation.detail

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.movie.R
import com.example.movie.data.player.WatchEpisode
import com.example.movie.data.player.namedTrackLabel
import com.example.movie.data.player.videoTrackLabel

internal enum class PictureMode(val resizeMode: Int, val label: Int) {
    Fit(AspectRatioFrameLayout.RESIZE_MODE_FIT, R.string.player_fit),
    Zoom(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, R.string.player_zoom),
    Fill(AspectRatioFrameLayout.RESIZE_MODE_FILL, R.string.player_fill),
}

private val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private data class TrackRow(val group: Tracks.Group, val index: Int, val label: String)

@OptIn(UnstableApi::class)
@Composable
internal fun PlayerSettingsSheet(
    player: ExoPlayer,
    tracks: Tracks,
    picture: PictureMode,
    episodes: List<WatchEpisode>,
    selectedEpisode: WatchEpisode?,
    onEpisode: (WatchEpisode) -> Unit,
    onPicture: (PictureMode) -> Unit,
    onClose: () -> Unit,
) {
    val video = remember(tracks) { rows(tracks, C.TRACK_TYPE_VIDEO) }
    val audio = remember(tracks) { rows(tracks, C.TRACK_TYPE_AUDIO) }
    val text = remember(tracks) { rows(tracks, C.TRACK_TYPE_TEXT) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xF0161616))
            .verticalScroll(rememberScrollState())
            .padding(top = 18.dp, bottom = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.player_settings),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        if (episodes.size > 1) {
            Section(stringResource(R.string.player_episodes))
            episodes.forEach { item ->
                Choice(
                    label = stringResource(R.string.player_episode, item.season, item.episode),
                    selected = item.season == selectedEpisode?.season && item.episode == selectedEpisode.episode,
                    onClick = {
                        onEpisode(item)
                        onClose()
                    },
                )
            }
        }
        Section(stringResource(R.string.player_quality))
        Choice(
            label = stringResource(R.string.player_quality_auto),
            selected = overrideIndex(player, video.firstOrNull()?.group) == null,
            onClick = {
                player.selectTrack(C.TRACK_TYPE_VIDEO, null, null)
                onClose()
            },
        )
        if (video.isEmpty()) {
            Missing()
        } else {
            video.forEach { row ->
                Choice(
                    label = row.label,
                    selected = overrideIndex(player, row.group) == row.index,
                    onClick = {
                        player.selectTrack(C.TRACK_TYPE_VIDEO, row.group, row.index)
                        onClose()
                    },
                )
            }
        }
        Section(stringResource(R.string.player_audio))
        if (audio.isEmpty()) {
            Missing()
        } else {
            audio.forEach { row ->
                Choice(
                    label = row.label,
                    selected = row.group.isTrackSelected(row.index),
                    onClick = {
                        player.selectTrack(C.TRACK_TYPE_AUDIO, row.group, row.index)
                        onClose()
                    },
                )
            }
        }
        Section(stringResource(R.string.player_subtitles))
        Choice(
            label = stringResource(R.string.player_subtitles_off),
            selected = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes || text.none { it.group.isTrackSelected(it.index) },
            onClick = {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .build()
                onClose()
            },
        )
        if (text.isEmpty()) {
            Missing()
        } else {
            val subsOff = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
            text.forEach { row ->
                Choice(
                    label = row.label,
                    selected = !subsOff && row.group.isTrackSelected(row.index),
                    onClick = {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .setOverrideForType(TrackSelectionOverride(row.group.mediaTrackGroup, listOf(row.index)))
                            .build()
                        onClose()
                    },
                )
            }
        }
        Section(stringResource(R.string.player_speed))
        speeds.forEach { speed ->
            val label = if (speed == 1f) stringResource(R.string.player_speed_normal) else "${speed}×"
            Choice(
                label = label,
                selected = player.playbackParameters.speed == speed,
                onClick = {
                    player.setPlaybackSpeed(speed)
                    onClose()
                },
            )
        }
        Section(stringResource(R.string.player_picture))
        PictureMode.entries.forEach { mode ->
            Choice(
                label = stringResource(mode.label),
                selected = picture == mode,
                onClick = {
                    onPicture(mode)
                    onClose()
                },
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.selectTrack(type: Int, group: Tracks.Group?, index: Int?) {
    val builder = trackSelectionParameters.buildUpon().clearOverridesOfType(type)
    if (group != null && index != null) {
        builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(index)))
    }
    trackSelectionParameters = builder.build()
}

@OptIn(UnstableApi::class)
private fun overrideIndex(player: ExoPlayer, group: Tracks.Group?): Int? {
    if (group == null) return null
    return player.trackSelectionParameters.overrides[group.mediaTrackGroup]?.trackIndices?.firstOrNull()
}

@OptIn(UnstableApi::class)
private fun rows(tracks: Tracks, type: Int): List<TrackRow> {
    val rows = ArrayList<TrackRow>()
    tracks.groups.forEach { group ->
        if (group.type != type) return@forEach
        for (index in 0 until group.length) {
            if (!group.isTrackSupported(index)) continue
            val format = group.getTrackFormat(index)
            val label = if (type == C.TRACK_TYPE_VIDEO) {
                videoTrackLabel(format.height, format.bitrate)
            } else {
                namedTrackLabel(format.label, format.language, rows.size)
            }
            rows += TrackRow(group, index, label)
        }
    }
    if (type == C.TRACK_TYPE_VIDEO) {
        rows.sortByDescending { it.group.getTrackFormat(it.index).height }
    }
    return rows
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        color = Color(0xFF8E8E93),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Missing() {
    Text(
        text = stringResource(R.string.player_unavailable),
        color = Color(0xFF6E6E73),
        fontSize = 15.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = if (selected) Color.White else Color.Transparent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = Color.White, fontSize = 16.sp)
    }
}
