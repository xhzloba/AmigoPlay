package com.example.movie.presentation.detail

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.presentation.cast.CastSheet
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.PlayBlue
import com.example.movie.ui.theme.PosterShape
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
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
import com.example.movie.data.download.OfflinePhase
import com.example.movie.domain.model.CastMember
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.FranchiseTrailer
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import com.example.movie.presentation.palette.PosterWash
import com.example.movie.presentation.palette.plexWash
import com.example.movie.presentation.palette.posterWash
import com.example.movie.data.player.TrailerSources
import com.example.movie.data.player.fetchTrailerSources
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
import com.example.movie.presentation.common.PlexPlay
import com.example.movie.presentation.common.DownloadRing
import com.example.movie.presentation.common.HandleSheet
import com.example.movie.presentation.common.ImdbScore
import com.example.movie.presentation.common.KpScore
import com.example.movie.presentation.common.LocalShelfNotices
import com.example.movie.presentation.common.NoticeShelfFavorites
import com.example.movie.presentation.common.NoticeShelfWatched
import com.example.movie.presentation.common.PosterImage
import com.example.movie.presentation.common.THEME_BLACK
import com.example.movie.presentation.common.THEME_GRAPHITE
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.common.drawGraphite
import com.example.movie.presentation.common.inProgress
import com.example.movie.presentation.common.offlineProgressTitle
import com.example.movie.data.download.OfflineStatus
import com.example.movie.ui.theme.CinemaMuted
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.movie.domain.model.SeriesSub
import com.example.movie.domain.model.releaseSnapshot
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.MovieLogoShadow
import com.example.movie.presentation.home.components.PosterCaption
import com.example.movie.presentation.home.components.PosterCaptionBlock
import com.example.movie.presentation.home.components.PosterGap
import com.example.movie.presentation.home.components.posterSize
import com.example.movie.presentation.home.components.posterTitlesOn
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.SeasonWatchSheet
import com.example.movie.presentation.home.PosterWave
import com.example.movie.presentation.home.posterWave
import com.example.movie.presentation.home.pressToOpen
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.min
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
private fun BellButton(armed: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (armed) Color.White else ActionCircle)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ShellIcons.bell,
            contentDescription = stringResource(R.string.action_alert),
            tint = if (armed) Color.Black else Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun Context.askNotifyPermission() {
    val activity = findActivity() ?: return
    if (
        ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        48,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun String.asActionLines(): String {
    if (contains(' ') || contains('\n') || length < 11) return this
    val cut = (length * 0.55f).toInt().coerceIn(5, length - 4)
    return take(cut) + "\n" + drop(cut)
}

@Composable
private fun RowScope.ActionSlot(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.width(58.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
        Spacer(Modifier.height(6.dp))
        Text(
            text = label.asActionLines(),
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
private fun MoreActionsSheet(
    downloadStatus: OfflineStatus?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tick = rememberHapticTick()
    HandleSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.action_more),
                modifier = Modifier.padding(horizontal = 22.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                onDownload()
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_download),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = when (downloadStatus?.phase) {
                            OfflinePhase.Ready -> stringResource(R.string.action_downloaded)
                            OfflinePhase.Downloading,
                            OfflinePhase.Preparing,
                            OfflinePhase.Paused,
                            -> offlineProgressTitle(downloadStatus)
                            else -> stringResource(R.string.action_download)
                        },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailerPickSheet(
    trailers: List<FranchiseTrailer>,
    onPick: (FranchiseTrailer) -> Unit,
    onDismiss: () -> Unit,
) {
    val tick = rememberHapticTick()
    HandleSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.franchise_trailers),
                modifier = Modifier.padding(horizontal = 22.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
            ) {
                trailers.forEach { trailer ->
                    val caption = trailer.season?.let { stringResource(R.string.franchise_trailer_season, it) }
                        ?: trailer.name.ifBlank { stringResource(R.string.franchise_trailers) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    tick?.invoke()
                                    onPick(trailer)
                                },
                            )
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = ShellIcons.trailers,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = caption,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
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
    onResolveWatch: () -> Unit,
    onOpenMovie: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    when {
        detail != null -> DetailContent(
            detail = detail,
            franchiseReady = state.franchiseReady,
            unavailable = state.unavailable,
            onResolveWatch = onResolveWatch,
            onOpenMovie = onOpenMovie,
            onMoviePeek = onMoviePeek,
            onBack = onBack,
            modifier = modifier,
        )
        state.phase is DetailPhase.Failed -> DetailMessage(
            message = state.phase.reason.message(),
            onRetry = onRetry,
            modifier = modifier,
        )
        else -> Box(
            modifier = modifier.fillMaxSize().appBackground(),
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
    franchiseReady: Boolean,
    unavailable: Boolean,
    onResolveWatch: () -> Unit,
    onOpenMovie: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var watching by remember(detail.id) { mutableStateOf<WatchRequest?>(null) }
    var episodesOpen by remember(detail.id) { mutableStateOf(false) }
    var seasonWatch by remember(detail.id) { mutableStateOf(false) }
    var castSheet by remember(detail.id) { mutableStateOf(false) }
    var downloadSheet by remember(detail.id) { mutableStateOf(false) }
    var moreOpen by remember(detail.id) { mutableStateOf(false) }
    var offlinePlay by remember(detail.id) { mutableStateOf(false) }
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
    val offline = LocalAppContainer.current.offline
    val resumeStore = LocalAppContainer.current.resume
    val resumeItems by resumeStore.items.collectAsStateWithLifecycle()
    val keptMarks by resumeStore.kept.collectAsStateWithLifecycle()
    val downloads by offline.items.collectAsStateWithLifecycle()
    val downloadTasks by offline.tasks.collectAsStateWithLifecycle()
    val downloadPending by offline.pending.collectAsStateWithLifecycle()
    val downloaded = downloads.firstOrNull { it.title.id == detail.id }
    val downloadStatus = downloaded?.let { offline.status(it, downloadTasks, downloadPending) }
    val continued = resumeItems.firstOrNull { it.id == detail.id }
    val progressMarks = combineMarks(continued?.episodeMarks().orEmpty(), keptMarks[detail.id].orEmpty())
    val releaseWatch = LocalAppContainer.current.releaseWatch
    val alertIds by releaseWatch.store.ids.collectAsStateWithLifecycle()
    LaunchedEffect(detail.id, detail.franchise, alertIds) {
        if (detail.id !in alertIds) return@LaunchedEffect
        val sub = releaseWatch.store.subs.value.firstOrNull { it.id == detail.id } ?: return@LaunchedEffect
        val franchise = detail.franchise
        val finished = franchise?.series == true &&
            franchise.serialStatus.equals("offline", ignoreCase = true)
        if (finished && !sub.awaitingPlayer) {
            releaseWatch.store.disarm(detail.id)
            return@LaunchedEffect
        }
        if (sub.airedKey.isNotEmpty()) return@LaunchedEffect
        val snapshot = franchise?.releaseSnapshot() ?: return@LaunchedEffect
        releaseWatch.store.arm(
            sub.copy(airedKey = snapshot.key, nextEpochDay = snapshot.nextEpochDay),
        )
    }
    val savedIds by favorites.ids.collectAsStateWithLifecycle()
    val watchedIds by watched.ids.collectAsStateWithLifecycle()
    val tick = rememberHapticTick()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val flat = PosterWash(CinemaBlack, CinemaBlack, CinemaBlack, CinemaBlack)
    val hidden = PosterWash(Color.Black, Color.Black, Color.Black, Color.Black)
    val colored = settings.extractBackdrop || settings.extractPlex
    var target by remember(detail.id) { mutableStateOf(if (colored) hidden else flat) }
    var backdropWash by remember(detail.id) { mutableStateOf(hidden) }
    val reveal = remember(detail.id) { Animatable(if (colored) 0f else 1f) }
    val backdrops = detail.backdrops
    val manualBackdrop = !settings.autoBackdrop && backdrops.size > 1
    val pagerState = rememberPagerState { backdrops.size.coerceAtLeast(1) }
    val scope = rememberCoroutineScope()
    var frame by remember(detail.id, backdrops) { mutableIntStateOf(0) }
    var frameReady by remember(detail.id, frame) { mutableStateOf(false) }
    var wantTrailer by remember(detail.id) { mutableStateOf(false) }
    var trailerSheet by remember(detail.id) { mutableStateOf(false) }
    var trailerUrl by remember(detail.id) { mutableStateOf<String?>(null) }
    var trailerPlay by remember(detail.id) { mutableIntStateOf(0) }
    var trailerMedia by remember(detail.id) { mutableStateOf<TrailerSources?>(null) }
    var trailerFrame by remember(detail.id) { mutableStateOf(false) }
    var holdTrailer by remember(detail.id) { mutableStateOf(false) }
    val trailerOn = wantTrailer && trailerMedia != null
    LaunchedEffect(trailerOn) {
        if (trailerOn) {
            holdTrailer = true
        } else {
            delay(700)
            trailerFrame = false
            holdTrailer = false
        }
    }
    val trailerStill = remember(detail.id) { Animatable(1f) }
    LaunchedEffect(trailerOn, trailerFrame) {
        trailerStill.animateTo(
            if (trailerOn && trailerFrame) 0f else 1f,
            tween(durationMillis = 700),
        )
    }
    val trailerPage = trailerUrl
    val trailerLoading = wantTrailer && !trailerFrame
    LaunchedEffect(wantTrailer, trailerPage, trailerPlay) {
        if (!wantTrailer) return@LaunchedEffect
        val page = trailerPage
        if (page == null) {
            wantTrailer = false
            return@LaunchedEffect
        }
        val direct = withContext(Dispatchers.IO) {
            try {
                fetchTrailerSources(page)
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) {
                null
            }
        }
        if (!wantTrailer) return@LaunchedEffect
        if (direct == null) wantTrailer = false else trailerMedia = direct
    }
    LaunchedEffect(watching) {
        if (watching != null) wantTrailer = false
    }
    LaunchedEffect(detail.id, frame, backdrops, settings.autoBackdrop, wantTrailer) {
        if (!settings.autoBackdrop || backdrops.size < 2 || wantTrailer) return@LaunchedEffect
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
    val loadedStills = remember(detail.id) { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(
        detail.id,
        shownBackdrop,
        settings.extractBackdrop,
        settings.extractPlex,
        settings.plexBackdrop,
    ) {
        when {
            settings.extractPlex -> {
                reveal.snapTo(1f)
                val poster = detail.posterUrl?.takeIf { it.isNotBlank() }
                val still = shownBackdrop
                if (poster == null && still == null) {
                    backdropWash = flat
                    return@LaunchedEffect
                }
                suspend fun washOf(url: String, width: Int, height: Int): PosterWash? {
                    val key = "plex:$url"
                    return washCache[key]
                        ?: context.extractBackdrop(url, width, height, plex = true)?.also { washCache[key] = it }
                }
                suspend fun fromPoster(): PosterWash? = poster?.let { washOf(it, 320, 480) }
                suspend fun fromStill(): PosterWash? = still?.let { washOf(it, 384, 216) }
                val extracted = when {
                    !settings.plexBackdrop -> fromPoster() ?: fromStill()
                    still == null -> fromPoster()
                    else -> {
                        if (washCache["plex:$still"] == null && loadedStills[still] != true) {
                            val onScreen = snapshotFlow { loadedStills[still] == true }
                            val late = backdropWash == hidden &&
                                withTimeoutOrNull(PosterStandInDelay) { onScreen.first { it } } == null
                            if (late) {
                                val standIn = fromPoster()
                                if (standIn != null && loadedStills[still] != true) backdropWash = standIn
                            }
                            onScreen.first { it }
                        }
                        fromStill()
                    }
                }
                if (extracted != null) backdropWash = extracted
            }
            settings.extractBackdrop -> {
                reveal.snapTo(1f)
                val url = shownBackdrop
                if (url == null) {
                    backdropWash = flat
                    return@LaunchedEffect
                }
                val key = "frame:$url"
                val extracted = washCache[key] ?: context.extractBackdrop(url, 320, 180)?.also { washCache[key] = it }
                if (extracted != null) backdropWash = extracted
            }
            else -> {
                target = flat
                reveal.snapTo(1f)
            }
        }
    }
    val washAlpha = reveal.value
    val paintWash = if (settings.extractBackdrop || settings.extractPlex) animateWash(backdropWash) else target
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
    LaunchedEffect(wantTrailer) {
        if (!wantTrailer) return@LaunchedEffect
        val origin = scroll.value
        snapshotFlow { scroll.value }.first { it != origin }
        trailerStill.snapTo(1f)
        trailerFrame = false
        holdTrailer = false
        wantTrailer = false
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                if (!colored && settings.appTheme == THEME_GRAPHITE) {
                    drawGraphite()
                    return@drawBehind
                }
                if (!colored && settings.appTheme == THEME_BLACK) {
                    drawRect(Color.Black)
                    return@drawBehind
                }
                drawRect(Color(0xFF121212))
                if (settings.extractPlex) {
                    drawPlexWash(paintWash, washAlpha)
                } else {
                    val washLift = size.height * 0.035f
                    scale(
                        scaleX = 1f,
                        scaleY = (size.height + washLift) / size.height,
                        pivot = Offset(size.width / 2f, size.height),
                    ) {
                        drawPosterWash(paintWash, washAlpha)
                    }
                }
                if (shade > 0f && colored && !settings.extractPlex) {
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
        val lift = maxHeight * 0.07f
        val heroHeight = maxHeight * 0.72f
        val contentTop = maxHeight * 0.44f - lift
        val textWidth = maxWidth - DetailInset * 2
        val barTravelPx = with(density) { DetailBarTravel.toPx() }
        // Плашка — когда центр лого/тайтла на уровне Chromecast (не top бокса).
        val castDock = WindowInsets.statusBars.getTop(density).toFloat() +
            with(density) { DetailBarRow.toPx() / 2f }
        var inkBelow by remember(detail.id, detail.logoUrl) { mutableStateOf(0.dp) }
        val useLogo = settings.showLogos && !detail.logoUrl.isNullOrBlank()
        val logoExtentPx = with(density) {
            if (useLogo) {
                (DetailLogoHeight - inkBelow).toPx().coerceAtLeast(0f)
            } else {
                44.sp.toPx()
            }
        }
        val logoTop = with(density) { contentTop.toPx() } - scroll.value
        val logoAnchor = logoTop + logoExtentPx / 2f
        val barIn = ((castDock + barTravelPx - logoAnchor) / barTravelPx).coerceIn(0f, 1f)
        if (backdrops.isNotEmpty() || (wantTrailer && trailerMedia != null)) {
            Box(
                modifier = Modifier
                    .offset(y = -lift)
                    .fillMaxWidth()
                    .height(heroHeight),
            ) {
                val media = trailerMedia
                if (holdTrailer && media != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(contentTop + lift)
                            .clipToBounds(),
                    ) {
                        key(trailerPlay) {
                            BackdropTrailer(
                                url = media.video,
                                audio = media.audio,
                                referer = trailerPage,
                                modifier = Modifier.fillMaxSize(),
                                onReady = { trailerFrame = true },
                                onFailed = { wantTrailer = false },
                            )
                        }
                    }
                }
            }
        }
        if (holdTrailer && trailerFrame) {
            TrailerEdgeFade(
                wash = paintWash,
                alpha = washAlpha,
                plex = settings.extractPlex,
                bottomFraction = with(density) { contentTop.toPx() / maxHeight.toPx() },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (backdrops.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .offset(y = -lift)
                    .fillMaxWidth()
                    .height(heroHeight),
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = (1f - pageAlpha) * backdropAlpha * trailerStill.value }
                        .fillMaxSize(),
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
                                loadedStills[backdrops[page]] = true
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
                                loadedStills[url] = true
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
        }
        if (trailerLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentTop),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp),
                )
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
                    .pointerInput(manualBackdrop, backdrops.size, wantTrailer) {
                        if (!manualBackdrop || wantTrailer) return@pointerInput
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
            val logoLift by animateDpAsState(
                targetValue = inkBelow,
                animationSpec = tween(180),
                label = "logo-gap",
            )
            Layout(
                content = {
                    Box(Modifier.graphicsLayer { alpha = 1f - barIn }) {
                        LogoOrTitle(detail, onInkBelow = { inkBelow = it })
                    }
                    MetaRow(detail)
                },
            ) { measurables, constraints ->
                val logo = measurables[0].measure(constraints)
                val meta = measurables[1].measure(constraints)
                val gap = LogoMetaGap.roundToPx()
                val metaY = (logo.height + gap - logoLift.roundToPx()).coerceAtLeast(gap)
                val logoX = ((constraints.maxWidth - logo.width) / 2).coerceAtLeast(0)
                layout(constraints.maxWidth, metaY + meta.height) {
                    logo.place(logoX, 0)
                    meta.place(0, metaY)
                }
            }
            Spacer(Modifier.height(22.dp))
            val watchId = detail.franchise?.watchId
            val notifyRelease = franchiseReady && unavailable
            val online = !notifyRelease
            val finished = detail.franchise?.series == true &&
                detail.franchise?.serialStatus.equals("offline", ignoreCase = true)
            val notifyArmed = detail.id in alertIds
            LaunchedEffect(watchRequested, watchId, franchiseReady) {
                if (!watchRequested) return@LaunchedEffect
                val id = watchId
                if (id != null) {
                    watchRequested = false
                    watching = WatchRequest(id)
                } else if (franchiseReady) {
                    watchRequested = false
                }
            }
            val seasonList = detail.franchise?.seasons.orEmpty()
            val series = seasonList.isNotEmpty() ||
                detail.franchise?.series == true ||
                detail.type == "serial" ||
                detail.type == "multserial"
            val notices = LocalShelfNotices.current
            val addedFavorite = stringResource(
                if (series) R.string.notice_favorite_series else R.string.notice_favorite_movie,
            )
            val removedFavorite = stringResource(
                if (series) R.string.notice_favorite_series_off else R.string.notice_favorite_movie_off,
            )
            val alertOn = stringResource(R.string.notice_alert_on)
            val alertOff = stringResource(R.string.notice_alert_off)
            val releaseOn = stringResource(R.string.notice_release_on)
            val releaseOff = stringResource(R.string.notice_release_off)
            val addedWatched = stringResource(R.string.notice_watched_movie)
            val removedWatched = stringResource(R.string.notice_watched_movie_off)
            WatchBar(
                glow = if (settings.watchGlow && franchiseReady && online) watchGlow else null,
                glowKey = detail.id,
                resume = if (notifyRelease) null else continued,
                quality = detail.franchise?.quality,
                qualityReady = franchiseReady,
                available = online,
                notify = notifyRelease,
                notifyArmed = notifyArmed,
                onNotify = {
                    tick?.invoke()
                    if (notifyArmed) {
                        releaseWatch.store.disarm(detail.id)
                        notices.show(releaseOff, NoticeShelfFavorites)
                    } else {
                        releaseWatch.store.arm(detail.releaseSub(awaiting = true))
                        context.askNotifyPermission()
                        notices.show(releaseOn, NoticeShelfFavorites)
                    }
                },
                onWatch = {
                    tick?.invoke()
                    if (downloaded != null && downloadStatus?.phase == OfflinePhase.Ready) {
                        offlinePlay = true
                        return@WatchBar
                    }
                    if (!online) return@WatchBar
                    if (seasonList.isNotEmpty()) {
                        episodesOpen = true
                    } else {
                        val id = watchId
                        if (id != null) watching = WatchRequest(id) else onResolveWatch()
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
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
                            val adding = detail.id !in savedIds
                            favorites.toggle(detail.toSaved())
                            if (adding) watched.remove(detail.id)
                            notices.show(
                                if (adding) addedFavorite else removedFavorite,
                                NoticeShelfFavorites,
                            )
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
                            when {
                                series -> seasonWatch = true
                                seen -> {
                                    resumeStore.clearMarks(detail.id)
                                    watched.toggle(detail.toSaved())
                                    notices.show(removedWatched, NoticeShelfWatched)
                                }
                                else -> {
                                    resumeStore.forget(detail.id)
                                    watched.add(detail.toSaved())
                                    favorites.remove(detail.id)
                                    notices.show(addedWatched, NoticeShelfWatched)
                                }
                            }
                        },
                    )
                }
                if (series && !notifyRelease && !finished) {
                    val armed = detail.id in alertIds
                    ActionSlot(stringResource(R.string.action_alert)) {
                        BellButton(armed = armed) {
                            tick?.invoke()
                            if (armed) {
                                releaseWatch.store.disarm(detail.id)
                                notices.show(alertOff, NoticeShelfFavorites)
                            } else {
                                releaseWatch.store.arm(detail.releaseSub(awaiting = false))
                                context.askNotifyPermission()
                                notices.show(alertOn, NoticeShelfFavorites)
                            }
                        }
                    }
                }
                val trailers = detail.franchise?.trailers.orEmpty()
                ActionSlot(stringResource(R.string.franchise_trailers)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ActionCircle)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = trailers.isNotEmpty(),
                                onClick = {
                                    tick?.invoke()
                                    trailerSheet = true
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = ShellIcons.trailers,
                            contentDescription = stringResource(R.string.franchise_trailers),
                            tint = Color.White.copy(alpha = if (trailers.isEmpty()) 0.35f else 0.92f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                ActionSlot(stringResource(R.string.action_more)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ActionCircle)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    tick?.invoke()
                                    if (downloadStatus?.phase?.inProgress() == true) {
                                        downloadSheet = true
                                    } else {
                                        moreOpen = true
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        DownloadRing(status = downloadStatus, size = 44.dp, stroke = 2.dp) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.action_more),
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
            val director = detail.franchise?.directors
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.joinToString(", ")
                .orEmpty()
                .ifBlank { detail.director.trim() }
            if (detail.overview.isNotBlank() || director.isNotBlank()) {
                Spacer(Modifier.height(22.dp))
                if (detail.overview.isNotBlank()) {
                    Overview(text = detail.overview, width = textWidth)
                }
                if (director.isNotBlank()) {
                    if (detail.overview.isNotBlank()) Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.detail_director, director),
                        color = Color(0xFFA1A1A6),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
                    onPlayEpisode = { season, episode ->
                        val id = detail.franchise?.watchId ?: return@FranchiseBlock
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
        DetailBarSlot(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = (1f - barIn) * (1f - pageAlpha) },
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            castSheet = true
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ChromeCastMark()
            }
        }
        DetailLogoBar(
            title = detail.title,
            logoUrl = detail.logoUrl,
            showLogo = settings.showLogos,
            color = lerp(paintWash.topLeft, paintWash.topRight, 0.5f).sunk(),
            progress = barIn * (1f - pageAlpha),
            onBack = onBack,
            onCast = { castSheet = true },
            modifier = Modifier.align(Alignment.TopCenter),
        )
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
        if (trailerSheet) {
            TrailerPickSheet(
                trailers = detail.franchise?.trailers.orEmpty(),
                onPick = { trailer ->
                    trailerSheet = false
                    trailerMedia = null
                    trailerFrame = false
                    trailerUrl = trailer.url
                    trailerPlay += 1
                    wantTrailer = true
                },
                onDismiss = { trailerSheet = false },
            )
        }
        if (castSheet) {
            val saved = resumeItems.firstOrNull { it.id == detail.id }
            val season = watching?.season ?: saved?.season
            val episode = watching?.episode ?: saved?.episode
            val page = detail.franchise?.watchId?.let { resumeWatchUrl(it, season, episode) }
            LaunchedEffect(castSheet) {
                if (page == null) onResolveWatch()
            }
            val franchise = detail.franchise
            CastSheet(
                title = detail.title,
                pageUrl = page,
                resolving = !franchiseReady,
                seasons = if (franchise?.series == true) franchise.seasons else emptyList(),
                resume = saved,
                marks = progressMarks,
                watchId = franchise?.watchId,
                onDismiss = { castSheet = false },
            )
        }
        if (seasonWatch) {
            SeasonWatchSheet(
                movie = detail.toSaved().toMovie(),
                knownSeasons = detail.franchise?.seasons.orEmpty(),
                onDismiss = { seasonWatch = false },
            )
        }
        if (moreOpen) {
            MoreActionsSheet(
                downloadStatus = downloadStatus,
                onDownload = {
                    moreOpen = false
                    downloadSheet = true
                },
                onDismiss = { moreOpen = false },
            )
        } else if (downloadSheet) {
            DownloadSheet(
                title = detail.toSaved(),
                series = detail.franchise?.series == true,
                watchId = detail.franchise?.watchId,
                knownSeasons = detail.franchise?.seasons.orEmpty(),
                existing = downloaded,
                onDismiss = { downloadSheet = false },
            )
        }
        if (offlinePlay && downloaded != null) {
            OfflinePlayer(
                item = downloaded,
                onDismiss = { offlinePlay = false },
            )
        }
        if (episodesOpen) {
            val franchise = detail.franchise
            if (franchise != null && franchise.seasons.isNotEmpty()) {
                EpisodeSheet(
                    seasons = franchise.seasons,
                    posterUrl = detail.posterUrl,
                    resume = resumeItems.firstOrNull { it.id == detail.id },
                    marks = progressMarks,
                    onPlay = { season, episode ->
                        episodesOpen = false
                        val id = franchise.watchId ?: return@EpisodeSheet
                        watching = WatchRequest(id, season, episode)
                    },
                    onDismiss = { episodesOpen = false },
                )
            }
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
                    if (finished) {
                        watched.add(detail.toSaved())
                        favorites.remove(detail.id)
                    }
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
            items(count = cast.size, key = { "${cast[it].id}\u0000${cast[it].name}" }) { index ->
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

private val WatchPillHeight = 40.dp
private val WatchPillShape = RoundedCornerShape(20.dp)

@Composable
private fun WatchBar(
    glow: Color?,
    glowKey: String,
    resume: ResumeItem?,
    quality: String? = null,
    qualityReady: Boolean = false,
    available: Boolean,
    notify: Boolean = false,
    notifyArmed: Boolean = false,
    onNotify: () -> Unit = {},
    onWatch: () -> Unit,
) {
    val travel = remember(glowKey) { Animatable(0f) }
    val appear = remember(glowKey) { Animatable(if (qualityReady) 1f else 0f) }
    LaunchedEffect(qualityReady) {
        appear.animateTo(
            if (qualityReady) 1f else 0f,
            tween(durationMillis = 520, easing = FastOutSlowInEasing),
        )
    }
    LaunchedEffect(glowKey, glow) {
        if (glow == null) {
            travel.snapTo(0f)
            return@LaunchedEffect
        }
        snapshotFlow { appear.value }.first { it >= 0.995f }
        travel.snapTo(0f)
        travel.animateTo(1f, tween(1600, easing = LinearEasing))
    }
    val livePulse = photoPulse()
    var heldPulse by remember(glowKey) { mutableFloatStateOf(livePulse) }
    if (!qualityReady && heldPulse != livePulse) heldPulse = livePulse
    val borderT = travel.value
    val borderReveal = (borderT / 0.72f).coerceIn(0f, 1f)
    val borderAlpha = if (borderT <= 0.72f) 1f else ((1f - borderT) / 0.28f).coerceIn(0f, 1f)
    val shownResume = if (qualityReady && available) resume else null
    val solid = available || (notify && !notifyArmed)
    val ink = if (solid) Color.Black else Color.White
    var pressed by remember(glowKey) { mutableStateOf(false) }
    val pressScale = rememberPressScale(pressed)
    val wave = remember(glowKey) { PosterWave() }
    val label = when {
        notify && notifyArmed -> R.string.watch_notify_on
        notify -> R.string.watch_notify
        !available -> R.string.watch_unavailable
        shownResume == null -> R.string.watch_now
        else -> R.string.section_continue
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(WatchPillHeight)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(WatchPillShape)
            .background(if (solid) Color.White else Color.White.copy(alpha = if (notifyArmed) 0.14f else 0.08f))
            .then(
                if (solid) {
                    Modifier
                } else {
                    Modifier.border(
                        1.dp,
                        Color.White.copy(alpha = if (notifyArmed) 0.72f else 0.28f),
                        WatchPillShape,
                    )
                },
            )
            .posterWave(wave, light = !solid)
            .then(
                if (qualityReady) {
                    Modifier.pressToOpen(
                        onPressed = { pressed = it },
                        onClick = {
                            when {
                                notify -> onNotify()
                                available -> onWatch()
                            }
                        },
                        wave = wave,
                    )
                } else {
                    Modifier
                },
            )
            .drawWithContent {
                drawContent()
                if (qualityReady && glow != null && borderAlpha > 0.01f && borderReveal > 0f) {
                    drawWatchBorder(glow.copy(alpha = borderAlpha * 0.4f), 2.dp.toPx(), borderReveal)
                    drawWatchBorder(glow.copy(alpha = borderAlpha), 1.1.dp.toPx(), borderReveal)
                }
            },
    ) {
        if (qualityReady) {
            val qualityChip = quality?.takeIf { available && it.isNotBlank() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = appear.value },
            ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (available || notify) {
                    Icon(
                        imageVector = if (notify) ShellIcons.bell else PlexPlay,
                        contentDescription = null,
                        tint = ink,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(label),
                        color = ink.copy(alpha = if (!available && !notify) 0.5f else 1f),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    )
                    if (shownResume != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = shownResume.progressCaption(),
                                color = Color.Black.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .width(36.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(Color.Black.copy(alpha = 0.16f)),
                            ) {
                                if (shownResume.fraction > 0f) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(shownResume.fraction.coerceIn(0.04f, 1f))
                                            .height(2.dp)
                                            .background(Color.Black),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (qualityChip != null) {
                Text(
                    text = qualityChip,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                        .graphicsLayer { alpha = appear.value }
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.Black,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                )
            }
            }
        }
        if (appear.value < 1f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = heldPulse * (1f - appear.value))),
            )
            CircularProgressIndicator(
                color = ink,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(18.dp)
                    .graphicsLayer { alpha = 1f - appear.value },
            )
        }
    }
}

private fun DrawScope.drawWatchBorder(color: Color, widthPx: Float, reveal: Float) {
    val inset = widthPx / 2f + 0.6.dp.toPx()
    val radius = (size.height / 2f - inset).coerceAtLeast(0f)
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

private fun MovieDetail.releaseSub(awaiting: Boolean): SeriesSub {
    val series = franchise?.series == true ||
        franchise?.seasons?.isNotEmpty() == true ||
        type == "serial" ||
        type == "multserial"
    val snapshot = if (awaiting) null else franchise?.releaseSnapshot()
    return SeriesSub(
        id = id,
        title = title,
        posterUrl = posterUrl,
        year = year,
        type = type.ifBlank { if (series) "serial" else "movie" },
        airedKey = if (awaiting) "pending" else snapshot?.key.orEmpty(),
        nextEpochDay = snapshot?.nextEpochDay,
        awaitingPlayer = awaiting,
        feedSeen = !awaiting,
    )
}

@Composable
private fun LogoOrTitle(detail: MovieDetail, onInkBelow: (Dp) -> Unit) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    var failed by remember(detail.id, detail.logoUrl) {
        mutableStateOf(detail.logoUrl.isNullOrBlank())
    }
    if (settings.showLogos && !failed) {
        var ready by remember(detail.id, detail.logoUrl) { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .height(DetailLogoHeight)
                .width(DetailLogoWidth),
            contentAlignment = Alignment.Center,
        ) {
            if (!ready) DetailLogoSkeleton()
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
                onSuccess = { state ->
                    ready = true
                    onInkBelow(state.result.inkBelow(density, DetailLogoWidth, DetailLogoHeight))
                },
                onError = {
                    failed = true
                    onInkBelow(0.dp)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (ready) 1f else 0f },
            )
        }
    } else {
        SideEffect { onInkBelow(0.dp) }
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

private val DetailLogoWidth = 300.dp
private val DetailLogoHeight = 84.dp
private val LogoMetaGap = 8.dp
private val DetailBarRow = 62.dp
private val DetailBarTravel = 48.dp

private const val ChromeCastScreen =
    "M39 39C39.3942 39.0007 39.7846 38.9235 40.1488 38.773C40.5131 38.6225 40.8441 38.4015 41.1228 38.1228C41.4015 37.8441 41.6225 37.5131 41.773 37.1488C41.9235 36.7846 42.0007 36.3942 42 36V12C42.0007 11.6058 41.9235 11.2154 41.773 10.8512C41.6225 10.4869 41.4015 10.1559 41.1228 9.87718C40.8441 9.59847 40.5131 9.37752 40.1488 9.22699C39.7846 9.07647 39.3942 8.99933 39 9H9C8.60585 8.99933 8.21544 9.07647 7.85116 9.22699C7.48688 9.37752 7.15589 9.59847 6.87718 9.87718C6.59847 10.1559 6.37752 10.4869 6.22699 10.8512C6.07647 11.2154 5.99933 11.6058 6 12V22.6747C5.02382 22.4332 4.02226 22.2538 3 22.1409V12C2.99976 11.212 3.1548 10.4317 3.45624 9.70362C3.75769 8.97556 4.19963 8.31403 4.75683 7.75683C5.31403 7.19963 5.97556 6.75769 6.70362 6.45624C7.43168 6.1548 8.212 5.99976 9 6H39C39.788 5.99976 40.5683 6.1548 41.2964 6.45624C42.0244 6.75769 42.686 7.19963 43.2432 7.75683C43.8004 8.31403 44.2423 8.97556 44.5438 9.70362C44.8452 10.4317 45.0002 11.212 45 12V36C45.0001 36.788 44.845 37.5682 44.5436 38.2963C44.2421 39.0243 43.8001 39.6858 43.2429 40.243C42.6858 40.8001 42.0243 41.2421 41.2963 41.5436C40.5682 41.845 39.788 42.0001 39 42H22.908C22.8255 41.1079 22.6884 40.2312 22.5 39.373C22.4726 39.2482 22.4441 39.1239 22.4146 39H39Z"

private val ChromeCast: ImageVector = ImageVector.Builder(
    name = "chromeCast",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).apply {
    listOf(
        ChromeCastScreen,
        "M3 38.8V42H6.2C6.2 40.2327 4.76731 38.8 3 38.8Z",
        "M9.4 42H12.6C12.6 36.6981 8.30193 32.4 3 32.4V35.6C6.53462 35.6 9.4 38.4654 9.4 42Z",
        "M15.8 42H19C19 33.1634 11.8366 26 3 26V29.2C10.0692 29.2 15.8 34.9308 15.8 42Z",
    ).forEach { data ->
        addPath(
            pathData = PathParser().parsePathString(data).toNodes(),
            fill = SolidColor(Color.Black),
        )
    }
}.build()

@Composable
private fun DetailBarSlot(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(DetailBarRow),
        content = content,
    )
}

@Composable
private fun ChromeCastMark(modifier: Modifier = Modifier) {
    Icon(
        imageVector = ChromeCast,
        contentDescription = null,
        tint = Color.White,
        modifier = modifier.size(26.dp),
    )
}

@Composable
private fun DetailLogoBar(
    title: String,
    logoUrl: String?,
    showLogo: Boolean,
    color: Color,
    progress: Float,
    onBack: () -> Unit,
    onCast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    val barPress = remember { MutableInteractionSource() }
    var logoFailed by remember(logoUrl) { mutableStateOf(logoUrl.isNullOrBlank()) }
    val useLogo = showLogo && !logoFailed
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = progress }
            .background(Color.Black.copy(alpha = 0.28f))
            .background(color.copy(alpha = 0.52f))
            .then(
                if (progress > 0f) {
                    Modifier.clickable(
                        interactionSource = barPress,
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onCast()
                        },
                    )
                } else {
                    Modifier
                },
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(DetailBarRow),
    ) {
        if (useLogo) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                onError = { logoFailed = true },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 64.dp)
                    .height(28.dp)
                    .fillMaxWidth(),
            )
        } else {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 64.dp)
                    .fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-2).dp)
                .size(44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = progress > 0.45f,
                    onClick = {
                        tick?.invoke()
                        onBack()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShellIcons.back,
                contentDescription = stringResource(R.string.section_back),
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        ChromeCastMark(Modifier.align(Alignment.CenterEnd).padding(end = 27.dp))
    }
}

@Composable
private fun DetailLogoSkeleton() {
    val transition = rememberInfiniteTransition(label = "detail-logo")
    val pulse by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "detail-logo-pulse",
    )
    Box(
        modifier = Modifier
            .width(210.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = pulse)),
    )
}

private fun SuccessResult.inkBelow(density: Density, maxWidth: Dp, maxHeight: Dp): Dp {
    val raw = (drawable as? BitmapDrawable)?.bitmap ?: return 0.dp
    val bitmap = if (raw.config == android.graphics.Bitmap.Config.HARDWARE) {
        raw.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return 0.dp
    } else {
        raw
    }
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return 0.dp
    val row = IntArray(width)
    var bottom = height - 1
    while (bottom > 0) {
        bitmap.getPixels(row, 0, width, 0, bottom, width, 1)
        if (row.any { it ushr 24 > 24 }) break
        bottom--
    }
    val maxW = with(density) { maxWidth.toPx() }
    val maxH = with(density) { maxHeight.toPx() }
    val scale = min(maxW / width, maxH / height)
    val scaledHeight = height * scale
    val below = (maxH - scaledHeight) / 2f + (height - 1 - bottom) * scale
    return with(density) { below.toDp() }.coerceAtLeast(0.dp)
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
        val agePad = with(density) { 18.dp.roundToPx() }
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
            if (detail.ratingImdb != null || detail.ratingKp != null) {
                Spacer(Modifier.height(8.dp))
                RatingLine(detail)
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
                    .height(22.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.age_badge, age),
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
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
private fun RatingLine(detail: MovieDetail) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        detail.ratingImdb?.let { score ->
            ImdbScore(
                score = score,
                color = Color.White,
                iconHeight = 12.dp,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        detail.ratingKp?.let { score ->
            KpScore(
                score = score,
                color = Color.White,
                iconSize = 12.dp,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        val imdb = detail.ratingImdb
        val kp = detail.ratingKp
        if (imdb != null && kp != null) {
            ScoreBlend(((imdb + kp) * 5f).roundToInt().coerceIn(0, 100))
        }
    }
}

@Composable
private fun ScoreBlend(percent: Int) {
    val high = percent >= 60
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(if (high) R.drawable.ic_rating_high else R.drawable.ic_rating_low),
            contentDescription = null,
            modifier = Modifier
                .height(14.dp)
                .aspectRatio(if (high) 42f / 48f else 52f / 48f),
        )
        Spacer(Modifier.width(4.dp))
        MetaText("$percent%")
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
private fun Overview(text: String, width: Dp) {
    var expanded by remember(text) { mutableStateOf(false) }
    val measurer = rememberTextMeasurer()
    val widthPx = with(LocalDensity.current) { width.roundToPx() }
    val canExpand = remember(text, widthPx) {
        measurer.measure(
            text = text,
            style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
            constraints = Constraints(maxWidth = widthPx.coerceAtLeast(0)),
        ).hasVisualOverflow
    }
    Text(
        text = text,
        color = Color.White,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Start,
        maxLines = if (expanded) Int.MAX_VALUE else 3,
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
    val context = LocalContext.current
    var failed by remember(url) { mutableStateOf(url.isNullOrBlank()) }
    val request = remember(url) {
        url?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(false)
                .memoryCacheKey(it)
                .diskCacheKey(it)
                .build()
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF2C2C2E)),
        contentAlignment = Alignment.Center,
    ) {
        if (request != null && !failed) {
            AsyncImage(
                model = request,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (failed) {
            Icon(
                imageVector = ShellIcons.person,
                contentDescription = name,
                tint = Color(0xFF9A9AA0),
                modifier = Modifier.size(size * 0.5f),
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
        items(cast, key = { it.id to it.name }) { member ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(112.dp)) {
                PersonPhoto(url = member.photoUrl, name = member.name, size = 112.dp)
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
    val posterWidth = posterSize(108.dp)
    HapticOnChange(listState) { listState.firstVisibleItemIndex }
    LazyRow(
        state = listState,
        modifier = Modifier
            .edgeToEdge(DetailInset)
            .fillMaxWidth()
            .height(posterWidth * 1.46f + if (showTitles) PosterCaptionBlock else 0.dp),
        contentPadding = PaddingValues(horizontal = DetailInset),
        horizontalArrangement = Arrangement.spacedBy(PosterGap),
    ) {
        items(count = movies.size, key = { it }) { index ->
            val movie = movies[index]
            var holdProgress by remember(movie.id) { mutableFloatStateOf(0f) }
            var pressed by remember(movie.id) { mutableStateOf(false) }
            val pressScale = rememberPressScale(pressed)
            val wave = remember(movie.id) { PosterWave() }
            Column(modifier = Modifier.width(posterWidth)) {
                PosterImage(
                    url = movie.posterUrl,
                    contentDescription = movie.title,
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
            .appBackground()
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

internal fun DrawScope.drawPlexWash(wash: PosterWash, alpha: Float) {
    if (alpha <= 0f) return
    if (wash.isFlat()) {
        drawRect(wash.topLeft.copy(alpha = alpha))
        return
    }
    val layer = alpha < 0.99f
    if (layer) {
        drawContext.canvas.saveLayer(
            Rect(0f, 0f, size.width, size.height),
            Paint().apply { this.alpha = alpha },
        )
    }
    drawUltrablur(wash)
    if (layer) drawContext.canvas.restore()
}

private fun PosterWash.isFlat(): Boolean =
    topLeft == topRight && topLeft == bottomRight && topLeft == bottomLeft

private fun DrawScope.drawUltrablur(wash: PosterWash) {
    drawRect(Color.Black)
    val side = max(size.width, size.height)
    val reach = side * UltrablurReach
    fun pool(color: Color, endX: Boolean, endY: Boolean) {
        scale(
            scaleX = size.width / side,
            scaleY = size.height / side,
            pivot = Offset.Zero,
        ) {
            val center = Offset(if (endX) side else 0f, if (endY) side else 0f)
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to color,
                        UltrablurCore to color,
                        1f to color.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = reach,
                ),
                size = Size(side, side),
            )
        }
    }
    pool(wash.bottomRight, endX = true, endY = true)
    pool(wash.bottomLeft, endX = false, endY = true)
    pool(wash.topRight, endX = true, endY = false)
    pool(wash.topLeft, endX = false, endY = false)
}

private const val PosterStandInDelay = 1_500L
private const val UltrablurReach = 0.9f
private const val UltrablurCore = 1f / 6f

private fun Int.asRuntime(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "%dч %dм".format(hours, minutes)
        hours > 0 -> "%dч".format(hours)
        else -> "%dм".format(minutes)
    }
}

private const val BackdropFadeStart = 0.52f

@Composable
private fun TrailerEdgeFade(
    wash: PosterWash,
    alpha: Float,
    plex: Boolean,
    bottomFraction: Float,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0.01f) return
    Canvas(modifier) {
        val bottom = size.height * bottomFraction
        val fadeTop = bottom * 0.74f
        drawContext.canvas.saveLayer(Rect(0f, fadeTop, size.width, bottom + 2f), Paint())
        if (plex) drawPlexWash(wash, alpha) else drawPosterWash(wash, alpha)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                fadeTop / size.height to Color.Transparent,
                (fadeTop + (bottom - fadeTop) * 0.55f) / size.height to Color.Black.copy(alpha = 0.45f),
                bottom / size.height to Color.Black,
            ),
            blendMode = BlendMode.DstIn,
        )
        drawContext.canvas.restore()
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
                        BackdropFadeStart to Color.Black,
                        0.68f to Color.Black.copy(alpha = 0.4f),
                        0.84f to Color.Transparent,
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
    plex: Boolean = false,
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
    val colors = withContext(Dispatchers.Default) {
        if (plex) bitmap.plexWash() else bitmap.posterWash()
    }
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
