package com.example.movie.presentation.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.movie.MainActivity
import com.example.movie.R
import com.example.movie.domain.model.SeriesAlert

internal object ReleaseNotifier {
    private const val CHANNEL = "series_release"

    fun post(context: Context, alert: SeriesAlert) {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    context.getString(R.string.alert_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_MOVIE, alert.movieId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            alert.movieId.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val note = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bell)
            .setContentTitle(alert.title)
            .setContentText(
                if (alert.feed) {
                    context.getString(R.string.alerts_feed)
                } else {
                    context.getString(R.string.alerts_episode, alert.season, alert.episode)
                },
            )
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(alert.movieId.hashCode(), note)
    }
}
