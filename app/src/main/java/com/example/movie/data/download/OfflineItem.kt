package com.example.movie.data.download

import com.example.movie.data.player.WatchEpisode
import com.example.movie.data.player.WatchKind
import com.example.movie.data.player.WatchPlayback
import com.example.movie.data.player.WatchStream
import com.example.movie.data.player.WatchSubtitle
import com.example.movie.domain.model.SavedTitle
import kotlinx.serialization.Serializable

@Serializable
data class OfflineCaption(val url: String, val name: String)

@Serializable
data class OfflineStream(
    val season: Int,
    val episode: Int,
    val url: String,
    val hls: Boolean,
    val start: Boolean = false,
    val height: Int = 0,
    val audioIndex: Int = 0,
    val audioNames: List<String> = emptyList(),
    val audioOrder: List<Int> = emptyList(),
    val subtitles: List<OfflineCaption> = emptyList(),
)

@Serializable
data class OfflineItem(
    val title: SavedTitle,
    val height: Int,
    val streams: List<OfflineStream>,
)

enum class OfflinePhase { Preparing, Downloading, Paused, Ready, Failed }

data class OfflineStatus(
    val percent: Int,
    val phase: OfflinePhase,
    val indeterminate: Boolean = false,
    val bytes: Long = 0L,
    val totalBytes: Long = -1L,
)

internal fun offlineItem(
    title: SavedTitle,
    playback: WatchPlayback,
    height: Int,
    picked: Set<Pair<Int, Int>>,
    audioIndex: Int,
): OfflineItem {
    val streams = if (playback.episodes.isEmpty()) {
        listOf(playback.stream.stored(0, 0, start = true, height = height, audioIndex = audioIndex))
    } else {
        val matched = playback.episodes.filter { (it.season to it.episode) in picked }
            .ifEmpty { playback.episodes.take(1) }
        matched.mapIndexed { index, item ->
            item.stream.stored(
                season = item.season,
                episode = item.episode,
                start = index == 0,
                height = height,
                audioIndex = audioIndex,
            )
        }
    }
    return OfflineItem(title, height, streams)
}

internal fun OfflineStream.downloadId(movieId: String): String =
    if (season == 0 && episode == 0) movieId else "$movieId#$season#$episode"

internal fun mergeStreams(
    fresh: List<OfflineStream>,
    previous: List<OfflineStream>,
    movieId: String,
): List<OfflineStream> {
    val ids = fresh.map { it.downloadId(movieId) }.toSet()
    val replaceCurrent = fresh.any { it.start }
    val kept = previous
        .filter { it.downloadId(movieId) !in ids }
        .map { if (replaceCurrent) it.copy(start = false) else it }
    return (fresh + kept).sortedWith(compareBy({ it.season }, { it.episode }))
}

internal fun OfflineItem.toPlayback(season: Int? = null, episode: Int? = null): WatchPlayback {
    val ordered = streams.sortedWith(compareBy({ it.season }, { it.episode }))
    val episodes = ordered.filter { it.season > 0 || it.episode > 0 }.map { it.toEpisode() }
    val picked = if (season != null && episode != null) {
        ordered.firstOrNull { it.season == season && it.episode == episode }
    } else {
        null
    }
    val current = picked?.toStream()
        ?: ordered.firstOrNull { it.start }?.toStream()
        ?: episodes.firstOrNull()?.stream
        ?: ordered.first().toStream()
    return WatchPlayback(current, episodes)
}

private fun WatchStream.stored(
    season: Int,
    episode: Int,
    start: Boolean,
    height: Int,
    audioIndex: Int,
) = OfflineStream(
    season = season,
    episode = episode,
    url = url,
    hls = kind == WatchKind.Hls,
    start = start,
    height = height,
    audioIndex = audioIndex,
    audioNames = audioNames,
    audioOrder = audioOrder,
    subtitles = subtitles.map { OfflineCaption(it.url, it.name) },
)

private fun OfflineStream.toEpisode() = WatchEpisode(season, episode, toStream())

private fun OfflineStream.toStream() = WatchStream(
    url = url,
    kind = if (hls) WatchKind.Hls else WatchKind.Dash,
    subtitles = subtitles.map { WatchSubtitle(it.url, it.name) },
    audioNames = audioNames,
    audioOrder = audioOrder,
)
