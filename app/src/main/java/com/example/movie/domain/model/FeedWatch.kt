package com.example.movie.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FeedWatch(
    val intervalHours: Int = 6,
    val movieUpdates: Boolean = false,
    val seriesUpdates: Boolean = false,
    val movieFresh: Boolean = false,
    val seriesFresh: Boolean = false,
    val seriesCheckedAt: Long = 0,
) {
    fun enabled(kind: String): Boolean = when (kind) {
        FeedKind.MovieUpdates -> movieUpdates
        FeedKind.SeriesUpdates -> seriesUpdates
        FeedKind.MovieFresh -> movieFresh
        FeedKind.SeriesFresh -> seriesFresh
        else -> false
    }

    fun with(kind: String, on: Boolean): FeedWatch = when (kind) {
        FeedKind.MovieUpdates -> copy(movieUpdates = on)
        FeedKind.SeriesUpdates -> copy(seriesUpdates = on)
        FeedKind.MovieFresh -> copy(movieFresh = on)
        FeedKind.SeriesFresh -> copy(seriesFresh = on)
        else -> this
    }

    val anyOn: Boolean
        get() = movieUpdates || seriesUpdates || movieFresh || seriesFresh
}

object FeedKind {
    const val SeriesUpdates = "serial-updatings"
    const val MovieUpdates = "movie-updatings"
    const val SeriesFresh = "serial-new"
    const val MovieFresh = "movie-new"
    val all = listOf(SeriesUpdates, MovieUpdates, SeriesFresh, MovieFresh)
    val intervals = listOf(1, 3, 6, 12, 24)

    fun sort(kind: String): String = if (kind.endsWith("new")) "new" else "updatings"

    fun type(kind: String): String = if (kind.startsWith("serial")) "serial" else "movie"
}

@Serializable
data class FeedCard(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val year: Int? = null,
    val type: String = "movie",
    val originalTitle: String = "",
    val ratingKp: Float? = null,
    val ratingImdb: Float? = null,
    val age: Int? = null,
)

@Serializable
data class FeedShelf(
    val kind: String,
    val cards: List<FeedCard> = emptyList(),
    val seen: List<String> = emptyList(),
    val at: Long = 0,
    val offAt: Long? = null,
    val read: Boolean = true,
    val polledAt: Long = 0,
)

const val FEED_HOLD_MS = 24 * 60 * 60 * 1000L

fun FeedShelf.kept(now: Long = System.currentTimeMillis()): Boolean {
    val off = offAt ?: return true
    return now - off < FEED_HOLD_MS
}

fun FeedShelf.pending(now: Long = System.currentTimeMillis()): Boolean =
    !read && cards.isNotEmpty() && kept(now)
