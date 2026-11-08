package com.example.movie.presentation.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.movie.MovieApplication
import com.example.movie.R
import com.example.movie.domain.model.DownloadProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

class UpdateDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (job?.isActive == true) return START_NOT_STICKY
        ensureChannel()
        startForeground(
            NOTE_ID,
            note(getString(R.string.update_downloading), indeterminate = true, percent = 0),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        UpdateDownloadHub.phase = UpdatePhase.Downloading
        UpdateDownloadHub.progress = DownloadProgress()
        val installer = (application as MovieApplication).container.apkInstaller
        job = scope.launch {
            try {
                val file = installer.download(
                    url = url,
                    destination = File(cacheDir, "updates/update.apk"),
                    onProgress = { snapshot ->
                        UpdateDownloadHub.progress = snapshot
                        val known = snapshot.total > 0
                        val percent = if (known) (snapshot.fraction * 100).roundToInt() else 0
                        runCatching {
                            manager().notify(
                                NOTE_ID,
                                note(downloadStatus(snapshot), indeterminate = !known, percent = percent),
                            )
                        }
                    },
                )
                UpdateDownloadHub.phase = UpdatePhase.Ready
                UpdateDownloadHub.progress = DownloadProgress()
                val open = installer.installIntent(this@UpdateDownloadService, file)
                val ready = NotificationCompat.Builder(this@UpdateDownloadService, CHANNEL)
                    .setSmallIcon(R.drawable.ic_stat_download)
                    .setContentTitle(getString(R.string.update_notification_title))
                    .setContentText(getString(R.string.update_notification_ready))
                    .setContentIntent(openPending(open))
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .build()
                manager().notify(NOTE_ID, ready)
                stopForeground(STOP_FOREGROUND_DETACH)
                runCatching { startActivity(open) }
            } catch (cancelled: CancellationException) {
                UpdateDownloadHub.phase = UpdatePhase.Ready
                stopForeground(STOP_FOREGROUND_REMOVE)
                throw cancelled
            } catch (_: Exception) {
                UpdateDownloadHub.phase = UpdatePhase.Failed
                stopForeground(STOP_FOREGROUND_REMOVE)
            } finally {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun manager() = getSystemService(NotificationManager::class.java)

    private fun ensureChannel() {
        val manager = manager()
        val current = manager.getNotificationChannel(CHANNEL)
        if (current != null && current.importance < NotificationManager.IMPORTANCE_DEFAULT) {
            manager.deleteNotificationChannel(CHANNEL)
        }
        val channel = NotificationChannel(
            CHANNEL,
            getString(R.string.update_notification_title),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    private fun note(text: String, indeterminate: Boolean, percent: Int): android.app.Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(getString(R.string.update_notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, percent.coerceIn(0, 100), indeterminate)
            .build()

    private fun openPending(intent: Intent): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val EXTRA_URL = "url"
        private const val CHANNEL = "app_update"
        private const val NOTE_ID = 70
    }
}
