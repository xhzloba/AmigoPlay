package com.example.movie.data.download

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import com.example.movie.data.player.LIFT_USER_AGENT
import com.example.movie.data.player.WatchStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(UnstableApi::class)
internal class OfflineStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val itemsState = MutableStateFlow(read())
    private val tasksState = MutableStateFlow<Map<String, Download>>(emptyMap())
    private val pendingState = MutableStateFlow<Set<String>>(emptySet())
    private val restored = AtomicBoolean(false)

    val items: StateFlow<List<OfflineItem>> = itemsState.asStateFlow()
    val tasks: StateFlow<Map<String, Download>> = tasksState.asStateFlow()
    val pending: StateFlow<Set<String>> = pendingState.asStateFlow()

    init {
        val manager = MediaCache.manager(app)
        manager.addListener(object : DownloadManager.Listener {
            override fun onInitialized(downloadManager: DownloadManager) {
                publish(downloadManager)
                if (restored.compareAndSet(false, true)) scope.launch { restore() }
            }

            override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) =
                publish(downloadManager)

            override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) = publish(downloadManager)
        })
        publish(manager)
    }

    fun status(
        item: OfflineItem,
        tasks: Map<String, Download> = tasksState.value,
        pending: Set<String> = pendingState.value,
    ): OfflineStatus {
        val ids = item.streams.map { it.downloadId(item.title.id) }
        val rows = ids.mapNotNull { tasks[it] }
        if (rows.isEmpty()) {
            val phase = if (ids.any { it in pending }) OfflinePhase.Preparing else OfflinePhase.Failed
            return OfflineStatus(0, phase)
        }
        if (rows.size == item.streams.size && rows.all { it.state == Download.STATE_COMPLETED }) {
            return OfflineStatus(100, OfflinePhase.Ready)
        }
        if (rows.any { it.state == Download.STATE_FAILED } && rows.none { it.state == Download.STATE_DOWNLOADING }) {
            return OfflineStatus(percent(rows), OfflinePhase.Failed)
        }
        return OfflineStatus(percent(rows), OfflinePhase.Downloading)
    }

    fun enqueue(item: OfflineItem) {
        if (item.streams.isEmpty()) return
        val previous = itemsState.value.firstOrNull { it.title.id == item.title.id }
        val merged = item.copy(streams = mergeStreams(item.streams, previous?.streams.orEmpty(), item.title.id))
        val next = listOf(merged) + itemsState.value.filterNot { it.title.id == item.title.id }
        write(next)
        scope.launch {
            var foreground = true
            for (stream in item.streams) {
                val id = stream.downloadId(item.title.id)
                if (itemsState.value.none { it.title.id == item.title.id }) return@launch
                markPending(id, true)
                val request = try {
                    MediaCache.request(merged, stream)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    Log.e(TAG, "request ${error.javaClass.simpleName}")
                    null
                } finally {
                    markPending(id, false)
                }
                if (request == null) continue
                if (itemsState.value.none { it.title.id == item.title.id }) return@launch
                val existing = tasksState.value[id]
                val sameFile = existing?.state == Download.STATE_COMPLETED &&
                    existing.request.uri == request.uri &&
                    existing.request.streamKeys == request.streamKeys
                if (sameFile) continue
                MediaCache.enqueue(app, request, foreground)
                foreground = false
            }
        }
    }

    private fun restore() {
        itemsState.value.forEach { item ->
            val missing = item.streams.filter { tasksState.value[it.downloadId(item.title.id)] == null }
            if (missing.isNotEmpty()) enqueue(item.copy(streams = missing))
        }
    }

    fun removeStream(movieId: String, season: Int, episode: Int) {
        val item = itemsState.value.firstOrNull { it.title.id == movieId } ?: return
        val target = item.streams.firstOrNull { it.season == season && it.episode == episode } ?: return
        val left = item.streams.filterNot { it.season == season && it.episode == episode }
        if (left.isEmpty()) {
            remove(movieId)
            return
        }
        write(itemsState.value.map { if (it.title.id == movieId) it.copy(streams = left) else it })
        DownloadService.sendRemoveDownload(app, MovieDownloadService::class.java, target.downloadId(movieId), false)
    }

    fun remove(movieId: String) {
        val item = itemsState.value.firstOrNull { it.title.id == movieId } ?: return
        write(itemsState.value.filterNot { it.title.id == movieId })
        item.streams.forEach { stream ->
            DownloadService.sendRemoveDownload(app, MovieDownloadService::class.java, stream.downloadId(movieId), false)
        }
    }

    suspend fun heights(stream: WatchStream): List<Int> = withContext(Dispatchers.IO) {
        val body = runCatching {
            client.newCall(
                Request.Builder().url(stream.url).header("User-Agent", LIFT_USER_AGENT).build(),
            ).execute().use { it.body?.string().orEmpty() }
        }.getOrDefault("")
        videoHeights(body)
    }

    private fun percent(rows: List<Download>): Int {
        if (rows.isEmpty()) return 0
        val values = rows.map { download ->
            val value = download.percentDownloaded
            if (value < 0f) 0f else value.coerceAtMost(100f)
        }
        return values.average().toInt().coerceIn(0, 100)
    }

    private fun publish(manager: DownloadManager) {
        val cursor = runCatching { manager.downloadIndex.getDownloads() }.getOrNull() ?: return
        val next = LinkedHashMap<String, Download>()
        cursor.use {
            while (it.moveToNext()) {
                val download = it.download
                next[download.request.id] = download
            }
        }
        tasksState.value = next
        if (pendingState.value.any { it in next.keys }) {
            pendingState.value = pendingState.value - next.keys
        }
    }

    private fun markPending(id: String, active: Boolean) {
        pendingState.value = if (active) pendingState.value + id else pendingState.value - id
    }

    private fun read(): List<OfflineItem> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<OfflineItem>>(raw) }.getOrDefault(emptyList())
    }

    private fun write(next: List<OfflineItem>) {
        prefs.edit().putString(KEY, json.encodeToString(next)).commit()
        itemsState.value = next
    }

    private companion object {
        const val FILE = "offline"
        const val KEY = "items"
        const val TAG = "OfflineStore"
    }
}
