package com.example.movie.presentation.detail

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.example.movie.data.download.MediaCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.movie.R
import com.example.movie.data.player.WatchEpisode
import com.example.movie.data.player.WatchKind
import com.example.movie.data.player.WatchPlayback
import com.example.movie.data.player.WatchStream
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
internal fun LiftExoPlayer(playback: WatchPlayback, title: String, onDismiss: () -> Unit) {
    val view = LocalView.current
    val activity = view.context.hostActivity()
    val delayProcessor = remember { AudioDelayProcessor() }
    var current by remember(playback) { mutableStateOf(playback.stream) }
    val player = remember(playback) {
        val source = MediaCache.readSource(activity)
        val renderers = object : DefaultRenderersFactory(activity) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParameters: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParameters)
                .setAudioProcessors(arrayOf(delayProcessor))
                .build()
        }
        ExoPlayer.Builder(activity, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(activity).setDataSourceFactory(source))
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build()
            .apply {
                setMediaItem(current.toMediaItem())
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .setAudioOffloadPreferences(
                        AudioOffloadPreferences.Builder()
                            .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                            .build(),
                    )
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
            .setAudioOffloadPreferences(
                AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                    .build(),
            )
            .build()
        player.prepare()
        player.playWhenReady = true
    }
    var tracks by remember(player) { mutableStateOf(player.currentTracks) }
    var page by remember(player) { mutableStateOf<PlayerPage?>(null) }
    var fromSettings by remember(player) { mutableStateOf(false) }
    var picture by remember(player) { mutableStateOf(PictureMode.Fit) }
    var chrome by remember(player) { mutableStateOf(true) }
    var locked by remember(player) { mutableStateOf(false) }
    var poke by remember(player) { mutableIntStateOf(0) }
    var playing by remember(player) { mutableStateOf(player.isPlaying) }
    var buffering by remember(player) { mutableStateOf(player.playbackState == Player.STATE_BUFFERING) }
    var position by remember(player) { mutableLongStateOf(0L) }
    var duration by remember(player) { mutableLongStateOf(0L) }
    var scrubbing by remember(player) { mutableStateOf(false) }
    var delayUs by remember(player) { mutableLongStateOf(0L) }
    var brightness by remember(player) { mutableStateOf<Float?>(null) }
    var autoNext by remember(player) { mutableStateOf(playback.episodes.size > 1) }
    var underCamera by remember(player) { mutableStateOf(false) }
    var inPip by remember(player) { mutableStateOf(activity.isInPictureInPictureMode) }
    val episode = playback.episodes.firstOrNull { it.stream.url == current.url }
    val episodeIndex = playback.episodes.indexOfFirst { it.stream.url == current.url }
    val follow = remember(playback) {
        object {
            var index = -1
            var auto = false
            var episodes: List<WatchEpisode> = emptyList()
        }
    }
    follow.index = episodeIndex
    follow.auto = autoNext
    follow.episodes = playback.episodes
    DisposableEffect(activity) {
        val window = activity.window
        val previous = window.attributes.screenBrightness
        val previousCutout = window.attributes.layoutInDisplayCutoutMode
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        val pipListener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> {
            inPip = it.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(pipListener)
        onDispose {
            activity.removeOnPictureInPictureModeChangedListener(pipListener)
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.attributes = window.attributes.apply {
                screenBrightness = previous
                layoutInDisplayCutoutMode = previousCutout
            }
        }
    }
    LaunchedEffect(underCamera) {
        val window = activity.window
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = if (underCamera) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(currentTracks: Tracks) {
                tracks = currentTracks
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED && follow.auto) {
                    follow.episodes.getOrNull(follow.index + 1)?.let { current = it.stream }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(current.url) {
        while (true) {
            if (!scrubbing) {
                position = player.currentPosition.coerceAtLeast(0L)
                duration = player.duration.takeIf { it > 0 } ?: 0L
            }
            delay(250)
        }
    }
    LaunchedEffect(delayUs) {
        delayProcessor.delayUs = delayUs
    }
    LaunchedEffect(brightness) {
        activity.window.attributes = activity.window.attributes.apply {
            screenBrightness = brightness ?: -1f
        }
    }
    LaunchedEffect(chrome, page, locked, playing, poke) {
        if (chrome && page == null && !locked && playing) {
            delay(3_000)
            chrome = false
        }
    }
    fun showChrome() {
        chrome = true
        poke++
    }
    fun open(target: PlayerPage, settings: Boolean) {
        page = target
        fromSettings = settings
        chrome = false
    }
    fun closeMenu() {
        page = null
        showChrome()
    }
    BackHandler {
        when {
            page != null && fromSettings && page != PlayerPage.Root -> page = PlayerPage.Root
            page != null -> closeMenu()
            locked -> locked = false
            else -> onDismiss()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (underCamera) Modifier else Modifier.windowInsetsPadding(WindowInsets.displayCutout),
                ),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = {
                it.player = player
                it.resizeMode = picture.resizeMode
            },
            onRelease = { it.player = null },
        )
        if (buffering && !inPip && !chrome) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).size(48.dp),
            )
        }
        val menu = page
        if (!inPip && menu == null && !chrome && !locked) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showChrome() },
                    ),
            )
        }
        if (!inPip && menu == null && chrome && !locked) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                TopBar(
                    title = title,
                    episode = episode,
                    onBack = onDismiss,
                    onLock = { locked = true },
                    onPip = { activity.enterPip() },
                    onSettings = { open(PlayerPage.Root, settings = true) },
                )
                Spacer(Modifier.weight(1f))
                BottomBar(
                    position = position,
                    duration = duration,
                    hasEpisodes = playback.episodes.size > 1,
                    onScrub = {
                        scrubbing = true
                        position = it
                        showChrome()
                    },
                    onScrubFinished = {
                        player.seekTo(position)
                        scrubbing = false
                        showChrome()
                    },
                    onQuality = { open(PlayerPage.Quality, settings = false) },
                    onAudio = { open(PlayerPage.Audio, settings = false) },
                    onEpisodes = { open(PlayerPage.Episodes, settings = false) },
                    onSubtitles = { open(PlayerPage.Subtitles, settings = false) },
                )
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CenterControls(
                    playing = playing,
                    hasEpisodes = playback.episodes.size > 1,
                    onRewind = {
                        player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
                        showChrome()
                    },
                    onPrevious = {
                        playback.episodes.getOrNull(episodeIndex - 1)?.let { current = it.stream }
                        showChrome()
                    },
                    onPlay = {
                        if (player.isPlaying) player.pause() else player.play()
                        showChrome()
                    },
                    onNext = {
                        playback.episodes.getOrNull(episodeIndex + 1)?.let { current = it.stream }
                        showChrome()
                    },
                    onForward = {
                        val end = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                        player.seekTo((player.currentPosition + 10_000).coerceAtMost(end))
                        showChrome()
                    },
                )
            }
        }
        if (!inPip && locked && menu == null) {
            PlayerGlyph(
                icon = R.drawable.ic_player_lock,
                description = stringResource(R.string.player_unlock),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
                    .padding(top = 8.dp, end = 12.dp)
                    .size(48.dp),
                onClick = { locked = false },
            )
        }
        if (!inPip && menu != null) {
            PlayerMenu(
                player = player,
                tracks = tracks,
                page = menu,
                picture = picture,
                episodes = playback.episodes,
                selectedEpisode = episode,
                delayUs = delayUs,
                brightness = brightness,
                autoNext = autoNext,
                underCamera = underCamera,
                audioNames = current.audioNames,
                audioOrder = current.audioOrder,
                onPage = { page = it },
                onBack = {
                    if (fromSettings && page != PlayerPage.Root) page = PlayerPage.Root else closeMenu()
                },
                onClose = { closeMenu() },
                onPicture = { picture = it },
                onEpisode = { current = it.stream },
                onAudio = { group, index -> player.switchAudio(group, index) },
                onDelay = { picked ->
                    delayUs = picked
                    delayProcessor.delayUs = picked
                    player.seekTo(player.currentPosition)
                },
                onBrightness = { brightness = it },
                onAutoNext = { autoNext = it },
                onUnderCamera = { underCamera = it },
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.switchAudio(group: Tracks.Group, index: Int) {
    if (group.isTrackSelected(index)) return
    val at = currentPosition
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(index)))
        .build()
    seekTo(at)
}

private fun Context.hostActivity(): ComponentActivity {
    var current = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    error("no activity")
}

private fun Activity.enterPip() {
    val params = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .build()
    runCatching { enterPictureInPictureMode(params) }
}

@Composable
private fun TopBar(
    title: String,
    episode: WatchEpisode?,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onPip: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        PlayerGlyph(R.drawable.ic_player_back, stringResource(R.string.player_exit), Modifier.size(48.dp), onBack)
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (episode != null) {
                Text(
                    text = stringResource(R.string.player_episode, episode.season, episode.episode),
                    color = Color.White.copy(alpha = 0.87f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        PlayerGlyph(R.drawable.ic_player_unlock, stringResource(R.string.player_lock), Modifier.size(44.dp), onLock)
        PlayerGlyph(R.drawable.ic_player_pip, stringResource(R.string.player_pip), Modifier.size(44.dp), onPip)
        PlayerGlyph(R.drawable.ic_player_settings, stringResource(R.string.player_settings), Modifier.size(44.dp), onSettings)
    }
}

@Composable
private fun CenterControls(
    playing: Boolean,
    hasEpisodes: Boolean,
    onRewind: () -> Unit,
    onPrevious: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onForward: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        SeekButton(rewind = true, onClick = onRewind)
        if (hasEpisodes) {
            PlayerGlyph(R.drawable.ic_player_prev, stringResource(R.string.player_episode_prev), Modifier.padding(start = 8.dp).size(56.dp), onPrevious)
        }
        PlayerGlyph(
            icon = if (playing) R.drawable.ic_player_pause else R.drawable.ic_player_play,
            description = stringResource(R.string.player_play_pause),
            modifier = Modifier.padding(horizontal = 10.dp).size(76.dp),
            onClick = onPlay,
        )
        if (hasEpisodes) {
            PlayerGlyph(R.drawable.ic_player_next, stringResource(R.string.player_episode_next), Modifier.padding(end = 8.dp).size(56.dp), onNext)
        }
        SeekButton(rewind = false, onClick = onForward)
    }
}

@Composable
private fun SeekButton(rewind: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_player_seek),
            contentDescription = stringResource(if (rewind) R.string.player_rewind else R.string.player_forward),
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer { if (rewind) rotationY = 180f },
        )
        Text(
            text = "10",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun BottomBar(
    position: Long,
    duration: Long,
    hasEpisodes: Boolean,
    onScrub: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    onQuality: () -> Unit,
    onAudio: () -> Unit,
    onEpisodes: () -> Unit,
    onSubtitles: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TimeText(position)
            Slider(
                value = if (duration > 0) position.toFloat().coerceIn(0f, duration.toFloat()) else 0f,
                onValueChange = { onScrub(it.toLong()) },
                onValueChangeFinished = onScrubFinished,
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                enabled = duration > 0,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                    disabledThumbColor = Color.White,
                    disabledActiveTrackColor = Color.White.copy(alpha = 0.5f),
                ),
            )
            TimeText(duration)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Action(R.drawable.ic_player_quality, stringResource(R.string.player_quality), onQuality)
            Action(R.drawable.ic_player_audio, stringResource(R.string.player_audio), onAudio)
            if (hasEpisodes) Action(R.drawable.ic_player_episodes, stringResource(R.string.player_episodes), onEpisodes)
            Action(R.drawable.ic_player_subs, stringResource(R.string.player_subtitles), onSubtitles)
        }
    }
}

@Composable
private fun Action(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painter = painterResource(icon), contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun TimeText(ms: Long) {
    Text(text = formatPlayerTime(ms), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun PlayerGlyph(icon: Int, description: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(icon), contentDescription = description, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

private fun formatPlayerTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
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
