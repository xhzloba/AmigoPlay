package com.example.movie.presentation.splash

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.movie.R
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.PlayBlue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val LogoHeight = 86.dp
private val LogoAspect = 754f / 186f
private val LetterACenter = LogoHeight * (75f / 186f)
private val LetterATop = LogoHeight * (42f / 186f)
private val HatWidth = LogoHeight * (92f / 186f) * 1.9f
private val HatHeight = HatWidth * (476f / 752f)
private const val HatCrownX = 362f / 752f
private const val HatCrownY = 280f / 476f
private val LetterAMid = LogoHeight * (104f / 186f)
private val MustacheCenter = LogoHeight * (75f / 186f)
private val MustacheWidth = LogoHeight * (92f / 186f) * 1.4f
private val MustacheHeight = MustacheWidth * (348f / 892f)
private const val MustacheX = 443f / 892f
private const val MustacheY = 207f / 348f
private val Slide = 64.dp

private data class SplitLogo(val amigo: ImageBitmap, val play: ImageBitmap)

@Composable
internal fun SplashScreen(onFinished: () -> Unit) {
    val amigo = remember { Animatable(0f) }
    val play = remember { Animatable(0f) }
    val hat = remember { Animatable(0f) }
    val whiskers = remember { Animatable(0f) }
    val veil = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { amigo.animateTo(1f, tween(720, easing = LinearOutSlowInEasing)) }
            delay(90)
            launch { play.animateTo(1f, tween(720, easing = LinearOutSlowInEasing)) }
        }
        hat.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
        whiskers.animateTo(1f, tween(680, easing = FastOutSlowInEasing))
        delay(520)
        veil.animateTo(0f, tween(860, easing = FastOutSlowInEasing))
        onFinished()
    }
    BackHandler(enabled = true) {}
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = veil.value }
            .background(CinemaBlack)
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
            hat = hat.value,
            whiskers = whiskers.value,
        )
    }
}

@Composable
private fun SplashWordmark(
    amigo: Float,
    play: Float,
    hat: Float,
    whiskers: Float,
) {
    val context = LocalContext.current
    val logo = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val source = BitmapFactory.decodeResource(context.resources, R.drawable.amigoplay_logo, options)
        source.splitLogo()
    }
    Box(
        modifier = Modifier
            .height(LogoHeight)
            .aspectRatio(LogoAspect, matchHeightConstraintsFirst = true)
            .semantics { contentDescription = "AmigoPlay" },
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationX = (1f - amigo) * -Slide.toPx()
                    alpha = amigo
                },
        ) {
            drawBold(logo.amigo, Color.White)
        }
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationX = (1f - play) * Slide.toPx()
                    alpha = play
                },
        ) {
            drawBold(logo.play, PlayBlue)
        }
        if (hat > 0.01f) {
            Image(
                painter = painterResource(R.drawable.sombrero),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = LetterACenter - HatWidth * HatCrownX,
                        y = LetterATop - HatHeight * HatCrownY,
                    )
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(HatCrownX, HatCrownY)
                        translationY = (1f - hat) * -18.dp.toPx()
                        alpha = hat
                        val scale = 0.94f + 0.06f * hat
                        scaleX = scale
                        scaleY = scale
                    }
                    .size(HatWidth, HatHeight),
            )
        }
        if (whiskers > 0.01f) {
            Image(
                painter = painterResource(R.drawable.mustache),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = MustacheCenter - MustacheWidth * MustacheX,
                        y = LetterAMid - MustacheHeight * MustacheY,
                    )
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(MustacheX, MustacheY)
                        translationY = (1f - whiskers) * 12.dp.toPx()
                        alpha = whiskers
                        val scale = 0.86f + 0.14f * whiskers
                        scaleX = scale
                        scaleY = scale
                    }
                    .size(MustacheWidth, MustacheHeight),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBold(
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
