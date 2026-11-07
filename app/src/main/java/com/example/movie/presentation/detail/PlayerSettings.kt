package com.example.movie.presentation.detail

import androidx.annotation.OptIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.movie.data.player.audioCodeIndex
import com.example.movie.data.player.audioTrackLabel
import com.example.movie.data.player.namedTrackLabel
import com.example.movie.data.player.videoTrackLabel

internal enum class PictureMode(val resizeMode: Int, val label: Int) {
    Fit(AspectRatioFrameLayout.RESIZE_MODE_FIT, R.string.player_fit),
    Zoom(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, R.string.player_zoom),
    Fill(AspectRatioFrameLayout.RESIZE_MODE_FILL, R.string.player_fill),
}

internal enum class PlayerPage { Root, Quality, Audio, Episodes, Subtitles, Speed, Scale, Delay, Brightness }

private val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private val delays = listOf(
    -500_000L to R.string.player_sound_early_500,
    -100_000L to R.string.player_sound_early_100,
    0L to R.string.player_delay_zero,
    100_000L to R.string.player_sound_late_100,
    500_000L to R.string.player_sound_late_500,
)

private data class TrackRow(val group: Tracks.Group, val index: Int, val label: String)

@OptIn(UnstableApi::class)
@Composable
internal fun PlayerMenu(
    player: ExoPlayer,
    tracks: Tracks,
    page: PlayerPage,
    picture: PictureMode,
    episodes: List<WatchEpisode>,
    selectedEpisode: WatchEpisode?,
    delayUs: Long,
    brightness: Float?,
    autoNext: Boolean,
    underCamera: Boolean,
    audioNames: List<String>,
    audioOrder: List<Int>,
    onPage: (PlayerPage) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onPicture: (PictureMode) -> Unit,
    onEpisode: (WatchEpisode) -> Unit,
    onAudio: (Tracks.Group, Int) -> Unit,
    onDelay: (Long) -> Unit,
    onBrightness: (Float?) -> Unit,
    onAutoNext: (Boolean) -> Unit,
    onUnderCamera: (Boolean) -> Unit,
) {
    val video = remember(tracks) { rows(tracks, C.TRACK_TYPE_VIDEO) }
    val audio = remember(tracks, audioNames, audioOrder) {
        rows(tracks, C.TRACK_TYPE_AUDIO, audioNames, audioOrder)
    }
    val text = remember(tracks) { rows(tracks, C.TRACK_TYPE_TEXT) }
    val title = when (page) {
        PlayerPage.Root -> stringResource(R.string.player_menu)
        PlayerPage.Quality -> stringResource(R.string.player_quality)
        PlayerPage.Audio -> stringResource(R.string.player_audio)
        PlayerPage.Episodes -> stringResource(R.string.player_episodes)
        PlayerPage.Subtitles -> stringResource(R.string.player_subtitles)
        PlayerPage.Speed -> stringResource(R.string.player_speed)
        PlayerPage.Scale -> stringResource(R.string.player_picture)
        PlayerPage.Delay -> stringResource(R.string.player_delay)
        PlayerPage.Brightness -> stringResource(R.string.player_brightness)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xEE111C22))
            .padding(top = 8.dp, bottom = 12.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
            MenuIcon(
                icon = R.drawable.ic_player_back,
                description = stringResource(R.string.player_back),
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = onBack,
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 56.dp),
            )
            MenuIcon(
                icon = R.drawable.ic_player_close,
                description = stringResource(R.string.player_close),
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onClose,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                PlayerPage.Root -> {
                    NavRow(stringResource(R.string.player_speed), speedLabel(player.playbackParameters.speed)) {
                        onPage(PlayerPage.Speed)
                    }
                    NavRow(stringResource(R.string.player_picture), stringResource(picture.label)) {
                        onPage(PlayerPage.Scale)
                    }
                    NavRow(stringResource(R.string.player_delay), stringResource(delayLabel(delayUs))) {
                        onPage(PlayerPage.Delay)
                    }
                    NavRow(
                        stringResource(R.string.player_brightness),
                        if (brightness == null) {
                            stringResource(R.string.player_brightness_system)
                        } else {
                            "${(brightness * 100).toInt()}%"
                        },
                    ) { onPage(PlayerPage.Brightness) }
                    NavRow(
                        stringResource(R.string.player_under_camera),
                        stringResource(if (underCamera) R.string.player_on else R.string.player_off),
                    ) { onUnderCamera(!underCamera) }
                    if (episodes.size > 1) {
                        NavRow(
                            stringResource(R.string.player_auto_next),
                            stringResource(if (autoNext) R.string.player_on else R.string.player_off),
                        ) { onAutoNext(!autoNext) }
                    }
                }
                PlayerPage.Quality -> {
                    Choice(stringResource(R.string.player_quality_auto), overrideIndex(player, video.firstOrNull()?.group) == null) {
                        player.selectTrack(C.TRACK_TYPE_VIDEO, null, null)
                        onClose()
                    }
                    if (video.isEmpty()) Missing() else video.forEach { row ->
                        Choice(row.label, overrideIndex(player, row.group) == row.index) {
                            player.selectTrack(C.TRACK_TYPE_VIDEO, row.group, row.index)
                            onClose()
                        }
                    }
                }
                PlayerPage.Audio -> {
                    if (audio.isEmpty()) Missing() else audio.forEach { row ->
                        Choice(row.label, row.group.isTrackSelected(row.index)) {
                            onAudio(row.group, row.index)
                            onClose()
                        }
                    }
                }
                PlayerPage.Episodes -> episodes.forEach { item ->
                    Choice(
                        stringResource(R.string.player_episode, item.season, item.episode),
                        item.season == selectedEpisode?.season && item.episode == selectedEpisode.episode,
                    ) {
                        onEpisode(item)
                        onClose()
                    }
                }
                PlayerPage.Subtitles -> {
                    val subsOff = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
                    Choice(stringResource(R.string.player_subtitles_off), subsOff || text.none { it.group.isTrackSelected(it.index) }) {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .build()
                        onClose()
                    }
                    if (text.isEmpty()) Missing() else text.forEach { row ->
                        Choice(row.label, !subsOff && row.group.isTrackSelected(row.index)) {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(TrackSelectionOverride(row.group.mediaTrackGroup, listOf(row.index)))
                                .build()
                            onClose()
                        }
                    }
                }
                PlayerPage.Speed -> speeds.forEach { speed ->
                    val label = if (speed == 1f) stringResource(R.string.player_speed_normal) else "${speed}×"
                    Choice(label, player.playbackParameters.speed == speed) {
                        player.setPlaybackSpeed(speed)
                        onClose()
                    }
                }
                PlayerPage.Scale -> PictureMode.entries.forEach { mode ->
                    Choice(stringResource(mode.label), picture == mode) {
                        onPicture(mode)
                        onClose()
                    }
                }
                PlayerPage.Delay -> delays.forEach { (us, label) ->
                    Choice(stringResource(label), delayUs == us) {
                        onDelay(us)
                        onClose()
                    }
                }
                PlayerPage.Brightness -> {
                    Choice(stringResource(R.string.player_brightness_system), brightness == null) {
                        onBrightness(null)
                    }
                    Slider(
                        value = brightness ?: 0.5f,
                        onValueChange = onBrightness,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun speedLabel(speed: Float): String =
    if (speed == 1f) stringResource(R.string.player_speed_normal) else "${speed}×"

private fun delayLabel(delayUs: Long): Int = delays.firstOrNull { it.first == delayUs }?.second ?: R.string.player_delay_zero

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
private fun rows(
    tracks: Tracks,
    type: Int,
    audioNames: List<String> = emptyList(),
    audioOrder: List<Int> = emptyList(),
): List<TrackRow> {
    val rows = ArrayList<TrackRow>()
    tracks.groups.forEach { group ->
        if (group.type != type) return@forEach
        for (index in 0 until group.length) {
            if (!group.isTrackSupported(index)) continue
            val format = group.getTrackFormat(index)
            val label = when (type) {
                C.TRACK_TYPE_VIDEO -> videoTrackLabel(format.height, format.bitrate)
                C.TRACK_TYPE_AUDIO -> audioTrackLabel(format.label, format.language, rows.size, audioNames)
                else -> namedTrackLabel(format.label, format.language, rows.size)
            }
            rows += TrackRow(group, index, label)
        }
    }
    if (type == C.TRACK_TYPE_VIDEO) {
        rows.sortByDescending { it.group.getTrackFormat(it.index).height }
        return rows
    }
    if (type != C.TRACK_TYPE_AUDIO) return rows
    val unique = LinkedHashMap<String, TrackRow>()
    rows.forEach { row ->
        val previous = unique[row.label]
        if (previous == null || row.group.isTrackSelected(row.index)) unique[row.label] = row
    }
    return unique.values.sortedBy { row ->
        val code = audioCodeIndex(row.group.getTrackFormat(row.index).label)
        val position = code?.let { audioOrder.indexOf(it) } ?: -1
        if (position >= 0) position else code ?: Int.MAX_VALUE
    }
}

@Composable
private fun Missing() {
    Text(
        text = stringResource(R.string.player_unavailable),
        color = Color(0xFF8E8E93),
        fontSize = 16.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

@Composable
private fun NavRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = Color(0xFFB0B0B0), fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_player_next),
            contentDescription = null,
            tint = Color(0xFFB0B0B0),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (selected) Text(text = "✓", color = Color.White, fontSize = 18.sp)
    }
}

@Composable
private fun MenuIcon(icon: Int, description: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .padding(horizontal = 8.dp)
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}
