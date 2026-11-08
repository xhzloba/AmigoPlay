package com.example.movie

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.example.movie.ui.theme.CinemaBlack
import kotlin.math.roundToInt
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.common.LocalShelfNotices
import com.example.movie.presentation.common.ShelfNoticeHost
import com.example.movie.presentation.common.ShelfNotices
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    var openRequest by mutableStateOf(0)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE)
        val closed = prefs.getBoolean(KEY_CLOSED, false)
        if (closed) prefs.edit().remove(KEY_CLOSED).commit()
        super.onCreate(if (closed) null else savedInstanceState)
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
                    val noticeScope = rememberCoroutineScope()
                    val notices = remember(noticeScope) { ShelfNotices(noticeScope) }
                    CompositionLocalProvider(LocalShelfNotices provides notices) {
                    val session by rememberSaveable { mutableIntStateOf(0) }
                    var splash by rememberSaveable { mutableStateOf(true) }
                    var revealed by remember { mutableStateOf(false) }
                    var stageWidth by remember { mutableIntStateOf(0) }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .onSizeChanged { stageWidth = it.width },
                    ) {
                        if (!splash || revealed) key(session) {
                        val stack = rememberSaveable(
                            saver = listSaver(
                                save = { it.toList() },
                                restore = { it.toMutableStateList() },
                            ),
                        ) { mutableStateListOf<String>() }
                        val openedId = stack.lastOrNull()
                        val detailShift = remember { Animatable(0f) }
                        val coverShift = remember { Animatable(0f) }
                        var covering by remember { mutableStateOf(false) }
                        var presented by remember { mutableStateOf(stack.toList()) }
                        LaunchedEffect(stack.toList()) {
                            val move = tween<Float>(240, easing = FastOutSlowInEasing)
                            val target = stack.toList()
                            if (target.size == presented.size + 1 && target.dropLast(1) == presented) {
                                covering = false
                                coverShift.snapTo(0f)
                                presented = target
                                detailShift.snapTo(1f)
                                withFrameNanos { }
                                withFrameNanos { }
                                detailShift.animateTo(0f, move)
                            } else {
                                while (
                                    presented.size > target.size &&
                                    presented.take(target.size) == target
                                ) {
                                    covering = true
                                    detailShift.snapTo(0f)
                                    coverShift.snapTo(-1f)
                                    withFrameNanos { }
                                    withFrameNanos { }
                                    coverShift.animateTo(0f, move)
                                    presented = presented.dropLast(1)
                                }
                                covering = false
                                coverShift.snapTo(0f)
                                if (presented != target) {
                                    presented = target
                                    detailShift.snapTo(0f)
                                } else if (detailShift.value != 0f) {
                                    detailShift.animateTo(0f, move)
                                }
                            }
                        }
                        var tabName by rememberSaveable { mutableStateOf("Home") }
                        val activity = this@MainActivity
                        LaunchedEffect(splash, activity.openRequest) {
                            if (splash) return@LaunchedEffect
                            val movie = activity.intent?.getStringExtra(EXTRA_MOVIE) ?: return@LaunchedEffect
                            activity.intent.removeExtra(EXTRA_MOVIE)
                            container.releaseWatch.store.markMovie(movie)
                            if (stack.lastOrNull() != movie) stack.add(movie)
                        }
                        var detailPeek by remember { mutableStateOf<Movie?>(null) }
                        SideEffect {
                            notices.jump = {
                                tabName = "Favorites"
                                stack.clear()
                            }
                        }
                        LaunchedEffect(openedId) { detailPeek = null }
                        val shellCovers = covering && presented.size == 1
                        Box(
                            Modifier
                                .fillMaxSize()
                                .zIndex(if (shellCovers) 50f else 0f)
                                .slideBy({ if (shellCovers) coverShift.value else 0f }, stageWidth),
                        ) {
                            AppShell(
                                tabName = tabName,
                                onTabChange = { tabName = it },
                                onMovieClick = { id -> if (stack.lastOrNull() != id) stack.add(id) },
                                handleBack = openedId == null,
                                session = session,
                            )
                        }
                        BackHandler(enabled = openedId != null) {
                            if (detailPeek != null) detailPeek = null
                            else stack.removeAt(stack.lastIndex)
                        }
                        presented.forEachIndexed { index, id ->
                            val top = index == presented.lastIndex
                            val rides = covering && index == presented.lastIndex - 1
                            val fraction = when {
                                rides -> coverShift.value
                                top && !covering -> detailShift.value
                                else -> 0f
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .zIndex(if (rides) presented.size + 5f else index + 1f)
                                    .slideBy({ fraction }, stageWidth)
                                    .background(CinemaBlack)
                                    .clickable(
                                        interactionSource = remember(id) { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {},
                                    ),
                            ) {
                                DetailRoute(
                                    movieId = id,
                                    onOpenMovie = { next -> if (stack.lastOrNull() != next) stack.add(next) },
                                    onMoviePeek = { detailPeek = it },
                                )
                                if (top) {
                                    MoviePeek(
                                        movie = detailPeek,
                                        onDismiss = { detailPeek = null },
                                        onOpen = { next ->
                                            detailPeek = null
                                            if (stack.lastOrNull() != next) stack.add(next)
                                        },
                                    )
                                }
                            }
                        }
                        }
                        if (!splash || revealed) AppUpdateHost()
                        ShelfNoticeHost()
                        if (splash) {
                            SplashScreen(
                                onReveal = { revealed = true },
                                onFinished = { splash = false },
                            )
                        }
                    }
                }
                }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequest++
    }

    override fun onStart() {
        super.onStart()
        AppForeground.shown.set(true)
        refreshReleases()
    }

    override fun onStop() {
        AppForeground.shown.set(false)
        if (!isChangingConfigurations) refreshReleases()
        super.onStop()
    }

    private fun refreshReleases() {
        val app = application as MovieApplication
        app.scope.launch { app.container.releaseWatch.refresh() }
    }

    override fun onResume() {
        super.onResume()
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.navigationBars())
    }

    override fun onDestroy() {
        if (isFinishing && !isChangingConfigurations) {
            getSharedPreferences(SESSION_PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_CLOSED, true)
                .commit()
        }
        super.onDestroy()
    }

    internal companion object {
        const val SESSION_PREFS = "session"
        const val KEY_CLOSED = "closed"
        const val EXTRA_MOVIE = "open_movie"
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

private fun Modifier.slideBy(fraction: () -> Float, widthPx: Int): Modifier =
    offset { IntOffset((fraction() * widthPx).roundToInt(), 0) }
