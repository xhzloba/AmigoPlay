package com.example.movie.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.FranchiseSeason
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.airedDone
import com.example.movie.domain.model.combineMarks
import com.example.movie.domain.model.episodeMarks
import com.example.movie.domain.model.toSaved
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaBlack
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PlayBlue
import kotlin.math.abs

private val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

private val HoldSheetScroll = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        Offset(0f, available.y)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        Velocity(0f, available.y)
}

private fun Modifier.holdSheet(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull() ?: break
            if (!change.pressed) break
            val delta = change.positionChange()
            if (!change.isConsumed && abs(delta.y) > abs(delta.x)) {
                change.consume()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SeasonWatchSheet(
    movie: Movie,
    knownSeasons: List<FranchiseSeason> = emptyList(),
    onDismiss: () -> Unit,
) {
    val container = LocalAppContainer.current
    val resumeStore = container.resume
    val watched = container.watched
    val resume by resumeStore.items.collectAsStateWithLifecycle()
    val kept by resumeStore.kept.collectAsStateWithLifecycle()
    val tick = rememberHapticTick()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var attempt by remember { mutableIntStateOf(0) }
    var seasons by remember { mutableStateOf(knownSeasons.takeIf { it.isNotEmpty() }) }
    var failed by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var seeded by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    LaunchedEffect(movie.id, attempt) {
        if (attempt == 0 && !seasons.isNullOrEmpty()) return@LaunchedEffect
        failed = false
        val loaded = container.loadFranchise(movie.id)?.seasons.orEmpty()
        if (loaded.isEmpty()) {
            seasons = null
            failed = true
        } else {
            seasons = loaded
            failed = false
        }
    }
    val loaded = seasons
    LaunchedEffect(loaded) {
        if (loaded == null || seeded) return@LaunchedEffect
        val marks = combineMarks(
            resume.firstOrNull { it.id == movie.id }?.episodeMarks().orEmpty(),
            kept[movie.id].orEmpty(),
        )
        picked = loaded.filter { it.airedDone(marks) }.map { it.number }.toSet()
        seeded = true
    }
    val numbers = loaded?.map { it.number }.orEmpty()
    val all = numbers.isNotEmpty() && numbers.all { it in picked }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CinemaBlack,
        contentColor = Color.White,
        scrimColor = Color(0xFF3A3A3E).copy(alpha = 0.86f),
        dragHandle = null,
        shape = SheetShape,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 14.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
            )
            Text(
                text = stringResource(R.string.watch_seasons_title),
                modifier = Modifier.padding(horizontal = 22.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            when {
                loaded == null && !failed -> CircularProgressIndicator(
                    color = PlayBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(vertical = 28.dp).size(28.dp),
                )
                failed -> {
                    Text(
                        text = stringResource(R.string.watch_seasons_failed),
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        color = CinemaMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.watch_seasons_retry),
                        color = PlayBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    tick?.invoke()
                                    attempt += 1
                                },
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                loaded != null -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .nestedScroll(HoldSheetScroll)
                            .holdSheet()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                    ) {
                        SeasonPickRow(
                            title = stringResource(R.string.watch_seasons_all),
                            checked = all,
                            onClick = {
                                tick?.invoke()
                                picked = if (all) emptySet() else numbers.toSet()
                            },
                        )
                        loaded.forEach { season ->
                            SeasonPickRow(
                                title = stringResource(R.string.franchise_season, season.number),
                                checked = season.number in picked,
                                onClick = {
                                    tick?.invoke()
                                    picked = if (season.number in picked) {
                                        picked - season.number
                                    } else {
                                        picked + season.number
                                    }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (applying || picked.isEmpty()) return@Button
                            tick?.invoke()
                            applying = true
                            val finished = resumeStore.watchSeasons(movie.id, loaded, picked)
                            if (finished) watched.add(movie.toSaved()) else watched.remove(movie.id)
                            onDismiss()
                        },
                        enabled = picked.isNotEmpty() && !applying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.White.copy(alpha = 0.18f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f),
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .padding(horizontal = 22.dp)
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.watch_seasons_done),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = stringResource(R.string.watch_seasons_reset),
                        color = CinemaMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !applying,
                                onClick = {
                                    if (applying) return@clickable
                                    tick?.invoke()
                                    applying = true
                                    resumeStore.resetProgress(movie.id)
                                    watched.remove(movie.id)
                                    onDismiss()
                                },
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonPickRow(title: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickMark(checked)
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun PickMark(checked: Boolean) {
    Canvas(Modifier.size(22.dp).clip(CircleShape)) {
        if (checked) {
            drawCircle(PlayBlue)
            val stroke = 2.dp.toPx()
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.28f, size.height * 0.52f),
                end = Offset(size.width * 0.44f, size.height * 0.68f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.44f, size.height * 0.68f),
                end = Offset(size.width * 0.74f, size.height * 0.34f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        } else {
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx()),
            )
        }
    }
}
