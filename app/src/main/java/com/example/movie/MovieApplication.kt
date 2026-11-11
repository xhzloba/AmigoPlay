package com.example.movie

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.example.movie.data.download.MediaCache
import com.example.movie.data.download.resumePendingDownloads
import com.example.movie.di.AppContainer
import com.example.movie.presentation.alerts.ReleaseWatchReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MovieApplication : Application() {
    lateinit var container: AppContainer
        private set

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        MediaCache.install(this)
        container = AppContainer(this)
        resumePendingDownloads(this)
        ReleaseWatchReceiver.schedule(this)
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .crossfade(350)
                .build(),
        )
    }
}
