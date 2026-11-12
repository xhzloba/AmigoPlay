package com.example.movie.presentation.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.presentation.common.PosterImage
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.PosterCaption
import com.example.movie.presentation.home.components.PosterCaptionSkeleton
import com.example.movie.presentation.home.components.FavoriteBadge
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.WatchedBadge
import com.example.movie.presentation.home.components.WatchedBadgeHang
import com.example.movie.presentation.home.components.posterGridColumns
import com.example.movie.presentation.home.components.posterTitlesOn
import com.example.movie.presentation.home.components.rememberFavoriteIds
import com.example.movie.presentation.home.components.rememberWatchedIds
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.presentation.common.appBackground
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PlayBlue
import com.example.movie.ui.theme.PosterShape

private val FieldShape = RoundedCornerShape(26.dp)
private val FieldFill = Color(0xFF282828)
private val FieldStrokeWidth = 0.8.dp
private val FieldStroke = Brush.verticalGradient(
    0f to Color(0xFF57545B),
    0.5f to Color(0xFF3C3940),
    1f to Color(0xFF454249),
)
private val ChipFill = Color(0xFF2A2A2C)
private val ChipShape = RoundedCornerShape(percent = 50)
private val SearchInset = 20.dp

@Composable
internal fun SearchPage(
    query: String,
    results: List<Movie>,
    searching: Boolean,
    error: DataError?,
    recent: List<String>,
    popular: List<Movie> = emptyList(),
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    onClearRecent: () -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val tick = rememberHapticTick()
    val voicePrompt = stringResource(R.string.search_voice)
    var listening by remember { mutableStateOf(false) }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        listening = false
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spoken.isNotEmpty()) onQuery(spoken)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(onBack = onClose)
    Column(
        modifier = modifier
            .fillMaxSize()
            .appBackground()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
            ),
            cursorBrush = SolidColor(PlayBlue),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SearchInset)
                .padding(top = 6.dp)
                .height(50.dp)
                .border(FieldStrokeWidth, FieldStroke, FieldShape)
                .clip(FieldShape)
                .background(FieldFill)
                .focusRequester(focus),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = ShellIcons.search,
                        contentDescription = null,
                        tint = CinemaMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                color = CinemaMuted,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = ShellIcons.close,
                            contentDescription = stringResource(R.string.search_clear_query),
                            tint = CinemaMuted,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        tick?.invoke()
                                        onQuery("")
                                    },
                                )
                                .padding(7.dp),
                        )
                    }
                    Icon(
                        imageVector = ShellIcons.mic,
                        contentDescription = voicePrompt,
                        tint = if (listening) PlayBlue else CinemaMuted,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    tick?.invoke()
                                    keyboard?.hide()
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                        )
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, voicePrompt)
                                    }
                                    listening = true
                                    runCatching { voice.launch(intent) }
                                        .onFailure { listening = false }
                                },
                            )
                            .padding(7.dp),
                    )
                }
            },
        )
        Spacer(Modifier.height(28.dp))
        when {
            query.isNotBlank() && error != null -> SearchMessage(error.message())
            query.isNotBlank() && searching && results.isEmpty() -> SearchSkeleton()
            query.isNotBlank() && query.trim().length >= 2 && results.isEmpty() && !searching -> {
                SearchMessage(stringResource(R.string.catalog_empty))
            }
            query.isNotBlank() -> SearchResults(
                movies = results,
                onMovieClick = onMovieClick,
                onMoviePeek = onMoviePeek,
            )
            else -> SearchIdle(
                recent = recent,
                popular = popular,
                onClearRecent = onClearRecent,
                onPickRecent = onQuery,
                onMovieClick = onMovieClick,
                onMoviePeek = onMoviePeek,
            )
        }
    }
}

@Composable
private fun SearchIdle(
    recent: List<String>,
    popular: List<Movie>,
    onClearRecent: () -> Unit,
    onPickRecent: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    val tick = rememberHapticTick()
    val showTitles = posterTitlesOn()
    val watched = rememberWatchedIds()
    val favorites = rememberFavoriteIds()
    LazyVerticalGrid(
        columns = GridCells.Fixed(posterGridColumns()),
        contentPadding = PaddingValues(
            start = PosterGridInset,
            end = PosterGridInset,
            bottom = 28.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (recent.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "recent-header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.search_recent),
                        color = Color.White,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.search_clear),
                        color = PlayBlue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                onClearRecent()
                            },
                        ),
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "recent-chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 12.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recent.forEach { query ->
                        Text(
                            text = query,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(ChipShape)
                                .background(ChipFill)
                                .clickable(
                                    interactionSource = remember(query) { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        tick?.invoke()
                                        onPickRecent(query)
                                    },
                                )
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                        )
                    }
                }
            }
        }
        if (popular.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "popular-header") {
                Text(
                    text = stringResource(R.string.search_popular),
                    color = Color.White,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (recent.isEmpty()) 0.dp else 4.dp, bottom = 6.dp),
                )
            }
            items(popular, key = { it.id }) { movie ->
                var holdProgress by remember(movie.id) { mutableFloatStateOf(0f) }
                var pressed by remember(movie.id) { mutableStateOf(false) }
                val pressScale = rememberPressScale(pressed)
                val wave = remember(movie.id) { PosterWave() }
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / 1.46f)
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            },
                    ) {
                        PosterImage(
                            url = movie.posterUrl ?: movie.backdropUrl,
                            contentDescription = stringResource(
                                R.string.poster_content_description,
                                movie.title,
                            ),
                            modifier = Modifier
                                .matchParentSize()
                                .clip(PosterShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .posterWave(wave)
                                .holdToPeek(
                                    onPeek = { onMoviePeek(movie) },
                                    onProgress = { holdProgress = it },
                                    onPressed = { pressed = it },
                                    onClick = {
                                        tick?.invoke()
                                        onMovieClick(movie.id)
                                    },
                                    wave = wave,
                                )
                                .holdCue(holdProgress),
                        )
                        if (movie.id in watched) {
                            WatchedBadge(Modifier.align(Alignment.BottomEnd))
                        } else if (movie.id in favorites) {
                            FavoriteBadge(Modifier.align(Alignment.BottomEnd))
                        }
                    }
                    Spacer(Modifier.height(WatchedBadgeHang))
                    if (showTitles) PosterCaption(movie.title, movie.year)
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    val tick = rememberHapticTick()
    val showTitles = posterTitlesOn()
    val watched = rememberWatchedIds()
    val favorites = rememberFavoriteIds()
    LazyVerticalGrid(
        columns = GridCells.Fixed(posterGridColumns()),
        contentPadding = PaddingValues(
            start = PosterGridInset,
            end = PosterGridInset,
            bottom = 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(movies, key = { it.id }) { movie ->
            var holdProgress by remember(movie.id) { mutableFloatStateOf(0f) }
            var pressed by remember(movie.id) { mutableStateOf(false) }
            val pressScale = rememberPressScale(pressed)
            val wave = remember(movie.id) { PosterWave() }
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / 1.46f)
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        },
                ) {
                    PosterImage(
                        url = movie.posterUrl ?: movie.backdropUrl,
                        contentDescription = stringResource(R.string.poster_content_description, movie.title),
                        modifier = Modifier
                            .matchParentSize()
                            .clip(PosterShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .posterWave(wave)
                            .holdToPeek(
                                onPeek = { onMoviePeek(movie) },
                                onProgress = { holdProgress = it },
                                onPressed = { pressed = it },
                                onClick = {
                                    tick?.invoke()
                                    onMovieClick(movie.id)
                                },
                                wave = wave,
                            )
                            .holdCue(holdProgress),
                    )
                    if (movie.id in watched) {
                        WatchedBadge(Modifier.align(Alignment.BottomEnd))
                    } else if (movie.id in favorites) {
                        FavoriteBadge(Modifier.align(Alignment.BottomEnd))
                    }
                }
                Spacer(Modifier.height(WatchedBadgeHang))
                if (showTitles) PosterCaption(movie.title, movie.year)
            }
        }
    }
}

@Composable
private fun SearchSkeleton() {
    val showTitles = posterTitlesOn()
    LazyVerticalGrid(
        columns = GridCells.Fixed(posterGridColumns()),
        contentPadding = PaddingValues(horizontal = PosterGridInset),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(9) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / 1.46f)
                        .clip(PosterShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
                Spacer(Modifier.height(WatchedBadgeHang))
                if (showTitles) PosterCaptionSkeleton()
            }
        }
    }
}

@Composable
private fun SearchMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = CinemaMuted,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun DataError.message(): String = when (this) {
    is DataError.Network -> stringResource(R.string.popular_network_error)
    is DataError.Http -> stringResource(R.string.popular_http_error, code)
    is DataError.Parsing -> stringResource(R.string.popular_parse_error)
    is DataError.Unknown -> stringResource(R.string.popular_load_error)
}
