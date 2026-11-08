package com.example.movie.presentation.alerts

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.FeedKind
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.CinemaMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val MenuScrim = Color(0xFF3A3A3E).copy(alpha = 0.86f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedSheet(onDismiss: () -> Unit) {
    val container = LocalAppContainer.current
    val store = container.releaseWatch.store
    val feed by store.feed.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val tick = rememberHapticTick()
    fun refresh() {
        scope.launch(Dispatchers.IO) { container.releaseWatch.refresh() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CinemaBlack,
        contentColor = Color.White,
        scrimColor = MenuScrim,
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
                .padding(bottom = 12.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 14.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
            )
            Text(
                text = stringResource(R.string.alerts_feed_title),
                modifier = Modifier.padding(horizontal = 20.dp),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Text(
                text = stringResource(R.string.alerts_feed_interval),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .horizontalScroll(rememberScrollState()),
            ) {
                FeedKind.intervals.forEach { hours ->
                    val selected = feed.intervalHours == hours
                    Text(
                        text = stringResource(R.string.alerts_feed_hour, hours),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Color.White else Color(0xFF2C2C30))
                            .clickable(
                                interactionSource = remember(hours) { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (selected) return@clickable
                                    tick?.invoke()
                                    store.setInterval(hours)
                                    refresh()
                                },
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (selected) Color.Black else Color.White,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            FeedKind.all.forEach { kind ->
                val on = feed.enabled(kind)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember(kind) { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                if (!on) context.askNotify()
                                store.setKind(kind, !on)
                                refresh()
                            },
                        )
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(categoryTitle(kind)),
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    )
                    FeedSwitch(on)
                }
            }
            Text(
                text = stringResource(R.string.alerts_feed_hint),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

internal fun categoryTitle(kind: String): Int = when (kind) {
    FeedKind.SeriesUpdates -> R.string.alerts_cat_series_updates
    FeedKind.MovieUpdates -> R.string.alerts_cat_movie_updates
    FeedKind.SeriesFresh -> R.string.alerts_cat_series_fresh
    FeedKind.MovieFresh -> R.string.alerts_cat_movie_fresh
    else -> R.string.alerts_feed_title
}

@Composable
private fun FeedSwitch(checked: Boolean) {
    val travel by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "switch",
    )
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color.White else Color(0xFF2C2C30))
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = travel)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color(0xFF8A8A90)),
        )
    }
}

private fun Context.askNotify() {
    val activity = findActivity() ?: return
    if (
        ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        49,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
