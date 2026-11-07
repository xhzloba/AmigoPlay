package com.example.movie.presentation.detail

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.PlayBlue
import com.example.movie.ui.theme.PosterShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.movie.R
import com.example.movie.domain.model.CastMember
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.FranchiseTrailer
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import com.example.movie.presentation.palette.PosterWash
import com.example.movie.presentation.palette.posterWash
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.ResumeItem
import com.example.movie.domain.model.combineMarks
import com.example.movie.domain.model.episodeMarks
import com.example.movie.domain.model.fullyWatched
import com.example.movie.domain.model.progressCaption
import com.example.movie.domain.model.toMovie
import com.example.movie.domain.model.toResume
import com.example.movie.domain.model.toSaved
import com.example.movie.presentation.common.BookmarkButton
import com.example.movie.presentation.common.ImdbScore
import com.example.movie.presentation.common.KpScore
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.MovieLogoShadow
import com.example.movie.presentation.home.components.PosterCaption
import com.example.movie.presentation.home.components.PosterCaptionBlock
import com.example.movie.presentation.home.components.posterTitlesOn
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.SeasonWatchSheet
import com.example.movie.presentation.home.PosterWave
import com.example.movie.presentation.home.posterWave
import com.example.movie.presentation.home.rememberPressScale
import com.example.movie.presentation.shell.ShellIcons
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import java.util.Locale

private val ActionCircle = Color.Black.copy(alpha = 0.16f)

private val WatchedOutline: ImageVector = ImageVector.Builder(
    name = "watchedOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(22f, 12f)
        arcTo(10f, 10f, 0f, true, true, 2f, 12f)
        arcTo(10f, 10f, 0f, true, true, 22f, 12f)
        moveTo(7.6f, 12.2f)
        lineTo(10.5f, 15.1f)
        lineTo(16.4f, 8.8f)
    }
}.build()

@Composable
private fun WatchedButton(seen: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(ActionCircle)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (seen) Icons.Filled.CheckCircle else WatchedOutline,
            contentDescription = stringResource(
                if (seen) R.string.action_seen else R.string.action_unseen,
            ),
            tint = if (seen) Color.White else Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun RowScope.ActionSlot(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 9.sp,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RoundAction(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(ActionCircle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(22.dp),
        )
    }
}

internal val DetailInset = 20.dp

internal fun Modifier.edgeToEdge(inset: Dp): Modifier = layout { measurable, constraints ->
    val extra = inset.roundToPx()
    val width = constraints.maxWidth + extra * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) {
        placeable.placeRelative(-extra, 0)
    }
}

@Composable
fun DetailScreen(
    state: DetailUiState,
    onRetry: () -> Unit,
    onOpenMovie: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    when {
        detail != null -> DetailContent(
            detail = detail,
            onOpenMovie = onOpenMovie,
            onMoviePeek = onMoviePeek,
            modifier = modifier,
        )
        state.phase is DetailPhase.Failed -> DetailMessage(
            message = state.phase.reason.message(),
            onRetry = onRetry,
            modifier = modifier,
        )
        else -> Box(
            modifier = modifier.fillMaxSize().background(Color(0xFF141414)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

private data class WatchRequest(
    val franchiseId: Int,
    val season: Int? = null,
    val episode: Int? = null,
)

@Composable
private fun DetailContent(
    detail: MovieDetail,
    onOpenMovie: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var playingTrailer by remember { mutableStateOf<FranchiseTrailer?>(null) }
    var watching by remember(detail.id) { mutableStateOf<WatchRequest?>(null) }
    var episodesOpen by remember(detail.id) { mutableStateOf(false) }
    var seasonWatch by remember(detail.id) { mutableStateOf(false) }
    var watchRequested by remember(detail.id) { mutableStateOf(false) }
    var franchisePage by remember(detail.id) { mutableStateOf<String?>(null) }
    val pageVeil = remember { Animatable(0f) }
    var pageShown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(franchisePage) {
        if (franchisePage != null) {
            pageShown = franchisePage
            withFrameNanos { }
            pageVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (pageShown != null) {
            pageVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            pageShown = null
        }
    }
    val pageAlpha = if (pageShown != null) pageVeil.value else 0f
    val favorites = LocalAppContainer.current.favorites
    val watched = LocalAppContainer.current.watched
    val resumeStore = LocalAppContainer.current.resume
    val resumeItems by resumeStore.items.collectAsStateWithLifecycle()
    val keptMarks by resumeStore.kept.collectAsStateWithLifecycle()
    val continued = resumeItems.firstOrNull { it.id == detail.id }
    val progressMarks = combineMarks(continued?.episodeMarks().orEmpty(), keptMarks[detail.id].orEmpty())
    val savedIds by favorites.ids.collectAsStateWithLifecycle()
    val watchedIds by watched.ids.collectAsStateWithLifecycle()
    val tick = rememberHapticTick()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val flat = PosterWash(CinemaBlack, CinemaBlack, CinemaBlack, CinemaBlack)
    val hidden = PosterWash(Color(0xFF121212), Color(0xFF121212), Color(0xFF121212), Color(0xFF121212))
    val colored = settings.extractColors || settings.extractBackdrop
    var target by remember(detail.id) { mutableStateOf(if (colored) hidden else flat) }
    var backdropWash by remember(detail.id) { mutableStateOf(hidden) }
    val reveal = remember(detail.id) { Animatable(if (colored) 0f else 1f) }
    val backdrops = detail.backdrops
    val manualBackdrop = !settings.autoBackdrop && backdrops.size > 1
    val pagerState = rememberPagerState { backdrops.size.coerceAtLeast(1) }
    val scope = rememberCoroutineScope()
    var frame by remember(detail.id, backdrops) { mutableIntStateOf(0) }
    var frameReady by remember(detail.id, frame) { mutableStateOf(false) }
    LaunchedEffect(detail.id, frame, backdrops, settings.autoBackdrop) {
        if (!settings.autoBackdrop || backdrops.size < 2) return@LaunchedEffect
        snapshotFlow { frameReady }.first { it }
        val next = backdrops[(frame + 1) % backdrops.size]
        val incoming = async {
            context.imageLoader.execute(
                ImageRequest.Builder(context).data(next).build(),
            ) is SuccessResult
        }
        delay(8_000)
        incoming.await()
        frame = (frame + 1) % backdrops.size
    }
    val shownBackdrop = if (backdrops.isEmpty()) {
        null
    } else {
        backdrops[(if (manualBackdrop) pagerState.currentPage else frame).coerceIn(0, backdrops.lastIndex)]
    }
    val washCache = remember(detail.id) { HashMap<String, PosterWash>() }
    LaunchedEffect(
        detail.id,
        detail.posterUrl,
        shownBackdrop,
        settings.extractColors,
        settings.extractBackdrop,
    ) {
        when {
            settings.extractColors -> {
                reveal.snapTo(0f)
                val extracted = detail.posterUrl?.let { context.extractBackdrop(it) } ?: return@LaunchedEffect
                target = extracted
                reveal.animateTo(1f, tween(durationMillis = 320))
            }
            settings.extractBackdrop -> {
                reveal.snapTo(1f)
                val url = shownBackdrop
                if (url == null) {
                    backdropWash = flat
                    return@LaunchedEffect
                }
                val extracted = washCache[url] ?: context.extractBackdrop(url, 320, 180)?.also { washCache[url] = it }
                if (extracted != null) backdropWash = extracted
            }
            else -> {
                target = flat
                reveal.snapTo(1f)
            }
        }
    }
    val washAlpha = reveal.value
    val paintWash = if (settings.extractBackdrop) animateWash(backdropWash) else target
    var backdropColorReady by remember(detail.id, shownBackdrop) { mutableStateOf(false) }
    var watchGlow by remember(detail.id) { mutableStateOf<Color?>(null) }
    LaunchedEffect(detail.id, settings.watchGlow) {
        if (!settings.watchGlow || watchGlow != null) return@LaunchedEffect
        snapshotFlow { shownBackdrop to backdropColorReady }
            .first { (url, ready) -> url != null && ready }
        val url = shownBackdrop ?: return@LaunchedEffect
        val color = context.extractBackdrop(url, 320, 180)?.playColor() ?: return@LaunchedEffect
        delay(1_800)
        if (watchGlow != null) return@LaunchedEffect
        watchGlow = color
    }
    val scroll = rememberScrollState()
    LaunchedEffect(detail.id) { scroll.scrollTo(0) }
    val density = LocalDensity.current
    val shadeTravel = with(density) { 200.dp.toPx() }
    val shade = (scroll.value / shadeTravel).coerceIn(0f, 1f)
    val backdropAlpha = if (colored) 1f - shade else 1f
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Color(0xFF121212))
                drawPosterWash(paintWash, washAlpha)
                if (shade > 0f && colored) {
                    val ink = lerp(paintWash.topLeft, paintWash.topRight, 0.5f).sunk()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to ink.copy(alpha = 0.94f * shade),
                            0.28f to ink.copy(alpha = 0.62f * shade),
                            0.58f to Color.Transparent,
                        ),
                    )
                }
            },
    ) {
        val heroHeight = maxHeight * 0.72f
        val contentTop = maxHeight * 0.44f
        val textWidth = maxWidth - DetailInset * 2
        if (backdrops.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = 1f - pageAlpha }
                    .fillMaxWidth()
                    .height(heroHeight)
                    .graphicsLayer { alpha = backdropAlpha },
            ) {
                if (manualBackdrop) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        BackdropStill(
                            url = backdrops[page],
                            height = heroHeight,
                            dim = if (colored) 0f else shade,
                            onReady = {
                                if (backdrops[page] == shownBackdrop) backdropColorReady = true
                            },
                        )
                    }
                } else {
                    Crossfade(
                        targetState = backdrops[frame],
                        animationSpec = tween(durationMillis = 900),
                        label = "backdrop",
                    ) { url ->
                        BackdropStill(
                            url = url,
                            height = heroHeight,
                            dim = if (colored) 0f else shade,
                            onReady = {
                                if (url == backdrops.getOrNull(frame)) frameReady = true
                                if (url == shownBackdrop) backdropColorReady = true
                            },
                            onError = {
                                if (url != backdrops.getOrNull(frame)) return@BackdropStill
                                scope.launch {
                                    delay(1_000)
                                    if (url == backdrops.getOrNull(frame)) {
                                        frame = (frame + 1) % backdrops.size
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        if (manualBackdrop) {
            BackdropDots(
                count = backdrops.size,
                index = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 12.dp)
                    .graphicsLayer { alpha = (1f - shade) * (1f - pageAlpha) },
            )
        }
        Column(
            modifier = Modifier
                .graphicsLayer { alpha = 1f - pageAlpha }
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = DetailInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentTop)
                    .pointerInput(manualBackdrop, backdrops.size) {
                        if (!manualBackdrop) return@pointerInput
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, drag ->
                                change.consume()
                                scope.launch { pagerState.scroll { scrollBy(-drag) } }
                            },
                            onDragEnd = {
                                val page = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                    .roundToInt()
                                    .coerceIn(0, backdrops.lastIndex)
                                scope.launch { pagerState.animateScrollToPage(page) }
                            },
                        )
                    },
            )
            LogoOrTitle(detail)
            Spacer(Modifier.height(16.dp))
            MetaRow(detail)
            if (detail.ratingImdb != null || detail.ratingKp != null) {
                Spacer(Modifier.height(16.dp))
                RatingsRow(detail)
            }
            Spacer(Modifier.height(22.dp))
            val watchId = detail.franchise?.id
            LaunchedEffect(watchRequested, watchId) {
                val id = watchId ?: return@LaunchedEffect
                if (!watchRequested) return@LaunchedEffect
                watchRequested = false
                watching = WatchRequest(id)
            }
            val seasonList = detail.franchise?.seasons.orEmpty()
            WatchBar(
                wash = paintWash,
                glow = if (settings.watchGlow) watchGlow else null,
                glowKey = detail.id,
                resume = continued,
                onWatch = {
                    tick?.invoke()
                    if (seasonList.isNotEmpty()) {
                        episodesOpen = true
                    } else {
                        val id = watchId
                        if (id != null) watching = WatchRequest(id) else watchRequested = true
                    }
                },
            )
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                val seen = detail.id in watchedIds
                ActionSlot(stringResource(R.string.action_later)) {
                    BookmarkButton(
                        saved = detail.id in savedIds,
                        circled = true,
                        circle = ActionCircle,
                        onClick = {
                            tick?.invoke()
                            favorites.toggle(detail.toSaved())
                        },
                    )
                }
                ActionSlot(
                    stringResource(if (seen) R.string.action_seen else R.string.action_unseen),
                ) {
                    WatchedButton(
                        seen = seen,
                        onClick = {
                            tick?.invoke()
                            val series = seasonList.isNotEmpty() ||
                                detail.franchise?.series == true ||
                                detail.type == "serial" ||
                                detail.type == "multserial"
                            when {
                                series -> seasonWatch = true
                                seen -> {
                                    resumeStore.clearMarks(detail.id)
                                    watched.toggle(detail.toSaved())
                                }
                                else -> {
                                    resumeStore.forget(detail.id)
                                    watched.toggle(detail.toSaved())
                                }
                            }
                        },
                    )
                }
                ActionSlot(stringResource(R.string.action_share)) {
                    RoundAction(Icons.Filled.Share)
                }
                ActionSlot(stringResource(R.string.menu_list)) {
                    RoundAction(Icons.Filled.List)
                }
                ActionSlot(stringResource(R.string.action_more)) {
                    RoundAction(Icons.Filled.MoreVert)
                }
            }
            if (detail.overview.isNotBlank()) {
                Spacer(Modifier.height(22.dp))
                Overview(text = detail.overview, width = textWidth)
            }
            if (detail.cast.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                OpenTitle(
                    title = stringResource(R.string.section_cast),
                    onOpen = { franchisePage = "cast" },
                )
                Spacer(Modifier.height(12.dp))
                CastRow(detail.cast)
            }
            if (detail.franchise != null) {
                Spacer(Modifier.height(28.dp))
                FranchiseBlock(
                    detail,
                    onPlayTrailer = { playingTrailer = it },
                    onPlayEpisode = { season, episode ->
                        val id = detail.franchise?.id ?: return@FranchiseBlock
                        watching = WatchRequest(id, season, episode)
                    },
                    marks = progressMarks,
                    onOpenAbout = { franchisePage = "about" },
                    onOpenFacts = { franchisePage = "facts" },
                )
            }
            if (detail.similar.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                SectionTitle(stringResource(R.string.section_similar))
                Spacer(Modifier.height(12.dp))
                MovieStrip(detail.similar, onOpenMovie, onMoviePeek)
            }
            if (detail.sequels.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                SectionTitle(stringResource(R.string.section_sequels))
                Spacer(Modifier.height(12.dp))
                MovieStrip(detail.sequels, onOpenMovie, onMoviePeek)
            }
            Spacer(Modifier.height(28.dp))
        }
        val openedPage = pageShown
        if (openedPage != null) {
            val pageModifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = pageAlpha }
            if (openedPage == "facts") {
                FranchiseFactsPage(
                    facts = detail.franchise?.facts.orEmpty(),
                    onBack = { franchisePage = null },
                    modifier = pageModifier,
                )
            } else if (openedPage == "cast") {
                CastPage(
                    cast = detail.cast,
                    onBack = { franchisePage = null },
                    modifier = pageModifier,
                )
            } else {
                FranchiseAboutPage(
                    detail = detail,
                    onBack = { franchisePage = null },
                    modifier = pageModifier,
                )
            }
        }
        if (seasonWatch) {
            SeasonWatchSheet(
                movie = detail.toSaved().toMovie(),
                knownSeasons = detail.franchise?.seasons.orEmpty(),
                franchiseId = detail.franchise?.id,
                onDismiss = { seasonWatch = false },
            )
        }
        if (episodesOpen) {
            val franchise = detail.franchise
            if (franchise != null && franchise.seasons.isNotEmpty()) {
                EpisodeSheet(
                    seasons = franchise.seasons,
                    resume = resumeItems.firstOrNull { it.id == detail.id },
                    marks = progressMarks,
                    onPlay = { season, episode ->
                        episodesOpen = false
                        val id = franchise.id ?: return@EpisodeSheet
                        watching = WatchRequest(id, season, episode)
                    },
                    onDismiss = { episodesOpen = false },
                )
            }
        }
        playingTrailer?.let { trailer ->
            TrailerPlayer(trailer = trailer, onDismiss = { playingTrailer = null })
        }
        val savedResume = remember(detail.id, watching) {
            watching?.let { resumeStore.items.value.firstOrNull { item -> item.id == detail.id } }
        }
        watching?.let { request ->
            val saved = savedResume
            val pinned = request.season != null && request.episode != null
            val season = if (pinned) request.season else saved?.season
            val episode = if (pinned) request.episode else saved?.episode
            val sameSaved = saved != null && saved.season == season && saved.episode == episode
            WatchPlayer(
                url = resumeWatchUrl(request.franchiseId, season, episode),
                title = detail.title,
                logoUrl = detail.logoUrl,
                startPositionMs = if (!pinned || sameSaved) saved?.positionMs ?: 0L else 0L,
                onProgress = { position, duration, playedSeason, playedEpisode ->
                    val item = detail.toResume(request.franchiseId, position, duration, playedSeason, playedEpisode)
                    val seasons = detail.franchise?.seasons.orEmpty()
                    val finished = resumeStore.record(item) { stored -> seasons.fullyWatched(stored.episodeMarks()) }
                    if (finished) watched.add(detail.toSaved())
                },
                onDismiss = { watching = null },
            )
        }
    }
}

@Composable
private fun CastPage(cast: List<CastMember>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val top = with(density) { WindowInsets.safeDrawing.getTop(this).toDp() }
    val bottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }
    val listState = rememberLazyListState()
    HapticOnChange(listState) { listState.firstVisibleItemIndex }
    val headerScrim = with(density) {
        if (listState.firstVisibleItemIndex > 0) 1f
        else (listState.firstVisibleItemScrollOffset / 28.dp.toPx()).coerceIn(0f, 1f)
    }
    StackPage(
        title = stringResource(R.string.section_cast),
        onBack = onBack,
        modifier = modifier,
        scrolling = false,
        headerScrim = headerScrim,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = top + BrandTopInset + 36.dp + 18.dp,
                bottom = bottom + 16.dp,
            ),
        ) {
            item {
                Text(
                    text = stringResource(peopleCountRes(cast.size), cast.size),
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
            }
            items(cast.size, key = { it }) { index ->
                val member = cast[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PersonPhoto(url = member.photoUrl, name = member.name, size = CastAvatar)
                    Spacer(Modifier.width(CastGap))
                    Text(
                        text = member.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = ShellIcons.chevron,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (index != cast.lastIndex) {
                    Box(
                        Modifier
                            .padding(start = CastAvatar + CastGap)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchBar(
    wash: PosterWash,
    glow: Color?,
    glowKey: String,
    resume: ResumeItem?,
    onWatch: () -> Unit,
) {
    val play = wash.playColor()
    val travel = remember(glowKey) { Animatable(0f) }
    LaunchedEffect(glowKey, glow) {
        if (glow == null) {
            travel.snapTo(0f)
            return@LaunchedEffect
        }
        travel.snapTo(0f)
        travel.animateTo(1f, tween(1600, easing = LinearEasing))
    }
    val borderT = travel.value
    val borderReveal = (borderT / 0.72f).coerceIn(0f, 1f)
    val borderAlpha = if (borderT <= 0.72f) 1f else ((1f - borderT) / 0.28f).coerceIn(0f, 1f)
    val context = LocalContext.current
    val logo = remember {
        val options = android.graphics.BitmapFactory.Options().apply { inScaled = false }
        val source = android.graphics.BitmapFactory.decodeResource(
            context.resources,
            R.drawable.amigoplay_logo,
            options,
        )
        source.splitWatchLogo()
    }
    val barHeight = if (resume == null) 72.dp else 84.dp
    Box(
        Modifier
            .fillMaxWidth()
            .height(barHeight)
            .drawWithContent {
                drawContent()
                if (glow != null && borderAlpha > 0.01f && borderReveal > 0f) {
                    drawWatchBorder(glow.copy(alpha = borderAlpha * 0.4f), 2.dp.toPx(), borderReveal)
                    drawWatchBorder(glow.copy(alpha = borderAlpha), 1.1.dp.toPx(), borderReveal)
                }
            },
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.38f),
                    1f to Color.Black.copy(alpha = 0.10f),
                ),
            )
            .padding(start = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val logoHeight = if (resume == null) 26.dp else 22.dp
            Box(
                modifier = Modifier.height(logoHeight),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(WatchLogoAspect, matchHeightConstraintsFirst = true),
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawImage(
                            image = logo.amigo,
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                            filterQuality = FilterQuality.High,
                            colorFilter = ColorFilter.tint(Color.White),
                        )
                    }
                    Canvas(Modifier.matchParentSize()) {
                        drawImage(
                            image = logo.play,
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                            filterQuality = FilterQuality.High,
                            colorFilter = ColorFilter.tint(play),
                        )
                    }
                }
            }
            val ink = logoHeight * WatchLogoAspect * WatchLogoInkLeft
            if (resume == null) {
                Spacer(Modifier.height(2.dp))
                WatchCaption(start = ink)
            } else {
                Spacer(Modifier.height(6.dp))
                ResumeCaption(item = resume, start = ink)
            }
        }
        Button(
            onClick = onWatch,
            modifier = Modifier.height(44.dp),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = if (resume == null) 18.dp else 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
            ),
        ) {
            Text(
                text = stringResource(if (resume == null) R.string.watch_now else R.string.watch_resume),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
    }
}

private fun DrawScope.drawWatchBorder(color: Color, widthPx: Float, reveal: Float) {
    val inset = widthPx / 2f + 0.6.dp.toPx()
    val radius = (18.dp.toPx() - inset).coerceAtLeast(0f)
    val outline = Path().apply {
        addRoundRect(
            RoundRect(
                left = inset,
                top = inset,
                right = size.width - inset,
                bottom = size.height - inset,
                cornerRadius = CornerRadius(radius, radius),
            ),
        )
    }
    val measure = PathMeasure().apply { setPath(outline, false) }
    if (measure.length <= 0f) return
    val segment = Path()
    measure.getSegment(0f, measure.length * reveal, segment, true)
    drawPath(
        path = segment,
        color = color,
        style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private const val WatchLogoAspect = 754f / 186f
private const val WatchLogoInkLeft = 29f / 754f

@Composable
private fun WatchCaption(start: Dp) {
    val text = stringResource(R.string.watch_free)
    val style = TextStyle(
        color = Color.White.copy(alpha = 0.72f),
        fontSize = 11.sp,
        lineHeight = 13.sp,
    )
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val bearing = measurer.measure(text.take(1), style = style, maxLines = 1)
        .getBoundingBox(0)
        .left
    Text(
        text = text,
        modifier = Modifier
            .padding(start = start)
            .offset(x = with(density) { -bearing.toDp() }),
        style = style,
        maxLines = 1,
    )
}

@Composable
private fun ResumeCaption(item: ResumeItem, start: Dp) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = start, end = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.section_continue),
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.progressCaption(),
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
            ) {
                if (item.fraction > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth(item.fraction.coerceIn(0.04f, 1f))
                            .height(3.dp)
                            .background(Color.White),
                    )
                }
            }
        }
    }
}

@Composable
private fun LogoOrTitle(detail: MovieDetail) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    var failed by remember(detail.id, detail.logoUrl) {
        mutableStateOf(detail.logoUrl.isNullOrBlank())
    }
    if (settings.showLogos && !failed) {
        var ready by remember(detail.id, detail.logoUrl) { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .height(84.dp)
                .width(300.dp),
            contentAlignment = Alignment.Center,
        ) {
            MovieLogoShadow(
                model = detail.logoUrl,
                visible = ready,
                modifier = Modifier.fillMaxSize(),
            )
            AsyncImage(
                model = detail.logoUrl,
                contentDescription = stringResource(R.string.logo_content_description, detail.title),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                onSuccess = { ready = true },
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        Text(
            text = detail.title,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaRow(detail: MovieDetail) {
    val genres = detail.genres.joinToString()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val metaStyle = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    val ageLabel = detail.age?.let { stringResource(R.string.age_badge, it) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = with(density) { 12.dp.roundToPx() }
        val agePad = with(density) { 12.dp.roundToPx() }
        var width = 0
        var count = 0
        ageLabel?.let { label ->
            width += measurer.measure(
                label,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            ).size.width + agePad
            count++
        }
        detail.year?.let { year ->
            width += measurer.measure(year.toString(), style = metaStyle, maxLines = 1).size.width
            count++
        }
        detail.runtimeMinutes?.let { minutes ->
            width += measurer.measure(minutes.asRuntime(), style = metaStyle, maxLines = 1).size.width
            count++
        }
        val genreWidth = if (genres.isEmpty()) {
            0
        } else {
            measurer.measure(genres, style = metaStyle, maxLines = 1).size.width
        }
        if (genres.isNotEmpty()) count++
        val gaps = (count - 1).coerceAtLeast(0) * gap
        val maxPx = with(density) { maxWidth.roundToPx() }
        val inline = genres.isEmpty() || width + genreWidth + gaps <= maxPx
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MetaFacts(detail, genres = if (inline) genres else "")
            if (!inline) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = genres,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaFacts(detail: MovieDetail, genres: String) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        detail.age?.let { age ->
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.age_badge, age),
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        detail.year?.let { year ->
            MetaText(year.toString())
        }
        detail.runtimeMinutes?.let { minutes ->
            MetaText(minutes.asRuntime())
        }
        if (genres.isNotEmpty()) {
            MetaText(genres)
        }
    }
}

@Composable
private fun MetaText(value: String) {
    Text(
        text = value,
        color = Color.White,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RatingsRow(detail: MovieDetail) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        detail.ratingImdb?.let { score ->
            ImdbScore(
                score = score,
                color = Color.White,
                iconHeight = 15.dp,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        detail.ratingKp?.let { score ->
            KpScore(
                score = score,
                color = Color.White,
                iconSize = 15.dp,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun Overview(text: String, width: Dp) {
    var expanded by remember(text) { mutableStateOf(false) }
    val measurer = rememberTextMeasurer()
    val widthPx = with(LocalDensity.current) { width.roundToPx() }
    val canExpand = remember(text, widthPx) {
        measurer.measure(
            text = text,
            style = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 4,
            constraints = Constraints(maxWidth = widthPx.coerceAtLeast(0)),
        ).hasVisualOverflow
    }
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 16.sp,
        lineHeight = 23.sp,
        textAlign = TextAlign.Start,
        maxLines = if (expanded) Int.MAX_VALUE else 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = canExpand,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { expanded = !expanded },
            ),
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
    )
}

private val CastAvatar = 68.dp
private val CastGap = 14.dp

@Composable
private fun PersonPhoto(url: String?, name: String, size: Dp) {
    var failed by remember(url) { mutableStateOf(url.isNullOrBlank()) }
    var loaded by remember(url) { mutableStateOf(false) }
    val loading = !failed && !loaded && !url.isNullOrBlank()
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF2C2C2E)),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = photoPulse())),
            )
        }
        if (failed) {
            Icon(
                imageVector = ShellIcons.person,
                contentDescription = name,
                tint = Color(0xFF9A9AA0),
                modifier = Modifier.size(size * 0.5f),
            )
        } else if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                onError = { failed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (loaded) 1f else 0f },
            )
        }
    }
}

@Composable
private fun photoPulse(): Float {
    val transition = rememberInfiniteTransition(label = "cast-photo")
    val pulse by transition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cast-pulse",
    )
    return pulse
}

private fun peopleCountRes(count: Int): Int {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> R.string.cast_people_many
        mod10 == 1 -> R.string.cast_people_one
        mod10 in 2..4 -> R.string.cast_people_few
        else -> R.string.cast_people_many
    }
}

@Composable
private fun CastRow(cast: List<CastMember>) {
    val listState = rememberLazyListState()
    HapticOnChange(listState) { listState.firstVisibleItemIndex }
    LazyRow(
        state = listState,
        modifier = Modifier
            .edgeToEdge(DetailInset)
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DetailInset),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(count = cast.size, key = { it }) { index ->
            val member = cast[index]
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
                PersonPhoto(url = member.photoUrl, name = member.name, size = 92.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = member.name,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MovieStrip(
    movies: List<Movie>,
    onOpenMovie: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    val listState = rememberLazyListState()
    val tick = rememberHapticTick()
    val showTitles = posterTitlesOn()
    HapticOnChange(listState) { listState.firstVisibleItemIndex }
    LazyRow(
        state = listState,
        modifier = Modifier
            .edgeToEdge(DetailInset)
            .fillMaxWidth()
            .height(108.dp * 1.46f + if (showTitles) PosterCaptionBlock else 0.dp),
        contentPadding = PaddingValues(horizontal = DetailInset),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(count = movies.size, key = { it }) { index ->
            val movie = movies[index]
            var holdProgress by remember(movie.id) { mutableFloatStateOf(0f) }
            var pressed by remember(movie.id) { mutableStateOf(false) }
            val pressScale = rememberPressScale(pressed)
            val wave = remember(movie.id) { PosterWave() }
            Column(modifier = Modifier.width(108.dp)) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / 1.46f)
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .clip(PosterShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .posterWave(wave)
                        .holdToPeek(
                            onPeek = { onMoviePeek(movie) },
                            onProgress = { holdProgress = it },
                            onPressed = { pressed = it },
                            onClick = {
                                tick?.invoke()
                                onOpenMovie(movie.id)
                            },
                            wave = wave,
                        )
                        .holdCue(holdProgress),
                )
                if (showTitles) PosterCaption(movie.title, movie.year)
            }
        }
    }
}

@Composable
private fun DetailMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
private fun DataError.message(): String = when (this) {
    is DataError.Network -> stringResource(R.string.popular_network_error)
    is DataError.Http -> stringResource(R.string.popular_http_error, code)
    is DataError.Parsing -> stringResource(R.string.popular_parse_error)
    is DataError.Unknown -> stringResource(R.string.popular_load_error)
}

private fun Color.sunk(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[2] = (hsv[2] * 0.34f).coerceIn(0.04f, 0.2f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

internal fun DrawScope.drawPosterWash(wash: PosterWash, alpha: Float) {
    if (alpha <= 0f) return
    val layer = alpha < 0.99f
    if (layer) {
        drawContext.canvas.saveLayer(
            Rect(0f, 0f, size.width, size.height),
            Paint().apply { this.alpha = alpha },
        )
    }
    drawRect(Color(0xFF1E1E1E))
    val side = max(size.width, size.height)
    val radius = side * 1.41421356f
    fun pool(color: Color, endX: Boolean, endY: Boolean) {
        scale(
            scaleX = size.width / side,
            scaleY = size.height / side,
            pivot = Offset.Zero,
        ) {
            val center = Offset(if (endX) side else 0f, if (endY) side else 0f)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to color,
                        0.95f to Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    }
    pool(wash.bottomLeft, endX = false, endY = true)
    pool(wash.bottomRight, endX = true, endY = true)
    pool(wash.topRight, endX = true, endY = false)
    pool(wash.topLeft, endX = false, endY = false)
    if (layer) drawContext.canvas.restore()
}

private fun Int.asRuntime(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "%dч %dм".format(hours, minutes)
        hours > 0 -> "%dч".format(hours)
        else -> "%dм".format(minutes)
    }
}

@Composable
private fun BackdropStill(
    url: String,
    height: Dp,
    dim: Float,
    onReady: () -> Unit = {},
    onError: () -> Unit = {},
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        onSuccess = { onReady() },
        onError = { onError() },
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        0.58f to Color.Black,
                        0.74f to Color.Black.copy(alpha = 0.4f),
                        0.9f to Color.Transparent,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
                if (dim > 0f) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.72f * dim),
                        blendMode = BlendMode.SrcAtop,
                    )
                }
            },
    )
}

@Composable
private fun BackdropDots(count: Int, index: Int, modifier: Modifier = Modifier) {
    val window = 9
    val start = if (count <= window) 0 else (index - window / 2).coerceIn(0, count - window)
    val shown = minOf(window, count)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(shown) { slot ->
            val page = start + slot
            val selected = page == index
            Box(
                modifier = Modifier
                    .size(if (selected) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (selected) 0.95f else 0.38f)),
            )
        }
    }
}

@Composable
private fun animateWash(target: PosterWash): PosterWash {
    val spec = tween<Color>(durationMillis = 900, easing = FastOutSlowInEasing)
    val topLeft by animateColorAsState(target.topLeft, spec, label = "washTl")
    val topRight by animateColorAsState(target.topRight, spec, label = "washTr")
    val bottomRight by animateColorAsState(target.bottomRight, spec, label = "washBr")
    val bottomLeft by animateColorAsState(target.bottomLeft, spec, label = "washBl")
    return PosterWash(topLeft, topRight, bottomRight, bottomLeft)
}

private suspend fun android.content.Context.extractBackdrop(
    url: String,
    width: Int = 160,
    height: Int = 240,
): PosterWash? {
    val result = imageLoader.execute(
        ImageRequest.Builder(this)
            .data(url)
            .allowHardware(false)
            .size(width, height)
            .build(),
    )
    val source = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: return null
    val bitmap = if (source.config == android.graphics.Bitmap.Config.HARDWARE) {
        source.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return null
    } else {
        source
    }
    val colors = withContext(Dispatchers.Default) { bitmap.posterWash() }
    if (bitmap !== source) bitmap.recycle()
    return colors
}

private fun PosterWash.playColor(): Color {
    val hsv = FloatArray(3)
    var bestSat = 0f
    var best = PlayBlue
    for (color in listOf(topLeft, topRight, bottomLeft, bottomRight)) {
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        if (hsv[1] <= bestSat) continue
        bestSat = hsv[1]
        hsv[1] = hsv[1].coerceIn(0.55f, 1f)
        hsv[2] = hsv[2].coerceIn(0.72f, 0.95f)
        best = Color(android.graphics.Color.HSVToColor(hsv))
    }
    return if (bestSat < 0.12f) PlayBlue else best
}

private data class WatchLogo(val amigo: ImageBitmap, val play: ImageBitmap)

private fun android.graphics.Bitmap.splitWatchLogo(): WatchLogo {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    val amigo = IntArray(pixels.size)
    val play = IntArray(pixels.size)
    for (index in pixels.indices) {
        val color = pixels[index]
        val alpha = color ushr 24
        if (alpha < 8) continue
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val mask = (alpha shl 24) or 0x00FFFFFF
        if (blue > red + 12 && blue >= green) play[index] = mask else amigo[index] = mask
    }
    recycle()
    return WatchLogo(amigo.toMask(width, height), play.toMask(width, height))
}

private fun IntArray.toMask(width: Int, height: Int): ImageBitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    bitmap.setPixels(this, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}
