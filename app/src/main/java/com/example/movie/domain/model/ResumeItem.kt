package com.example.movie.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ResumeItem(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val year: Int? = null,
    val type: String = "movie",
    val genres: String = "",
    val logoUrl: String? = null,
    val franchiseId: Int,
    val season: Int? = null,
    val episode: Int? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val fraction: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

internal fun ResumeItem.toSaved(): SavedTitle = SavedTitle(
    id = id,
    title = title,
    posterUrl = posterUrl,
    year = year,
    type = type,
    genres = genres,
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
