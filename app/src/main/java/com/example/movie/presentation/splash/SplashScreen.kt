package com.example.movie.presentation.splash

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.movie.R
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.PlayBlue
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val LogoHeight = 86.dp
private val LogoAspect = 754f / 186f
private val LetterACenter = LogoHeight * (75f / 186f)
private val LetterATop = LogoHeight * (42f / 186f)
private val HatWidth = LogoHeight * (92f / 186f) * 1.9f
private val HatHeight = HatWidth * (476f / 752f)
private const val HatCrownX = 362f / 752f
private const val HatCrownY = 280f / 476f
private val Enter = CubicBezierEasing(0.22f, 0.08f, 0.18f, 1f)

private data class SplitLogo(val amigo: ImageBitmap, val play: ImageBitmap)

@Composable
internal fun SplashScreen(onFinished: () -> Unit) {
    val amigo = remember { Animatable(0f) }
    val play = remember { Animatable(0f) }
    val hat = remember { Animatable(0f) }
    val veil = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        amigo.animateTo(1f, tween(920, easing = Enter))
        play.animateTo(1f, tween(920, easing = Enter))
        hat.animateTo(1f, tween(760, easing = FastOutSlowInEasing))
        delay(460)
        veil.animateTo(0f, tween(860, easing = FastOutSlowInEasing))
        onFinished()
    }
    BackHandler(enabled = true) {}
    BoxWithConstraints(
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
            slide = maxWidth * 0.62f,
        )
    }
}

@Composable
private fun SplashWordmark(
    amigo: Float,
    play: Float,
    hat: Float,
    slide: Dp,
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
                    translationX = (1f - amigo) * -slide.toPx()
                    alpha = (amigo / 0.12f).coerceIn(0f, 1f)
                },
        ) {
            drawBold(logo.amigo, Color.White)
        }
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationX = (1f - play) * slide.toPx()
                    alpha = (play / 0.12f).coerceIn(0f, 1f)
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
                        translationY = (1f - hat) * -46.dp.toPx()
                        rotationZ = (1f - hat) * -16f
                        alpha = (hat * 1.7f).coerceIn(0f, 1f)
                        val scale = 0.8f + 0.2f * hat
                        scaleX = scale
                        scaleY = scale
                    }
                    .size(HatWidth, HatHeight),
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
