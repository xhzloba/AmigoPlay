package com.example.movie.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ViewResponseDto(
    val details: MovieDetailsDto = MovieDetailsDto(),
    val casts: JsonElement? = null,
    val similars: JsonElement? = null,
    val sequelsAndPrequels: JsonElement? = null,
)

@Serializable
internal data class CastMemberDto(
    val id: String = "",
    val title: String = "",
    val poster: String? = null,
)
