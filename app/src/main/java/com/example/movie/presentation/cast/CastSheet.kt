package com.example.movie.presentation.cast

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.data.cast.TvCastHub
import com.example.movie.presentation.common.HandleSheet
import com.example.movie.ui.theme.PlayBlue

@Composable
internal fun CastSheet(
    title: String,
    pageUrl: String?,
    resolving: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val state by TvCastHub.state.collectAsStateWithLifecycle()
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(pageUrl, title) {
        if (!pageUrl.isNullOrBlank()) TvCastService.prepare(context, pageUrl, title)
    }
    HandleSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 18.dp),
        ) {
            Text(
                text = stringResource(R.string.cast_title),
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            val message = when {
                pageUrl.isNullOrBlank() && resolving -> stringResource(R.string.cast_preparing)
                pageUrl.isNullOrBlank() -> stringResource(R.string.cast_no_stream)
                state.message.isNotBlank() -> state.message
                state.running || state.busy -> stringResource(R.string.cast_preparing)
                else -> stringResource(R.string.cast_idle)
            }
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
            if (!pageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                if (state.running) {
                    CastRow(
                        text = stringResource(R.string.cast_find),
                        enabled = !state.busy,
                        filled = true,
                        onClick = { TvCastService.search(context) },
                    )
                } else {
                    CastRow(
                        text = stringResource(R.string.cast_start),
                        enabled = !state.busy,
                        filled = true,
                        onClick = { TvCastService.prepare(context, pageUrl, title) },
                    )
                }
                state.devices.forEach { device ->
                    Spacer(Modifier.height(8.dp))
                    CastRow(
                        text = device.name,
                        enabled = !state.busy,
                        onClick = { TvCastService.play(context, device.name, device.controlUrl) },
                    )
                }
                val browser = state.browserUrl
                if (!browser.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.cast_browser),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    CastRow(
                        text = browser,
                        enabled = true,
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(ClipData.newPlainText("tv", browser))
                            copied = true
                        },
                    )
                    if (copied) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.cast_copied),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 13.sp,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.cast_hint),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                if (state.running) {
                    Spacer(Modifier.height(14.dp))
                    CastRow(
                        text = stringResource(R.string.cast_stop),
                        enabled = true,
                        onClick = {
                            copied = false
                            TvCastService.stop(context)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CastRow(
    text: String,
    enabled: Boolean,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) PlayBlue else Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        color = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
        fontSize = 16.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
