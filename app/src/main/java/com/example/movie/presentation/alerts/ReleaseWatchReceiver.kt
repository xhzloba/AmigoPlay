package com.example.movie.presentation.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.movie.MovieApplication
import com.example.movie.data.alerts.ReleaseWatchStore
import com.example.movie.domain.model.FeedKind
import kotlinx.coroutines.launch

internal class ReleaseWatchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as MovieApplication
        val pending = goAsync()
        app.scope.launch {
            try {
                app.container.releaseWatch.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION = "com.example.movie.RELEASE_WATCH"
        private const val RETRY_MS = 15L * 60L * 1000L

        fun schedule(context: Context) {
            val alarm = context.getSystemService(AlarmManager::class.java) ?: return
            val pending = pending(context)
            val delay = nextDelay(context)
            if (delay == null) {
                alarm.cancel(pending)
                return
            }
            alarm.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay,
                pending,
            )
        }

        private fun pending(context: Context): PendingIntent {
            val intent = Intent(context, ReleaseWatchReceiver::class.java).setAction(ACTION)
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun nextDelay(context: Context): Long? {
            val store = ReleaseWatchStore(context)
            val feed = store.feed.value
            val watchSubs = store.subs.value.isNotEmpty()
            if (!watchSubs && !feed.anyOn) return null
            val interval = (feed.intervalHours.takeIf { it in FeedKind.intervals } ?: 6) * 60L * 60L * 1000L
            val now = System.currentTimeMillis()
            var next = Long.MAX_VALUE
            if (feed.anyOn) {
                for (kind in FeedKind.all) {
                    if (!feed.enabled(kind)) continue
                    val polled = store.shelves.value.firstOrNull { it.kind == kind }?.polledAt ?: 0L
                    val at = if (polled == 0L) now else polled + interval
                    if (at < next) next = at
                }
            }
            if (watchSubs) {
                val polled = feed.seriesCheckedAt
                val at = if (polled == 0L) now else polled + interval
                if (at < next) next = at
            }
            val wait = next - now
            return if (wait <= 0L) RETRY_MS else wait
        }
    }
}
