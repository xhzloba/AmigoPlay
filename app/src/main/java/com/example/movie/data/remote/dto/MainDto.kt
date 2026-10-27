package com.example.movie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MainResponseDto(
    val main: List<SectionDto> = emptyList(),
    val channels: List<SectionDto> = emptyList(),
)

@Serializable
internal data class SectionDto(
    val title: String = "",
    @SerialName("playlist_url") val playlistUrl: String = "",
)
