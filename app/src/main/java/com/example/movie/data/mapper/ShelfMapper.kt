package com.example.movie.data.mapper

import com.example.movie.data.remote.dto.MainResponseDto
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.Shelf
import java.net.URLDecoder

internal fun MainResponseDto.toShelves(): List<Shelf> {
    val listTitles = buildMap {
        main.forEach { section ->
            val query = section.playlistUrl.toListQuery() ?: return@forEach
            if (section.title.isNotBlank()) put(query.id, section.title.trim())
        }
    }
    val channelTitles = buildMap {
        channels.forEach { channel ->
            val type = channel.playlistUrl.queryParam("type") ?: return@forEach
            if (channel.title.isNotBlank()) put(type, channel.title.trim())
        }
    }
    return listOf(
        shelf(listTitles["popular:"] ?: "В тренде", "popular", null),
        shelf("Сейчас смотрят", "watching", null),
        shelf("4K", "new", null, uhd = true),
        shelf("Dolby Vision", "new", null, dolbyVision = true),
        shelf(listTitles["updatings:"] ?: "Обновления", "updatings", null),
        shelf(listTitles["popular:movie"] ?: "Фильмы", "popular", "movie"),
        shelf(listTitles["popular:serial"] ?: "Сериалы", "popular", "serial"),
        shelf(channelTitles["multfilm"] ?: "Мультфильмы", "popular", "multfilm"),
    )
}

private fun shelf(
    title: String,
    sort: String,
    type: String?,
    uhd: Boolean = false,
    dolbyVision: Boolean = false,
): Shelf {
    val query = CatalogQuery(sort = sort, type = type, uhd = uhd, dolbyVision = dolbyVision)
    return Shelf(id = query.id, title = title, query = query)
}

private fun String.toListQuery(): CatalogQuery? {
    if (!contains("/v2/list")) return null
    val sort = queryParam("sort") ?: return null
    return CatalogQuery(sort = sort, type = queryParam("type")?.takeIf { it.isNotBlank() })
}

private fun String.queryParam(name: String): String? {
    val query = substringAfter('?', missingDelimiterValue = "")
    return query.split('&').firstNotNullOfOrNull { part ->
        val pieces = part.split('=', limit = 2)
        if (pieces.size == 2 && pieces[0] == name) {
            URLDecoder.decode(pieces[1], Charsets.UTF_8)
        } else {
            null
        }
    }
}
