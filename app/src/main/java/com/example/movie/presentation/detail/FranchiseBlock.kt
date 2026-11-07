package com.example.movie.presentation.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.movie.presentation.home.SectionScrim
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.data.player.WatchPlayback
import com.example.movie.data.player.fetchLiftStream
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaBlack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import android.net.Uri
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
private val AboutLabel = Color(0xFFD4D4DA)
private val AboutValue = Color(0xFFE8E8ED)
private val Value = Color(0xFFE8E8ED)
private val Card = Color.White.copy(alpha = 0.06f)
private val Panel = Color.White.copy(alpha = 0.16f)
private val Plaque = Color.Black.copy(alpha = 0.32f)
private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

@Composable
internal fun FranchiseBlock(
    detail: MovieDetail,
    onPlayTrailer: (FranchiseTrailer) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenFacts: () -> Unit,
) {
    val franchise = detail.franchise ?: return
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        OpenTitle(
            title = stringResource(if (franchise.series) R.string.franchise_about_series else R.string.franchise_about),
            onOpen = onOpenAbout,
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
            Facts(franchise.facts, onOpenFacts)
        }
        if (franchise.seasons.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Seasons(franchise)
        }
        if (franchise.kinopoiskId != null || !franchise.imdbId.isNullOrBlank()) {
            Spacer(Modifier.height(22.dp))
            ExternalLinks(franchise)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = AboutValue) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = AboutLabel,
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
        items(photos.size, key = { it }) { index ->
            val url = photos[index]
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
        items(trailers.size, key = { it }) { index ->
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

internal fun liftWatchUrl(id: Int): String =
    "https://embed.liftw.ws/embed/movie/$id?host=lift.com&imp2=a7579fe91a3b6593c0b2f4bdb69fae4e6e539ff0a39a988f90a280e97fd43718&t2=1794007180&ht=5f79777865aa38c62bbc232817687bfdd3003a8e2dabebba331258f181643072&imp3=9125fe15854f815949c15971ca44fd92cf9abddf40a233ebcc375e358119b86a"

internal fun resumeWatchUrl(franchiseId: Int, season: Int?, episode: Int?): String {
    val base = liftWatchUrl(franchiseId)
    if (season == null || episode == null) return base
    return "$base&season=$season&episode=$episode"
}

@Composable
internal fun WatchPlayer(
    url: String,
    title: String,
    logoUrl: String?,
    startPositionMs: Long = 0L,
    onProgress: (Long, Long, Int?, Int?) -> Unit = { _, _, _, _ -> },
    onDismiss: () -> Unit,
) {
    var playback by remember(url) { mutableStateOf<WatchPlayback?>(null) }
    var embed by remember(url) { mutableStateOf(false) }
    val start = remember(url) { startPositionMs }
    LaunchedEffect(url) {
        val resolved = withContext(Dispatchers.IO) {
            runCatching { fetchLiftStream(url) }.getOrNull()
        }
        if (resolved == null) embed = true else playback = resolved
    }
    val direct = playback
    when {
        embed -> EmbedPlayer(url = url, direct = true, allowedHostSuffix = null, onDismiss = onDismiss)
        direct != null -> LiftExoPlayer(
            playback = direct,
            title = title,
            logoUrl = logoUrl,
            startPositionMs = start,
            onProgress = onProgress,
            onDismiss = onDismiss,
        )
        else -> Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
internal fun TrailerPlayer(trailer: FranchiseTrailer, onDismiss: () -> Unit) {
    EmbedPlayer(url = trailer.url, direct = false, allowedHostSuffix = "nextembed.ws", onDismiss = onDismiss)
}

@Composable
private fun EmbedPlayer(
    url: String,
    direct: Boolean,
    allowedHostSuffix: String?,
    onDismiss: () -> Unit,
) {
    val host = remember { arrayOfNulls<TrailerHost>(1) }
    BackHandler {
        if (host[0]?.exitFullscreen() == true) return@BackHandler
        onDismiss()
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                TrailerHost(context, url, direct, allowedHostSuffix).also { host[0] = it }
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

private class TrailerHost(
    context: android.content.Context,
    url: String,
    direct: Boolean,
    allowedHostSuffix: String?,
) : FrameLayout(context) {
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
                if (scheme != "http" && scheme != "https") return true
                val suffix = allowedHostSuffix ?: return false
                val host = request.url.host.orEmpty()
                return !host.endsWith(suffix)
            }
        }
        addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        if (direct) {
            web.loadUrl(url)
        } else {
            val page = trailerPage(url)
            web.loadDataWithBaseURL("https://api.nextembed.ws/", page, "text/html", "utf-8", null)
        }
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
private fun Facts(facts: List<String>, onOpen: () -> Unit) {
    var selected by remember(facts) { mutableStateOf<String?>(null) }
    val tick = rememberHapticTick()
    val row = rememberLazyListState()
    HapticOnChange(row) { row.firstVisibleItemIndex }
    OpenTitle(title = stringResource(R.string.franchise_facts), onOpen = onOpen)
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
                    .clip(RoundedCornerShape(18.dp))
                    .background(Plaque)
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

@Composable
private fun ExternalLinks(franchise: Franchise) {
    val context = LocalContext.current
    val tick = rememberHapticTick()
    val kp = franchise.kinopoiskId
    val imdb = franchise.imdbId
    Column(Modifier.fillMaxWidth()) {
    Text(
        text = stringResource(R.string.franchise_links),
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (kp != null) {
            val path = if (franchise.series) "series" else "film"
            SiteBox(
                icon = R.drawable.ic_link_kinopoisk,
                label = stringResource(R.string.franchise_link_kp),
                onClick = {
                    tick?.invoke()
                    context.openLink("https://www.kinopoisk.ru/$path/$kp/")
                },
            )
        }
        if (!imdb.isNullOrBlank()) {
            SiteBox(
                icon = R.drawable.ic_link_imdb,
                label = stringResource(R.string.franchise_link_imdb),
                onClick = {
                    tick?.invoke()
                    context.openLink("https://www.imdb.com/title/tt$imdb/")
                },
            )
        }
    }
    }
}

@Composable
private fun SiteBox(
    icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    val open = rememberUpdatedState(onClick)
    Column(
        modifier = Modifier
            .width(112.dp)
            .height(108.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Plaque)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { open.value() })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun android.content.Context.openLink(url: String) {
    val view = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    val chooser = Intent.createChooser(view, null).apply {
        putExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, false)
    }
    runCatching { startActivity(chooser) }
}

@Composable
internal fun OpenTitle(title: String, onOpen: () -> Unit) {
    val tick = rememberHapticTick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                tick?.invoke()
                onOpen()
            },
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = ShellIcons.chevron,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun FranchiseAboutPage(detail: MovieDetail, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val franchise = detail.franchise
    val title = stringResource(
        if (franchise?.series == true) R.string.franchise_about_series else R.string.franchise_about,
    )
    val lines = aboutLines(detail)
    StackPage(title = title, onBack = onBack, modifier = modifier) {
        lines.forEach { line ->
            Text(
                text = line.label,
                color = Label,
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = line.value,
                color = line.color,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
internal fun FranchiseFactsPage(facts: List<String>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    StackPage(
        title = stringResource(R.string.franchise_facts),
        onBack = onBack,
        modifier = modifier,
    ) {
        facts.forEachIndexed { index, fact ->
            Text(
                text = fact,
                color = Value,
                fontSize = 16.sp,
                lineHeight = 23.sp,
            )
            if (index != facts.lastIndex) {
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.14f)),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
internal fun StackPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrolling: Boolean = true,
    headerScrim: Float = 0f,
    content: @Composable () -> Unit,
) {
    BackHandler(onBack = onBack)
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val scrim = if (scrolling) {
        with(density) { (scroll.value / 28.dp.toPx()).coerceIn(0f, 1f) }
    } else {
        headerScrim
    }
    Box(
        modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        if (scrolling) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                    .padding(top = BrandTopInset + 36.dp + 18.dp, bottom = 16.dp)
                    .padding(horizontal = 20.dp),
            ) {
                content()
            }
        } else {
            Box(Modifier.fillMaxSize()) { content() }
        }
        SectionScrim(progress = scrim, modifier = Modifier.align(Alignment.TopCenter))
        PageBar(title = title, onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun PageBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val tick = rememberHapticTick()
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = BrandTopInset)
            .height(36.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = PosterGridInset - 7.dp)
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onBack()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShellIcons.back,
                contentDescription = stringResource(R.string.section_back),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private data class StackLine(val label: String, val value: String, val color: Color = Value)

@Composable
private fun aboutLines(detail: MovieDetail): List<StackLine> {
    val franchise = detail.franchise ?: return emptyList()
    val lines = mutableListOf<StackLine>()
    fun add(label: String, value: String, color: Color = Value) {
        if (value.isBlank() || value == "—") return
        lines += StackLine(label, value, color)
    }
    val status = franchise.serialStatus
    if (franchise.series && !status.isNullOrBlank()) {
        val finished = status.equals("offline", ignoreCase = true)
        add(
            stringResource(R.string.franchise_status),
            stringResource(if (finished) R.string.franchise_finished else R.string.franchise_airing),
            if (finished) Color(0xFFFF453A) else Color(0xFF32D74B),
        )
    }
    if (franchise.series && franchise.seasons.isNotEmpty()) {
        add(
            stringResource(R.string.franchise_seasons),
            stringResource(seasonCountRes(franchise.seasons.size), franchise.seasons.size),
        )
    }
    add(stringResource(R.string.franchise_director), franchise.directors.orDash(detail.director))
    add(stringResource(R.string.franchise_writers), franchise.screenwriters.orDash())
    add(stringResource(R.string.franchise_producers), franchise.producers.orDash())
    add(stringResource(R.string.franchise_operator), franchise.operators.orDash())
    add(stringResource(R.string.franchise_editor), franchise.editors.orDash())
    add(stringResource(R.string.franchise_design), franchise.designers.orDash())
    add(stringResource(R.string.franchise_genres), franchise.genres.orDash(detail.genres.joinToString(", ")))
    add(stringResource(R.string.franchise_country), franchise.countries.orDash(detail.country))
    add(stringResource(R.string.franchise_premiere), franchise.premiere.asDate())
    franchise.slogan?.let { add(stringResource(R.string.franchise_slogan), "«$it»") }
    franchise.premiereRu?.let { add(stringResource(R.string.franchise_premiere_ru), it.asDate()) }
    franchise.mpaa?.let { add(stringResource(R.string.franchise_mpaa), it) }
    franchise.budget?.let { add(stringResource(R.string.franchise_budget), it) }
    franchise.feesWorld?.let { add(stringResource(R.string.franchise_fees_world), it) }
    franchise.feesRus?.let { add(stringResource(R.string.franchise_fees_rus), it) }
    if (franchise.dubActors.isNotEmpty()) {
        add(stringResource(R.string.franchise_dub), franchise.dubActors.joinToString(", "))
    }
    if (franchise.voiceActing.isNotEmpty()) {
        add(stringResource(R.string.franchise_voice), franchise.voiceActing.joinToString(", "))
    }
    return lines
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
