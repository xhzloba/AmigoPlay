package com.example.movie

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.isNavigationBarContrastEnforced = false
        setContent {
            val container = (application as MovieApplication).container
            CompositionLocalProvider(
                LocalAppContainer provides container,
                LocalUserSettings provides container.userSettings,
            ) {
                MovieTheme {
                    val settings by container.userSettings.settings.collectAsStateWithLifecycle()
                    NavigationGesture(settings.navGesture)
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

    override fun onResume() {
        super.onResume()
        val enabled = (application as MovieApplication).container.userSettings.settings.value.navGesture
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (enabled) {
                show(WindowInsetsCompat.Type.navigationBars())
            } else {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
}

@Composable
private fun NavigationGesture(enabled: Boolean) {
    val view = LocalView.current
    val enabledState by rememberUpdatedState(enabled)
    fun apply() {
        val window = (view.context as android.app.Activity).window
        WindowInsetsControllerCompat(window, view).apply {
            if (enabledState) {
                show(WindowInsetsCompat.Type.navigationBars())
            } else {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
    SideEffect {
        val window = (view.context as android.app.Activity).window
        WindowInsetsControllerCompat(window, view).apply {
            if (enabled) {
                show(WindowInsetsCompat.Type.navigationBars())
            } else {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
    DisposableEffect(view) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) apply()
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }
}
