package com.example.movie.domain.model

data class CastMember(
    val id: String,
    val name: String,
    val photoUrl: String?,
)

data class MovieDetail(
    val id: String,
    val title: String,
    val logoUrl: String?,
    val posterUrl: String?,
    val year: Int?,
    val runtimeMinutes: Int?,
    val age: Int?,
    val genres: List<String>,
    val overview: String,
    val ratingKp: Float?,
    val ratingImdb: Float?,
    val backdrops: List<String>,
    val type: String = "",
    val cast: List<CastMember> = emptyList(),
    val similar: List<Movie> = emptyList(),
    val sequels: List<Movie> = emptyList(),
)
