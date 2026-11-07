package com.example.movie

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.example.movie.di.AppContainer

class MovieApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .crossfade(350)
                .build(),
        )
    }
}
