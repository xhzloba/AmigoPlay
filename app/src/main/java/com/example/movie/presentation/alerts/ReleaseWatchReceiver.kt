package com.example.movie.presentation.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.movie.MovieApplication
import kotlinx.coroutines.launch

internal class ReleaseWatchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as MovieApplication
        val pending = goAsync()
        app.scope.launch {
            try {
                app.container.releaseWatch.refresh()
            } finally {
                schedule(app)
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION = "com.example.movie.RELEASE_WATCH"
        private const val INTERVAL = 60 * 60 * 1000L

        fun schedule(context: Context) {
            val alarm = context.getSystemService(AlarmManager::class.java) ?: return
            val intent = Intent(context, ReleaseWatchReceiver::class.java).setAction(ACTION)
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarm.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL,
                pending,
            )
        }
    }
}
