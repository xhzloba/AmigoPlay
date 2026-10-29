package com.example.movie.presentation.home.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.movie.R
import com.example.movie.domain.model.Shelf
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.PlayBlue

internal val CategoryTabFontSize = 18.sp
internal val CategoryTabLineHeight = 24.sp
internal val CategoryTabLetterSpacing = (-0.2).sp

private val LogoHeight = 36.dp
private val LogoAspect = 754f / 186f
internal val BrandLogoBleed = LogoHeight * (29f / 186f)
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

private val TabInk = Color(0xFFC7C7CC)
private val TabShape = CircleShape

private val TabSkeletonLabels = listOf(
    "В тренде",
    "Сейчас смотрят",
    "4K",
    "Dolby Vision",
    "Обновления",
    "Фильмы",
)

@Composable
internal fun CategoryTabs(
    shelves: List<Shelf>,
    selectedId: String?,
    playColor: Color,
    onSelect: (String) -> Unit,
    onSearch: () -> Unit,
    hat: Boolean = false,
    mustache: Boolean = false,
    onMenuSettled: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    MenuSombreroEffect(listState, onMenuSettled)
    HapticOnChange(listState) { listState.passedTab() }
    LaunchedEffect(selectedId, shelves) {
        val index = shelves.indexOfFirst { it.id == selectedId }
        if (index < 0) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == index }
        if (!visible) listState.animateScrollToItem(index)
    }
    Column(modifier = modifier) {
        BrandRow(play = playColor, onSearch = onSearch, hat = hat, mustache = mustache)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        items(shelves, key = { it.id }) { shelf ->
            ShelfCapsule(
                title = shelf.title,
                selected = shelf.id == selectedId,
                onClick = { onSelect(shelf.id) },
            )
        }
        }
    }
}

private fun LazyListState.passedTab(): Int {
    val head = layoutInfo.visibleItemsInfo.firstOrNull() ?: return firstVisibleItemIndex
    return if (head.offset + head.size / 2 < 0) head.index + 1 else head.index
}

@Composable
private fun ShelfCapsule(
    title: String,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    ghost: Boolean = false,
) {
    Text(
        text = title,
        color = if (ghost) Color.Transparent else if (selected) Color.White else TabInk,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        fontSize = CategoryTabFontSize,
        lineHeight = CategoryTabLineHeight,
        letterSpacing = CategoryTabLetterSpacing,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.12f), TabShape)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = remember(title) { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun BrandRow(
    play: Color,
    onSearch: () -> Unit,
    hat: Boolean = false,
    mustache: Boolean = false,
) {
    val tick = rememberHapticTick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp - BrandLogoBleed, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandWordmark(play = play, hat = hat, mustache = mustache)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onSearch()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShellIcons.search,
                contentDescription = stringResource(R.string.search),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private data class SplitLogo(val amigo: ImageBitmap, val play: ImageBitmap)

@Composable
internal fun MenuSombreroEffect(listState: LazyListState, onSettled: () -> Unit) {
    SettleEffect(listState, { listState.isScrollInProgress }, onSettled)
}

@Composable
internal fun SettleEffect(key: Any, scrolling: () -> Boolean, onSettled: () -> Unit) {
    val settled by rememberUpdatedState(onSettled)
    LaunchedEffect(key) {
        var trailing = false
        snapshotFlow { scrolling() }
            .distinctUntilChanged()
            .collect { moving ->
                if (moving) {
                    trailing = true
                } else if (trailing) {
                    delay(140)
                    if (scrolling()) return@collect
                    trailing = false
                    settled()
                }
            }
    }
}

internal class LogoMood {
    var hat by mutableStateOf(false)
        private set
    var mustache by mutableStateOf(false)
        private set
    private var rides = 0

    fun onSettled() {
        if (!hat && !mustache) {
            if (Random.nextFloat() < 0.14f) {
                if (Random.nextBoolean()) hat = true else mustache = true
                rides = 0
            }
            return
        }
        rides += 1
        if (rides == 1) return
        val combo = hat && mustache
        if (!combo && Random.nextFloat() < 0.12f) {
            hat = true
            mustache = true
            rides = 0
            return
        }
        val leaves = when {
            combo && rides == 2 -> 0.3f
            combo -> 0.65f
            rides == 2 -> 0.25f
            else -> 0.55f
        }
        if (Random.nextFloat() < leaves) {
            if (combo && Random.nextFloat() < 0.5f) {
                if (Random.nextBoolean()) hat = false else mustache = false
            } else {
                hat = false
                mustache = false
            }
            rides = 0
        }
    }
}

@Composable
internal fun BrandWordmark(
    play: Color,
    hat: Boolean = false,
    mustache: Boolean = false,
) {
    val context = LocalContext.current
    val logo = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val source = BitmapFactory.decodeResource(context.resources, R.drawable.amigoplay_logo, options)
        source.splitLogo()
    }
    val drop = remember { Animatable(0f) }
    val whiskers = remember { Animatable(0f) }
    LaunchedEffect(hat) {
        if (hat) {
            drop.animateTo(
                1f,
                spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow),
            )
        } else {
            drop.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(mustache) {
        if (mustache) {
            whiskers.animateTo(
                1f,
                spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
            )
        } else {
            whiskers.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
        }
    }
    val landed = drop.value
    val grown = whiskers.value
    Box(
        modifier = Modifier
            .height(LogoHeight)
            .aspectRatio(LogoAspect, matchHeightConstraintsFirst = true),
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .semantics { contentDescription = "AmigoPlay" },
        ) {
            val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
            drawImage(
                image = logo.amigo,
                dstSize = dst,
                filterQuality = FilterQuality.High,
                colorFilter = ColorFilter.tint(Color.White),
            )
            drawImage(
                image = logo.play,
                dstSize = dst,
                filterQuality = FilterQuality.High,
                colorFilter = ColorFilter.tint(play),
            )
        }
        if (hat || landed > 0.01f) {
            val lift = (1f - landed) * -16f
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
                        translationY = lift.dp.toPx()
                        alpha = (landed * 1.6f).coerceIn(0f, 1f)
                        val scale = 0.62f + 0.38f * landed.coerceAtMost(1.12f)
                        scaleX = scale
                        scaleY = scale
                        rotationZ = (1f - landed.coerceAtMost(1f)) * -8f
                    }
                    .size(HatWidth, HatHeight),
            )
        }
        if (mustache || grown > 0.01f) {
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
                        val scale = 0.2f + 0.8f * grown.coerceAtMost(1.08f)
                        scaleX = scale
                        scaleY = scale
                        alpha = (grown * 1.8f).coerceIn(0f, 1f)
                        translationY = (1f - grown) * 8.dp.toPx()
                    }
                    .size(MustacheWidth, MustacheHeight),
            )
        }
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

@Composable
fun CategoryTabsSkeleton(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BrandRow(play = PlayBlue, onSearch = onSearch)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false,
        ) {
        items(TabSkeletonLabels.size) { index ->
            ShelfCapsule(
                title = TabSkeletonLabels[index],
                selected = false,
                ghost = true,
            )
        }
        }
    }
}
