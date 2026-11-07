package com.example.movie.presentation.detail

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.movie.R
import com.example.movie.data.player.LIFT_USER_AGENT
import com.example.movie.data.player.WatchKind
import com.example.movie.data.player.WatchPlayback
import com.example.movie.data.player.WatchStream

@OptIn(UnstableApi::class)
@Composable
internal fun LiftExoPlayer(playback: WatchPlayback, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var current by remember(playback) { mutableStateOf(playback.stream) }
    val player = remember(playback) {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(LIFT_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(http))
            .build()
            .apply {
                setMediaItem(current.toMediaItem())
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
                playWhenReady = true
                prepare()
            }
    }
    LaunchedEffect(current.url) {
        val playing = player.currentMediaItem?.localConfiguration?.uri?.toString()
        if (playing == current.url) return@LaunchedEffect
        val textOff = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
        player.setMediaItem(current.toMediaItem())
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverrides()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, textOff)
            .build()
        player.prepare()
        player.playWhenReady = true
    }
    var tracks by remember(player) { mutableStateOf(player.currentTracks) }
    var settings by remember(player) { mutableStateOf(false) }
    var picture by remember(player) { mutableStateOf(PictureMode.Fit) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(current: Tracks) {
                tracks = current
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    BackHandler {
        if (settings) settings = false else onDismiss()
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShowSubtitleButton(false)
                    useController = true
                    controllerShowTimeoutMs = 2_500
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener {
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)?.visibility =
                            android.view.View.GONE
                    })
                    findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)?.visibility =
                        android.view.View.GONE
                }
            },
            update = {
                it.player = player
                it.resizeMode = picture.resizeMode
            },
            onRelease = { it.player = null },
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 12.dp, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { settings = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.player_settings),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(end = 12.dp, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.section_back),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        if (settings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { settings = false },
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )) {
                    PlayerSettingsSheet(
                        player = player,
                        tracks = tracks,
                        picture = picture,
                        episodes = playback.episodes,
                        selectedEpisode = playback.episodes.firstOrNull { it.stream.url == current.url },
                        onEpisode = { current = it.stream },
                        onPicture = { picture = it },
                        onClose = { settings = false },
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
private fun WatchStream.toMediaItem(): MediaItem {
    val mime = if (kind == WatchKind.Hls) MimeTypes.APPLICATION_M3U8 else MimeTypes.APPLICATION_MPD
    val subtitles = subtitles.map { sub ->
        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
            .setMimeType(MimeTypes.TEXT_VTT)
            .setLabel(sub.name)
            .setLanguage("ru")
            .build()
    }
    return MediaItem.Builder()
        .setUri(url)
        .setMimeType(mime)
        .setSubtitleConfigurations(subtitles)
        .build()
}
