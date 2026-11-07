package com.example.movie.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class ResumeItem(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val year: Int? = null,
    val type: String = "movie",
    val genres: String = "",
    val overview: String = "",
    val ratingKp: Float? = null,
    val ratingImdb: Float? = null,
    val logoUrl: String? = null,
    val franchiseId: Int,
    val season: Int? = null,
    val episode: Int? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = 0L,
    val marks: List<EpisodeProgress> = emptyList(),
) {
    val fraction: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

@Serializable
data class EpisodeProgress(
    val season: Int,
    val episode: Int,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val completed: Boolean = false,
) {
    val fraction: Float
        get() = when {
            completed -> 1f
            durationMs <= 0L -> 0f
            else -> (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
}

internal fun episodeFinished(positionMs: Long, durationMs: Long): Boolean =
    durationMs > 0L && (durationMs - positionMs < 20_000L || positionMs >= durationMs * 95 / 100)

internal fun String?.isUnreleased(today: LocalDate = LocalDate.now()): Boolean {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return false
    val date = runCatching { LocalDate.parse(raw.take(10)) }.getOrNull() ?: return false
    return date.isAfter(today)
}

internal fun List<FranchiseSeason>.fullyWatched(marks: List<EpisodeProgress>): Boolean {
    val aired = flatMap { season ->
        season.episodes.filterNot { it.release.isUnreleased() }.map { season.number to it.number }
    }
    if (aired.isEmpty()) return false
    return aired.all { (season, episode) ->
        marks.any { it.completed && it.season == season && it.episode == episode }
    }
}

internal fun ResumeItem.episodeMarks(): List<EpisodeProgress> {
    val season = season ?: return marks
    val episode = episode ?: return marks
    if (marks.any { it.season == season && it.episode == episode }) return marks
    return marks + EpisodeProgress(
        season = season,
        episode = episode,
        positionMs = positionMs,
        durationMs = durationMs,
        completed = episodeFinished(positionMs, durationMs),
    )
}

internal fun ResumeItem.toSaved(): SavedTitle = SavedTitle(
    id = id,
    title = title,
    posterUrl = posterUrl,
    year = year,
    type = type,
    genres = genres,
    overview = overview,
    ratingKp = ratingKp,
    ratingImdb = ratingImdb,
    logoUrl = logoUrl,
)

internal fun ResumeItem.toMovie(): Movie = toSaved().toMovie()

internal fun ResumeItem.progressCaption(): String {
    val clock = resumeClock(positionMs)
    return if (season != null && episode != null) "$season×$episode · $clock" else clock
}

internal fun MovieDetail.toResume(
    franchiseId: Int,
    positionMs: Long,
    durationMs: Long,
    season: Int?,
    episode: Int?,
): ResumeItem {
    val saved = toSaved()
    val series = franchise?.series == true && saved.type == "movie"
    return ResumeItem(
        id = saved.id,
        title = saved.title,
        posterUrl = saved.posterUrl,
        year = saved.year,
        type = if (series) "serial" else saved.type,
        genres = saved.genres,
        overview = saved.overview,
        ratingKp = saved.ratingKp,
        ratingImdb = saved.ratingImdb,
        logoUrl = saved.logoUrl,
        franchiseId = franchiseId,
        season = season,
        episode = episode,
        positionMs = positionMs,
        durationMs = durationMs,
    )
}

internal fun resumeClock(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
