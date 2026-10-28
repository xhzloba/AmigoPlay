package com.example.movie.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedTitle(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val year: Int? = null,
    val kind: String = ContentKind.Movie.name,
    val type: String = "movie",
    val originalTitle: String = "",
    val overview: String = "",
    val genres: String = "",
    val country: String = "",
    val director: String = "",
    val ratingKp: Float? = null,
    val ratingImdb: Float? = null,
    val age: Int? = null,
)

internal fun Movie.toSaved(): SavedTitle = SavedTitle(
    id = id,
    title = title,
    posterUrl = posterUrl,
    year = year,
    kind = kind.name,
    type = type.favoriteType(kind == ContentKind.Series),
    originalTitle = originalTitle,
    overview = overview,
    genres = genres,
    country = country,
    director = director,
    ratingKp = ratingKp,
    ratingImdb = ratingImdb,
    age = age,
)

internal fun MovieDetail.toSaved(): SavedTitle = SavedTitle(
    id = id,
    title = title,
    posterUrl = posterUrl,
    year = year,
    type = type.favoriteType(),
    overview = overview,
    genres = genres.joinToString(", "),
    ratingKp = ratingKp,
    ratingImdb = ratingImdb,
    age = age,
)

internal fun SavedTitle.toMovie(): Movie = Movie(
    id = id,
    title = title,
    originalTitle = originalTitle,
    year = year,
    overview = overview,
    genres = genres,
    country = country,
    director = director,
    posterUrl = posterUrl,
    backdropUrl = null,
    logoUrl = null,
    ratingKp = ratingKp,
    ratingImdb = ratingImdb,
    age = age,
    kind = when (type) {
        "serial" -> ContentKind.Series
        "anime" -> ContentKind.Anime
        else -> ContentKind.Movie
    },
    camrip = false,
    type = type,
)

internal fun String.favoriteType(series: Boolean = false): String = when (lowercase()) {
    "serial", "series", "tv" -> "serial"
    "multfilm", "mult", "cartoon" -> "multfilm"
    "anime" -> "anime"
    "movie" -> "movie"
    else -> if (series) "serial" else "movie"
}
