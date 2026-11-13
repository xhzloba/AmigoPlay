package com.example.movie.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.presentation.common.BookmarkFilled
import com.example.movie.presentation.common.PlexPlay
import com.example.movie.presentation.common.THEME_GRAPHITE
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.common.appChrome
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaMuted

/** Id полок главной (главный слайдер не входит — всегда на экране). */
internal object HomeRails {
    const val Recommendations = "recommendations"
    const val History = "history"
    const val Continue = "continue"
    const val Favorites = "favorites"
    const val Watched = "watched"
    const val Watching = "watching"
    const val Best = "best"

    data class Entry(
        val id: String,
        val titleRes: Int,
        val icon: ImageVector? = null,
    )

    val All: List<Entry> = listOf(
        Entry(Recommendations, R.string.section_recommendations),
        Entry(History, R.string.section_history, ShellIcons.history),
        Entry(Continue, R.string.section_continue, PlexPlay),
        Entry(Favorites, R.string.section_favorites, BookmarkFilled),
        Entry(Watched, R.string.section_watched),
        Entry(Watching, R.string.section_watching),
        Entry(Best, R.string.section_best),
    )
}

private enum class RailsLookTab {
    Look,
    Shelves,
}

private val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val MenuScrim = Color(0xFF3A3A3E).copy(alpha = 0.86f)
private val PlaqueShape = RoundedCornerShape(18.dp)
private val CinemaPlaque = Color.White.copy(alpha = 0.07f)
private val GraphitePlaque = Color(0xFF2A2E38)
private val IconWellCinema = Color.White.copy(alpha = 0.10f)
private val IconWellGraphite = Color.White.copy(alpha = 0.12f)

@Composable
internal fun HomeRailsLookPlaque(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val graphite = settings.appTheme == THEME_GRAPHITE
    val tick = rememberHapticTick()
    var pressed by remember { mutableStateOf(false) }
    val scale = rememberPressScale(pressed)
    val wave = remember { PosterWave() }
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = PosterGridInset)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(PlaqueShape)
            .background(if (graphite) GraphitePlaque else CinemaPlaque)
            .posterWave(wave, light = true)
            .pressToOpen(
                onPressed = { pressed = it },
                onClick = {
                    tick?.invoke()
                    onOpen()
                },
                wave = wave,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (graphite) IconWellGraphite else IconWellCinema),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ShellIcons.settings,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_rails_look_title),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_rails_look_hint),
                    color = CinemaMuted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = ShellIcons.chevron,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeRailsLookSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repository = LocalUserSettings.current
    val settings by repository.settings.collectAsStateWithLifecycle()
    val graphite = settings.appTheme == THEME_GRAPHITE
    var tab by remember { mutableStateOf(RailsLookTab.Look) }
    // Чуть выше половины экрана; длинный список крутится внутри.
    val sheetCap = (LocalConfiguration.current.screenHeightDp * 0.58f).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = appChrome(),
        contentColor = Color.White,
        scrimColor = MenuScrim,
        dragHandle = null,
        shape = SheetShape,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetCap)
                .appBackground()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 14.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
            )
            Text(
                text = stringResource(R.string.home_rails_look_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(horizontal = 22.dp),
            )
            Spacer(Modifier.height(14.dp))
            RailsLookTabs(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PosterGridInset),
            )
            Spacer(Modifier.height(14.dp))
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) {
                            if (forward) it / 10 else -it / 10
                        }
                    val exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) {
                            if (forward) -it / 12 else it / 12
                        }
                    enter togetherWith exit
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = PosterGridInset),
                label = "rails-look-tab",
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (graphite) GraphitePlaque else Color.White.copy(alpha = 0.08f))
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (page) {
                        RailsLookTab.Look -> {
                            RailsToggle(
                                title = stringResource(R.string.settings_hero_colors),
                                hint = stringResource(R.string.settings_hero_colors_hint),
                                checked = settings.heroPosterColors,
                                onChecked = repository::setHeroPosterColors,
                            )
                            if (settings.heroPosterColors) {
                                RailsDivider()
                                RailsToggle(
                                    title = stringResource(R.string.settings_hero_page),
                                    hint = stringResource(R.string.settings_hero_page_hint),
                                    checked = settings.heroPosterPage,
                                    onChecked = repository::setHeroPosterPage,
                                )
                            }
                            RailsDivider()
                            RailsToggle(
                                title = stringResource(R.string.settings_large_posters),
                                hint = stringResource(R.string.settings_large_posters_hint),
                                checked = settings.largePosters,
                                onChecked = repository::setLargePosters,
                            )
                            RailsDivider()
                            RailsToggle(
                                title = stringResource(R.string.settings_slider_shade),
                                hint = stringResource(R.string.settings_slider_shade_hint),
                                checked = settings.sliderShade,
                                onChecked = repository::setSliderShade,
                            )
                            RailsDivider()
                            RailsToggle(
                                title = stringResource(R.string.settings_poster_titles),
                                hint = stringResource(R.string.settings_poster_titles_hint),
                                checked = settings.showPosterTitles,
                                onChecked = repository::setShowPosterTitles,
                            )
                            RailsDivider()
                            RailsToggle(
                                title = stringResource(R.string.settings_white_titles),
                                hint = stringResource(R.string.settings_white_titles_hint),
                                checked = settings.whiteTitles,
                                onChecked = repository::setWhiteTitles,
                            )
                            RailsDivider()
                            RailsToggle(
                                title = stringResource(R.string.settings_watched_badge),
                                hint = stringResource(R.string.settings_watched_badge_hint),
                                checked = settings.watchedBadge,
                                onChecked = repository::setWatchedBadge,
                            )
                        }
                        RailsLookTab.Shelves -> {
                            Text(
                                text = stringResource(R.string.home_rails_shelves_note),
                                color = CinemaMuted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                            RailsDivider()
                            HomeRails.All.forEachIndexed { index, entry ->
                                if (index > 0) RailsDivider()
                                RailsToggle(
                                    title = stringResource(entry.titleRes),
                                    hint = stringResource(R.string.home_rails_shelf_hint),
                                    checked = settings.homeRailVisible(entry.id),
                                    icon = entry.icon,
                                    onChecked = { visible ->
                                        repository.setHomeRailVisible(entry.id, visible)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RailsLookTabs(
    selected: RailsLookTab,
    onSelect: (RailsLookTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val graphite = settings.appTheme == THEME_GRAPHITE
    val pillSpring = spring<Dp>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow,
    )
    BoxWithConstraints(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (graphite) GraphitePlaque else Color.White.copy(alpha = 0.08f))
            .padding(3.dp),
    ) {
        val gap = 2.dp
        val pillW = (maxWidth - gap) / RailsLookTab.entries.size
        val pillX by animateDpAsState(
            targetValue = pillW * selected.ordinal + gap * selected.ordinal,
            animationSpec = pillSpring,
            label = "rails-tab-pill",
        )
        Box(
            Modifier
                .offset(x = pillX)
                .width(pillW)
                .fillMaxHeight()
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White.copy(alpha = 0.14f)),
        )
        Row(Modifier.fillMaxSize()) {
            RailsLookTab.entries.forEach { tab ->
                val on = tab == selected
                val label = when (tab) {
                    RailsLookTab.Look -> stringResource(R.string.home_rails_tab_look)
                    RailsLookTab.Shelves -> stringResource(R.string.home_rails_tab_shelves)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember(tab) { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!on) {
                                    tick?.invoke()
                                    onSelect(tab)
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (on) Color.White else Color(0xFF8A8A90),
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RailsDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.08f)),
    )
}

@Composable
private fun RailsToggle(
    title: String,
    hint: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    icon: ImageVector? = null,
) {
    val tick = rememberHapticTick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onChecked(!checked)
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = hint,
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        RailsSwitch(checked)
    }
}

@Composable
private fun RailsSwitch(checked: Boolean) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val graphite = settings.appTheme == THEME_GRAPHITE
    val travel by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "rails-switch",
    )
    val offTrack = if (graphite) Color(0xFF3A3F4A) else Color(0xFF2C2C30)
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color.White else offTrack)
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = travel)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color(0xFF8A8A90)),
        )
    }
}
