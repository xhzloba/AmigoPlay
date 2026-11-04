package com.example.movie.presentation.update

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.domain.model.AppRelease
import com.example.movie.domain.model.DownloadProgress
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val ScrimMillis = 280
private const val SheetMillis = 340
private const val ScrimOutMillis = 200
private const val SheetOutMillis = 220

private val CardShape = RoundedCornerShape(28.dp)
private val ButtonShape = RoundedCornerShape(22.dp)
private val VersionBlue = Color(0xFF7EC8FF)
private val NotesColor = Color(0xFFD2D2D8)
private val FailedColor = Color(0xFFE7B3B3)

@Composable
fun AppUpdateHost() {
    val updates = LocalAppUpdate.current
    val context = LocalContext.current
    val tick = rememberHapticTick()
    val release = updates.release
    val visible = updates.sheet && release != null
    var held by remember { mutableStateOf<AppRelease?>(null) }
    if (visible && release != null) held = release
    val shown = held

    BackHandler(enabled = visible && shown != null) { updates.postpone() }

    if (shown != null) {
        UpdateSheet(
            release = shown,
            phase = updates.phase,
            progress = updates.progress,
            visible = visible,
            onLater = updates::postpone,
            onUpdate = {
                tick?.invoke()
                updates.download(context)
            },
            onHidden = { held = null },
        )
    }
}

@Composable
private fun UpdateSheet(
    release: AppRelease,
    phase: UpdatePhase,
    progress: DownloadProgress,
    visible: Boolean,
    onLater: () -> Unit,
    onUpdate: () -> Unit,
    onHidden: () -> Unit,
) {
    val scrim = remember { Animatable(0f) }
    val sheet = remember { Animatable(1f) }
    LaunchedEffect(visible) {
        if (visible) {
            coroutineScope {
                launch { scrim.animateTo(1f, tween(ScrimMillis, easing = FastOutSlowInEasing)) }
                launch { sheet.animateTo(0f, tween(SheetMillis, easing = FastOutSlowInEasing)) }
            }
        } else {
            coroutineScope {
                launch { scrim.animateTo(0f, tween(ScrimOutMillis, easing = FastOutSlowInEasing)) }
                launch { sheet.animateTo(1f, tween(SheetOutMillis, easing = FastOutSlowInEasing)) }
            }
            onHidden()
        }
    }
    val animated by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(120),
        label = "updateProgress",
    )
    val notes = remember(release.notes) { releaseNotes(release.notes) }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrim.value }
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLater,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    val travel = size.height
                    translationY = if (travel == 0f) 4000f else travel * sheet.value
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .fillMaxWidth()
                .clip(CardShape)
                .background(CinemaSurface)
                .border(0.5.dp, Color.White.copy(alpha = 0.10f), CardShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp),
        ) {
            Text(
                text = stringResource(R.string.update_title),
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Text(
                text = release.versionName,
                modifier = Modifier.padding(top = 4.dp),
                color = VersionBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.6).sp,
            )
            if (notes.added.isNotEmpty() || notes.fixed.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (notes.added.isNotEmpty()) {
                        NoteLabel(stringResource(R.string.changelog_added))
                        notes.added.forEach { NoteLine(it) }
                    }
                    if (notes.fixed.isNotEmpty()) {
                        if (notes.added.isNotEmpty()) Spacer(Modifier.height(16.dp))
                        NoteLabel(stringResource(R.string.changelog_fixed))
                        notes.fixed.forEach { NoteLine(it) }
                    }
                }
            }
            if (phase == UpdatePhase.Failed) {
                Text(
                    text = stringResource(R.string.update_failed),
                    modifier = Modifier.padding(top = 14.dp),
                    color = FailedColor,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                )
            }
            if (phase == UpdatePhase.Downloading) {
                Text(
                    text = downloadStatus(progress),
                    modifier = Modifier.padding(top = 16.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animated.coerceIn(0f, 1f))
                            .background(Color.White),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(top = if (phase == UpdatePhase.Downloading) 14.dp else 22.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.update_later),
                    modifier = Modifier
                        .clip(ButtonShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onLater,
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    color = CinemaMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.weight(1f))
                val label = when (phase) {
                    UpdatePhase.Failed -> stringResource(R.string.retry)
                    UpdatePhase.Downloading -> stringResource(R.string.update_downloading)
                    UpdatePhase.Ready -> stringResource(R.string.update_action)
                }
                Text(
                    text = label,
                    modifier = Modifier
                        .clip(ButtonShape)
                        .background(Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = phase != UpdatePhase.Downloading,
                            onClick = onUpdate,
                        )
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun NoteLabel(text: String) {
    Text(
        text = text,
        color = CinemaMuted,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
}

@Composable
private fun NoteLine(text: String) {
    Row(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = "—",
            color = NotesColor,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            color = NotesColor,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}
