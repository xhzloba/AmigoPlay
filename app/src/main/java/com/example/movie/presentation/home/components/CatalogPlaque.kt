package com.example.movie.presentation.home.components

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaSurface
import com.example.movie.ui.theme.PosterShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val CardShape = RoundedCornerShape(22.dp)
private val CardHeight = 132.dp
private val PosterWidth = 84.dp
private val PosterGap = 3.dp
private val FadeWidth = 56.dp
private val GlowBleedX = 18.dp
private val GlowBleedY = 22.dp
private const val SnakeSegments = 96

private val Rainbow = listOf(
    Color(0xFFFF4D6D),
    Color(0xFFFF9F1C),
    Color(0xFFFFE66D),
    Color(0xFF5CFF8A),
    Color(0xFF3DDCFF),
    Color(0xFF6C7BFF),
    Color(0xFFD46CFF),
    Color(0xFFFF4D6D),
)

@Composable
internal fun CatalogPlaque(
    movies: List<Movie>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val tick = rememberHapticTick()
    val context = LocalContext.current
    var opened by remember { mutableStateOf(context.catalogPlaqueOpened()) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(opened) {
        if (opened) glow.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
        else glow.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val covers = List(3) { movies.getOrNull(it) }
    val stack = PosterWidth * covers.size + PosterGap * (covers.size - 1)
    Box(
        modifier = modifier
            .padding(horizontal = 22.dp - GlowBleedX)
            .fillMaxWidth(),
    ) {
        if (!opened || glow.value > 0.01f) {
            RainbowHalo(
                alpha = glow.value,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .padding(horizontal = GlowBleedX, vertical = GlowBleedY)
                .fillMaxWidth()
                .height(CardHeight)
                .clip(CardShape)
                .background(CinemaSurface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        if (!opened) {
                            context.markCatalogPlaqueOpened()
                            opened = true
                        }
                        onClick()
                    },
                ),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(stack)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(PosterGap),
            ) {
                covers.forEach { movie ->
                    if (movie == null) {
                        Box(
                            modifier = Modifier
                                .width(PosterWidth)
                                .fillMaxHeight()
                                .clip(PosterShape)
                                .background(Color.White.copy(alpha = 0.06f)),
                        )
                    } else {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = stringResource(R.string.poster_content_description, movie.title),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(PosterWidth)
                                .fillMaxHeight()
                                .clip(PosterShape),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(FadeWidth)
                    .fillMaxHeight()
                    .offset(x = -(stack - FadeWidth))
                    .background(
                        Brush.horizontalGradient(
                            listOf(CinemaSurface, Color.Transparent),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp)
                    .width(150.dp),
            ) {
                Text(
                    text = stringResource(R.string.catalog_plaque_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.catalog_plaque_hint),
                    color = Color(0xFFB4B4B8),
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    letterSpacing = (-0.1).sp,
                )
            }
        }
    }
}

@Composable
private fun RainbowHalo(alpha: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "catalogGlow")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6400, easing = LinearEasing)),
        label = "catalogGlowTurn",
    )
    val breath by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "catalogGlowBreath",
    )
    Box(
        modifier
            .graphicsLayer {
                val radius = 6.dp.toPx()
                renderEffect = BlurEffect(radius, radius, TileMode.Decal)
                clip = false
            }
            .drawBehind {
            val strength = alpha * breath
            val card = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = GlowBleedX.toPx(),
                        top = GlowBleedY.toPx(),
                        right = size.width - GlowBleedX.toPx(),
                        bottom = size.height - GlowBleedY.toPx(),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                    ),
                )
            }
            clipPath(card, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                drawSnake(shift = turn / 360f, strength = strength)
            }
        },
    )
}

private fun DrawScope.drawSnake(shift: Float, strength: Float) {
    val left = GlowBleedX.toPx()
    val top = GlowBleedY.toPx()
    val right = size.width - left
    val bottom = size.height - top
    val radius = 22.dp.toPx()
    val straightX = (right - left - 2f * radius).coerceAtLeast(0f)
    val straightY = (bottom - top - 2f * radius).coerceAtLeast(0f)
    val arc = (PI.toFloat() / 2f) * radius
    val perimeter = 2f * (straightX + straightY) + 4f * arc
    val thin = 11.dp.toPx()
    val thick = 22.dp.toPx()
    val points = Array(SnakeSegments) { index ->
        pointOnRoundRect(
            distance = perimeter * index / SnakeSegments,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            radius = radius,
            straightX = straightX,
            straightY = straightY,
            arc = arc,
        )
    }
    repeat(2) { pass ->
        val widthScale = if (pass == 0) 1f else 0.42f
        val alphaScale = if (pass == 0) 0.38f else 1f
        for (index in 0 until SnakeSegments) {
            val t = index / SnakeSegments.toFloat()
            val wave = 0.5f + 0.5f * sin((t * 2f - shift) * (PI.toFloat() * 2f))
            val width = (thin + (thick - thin) * (0.62f + 0.38f * wave)) * widthScale
            drawLine(
                color = rainbowAt((t + shift) % 1f).copy(
                    alpha = strength * alphaScale * (0.84f + 0.16f * wave),
                ),
                start = points[index],
                end = points[(index + 1) % SnakeSegments],
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun pointOnRoundRect(
    distance: Float,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
    straightX: Float,
    straightY: Float,
    arc: Float,
): Offset {
    var d = distance
    if (d < straightX) return Offset(left + radius + d, top)
    d -= straightX
    if (d < arc) {
        return arcPoint(right - radius, top + radius, radius, -PI.toFloat() / 2f, d / arc)
    }
    d -= arc
    if (d < straightY) return Offset(right, top + radius + d)
    d -= straightY
    if (d < arc) return arcPoint(right - radius, bottom - radius, radius, 0f, d / arc)
    d -= arc
    if (d < straightX) return Offset(right - radius - d, bottom)
    d -= straightX
    if (d < arc) return arcPoint(left + radius, bottom - radius, radius, PI.toFloat() / 2f, d / arc)
    d -= arc
    if (d < straightY) return Offset(left, bottom - radius - d)
    d -= straightY
    return arcPoint(left + radius, top + radius, radius, PI.toFloat(), (d / arc).coerceIn(0f, 1f))
}

private fun arcPoint(cx: Float, cy: Float, radius: Float, start: Float, fraction: Float): Offset {
    val angle = start + fraction * (PI.toFloat() / 2f)
    return Offset(cx + cos(angle) * radius, cy + sin(angle) * radius)
}

private fun rainbowAt(t: Float): Color {
    val span = Rainbow.size - 1
    val scaled = (t % 1f) * span
    val index = scaled.toInt().coerceIn(0, span - 1)
    return lerp(Rainbow[index], Rainbow[index + 1], scaled - index)
}

private fun Context.catalogPlaqueOpened(): Boolean =
    applicationContext.getSharedPreferences(PLAQUE_PREFS, Context.MODE_PRIVATE)
        .getBoolean(PLAQUE_OPENED, false)

private fun Context.markCatalogPlaqueOpened() {
    applicationContext.getSharedPreferences(PLAQUE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(PLAQUE_OPENED, true)
        .commit()
}

private const val PLAQUE_PREFS = "catalog_plaque"
private const val PLAQUE_OPENED = "opened"
