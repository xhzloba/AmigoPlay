package com.example.movie.presentation.cast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.movie.R
import com.example.movie.data.cast.CastProxy
import com.example.movie.data.cast.DlnaDevice
import com.example.movie.data.cast.TvCastHub
import com.example.movie.data.cast.TvCastUi
import com.example.movie.data.cast.discoverRenderers
import com.example.movie.data.cast.lanIpv4
import com.example.movie.data.cast.runningOnEmulator
import com.example.movie.data.cast.TvPairing
import com.example.movie.data.cast.openTvBrowser
import com.example.movie.data.cast.playOnRenderer
import com.example.movie.data.cast.stopRenderer
import com.example.movie.data.player.WatchKind
import com.example.movie.data.player.WatchStream
import com.example.movie.data.player.fetchLiftStream
import com.example.movie.data.player.playlistHeights
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

class TvCastService : Service() {
    private val commands = Executors.newSingleThreadExecutor()
    private var proxy: CastProxy? = null
    private var host: String? = null
    private var mediaUrl: String? = null
    private var mediaTitle: String = ""
    private var preparedPage: String? = null
    private var hls: Boolean = false
    private var castRev: Int = 0
    private var controlUrl: String? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var session: MediaSession? = null

    @Volatile
    private var alive = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        alive = true
        ensureChannel()
        startForeground(
            NOTE_ID,
            note(mediaTitle),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        if (intent == null) return START_NOT_STICKY
        val action = intent.action
        val page = intent.getStringExtra(EXTRA_PAGE).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val control = intent.getStringExtra(EXTRA_CONTROL).orEmpty()
        val location = intent.getStringExtra(EXTRA_LOCATION).orEmpty()
        val quality = intent.getIntExtra(EXTRA_QUALITY, -1)
        val audio = intent.getIntExtra(EXTRA_AUDIO, 0)
        try {
            commands.execute {
                if (!alive) return@execute
                try {
                    when (action) {
                        ACTION_PREPARE -> prepare(page, title)
                        ACTION_PAGE -> retarget(page, title)
                        ACTION_SEARCH -> search()
                        ACTION_QUALITY -> chooseQuality(quality)
                        ACTION_AUDIO -> chooseAudio(audio)
                        ACTION_PLAY -> {
                            retarget(page, title)
                            play(name, control)
                        }
                        ACTION_BROWSER -> {
                            retarget(page, title)
                            openBrowser(name, location)
                        }
                        ACTION_STOP -> shutdown()
                    }
                } catch (error: Exception) {
                    TvCastHub.update {
                        it.copy(busy = false, message = getString(R.string.cast_failed, error.message.orEmpty()))
                    }
                }
            }
        } catch (_: RejectedExecutionException) {
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        alive = false
        release()
        commands.shutdownNow()
        super.onDestroy()
    }

    private fun prepare(pageUrl: String, title: String) {
        if (pageUrl.isBlank()) {
            TvCastHub.update { it.copy(busy = false, message = getString(R.string.cast_no_stream)) }
            return
        }
        TvCastHub.update {
            it.copy(busy = true, running = true, playingOn = null, message = getString(R.string.cast_preparing))
        }
        val emulator = runningOnEmulator()
        val address = lanIpv4(this)
        if (address == null && !emulator) {
            release()
            TvCastHub.update { TvCastUi(message = getString(R.string.cast_no_wifi)) }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val published = if (emulator) "127.0.0.1" else address!!
        val previousHost = host
        holdLocks(title)
        val current = if (proxy != null && previousHost == published) {
            proxy!!
        } else {
            val previous = proxy
            proxy = null
            previous?.stop()
            CastProxy(this, published).also {
                it.start()
                proxy = it
            }
        }
        host = published
        bindStream(current, pageUrl, title, emulator)
        if (!emulator) search()
    }

    private fun retarget(pageUrl: String, title: String) {
        if (pageUrl.isBlank() || pageUrl == preparedPage) return
        val current = proxy ?: return
        TvCastHub.update {
            it.copy(busy = true, message = getString(R.string.cast_preparing))
        }
        bindStream(current, pageUrl, title, runningOnEmulator())
    }

    private fun bindStream(current: CastProxy, pageUrl: String, title: String, emulator: Boolean) {
        val stream = preferHls(fetchLiftStream(pageUrl).stream)
        current.referer = pageUrl.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}/" }
        current.title = title.ifBlank { mediaTitle.ifBlank { getString(R.string.cast_notification_title) } }
        mediaTitle = current.title
        session?.setMetadata(
            MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, mediaTitle).build(),
        )
        runCatching { manager().notify(NOTE_ID, note(mediaTitle)) }
        val local = publish(current.localFor(stream.url))
        current.kind = if (stream.kind == WatchKind.Hls) "hls" else "dash"
        hls = stream.kind == WatchKind.Hls
        preparedPage = pageUrl
        val names = stream.audioNames
        val heights = playlistHeights(stream.url)
        val keepQuality = TvCastHub.state.value.quality?.takeIf { it in heights }
        current.qualityHeight = keepQuality
        current.audioIndex = 0
        current.audioName = names.firstOrNull()
        TvCastHub.update {
            it.copy(
                busy = false,
                running = true,
                hls = hls,
                browserUrl = current.origin + "/",
                streamUrl = local,
                qualities = heights,
                audios = names,
                quality = keepQuality,
                audio = 0,
                message = when {
                    emulator -> getString(R.string.cast_emulator, current.origin + "/")
                    hls -> getString(R.string.cast_ready)
                    else -> getString(R.string.cast_dash_only)
                },
            )
        }
    }

    private fun chooseQuality(height: Int) {
        val picked = height.takeIf { it > 0 }
        proxy?.qualityHeight = picked
        val next = publish(mediaUrl)
        TvCastHub.update { it.copy(quality = picked, streamUrl = next ?: it.streamUrl) }
    }

    private fun chooseAudio(index: Int) {
        val names = TvCastHub.state.value.audios
        val picked = index.coerceIn(0, (names.size - 1).coerceAtLeast(0))
        proxy?.audioIndex = picked
        proxy?.audioName = names.getOrNull(picked)
        val next = publish(mediaUrl)
        TvCastHub.update { it.copy(audio = picked, streamUrl = next ?: it.streamUrl) }
    }

    private fun publish(local: String?): String? {
        val bare = local?.substringBefore('?')?.takeIf { it.isNotBlank() } ?: return null
        castRev += 1
        val next = "$bare?v=$castRev"
        proxy?.playlistUrl = next
        mediaUrl = next
        return next
    }

    private fun search() {
        val address = host
        val current = proxy
        if (address == null || current == null) {
            TvCastHub.update { it.copy(message = getString(R.string.cast_preparing)) }
            return
        }
        TvCastHub.update { it.copy(busy = true, searching = true, message = getString(R.string.cast_searching)) }
        val devices = discoverRenderers(this, address)
        TvCastHub.update {
            it.copy(
                busy = false,
                searching = false,
                devices = devices,
                message = when {
                    devices.isEmpty() -> getString(R.string.cast_none)
                    !hls -> getString(R.string.cast_dash_only)
                    else -> getString(R.string.cast_ready)
                },
            )
        }
    }

    private fun play(name: String, control: String) {
        if (!hls) {
            TvCastHub.update { it.copy(message = getString(R.string.cast_dash_only)) }
            return
        }
        val url = publish(mediaUrl)
        if (url.isNullOrBlank() || control.isBlank()) return
        TvCastHub.update { it.copy(busy = true) }
        playOnRenderer(DlnaDevice(name, control, control), url, mediaTitle)
        controlUrl = control
        TvCastHub.update {
            it.copy(busy = false, playingOn = name, streamUrl = url, message = getString(R.string.cast_playing, name))
        }
    }

    private fun openBrowser(name: String, location: String) {
        val page = proxy?.let { it.origin + "/" }
        if (page.isNullOrBlank()) {
            TvCastHub.update { it.copy(busy = false, message = getString(R.string.cast_no_stream)) }
            return
        }
        val tv = location.toHttpUrlOrNull()?.host
        if (tv.isNullOrBlank()) throw IllegalStateException("нет адреса телевизора")
        TvCastHub.update { it.copy(busy = true, message = getString(R.string.cast_browser_allow)) }
        openTvBrowser(tv, page, TvPairing(this))
        TvCastHub.update {
            it.copy(busy = false, playingOn = name, message = getString(R.string.cast_browser_opened, name))
        }
    }

    private fun shutdown() {
        release()
        TvCastHub.update { TvCastUi() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun release() {
        val control = controlUrl
        controlUrl = null
        mediaUrl = null
        preparedPage = null
        castRev = 0
        if (control != null) runCatching { stopRenderer(control) }
        proxy?.stop()
        proxy = null
        host = null
        val wifi = wifiLock
        wifiLock = null
        if (wifi?.isHeld == true) runCatching { wifi.release() }
        val wake = wakeLock
        wakeLock = null
        if (wake?.isHeld == true) runCatching { wake.release() }
        session?.release()
        session = null
    }

    @Suppress("DEPRECATION")
    private fun holdLocks(title: String) {
        if (wakeLock == null) {
            val power = getSystemService(PowerManager::class.java)
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "movie:cast").apply { acquire() }
        }
        if (wifiLock == null) {
            val wifi = applicationContext.getSystemService(WifiManager::class.java)
            wifiLock = wifi?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "movie:cast")?.apply { acquire() }
        }
        val current = session ?: MediaSession(this, "tv-cast").also { session = it }
        current.setMetadata(
            MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title).build(),
        )
        current.setPlaybackState(
            PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0L, 1f).build(),
        )
        current.isActive = true
    }

    private fun manager() = getSystemService(NotificationManager::class.java)

    private fun ensureChannel() {
        val manager = manager()
        if (manager.getNotificationChannel(CHANNEL) != null) return
        val channel = NotificationChannel(
            CHANNEL,
            getString(R.string.cast_notification_title),
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    private fun note(work: String): android.app.Notification {
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, TvCastService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val headline = work.ifBlank { getString(R.string.cast_notification_title) }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bell)
            .setContentTitle(headline)
            .setContentText(getString(R.string.cast_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, getString(R.string.cast_stop), stop)
            .build()
    }

    companion object {
        const val ACTION_PREPARE = "prepare"
        const val ACTION_PAGE = "page"
        const val ACTION_SEARCH = "search"
        const val ACTION_QUALITY = "quality"
        const val ACTION_AUDIO = "audio"
        const val ACTION_PLAY = "play"
        const val ACTION_BROWSER = "browser"
        const val ACTION_STOP = "stop"
        const val EXTRA_PAGE = "page"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NAME = "name"
        const val EXTRA_CONTROL = "control"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_AUDIO = "audio"
        private const val CHANNEL = "tv_cast"
        private const val NOTE_ID = 73

        fun prepare(context: Context, pageUrl: String, title: String) {
            send(context, ACTION_PREPARE) {
                putExtra(EXTRA_PAGE, pageUrl)
                putExtra(EXTRA_TITLE, title)
            }
        }

        fun search(context: Context) = send(context, ACTION_SEARCH)

        fun retarget(context: Context, pageUrl: String, title: String) {
            send(context, ACTION_PAGE) {
                putExtra(EXTRA_PAGE, pageUrl)
                putExtra(EXTRA_TITLE, title)
            }
        }

        fun quality(context: Context, height: Int?) {
            send(context, ACTION_QUALITY) {
                putExtra(EXTRA_QUALITY, height ?: -1)
            }
        }

        fun audio(context: Context, index: Int) {
            send(context, ACTION_AUDIO) {
                putExtra(EXTRA_AUDIO, index)
            }
        }

        fun play(context: Context, name: String, controlUrl: String, pageUrl: String? = null, title: String? = null) {
            send(context, ACTION_PLAY) {
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_CONTROL, controlUrl)
                putExtra(EXTRA_PAGE, pageUrl)
                putExtra(EXTRA_TITLE, title)
            }
        }

        fun openBrowser(context: Context, name: String, location: String, pageUrl: String? = null, title: String? = null) {
            send(context, ACTION_BROWSER) {
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_LOCATION, location)
                putExtra(EXTRA_PAGE, pageUrl)
                putExtra(EXTRA_TITLE, title)
            }
        }

        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String, fill: Intent.() -> Unit = {}) {
            val intent = Intent(context, TvCastService::class.java).setAction(action).apply(fill)
            context.startForegroundService(intent)
        }
    }
}

private fun preferHls(stream: WatchStream): WatchStream {
    val alternate = stream.alternateUrl
    if (stream.kind == WatchKind.Dash && stream.alternateKind == WatchKind.Hls && !alternate.isNullOrBlank()) {
        return stream.copy(
            url = alternate,
            kind = WatchKind.Hls,
            alternateUrl = stream.url,
            alternateKind = WatchKind.Dash,
        )
    }
    return stream
}
