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
import com.example.movie.domain.model.FeedKind
import com.example.movie.domain.model.SeriesAlert

internal fun SeriesAlert.textRes(): Int = when {
    available -> R.string.alerts_available
    feed -> R.string.alerts_feed
    else -> R.string.alerts_episode
}

internal object ReleaseNotifier {
    private const val CHANNEL = "series_release"

    fun post(context: Context, alert: SeriesAlert) {
        val manager = manager(context) ?: return
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
            .setContentText(context.getString(alert.textRes(), alert.season, alert.episode))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(alert.movieId.hashCode(), note)
    }

    fun postShelf(context: Context, kind: String, count: Int) {
        val manager = manager(context) ?: return
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ALERTS, true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            kind.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val note = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bell)
            .setContentTitle(context.getString(categoryTitle(kind)))
            .setContentText(context.getString(R.string.alerts_shelf_body, count))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(kind.hashCode(), note)
    }

    private fun manager(context: Context): NotificationManager? {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return null
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    context.getString(R.string.alert_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        return manager
    }

    private fun categoryTitle(kind: String): Int = when (kind) {
        FeedKind.SeriesUpdates -> R.string.alerts_cat_series_updates
        FeedKind.MovieUpdates -> R.string.alerts_cat_movie_updates
        FeedKind.SeriesFresh -> R.string.alerts_cat_series_fresh
        FeedKind.MovieFresh -> R.string.alerts_cat_movie_fresh
        else -> R.string.alerts_feed_title
    }
}
