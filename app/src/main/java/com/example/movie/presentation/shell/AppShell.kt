package com.example.movie.presentation.shell

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.HomeRoute
import com.example.movie.presentation.settings.SettingsRoute
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.CinemaMuted
import kotlin.math.abs
import kotlin.math.roundToInt

private val BarShape = RoundedCornerShape(32.dp)
private val PillShape = RoundedCornerShape(26.dp)
private val SelectedWeight = 2.2f
private val IdleWeight = 1f
private val DragPill = 40.dp
private val Move = spring<Float>(
    dampingRatio = 1f,
    stiffness = 380f,
)

private enum class AppTab(@StringRes val label: Int, val color: Color) {
    Home(R.string.nav_home, Color(0xFF3B6EF5)),
    Catalog(R.string.nav_catalog, Color(0xFF14A36A)),
    Favorites(R.string.nav_favorites, Color(0xFFE23B5A)),
    Settings(R.string.nav_settings, Color(0xFFD4920A)),
    ;

    fun icon(selected: Boolean): ImageVector = when (this) {
        Home -> if (selected) ShellIcons.homeFilled else ShellIcons.home
        Catalog -> if (selected) ShellIcons.catalogFilled else ShellIcons.catalog
        Favorites -> if (selected) ShellIcons.favoriteFilled else ShellIcons.favorite
        Settings -> ShellIcons.settings
    }
}

@Composable
fun AppShell(onMovieClick: (String) -> Unit) {
    var tabName by rememberSaveable { mutableStateOf(AppTab.Home.name) }
    val tab = AppTab.valueOf(tabName)
    BackHandler(enabled = tab != AppTab.Home) {
        tabName = AppTab.Home.name
    }
    val hazeState = rememberHazeState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaBlack),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
        ) {
            when (tab) {
                AppTab.Home -> HomeRoute(onMovieClick = onMovieClick)
                AppTab.Catalog -> SectionScreen(title = stringResource(R.string.nav_catalog))
                AppTab.Favorites -> SectionScreen(title = stringResource(R.string.nav_favorites))
                AppTab.Settings -> SettingsRoute()
            }
        }
        AppBottomBar(
            selected = tab,
            onSelect = { tabName = it.name },
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SectionScreen(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
            .padding(top = 28.dp, bottom = 96.dp),
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

private val BarGlass = Color(0xFF1C1C1E)

@Composable
private fun AppBottomBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    val tabs = AppTab.entries
    val index = tabs.indexOf(selected)
    val currentIndex by rememberUpdatedState(index)
    val select by rememberUpdatedState(onSelect)
    val tickNow by rememberUpdatedState(tick)
    var dragging by remember { mutableStateOf(false) }
    var fingerLeft by remember { mutableStateOf<Float?>(null) }
    var fingerX by remember { mutableStateOf<Float?>(null) }
    var hoverIndex by remember { mutableStateOf(-1) }
    val pillLeft = remember { Animatable(0f) }
    val placed = remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .height(58.dp)
            .clip(BarShape)
            .hazeEffect(
                state = hazeState,
                style = HazeDefaults.style(
                    backgroundColor = BarGlass,
                    tint = HazeTint(BarGlass.copy(alpha = 0.78f)),
                    blurRadius = 20.dp,
                    noiseFactor = 0f,
                ),
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), BarShape),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .pointerInput(Unit) {
                    val track = size.width.toFloat()
                    val totalWeight = SelectedWeight + IdleWeight * (tabs.size - 1)
                    val pillPx = track * (SelectedWeight / totalWeight)
                    val idlePx = track * (IdleWeight / totalWeight)
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val visualLeft = fingerLeft ?: pillLeft.value
                        val onPill = down.position.x in visualLeft..(visualLeft + pillPx)
                        val grab = down.position.x - visualLeft
                        var moved = false
                        var detent = currentIndex
                        try { while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (moved) {
                                    val left = fingerLeft ?: visualLeft
                                    val nearest = (left / idlePx)
                                        .roundToInt()
                                        .coerceIn(0, tabs.lastIndex)
                                    if (nearest != currentIndex) {
                                        if (nearest != detent) tickNow?.invoke()
                                        select(tabs[nearest])
                                    }
                                    dragging = false
                                } else {
                                    val tapped = slotOf(
                                        x = change.position.x,
                                        selectedIndex = currentIndex,
                                        idle = idlePx,
                                        pill = pillPx,
                                        count = tabs.size,
                                    )
                                    if (tapped != currentIndex) {
                                        tickNow?.invoke()
                                        select(tabs[tapped])
                                    }
                                }
                                break
                            }
                            val travel = change.position.x - down.position.x
                            if (!moved && onPill && abs(travel) > viewConfiguration.touchSlop) {
                                moved = true
                                dragging = true
                            }
                            if (!moved) continue
                            change.consume()
                            val desired = change.position.x - grab
                            val maxLeft = (track - pillPx).coerceAtLeast(0f)
                            fingerLeft = desired.coerceIn(0f, maxLeft)
                            fingerX = change.position.x
                            val nearest = ((fingerLeft ?: visualLeft) / idlePx)
                                .roundToInt()
                                .coerceIn(0, tabs.lastIndex)
                            if (nearest != detent) {
                                detent = nearest
                                hoverIndex = nearest
                                tickNow?.invoke()
                            }
                        }
                        } finally {
                            hoverIndex = -1
                            if (moved) dragging = false
                        }
                    }
                },
        ) {
            val total = SelectedWeight + IdleWeight * (tabs.size - 1)
            val pillWidth = maxWidth * (SelectedWeight / total)
            val idleWidth = maxWidth * (IdleWeight / total)
            val density = LocalDensity.current
            val idlePx = with(density) { idleWidth.toPx() }
            val fullPx = with(density) { pillWidth.toPx() }
            val trackPx = with(density) { maxWidth.toPx() }
            val widthFraction = remember { Animatable(1f) }
            val arrived = hoverIndex in tabs.indices
            LaunchedEffect(dragging, arrived) {
                val compact = dragging && !arrived
                widthFraction.animateTo(
                    if (compact) 0f else 1f,
                    spring(dampingRatio = 1f, stiffness = 380f),
                )
            }
            val shownWidth = lerp(DragPill.coerceAtMost(pillWidth), pillWidth, widthFraction.value)
            val shownPx = with(density) { shownWidth.toPx() }
            LaunchedEffect(index, maxWidth, dragging) {
                if (dragging) return@LaunchedEffect
                val from = fingerLeft
                val x = fingerX
                if (from != null && x != null && fullPx > 0f) {
                    val grab = (x - from).coerceIn(0f, fullPx)
                    val visual = x - shownPx * (grab / fullPx)
                    val parked = visual - (fullPx - shownPx) / 2f
                    val maxLeft = (trackPx - fullPx).coerceAtLeast(0f)
                    pillLeft.snapTo(parked.coerceIn(0f, maxLeft))
                    fingerLeft = null
                    fingerX = null
                } else {
                    from?.let {
                        pillLeft.snapTo(it)
                        fingerLeft = null
                    }
                }
                val target = index * idlePx
                if (!placed.value) {
                    pillLeft.snapTo(target)
                    placed.value = true
                    return@LaunchedEffect
                }
                pillLeft.animateTo(target, spring(dampingRatio = 1f, stiffness = 380f))
            }
            val litIndex = if (hoverIndex in tabs.indices) hoverIndex else index
            val heldX = fingerX
            val heldLeft = fingerLeft
            val visualLeft = if (heldX != null && heldLeft != null && fullPx > 0f) {
                val grab = (heldX - heldLeft).coerceIn(0f, fullPx)
                heldX - shownPx * (grab / fullPx)
            } else {
                pillLeft.value + (fullPx - shownPx) / 2f
            }.coerceIn(0f, (trackPx - shownPx).coerceAtLeast(0f))
            Box(
                modifier = Modifier
                    .offset { IntOffset(visualLeft.roundToInt(), 0) }
                    .width(shownWidth)
                    .fillMaxHeight()
                    .clip(PillShape)
                    .background(tabs[litIndex].color.copy(alpha = 0.92f)),
            )
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { i, tab ->
                    val lit = i == litIndex
                    val weight by animateFloatAsState(
                        targetValue = if (lit) SelectedWeight else IdleWeight,
                        animationSpec = Move,
                        label = tab.name,
                    )
                    Row(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(PillShape),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TabFace(tab = tab, selected = lit, lit = lit)
                    }
                }
            }
        }
    }
}

private fun slotOf(
    x: Float,
    selectedIndex: Int,
    idle: Float,
    pill: Float,
    count: Int,
): Int {
    var cursor = 0f
    for (i in 0 until count) {
        val slot = if (i == selectedIndex) pill else idle
        if (x < cursor + slot) return i
        cursor += slot
    }
    return count - 1
}

@Composable
private fun TabFace(tab: AppTab, selected: Boolean, lit: Boolean) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
    Icon(
        imageVector = tab.icon(lit),
        contentDescription = if (selected) null else stringResource(tab.label),
        tint = if (lit) Color.White else Color(0xFF8A8A90),
        modifier = Modifier.size(22.dp),
    )
    AnimatedVisibility(
        visible = selected,
        enter = fadeIn(tween(140)) + expandHorizontally(tween(180)),
        exit = fadeOut(tween(80)) + shrinkHorizontally(tween(120)),
    ) {
        Text(
            text = stringResource(tab.label),
            modifier = Modifier.padding(start = 6.dp),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            letterSpacing = (-0.2).sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
    }
}
