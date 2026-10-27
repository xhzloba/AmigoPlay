package com.example.movie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class CatalogResponseDto(
    val channels: List<ChannelDto> = emptyList(),
)

@Serializable
internal data class ChannelDto(
    val details: MovieDetailsDto = MovieDetailsDto(),
)

@Serializable
internal data class MovieDetailsDto(
    val id: String = "",
    val ident: String = "",
    val type: String = "",
    val name: String = "",
    val originalname: String = "",
    val released: Int = 0,
    val runtime: Int = 0,
    val about: String = "",
    val director: String = "",
    val country: String = "",
    val genre: String = "",
    val poster: String? = null,
    @SerialName("is_tv") val isTv: Boolean = false,
    @SerialName("wide_poster") val widePoster: String? = null,
    @SerialName("logo_poster") val logoPoster: String? = null,
    @SerialName("bg_poster") val bgPoster: BackdropDto? = null,
    @SerialName("rating_kp") val ratingKp: JsonElement? = null,
    @SerialName("rating_imdb") val ratingImdb: JsonElement? = null,
    val age: Int? = null,
    val camrip: Boolean = false,
)

@Serializable
internal data class BackdropDto(
    val backdrop: String? = null,
    val ids: List<String> = emptyList(),
    val pattern: String? = null,
)
