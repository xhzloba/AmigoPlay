package com.example.movie.domain.model

data class CatalogQuery(
    val sort: String,
    val type: String? = null,
    val uhd: Boolean = false,
    val dolbyVision: Boolean = false,
    val genre: String? = null,
) {
    val id: String = buildString {
        append(sort)
        append(':')
        append(type.orEmpty())
        if (uhd) append(":uhd")
        if (dolbyVision) append(":dolby")
        if (!genre.isNullOrBlank()) append(':').append(genre)
    }
}

data class Shelf(
    val id: String,
    val title: String,
    val query: CatalogQuery,
)
