package com.example.movie.data.mapper

import com.example.movie.data.remote.dto.CatalogResponseDto
import com.example.movie.domain.model.ContentKind
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieMapperTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun mapsPopularItem_andTreatsZeroRatingAsMissing() {
        val payload = """
            {
              "channels": [
                {
                  "details": {
                    "id": "1",
                    "type": "movie",
                    "name": "Битва моторов",
                    "originalname": "Битва моторов",
                    "released": 2026,
                    "about": "текст",
                    "director": "Илья Маланин",
                    "country": "Россия",
                    "genre": "приключения, драма",
                    "poster": "https://example.com/p.jpg",
                    "wide_poster": null,
                    "logo_poster": "https://example.com/l.png",
                    "bg_poster": {},
                    "rating_kp": "0.0",
                    "rating_imdb": 8.1,
                    "age": 12,
                    "is_tv": false,
                    "camrip": true
                  }
                }
              ]
            }
        """.trimIndent()

        val movie = json.decodeFromString<CatalogResponseDto>(payload).channels.first().toDomain()

        assertEquals("1", movie.id)
        assertEquals("Битва моторов", movie.title)
        assertEquals(2026, movie.year)
        assertEquals("https://example.com/p.jpg", movie.backdropUrl)
        assertEquals("https://example.com/l.png", movie.logoUrl)
        assertNull(movie.ratingKp)
        assertEquals(8.1f, movie.ratingImdb ?: 0f, 0.001f)
        assertEquals(12, movie.age)
        assertEquals(ContentKind.Movie, movie.kind)
        assertEquals(true, movie.camrip)
    }

    @Test
    fun prefersBackdropOverPoster() {
        val payload = """
            {
              "channels": [
                {
                  "details": {
                    "id": "serial-1",
                    "type": "serial",
                    "name": "Гангстерленд",
                    "poster": "https://example.com/poster.jpg",
                    "wide_poster": "https://example.com/wide.jpg",
                    "bg_poster": { "backdrop": "https://example.com/back.jpg" },
                    "rating_kp": "8.2"
                  }
                }
              ]
            }
        """.trimIndent()

        val movie = json.decodeFromString<CatalogResponseDto>(payload).channels.first().toDomain()

        assertEquals("https://example.com/back.jpg", movie.backdropUrl)
        assertEquals(8.2f, movie.ratingKp ?: 0f, 0.001f)
        assertEquals(ContentKind.Series, movie.kind)
    }
}
