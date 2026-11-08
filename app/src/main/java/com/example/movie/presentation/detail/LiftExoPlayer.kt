package com.example.movie.presentation.detail

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
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
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.movie.R
import com.example.movie.data.player.LIFT_USER_AGENT
import com.example.movie.data.player.WatchEpisode
import com.example.movie.data.player.WatchKind
import com.example.movie.data.player.WatchPlayback
import com.example.movie.data.player.WatchStream
import com.example.movie.data.player.fallbackManifest
import com.example.movie.data.player.preferRicherManifest
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.ShellIcons
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
internal fun LiftExoPlayer(
    playback: WatchPlayback,
    title: String,
    logoUrl: String?,
    startPositionMs: Long = 0L,
    onProgress: (Long, Long, Int?, Int?) -> Unit = { _, _, _, _ -> },
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    val activity = view.context.hostActivity()
    val settingsRepository = LocalUserSettings.current
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()
    val delayProcessor = remember { AudioDelayProcessor() }
    var current by remember(playback) { mutableStateOf(playback.stream) }
    val player = remember(playback) {
        val source = DefaultHttpDataSource.Factory()
            .setUserAgent(LIFT_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
        val selector = DefaultTrackSelector(activity).apply {
            parameters = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setExceedVideoConstraintsIfNecessary(true)
                .clearViewportSizeConstraints()
                .setAudioOffloadPreferences(
                    AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                        .build(),
                )
                .build()
        }
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
            .setTrackSelector(selector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(activity).setDataSourceFactory(source))
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build()
            .apply { playWhenReady = true }
    }
    var pendingStart by remember(playback) { mutableLongStateOf(startPositionMs) }
    LaunchedEffect(current.url, current.kind, current.manifestPicked, current.alternateUrl) {
        if (!current.manifestPicked && current.alternateUrl != null) {
            val picked = withContext(Dispatchers.IO) { current.preferRicherManifest() }
            current = picked
            return@LaunchedEffect
        }
        val playing = player.currentMediaItem?.localConfiguration?.uri?.toString()
        if (playing == current.url && player.playbackState != Player.STATE_IDLE) return@LaunchedEffect
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
        val jump = pendingStart
        if (jump > 1_000L) {
            player.seekTo(jump)
            pendingStart = 0L
        }
    }
    var tracks by remember(player) { mutableStateOf(player.currentTracks) }
    var page by remember(player) { mutableStateOf<PlayerPage?>(null) }
    var fromSettings by remember(player) { mutableStateOf(false) }
    var picture by remember(player) { mutableStateOf(settings.playerPicture.toPictureMode()) }
    var chrome by remember(player) { mutableStateOf(true) }
    var locked by remember(player) { mutableStateOf(false) }
    var poke by remember(player) { mutableIntStateOf(0) }
    var playing by remember(player) { mutableStateOf(player.isPlaying) }
    var buffering by remember(player) { mutableStateOf(player.playbackState == Player.STATE_BUFFERING) }
    var position by remember(player) { mutableLongStateOf(0L) }
    var duration by remember(player) { mutableLongStateOf(0L) }
    var scrubbing by remember(player) { mutableStateOf(false) }
    var delayUs by remember(player) { mutableLongStateOf(settings.playerDelayUs) }
    var brightness by remember(player) { mutableStateOf(settings.playerBrightness) }
    var autoNext by remember(player) { mutableStateOf(settings.playerAutoNext) }
    var underCamera by remember(player) { mutableStateOf(settings.playerUnderCamera) }
    var hdr by remember(player) { mutableStateOf(settings.playerHdr) }
    var qualityPinned by remember(player) { mutableStateOf(false) }
    var inPip by remember(player) { mutableStateOf(activity.isInPictureInPictureMode) }
    val episode = playback.episodes.firstOrNull { it.stream.sameSource(current) }
    val episodeIndex = playback.episodes.indexOfFirst { it.stream.sameSource(current) }
    val follow = remember(playback) {
        object {
            var index = -1
            var auto = false
            var episodes: List<WatchEpisode> = emptyList()
            var stream: WatchStream = playback.stream
            var season: Int? = null
            var episode: Int? = null
            var emit: (Long, Long, Int?, Int?) -> Unit = { _, _, _, _ -> }
        }
    }
    follow.index = episodeIndex
    follow.auto = autoNext
    follow.episodes = playback.episodes
    follow.stream = current
    follow.season = episode?.season
    follow.episode = episode?.episode
    follow.emit = onProgress
    val haze = rememberHazeState()
    DisposableEffect(activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity.window
        val previous = window.attributes.screenBrightness
        val previousCutout = window.attributes.layoutInDisplayCutoutMode
        val previousColor = window.colorMode
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        val pipListener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> {
            inPip = it.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(pipListener)
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity.removeOnPictureInPictureModeChangedListener(pipListener)
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.colorMode = previousColor
            window.attributes = window.attributes.apply {
                screenBrightness = previous
                layoutInDisplayCutoutMode = previousCutout
            }
        }
    }
    LaunchedEffect(hdr) {
        activity.window.colorMode = if (hdr && activity.displaySupportsHdr()) {
            ActivityInfo.COLOR_MODE_HDR
        } else {
            ActivityInfo.COLOR_MODE_DEFAULT
        }
    }
    LaunchedEffect(current.url) {
        qualityPinned = false
    }
    LaunchedEffect(tracks, hdr, qualityPinned) {
        if (!qualityPinned) player.preferVideoRange(hdr, activity.displaySupportsHdr())
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

            override fun onPlayerError(error: PlaybackException) {
                val next = follow.stream.fallbackManifest() ?: return
                if (next.url == follow.stream.url) return
                current = next
            }
        }
        player.addListener(listener)
        onDispose {
            val played = player.currentPosition
            val length = player.duration
            if (length > 0L && played > 0L) follow.emit(played, length, follow.season, follow.episode)
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(current.url) {
        var reported = 0L
        while (true) {
            if (!scrubbing) {
                position = player.currentPosition.coerceAtLeast(0L)
                duration = player.duration.takeIf { it > 0 } ?: 0L
                playing = player.isPlaying
                val now = android.os.SystemClock.elapsedRealtime()
                if (duration > 0L && position > 0L && now - reported >= 5_000L) {
                    reported = now
                    follow.emit(position, duration, follow.season, follow.episode)
                }
            }
            delay(250)
        }
    }
    LaunchedEffect(player) {
        player.setPlaybackSpeed(settings.playerSpeed)
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
                .hazeSource(haze)
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
            val chromeInsets = if (underCamera) {
                WindowInsets.systemBars.union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            } else {
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { chrome = false },
                    ),
            ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(chromeInsets)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                TopBar(
                    title = title,
                    logoUrl = logoUrl,
                    showLogo = settings.playerLogo,
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
                    caption = trackCaption(
                        player,
                        tracks,
                        current.audioNames,
                        current.audioOrder,
                        qualityPinned,
                    ),
                )
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CenterControls(
                    haze = haze,
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
                showLogo = settings.playerLogo,
                hdr = hdr,
                qualityPinned = qualityPinned,
                audioNames = current.audioNames,
                audioOrder = current.audioOrder,
                onPage = { page = it },
                onBack = {
                    if (fromSettings && page != PlayerPage.Root) page = PlayerPage.Root else closeMenu()
                },
                onClose = { closeMenu() },
                onSpeed = { speed ->
                    player.setPlaybackSpeed(speed)
                    settingsRepository.setPlayerSpeed(speed)
                },
                onPicture = {
                    picture = it
                    settingsRepository.setPlayerPicture(it.stored())
                },
                onEpisode = { current = it.stream },
                onAudio = { group, index -> player.switchAudio(group, index) },
                onDelay = { picked ->
                    delayUs = picked
                    delayProcessor.delayUs = picked
                    settingsRepository.setPlayerDelayUs(picked)
                    player.seekTo(player.currentPosition)
                },
                onBrightness = {
                    brightness = it
                    settingsRepository.setPlayerBrightness(it)
                },
                onAutoNext = {
                    autoNext = it
                    settingsRepository.setPlayerAutoNext(it)
                },
                onUnderCamera = {
                    underCamera = it
                    settingsRepository.setPlayerUnderCamera(it)
                },
                onShowLogo = settingsRepository::setPlayerLogo,
                onHdr = {
                    hdr = it
                    qualityPinned = false
                    settingsRepository.setPlayerHdr(it)
                },
                onVideoPinned = { qualityPinned = it },
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

private fun Activity.displaySupportsHdr(): Boolean {
    val display = display ?: return false
    return display.supportedModes.any { it.supportedHdrTypes.isNotEmpty() }
}

@OptIn(UnstableApi::class)
private fun Format.isHdr(): Boolean {
    val transfer = colorInfo?.colorTransfer ?: Format.NO_VALUE
    if (transfer == C.COLOR_TRANSFER_ST2084 || transfer == C.COLOR_TRANSFER_HLG) return true
    if (sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION) return true
    val codec = codecs ?: return false
    return codec.startsWith("dvh1") || codec.startsWith("dvhe") || codec.startsWith("dav1")
}

private data class VideoPick(val group: Tracks.Group, val indices: List<Int>)

@OptIn(UnstableApi::class)
private fun Tracks.pickVideo(preferHdr: Boolean, displayHdr: Boolean): VideoPick? {
    data class Item(
        val group: Tracks.Group,
        val index: Int,
        val hdr: Boolean,
        val withinCaps: Boolean,
        val pixels: Int,
        val bitrate: Int,
    )
    val items = ArrayList<Item>()
    groups.forEach { group ->
        if (group.type != C.TRACK_TYPE_VIDEO) return@forEach
        for (index in 0 until group.length) {
            if (!group.isTrackSupported(index, true)) continue
            val format = group.getTrackFormat(index)
            val hdr = format.isHdr()
            items += Item(
                group = group,
                index = index,
                hdr = hdr,
                withinCaps = group.isTrackSupported(index),
                pixels = format.width.coerceAtLeast(0) * format.height.coerceAtLeast(0),
                bitrate = format.bitrate.coerceAtLeast(0),
            )
        }
    }
    if (items.isEmpty()) return null
    val wanted = items.filter { item ->
        item.hdr == preferHdr && (item.withinCaps || (preferHdr && displayHdr && item.hdr))
    }
    val pool = wanted.ifEmpty { items.filter { it.withinCaps }.ifEmpty { items } }
    val best = pool.maxWith(compareBy(Item::withinCaps, Item::pixels, Item::bitrate))
    val indices = pool.filter { it.group == best.group }.map { it.index }.sorted()
    return VideoPick(best.group, indices)
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.preferVideoRange(preferHdr: Boolean, displayHdr: Boolean) {
    val picked = currentTracks.pickVideo(preferHdr, displayHdr) ?: return
    val supported = (0 until picked.group.length).filter { picked.group.isTrackSupported(it, true) }
    val otherVideo = currentTracks.groups.any { group ->
        group !== picked.group &&
            group.type == C.TRACK_TYPE_VIDEO &&
            (0 until group.length).any { group.isTrackSupported(it, true) }
    }
    val desired = if (otherVideo || picked.indices.toSet() != supported.toSet()) picked.indices else emptyList()
    val videoGroups = currentTracks.groups
        .filter { it.type == C.TRACK_TYPE_VIDEO }
        .map { it.mediaTrackGroup }
        .toSet()
    val videoOverrides = trackSelectionParameters.overrides.filterKeys { it in videoGroups }
    val same = if (desired.isEmpty()) {
        videoOverrides.isEmpty()
    } else {
        videoOverrides.size == 1 &&
            videoOverrides[picked.group.mediaTrackGroup]?.trackIndices?.sorted() == desired
    }
    if (same) return
    val builder = trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO)
    if (desired.isNotEmpty()) {
        builder.setOverrideForType(TrackSelectionOverride(picked.group.mediaTrackGroup, desired))
    }
    trackSelectionParameters = builder.build()
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
    logoUrl: String?,
    showLogo: Boolean,
    episode: WatchEpisode?,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onPip: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShellIcons.back,
                contentDescription = stringResource(R.string.player_exit),
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            TitleMark(title = title, logoUrl = logoUrl, showLogo = showLogo)
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
    haze: HazeState,
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
            BareControl(
                modifier = Modifier.padding(start = 8.dp).size(48.dp),
                onClick = onPrevious,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_player_prev),
                    contentDescription = stringResource(R.string.player_episode_prev),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        GlassCircle(
            haze = haze,
            modifier = Modifier.padding(horizontal = 14.dp).size(70.dp),
            onClick = onPlay,
        ) {
            if (playing) {
                Icon(
                    imageVector = Pause,
                    contentDescription = stringResource(R.string.player_play_pause),
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.player_play_pause),
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        if (hasEpisodes) {
            BareControl(
                modifier = Modifier.padding(end = 8.dp).size(48.dp),
                onClick = onNext,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_player_next),
                    contentDescription = stringResource(R.string.player_episode_next),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        SeekButton(rewind = false, onClick = onForward)
    }
}

private val Pause: ImageVector = ImageVector.Builder(
    name = "Pause",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(8f, 19f)
        curveTo(9.1f, 19f, 10f, 18.1f, 10f, 17f)
        verticalLineTo(7f)
        curveTo(10f, 5.9f, 9.1f, 5f, 8f, 5f)
        reflectiveCurveTo(6f, 5.9f, 6f, 7f)
        verticalLineTo(17f)
        curveTo(6f, 18.1f, 6.9f, 19f, 8f, 19f)
        close()
        moveTo(16f, 5f)
        curveTo(14.9f, 5f, 14f, 5.9f, 14f, 7f)
        verticalLineTo(17f)
        curveTo(14f, 18.1f, 14.9f, 19f, 16f, 19f)
        reflectiveCurveTo(18f, 18.1f, 18f, 17f)
        verticalLineTo(7f)
        curveTo(18f, 5.9f, 17.1f, 5f, 16f, 5f)
        close()
    }
}.build()

@Composable
private fun GlassCircle(
    haze: HazeState,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(CircleShape)
            .hazeEffect(
                state = haze,
                style = HazeDefaults.style(
                    backgroundColor = Color.White.copy(alpha = 0.14f),
                    tint = HazeTint(Color.White.copy(alpha = 0.24f)),
                    blurRadius = 24.dp,
                    noiseFactor = 0.16f,
                ),
            )
            .border(0.6.dp, Color.White.copy(alpha = 0.42f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun BareControl(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SeekButton(rewind: Boolean, onClick: () -> Unit) {
    BareControl(modifier = Modifier.size(54.dp), onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_player_seek),
            contentDescription = stringResource(if (rewind) R.string.player_rewind else R.string.player_forward),
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .size(26.dp)
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
private fun TitleMark(title: String, logoUrl: String?, showLogo: Boolean) {
    var failed by remember(logoUrl) { mutableStateOf(logoUrl.isNullOrBlank()) }
    if (showLogo && !failed) {
        AsyncImage(
            model = logoUrl,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            onError = { failed = true },
            modifier = Modifier.height(18.dp).widthIn(max = 112.dp),
        )
    } else {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BottomBar(
    position: Long,
    duration: Long,
    hasEpisodes: Boolean,
    caption: TrackCaption,
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
            SeekBar(
                position = position,
                duration = duration,
                onScrub = onScrub,
                onScrubFinished = onScrubFinished,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            TimeText(duration)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Action(
                R.drawable.ic_player_quality,
                caption.quality ?: stringResource(R.string.player_quality_auto),
                onQuality,
            )
            Action(
                R.drawable.ic_player_audio,
                caption.audio ?: stringResource(R.string.player_audio),
                onAudio,
            )
            if (hasEpisodes) {
                Action(R.drawable.ic_player_episodes, stringResource(R.string.player_episodes), onEpisodes)
            }
            Action(
                R.drawable.ic_player_subs,
                caption.subtitles ?: stringResource(R.string.player_subtitles),
                onSubtitles,
            )
        }
    }
}

@Composable
private fun Action(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .widthIn(max = 148.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = painterResource(icon), contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun SeekBar(
    position: Long,
    duration: Long,
    onScrub: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    BoxWithConstraints(
        modifier
            .height(28.dp)
            .pointerInput(duration) {
                if (duration <= 0L) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun at(x: Float): Long {
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        return (x / width * duration).toLong().coerceIn(0L, duration)
                    }
                    onScrub(at(down.position.x))
                    drag(down.id) { change ->
                        onScrub(at(change.position.x))
                        change.consume()
                    }
                    onScrubFinished()
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumb = 14.dp
        val travel = (maxWidth - thumb).coerceAtLeast(0.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.35f)),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White),
        )
        Box(
            Modifier
                .offset(x = travel * fraction)
                .size(thumb)
                .clip(CircleShape)
                .background(Color.White),
        )
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

private fun WatchStream.sameSource(other: WatchStream): Boolean {
    val left = listOfNotNull(url, alternateUrl)
    val right = listOfNotNull(other.url, other.alternateUrl)
    return left.any { it in right }
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
