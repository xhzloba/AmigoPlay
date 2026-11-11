package com.example.movie.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.example.movie.R

@OptIn(UnstableApi::class)
class MovieDownloadService : DownloadService(
    NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_description,
) {
    override fun onCreate() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.download_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                    description = getString(R.string.download_channel_description)
                },
            )
        }
        super.onCreate()
    }

    override fun getDownloadManager(): DownloadManager = MediaCache.manager(this)

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
        val active = downloads.firstOrNull { it.state != Download.STATE_REMOVING }
        val title = active?.request?.data?.let { bytes ->
            runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull()
        }?.takeIf { it.isNotBlank() } ?: getString(R.string.download_channel_name)
        val percent = active?.percentDownloaded?.takeIf { it >= 0f }?.toInt()
        val text = when {
            notMetRequirements != 0 -> getString(R.string.download_waiting)
            percent != null -> getString(R.string.download_progress, percent)
            else -> getString(R.string.download_running)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent ?: 0, percent == null)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "offline"
        const val NOTIFICATION_ID = 41
    }
}
