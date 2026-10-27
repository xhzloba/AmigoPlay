package com.example.movie.presentation.detail

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaBlack
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import com.example.movie.presentation.palette.DominantColors
import com.example.movie.presentation.common.ImdbScore
import com.example.movie.presentation.common.KpScore
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.palette.backdropBrush
import com.example.movie.presentation.palette.dominantColors
import com.example.movie.presentation.palette.forGlow
import com.example.movie.presentation.palette.seat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale

private val ActionIcons = listOf(
    Icons.Filled.FavoriteBorder,
    Icons.Filled.PlayArrow,
    Icons.Filled.CheckCircle,
    Icons.Filled.Share,
    Icons.Filled.List,
    Icons.Filled.MoreVert,
)

private val DetailInset = 20.dp

private fun Modifier.edgeToEdge(inset: Dp): Modifier = layout { measurable, constraints ->
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
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    when {
        detail != null -> DetailContent(
            detail = detail,
            onOpenMovie = onOpenMovie,
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

@Composable
private fun DetailContent(
    detail: MovieDetail,
    onOpenMovie: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val flat = DominantColors(CinemaBlack, CinemaBlack)
    val hidden = DominantColors(Color(0xFF121212), Color(0xFF121212))
    var target by remember(detail.id) { mutableStateOf(if (settings.extractColors) hidden else flat) }
    LaunchedEffect(detail.id, detail.posterUrl, settings.extractColors) {
        if (!settings.extractColors) {
            target = flat
            return@LaunchedEffect
        }
        val extracted = detail.posterUrl?.let { context.extractBackdrop(it) } ?: return@LaunchedEffect
        target = extracted.seat()
    }
    val primary by animateColorAsState(target.primary, tween(durationMillis = 800), label = "washPrimary")
    val secondary by animateColorAsState(target.secondary, tween(durationMillis = 800), label = "washSecondary")
    val colors = DominantColors(primary, secondary)
    val backdrops = detail.backdrops
    var frame by remember(detail.id, backdrops) { mutableIntStateOf(0) }
    LaunchedEffect(detail.id, backdrops) {
        if (backdrops.size < 2) return@LaunchedEffect
        while (isActive) {
            delay(5_000)
            frame = (frame + 1) % backdrops.size
        }
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backdropBrush()),
    ) {
        val heroHeight = maxHeight * 0.82f
        val contentTop = maxHeight * 0.46f
        val textWidth = maxWidth - DetailInset * 2
        if (backdrops.isNotEmpty()) {
            Crossfade(
                targetState = backdrops[frame],
                animationSpec = tween(durationMillis = 900),
                label = "backdrop",
            ) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Black,
                                    0.40f to Color.Black,
                                    0.52f to Color.Black.copy(alpha = 0.55f),
                                    0.62f to Color.Black.copy(alpha = 0.14f),
                                    0.74f to Color.Transparent,
                                    1f to Color.Transparent,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        },
                )
            }
        }
        val scroll = rememberScrollState()
        LaunchedEffect(detail.id) { scroll.scrollTo(0) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val fade = 320.dp.toPx()
                    val alpha = (scroll.value / fade).coerceIn(0f, 0.55f)
                    drawRect(Color.Black.copy(alpha = alpha))
                },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.38f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = DetailInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(contentTop))
            LogoOrTitle(detail)
            Spacer(Modifier.height(16.dp))
            MetaRow(detail)
            if (detail.ratingImdb != null || detail.ratingKp != null) {
                Spacer(Modifier.height(16.dp))
                RatingsRow(detail)
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.watch_now),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionIcons.forEach { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            if (detail.overview.isNotBlank()) {
                Spacer(Modifier.height(22.dp))
                Overview(text = detail.overview, width = textWidth)
            }
            if (detail.cast.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                SectionTitle(stringResource(R.string.section_cast))
                Spacer(Modifier.height(12.dp))
                CastRow(detail.cast)
            }
            if (detail.similar.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                SectionTitle(stringResource(R.string.section_similar))
                Spacer(Modifier.height(12.dp))
                MovieStrip(detail.similar, onOpenMovie)
            }
            if (detail.sequels.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                SectionTitle(stringResource(R.string.section_sequels))
                Spacer(Modifier.height(12.dp))
                MovieStrip(detail.sequels, onOpenMovie)
            }
            Spacer(Modifier.height(28.dp))
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
        AsyncImage(
            model = detail.logoUrl,
            contentDescription = stringResource(R.string.logo_content_description, detail.title),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            onError = { failed = true },
            modifier = Modifier
                .height(84.dp)
                .width(300.dp),
        )
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
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
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
        detail.genres.forEachIndexed { index, genre ->
            val label = if (index < detail.genres.lastIndex) "$genre," else genre
            MetaText(label)
        }
    }
}

@Composable
private fun MetaText(value: String) {
    Text(
        text = value,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
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
                iconHeight = 18.dp,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        detail.ratingKp?.let { score ->
            KpScore(
                score = score,
                color = Color.White,
                iconSize = 18.dp,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
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

@Composable
private fun CastRow(cast: List<CastMember>) {
    val listState = rememberLazyListState()
    HapticOnChange(listState) { listState.firstVisibleItemIndex }
    LazyRow(
        state = listState,
        modifier = Modifier
            .edgeToEdge(DetailInset)
            .fillMaxWidth()
            .height(118.dp),
        contentPadding = PaddingValues(horizontal = DetailInset),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(count = cast.size, key = { it }) { index ->
            val member = cast[index]
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    if (!member.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = member.photoUrl,
                            contentDescription = member.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = member.name,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
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
) {
    val listState = rememberLazyListState()
    val tick = rememberHapticTick()
    HapticOnChange(listState) { listState.firstVisibleItemIndex }
    LazyRow(
        state = listState,
        modifier = Modifier
            .edgeToEdge(DetailInset)
            .fillMaxWidth()
            .height(200.dp),
        contentPadding = PaddingValues(horizontal = DetailInset),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(count = movies.size, key = { it }) { index ->
            val movie = movies[index]
            Column(
                modifier = Modifier
                    .width(108.dp)
                    .clickable(
                        interactionSource = remember(movie.id) { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onOpenMovie(movie.id)
                        },
                    ),
            ) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / 1.46f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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

private fun Int.asRuntime(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "%dч %dм".format(hours, minutes)
        hours > 0 -> "%dч".format(hours)
        else -> "%dм".format(minutes)
    }
}

private suspend fun android.content.Context.extractBackdrop(url: String): DominantColors? {
    val result = imageLoader.execute(
        ImageRequest.Builder(this)
            .data(url)
            .allowHardware(false)
            .size(160, 240)
            .build(),
    )
    val source = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: return null
    val bitmap = if (source.config == android.graphics.Bitmap.Config.HARDWARE) {
        source.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return null
    } else {
        source
    }
    val colors = withContext(Dispatchers.Default) { bitmap.dominantColors { it.forGlow() } }
    if (bitmap !== source) bitmap.recycle()
    return colors
}
