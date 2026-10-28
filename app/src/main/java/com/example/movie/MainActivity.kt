package com.example.movie

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.detail.DetailRoute
import com.example.movie.presentation.home.MoviePeek
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.AppShell
import com.example.movie.presentation.update.AppUpdateHost
import com.example.movie.ui.theme.MovieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val container = (application as MovieApplication).container
            CompositionLocalProvider(
                LocalAppContainer provides container,
                LocalUserSettings provides container.userSettings,
            ) {
                MovieTheme {
                    Box(Modifier.fillMaxSize()) {
                        val stack = rememberSaveable(
                            saver = listSaver(
                                save = { it.toList() },
                                restore = { it.toMutableStateList() },
                            ),
                        ) { mutableStateListOf<String>() }
                        val openedId = stack.lastOrNull()
                        var tabName by rememberSaveable { mutableStateOf("Home") }
                        var detailPeek by remember { mutableStateOf<Movie?>(null) }
                        LaunchedEffect(openedId) { detailPeek = null }
                        BackHandler(enabled = openedId != null) {
                            if (detailPeek != null) detailPeek = null
                            else stack.removeAt(stack.lastIndex)
                        }
                        if (openedId == null) {
                            AppShell(
                                tabName = tabName,
                                onTabChange = { tabName = it },
                                onMovieClick = { stack.add(it) },
                            )
                        } else {
                            DetailRoute(
                                movieId = openedId,
                                onOpenMovie = { id -> if (stack.lastOrNull() != id) stack.add(id) },
                                onMoviePeek = { detailPeek = it },
                            )
                            MoviePeek(movie = detailPeek, onDismiss = { detailPeek = null })
                        }
                        AppUpdateHost()
                    }
                }
            }
        }
    }
}
