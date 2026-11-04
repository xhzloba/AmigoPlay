package com.example.movie

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.detail.DetailRoute
import com.example.movie.presentation.home.MoviePeek
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.AppShell
import com.example.movie.presentation.splash.SplashScreen
import com.example.movie.presentation.update.AppUpdateHost
import com.example.movie.presentation.update.LocalAppUpdate
import com.example.movie.presentation.update.rememberUpdateController
import com.example.movie.ui.theme.MovieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 31) {
            splashScreen.setOnExitAnimationListener { view -> view.remove() }
        }
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
                val updates = rememberUpdateController()
                CompositionLocalProvider(LocalAppUpdate provides updates) {
                MovieTheme {
                    NavigationGesture()
                    val lifecycleOwner = LocalLifecycleOwner.current
                    var session by rememberSaveable { mutableIntStateOf(0) }
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            val turning = (lifecycleOwner as? android.app.Activity)?.isChangingConfigurations == true
                            if (event == Lifecycle.Event.ON_STOP && !turning) session++
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                    var splash by remember { mutableStateOf(true) }
                    Box(Modifier.fillMaxSize()) {
                        key(session) {
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
                        AppShell(
                            tabName = tabName,
                            onTabChange = { tabName = it },
                            onMovieClick = { id -> if (stack.lastOrNull() != id) stack.add(id) },
                            handleBack = openedId == null,
                            session = session,
                        )
                        BackHandler(enabled = openedId != null) {
                            if (detailPeek != null) detailPeek = null
                            else stack.removeAt(stack.lastIndex)
                        }
                        if (openedId != null) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {},
                                    ),
                            ) {
                                DetailRoute(
                                    movieId = openedId,
                                    onOpenMovie = { id -> if (stack.lastOrNull() != id) stack.add(id) },
                                    onMoviePeek = { detailPeek = it },
                                )
                                MoviePeek(
                                    movie = detailPeek,
                                    onDismiss = { detailPeek = null },
                                    onOpen = { id ->
                                        detailPeek = null
                                        if (stack.lastOrNull() != id) stack.add(id)
                                    },
                                )
                            }
                        }
                        }
                        AppUpdateHost()
                        if (splash) {
                            SplashScreen(onFinished = { splash = false })
                        }
                    }
                }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.navigationBars())
    }
}

@Composable
private fun NavigationGesture() {
    val view = LocalView.current
    fun apply() {
        val window = (view.context as android.app.Activity).window
        WindowInsetsControllerCompat(window, view).show(WindowInsetsCompat.Type.navigationBars())
    }
    SideEffect { apply() }
    DisposableEffect(view) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) apply()
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }
}
