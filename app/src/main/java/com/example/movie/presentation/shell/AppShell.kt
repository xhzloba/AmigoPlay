package com.example.movie.presentation.shell

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.favorites.FavoritesScreen
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.HomeRoute
import com.example.movie.presentation.home.HomeSection
import com.example.movie.presentation.home.SectionRoute
import com.example.movie.presentation.home.MoviePeek
import com.example.movie.presentation.settings.SettingsRoute
import com.example.movie.presentation.update.LocalAppUpdate
import com.example.movie.presentation.update.UpdateBadge
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.CinemaMuted

private enum class AppTab(@StringRes val label: Int, val color: Color) {
    Home(R.string.nav_home, Color(0xFF3B6EF5)),
    Catalog(R.string.nav_catalog, Color(0xFF14A36A)),
    Favorites(R.string.nav_favorites, Color(0xFFE23B5A)),
    Settings(R.string.nav_settings, Color(0xFFD4920A)),
    ;

    val icon: ImageVector
        get() = when (this) {
            Home -> ShellIcons.home
            Catalog -> ShellIcons.catalog
            Favorites -> ShellIcons.media
            Settings -> ShellIcons.settings
        }
}

@Composable
fun AppShell(
    tabName: String,
    onTabChange: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    handleBack: Boolean = true,
    session: Int = 0,
) {
    val tab = AppTab.valueOf(tabName)
    val keyboard = LocalSoftwareKeyboardController.current
    var peek by remember { mutableStateOf<Movie?>(null) }
    var sectionName by rememberSaveable { mutableStateOf<String?>(null) }
    var searchCover by remember { mutableFloatStateOf(0f) }
    val section = sectionName?.let { runCatching { HomeSection.valueOf(it) }.getOrNull() }
        ?.takeIf { tab == AppTab.Home }
    val sectionVeil = remember { Animatable(0f) }
    var sectionShown by remember { mutableStateOf<HomeSection?>(null) }
    LaunchedEffect(section) {
        if (section != null) {
            sectionShown = section
            sectionVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (sectionShown != null) {
            sectionVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            sectionShown = null
        }
    }
    val sectionAlpha = if (sectionShown != null) sectionVeil.value else 0f
    LaunchedEffect(peek) {
        if (peek != null) keyboard?.hide()
    }
    BackHandler(
        enabled = handleBack && (peek != null || sectionShown != null || tab != AppTab.Home),
    ) {
        when {
            peek != null -> peek = null
            tab == AppTab.Home && sectionShown != null -> sectionName = null
            else -> onTabChange(AppTab.Home.name)
        }
    }
    LaunchedEffect(tab) {
        if (tab != AppTab.Home && tab != AppTab.Favorites) peek = null
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaBlack),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - sectionAlpha },
        ) {
            when (tab) {
                AppTab.Home -> HomeRoute(
                    onMovieClick = onMovieClick,
                    onMoviePeek = { peek = it },
                    onOpenSection = { sectionName = it.name },
                    onSearchCover = { searchCover = it },
                    session = session,
                )
                AppTab.Catalog -> SectionScreen(title = stringResource(R.string.nav_catalog))
                AppTab.Favorites -> FavoritesScreen(
                    onMovieClick = onMovieClick,
                    onMoviePeek = { peek = it },
                )
                AppTab.Settings -> SettingsRoute()
            }
        }
        val shown = sectionShown
        if (shown != null) {
            SectionRoute(
                section = shown,
                onBack = { sectionName = null },
                onMovieClick = onMovieClick,
                onMoviePeek = { peek = it },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = sectionAlpha },
            )
        }
        val barAlpha = (1f - searchCover) * (1f - sectionAlpha)
        if (barAlpha > 0.02f) {
            AppBottomBar(
                selected = tab,
                onSelect = { next -> onTabChange(next.name) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = barAlpha },
            )
        }
        MoviePeek(
            movie = peek,
            onDismiss = { peek = null },
            onOpen = { id ->
                peek = null
                onMovieClick(id)
            },
        )
    }
}

@Composable
private fun SectionScreen(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
            .padding(top = BrandTopInset, bottom = 96.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 22.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.6).sp,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.catalog_empty),
                color = CinemaMuted,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

private val IdleInk = Color(0xFFC7C7CC)

@Composable
private fun AppBottomBar(
    selected: AppTab?,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    val press = ripple(color = Color(0xFF8E8E93))
    val updatePending = LocalAppUpdate.current.pending
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val belowGesture = (nav + 4.dp).coerceAtLeast(22.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CinemaBlack)
            .drawBehind {
                val stroke = 0.6.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(0f, stroke / 2f),
                    end = Offset(size.width, stroke / 2f),
                    strokeWidth = stroke,
                )
            }
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = belowGesture),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTab.entries.forEach { tab ->
            val lit = tab == selected
            val badge = tab == AppTab.Settings && updatePending
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember(tab) { MutableInteractionSource() },
                            indication = press,
                            onClick = {
                                if (tab != selected) {
                                    tick?.invoke()
                                    onSelect(tab)
                                }
                            },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.label),
                        tint = if (lit) tab.color else IdleInk,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(tab.label),
                        modifier = Modifier.padding(top = 5.dp),
                        color = if (lit) tab.color else IdleInk,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (badge) {
                    UpdateBadge(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = 16.dp, y = (-2).dp),
                    )
                }
            }
        }
    }
}
