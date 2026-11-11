package com.example.movie.data.download

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.StreamKey
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.example.movie.data.player.LIFT_USER_AGENT
import com.example.movie.data.player.audioCodeIndex
import com.example.movie.data.player.WatchKind
import com.example.movie.data.player.WatchStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
internal object MediaCache {
    private val lock = Any()
    private var cacheRef: Cache? = null
    private var managerRef: DownloadManager? = null
    private var readRef: CacheDataSource.Factory? = null

    fun install(context: Context) {
        ensure(context)
    }

    fun manager(context: Context): DownloadManager = synchronized(lock) {
        ensure(context)
        managerRef!!
    }

    fun readSource(context: Context): CacheDataSource.Factory = synchronized(lock) {
        ensure(context)
        readRef!!
    }

    fun enqueue(context: Context, request: DownloadRequest, foreground: Boolean) {
        manager(context).addDownload(request)
        runCatching {
            DownloadService.sendAddDownload(context, MovieDownloadService::class.java, request, foreground)
        }.onFailure { error ->
            Log.e(TAG, "service ${error.javaClass.simpleName}")
            if (foreground) {
                runCatching {
                    DownloadService.sendAddDownload(context, MovieDownloadService::class.java, request, false)
                }
            }
        }
    }

    suspend fun request(item: OfflineItem, stream: OfflineStream): DownloadRequest {
        val id = stream.downloadId(item.title.id)
        val height = stream.height.takeIf { it > 0 } ?: item.height
        if (stream.hls) {
            val keys = withContext(Dispatchers.IO) {
                hlsStreamKeys(fetch(stream.url), height, stream.audioIndex)
            }
            if (keys.isEmpty()) error("no rendition")
            return DownloadRequest.Builder(id, Uri.parse(stream.url))
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .setStreamKeys(keys.map { StreamKey(it.group, it.index) })
                .setData(item.title.title.toByteArray())
                .build()
        }
        return runCatching { withTimeout(20_000) { helperRequest(item, stream, id) } }
            .getOrElse { error ->
                Log.e(TAG, "dash ${error.javaClass.simpleName}")
                DownloadRequest.Builder(id, Uri.parse(stream.url))
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .setData(item.title.title.toByteArray())
                    .build()
            }
    }

    private suspend fun helperRequest(item: OfflineItem, stream: OfflineStream, id: String): DownloadRequest {
        val context = holder
        val watch = stream.toWatch()
        return suspendCancellableCoroutine { cont ->
            val helper = DownloadHelper.forMediaItem(
                context,
                watch.toMediaItem(),
                DefaultRenderersFactory(context),
                http(),
            )
            val released = AtomicBoolean(false)
            fun release() {
                if (released.compareAndSet(false, true)) helper.release()
            }
            helper.prepare(object : DownloadHelper.Callback {
                override fun onPrepared(helper: DownloadHelper, tracksInfoAvailable: Boolean) {
                    if (!cont.isActive) {
                        release()
                        return
                    }
                    try {
                        select(helper, stream.height.takeIf { it > 0 } ?: item.height, stream.audioIndex)
                        val request = helper.getDownloadRequest(id, item.title.title.toByteArray())
                        release()
                        cont.resume(request)
                    } catch (error: Exception) {
                        release()
                        cont.resumeWithException(error)
                    }
                }

                override fun onPrepareError(helper: DownloadHelper, error: IOException) {
                    release()
                    if (cont.isActive) cont.resumeWithException(error)
                }
            })
            cont.invokeOnCancellation { release() }
        }
    }

    private lateinit var holder: Context

    private fun build(context: Context): DownloadManager {
        holder = context
        val directory = File(context.filesDir, "offline")
        val database = StandaloneDatabaseProvider(context)
        val cache = SimpleCache(directory, NoOpCacheEvictor(), database)
        cacheRef = cache
        val upstream = http()
        readRef = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return DownloadManager(
            context,
            database,
            cache,
            upstream,
            Executors.newFixedThreadPool(3),
        ).also { manager ->
            manager.maxParallelDownloads = 2
            managerRef = manager
        }
    }

    private fun ensure(context: Context) {
        if (managerRef == null) build(context.applicationContext)
    }

    private fun fetch(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", LIFT_USER_AGENT)
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 20_000
        }
        try {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299 || body.isBlank()) error("manifest ${connection.responseCode}")
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun http() = DefaultHttpDataSource.Factory()
        .setUserAgent(LIFT_USER_AGENT)
        .setAllowCrossProtocolRedirects(true)

    private fun select(helper: DownloadHelper, height: Int, audioIndex: Int) {
        val cap = if (height <= 0) Int.MAX_VALUE else height
        for (period in 0 until helper.periodCount) {
            helper.clearTrackSelections(period)
            helper.addTrackSelection(
                period,
                TrackSelectionParameters.Builder()
                    .setMaxVideoSize(Int.MAX_VALUE, cap)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build(),
            )
            val audio = audioTrack(helper, period, audioIndex) ?: continue
            helper.addTrackSelection(
                period,
                TrackSelectionParameters.Builder()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .setOverrideForType(TrackSelectionOverride(audio.first.mediaTrackGroup, listOf(audio.second)))
                    .build(),
            )
        }
    }

    private fun audioTrack(helper: DownloadHelper, period: Int, audioIndex: Int): Pair<androidx.media3.common.Tracks.Group, Int>? {
        val groups = helper.getTracks(period).groups.filter { group ->
            group.type == C.TRACK_TYPE_AUDIO &&
                !group.mediaTrackGroup.id.orEmpty().contains("failover", ignoreCase = true)
        }
        val supported = groups.flatMap { group ->
            (0 until group.length).mapNotNull { index ->
                if (group.isTrackSupported(index)) group to index else null
            }
        }
        if (supported.isEmpty()) return null
        if (audioIndex >= 0) {
            supported.firstOrNull { (group, index) ->
                audioCodeIndex(group.getTrackFormat(index).label) == audioIndex
            }?.let { return it }
            supported.getOrNull(audioIndex)?.let { return it }
        }
        return supported.first()
    }

    private const val TAG = "MediaCache"
}

@OptIn(UnstableApi::class)
private fun WatchStream.toMediaItem(): MediaItem {
    val mime = if (kind == WatchKind.Hls) MimeTypes.APPLICATION_M3U8 else MimeTypes.APPLICATION_MPD
    return MediaItem.Builder().setUri(url).setMimeType(mime).build()
}

private fun OfflineStream.toWatch() = WatchStream(
    url = url,
    kind = if (hls) WatchKind.Hls else WatchKind.Dash,
    subtitles = emptyList(),
)

internal fun resumePendingDownloads(context: Context) {
    val manager = MediaCache.manager(context)
    val cursor = runCatching {
        manager.downloadIndex.getDownloads(
            androidx.media3.exoplayer.offline.Download.STATE_QUEUED,
            androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING,
            androidx.media3.exoplayer.offline.Download.STATE_RESTARTING,
        )
    }.getOrNull() ?: return
    val pending = cursor.use { it.moveToFirst() }
    if (pending) {
        DownloadService.sendResumeDownloads(context, MovieDownloadService::class.java, false)
    }
}
