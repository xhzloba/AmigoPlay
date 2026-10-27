package com.example.movie.data.mapper

import com.example.movie.data.remote.dto.CastMemberDto
import com.example.movie.data.remote.dto.ChannelDto
import com.example.movie.data.remote.dto.MovieDetailsDto
import com.example.movie.data.remote.dto.ViewResponseDto
import com.example.movie.domain.model.CastMember
import com.example.movie.domain.model.ContentKind
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

internal fun ChannelDto.toDomain(): Movie {
    val details = details
    val backdrop = details.bgPoster?.backdrop.orNullIfBlank()
        ?: details.widePoster.orNullIfBlank()
        ?: details.poster.orNullIfBlank()
    return Movie(
        id = details.id.ifBlank { details.ident },
        title = details.name.trim(),
        originalTitle = details.originalname.trim(),
        year = details.released.takeIf { it > 0 },
        overview = details.about.trim(),
        genres = details.genre.trim(),
        country = details.country.trim(),
        director = details.director.trim(),
        posterUrl = details.poster.orNullIfBlank(),
        backdropUrl = backdrop,
        logoUrl = details.logoPoster.orNullIfBlank(),
        ratingKp = details.ratingKp.positiveRating(),
        ratingImdb = details.ratingImdb.positiveRating(),
        age = details.age?.takeIf { it > 0 },
        kind = details.toKind(),
        camrip = details.camrip,
    )
}

private val viewJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    explicitNulls = false
}

internal fun ViewResponseDto.toMovieDetail(): MovieDetail {
    val movie = details.toDetail()
    return movie.copy(
        cast = casts.asItems<CastMemberDto>().mapNotNull { member ->
            val name = member.title.trim()
            if (name.isEmpty()) {
                null
            } else {
                CastMember(
                    id = member.id.ifBlank { name },
                    name = name,
                    photoUrl = member.poster.orNullIfBlank(),
                )
            }
        },
        similar = similars.asItems<ChannelDto>().map { it.toDomain() }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() },
        sequels = sequelsAndPrequels.asItems<ChannelDto>().map { it.toDomain() }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() },
    )
}

private inline fun <reified T> JsonElement?.asItems(): List<T> {
    val array = this as? JsonArray ?: return emptyList()
    return array.mapNotNull { element ->
        runCatching { viewJson.decodeFromJsonElement<T>(element) }.getOrNull()
    }
}

internal fun MovieDetailsDto.toDetail(): MovieDetail {
    val backdrops = buildList {
        bgPoster?.backdrop.orNullIfBlank()?.fullBackdrop()?.let(::add)
        val pattern = bgPoster?.pattern.orNullIfBlank()?.fullBackdrop()
        bgPoster?.ids.orEmpty().forEach { rawId ->
            val id = rawId.trim()
            if (id.isEmpty() || pattern == null) return@forEach
            val url = pattern.replace("{id}", id)
            if (url !in this) add(url)
        }
    }
    return MovieDetail(
        id = id.ifBlank { ident },
        title = name.trim(),
        logoUrl = logoPoster.orNullIfBlank(),
        posterUrl = poster.orNullIfBlank(),
        year = released.takeIf { it > 0 },
        runtimeMinutes = runtime.takeIf { it > 0 },
        age = age?.takeIf { it > 0 },
        genres = genre.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        overview = about.trim(),
        ratingKp = ratingKp.positiveRating(),
        ratingImdb = ratingImdb.positiveRating(),
        backdrops = backdrops,
    )
}

private fun MovieDetailsDto.toKind(): ContentKind =
    when (type.lowercase()) {
        "movie" -> ContentKind.Movie
        "serial" -> ContentKind.Series
        "anime" -> ContentKind.Anime
        else -> if (isTv) ContentKind.Series else ContentKind.Other
    }

private fun String?.orNullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private val BackdropSize = Regex("/t/p/w\\d+/")

private fun String.fullBackdrop(): String = replace(BackdropSize, "/t/p/original/")

private fun JsonElement?.positiveRating(): Float? {
    val raw = when (this) {
        null, is JsonNull -> return null
        is JsonPrimitive -> contentOrNull
        else -> null
    } ?: return null
    return raw.trim().replace(',', '.').toFloatOrNull()?.takeIf { it > 0f }
}
