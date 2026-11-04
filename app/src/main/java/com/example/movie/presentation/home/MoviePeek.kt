package com.example.movie.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import coil.compose.AsyncImage
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.toSaved
import com.example.movie.presentation.common.BookmarkFilled
import com.example.movie.presentation.common.BookmarkOutline
import com.example.movie.presentation.common.ImdbScore
import com.example.movie.presentation.common.KpScore
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PlayBlue

private val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val PopupShape = RoundedCornerShape(22.dp)
private val Scrim = Color.Black.copy(alpha = 0.55f)
private val RowInk = Color(0xFFE8E8EC)

private enum class PeekPage {
    Menu,
    Summary,
}

internal val PeekEnter =
    fadeIn(tween(180)) + scaleIn(
        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
        initialScale = 0.96f,
    )

internal val PeekExit =
    fadeOut(tween(140)) + scaleOut(animationSpec = tween(140), targetScale = 0.98f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoviePeek(
    movie: Movie?,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var page by remember { mutableStateOf(PeekPage.Menu) }
    var suppressDismiss by remember { mutableStateOf(false) }
    LaunchedEffect(movie?.id) {
        page = PeekPage.Menu
        suppressDismiss = false
    }
    val shown = movie ?: return
    when (page) {
        PeekPage.Menu -> MovieMenu(
            movie = shown,
            onDismiss = {
                if (!suppressDismiss) onDismiss()
            },
            onSummary = {
                suppressDismiss = true
                page = PeekPage.Summary
            },
        )
        PeekPage.Summary -> OverviewPopup(
            movie = shown,
            onDismiss = onDismiss,
            onOpen = { onOpen(shown.id) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieMenu(
    movie: Movie,
    onDismiss: () -> Unit,
    onSummary: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val favorites = LocalAppContainer.current.favorites
    val watched = LocalAppContainer.current.watched
    val savedIds by favorites.ids.collectAsStateWithLifecycle()
    val watchedIds by watched.ids.collectAsStateWithLifecycle()
    val saved = movie.id in savedIds
    val seen = movie.id in watchedIds
    val tick = rememberHapticTick()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CinemaBlack,
        contentColor = Color.White,
        scrimColor = Scrim,
        dragHandle = null,
        shape = SheetShape,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
            )
            MenuHeading(movie)
            Spacer(Modifier.height(6.dp))
            MenuRow(
                icon = OverviewIcon,
                label = stringResource(R.string.menu_overview),
                onClick = {
                    tick?.invoke()
                    onSummary()
                },
            )
            MenuRow(
                icon = if (saved) BookmarkFilled else BookmarkOutline,
                label = stringResource(if (saved) R.string.menu_favorite_remove else R.string.menu_favorite_add),
                onClick = {
                    tick?.invoke()
                    favorites.toggle(movie.toSaved())
                    onDismiss()
                },
            )
            MenuRow(
                icon = CheckIcon,
                label = stringResource(if (seen) R.string.menu_watched else R.string.menu_unwatched),
                marked = seen,
                onClick = {
                    tick?.invoke()
                    watched.toggle(movie.toSaved())
                    onDismiss()
                },
            )
            MenuRow(
                icon = ListIcon,
                label = stringResource(R.string.menu_list),
                enabled = false,
            )
            MenuRow(
                icon = CommentIcon,
                label = stringResource(R.string.menu_comment),
                enabled = false,
            )
        }
    }
}

@Composable
private fun MenuHeading(movie: Movie) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    var logoFailed by remember(movie.id, movie.logoUrl, settings.showLogos) {
        mutableStateOf(!settings.showLogos || movie.logoUrl.isNullOrBlank())
    }
    var logoReady by remember(movie.id, movie.logoUrl, settings.showLogos) { mutableStateOf(false) }
    if (!logoFailed) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!logoReady) MenuLogoSkeleton()
            AsyncImage(
                model = movie.logoUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                onSuccess = { logoReady = true },
                onError = { logoFailed = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp)
                    .height(48.dp)
                    .graphicsLayer { alpha = if (logoReady) 1f else 0f },
            )
        }
    } else {
        Text(
            text = movie.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 23.sp,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val facts = movie.menuFacts()
    if (facts.isBlank() && movie.ratingKp == null && movie.ratingImdb == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (facts.isNotBlank()) {
            Text(
                text = facts,
                modifier = Modifier.weight(1f, fill = false),
                color = CinemaMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        movie.ratingKp?.let { score ->
            Spacer(Modifier.width(10.dp))
            KpScore(
                score = score,
                color = CinemaMuted,
                iconSize = 12.dp,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        movie.ratingImdb?.let { score ->
            Spacer(Modifier.width(10.dp))
            ImdbScore(
                score = score,
                color = CinemaMuted,
                iconHeight = 12.dp,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun MenuLogoSkeleton() {
    val pulse by rememberInfiniteTransition(label = "menu-logo").animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "menu-logo-pulse",
    )
    Box(
        Modifier
            .width(148.dp)
            .height(16.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = pulse)),
    )
}

private fun Movie.menuFacts(): String {
    val country = country
        .split(',', '/', '·', '|', ';')
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
    val genres = genres
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString(", ")
    return listOfNotNull(
        country,
        year?.toString(),
        genres.takeIf { it.isNotEmpty() },
    ).joinToString(" · ")
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    marked: Boolean = false,
    onClick: () -> Unit = {},
) {
    val ink = if (enabled) RowInk else CinemaMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (enabled) onClick() },
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (marked) PlayBlue else ink,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = if (marked) Color.White else ink,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OverviewPopup(
    movie: Movie,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    val tick = rememberHapticTick()
    val text = movie.overview.ifBlank { stringResource(R.string.menu_overview_empty) }
    AnimatedVisibility(
        visible = true,
        enter = PeekEnter,
        exit = PeekExit,
    ) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .clip(PopupShape)
                    .background(CinemaBlack)
                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), PopupShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = text,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    color = Color(0xFFD2D2D8),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                onOpen()
                            },
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        text = stringResource(R.string.menu_open_detail),
                        color = PlayBlue,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private val OverviewIcon = strokeIcon("overview") {
    moveTo(4.5f, 6.5f)
    lineTo(19.5f, 6.5f)
    moveTo(4.5f, 12f)
    lineTo(19.5f, 12f)
    moveTo(4.5f, 17.5f)
    lineTo(13f, 17.5f)
}

private val CheckIcon = strokeIcon("check") {
    circle(12f, 12f, 8.2f)
    moveTo(8.3f, 12.2f)
    lineTo(10.9f, 14.8f)
    lineTo(15.8f, 9.4f)
}

private val ListIcon = strokeIcon("list") {
    circle(5f, 6.5f, 1.15f)
    circle(5f, 12f, 1.15f)
    circle(5f, 17.5f, 1.15f)
    moveTo(8.6f, 6.5f)
    lineTo(19.5f, 6.5f)
    moveTo(8.6f, 12f)
    lineTo(19.5f, 12f)
    moveTo(8.6f, 17.5f)
    lineTo(19.5f, 17.5f)
}

private val CommentIcon = strokeIcon("comment") {
    moveTo(6.2f, 5.2f)
    lineTo(17.8f, 5.2f)
    quadTo(20.2f, 5.2f, 20.2f, 7.6f)
    lineTo(20.2f, 13.6f)
    quadTo(20.2f, 16f, 17.8f, 16f)
    lineTo(10.2f, 16f)
    lineTo(6.2f, 19.4f)
    lineTo(6.2f, 16f)
    lineTo(6.2f, 7.6f)
    quadTo(6.2f, 5.2f, 8.6f, 5.2f)
    close()
}

private fun strokeIcon(name: String, body: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = body,
        )
    }.build()

private fun PathBuilder.circle(cx: Float, cy: Float, radius: Float) {
    moveTo(cx + radius, cy)
    arcTo(radius, radius, 0f, true, true, cx - radius, cy)
    arcTo(radius, radius, 0f, true, true, cx + radius, cy)
}
