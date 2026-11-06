package com.example.movie.presentation.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.domain.model.Franchise
import com.example.movie.domain.model.FranchiseTrailer
import com.example.movie.domain.model.MovieDetail
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaBlack
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Label = Color(0xFF8E8E93)
private val Value = Color(0xFFE8E8ED)
private val Card = Color.White.copy(alpha = 0.06f)
private val Panel = Color.White.copy(alpha = 0.16f)
private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

@Composable
internal fun FranchiseBlock(detail: MovieDetail, onPlayTrailer: (FranchiseTrailer) -> Unit) {
    val franchise = detail.franchise ?: return
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            text = stringResource(if (franchise.series) R.string.franchise_about_series else R.string.franchise_about),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        val status = franchise.serialStatus
        if (franchise.series && !status.isNullOrBlank()) {
            val finished = status.equals("offline", ignoreCase = true)
            InfoLine(
                label = stringResource(R.string.franchise_status),
                value = stringResource(if (finished) R.string.franchise_finished else R.string.franchise_airing),
                valueColor = if (finished) Color(0xFFFF453A) else Color(0xFF32D74B),
            )
        }
        if (franchise.series && franchise.seasons.isNotEmpty()) {
            InfoLine(
                label = stringResource(R.string.franchise_seasons),
                value = stringResource(seasonCountRes(franchise.seasons.size), franchise.seasons.size),
            )
        }
        InfoLine(stringResource(R.string.franchise_director), franchise.directors.orDash(detail.director))
        InfoLine(stringResource(R.string.franchise_writers), franchise.screenwriters.orDash())
        InfoLine(stringResource(R.string.franchise_producers), franchise.producers.orDash())
        InfoLine(stringResource(R.string.franchise_operator), franchise.operators.orDash())
        InfoLine(stringResource(R.string.franchise_editor), franchise.editors.orDash())
        InfoLine(stringResource(R.string.franchise_design), franchise.designers.orDash())
        InfoLine(stringResource(R.string.franchise_genres), franchise.genres.orDash(detail.genres.joinToString(", ")))
        InfoLine(stringResource(R.string.franchise_country), franchise.countries.orDash(detail.country))
        InfoLine(stringResource(R.string.franchise_premiere), franchise.premiere.asDate())
        franchise.slogan?.let { InfoLine(stringResource(R.string.franchise_slogan), "«$it»") }
        franchise.premiereRu?.let { InfoLine(stringResource(R.string.franchise_premiere_ru), it.asDate()) }
        franchise.mpaa?.let { InfoLine(stringResource(R.string.franchise_mpaa), it) }
        franchise.budget?.let { InfoLine(stringResource(R.string.franchise_budget), it) }
        franchise.feesWorld?.let { InfoLine(stringResource(R.string.franchise_fees_world), it) }
        franchise.feesRus?.let { InfoLine(stringResource(R.string.franchise_fees_rus), it) }
        if (franchise.dubActors.isNotEmpty()) {
            InfoLine(stringResource(R.string.franchise_dub), franchise.dubActors.joinToString(", "))
        }
        if (franchise.voiceActing.isNotEmpty()) {
            InfoLine(stringResource(R.string.franchise_voice), franchise.voiceActing.joinToString(", "))
        }
        if (franchise.photos.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            PhotoStrip(franchise.photos)
        }
        if (franchise.trailers.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.franchise_trailers),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            TrailerRow(franchise.trailers, onPlayTrailer)
        }
        if (franchise.facts.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Facts(franchise.facts)
        }
        if (franchise.seasons.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Seasons(franchise)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = Value) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = Label,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.width(132.dp),
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PhotoStrip(photos: List<String>) {
    LazyRow(
        contentPadding = PaddingValues(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(photos, key = { it }) { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 148.dp, height = 96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Card),
            )
        }
    }
}

@Composable
internal fun TrailerRow(trailers: List<FranchiseTrailer>, onPlay: (FranchiseTrailer) -> Unit) {
    val tick = rememberHapticTick()
    val row = rememberLazyListState()
    HapticOnChange(row) { row.firstVisibleItemIndex }
    LazyRow(
        state = row,
        modifier = Modifier
            .edgeToEdge(DetailInset)
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DetailInset),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(trailers.size, key = { trailers[it].url }) { index ->
            val trailer = trailers[index]
            Column(Modifier.width(260.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Card)
                        .pointerInput(trailer.url) {
                            detectTapGestures(onTap = {
                                tick?.invoke()
                                onPlay(trailer)
                            })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!trailer.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = trailer.posterUrl,
                            contentDescription = trailer.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                val caption = trailer.season?.let { stringResource(R.string.franchise_trailer_season, it) }
                    ?: trailer.name
                if (caption.isNotBlank()) {
                    Text(
                        text = caption,
                        color = Color(0xFFC7C7CC),
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun TrailerPlayer(trailer: FranchiseTrailer, onDismiss: () -> Unit) {
    val host = remember { arrayOfNulls<TrailerHost>(1) }
    BackHandler {
        if (host[0]?.exitFullscreen() == true) return@BackHandler
        onDismiss()
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                TrailerHost(context, trailer.url).also { host[0] = it }
            },
            onRelease = {
                it.release()
                host[0] = null
            },
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(end = 12.dp, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.section_back),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private class TrailerHost(context: android.content.Context, url: String) : FrameLayout(context) {
    private val web = WebView(context)
    private var custom: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    init {
        setBackgroundColor(android.graphics.Color.BLACK)
        web.setBackgroundColor(android.graphics.Color.BLACK)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.settings.useWideViewPort = true
        web.settings.loadWithOverviewMode = true
        web.settings.userAgentString = web.settings.userAgentString.replace("; wv", "")
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null) {
                    callback?.onCustomViewHidden()
                    return
                }
                hideFullscreen()
                (view.parent as? android.view.ViewGroup)?.removeView(view)
                custom = view
                customCallback = callback
                web.visibility = GONE
                addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            }

            override fun onHideCustomView() {
                hideFullscreen()
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val scheme = request.url.scheme.orEmpty()
                if (scheme == "about" || scheme == "data" || scheme == "blob") return false
                val host = request.url.host.orEmpty()
                return !host.endsWith("nextembed.ws")
            }
        }
        addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val page = trailerPage(url)
        web.loadDataWithBaseURL("https://api.nextembed.ws/", page, "text/html", "utf-8", null)
    }

    fun exitFullscreen(): Boolean {
        val callback = customCallback ?: return false
        callback.onCustomViewHidden()
        return true
    }

    fun hideFullscreen() {
        val view = custom ?: return
        removeView(view)
        custom = null
        customCallback = null
        web.visibility = VISIBLE
    }

    fun release() {
        hideFullscreen()
        removeView(web)
        web.destroy()
    }
}

private fun trailerPage(url: String): String {
    val safe = url.replace("&", "&amp;").replace("\"", "&quot;")
    return """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        <style>html,body{margin:0;height:100%;background:#000;overflow:hidden}iframe{position:fixed;inset:0;border:0;width:100%;height:100%}</style>
        </head><body>
        <iframe src="$safe" allow="autoplay; fullscreen; encrypted-media" allowfullscreen></iframe>
        </body></html>
    """.trimIndent()
}

@Composable
private fun Facts(facts: List<String>) {
    var selected by remember(facts) { mutableStateOf<String?>(null) }
    val tick = rememberHapticTick()
    val row = rememberLazyListState()
    HapticOnChange(row) { row.firstVisibleItemIndex }
    Text(
        text = stringResource(R.string.franchise_facts),
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(12.dp))
    LazyRow(
        state = row,
        modifier = Modifier
            .edgeToEdge(DetailInset)
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DetailInset),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(facts.size, key = { it }) { index ->
            val fact = facts[index]
            Text(
                text = fact,
                color = Color(0xFFC7C7CC),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(236.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Panel)
                    .pointerInput(fact) {
                        detectTapGestures(onTap = {
                            tick?.invoke()
                            selected = fact
                        })
                    }
                    .padding(14.dp),
            )
        }
    }
    selected?.let { fact ->
        FactPopup(fact = fact, onDismiss = { selected = null })
    }
}

@Composable
private fun FactPopup(fact: String, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(CinemaBlack)
                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    text = stringResource(R.string.franchise_facts),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = fact,
                    color = Color(0xFFD2D2D8),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun Seasons(franchise: Franchise) {
    val tick = rememberHapticTick()
    var opened by remember(franchise.seasons) { mutableStateOf(setOf<Int>()) }
    Text(
        text = stringResource(R.string.franchise_seasons),
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(12.dp))
    franchise.seasons.forEach { season ->
        val expanded = season.number in opened
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Panel),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            opened = if (expanded) opened - season.number else opened + season.number
                        },
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!season.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = season.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 36.dp, height = 52.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.franchise_season, season.number),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (season.episodes.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.franchise_episodes, season.episodes.size),
                            color = Label,
                            fontSize = 13.sp,
                        )
                    }
                }
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = tween(220),
                    label = "seasonArrow",
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { rotationZ = rotation },
                )
            }
            if (expanded) {
                season.episodes.forEach { episode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = episode.number.toString(),
                            color = Label,
                            fontSize = 14.sp,
                            modifier = Modifier.width(28.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = episode.name ?: stringResource(R.string.franchise_episode, episode.number),
                                color = Value,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            episode.release?.let { release ->
                                Text(
                                    text = release.asDate(),
                                    color = Label,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun seasonCountRes(count: Int): Int {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> R.string.franchise_season_many
        mod10 == 1 -> R.string.franchise_season_one
        mod10 in 2..4 -> R.string.franchise_season_few
        else -> R.string.franchise_season_many
    }
}

private fun List<String>.orDash(fallback: String = ""): String =
    joinToString(", ").ifBlank { fallback.trim() }.ifBlank { "—" }

private fun String?.asDate(): String {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return "—"
    val parsed = runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
    return parsed?.format(dateFormat) ?: raw
}
