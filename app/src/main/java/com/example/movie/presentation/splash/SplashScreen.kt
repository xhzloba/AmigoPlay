package com.example.movie.presentation.splash

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.movie.R
import com.example.movie.presentation.common.appBackground
import com.example.movie.ui.theme.PlayBlue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val LogoHeight = 86.dp
private val LogoAspect = 754f / 186f
private val Enter = CubicBezierEasing(0.22f, 0.08f, 0.18f, 1f)

private data class SplitLogo(val amigo: ImageBitmap, val play: ImageBitmap)

@Composable
internal fun SplashScreen(onReveal: () -> Unit, onFinished: () -> Unit) {
    val amigo = remember { Animatable(0f) }
    val play = remember { Animatable(0f) }
    val veil = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { amigo.animateTo(1f, tween(920, easing = Enter)) }
            delay(340)
            launch { play.animateTo(1f, tween(680, easing = Enter)) }
        }
        onReveal()
        delay(280)
        veil.animateTo(0f, tween(640, easing = FastOutSlowInEasing))
        onFinished()
    }
    BackHandler(enabled = true) {}
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = veil.value)
            .appBackground()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        SplashWordmark(
            amigo = amigo.value,
            play = play.value,
            slide = maxWidth * 0.62f,
        )
    }
}

@Composable
private fun SplashWordmark(
    amigo: Float,
    play: Float,
    slide: Dp,
) {
    val context = LocalContext.current
    val logo = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val source = BitmapFactory.decodeResource(context.resources, R.drawable.amigoplay_logo, options)
        source.splitLogo()
    }
    val amigoMark = rememberBold(logo.amigo, Color.White)
    val playMark = rememberBold(logo.play, PlayBlue)
    val slidePx = with(LocalDensity.current) { slide.toPx() }
    Box(
        modifier = Modifier
            .height(LogoHeight)
            .aspectRatio(LogoAspect, matchHeightConstraintsFirst = true)
            .semantics { contentDescription = "AmigoPlay" },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer(
                    translationX = (1f - amigo) * -slidePx,
                    alpha = (amigo / 0.12f).coerceIn(0f, 1f),
                )
                .then(amigoMark),
        )
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer(
                    translationX = (1f - play) * slidePx,
                    alpha = (play / 0.12f).coerceIn(0f, 1f),
                )
                .then(playMark),
        )
    }
}

@Composable
private fun rememberBold(image: ImageBitmap, tint: Color): Modifier {
    return remember(image, tint) {
        Modifier.drawWithCache {
            onDrawBehind { drawBold(image, tint) }
        }
    }
}

private fun DrawScope.drawBold(
    image: ImageBitmap,
    tint: Color,
) {
    val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
    val filter = ColorFilter.tint(tint)
    val thicken = 1
    val offsets = arrayOf(
        IntOffset.Zero,
        IntOffset(thicken, 0),
        IntOffset(0, thicken),
    )
    offsets.forEach { offset ->
        drawImage(
            image = image,
            dstOffset = offset,
            dstSize = dst,
            filterQuality = FilterQuality.High,
            colorFilter = filter,
        )
    }
}

private fun Bitmap.splitLogo(): SplitLogo {
    val width = this.width
    val height = this.height
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
    return SplitLogo(amigo.toMask(width, height), play.toMask(width, height))
}

private fun IntArray.toMask(width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(this, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}
