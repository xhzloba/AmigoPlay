package com.example.movie.domain.model

data class Franchise(
    val id: Int? = null,
    val directors: List<String> = emptyList(),
    val screenwriters: List<String> = emptyList(),
    val producers: List<String> = emptyList(),
    val operators: List<String> = emptyList(),
    val editors: List<String> = emptyList(),
    val designers: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val premiere: String? = null,
    val premiereRu: String? = null,
    val slogan: String? = null,
    val mpaa: String? = null,
    val budget: String? = null,
    val feesWorld: String? = null,
    val feesRus: String? = null,
    val dubActors: List<String> = emptyList(),
    val voiceActing: List<String> = emptyList(),
    val serialStatus: String? = null,
    val series: Boolean = false,
    val photos: List<String> = emptyList(),
    val facts: List<String> = emptyList(),
    val trailers: List<FranchiseTrailer> = emptyList(),
    val seasons: List<FranchiseSeason> = emptyList(),
    val kinopoiskId: Int? = null,
    val imdbId: String? = null,
)

data class FranchiseTrailer(
    val name: String,
    val posterUrl: String?,
    val url: String,
    val season: Int? = null,
)

data class FranchiseSeason(
    val number: Int,
    val posterUrl: String?,
    val episodes: List<FranchiseEpisode>,
)

data class FranchiseEpisode(
    val number: Int,
    val name: String?,
    val release: String?,
)
