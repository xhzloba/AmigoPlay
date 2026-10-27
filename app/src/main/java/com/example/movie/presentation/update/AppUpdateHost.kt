package com.example.movie.presentation.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.BuildConfig
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.AppRelease
import com.example.movie.domain.model.DownloadProgress
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

private val CardShape = RoundedCornerShape(28.dp)
private val ButtonShape = RoundedCornerShape(22.dp)
private val VersionBlue = Color(0xFF7EC8FF)
private val NotesColor = Color(0xFFD2D2D8)
private val FailedColor = Color(0xFFE7B3B3)

private enum class UpdatePhase {
    Ready,
    Downloading,
    Failed,
}

@Composable
fun AppUpdateHost() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tick = rememberHapticTick()
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var release by remember { mutableStateOf<AppRelease?>(null) }
    var visible by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(UpdatePhase.Ready) }
    var progress by remember { mutableStateOf(DownloadProgress()) }
    var download by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        if (dismissed) return@LaunchedEffect
        val found = container.checkAppUpdate(BuildConfig.VERSION_CODE) ?: return@LaunchedEffect
        if (dismissed) return@LaunchedEffect
        release = found
        visible = true
    }

    fun hide() {
        download?.cancel()
        download = null
        dismissed = true
        visible = false
    }

    fun startUpdate() {
        val item = release ?: return
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
        tick?.invoke()
        download?.cancel()
        download = scope.launch {
            phase = UpdatePhase.Downloading
            progress = DownloadProgress()
            try {
                val file = container.apkInstaller.download(
                    url = item.apkUrl,
                    destination = File(context.cacheDir, "updates/update.apk"),
                    onProgress = { progress = it },
                )
                container.apkInstaller.install(context, file)
                hide()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                phase = UpdatePhase.Failed
            }
        }
    }

    BackHandler(enabled = visible) { hide() }

    val shown = release
    AnimatedVisibility(
        visible = visible && shown != null,
        enter = fadeIn(tween(180)) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { it },
        ),
        exit = fadeOut(tween(140)) + slideOutVertically(
            animationSpec = tween(180),
            targetOffsetY = { it },
        ),
    ) {
        if (shown != null) {
            UpdateSheet(
                release = shown,
                phase = phase,
                progress = progress,
                onLater = ::hide,
                onUpdate = ::startUpdate,
            )
        }
    }
}

@Composable
private fun UpdateSheet(
    release: AppRelease,
    phase: UpdatePhase,
    progress: DownloadProgress,
    onLater: () -> Unit,
    onUpdate: () -> Unit,
) {
    val animated by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(120),
        label = "updateProgress",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
            if (release.notes.isNotBlank()) {
                Text(
                    text = dashedNotes(release.notes),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .heightIn(max = 168.dp)
                        .verticalScroll(rememberScrollState()),
                    color = NotesColor,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
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
