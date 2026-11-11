package com.example.movie.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.data.download.OfflinePhase
import com.example.movie.data.download.OfflineStatus
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PlayBlue
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive

/**
 * Media3 часто долго отдаёт percentDownloaded = -1 на HLS.
 * Пока процента нет — ползунок медленно ползёт; когда % появился — плавно догоняет.
 */
@Composable
internal fun smoothDownloadProgress(status: OfflineStatus): Float {
    val displayed = remember { Animatable(0f) }
    LaunchedEffect(status.phase, status.percent, status.indeterminate) {
        when (status.phase) {
            OfflinePhase.Ready -> displayed.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            OfflinePhase.Failed -> Unit
            OfflinePhase.Paused -> {
                val target = (status.percent / 100f).coerceIn(0f, 1f)
                if (target > 0f) displayed.snapTo(maxOf(displayed.value, target))
            }
            OfflinePhase.Preparing, OfflinePhase.Downloading -> {
                if (status.indeterminate || status.percent <= 0) {
                    while (isActive) {
                        val next = (displayed.value + 0.01f).coerceAtMost(0.16f)
                        if (next <= displayed.value + 0.0001f) {
                            displayed.animateTo((displayed.value - 0.01f).coerceAtLeast(0.08f), tween(500))
                            displayed.animateTo(0.16f, tween(700, easing = FastOutSlowInEasing))
                        } else {
                            displayed.animateTo(next, tween(320, easing = LinearEasing))
                        }
                    }
                } else {
                    val target = maxOf(displayed.value, status.percent / 100f).coerceIn(0.02f, 1f)
                    val jump = abs(target - displayed.value)
                    displayed.animateTo(
                        target,
                        tween(
                            durationMillis = when {
                                jump > 0.35f -> 1_150
                                jump > 0.15f -> 780
                                else -> 420
                            },
                            easing = FastOutSlowInEasing,
                        ),
                    )
                }
            }
        }
    }
    return displayed.value
}

@Composable
internal fun DownloadRing(
    status: OfflineStatus?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    stroke: Dp = 2.5.dp,
    track: Color = Color.White.copy(alpha = 0.18f),
    fill: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        when (status?.phase) {
            OfflinePhase.Preparing -> PreparingRing(stroke = stroke, track = track, fill = fill)
            OfflinePhase.Downloading, OfflinePhase.Paused -> {
                if (status.indeterminate || status.percent <= 0) {
                    PreparingRing(stroke = stroke, track = track, fill = fill)
                } else {
                    ProgressRing(
                        progress = smoothDownloadProgress(status),
                        stroke = stroke,
                        track = track,
                        fill = fill,
                    )
                }
            }
            else -> Unit
        }
        content()
    }
}

@Composable
private fun PreparingRing(stroke: Dp, track: Color, fill: Color) {
    val spin by rememberInfiniteTransition(label = "download-spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "download-spin-angle",
    )
    Canvas(Modifier.fillMaxSize()) {
        val width = stroke.toPx()
        val inset = width / 2f
        val arc = Size(size.width - width, size.height - width)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arc,
            style = Stroke(width = width, cap = StrokeCap.Round),
        )
        drawArc(
            color = fill,
            startAngle = -90f + spin,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arc,
            style = Stroke(width = width, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun ProgressRing(progress: Float, stroke: Dp, track: Color, fill: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val width = stroke.toPx()
        val inset = width / 2f
        val arc = Size(size.width - width, size.height - width)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arc,
            style = Stroke(width = width, cap = StrokeCap.Round),
        )
        drawArc(
            color = fill,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arc,
            style = Stroke(width = width, cap = StrokeCap.Round),
        )
    }
}

@Composable
internal fun DownloadBar(
    status: OfflineStatus,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    track: Color = Color.White.copy(alpha = 0.12f),
    fill: Color = PlayBlue,
    showPercent: Boolean = true,
) {
    val showLabel = showPercent &&
        status.phase.inProgress() &&
        !status.indeterminate &&
        status.percent > 0
    Column(modifier.fillMaxWidth()) {
        if (showLabel) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${status.percent}%",
                    color = CinemaMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        when (status.phase) {
            OfflinePhase.Preparing, OfflinePhase.Downloading, OfflinePhase.Paused -> ProgressBar(
                progress = if (status.phase == OfflinePhase.Paused && status.percent > 0) {
                    status.percent / 100f
                } else {
                    smoothDownloadProgress(status)
                },
                modifier = Modifier.fillMaxWidth(),
                height = height,
                track = track,
                fill = if (status.phase == OfflinePhase.Paused) fill.copy(alpha = 0.55f) else fill,
            )
            OfflinePhase.Ready -> Canvas(Modifier.fillMaxWidth().height(height)) {
                drawRoundRect(
                    color = fill,
                    cornerRadius = CornerRadius(height.toPx(), height.toPx()),
                )
            }
            OfflinePhase.Failed -> Canvas(Modifier.fillMaxWidth().height(height)) {
                drawRoundRect(
                    color = track,
                    cornerRadius = CornerRadius(height.toPx(), height.toPx()),
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    modifier: Modifier,
    height: Dp,
    track: Color,
    fill: Color,
) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(height.toPx(), height.toPx())
        drawRoundRect(color = track, cornerRadius = radius)
        drawRoundRect(
            color = fill,
            size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
            cornerRadius = radius,
        )
    }
}

@Composable
internal fun offlineProgressTitle(status: OfflineStatus): String = when (status.phase) {
    OfflinePhase.Ready -> stringResource(R.string.action_downloaded)
    OfflinePhase.Failed -> stringResource(R.string.download_failed)
    OfflinePhase.Preparing -> stringResource(R.string.download_preparing)
    OfflinePhase.Downloading -> stringResource(R.string.download_running)
    OfflinePhase.Paused -> stringResource(R.string.download_paused)
}

internal fun OfflinePhase.inProgress(): Boolean =
    this == OfflinePhase.Preparing || this == OfflinePhase.Downloading || this == OfflinePhase.Paused

internal fun OfflinePhase.canPause(): Boolean =
    this == OfflinePhase.Preparing || this == OfflinePhase.Downloading

/**
 * Размер + синий лоадер, % у правого края, бар вплотную,
 * затем Пауза/Продолжить и Удалить на всю ширину (опционально).
 */
@Composable
internal fun DownloadProgressControls(
    status: OfflineStatus,
    caption: String,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDelete: Boolean = true,
    showActions: Boolean = true,
) {
    val progress = status.phase.inProgress()
    val spinning = status.phase == OfflinePhase.Downloading || status.phase == OfflinePhase.Preparing
    Column(modifier.fillMaxWidth()) {
        if (progress) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = caption,
                    color = CinemaMuted,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (spinning) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(
                        color = PlayBlue,
                        strokeWidth = 1.6.dp,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!status.indeterminate && status.percent > 0) {
                    Text(
                        text = "${status.percent}%",
                        color = CinemaMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            DownloadBar(status = status, showPercent = false, height = 3.dp)
        }
        if (showActions && (status.phase.canPause() || status.phase == OfflinePhase.Paused)) {
            Spacer(Modifier.height(14.dp))
            DownloadWideButton(
                text = stringResource(
                    if (status.phase == OfflinePhase.Paused) {
                        R.string.download_resume
                    } else {
                        R.string.download_pause
                    },
                ),
                emphasized = true,
                onClick = {
                    if (status.phase == OfflinePhase.Paused) onResume() else onPause()
                },
            )
        }
        if (showActions && showDelete) {
            Spacer(Modifier.height(8.dp))
            DownloadWideButton(
                text = stringResource(R.string.download_delete),
                emphasized = false,
                onClick = onDelete,
            )
        }
    }
}

@Composable
internal fun DownloadWideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val tick = rememberHapticTick()
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (emphasized) 0.12f else 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (emphasized) Color.White else CinemaMuted,
            fontSize = 15.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

internal fun offlineSizeLabel(status: OfflineStatus): String? {
    val total = status.totalBytes
    val done = status.bytes
    return when {
        total > 0L && done > 0L -> "${formatOfflineBytes(done)} из ${formatOfflineBytes(total)}"
        total > 0L -> formatOfflineBytes(total)
        done > 0L -> formatOfflineBytes(done)
        else -> null
    }
}

internal fun formatOfflineBytes(bytes: Long): String {
    val value = bytes.coerceAtLeast(0).toDouble()
    val mb = value / (1024.0 * 1024.0)
    if (mb >= 10) return "${mb.roundToInt()} МБ"
    if (mb >= 0.1) {
        val tenths = (mb * 10).roundToInt()
        return "${tenths / 10},${abs(tenths % 10)} МБ"
    }
    val kb = value / 1024.0
    if (kb >= 10) return "${kb.roundToInt()} КБ"
    val tenths = (kb * 10).roundToInt()
    return "${tenths / 10},${abs(tenths % 10)} КБ"
}
