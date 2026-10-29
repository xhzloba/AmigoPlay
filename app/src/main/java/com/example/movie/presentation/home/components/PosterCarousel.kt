package com.example.movie.presentation.home.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.presentation.settings.LocalUserSettings
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.movie.R
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.rememberPressScale
import com.example.movie.presentation.palette.dominantColors
import com.example.movie.presentation.palette.forGlow
import com.example.movie.presentation.palette.forWordmark
import com.example.movie.ui.theme.PosterShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val CoverWidthFraction = 0.5f
private const val CoverAspect = 1.46f
private const val NeighborScale = 0.84f
private const val NeighborShift = 0.5f
private const val DotWindow = 5
private val ReflectionHeight = 104.dp
private val CaptionLift = 48.dp
private val LogoNudge = 20.dp
private val MetaNudge = 12.dp
private val DotsNudge = 16.dp
private val LogoSlot = 52.dp

private data class PosterPalette(val primary: Color, val secondary: Color)

private data class CoverTilt(val side: Float)

@Composable
private fun rememberCoverTilt(): CoverTilt {
    val context = LocalContext.current
    val gyroOn by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    var side by remember { mutableFloatStateOf(0f) }
    DisposableEffect(context, gyroOn.gyro) {
        if (!gyroOn.gyro) {
            side = 0f
            onDispose { }
        } else {
        val manager = context.getSystemService(SensorManager::class.java)
        val gyroscope = manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val rotation = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || gyroscope == null || rotation == null) {
            onDispose { }
        } else {
            val matrix = FloatArray(9)
            var smoothedSide = 0f
            var referenceHeading = Float.NaN
            var lastNanos = 0L
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type != rotation.type) return
                    SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    val now = event.timestamp
                    val dt = if (lastNanos == 0L) {
                        0.016f
                    } else {
                        ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    }
                    lastNanos = now
                    val up = matrix[7]
                    val roll = atan2(-matrix[6], hypot(up, matrix[8]).coerceAtLeast(0.05f))
                    val heading = atan2(matrix[2], matrix[5])
                    if (referenceHeading.isNaN() && up > 0.45f) referenceHeading = heading
                    val yaw = if (referenceHeading.isNaN()) 0f else wrapRadians(heading - referenceHeading)
                    val upright = ((up - 0.15f) / 0.5f).coerceIn(0f, 1f)
                    val lateral = if (roll.absoluteValue >= yaw.absoluteValue) roll else yaw
                    val sideTarget = (lateral / 0.4f).coerceIn(-1f, 1f) * upright
                    val alpha = 1f - exp(-dt / 0.28f)
                    smoothedSide += (sideTarget - smoothedSide) * alpha
                    side = smoothedSide
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_GAME)
            onDispose { manager.unregisterListener(listener) }
        }
        }
    }
    return CoverTilt(side)
}

private fun wrapRadians(angle: Float): Float {
    val turn = (Math.PI * 2).toFloat()
    var value = angle % turn
    if (value > Math.PI) value -= turn
    if (value < -Math.PI) value += turn
    return value
}

@Composable
fun PosterCarousel(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onPlayColor: (Color, Boolean) -> Unit = { _, _ -> },
    onSettled: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val loop = movies.size > 1
    val initialPage = if (loop) {
        val mid = Int.MAX_VALUE / 2
        mid - mid % movies.size
    } else {
        0
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { if (loop) Int.MAX_VALUE else movies.size.coerceAtLeast(1) },
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val posterColors = remember { mutableStateMapOf<String, PosterPalette>() }
    val activeIndex = if (movies.isEmpty()) 0 else pagerState.currentPage % movies.size
    val playOffset = pagerState.currentPageOffsetFraction
    val play = wordmarkPlay(
        movies = movies,
        activeIndex = activeIndex,
        offset = playOffset,
        colors = posterColors,
    )
    SideEffect {
        val color = play ?: return@SideEffect
        onPlayColor(color, playOffset.absoluteValue > 0.02f)
    }
    val active = movies.getOrNull(activeIndex)
    val tilt = rememberCoverTilt()
    val tick = rememberHapticTick()
    HapticOnChange(pagerState) { pagerState.currentPage }
    SettleEffect(pagerState, { pagerState.isScrollInProgress }, onSettled)

    LaunchedEffect(movies, activeIndex) {
        if (movies.isEmpty()) return@LaunchedEffect
        listOf(activeIndex - 1, activeIndex, activeIndex + 1)
            .map { it.floorMod(movies.size) }
            .distinct()
            .forEach { index ->
                val movie = movies[index]
                if (posterColors.containsKey(movie.id)) return@forEach
                launch {
                    val palette = movie.extractPosterPalette(context) ?: return@launch
                    posterColors[movie.id] = palette
                }
            }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val fittedWidth = maxWidth * CoverWidthFraction
            val cardHeight = fittedWidth * CoverAspect
            val sidePadding = ((maxWidth - fittedWidth) / 2).coerceAtLeast(0.dp)
            val glow = posterGlowColor(
                movies = movies,
                activeIndex = activeIndex,
                offset = pagerState.currentPageOffsetFraction,
                colors = posterColors,
            )
            val glowReveal = remember { Animatable(0f) }
            var revealMovies by remember { mutableStateOf(movies) }
            val glowStrength = if (revealMovies != movies) 0f else glowReveal.value
            LaunchedEffect(movies) {
                glowReveal.snapTo(0f)
                revealMovies = movies
                if (movies.isEmpty()) return@LaunchedEffect
                snapshotFlow {
                    val index = pagerState.currentPage % movies.size
                    posterColors[movies[index].id]?.primary?.alpha ?: 0f
                }.first { it > 0.04f }
                glowReveal.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .drawBehind {
                            fun feather(center: Offset, radius: Float, color: Color, mid: Float, coreAlpha: Float, midAlpha: Float) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colorStops = arrayOf(
                                            0f to color.copy(alpha = coreAlpha),
                                            mid to color.copy(alpha = midAlpha),
                                            1f to Color.Transparent,
                                        ),
                                        center = center,
                                        radius = radius,
                                    ),
                                    radius = radius,
                                    center = center,
                                )
                            }
                            val leftRadius = size.width * 0.52f
                            val rightRadius = size.width * 0.42f
                            feather(
                                center = Offset(size.width * 0.28f, size.height * 0.18f),
                                radius = leftRadius,
                                color = glow.primary,
                                mid = 0.42f,
                                coreAlpha = 0.95f * glowStrength,
                                midAlpha = 0.5f * glowStrength,
                            )
                            feather(
                                center = Offset(size.width * 0.72f, size.height * 0.22f),
                                radius = rightRadius,
                                color = glow.secondary,
                                mid = 0.4f,
                                coreAlpha = 0.8f * glowStrength,
                                midAlpha = 0.38f * glowStrength,
                            )
                            feather(
                                center = Offset(size.width * 0.28f, size.height * 0.82f),
                                radius = leftRadius,
                                color = glow.primary,
                                mid = 0.42f,
                                coreAlpha = 0.95f * glowStrength,
                                midAlpha = 0.5f * glowStrength,
                            )
                            feather(
                                center = Offset(size.width * 0.72f, size.height * 0.78f),
                                radius = rightRadius,
                                color = glow.secondary,
                                mid = 0.4f,
                                coreAlpha = 0.8f * glowStrength,
                                midAlpha = 0.38f * glowStrength,
                            )
                        },
                )
                HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = sidePadding),
                pageSize = PageSize.Fixed(fittedWidth),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                key = { page ->
                    val movie = movies[page % movies.size]
                    "${movie.id}-$page"
                },
            ) { page ->
                val movie = movies[page % movies.size]
                val pageOffset = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                val distance = pageOffset.absoluteValue
                val active = page == pagerState.currentPage && distance < 0.12f
                var posterReady by remember(movie.id) { mutableStateOf(false) }
                var holdProgress by remember { mutableFloatStateOf(0f) }
                var pressed by remember { mutableStateOf(false) }
                val pressScale = rememberPressScale(pressed)
                val shine = remember(page) { Animatable(0f) }
                val travel = shine.value
                val shineTint = posterColors[movie.id]?.primary ?: Color.White
                LaunchedEffect(active, posterReady) {
                    if (!active || !posterReady) {
                        shine.snapTo(0f)
                        return@LaunchedEffect
                    }
                    delay(520)
                    shine.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f - distance)
                        .graphicsLayer {
                            val clamped = distance.coerceIn(0f, 1f)
                            val facing = 1f - clamped
                            val scale = lerp(1f, NeighborScale, clamped) * pressScale
                            scaleX = scale
                            scaleY = scale
                            translationX = -pageOffset * size.width * NeighborShift +
                                tilt.side * 10.dp.toPx() * facing
                            alpha = if (distance <= 1.05f) 1f else lerp(1f, 0f, (distance - 1.05f).coerceIn(0f, 1f))
                        }
                        .clip(PosterShape)
                        .holdToPeek(
                            onPeek = { onMoviePeek(movie) },
                            onProgress = { holdProgress = it },
                            onPressed = { pressed = it },
                        )
                        .holdCue(holdProgress)
                        .clickable(
                            interactionSource = remember(movie.id) { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                onMovieClick(movie.id)
                            },
                        )
                        .drawWithContent {
                            drawContent()
                            if (!active || !posterReady || travel <= 0f || travel >= 1f) return@drawWithContent
                            val envelope = sin(travel * PI.toFloat())
                            val lift = Color(
                                red = (shineTint.red * 0.55f + 0.45f).coerceIn(0f, 1f),
                                green = (shineTint.green * 0.55f + 0.45f).coerceIn(0f, 1f),
                                blue = (shineTint.blue * 0.55f + 0.45f).coerceIn(0f, 1f),
                                alpha = 0.28f * envelope,
                            )
                            drawRect(
                                brush = Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0f to lift,
                                        0.7f to lift.copy(alpha = lift.alpha * 0.55f),
                                        1f to Color.Transparent,
                                    ),
                                    center = Offset(size.width * 0.5f, size.height * 0.4f),
                                    radius = size.maxDimension * 0.85f,
                                ),
                                blendMode = BlendMode.Screen,
                            )
                        },
                ) {
                    PosterPage(
                        movie = movie,
                        modifier = Modifier.fillMaxSize(),
                        onReady = { posterReady = true },
                    )
                    val shade = lerp(0f, 0.46f, distance.coerceIn(0f, 1f))
                    if (shade > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = shade)),
                        )
                    }
                }
            }
            }
            val pageShift = pagerState.currentPageOffsetFraction
            val clamped = pageShift.absoluteValue.coerceIn(0f, 1f)
            val settle = (1f - clamped / 0.28f).coerceIn(0f, 1f)
            val appear = settle * settle * (3f - 2f * settle)
            PosterReflection(
                movie = active,
                width = fittedWidth,
                cardHeight = cardHeight,
                appear = appear,
                pageShift = pageShift,
                tiltSide = tilt.side,
            )
            }
        }
        Spacer(Modifier.height(8.dp))
        MovieCaption(movie = active)
        Spacer(Modifier.height(18.dp))
        PagerDots(
            count = movies.size,
            activeIndex = activeIndex,
            offsetFraction = pagerState.currentPageOffsetFraction,
            onSelect = { index ->
                if (!loop) return@PagerDots
                val delta = index - activeIndex
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + delta) }
            },
        )
    }
}

@Composable
private fun PagerDots(
    count: Int,
    activeIndex: Int,
    offsetFraction: Float,
    onSelect: (Int) -> Unit,
) {
    if (count <= 0) return
    val window = minOf(DotWindow, count)
    val position = activeIndex + offsetFraction
    val maxStart = (count - window).coerceAtLeast(0).toFloat()
    val anchor = (window - 1) / 2f
    val start = (position - anchor).coerceIn(0f, maxStart)
    val travel = position - start
    val stride = 16.dp
    val base = start.toInt()
    val shift = start - base
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Box(
            modifier = Modifier
                .offset(y = -DotsNudge)
                .width(stride * window - 4.dp)
                .height(12.dp)
                .clipToBounds(),
        ) {
            Row(
                modifier = Modifier.offset(x = stride * -shift),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val drawn = if (shift > 0.001f && base + window < count) window + 1 else window
                repeat(drawn) { slot ->
                    val index = base + slot
                    val distance = (index - position).absoluteValue
                    val edge = distance > 1.6f
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clickable(
                                interactionSource = remember(index) { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (edge) 3.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (edge) Color(0xFF2C2C30) else Color(0xFF5C5C62)),
                        )
                    }
                }
            }
            val settled = position.roundToInt().coerceIn(0, count - 1)
            Box(
                modifier = Modifier
                    .offset(x = stride * travel)
                    .size(12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(settled) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
    }
}

@Composable
private fun MovieCaption(movie: Movie?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var targetLift by remember { mutableStateOf(0.dp) }
        val extraLift by animateDpAsState(
            targetValue = targetLift,
            animationSpec = tween(durationMillis = 220),
            label = "caption-gap",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(LogoSlot),
            contentAlignment = Alignment.Center,
        ) {
            if (movie != null) {
                MovieLogoOrTitle(
                    movie = movie,
                    modifier = Modifier.offset(y = -LogoNudge),
                    titleInsetExtra = 12.dp,
                    titleFontSize = 28.sp,
                    titleLineHeight = 34.sp,
                    onContentHeight = { targetLift = it },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(20.dp)
                .fillMaxWidth()
                .offset(y = -(MetaNudge + extraLift)),
            contentAlignment = Alignment.Center,
        ) {
            if (movie != null) {
                val meta = Color(0xFF8E8E93)
                val genres = movie.genres
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    movie.year?.let { year ->
                        Text(
                            text = year.toString(),
                            color = meta,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                    if (genres.isNotEmpty()) {
                        Text(
                            text = genres.joinToString(", "),
                            color = meta,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MovieLogoOrTitle(
    movie: Movie,
    modifier: Modifier = Modifier,
    titleInsetExtra: Dp = 10.dp,
    titleFontSize: TextUnit = 20.sp,
    titleLineHeight: TextUnit = 26.sp,
    onContentHeight: (Dp) -> Unit = {},
    onUsingLogo: (Boolean) -> Unit = {},
) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    var logoFailed by remember(movie.id, movie.logoUrl) {
        mutableStateOf(movie.logoUrl.isNullOrBlank())
    }
    val usingLogo = settings.showLogos && !logoFailed
    SideEffect { onUsingLogo(usingLogo) }
    if (usingLogo) {
        var logoReady by remember(movie.id, movie.logoUrl) { mutableStateOf(false) }
        Box(
            modifier = modifier
                .height(LogoSlot)
                .width(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!logoReady) LogoSkeleton()
            MovieLogoShadow(
                model = movie.logoUrl,
                visible = logoReady,
                modifier = Modifier.fillMaxSize(),
            )
            AsyncImage(
                model = movie.logoUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                onSuccess = { state ->
                    logoReady = true
                    val raw = (state.result.drawable as? BitmapDrawable)?.bitmap ?: return@AsyncImage
                    val bitmap = if (raw.config == Bitmap.Config.HARDWARE) {
                        raw.copy(Bitmap.Config.ARGB_8888, false) ?: return@AsyncImage
                    } else {
                        raw
                    }
                    val width = bitmap.width
                    val height = bitmap.height
                    if (width <= 0 || height <= 0) return@AsyncImage
                    val row = IntArray(width)
                    var bottom = height - 1
                    while (bottom > 0) {
                        bitmap.getPixels(row, 0, width, 0, bottom, width, 1)
                        if (row.any { it ushr 24 > 24 }) break
                        bottom--
                    }
                    val maxW = with(density) { 220.dp.toPx() }
                    val maxH = with(density) { LogoSlot.toPx() }
                    val scale = min(maxW / width, maxH / height)
                    val scaledHeight = height * scale
                    val below = (maxH - scaledHeight) / 2f + (height - 1 - bottom) * scale
                    onContentHeight(with(density) { below.toDp() }.coerceAtLeast(0.dp))
                },
                onError = { logoFailed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (logoReady) 1f else 0f },
            )
        }
    } else {
        Text(
            text = movie.title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = titleFontSize,
            lineHeight = titleLineHeight,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout ->
                val textHeight = with(density) { layout.size.height.toDp() }
                onContentHeight(((LogoSlot - textHeight) / 2 + titleInsetExtra).coerceAtLeast(0.dp))
            },
            modifier = modifier,
        )
    }
}

@Composable
internal fun MovieLogoShadow(
    model: Any?,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center,
        colorFilter = ColorFilter.tint(Color.Black, BlendMode.SrcIn),
        modifier = modifier.graphicsLayer {
            val blur = 9.dp.toPx()
            renderEffect = BlurEffect(blur, blur, TileMode.Decal)
            translationY = 5.dp.toPx()
            alpha = if (visible) 0.78f else 0f
        },
    )
}

@Composable
private fun LogoSkeleton() {
    val transition = rememberInfiniteTransition(label = "logo-skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo-pulse",
    )
    Box(
        modifier = Modifier
            .width(156.dp)
            .height(18.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = pulse)),
    )
}

@Composable
internal fun CoverStackSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cover-skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cover-pulse",
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = maxWidth * CoverWidthFraction
            val cardHeight = cardWidth * CoverAspect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                contentAlignment = Alignment.Center,
            ) {
                SkeletonCard(cardWidth, cardHeight, shift = -NeighborShift, scale = NeighborScale, pulse = pulse, z = 0f)
                SkeletonCard(cardWidth, cardHeight, shift = NeighborShift, scale = NeighborScale, pulse = pulse, z = 0f)
                SkeletonCard(cardWidth, cardHeight, shift = 0f, scale = 1f, pulse = pulse, z = 1f, solid = true)
            }
        }
        Spacer(Modifier.height(ReflectionHeight + 8.dp - CaptionLift))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .offset(y = -LogoNudge)
                    .width(220.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = pulse)),
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .offset(y = -MetaNudge)
                .width(64.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = pulse)),
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.offset(y = -DotsNudge),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(DotWindow) { index ->
                Box(
                    modifier = Modifier.size(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (index == 0) 6.dp else if (index == DotWindow - 1) 3.dp else 5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = pulse)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonCard(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    shift: Float,
    scale: Float,
    pulse: Float,
    z: Float,
    solid: Boolean = false,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .zIndex(z)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = shift * size.width
            }
            .clip(PosterShape)
            .background(if (solid) Color(0xFF1A1A1E) else Color.Transparent)
            .background(Color.White.copy(alpha = pulse)),
    )
}

@Composable
private fun PosterReflection(
    movie: Movie?,
    width: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    appear: Float,
    pageShift: Float,
    tiltSide: Float,
) {
    val context = LocalContext.current
    var image by remember(movie?.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(movie?.id, movie?.posterUrl, movie?.backdropUrl) {
        val url = movie?.posterUrl ?: movie?.backdropUrl
        image = null
        if (url == null) return@LaunchedEffect
        val result = context.imageLoader.execute(
            ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(540, 800)
                .build(),
        )
        val bitmap = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        image = bitmap?.asImageBitmap()
    }
    val bitmap = image
    val clamped = pageShift.absoluteValue.coerceIn(0f, 1f)
    val facing = 1f - clamped
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val full = ReflectionHeight.roundToPx()
                val lift = CaptionLift.roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(minHeight = full, maxHeight = full),
                )
                layout(placeable.width, (full - lift).coerceAtLeast(0)) {
                    placeable.place(0, 0)
                }
            }
            .width(width)
            .height(ReflectionHeight)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0f)
                val scale = lerp(1f, NeighborScale, clamped)
                scaleX = scale
                scaleY = scale
                translationX = pageShift * size.width * NeighborShift +
                    tiltSide * 10.dp.toPx() * facing
                alpha = appear
                compositingStrategy = CompositingStrategy.Offscreen
            },
    ) {
        if (bitmap != null && appear > 0.01f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
            ) {
                val srcH = (bitmap.height * (size.height / cardHeight.toPx()))
                    .roundToInt()
                    .coerceIn(1, bitmap.height)
                scale(scaleX = 1f, scaleY = -1f, pivot = Offset(size.width / 2f, 0f)) {
                    drawImage(
                        image = bitmap,
                        srcOffset = IntOffset(0, bitmap.height - srcH),
                        srcSize = IntSize(bitmap.width, srcH),
                        dstOffset = IntOffset(0, -size.height.roundToInt()),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    )
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.38f),
                            0.22f to Color.Black.copy(alpha = 0.2f),
                            0.55f to Color.Black.copy(alpha = 0.06f),
                            1f to Color.Transparent,
                        ),
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
    }
}

@Composable
private fun PosterPage(
    movie: Movie,
    modifier: Modifier = Modifier,
    onReady: () -> Unit = {},
) {
    AsyncImage(
        model = movie.posterUrl ?: movie.backdropUrl,
        contentDescription = stringResource(R.string.poster_content_description, movie.title),
        contentScale = ContentScale.Crop,
        modifier = modifier.background(Color(0xFF2A2A2E)),
        onSuccess = { onReady() },
    )
}

private fun wordmarkPlay(
    movies: List<Movie>,
    activeIndex: Int,
    offset: Float,
    colors: Map<String, PosterPalette>,
): Color? {
    if (movies.isEmpty()) return null
    val current = colors[movies[activeIndex].id]?.primary?.forWordmark() ?: return null
    val direction = if (offset >= 0f) 1 else -1
    val neighbor = colors[movies[(activeIndex + direction).floorMod(movies.size)].id]
        ?.primary
        ?.forWordmark()
        ?: current
    return lerp(current, neighbor, offset.absoluteValue.coerceIn(0f, 1f))
}

private fun posterGlowColor(
    movies: List<Movie>,
    activeIndex: Int,
    offset: Float,
    colors: Map<String, PosterPalette>,
): PosterPalette {
    val empty = PosterPalette(Color.Transparent, Color.Transparent)
    if (movies.isEmpty()) return empty
    val current = colors[movies[activeIndex].id] ?: return empty
    val direction = if (offset >= 0f) 1 else -1
    val neighbor = colors[movies[(activeIndex + direction).floorMod(movies.size)].id] ?: current
    val fraction = offset.absoluteValue.coerceIn(0f, 1f)
    return PosterPalette(
        primary = lerp(current.primary, neighbor.primary, fraction),
        secondary = lerp(current.secondary, neighbor.secondary, fraction),
    )
}

private suspend fun Movie.extractPosterPalette(context: android.content.Context): PosterPalette? {
    val url = posterUrl ?: backdropUrl ?: return null
    val result = context.imageLoader.execute(
        ImageRequest.Builder(context)
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
    return colors?.let { PosterPalette(it.primary, it.secondary) }
}

private fun Int.floorMod(mod: Int): Int {
    val remainder = this % mod
    return if (remainder < 0) remainder + mod else remainder
}
