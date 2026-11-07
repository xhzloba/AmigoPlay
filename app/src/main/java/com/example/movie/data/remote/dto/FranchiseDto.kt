package com.example.movie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class TimelineDto(
    @SerialName("kp_id") val kpId: JsonElement? = null,
)

@Serializable
internal data class FranchiseDto(
    val id: JsonElement? = null,
    val slogan: String? = null,
    val budget: String? = null,
    @SerialName("fees_world") val feesWorld: String? = null,
    @SerialName("fees_rus") val feesRus: String? = null,
    val premier: String? = null,
    @SerialName("premier_rus") val premierRus: String? = null,
    @SerialName("rate_mpaa") val rateMpaa: String? = null,
    @SerialName("serial_status") val serialStatus: String? = null,
    val type: String? = null,
    @SerialName("kinopoisk_id") val kinopoiskId: JsonElement? = null,
    @SerialName("imdb_id") val imdbId: JsonElement? = null,
    val director: JsonElement? = null,
    val screenwriter: JsonElement? = null,
    val producer: JsonElement? = null,
    val operator: JsonElement? = null,
    val editor: JsonElement? = null,
    val design: JsonElement? = null,
    val country: JsonElement? = null,
    val genre: JsonElement? = null,
    @SerialName("actors_dubl") val actorsDubl: JsonElement? = null,
    val voiceActing: JsonElement? = null,
    val trivia: JsonElement? = null,
    val trailers: JsonElement? = null,
    val facts: JsonElement? = null,
    @SerialName("shooting_photos") val shootingPhotos: JsonElement? = null,
    val seasons: JsonElement? = null,
)
